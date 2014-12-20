package org.flowvisor;

import java.util.HashMap;

import org.flowvisor.PortInfo.PortType;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

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
	
	
	
	public static boolean parseJsonConfig(String jsonConfig){
		try {
			Object obj = JSONValue.parseWithException(jsonConfig);
			JSONObject json = (JSONObject)obj;
			for(Object dpidObj : json.keySet()){
				String dpid = (String)dpidObj;
				JSONObject switchObj = (JSONObject) json.get(dpidObj);
				if(!processSwitch(switchObj, (String)dpidObj)){
					return false;
				}
			}
		} catch (ParseException e) {
			System.out.println("JSON parsing failed");
			return false;
		}
		
		return true;
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
}
