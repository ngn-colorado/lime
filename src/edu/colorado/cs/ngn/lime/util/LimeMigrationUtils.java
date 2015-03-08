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

/**
 * Class to store utility functions for the LimeMigrationHandler
 * 
 * @author Michael Coughlin
 *
 */
public class LimeMigrationUtils {

	/**
	 * Delete a flow mod from a switch
	 * 
	 * @param destinationSwitch The WorkerSwitch object of the switch to delete the flow mod from
	 * @param flowMod The flow mod to be deleted
	 */
	public static void deleteFlowMod(WorkerSwitch destinationSwitch, FVFlowMod flowMod){
		FVFlowMod deleteMod = (FVFlowMod) FlowVisor.getInstance().getFactory().getMessage(OFType.FLOW_MOD);
		deleteMod.setMatch(flowMod.getMatch());
		deleteMod.setCommand(OFFlowMod.OFPFC_DELETE);
		deleteMod.setOutPort(OFPort.OFPP_NONE);
		deleteMod.setBufferId(0xffffffff); // buffer to NONE
		destinationSwitch.sendMsg(deleteMod, destinationSwitch);
	}
	
	/**
	 * Clone a LimeVlanTranslationInfo into a new object with the receiverTargetMigrated flag
	 * set to targetMigrated
	 * 
	 * @param originalInfo The LimeVlanTranslationInfo to be cloned
	 * @param targetMigrated Whether the target of the flow mod pair has been migrated
	 * @return The cloned LimeVlanTranslationInfo object
	 * 
	 * @deprecated This function is no longer relevant now that lime supports multiple output ports
	 */
	@Deprecated
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

	/**
	 * Get the output port of a FlowMod.
	 * 
	 * Note: returns the output port of the first OFActionOutput in the mod's action list
	 * 
	 * @param receiverMod The mod to be checked
	 * @return The found output port
	 * 
	 * @deprecated This function is no longer relevent now that lime supports multiple output ports
	 */
	@Deprecated
	public static short getFlowModOutputPort(FVFlowMod receiverMod) {
		for(OFAction action : receiverMod.getActions()){
			if(action instanceof OFActionOutput){
				//TODO: still only supporting one output port for now
				return ((OFActionOutput) action).getPort();
			}
		}
		return 0;
	}
	
	/**
	 * Check if a flow mod has the passed input port in its match but does not
	 * have any OFActionVirtualLanIdentifier of OFActionStripVirtualLan actions
	 * in its action list
	 * 
	 * @param flowMod The flow mod the be checked
	 * @param connectedPort The input port to check in the flow mod
	 * @return True if the input exists as the match input port and there are no vlan actions, False otherwise
	 */
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
	 * Checks if an output port in an OFActionOuput action in the input flow mod has been migrated from the 
	 * switch indicated by dpid. It determines this by checking if a host with the connected port equal to the input port
	 * is present in the input list of migrated hosts.
	 * 
	 * @param switchDpid The switch to check for the migrated host
	 * @param flowMod The host to check if an output has been migrated
	 * @param port The port to check if it has been migrated
	 * @param migratedHosts The list of migrated hosts
	 * @return True if the host connected to the port on the switch has been migrated and the flow mod would output to this port, False otherwise
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
	
	/**
	 * Adds a vlan migration mod to the map of dpids to corresponding vlan mods
	 * 
	 * @param senderSwitch The switch that the vlan mod was written to
	 * @param vlanMod The vlan mod that was written
	 * @param vlanHandlerMods The map of dpids to vlan mods
	 */
	public static void addToVlanMap(DPID senderSwitch, FVFlowMod vlanMod, Map<DPID, List<FVFlowMod>> vlanHandlerMods) {
		if(vlanHandlerMods.containsKey(senderSwitch)){
			List<FVFlowMod> flowMods = vlanHandlerMods.get(senderSwitch);
			flowMods.add(vlanMod);
		} else{
			ArrayList<FVFlowMod> flowMods = new ArrayList<FVFlowMod>();
			flowMods.add(vlanMod);
			vlanHandlerMods.put(senderSwitch, flowMods);
		}
		
	}
	
	/**
	 * Convert a mac address string to an array of bytes
	 * 
	 * @param macAddress The mac address to convert
	 * @return The converted array of bytes
	 */
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
	
	/**
	 * Writes an OF mod to a switch
	 * 
	 * @param flowMod The flow mod to be written
	 * @param modifiedSwitch The WorkerSwitch object that represents the switch the mod is to be written to
	 */
	public static void sendFlowMod(FVFlowMod flowMod, WorkerSwitch modifiedSwitch) {
		DPID switchDpid = new DPID(modifiedSwitch.getDPID());
		System.out.println("\n\n\nSwitch "+switchDpid.getDpidString()+" is receiving flow mod: "+flowMod+"\n\n");
		modifiedSwitch.handleFlowModAndSend(flowMod);
		
	}
	
	/**
	 * Stores a mac address for a host on a switch into the map of dpids to connected mac addresses 
	 * 
	 * @param dpid The switch the mac address is associated with
	 * @param port The port that the mac address is connected to
	 * @param mac The mac address to be stored
	 * @param dpidToMacMap The map of dpids to mac addresses
	 */
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
	
