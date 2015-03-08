package edu.colorado.cs.ngn.lime.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowvisor.FlowVisor;
import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.message.FVFlowMod;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionStripVirtualLan;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;

import edu.colorado.cs.ngn.lime.LimeContainer;
import edu.colorado.cs.ngn.lime.exceptions.LimeDummyPortNotFoundException;
import edu.colorado.cs.ngn.lime.exceptions.MacLookupException;
import edu.colorado.cs.ngn.lime.migration.LimeHost;
import edu.colorado.cs.ngn.lime.migration.LimeVlanTranslationInfo;
import edu.colorado.cs.ngn.lime.util.PortInfo.PortType;

public class LimeMigrationUtils {

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
	
	/**
	 * checks if an output port in an OFActionOuput action in the input flow mod has been migrated from the 
	 * switch indicated by dpid. it determines this if a host with the connected port equal to the input port
	 * is present in the list of migrated hosts that is another input
	 * @param switchDpid
	 * @param flowMod
	 * @param port
	 * @param migratedHosts
	 * @return
	 */
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

	public static boolean inputPortMigrated(FVFlowMod flowMod, DPID originalDpid, List<LimeHost> migratedHosts) {
		short inputPort = flowMod.getMatch().getInputPort();
		if(inputPort <= 0 || 
		   !LimeContainer.getDpidToPortInfoMap().get(originalDpid).containsKey(inputPort) || 
		   LimeContainer.getDpidToPortInfoMap().get(originalDpid).get(inputPort).getType() != PortType.H_CONNECTED){
			return false;
		}
		for(LimeHost host : migratedHosts){
			if(inputPort == host.getConnectedPort()){
				return true;
			}
		}
		return false;
	}

	public static boolean hasOutputPortWithoutVlan(FVFlowMod flowMod,
			Short connectedPort) {
		boolean hasOutputPort = false;
		for(OFAction action : flowMod.getActions()){
			if(action instanceof OFActionVirtualLanIdentifier || action instanceof OFActionStripVirtualLan){
				return false;
			}
			if(action instanceof OFActionOutput && ((OFActionOutput) action).getPort() == connectedPort){
				hasOutputPort = true;
			}
		}
		return hasOutputPort;
	}

	public static short getDummyPort(DPID currentSwitch) throws LimeDummyPortNotFoundException {
		Map<Short, PortInfo> portInfo = LimeContainer.getDpidToPortInfoMap().get(currentSwitch);
		for(short port : portInfo.keySet()){
			if(portInfo.get(port).getType() == PortType.DUMMY){
				return port;
			}
		}
		throw new LimeDummyPortNotFoundException("Cannot use Lime on OVX without providing a dummy port. Each OVX switch needs to have a dummy port");
	}
	
	/**
	 * Checks if a flow has a valid output action and does not contain a stripvlanaction or virtuallanidentifier action
	 * @param flowMod
	 * @return
	 */
	public static boolean isValidFlowModWithoutVlan(FVFlowMod flowMod) {
		OFMatch match = flowMod.getMatch();
		if(flowMod.getOutPort() == -1 || match.getInputPort() == -1){
			return false;
		}
		for(OFAction action : flowMod.getActions()){
			if(action instanceof OFActionVirtualLanIdentifier || action instanceof OFActionStripVirtualLan){
				return false;
			}
			if(action instanceof OFActionOutput && ((OFActionOutput) action).getPort() == -1){
				return false;
			}
		}
		return true;
	}
}
