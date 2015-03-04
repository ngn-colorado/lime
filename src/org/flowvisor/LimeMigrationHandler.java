/**
 * 
 */
package org.flowvisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.PortInfo.PortType;
import org.flowvisor.classifier.LimeFlowTable;
import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.message.FVFlowMod;
import org.flowvisor.message.actions.FVActionDataLayerDestination;
import org.flowvisor.message.actions.FVActionDataLayerSource;
import org.flowvisor.message.actions.FVActionOutput;
import org.flowvisor.message.actions.FVActionVirtualLanIdentifier;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionStripVirtualLan;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;



/**
 * @author Murad Kaplan, Michael Coughlin
 *
 */
public final class LimeMigrationHandler {
	private int cloneSwitchCounter;
	private static LimeMigrationHandler singleInstance = null;
	private boolean migrating;
	private HashMap<DPID, List<FVFlowMod>> originalFlowMods;
	private HashMap<DPID, List<FVFlowMod>> vlanHandlerMods;
	private HashMap<DPID, DPID> localOriginalToCloneSwitchMap;
	private short vlanCounter;
	//maps vlan tag numbers to migration flow mod information
	private HashMap<Short, LimeVlanTranslationInfo> vlanTranslationMap;
	private ArrayList<LimeHost> migratedHosts;
	
	private LimeMigrationHandler(){
		cloneSwitchCounter = 0;
		vlanCounter = 0;
		migrating = false;
		originalFlowMods = new HashMap<DPID, List<FVFlowMod>>();
		vlanHandlerMods = new HashMap<DPID, List<FVFlowMod>>();
		localOriginalToCloneSwitchMap = new HashMap<DPID, DPID>();
		vlanTranslationMap = new HashMap<Short, LimeVlanTranslationInfo>();
		migratedHosts = new ArrayList<LimeHost>();
	}
	