	/**
	 * Gets the mac address associated with a port and switch dpid. Throws a lookup error
	 * if an entry does not exist for this port
	 * 
	 * @param dpid The switch the mac address is connected to
	 * @param port The port the mac address is connected to
	 * @param dpidToMacMap The map of dpids to mac address
	 * @return The found mac address string
	 * @throws MacLookupException Thrown if an entry is not found in the dpid to mac address map
	 * 
	 * @Precondition The port to be looked-up must have a mac address associated with it on the input dpid in the dpid to mac map
	 */
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

	/**
	 * Checks if the input port of a flow mod has been migrated. This
	 * is determined using the input list of migrated hosts.
	 * 
	 * @param flowMod The flow mod to be checked
	 * @param dpid The dpid to be checked
	 * @param migratedHosts The list of migrated hosts
	 * @return True id the input port has been migrated, False otherwise
	 */
	public static boolean inputPortMigrated(FVFlowMod flowMod, DPID dpid, List<LimeHost> migratedHosts) {
		short inputPort = flowMod.getMatch().getInputPort();
		if(inputPort <= 0 || 
		   !LimeContainer.getDpidToOriginalPortInfoMap().get(dpid).containsKey(inputPort) || 
		   LimeContainer.getDpidToOriginalPortInfoMap().get(dpid).get(inputPort).getType() != PortType.H_CONNECTED){
			return false;
		}
		for(LimeHost host : migratedHosts){
			if(inputPort == host.getConnectedPort()){
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if a flow mod has the provided port as an output port in its action list
	 * 
	 * @param flowMod The flow mod to be checked
	 * @param inputPort The output port to be checked
	 * @return True is the input is in the flow mod's action list, False otherwise
	 */
	public static boolean hasOutputPortWithoutVlan(FVFlowMod flowMod, Short inputPort) {
		boolean hasOutputPort = false;
		for(OFAction action : flowMod.getActions()){
			if(action instanceof OFActionVirtualLanIdentifier || action instanceof OFActionStripVirtualLan){
				return false;
			}
			if(action instanceof OFActionOutput && ((OFActionOutput) action).getPort() == inputPort){
				hasOutputPort = true;
			}
		}
		return hasOutputPort;
	}

	/**
	 * Finds and returns the dummy port that is associated with each OVX switch.
	 * 
	 * Note: this is needed for OVX compatibility. Each flow mod written to OVX must have an input port
	 * set in its OFMatch, even if the input port is wildcarded, else OVX throws a NullPointerException.
	 * If this input port is equal to a port that is an output port of a mod (EVEN if the input port 
	 * is wildcarded), then the mod is modified by OVX to set the output action from output to the port 
	 * to output to INPORT. If the input port is set to the ghost port (EVEN if the input port is 
	 * wildcarded) then OVX modifies the flow mod so that there is a mod_dl_dst and mod_dl_src that
	 * is set to the values that are stored in an OVX data structure. The logic of this is not clear,
	 * but must be related to how OVX handles the virtualization of flow mods. However, this function
	 * is clearly bugged. The mac address that is written is the value of a the dl_dst and dl_src of a 
	 * flow mod that outputs to a virtual link (EVEN if the dl_dst and dl_src of this mod is wildcarded
	 * and these values are set to 00:00:00:00:00:00) that matches the value of dl_dst and dl_src in
	 * the OF match of the mod being written (EVEN if THIS mod also wildcards dl_dst and dl_src and sets
	 * them to 00:00:00:00:00:00). If 00:00:00:00:00:00 is found by OVX, or no value is found, then
	 * 00:00:00:00:00:00 is written as the dl_dst or dl_src, which likely makes the packets matching this
	 * mod unrouteable. This can be circumvented in the single output port case by having the sending
	 * mod set the dl_src and dl_dst fields of the flow mod to the dl_src and dl_dst of the input and
	 * output ports, but this fails in the multiple ports case. Therefore, need to set the input port
	 * of receiver migration mods to a dummy port - an active port in the OVX network that is not connected
	 * to a anything useful in the physical plane. 
	 * 
	 * @param currentSwitch The switch to lookup the dummy port in
	 * @return The dummy port for this switch
	 * @throws LimeDummyPortNotFoundException
	 */
	public static short getDummyPort(DPID currentSwitch) throws LimeDummyPortNotFoundException {
		Map<Short, PortInfo> portInfo = LimeContainer.getDpidToOriginalPortInfoMap().get(currentSwitch);
		for(short port : portInfo.keySet()){
			if(portInfo.get(port).getType() == PortType.DUMMY){
				return port;
			}
		}
		throw new LimeDummyPortNotFoundException("Cannot use Lime on OVX without providing a dummy port. Each OVX switch needs to have a dummy port");
	}
	
	/**
	 * Checks if a flow has a valid output action and does not contain a stripvlanaction or virtuallanidentifier action
	 * 
	 * @param flowMod The flow mod to be checked
	 * @return True if the flow mod is valid without vlan actions, false otherwise
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
