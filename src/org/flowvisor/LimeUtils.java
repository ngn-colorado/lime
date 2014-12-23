package org.flowvisor;

import java.util.HashMap;

import org.flowvisor.PortInfo.PortType;
import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.message.FVFlowMod;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;

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
	
	
	public static boolean parseJsonConfig(String jsonConfig, JsonFormat type, LimeMigrationHandler handler){
		try {
			Object obj = JSONValue.parseWithException(jsonConfig);
			JSONObject json = (JSONObject)obj;
			for(Object jsonObj : json.keySet()){
				switch(type){
					case SWITCH:
						String dpid = (String)jsonObj;
						JSONObject configObj = (JSONObject) json.get(jsonObj);
						return processSwitch(configObj, (String)jsonObj);
					case HOST:
						LimeHost host = parseVM((JSONObject)jsonObj);
						return handler.migrateVM(host);
					default:
						return false;
				}
			}
			System.out.println("No json objects in key set");
			return false;
		} catch (ParseException e) {
			System.out.println("JSON parsing failed");
			return false;
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
		HashMap<Short, PortInfo> portMap = processPorts(portsObj);
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
//			LimeContainer.addOriginalSwitch(currentSwitch.getDpidLong(), portMap);
//			LimeContainer.insertActiveToOriginalSwitchMap(currentSwitch.getDpidLong(), currentSwitch.getDpidLong());
			return true;
		} else if(isClone){
			if(originalDpid == null){
				System.out.println("Need the original dpid for the clone switch");
				return false;
			}
			System.out.println("Writing switch with DPID: "+currentSwitch.getDpidHexString()+" as clone switch to Lime with original switch: "+originalDpid.getDpidHexString() +" with port map:\n"+printPortMap(portMap));
//			LimeContainer.addCloneSwitch(currentSwitch.getDpidLong(), portMap);
//			LimeContainer.insertActiveToCloneSwitchMap(originalDpid.getDpidLong(), currentSwitch.getDpidLong());
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

	private static HashMap<Short, PortInfo> processPorts(JSONObject portsObj) {
		HashMap<Short, PortInfo> portMap = new HashMap<Short, PortInfo>();
		for(Object portObj : portsObj.keySet()){
			String typeString = (String) portsObj.get(portObj);
			Short port = Short.parseShort((String)portObj);
			PortType type = PortType.valueOf(typeString);
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
		FVFlowMod deleteMod = (FVFlowMod) flowMod.clone();
		deleteMod.setCommand(OFFlowMod.OFPFC_DELETE);
		OFFlowMod fm = new OFFlowMod();
//		fm.setMatch(match);
//		fm.setCommand(OFFlowMod.OFPFC_DELETE);
		deleteMod.setOutPort(OFPort.OFPP_NONE);
		deleteMod.setBufferId(0xffffffff); // buffer to NONE
		destinationSwitch.sendMsg(fm, destinationSwitch);
	}
}
