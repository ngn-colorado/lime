package org.flowvisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowvisor.PortInfo.PortType;
import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.message.FVFlowMod;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionStripVirtualLan;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;

public class LimeUtils {
//	json format:
//	{
//		<switch-dpid>:{
//			ports:{
//				<port-number>:<type> -> one of H_CONNECTED, GHOST, EMPTY
//			},
//			original:<value> -> value: dpid(original switch of this clone), if value not present, assume original
//		}
//	}
	public static enum JsonFormat{
		SWITCH, HOST
	}
	
	/**
	 * 
	 * @param jsonConfig
	 * @param type
	 * @param handler
	 * @return null on failure, String to print on web page on success
	 */
	public static String parseJsonConfig(String jsonConfig, JsonFormat type){
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
						return processSwitch(configObj, (String)jsonObj) ? response + "successfully\n" : response + "unsuccessfully\n";
					}
					System.out.println("No json objects in key set");
					return null;
				case HOST:
					LimeHost host = parseVM(json);
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
		}
	}

	private static LimeHost parseVM(JSONObject jsonObj) {
		String originalHost = (String)jsonObj.get("originalHost");
		if(!validIPAddress(originalHost)){
			System.out.println("original host ip is invalid");
			return null;
		}
		
		String destinationHost = (String)jsonObj.get("destinationHost");
		if(!validIPAddress(destinationHost)){
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

	private static boolean processSwitch(JSONObject switchObj, String dpid) {
		DPID currentSwitch = new DPID(dpid);
		JSONObject portsObj = (JSONObject)switchObj.get("ports");
		String originalObj = (String)switchObj.get("original");
		if(portsObj == null){
			System.out.println("A switch should have ports defined");
		}
		HashMap<Short, PortInfo> portMap = processPorts(portsObj, currentSwitch);
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
			System.out.println("Writing switch with DPID: "+currentSwitch.getDpidHexString()+" as original switch to Lime with port map:\n"+printPortMap(portMap));
			LimeContainer.addOriginalSwitch(currentSwitch.getDpidLong(), portMap);
			LimeContainer.insertActiveToOriginalSwitchMap(currentSwitch.getDpidLong(), currentSwitch.getDpidLong());
			//need a copy of the port map, not the same reference
			LimeContainer.getDpidToPortInfoMap().put(currentSwitch, new HashMap<Short, PortInfo>(portMap));
			return true;
		} else if(isClone){
			if(originalDpid == null){
				System.out.println("Need the original dpid for the clone switch");
				return false;
			}
			System.out.println("Writing switch with DPID: "+currentSwitch.getDpidHexString()+" as clone switch to Lime with original switch: "+originalDpid.getDpidHexString() +" with port map:\n"+printPortMap(portMap));
			LimeContainer.addCloneSwitch(currentSwitch.getDpidLong(), portMap);
			//need a copy of the port map, not the same reference
			LimeContainer.getDpidToPortInfoMap().put(currentSwitch, new HashMap<Short, PortInfo>(portMap));
			LimeContainer.insertActiveToCloneSwitchMap(originalDpid.getDpidLong(), currentSwitch.getDpidLong());
			return true;
		} else{
			System.out.println("Illegal state");
			return false;
		}
	}

	private static String printPortMap(HashMap<Short, PortInfo> portMap) {
		String response = "";
		for(Short port : portMap.keySet()){
			PortInfo info = portMap.get(port);
			response += "Port: "+port+" Type: "+info.getType()+"\n";
		}
		return response;
	}

	private static HashMap<Short, PortInfo> processPorts(JSONObject portsObj, DPID dpid) {
		HashMap<Short, PortInfo> portMap = new HashMap<Short, PortInfo>();
		for(Object portObj : portsObj.keySet()){
			JSONObject portInfo = (JSONObject)portsObj.get(portObj);
			String typeString = (String) portInfo.get("type");
			Short port = Short.parseShort((String)portObj);
			PortType type = PortType.valueOf(typeString);
			if(type == PortType.H_CONNECTED){
				String mac = (String)portInfo.get("mac");
				LimeUtils.storeMacForDpid(dpid, port, mac, LimeContainer.getDpidToMacMap());
			}
			portMap.put(port, new PortInfo(type, null, null));
		}
		return portMap;
	}
	
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

	public static LimeHost parseVM(String data) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void deleteFlowMod(WorkerSwitch destinationSwitch, FVFlowMod flowMod){
//		FVFlowMod deleteMod = (FVFlowMod) flowMod.clone();
		FVFlowMod deleteMod = (FVFlowMod) FlowVisor.getInstance().getFactory().getMessage(OFType.FLOW_MOD);
		deleteMod.setMatch(flowMod.getMatch());
		deleteMod.setCommand(OFFlowMod.OFPFC_DELETE);
//		OFFlowMod fm = new OFFlowMod();
//		fm.setMatch(match);
//		fm.setCommand(OFFlowMod.OFPFC_DELETE);
		deleteMod.setOutPort(OFPort.OFPP_NONE);
		deleteMod.setBufferId(0xffffffff); // buffer to NONE
		destinationSwitch.sendMsg(deleteMod, destinationSwitch);
	}
	
	
	public static LimeVlanTranslationInfo cloneTranslationInfoMigrated(LimeVlanTranslationInfo originalInfo, boolean targetMigrated) {
		return new LimeVlanTranslationInfo(originalInfo.getReceiverMod(), 
										   originalInfo.getSenderMod(), 
										   originalInfo.getReceiverSwitch(),
										   originalInfo.getSenderSwitch(),
										   originalInfo.isOriginalToClone(),
										   originalInfo.getOriginalMod(), 
										   originalInfo.getVlanNumber(),
										   targetMigrated);
	}

	public static short getFlowModOutputPort(FVFlowMod receiverMod) {
		for(OFAction action : receiverMod.getActions()){
			if(action instanceof OFActionOutput){
				//TODO: still only supporting one output port for now
				return ((OFActionOutput) action).getPort();
			}
		}
		return 0;
	}
	
	public static boolean hasInputPortWithoutVlan(FVFlowMod flowMod, Short connectedPort) {
		List<OFAction> actions = flowMod.getActions();
		for(OFAction action : actions){
			if(action instanceof OFActionVirtualLanIdentifier || action instanceof OFActionStripVirtualLan){
				return false;
			}
		}
		OFMatch match = flowMod.getMatch();
		return (match.getInputPort() == connectedPort);
	}
	
	public static boolean outputPortMigrated(DPID switchDpid, FVFlowMod flowMod, short port, List<LimeHost> migratedHosts) {
		for(OFAction action : flowMod.getActions()){
			if(action instanceof OFActionOutput){
				short outPort =  ((OFActionOutput) action).getPort();
				for(LimeHost host : migratedHosts){
					//if the current switch is the original switch of the host AND
					//the port being checked is the current outputPort being examined AND
					//the current outputPort is the original connected port of the host, THEN
					//the host was migrated, as it is present in the migrated hosts list
					if(host.getOriginalDpid().equals(switchDpid) && port == outPort && host.getConnectedPort() == outPort){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static void addToVlanMap(DPID senderSwitch, FVFlowMod senderVlanMod, Map<DPID, List<FVFlowMod>> vlanHandlerMods) {
		if(vlanHandlerMods.containsKey(senderSwitch)){
			List<FVFlowMod> flowMods = vlanHandlerMods.get(senderSwitch);
			flowMods.add(senderVlanMod);
		} else{
			ArrayList<FVFlowMod> flowMods = new ArrayList<FVFlowMod>();
			flowMods.add(senderVlanMod);
			vlanHandlerMods.put(senderSwitch, flowMods);
		}
		
	}
	
	public static byte[] convertMacToBytes(String macAddress) {
		String[] macAddressParts = macAddress.split(":");

		// convert hex string to byte values
		byte[] macAddressBytes = new byte[6];
		for(int i=0; i<6; i++){
		    Integer hex = Integer.parseInt(macAddressParts[i], 16);
		    macAddressBytes[i] = hex.byteValue();
		}
		return macAddressBytes;
	}
	
	public static void sendFlowMod(FVFlowMod flowMod, WorkerSwitch modifiedSwitch) {
		DPID switchDpid = new DPID(modifiedSwitch.getDPID());
		System.out.println("\n\n\nSwitch "+switchDpid.getDpidString()+" is receiving flow mod: "+flowMod+"\n\n");
		modifiedSwitch.handleFlowModAndSend(flowMod);
		
	}
	
	public static void storeMacForDpid(DPID dpid, Short port, String mac, HashMap<DPID, HashMap<Short, String>> dpidToMacMap){
		HashMap<Short, String> current;
		boolean exists = dpidToMacMap.containsKey(dpid);
		if(!exists){
			current = new HashMap<Short, String>();
		} else{
			current= dpidToMacMap.get(dpid);
		}
		current.put(port, mac);
		if(!exists){
			dpidToMacMap.put(dpid, current);
		}
	}
	
	public static String getMacForPort(DPID dpid, short port, HashMap<DPID, HashMap<Short, String>> dpidToMacMap) throws MacLookupException {
		//TODO: for now, ignore errors. may need to deal with them later
		if(!dpidToMacMap.containsKey(dpid)){
			throw new MacLookupException("Invalid dpid: "+dpid);
		}
		HashMap<Short, String> current = dpidToMacMap.get(dpid);
		if(!current.containsKey(port)){
			throw new MacLookupException("Invalid port: "+port);
		}
		return current.get(port);
	}
}
