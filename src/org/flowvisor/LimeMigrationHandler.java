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
import org.flowvisor.message.FVPortMod;
import org.flowvisor.message.actions.FVActionOutput;
import org.flowvisor.message.actions.FVActionStripVirtualLan;
import org.flowvisor.message.actions.FVActionVirtualLanIdentifier;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionStripVirtualLan;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;
import org.openflow.util.U16;



/**
 * @author Murad Kaplan
 *
 */
public final class LimeMigrationHandler {
	private int cloneSwitchCounter;
	private static LimeMigrationHandler singleInstance = null;
	private boolean migrating;
	private HashMap<DPID, List<FVFlowMod>> originalFlowMods;
	private HashMap<DPID, List<FVFlowMod>> vlanHandlerMods;
	private HashMap<DPID, DPID> localCloneToSwitchMap;
	private short vlanCounter;
	private HashMap<Short, LimeVlanTranslationInfo> vlanTranslationMap;
	
	private LimeMigrationHandler(){
		cloneSwitchCounter = 0;
		vlanCounter = 0;
		migrating = false;
		originalFlowMods = new HashMap<DPID, List<FVFlowMod>>();
		vlanHandlerMods = new HashMap<DPID, List<FVFlowMod>>();
		localCloneToSwitchMap = new HashMap<DPID, DPID>();
		vlanTranslationMap = new HashMap<Short, LimeVlanTranslationInfo>();
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
	public void init() throws InterruptedException{ // TODO create LIME exception of missing ports or switches
		//TODO: need to specify topology here based on what is received in the LimeServer
		System.out.println("MURAD: LimeMigration, inititlizing migration process");
		// this should be received from operator, for testing, we just assume that we have them

		//TODO the below code of setting up clone switches and their ports should not be hard coded but rather received from outside
		// top switch only has two ports and they are connected to switches (SW_CONNECTED)
//		HashMap<Short, PortInfo> portTable1 = new HashMap<>();
//		portTable1.put((short) 1, new PortInfo(PortType.EMPTY, null, null));
//		portTable1.put((short) 2, new PortInfo(PortType.EMPTY, null, null));
//		portTable1.put((short) 3, new PortInfo(PortType.GHOST, null, null));
		/*LimeContainer.addCloneSwitch(46200400562356228L, portTable1);
		LimeContainer.insertActiveToCloneSwitchMap(46200400562356228L-3, 46200400562356228L);
		System.out.println("MURAD: Clone Top-level Switch: " + 46200400562356228L);*/
//		LimeContainer.addCloneSwitch(512, portTable1);
//		LimeContainer.insertActiveToCloneSwitchMap(256, 512);
//		System.out.println("MURAD: Clone Top-level Switch: " + 512);
		
		//for(long j=46200400562356229L; j<46200400562356231L; j++){
		/*for(long j=1280; j<1537; j=j+256){
			portTable1 = new HashMap<>();
			for(short i= 1; i<3; i++){
				portTable1.put(i, new PortInfo(PortType.EMPTY, null, null));
			}	
			portTable1.put((short) 3, new PortInfo(PortType.SW_CONNECTED, null, null));
			portTable1.put((short) 4, new PortInfo(PortType.GHOST, null, null));
			LimeContainer.addCloneSwitch(j, portTable1);
			LimeContainer.insertActiveToCloneSwitchMap(j-768, j);
			System.out.println("MURAD: Clone Second-level Switch: " + j);
		}*/

		// loop through classifier to make sure that all needed switches (active/cloned) are there
		// make sure that all required ports are there from active-port table in active classifier 
		// clone switches should not be connected to any VMs!!

		// just loop and make sure port number is there, if so, change its type to what its in clone one
		// don't change CONNECTED switch, trigger error!
		// add rules for both active and clone switches to handle packets in and out ghost ports
		// For any packet coming from port G 
		// 		remove vlanid 
		// 		forward to port = to vlanid

//		boolean firstSuccessful = LimeVMMigrater.liveMigrateQemuVM("128.138.189.249", "128.138.189.140", "ubuntu-int1");
//		boolean secondSuccessful = LimeVMMigrater.liveMigrateQemuVM("128.138.189.249", "128.138.189.140", "ubuntu-int2");
//		
//		if(!firstSuccessful){
//			System.out.println("MICHAEL: Error: migration of ubuntu-int1 not successful");
//		}
//		
//		if(!secondSuccessful){
//			System.out.println("MICHAEL: Error: migration of ubuntu-int2 not successful");
//		}
//		
//		if(!firstSuccessful || !secondSuccessful){
//			return;
//		}
		//populate original flow table map with all the flows in all of the active switches
		for(Long activeSwID : LimeContainer.getActiveToOriginalSwitchMap().keySet()){
			WorkerSwitch currentSwitch = LimeContainer.getAllWorkingSwitches().get(activeSwID);
			ArrayList<FVFlowMod> currentList = new ArrayList<FVFlowMod>();
			for(FVFlowMod flowMod : currentSwitch.getFlowTable().getFlowTable()){
				if(isValidFlowModWithoutVlan(flowMod)){
					currentList.add(flowMod);
				}
			}
			originalFlowMods.put(new DPID(activeSwID), currentList);
		}

//		for (Map.Entry<Long, Long> entry : LimeContainer.getActiveToCloneSwitchMap().entrySet()) {
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
				//TODO: this method should work or it should not be in WorkerSwitch
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
//						if(pInfo.getType().equals(PortType.GHOST)){
//							ghostPort = portNumber; // this should only happens once since we only have one ghost port
//						}
						
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
				
				//skip check for now:
//				portMissing = false;
				if (!portMissing && ghostPort != -1){
					// setup active switch
					activeSwitch.setDuplicateSwitch(cloneSwitch);
					// setup clone switch
					cloneSwitch.setDuplicateSwitch(activeSwitch);
					// copy FlowMod table from active to switch and push it the switch
					System.out.println("\n\n\nOriginal switch dpid: "+new DPID(activeSwitch.getDPID()).getDpidHexString());
					System.out.println("\n\n\nClone switch dpid: "+new DPID(cloneSwitch.getDPID()).getDpidHexString());
					System.out.println("Starting flowmod migration function");
					//TODO: need to keep track of original rules, as they may be modified/removed by ovx and
					//the current lime data structures might not work
//					WorkerSwitch.insertFlowRuleTableAndSendModified(activeSwitch, cloneSwitch, activeSwitch.getFlowTable().getFlowTable(), vlanHandlerMods);  //FIXME we may need to clone this
					setupHandlerModsOriginalToClone(activeSwitch, cloneSwitch);
					
//					switchDoneMigrating(cloneSwitch, activeSwitch);
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
			createVlanHandlers(flowMod, cloneSwitchDpid, originalSwitchDpid);
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
			//create reverse rule. need to have the connected port
			WorkerSwitch originalSwitch = LimeContainer.getAllWorkingSwitches().get(host.getOriginalDpid().getDpidLong());
			WorkerSwitch cloneSwitch = LimeContainer.getAllWorkingSwitches().get(host.getCloneDpid().getDpidLong());
			
			
			//TODO: this logic is not quite correct: this will create a vlan tag to send ALL of the non-vlan rules in the original switch
			//and create a handler in the clone switch. need to do so for each host individually, so if the output port is the same as the port
			//that the host is connected to. need a sub-method that does this for a port, or create a table of flow mods that only contains flow
			//mods where the output port == the port the host is connected to
			//also need to delete the non-vlan rules from the physical switch, but not from the lime flow table
			//for now, assume that the host is connected to the same port on the cloned switch as on the original switch
			ArrayList<FVFlowMod> matchingMods = new ArrayList<FVFlowMod>();
			DPID originalDpid = null;
			for(DPID dpid : originalFlowMods.keySet()){
				if(dpid.getDpidLong().longValue() == host.getOriginalDpid().getDpidLong().longValue()){
					originalDpid = dpid;
				}
			}
//			if(originalDpid != null){
//				for(FVFlowMod flowMod : originalFlowMods.get(originalDpid)){
			//TODO: the problem here is that this determines mods based on their output ports,
			//but the mods to forward the from the original switch from the output ports in the
			//original switch are already created. instead, need to determine mods based on the 
			//inport == the attached host port, as we need to intercept the packets from the host,
			//not redirect packets that are being sent
				for(FVFlowMod flowMod : originalFlowMods.get(host.getOriginalDpid())){
					if(hasInputPortWithoutVlan(flowMod, host.getConnectedPort())){
						matchingMods.add(flowMod);
					}
					
				}
//			}
			//TODO: this correctly creates sender vlan mod, but does not create correct vlan handler mod on the original switch
//			WorkerSwitch.insertFlowRuleTableAndSendModified(cloneSwitch, originalSwitch, matchingMods, vlanHandlerMods);
			createHandlerModsCloneToOriginal(host.getCloneDpid(), host.getOriginalDpid(), matchingMods);
			//TODO: need to delete the incorrect rules from the physical switches, but keep the rules in the lime flow table object
			for(FVFlowMod flowMod : matchingMods){
				//delete all non-vlan tag mods from the original switch, bypassing the lime flow table object, assuming this method works
				LimeUtils.deleteFlowMod(originalSwitch, flowMod);
			}

			return true;
		}
		return false;
	}


	

	private boolean hasInputPortWithoutVlan(FVFlowMod flowMod, Short connectedPort) {
		List<OFAction> actions = flowMod.getActions();
		for(OFAction action : actions){
			if(action instanceof OFActionVirtualLanIdentifier || action instanceof OFActionStripVirtualLan){
				return false;
			}
		}
		OFMatch match = flowMod.getMatch();
		return (match.getInputPort() == connectedPort);
	}

	private void createHandlerModsCloneToOriginal(DPID cloneSwitch, DPID originalSwitch, ArrayList<FVFlowMod> matchingMods) {
		for(FVFlowMod flowMod : matchingMods){
			createVlanHandlers(flowMod, originalSwitch, cloneSwitch);			
		}
		
	}

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
		localCloneToSwitchMap.put(new DPID(activeSwitch.getDPID()), new DPID(cloneSwitch.getDPID()));
		
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
			for(DPID originalSwitch : localCloneToSwitchMap.keySet()){
				List<FVFlowMod> originalFlows = originalFlowMods.get(originalSwitch);
				for(FVFlowMod flowMod : originalFlows){
					DPID currentCloneDPID = localCloneToSwitchMap.get(originalFlows);
					WorkerSwitch currentClone = LimeContainer.getAllWorkingSwitches().get(currentCloneDPID.getDpidLong());
					currentClone.handleFlowModAndSend(flowMod);
				}
			}
			migrating = false;
		}
	}


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
	public LimeVlanTranslationInfo createVlanHandlers(FVFlowMod originalMod, DPID receiverSwitch, DPID senderSwitch){
		WorkerSwitch senderSwitchObject = LimeContainer.getAllWorkingSwitches().get(senderSwitch.getDpidLong());
		WorkerSwitch receiverSwitchObject = LimeContainer.getAllWorkingSwitches().get(receiverSwitch.getDpidLong());
		short receiverGhostPort = receiverSwitchObject.getGhostPort();
		short senderGhostPort = senderSwitchObject.getGhostPort();
		short currentVlan = allocateVlanForMod(originalMod);
		
		FVFlowMod senderVlanMod = createAndSendVlanSenderMod(currentVlan, senderGhostPort, originalMod, senderSwitchObject);
		FVFlowMod receiverVlanMod = createAndSendVlanReceiverMod(currentVlan, receiverGhostPort, originalMod, receiverSwitchObject);
		LimeVlanTranslationInfo currentInfo = new LimeVlanTranslationInfo(receiverVlanMod, senderVlanMod, receiverSwitch, senderSwitch, true, originalMod, currentVlan);
		vlanTranslationMap.put(currentVlan, currentInfo);
		addToVlanMap(senderSwitch, senderVlanMod);
		addToVlanMap(receiverSwitch, receiverVlanMod);
		return currentInfo;
	}

	private void addToVlanMap(DPID senderSwitch, FVFlowMod senderVlanMod) {
		if(vlanHandlerMods.containsKey(senderSwitch)){
			List<FVFlowMod> flowMods = vlanHandlerMods.get(senderSwitch);
			flowMods.add(senderVlanMod);
		} else{
			ArrayList<FVFlowMod> flowMods = new ArrayList<FVFlowMod>();
			flowMods.add(senderVlanMod);
			vlanHandlerMods.put(senderSwitch, flowMods);
		}
		
	}

	private FVFlowMod createAndSendVlanReceiverMod(short vlanNumber, short ghostPort, FVFlowMod originalMod, WorkerSwitch receiverSwitchObject) {
		// based off of org.flowvisor.mesage.FVPacketIn.sendDropRule()
				short outPort = originalMod.getOutPort();
				for(OFAction action : originalMod.getActions()){
					if(action instanceof OFActionOutput){
						if(((OFActionOutput) action).getPort() != outPort){
							outPort = ((OFActionOutput) action).getPort();
						}
					}
				}
				FVFlowMod newMod = (FVFlowMod) FlowVisor.getInstance().getFactory().getMessage(OFType.FLOW_MOD);
				//create match to match packets coming in ghostPort for a particular vlan
				FVActionStripVirtualLan stripVlan = new FVActionStripVirtualLan();
//				stripVlan.
				FVMatch match = new FVMatch();
				int wildcards = FVMatch.OFPFW_ALL;
				//TODO: cannot support using input matching for ovs, ovx and gre tunnels
//				wildcards &= ~FVMatch.OFPFW_IN_PORT;
				wildcards &= ~FVMatch.OFPFW_DL_VLAN;
				match.setDataLayerVirtualLan(vlanNumber);
//				match.setWildcards(~(FVMatch.OFPFW_DL_VLAN & -1));
				//need to set input port or ovx has a nullpointerexception. is this part of openflow spec?
				match.setInputPort(ghostPort);
				match.setWildcards(wildcards);
				
				//TODO: set the actions of this mod to be the actions of the original mod.
				//For now, use the vlan tag # as the output port of this mod
				FVActionOutput outputAction = new FVActionOutput();
				outputAction.setMaxLength((short)32767);
				//NOTE: this switch port must exist in OVX or else ovx will drop the flow mod
				outputAction.setPort(OFPort.OFPP_ALL.getValue());
				newMod.setMatch(match);
				newMod.setActions(new LinkedList<OFAction>());
				newMod.getActions().add(stripVlan);
				newMod.getActions().add(outputAction);
				newMod.setOutPort(OFPort.OFPP_ALL);
//				newMod.
				newMod.setHardTimeout((short)0);
				newMod.setIdleTimeout((short)0);
				newMod.setCommand(FVFlowMod.OFPFC_ADD);
				newMod.setCookie(originalMod.getCookie());
				//hard code priority
				newMod.setPriority((short)100);
				//need this flag?
				newMod.setFlags((short)1);
				newMod.computeLength();
//				handlerSwitch.handleFlowModAndSend(newMod, true);
				sendFlowMod(newMod, receiverSwitchObject);
				return newMod;
	}

	private FVFlowMod createAndSendVlanSenderMod(short vlanTagNumber, short ghostPort, FVFlowMod flowMod, WorkerSwitch senderSwitchObject) {
		FVFlowMod clonedMod = (FVFlowMod) flowMod.clone();
		System.out.println("\nCurrent flowmod: "+flowMod+"\n");
		short vlanNumber = vlanTagNumber;
		//Loop through each action, however at this time we only support actions with one output port
		//TODO: support multiple output ports in future
		for(int i = 0; i<clonedMod.getActions().size(); i++ ){
			OFAction action = clonedMod.getActions().get(i);
			if(action instanceof OFActionOutput){
				//TODO: i don't think that we can necessarily tell the ports this way 
//				if(senderSwitchObject.getActivePorts().containsKey(((OFActionOutput) action).getPort())){
					//TODO: I think that all flow mods need to be written to clone. The host migration process occurs later
					//and we would not be able to tell which hosts are still attached to the switch this way anyway
//					if (destinationSwitch.getActivePorts().get(((OFActionOutput) action).getPort()).getType().equals(PortType.EMPTY)){
						System.out.println("Modifying flow: "+clonedMod);
						int originalSize = clonedMod.getLengthU();		
						//create vlan tag action
//						OFActionVirtualLanIdentifier addedVlanAction = new OFActionVirtualLanIdentifier(originalPort);
						FVActionVirtualLanIdentifier addedVlanAction = new FVActionVirtualLanIdentifier();
						addedVlanAction.setVirtualLanIdentifier(vlanNumber);
						
						int tagSize = addedVlanAction.getLengthU();
						//add vlan tag action to mod
						clonedMod.getActions().add(i, addedVlanAction);
						//recompute length?
						//DOESN'T appear to work
//						clonedMod.computeLength();
						
						//INSTEAD recomputer manually
						System.out.println("Expected length: " + (originalSize + tagSize) +"\nCurrent length: "+flowMod.getLengthU());
						clonedMod.setLengthU(originalSize + tagSize);
						//set output action of the mod to output on the ghostport
						((OFActionOutput) action).setPort(ghostPort);
//						flowMod.setOriginalOutputPort(originalPort);
						System.out.println("Flow after modification: "+clonedMod);
						
						//TODO: add rule to remove tag on original switch- e.g. the ActiveSwitch object -  this will be done in another function call
						
						break; //Assuming that there is only one output port...	
//					}
//				}
			}
		}
		sendFlowMod(clonedMod, senderSwitchObject);
		return clonedMod;
	}

	private void sendFlowMod(FVFlowMod flowMod, WorkerSwitch modifiedSwitch) {
		DPID switchDpid = new DPID(modifiedSwitch.getDPID());
		System.out.println("\n\n\nSwitch "+switchDpid.getDpidString()+" is receiving flow mod: "+flowMod+"\n\n");
		modifiedSwitch.handleFlowModAndSend(flowMod);
		
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

























