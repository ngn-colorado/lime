package edu.colorado.cs.ngn.lime.util;

import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import edu.colorado.cs.ngn.lime.LimeContainer;
import edu.colorado.cs.ngn.lime.exceptions.DPIDLookupException;
import edu.colorado.cs.ngn.lime.exceptions.LimeDummyPortNotFoundException;
import edu.colorado.cs.ngn.lime.exceptions.SwitchOriginalAndCloneException;
import edu.colorado.cs.ngn.lime.migration.LimeHost;
import edu.colorado.cs.ngn.lime.migration.LimeMigrationHandler;
import edu.colorado.cs.ngn.lime.util.PortInfo.PortType;

/**
 * Utility class to hold functions that implements the Lime API logic 
 * 
 * @author Michael Coughlin
 *
 */
public class LimeAPIUtils {

	public static enum JsonFormat{
		SWITCH, HOST
	}

	/** 
	 * Function to parse a JSON config string received by the Lime API.
	 * 
	 * @param jsonConfig The JSON string containing the config parameters passed to the api
	 * @param type The type of JSON config received. Set by the api function that is calling the parse function
	 * @return null on failure, String to print on web page on success
	 */
	public static String parseJsonConfig(String jsonConfig, LimeAPIUtils.JsonFormat type){
		//TODO: prevent config changes once migration has started
		//TODO: add support for overwriting existing config for switches and hosts
		//TODO: see if it is possible to modify the lime data structures after the OVX network has been booted
		try {
			Object obj = JSONValue.parseWithException(jsonConfig);
			JSONObject json = (JSONObject)obj;
			switch(type){
				case SWITCH:
					for(Object jsonObj : json.keySet()){
						//TODO: support more than one switch here
						String dpid = (String)jsonObj;
						JSONObject configObj = (JSONObject) json.get(jsonObj);
						String response = "Configuration was processed ";
						return LimeAPIUtils.processSwitch(configObj, (String)jsonObj) ? response + "successfully\n" : response + "unsuccessfully\n";
					}
					System.out.println("No json objects in key set");
					return null;
				case HOST:
					LimeHost host = LimeAPIUtils.parseVM(json);
					if(host == null){
						return "Could not extract host information from provided json";
					}
					System.out.println("data contains valid json");
					String response = "Machine information was processed ";
					boolean success = false;
					System.out.println("preparing to migrate host: "+host);
					LimeMigrationHandler handler = LimeMigrationHandler.getInstance();
					if(handler.isMigrating()){
						success = handler.migrateVM(host);
					} else{
						return "Migration must be initiated first";
					}
					return success ? response  + "successfully\n" : response + "unsuccessfully\n";
				default:
					return null;
			}
		} catch (ParseException e) {
			System.out.println("JSON parsing failed");
			return null;
		} catch (LimeDummyPortNotFoundException e) {
			String message = "Need to have a dummy port in order to work with OVX";
			System.out.println(message);
			return message;
		} catch (SwitchOriginalAndCloneException e) {
			String message = "Caught illegal state where a switch is both an original and clone switch";
			System.out.println(message);
			return message;
		} catch (DPIDLookupException e) {
			String message = "Tried to lookup a dpid that did not exist";
			System.out.println(message);
			return message;
		}
	}
	
	/**
	 * Parses the config information of a VM and creates a corresponding LimeHost object
	 * with the correct information
	 * 
	 * @param jsonObj The JSON string containing the host config
	 * @return A LimeHost object with a config based off of the passed JSON string
	 */
	private static LimeHost parseVM(JSONObject jsonObj) {
		//TODO: add error catching of invalid configs here, including invalid or already allocated ports
		String originalHost = (String)jsonObj.get("originalHost");
		if(!LimeAPIUtils.validIPAddress(originalHost)){
			System.out.println("original host ip is invalid");
			return null;
		}
		
		String destinationHost = (String)jsonObj.get("destinationHost");
		if(!LimeAPIUtils.validIPAddress(destinationHost)){
			System.out.println("destination host ip is invalid");
			return null;
		}
		
		String libvirtDomain = (String)jsonObj.get("domain");
		if(!LimeVMMigrater.checkDomain(originalHost, libvirtDomain)){
			System.out.println("Could not verify domain name");
			return null;
		}
		
		try{
			DPID originalDpid = new DPID((String)jsonObj.get("originalDpid"));
			DPID cloneDpid = new DPID((String)jsonObj.get("cloneDpid"));
			Short connectedPort = Short.parseShort((String)jsonObj.get("connectedPort"));
			Short clonePort = Short.parseShort((String)jsonObj.get("clonePort"));
			LimeHost host = new LimeHost(originalHost, destinationHost, libvirtDomain, originalDpid, cloneDpid, connectedPort, clonePort);
			return host;
		} catch(NumberFormatException e){
			System.out.println("Could not create Short from the provided port numbers");
			return null;
		} catch (IllegalArgumentException e){
			System.out.println("Could not create a dpid from the provided dpid strings");
			return null;
		} 
	}