	public static LimeMigrationHandler getInstance(){
		if(singleInstance == null){
			singleInstance = new LimeMigrationHandler();
		}
		return singleInstance;
	}
	
	
	/**
	 * Starts the migration process. Order of migration:
	 * 
	 * 1. Provide a new virtual topology to LIME
	 * 2. Request new virtual topology
	 * 	1. Create/add new virtual topology in OVX
	 * 	2.  Create necessary tunnels in OVS physical plane for ghost ports
	 * 3. Clone flow tables to clone switches with vlan tags
	 * 4. Perform physical migration of VMs
	 * 5. CLean up old network
	 * 
	 * this method is called before vms are migrated
	 * 
	 * @throws InterruptedException
	 */
	public void init() throws InterruptedException{ 
		// TODO create LIME exception of missing ports or switches
		//TODO: drop flows from internal data structure when a new flow comes in with the same match
		System.out.println("MURAD: LimeMigration, initializing migration process");
		for(Long activeSwID : LimeContainer.getActiveToOriginalSwitchMap().keySet()){
			WorkerSwitch currentSwitch = LimeContainer.getAllWorkingSwitches().get(activeSwID);
			ArrayList<FVFlowMod> currentList = new ArrayList<FVFlowMod>();
			for(FVFlowMod flowMod : currentSwitch.getFlowTable().getFlowTable()){
				if(isValidFlowModWithoutVlan(flowMod)){
					currentList.add(flowMod);
				}
			}
			DPID current = new DPID(activeSwID);
			originalFlowMods.put(current, currentList);
		}

		for(Long activeSwID : LimeContainer.getActiveToCloneSwitchMap().keySet()){
			Long cloneSwID = LimeContainer.getActiveToCloneSwitchMap().get(activeSwID);
			boolean activeSwitchIsWorking = LimeContainer.getAllWorkingSwitches().containsKey(activeSwID);
			boolean cloneSwitchIsWorking = LimeContainer.getAllWorkingSwitches().containsKey(cloneSwID);
			if(activeSwitchIsWorking && cloneSwitchIsWorking){
				WorkerSwitch activeSwitch = LimeContainer.getAllWorkingSwitches().get(activeSwID);
				WorkerSwitch cloneSwitch = LimeContainer.getAllWorkingSwitches().get(cloneSwID);
				
//				print flow table of active switch:
				LimeFlowTable ft = activeSwitch.getFlowTable();
				System.out.println("Table dump: "+ft.dump());

				boolean portMissing = false;
				short ghostPort = cloneSwitch.getGhostPort();
				HashMap<Short, PortInfo> clonePortTable = LimeContainer.getCloneSwitchContainer().get(cloneSwID).getPortTable();
				cloneSwitch.setActivePorts(clonePortTable);
				int emptyPortsCounter = 0;
				for (Short portNumber : clonePortTable.keySet()){
					PortInfo pInfo = clonePortTable.get(portNumber);
					if(activeSwitch.getActivePorts().containsKey(portNumber)){   //TODO need to check if clone switch has all the required ports
						if(!activeSwitch.getActivePorts().get(portNumber).getType().equals(PortType.H_CONNECTED)){// we don't want to change these to empty ports
							//&&!activeSwitch.getActivePorts().get(portNo).getType().equals(PortType.SW_CONNECTED)){ 
							activeSwitch.getActivePorts().get(portNumber).setType(pInfo.getType());
						}
						//TODO: what is the purpose of this line?
						cloneSwitch.getActivePorts().get(portNumber).setType(pInfo.getType());
						
						if(pInfo.getType().equals(PortType.EMPTY)){
							emptyPortsCounter ++;
						}
						
					}
					else{
						portMissing = true;
						System.out.println("MURAD: LimeMigrationHandler, ERROR, port " + portNumber+ " is not found for aSW " + activeSwID + " or cSW " + cloneSwID);
						break;
					}
				}
				cloneSwitch.setConnectedHostCounter(emptyPortsCounter);

				System.out.println("portMissing variable: "+portMissing);
				System.out.println("ghostPort variable: "+ghostPort);
				
				if (!portMissing && ghostPort != -1){
					// setup active switch
					activeSwitch.setDuplicateSwitch(cloneSwitch);
					// setup clone switch
					cloneSwitch.setDuplicateSwitch(activeSwitch);
					// copy FlowMod table from active to switch and push it the switch
					System.out.println("\n\n\nOriginal switch dpid: "+new DPID(activeSwitch.getDPID()).getDpidHexString());
					System.out.println("\n\n\nClone switch dpid: "+new DPID(cloneSwitch.getDPID()).getDpidHexString());
					System.out.println("Starting flowmod migration function");
					//copy mac address mapping from originalSwitch entries to new cloneSwitch entries
					HashMap<Short, String> originalMappings = LimeContainer.getDpidToMacMap().get(new DPID(activeSwitch.getDPID()));
					LimeContainer.getDpidToMacMap().put(new DPID(cloneSwitch.getDPID()), originalMappings);
					setupHandlerModsOriginalToClone(activeSwitch, cloneSwitch);
				}
				else{
					System.out.println("MURAD: ERROR finding port!!");
					return;
				}

			}
			else{
				System.out.println("MURAD: ERROR finding Active to Clone switches!!!!!!!!!!: " + activeSwID + " " + cloneSwID);
				return;
			}
		}
		System.out.println("Initialization was successful..");
		migrating = true;
	}
	
	/**
	 * check if a flow has a valid output action and does not contain a stripvlanaction or virtuallanidentifier action
	 * @param flowMod
	 * @return
	 */
	private boolean isValidFlowModWithoutVlan(FVFlowMod flowMod) {
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

	/**
	 * this method writes vlan handler and receiver mods to the switches to handle ALL flow mods
	 * in a given original switch - clone switch pair
	 * this method uses the original flow mods object to determine the mods to write
	 * @param activeSwitch
	 * @param cloneSwitch
	 */
	private void setupHandlerModsOriginalToClone(WorkerSwitch originalSwitch, WorkerSwitch cloneSwitch) {
		System.out.println("Starting flow mod migration of " + originalSwitch.getDPID() + " to "+cloneSwitch.getDPID());
		
		if(originalSwitch.getGhostPort() < 1){
			throw new IllegalArgumentException("Receiver switch ghost port cannot be -1");
		}
		if(cloneSwitch.getGhostPort() < 1){
			throw new IllegalArgumentException("Sending switch ghost port cannot be -1");
		}
		DPID originalSwitchDpid = new DPID(originalSwitch.getDPID());
		DPID cloneSwitchDpid = new DPID(cloneSwitch.getDPID());
		
		List<FVFlowMod> flowMods = originalFlowMods.get(originalSwitchDpid);
		
		for(FVFlowMod flowMod : flowMods){
			//create vlan sending and receivng mods for each flos mod
			//the receiving switch in this case is the original switch
			//the sending switch is the clone switch;
			createVlanHandlers(flowMod, cloneSwitchDpid, originalSwitchDpid, true);
		}
		
	}

	/**
	 * This will use libvirt to migrate the vms. Will need to have access rights for libvirt
	 * to the hosts, the ip address of the libvirt hosts/hypervisors, the destination libvirt host/hypervisor ip, and the libvirt names of the hosts
	 */
	public boolean migrateVM(LimeHost host){
		System.out.println("In migrate vm function");
		boolean migrated = LimeVMMigrater.liveMigrateQemuVM(host.getOriginalHost(), host.getDestinationHost(), host.getLibvirtDomain());
		System.out.println("migrate vm function returned");
		if(migrated){
			//recheck ALL flow mods to make sure that they point to VMs that are on the correct switches
			//create reverse rule. need to have the connected port
			WorkerSwitch originalSwitch = LimeContainer.getAllWorkingSwitches().get(host.getOriginalDpid().getDpidLong());
			WorkerSwitch cloneSwitch = LimeContainer.getAllWorkingSwitches().get(host.getCloneDpid().getDpidLong());
			
			ArrayList<FVFlowMod> matchingMods = new ArrayList<FVFlowMod>();
			DPID originalDpid = null;
			for(DPID dpid : originalFlowMods.keySet()){
				if(dpid.getDpidLong().longValue() == host.getOriginalDpid().getDpidLong().longValue()){
					originalDpid = dpid;
				}
			}
			
			for(FVFlowMod flowMod : originalFlowMods.get(host.getOriginalDpid())){
				if(LimeUtils.hasInputPortWithoutVlan(flowMod, host.getConnectedPort())){
					matchingMods.add(flowMod);
				}
				
			}
			
			migratedHosts.add(host);
			setupHandlerModsOriginalToClone(originalSwitch, cloneSwitch);
			createHandlerModsCloneToOriginal(host.getCloneDpid(), host.getOriginalDpid(), matchingMods);
			//instead of rechecking, just recreate all mods between the switches with the new state, as the
			//functions now take this into account
//			fixExistingMigrationMods(host);
			
			//TODO: need to delete the incorrect rules from the physical switches, but keep the rules in the lime flow table object
			for(FVFlowMod flowMod : matchingMods){
				//delete all non-vlan tag mods from the original switch, bypassing the lime flow table object, assuming this method works
				LimeUtils.deleteFlowMod(originalSwitch, flowMod);
			}
			
			return true;
		}
		return false;
	}

	/**
	 * iterate through existing migration flow mods and modify those that output to a host that is being migrated
	 * @param host
	 */
	private void fixExistingMigrationMods(LimeHost host) {
		//TODO: for now, we still assume that host are connected to the same ports in the original and source switches
		short currentlyMigratingPort = host.getConnectedPort();
		for(Short vlanTag : vlanTranslationMap.keySet()){
			LimeVlanTranslationInfo info = vlanTranslationMap.get(vlanTag);
			short currentReceiverOutputPort = LimeUtils.getFlowModOutputPort(info.getReceiverMod());
			
			//if is a mod from clone to original and is  and the output of the mod points to the host currently being migrated
			//and the receiver mod is on the same switch as the host was on
			if(!info.isOriginalToClone() && currentReceiverOutputPort == currentlyMigratingPort && info.getReceiverSwitch().equals(host.getOriginalDpid())){
				//change sending mod to instead output to the connected port on the clone(sending) switch and remove vlan action
				FVFlowMod sendingMod = info.getSenderMod();
				List<OFAction> actions = sendingMod.getActions();
				List<OFAction> actionsToDelete = new ArrayList<OFAction>();
				
				//find all vlan actions. there should only be one
				//modify action output to output to the host's connected port, rather than the ghost port at the same time (should also be only one mod)
				boolean foundOutput = false;
				for(OFAction action : actions){
					if(action instanceof OFActionVirtualLanIdentifier){
						actionsToDelete.add(action);
					}
					if(!foundOutput && action instanceof OFActionOutput){
						((OFActionOutput)action).setPort(currentlyMigratingPort);
						foundOutput = true;
					}
				}
				
				//remove all found vlan actions
				actions.removeAll(actionsToDelete);
				
				//re-write the mod to the clone switch
				sendingMod.computeLength();
				WorkerSwitch cloneSwitch = LimeContainer.getAllWorkingSwitches().get(info.getSenderSwitch().getDpidLong());
				LimeUtils.sendFlowMod(sendingMod, cloneSwitch);
				
				//also delete receiver mod on the original switch, but no neccessary
				WorkerSwitch originalSwitch = LimeContainer.getAllWorkingSwitches().get(info.getReceiverSwitch().getDpidLong());
				LimeUtils.deleteFlowMod(originalSwitch, info.getReceiverMod());
				
				//also change entry in translation table
				LimeVlanTranslationInfo newInfo = LimeUtils.cloneTranslationInfoMigrated(info, true);
				vlanTranslationMap.put(vlanTag, newInfo);
			}
		}
	}
	
	/**
	 * create migration flow mods that handle networking that goes from already migrated hosts to hosts that are not migrated from the original
	 * to the clone switch
	 * @param cloneSwitch
	 * @param originalSwitch
	 * @param matchingMods
	 */
	private void createHandlerModsCloneToOriginal(DPID cloneSwitch, DPID originalSwitch, ArrayList<FVFlowMod> matchingMods) {
		for(FVFlowMod flowMod : matchingMods){
			//need to iterate over each output port of the flow mod
			boolean allMigrated = true;
			for(OFAction action : flowMod.getActions()){
				if(action instanceof OFActionOutput){
					short currentOutputPort = ((OFActionOutput) action).getPort();
					//this if statement should only occur if ALL of the output ports have been migrated
					if(!LimeUtils.outputPortMigrated(originalSwitch, flowMod, currentOutputPort, migratedHosts)){
						allMigrated = false;
					}
				}
			}
			
			if(allMigrated){
				WorkerSwitch cloneSwitchObj = LimeContainer.getAllWorkingSwitches().get(cloneSwitch.getDpidLong());
				WorkerSwitch originalSwitchObj = LimeContainer.getAllWorkingSwitches().get(originalSwitch.getDpidLong());
				LimeUtils.sendFlowMod(flowMod, cloneSwitchObj);
				//TODO: create mod that matches the vlan assigned for this mod, if one exists (possibly, there is not a pair of mods)
//				//on original switch, replace mod that matches this vlan with one that sends back to the clone
//				//then, match on this vlan in the clone and perform the output action
//				short vlanTag = allocateVlanForMod(flowMod);
//				//TODO: need to manually construct flow mods here
//				createAndSendVlanSenderMod(vlanTag, , flowMod, senderSwitchObject)
			}else{
				//recreate the flow mods going in the other direction with the updated state
//				createVlanHandlers(flowMod, originalSwitch, cloneSwitch, false);
				createVlanHandlers(flowMod, originalSwitch, cloneSwitch, false);
			}
		}
		
	}
	
	/**
	 * signal that a particular switch is done migrating
	 * @param activeSwitchDpid
	 */
	public synchronized void switchDoneMigrating(DPID activeSwitchDpid){
		WorkerSwitch activeSwitch = LimeContainer.getAllWorkingSwitches().get(activeSwitchDpid.getDpidLong());
		WorkerSwitch cloneSwitch = activeSwitch.getDuplicateSwitch();
		cloneSwitchCounter ++;
		
		// edit original switch to map to this clone switch now
		activeSwitch.getOriginalSwitchByName(LimeContainer.OriginalSwitch).setActiveSwitch(cloneSwitch);
		activeSwitch.tearDownOriginalSwitch(LimeContainer.OriginalSwitch);
		
		// make activeSwitch true for clone switch
		cloneSwitch.makeActive();
		
		// clear duplicate switch field
		cloneSwitch.setDuplicateSwitch(null);
		
		// change mapping from active to original switch in lime container
		long oSwID = LimeContainer.getActiveToOriginalSwitchMap().get(activeSwitch.getDPID());
		LimeContainer.getActiveToOriginalSwitchMap().remove(activeSwitch.getDPID());
		LimeContainer.getActiveToOriginalSwitchMap().put(cloneSwitch.getDPID(), oSwID);
		localOriginalToCloneSwitchMap.put(new DPID(activeSwitch.getDPID()), new DPID(cloneSwitch.getDPID()));
		
		// remove ghost port
		// this will happen automatically when OVX remove port
		
		// check switch counter
		if (cloneSwitchCounter == LimeContainer.getCloneSwitchContainer().size()){
			// send to ovx to delete all old active switches
			LimeContainer.getCloneSwitchContainer().clear();
			LimeContainer.getActiveToCloneSwitchMap().clear();
			//TODO: need to delete all vlan mods in clone switch
			//TODO: fix this so that is uses the vlan map, rather than the handler mods map
			for(DPID switchDPID : vlanHandlerMods.keySet()){
				for(FVFlowMod flowMod : vlanHandlerMods.get(switchDPID)){
					WorkerSwitch currentSwitch = LimeContainer.getAllWorkingSwitches().get(switchDPID.getDpidLong());
					LimeUtils.deleteFlowMod(currentSwitch, flowMod);
				}
			}
			
			//TODO: need to put correct flow tables into the new switches
			for(DPID originalSwitch : localOriginalToCloneSwitchMap.keySet()){
				List<FVFlowMod> originalFlows = originalFlowMods.get(originalSwitch);
				for(FVFlowMod flowMod : originalFlows){
					DPID currentCloneDPID = localOriginalToCloneSwitchMap.get(originalSwitch);
					WorkerSwitch currentClone = LimeContainer.getAllWorkingSwitches().get(currentCloneDPID.getDpidLong());
					currentClone.handleFlowModAndSend(flowMod);
				}
			}
			migrating = false;
		}
	}

	/**
	 * migrate a VM in a separate thread
	 * @param currentHost
	 */
	public void migrateVMAsynchronously(final LimeHost currentHost) {
		Runnable migrateVMTask = new Runnable(){
			@Override
			public void run() {
				migrateVM(currentHost);
			}
		};
		Thread migrateVMThread = new Thread(migrateVMTask);
		migrateVMThread.start();
	}
	
	/**
	 * check if the migration flag has been set
	 * @return
	 */
	public boolean isMigrating(){
		return migrating;
	}
	
	/**
	 * record a flow mod in the migration handler data structure
	 * that creates a vlan handler mod and a vlan handler mod.
	 * it also
	 * @param originalMod
	 * @return
	 */
	public LimeVlanTranslationInfo createVlanHandlers(FVFlowMod originalMod, DPID receiverSwitch, DPID senderSwitch, boolean originalToClone){
		WorkerSwitch senderSwitchObject = LimeContainer.getAllWorkingSwitches().get(senderSwitch.getDpidLong());
		WorkerSwitch receiverSwitchObject = LimeContainer.getAllWorkingSwitches().get(receiverSwitch.getDpidLong());
		short receiverGhostPort = receiverSwitchObject.getGhostPort();
		short senderGhostPort = senderSwitchObject.getGhostPort();
		short currentVlan = allocateVlanForMod(originalMod);
		
		FVFlowMod senderVlanMod = createAndSendVlanSenderMod(currentVlan, senderGhostPort, originalMod, senderSwitchObject, originalToClone);
		FVFlowMod receiverVlanMod = createAndSendVlanReceiverMod(currentVlan, receiverGhostPort, originalMod, receiverSwitchObject, originalToClone);
		LimeVlanTranslationInfo currentInfo = new LimeVlanTranslationInfo(receiverVlanMod, senderVlanMod, receiverSwitch, senderSwitch, originalToClone, originalMod, currentVlan, false);
		vlanTranslationMap.put(currentVlan, currentInfo);
		LimeUtils.addToVlanMap(senderSwitch, senderVlanMod, vlanHandlerMods);
		LimeUtils.addToVlanMap(receiverSwitch, receiverVlanMod, vlanHandlerMods);
		return currentInfo;
	}
	
	/**
	 * create a migration flow mod that receives from the ghost port on a particular switch and performs the action of the original mod
	 * this depends on the state of which hosts has been migrated. will only forward to hosts that exist on this switch 
	 * @param vlanNumber
	 * @param ghostPort
	 * @param originalMod
	 * @param receiverSwitchObject
	 * @return
	 */
	private FVFlowMod createAndSendVlanReceiverMod(short vlanNumber, short ghostPort, FVFlowMod originalMod, WorkerSwitch receiverSwitchObject, boolean receivingFromOriginal) {
		// based off of org.flowvisor.mesage.FVPacketIn.sendDropRule()
		FVFlowMod clonedMod = (FVFlowMod) originalMod.clone();

		//create match to match packets coming in ghostPort for a particular vlan
		FVMatch match = new FVMatch();
		int wildcards = FVMatch.OFPFW_ALL;
		//do not need to match on input port if matching on vlan
//		wildcards &= ~FVMatch.OFPFW_IN_PORT;
		wildcards &= ~FVMatch.OFPFW_DL_VLAN;
		match.setDataLayerVirtualLan(vlanNumber);
//		match.setDataLayerDestination(LimeUtils.convertMacToBytes(destMac));
//		match.setDataLayerSource(LimeUtils.convertMacToBytes(srcMac));
		//need to set input port or ovx has a nullpointerexception. is this part of openflow spec?
		//if wildcarding input port, try setting to -1?
//		match.setInputPort(originalMod.getMatch().getInputPort());
		//for now, too tedious to avoid this. could use a dummy port on each switch as the input port in future
//		match.setInputPort(ghostPort);
		match.setInputPort((short)0);
		
		//NOTE: see openvirtex.messages.actions.OVXActionOutput.java line 171 else statement:
		//if the input port is an edge, e.g. is a link to another switch, like the ghost ports are,
		//then ovx attempts to get the dl_src and dl_dst of the mods, which will not exist if they 
		//are wildcarded-> cannot set to input port, as this causes ovx to set the action as IN_PORT
		match.setWildcards(wildcards);
		clonedMod.setMatch(match);
		
		//TODO: need to support multiple output ports now
		//if am receiving from original, need to output to only ports where a host has been migrated to
		//if am receiving from clone, need to only output to ports that have a host still attached, e.g. have not been migrated
		//	this works, as the sender mod will only send packets to the ghost port if the output port is on the remote switch
		//	e.g. the output port has been migrated to this switch, or the output port is not connected to a host
		//build a table of actionoutputs that are not needed
		//also use this loop to update the flow mod to have the set_dl_dst and set_dl_src actions. the mod should then be ready to be written
		ArrayList<OFActionOutput> unneededActions = new ArrayList<OFActionOutput>();
		ArrayList<OFAction> originalActions = new ArrayList<OFAction>(clonedMod.getActions());
		for(OFAction action : originalActions){
			if(action instanceof OFActionOutput){
				short currentOutputPort = ((OFActionOutput) action).getPort();
//				clonedMod.getMatch().setInputPort(currentOutputPort);
				FVActionDataLayerSource mod_dl_src = new FVActionDataLayerSource();
				FVActionDataLayerDestination mod_dl_dst = new FVActionDataLayerDestination();
				String srcMac = null;
				String destMac = null;
				
				if(receivingFromOriginal){//am the clone switch
					WorkerSwitch originalSwitch = receiverSwitchObject.getDuplicateSwitch();
					PortType portType = LimeContainer.getDpidToPortInfoMap().get(new DPID(originalSwitch.getDPID())).get(currentOutputPort).getType(); 
					
					//if are an unneeded output, you are an H_CONNECTED port that HAS NOT been migrated (as this is the clone switch)
					if(portType == PortType.H_CONNECTED && !LimeUtils.outputPortMigrated(new DPID(originalSwitch.getDPID()), originalMod, currentOutputPort, migratedHosts)){
						unneededActions.add((OFActionOutput) action);
					} else if(portType == PortType.H_CONNECTED){
						try {
							srcMac = LimeUtils.getMacForPort(new DPID(originalSwitch.getDPID()), originalMod.getMatch().getInputPort(), LimeContainer.getDpidToMacMap());
							destMac = LimeUtils.getMacForPort(new DPID(originalSwitch.getDPID()), currentOutputPort, LimeContainer.getDpidToMacMap());
							mod_dl_src.setDataLayerAddress(LimeUtils.convertMacToBytes(srcMac));
							mod_dl_dst.setDataLayerAddress(LimeUtils.convertMacToBytes(destMac));
						} catch (MacLookupException e) {
							//This should not happen. It means we don't have a mac address for this port, and so a host is not connected
							//ignore and treat as if the else if condition was not met
						}
					}
				} else{
					WorkerSwitch cloneSwitch = receiverSwitchObject.getDuplicateSwitch();
					PortType portType = LimeContainer.getDpidToPortInfoMap().get(new DPID(receiverSwitchObject.getDPID())).get(currentOutputPort).getType();
					
					//if are an unneeded output, you are an H_CONNECTED port that HAS been migrated (as this switch is the original switch)
					if(portType == PortType.H_CONNECTED && LimeUtils.outputPortMigrated(new DPID(receiverSwitchObject.getDPID()), originalMod, currentOutputPort, migratedHosts)){
						unneededActions.add((OFActionOutput) action);
					}else if(portType == PortType.H_CONNECTED){
						try {
							srcMac = LimeUtils.getMacForPort(new DPID(receiverSwitchObject.getDPID()), originalMod.getMatch().getInputPort(), LimeContainer.getDpidToMacMap());
							destMac = LimeUtils.getMacForPort(new DPID(receiverSwitchObject.getDPID()), currentOutputPort, LimeContainer.getDpidToMacMap());
							mod_dl_src.setDataLayerAddress(LimeUtils.convertMacToBytes(srcMac));
							mod_dl_dst.setDataLayerAddress(LimeUtils.convertMacToBytes(destMac));
						} catch (MacLookupException e) {
							//This should not happen. It means we don't have a mac address for this port, and so a host is not connected
							//ignore and treat as if the else if condition was not met
						}
						
					}
				}
				
				if(srcMac != null && destMac != null){
					clonedMod.getActions().remove(action);
					clonedMod.getActions().add(action);
					OFActionStripVirtualLan stripVlan = (OFActionStripVirtualLan) FlowVisor.getInstance().getFactory().getAction(OFActionType.STRIP_VLAN);
					int actionIndex = clonedMod.getActions().indexOf(action);
					clonedMod.getActions().add(actionIndex, stripVlan);
					clonedMod.getActions().add(actionIndex, mod_dl_src);
					clonedMod.getActions().add(actionIndex, mod_dl_dst);
				}
			}
		}
		
		clonedMod.getActions().removeAll(unneededActions);
		clonedMod.computeLength();
		
		System.out.println("Actions for this mod:");
		for(OFAction action : clonedMod.getActions()){
			System.out.println(action);
		}
		
		LimeUtils.sendFlowMod(clonedMod, receiverSwitchObject);
		return clonedMod;
	}

	
	/**
	 * create a migration flow mod that sends from one switch to another using the ghost port, based on the original flow mod
	 * relies on stored state of which hosts have been migrated. will forward out the ghost port with a vlan tag if there is a
	 * host that has been migrated to the clone switch. if there are hosts that have are on the current switch and are output
	 * actions, then will forward to them without adding a vlan tag
	 * @param vlanTagNumber
	 * @param ghostPort
	 * @param flowMod
	 * @param senderSwitchObject
	 * @return
	 */
	private FVFlowMod createAndSendVlanSenderMod(short vlanTagNumber, short ghostPort, FVFlowMod flowMod, WorkerSwitch senderSwitchObject, boolean originalToClone) {
		FVFlowMod clonedMod = (FVFlowMod) flowMod.clone();
		System.out.println("\nCurrent flowmod: "+flowMod+"\n");
		short vlanNumber = vlanTagNumber;
		
		boolean setMatchMacs = false;
		ArrayList<OFActionOutput> localPortsActions = new ArrayList<OFActionOutput>();
		ArrayList<OFActionOutput> remotePortsActions = new ArrayList<OFActionOutput>();
		ArrayList<OFActionOutput> unneededMods = new ArrayList<OFActionOutput>();
		
		//set the wildcards of the new OF mod
		OFMatch match = clonedMod.getMatch();
		int wildcards = match.getWildcards();
		match.setWildcards(wildcards);
		clonedMod.setMatch(match);
		
		for(int i = 0; i<clonedMod.getActions().size(); i++ ){
			OFAction action = clonedMod.getActions().get(i);
			if(action instanceof OFActionOutput){
				//using macs of the first output action to store in ovx's virtualization table
				//the receiver mod should manually reset 
				if(!setMatchMacs){
					String destMac;
					try {
						destMac = LimeUtils.getMacForPort(new DPID(senderSwitchObject.getDPID()), ((OFActionOutput) action).getPort(), LimeContainer.getDpidToMacMap());
						String srcMac = LimeUtils.getMacForPort(new DPID(senderSwitchObject.getDPID()), flowMod.getMatch().getInputPort(), LimeContainer.getDpidToMacMap());
//						clonedMod.getMatch().setDataLayerSource(LimeUtils.convertMacToBytes(srcMac));
//						clonedMod.getMatch().setDataLayerDestination(LimeUtils.convertMacToBytes(destMac));
						setMatchMacs = true;
					} catch (MacLookupException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				short currentOutputPort = ((OFActionOutput)action).getPort();
				//only need to virtualize migrated H_CONNECTED ports for now
				//don't know if sending switch is clone or original: need to output unmodified if a host
				//is connected locally, else send over ghost port
				if(originalToClone){
					//THIS logic works if are the original switch sending:
					PortType portType = LimeContainer.getDpidToPortInfoMap().get(new DPID(senderSwitchObject.getDPID())).get(currentOutputPort).getType(); 
					if(portType != PortType.H_CONNECTED || LimeUtils.outputPortMigrated(new DPID(senderSwitchObject.getDPID()), flowMod, currentOutputPort, migratedHosts)){ //if a host was connected here but has been migrated, or something other than a host is supposed to be here
						remotePortsActions.add((OFActionOutput) action);
					} else if(portType == PortType.H_CONNECTED){ //a host is supposed to be connected here and has not been migrated
						localPortsActions.add((OFActionOutput) action);
					}
				} else{
					//if are the clone switch, must do the opposite of the last if:
					//need to have the remote ports be ports that are still connected to the original switch,
					//the local ports are hosts that have already been migrated
					//can get this object directly, as it set by init()
					WorkerSwitch originalSwitch = senderSwitchObject.getDuplicateSwitch();
					PortType portType = LimeContainer.getDpidToPortInfoMap().get(new DPID(originalSwitch.getDPID())).get(currentOutputPort).getType(); 
					if(LimeUtils.outputPortMigrated(new DPID(originalSwitch.getDPID()), flowMod, currentOutputPort, migratedHosts)){ //if a host was connected here but has been migrated
						localPortsActions.add((OFActionOutput) action);
					} else if(portType == PortType.H_CONNECTED){ //a host is supposed to connected here and has not been migrated
						remotePortsActions.add((OFActionOutput) action);
					}
				}
			}
		}
		
		boolean ghostPortRuleWritten = false;
		for(OFActionOutput migratedAction : remotePortsActions){
			//need to make these actions last in the list for now, or else the mod will put vlan on all packets
			clonedMod.getActions().remove(migratedAction);
			if(!ghostPortRuleWritten){
				clonedMod.getActions().add(migratedAction);
				migratedAction.setPort(ghostPort);
				//create vlan tag action
				FVActionVirtualLanIdentifier addedVlanAction = new FVActionVirtualLanIdentifier();
				addedVlanAction.setVirtualLanIdentifier(vlanNumber);
				//add vlan tag action to mod
				//insert vlan action before the output action in the action list
				int actionIndex = clonedMod.getActions().indexOf(migratedAction);
//				clonedMod.getActions().add(actionIndex, addedVlanAction);
				//only need to output out ghost port once
			}
		}
		
		clonedMod.computeLength();
		
		System.out.println("Flow after modification: "+clonedMod);
		LimeUtils.sendFlowMod(clonedMod, senderSwitchObject);
		return clonedMod;
	}

	/**
	 * this method looks up an existing vlan tag number for a mod
	 * or creates a new one if no tag exists in the data structure
	 * 
	 * is synchronized so that potential conccurrent calls do not get 
	 * inconsistent tag numbers
	 * 
	 * @param flowMod TODO
	 * @return the vlan tag # that should be used for this mod
	 */
	private synchronized short allocateVlanForMod(FVFlowMod flowMod) {
		//TODO: refactor so that a vlan tag number is generated by the tags
		//in use in the data structure, rather than in the global variable
		for(short tag : vlanTranslationMap.keySet()){
			LimeVlanTranslationInfo info = vlanTranslationMap.get(tag);
			FVFlowMod current = info.getOriginalMod();
			if(flowMod.equals(current)){
				return tag;
			}
		}
		vlanCounter++;
		return vlanCounter;
	}
}