	/**
	 * Parse and process a switch's config for a certain DPID. Updates the correct Liem data
	 * structures as well.
	 * 
	 * NOTE: once the OVX network has been booted and switches have been added using this function,
	 * further switch configs do not seem to be recognized
	 * 
	 * @param switchObj The JSON string of the current switch config
	 * @param dpid The DPID of the current switch
	 * @return True if a switch was successfully parsed, False otherwise
	 */
	private static boolean processSwitch(JSONObject switchObj, String dpid) {
		DPID currentSwitch = new DPID(dpid);
		JSONObject portsObj = (JSONObject)switchObj.get("ports");
		String originalObj = (String)switchObj.get("original");
		if(portsObj == null){
			System.out.println("A switch should have ports defined");
		}
		HashMap<Short, PortInfo> portMap = LimeAPIUtils.processPorts(portsObj, currentSwitch);
		if(portMap.size() < 2){
			System.out.println("A switch should have at least two ports");
			return false;
		}
		boolean hasOriginalParam = (originalObj != null);
		boolean isClone = false;
		boolean isOriginal = false;
		DPID originalDpid = null;
		if(hasOriginalParam){
			isClone = true;
			originalDpid = new DPID(originalObj);
		} else{
			isOriginal = true;
		}
		if(!isClone && !isOriginal){
			//assume original switch
			isOriginal = true;
		} else if(isOriginal && isClone){
			//cannot be both
			System.out.println("Illegal state. Cannot be a clone and an original switch");
			return false;
		}
		if(isOriginal){
			System.out.println("Writing switch with DPID: "+currentSwitch.getDpidHexString()+" as original switch to Lime with port map:\n"+LimeAPIUtils.printPortMap(portMap));
			LimeContainer.addOriginalSwitch(currentSwitch.getDpidLong(), portMap);
			LimeContainer.insertActiveToOriginalSwitchMap(currentSwitch.getDpidLong(), currentSwitch.getDpidLong());
			//need a copy of the port map, not the same reference
			LimeContainer.getDpidToOriginalPortInfoMap().put(currentSwitch, LimeAPIUtils.clonePortTable(portMap));
			return true;
		} else if(isClone){
			if(originalDpid == null){
				System.out.println("Need the original dpid for the clone switch");
				return false;
			}
			System.out.println("Writing switch with DPID: "+currentSwitch.getDpidHexString()+" as clone switch to Lime with original switch: "+originalDpid.getDpidHexString() +" with port map:\n"+LimeAPIUtils.printPortMap(portMap));
			LimeContainer.addCloneSwitch(currentSwitch.getDpidLong(), portMap);
			//need a copy of the port map, not the same reference
			LimeContainer.getDpidToOriginalPortInfoMap().put(currentSwitch, LimeAPIUtils.clonePortTable(portMap));
			LimeContainer.insertActiveToCloneSwitchMap(originalDpid.getDpidLong(), currentSwitch.getDpidLong());
			return true;
		} else{
			System.out.println("Illegal state");
			return false;
		}
	}

	/**
	 * Performs a deep clone of a Lime switch port map
	 * 
	 * @param portMap The port map to be cloned
	 * @return The cloned port map
	 */
	private static HashMap<Short, PortInfo> clonePortTable(HashMap<Short, PortInfo> portMap) {
		HashMap<Short, PortInfo> newTable = new HashMap<Short, PortInfo>();
		for(Short port : portMap.keySet()){
			Short newPort = new Short(port);
			PortInfo info = portMap.get(port);
			PortInfo newInfo = new PortInfo(info.getType(), info.getAttachmentMAC(), info.getAttachmentIP());
			newTable.put(newPort, newInfo);
		}
		return newTable;
	}

	/**
	 * Creates a string representation of a Lime port map
	 * 
	 * @param portMap The port map to be printed
	 * @return The string representation of the port map
	 */
	private static String printPortMap(HashMap<Short, PortInfo> portMap) {
		String response = "";
		for(Short port : portMap.keySet()){
			PortInfo info = portMap.get(port);
			response += "Port: "+port+" Type: "+info.getType()+"\n";
		}
		return response;
	}

	/**
	 * Process a set of switch ports for a specific JSON string config. Is received
	 * as part of a switch's JSON config
	 * 
	 * @param portsObj The JSON config of the ports to be processed
	 * @param dpid The DPID of the switch for this port
	 * @return The port map for this switch extracted from the JSON config
	 */
	private static HashMap<Short, PortInfo> processPorts(JSONObject portsObj, DPID dpid) {
		HashMap<Short, PortInfo> portMap = new HashMap<Short, PortInfo>();
		for(Object portObj : portsObj.keySet()){
			JSONObject portInfo = (JSONObject)portsObj.get(portObj);
			String typeString = (String) portInfo.get("type");
			Short port = Short.parseShort((String)portObj);
			PortType type = PortType.valueOf(typeString);
			if(type == PortType.H_CONNECTED){
				String mac = (String)portInfo.get("mac");
				LimeMigrationUtils.storeMacForDpid(dpid, port, mac, LimeContainer.getDpidToMacMap());
			}
			portMap.put(port, new PortInfo(type, null, null));
		}
		return portMap;
	}

	/**
	 * Utility function to check if a string represents a valid IP address
	 * 
	 * @param ip The string to be validated
	 * @return True if the string represents a valid IP address, False otherwise
	 */
	public static boolean validIPAddress(String ip){
		String[] tokens = ip.split("\\.");
		if(tokens.length != 4){
			return false;
		}
		for(String str : tokens){
			try{
				int i = Integer.parseInt(str);
				if(i < 0 || i > 255){
					return false;
				}
			} catch(NumberFormatException e){
				return false;
			}
		}
		return true;
	}

}
