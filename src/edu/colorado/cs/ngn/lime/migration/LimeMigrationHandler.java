/**
 * 
 */
package edu.colorado.cs.ngn.lime.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowvisor.FlowVisor;
import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.message.FVFlowMod;
import org.flowvisor.message.actions.FVActionDataLayerDestination;
import org.flowvisor.message.actions.FVActionDataLayerSource;
import org.flowvisor.message.actions.FVActionStripVirtualLan;
import org.flowvisor.message.actions.FVActionVirtualLanIdentifier;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

import edu.colorado.cs.ngn.lime.LimeContainer;
import edu.colorado.cs.ngn.lime.LimeFlowTable;
import edu.colorado.cs.ngn.lime.exceptions.DPIDLookupException;
import edu.colorado.cs.ngn.lime.exceptions.LimeDummyPortNotFoundException;
import edu.colorado.cs.ngn.lime.exceptions.MacLookupException;
import edu.colorado.cs.ngn.lime.exceptions.SwitchOriginalAndCloneException;
import edu.colorado.cs.ngn.lime.util.DPID;
import edu.colorado.cs.ngn.lime.util.LimeMigrationUtils;
import edu.colorado.cs.ngn.lime.util.LimeVMMigrater;
import edu.colorado.cs.ngn.lime.util.PortInfo;
import edu.colorado.cs.ngn.lime.util.PortInfo.PortType;



/**
 * @author Murad Kaplan, Michael Coughlin
 * 
 * Class to handle migration of a network. Is a singleton object so that
 * api calls to start migration cannot accidentally start migration twice.
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
	//maps original OF mods to migration flow mod information
	private HashMap<FVFlowMod, LimeVlanTranslationInfo> migrationModTranslationMap;
	private ArrayList<LimeHost> migratedHosts;
	
	private LimeMigrationHandler(){
		cloneSwitchCounter = 0;
		vlanCounter = 0;
		migrating = false;
		originalFlowMods = new HashMap<DPID, List<FVFlowMod>>();
		vlanHandlerMods = new HashMap<DPID, List<FVFlowMod>>();
		localOriginalToCloneSwitchMap = new HashMap<DPID, DPID>();
		migrationModTranslationMap = new HashMap<FVFlowMod, LimeVlanTranslationInfo>();
		migratedHosts = new ArrayList<LimeHost>();
	}
	
	public static LimeMigrationHandler getInstance(){
		if(singleInstance == null){
			singleInstance = new LimeMigrationHandler();
		}
		return singleInstance;
	}
	
	
	/**
	 * Starts the migration process.
	
	 * This method is called before VMs are migrated.
	 * 
	 * @throws InterruptedException
	 * @throws LimeDummyPortNotFoundException 
	 * @throws DPIDLookupException 
	 * @throws SwitchOriginalAndCloneException 
	 */
	public synchronized void init() throws InterruptedException, LimeDummyPortNotFoundException, SwitchOriginalAndCloneException, DPIDLookupException{ 
		// TODO create LIME exception of missing ports or switches
		//TODO: drop flows from internal data structure when a new flow comes in with the same match
		if(migrating){
			System.out.println("Migration has already been initiated");
			return;
		}
		System.out.println("MURAD: LimeMigration, initializing migration process");
		for(Long activeSwID : LimeContainer.getActiveToOriginalSwitchMap().keySet()){
			WorkerSwitch currentSwitch = LimeContainer.getAllWorkingSwitches().get(activeSwID);
			ArrayList<FVFlowMod> currentList = new ArrayList<FVFlowMod>();
			for(FVFlowMod flowMod : currentSwitch.getFlowTable().getFlowTable()){
				if(LimeMigrationUtils.isValidFlowModWithoutVlan(flowMod)){
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
					//TODO: make sure mods that do not output to a host are written to the clone switches unmodified
					createHandlerModsOriginalToClone(activeSwitch, cloneSwitch);
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
	 * This method writes vlan handler and receiver mods to the switches to handle ALL flow mods
	 * in a given original switch - clone switch pair
	 * 
	 * This method uses the original flow mods object to determine the mods to write
	 * 
	 * @param originalSwitch The original switch for this flow mod pair
	 * @param cloneSwitch The clone switch for this flow mod pair
	 * @throws LimeDummyPortNotFoundException
	 * @throws DPIDLookupException 
	 * @throws SwitchOriginalAndCloneException 
	 */
	private void createHandlerModsOriginalToClone(WorkerSwitch originalSwitch, WorkerSwitch cloneSwitch) throws LimeDummyPortNotFoundException, SwitchOriginalAndCloneException, DPIDLookupException {
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
			//create vlan sending and receivng mods for each flow mod
			//the receiving switch in this case is the original switch
			//the sending switch is the clone switch;
			createVlanHandlers(flowMod, cloneSwitchDpid, originalSwitchDpid, true, false, (short)-1);
		}
		
	}

	/**
	 * Creates migration flow mods that handle networking that goes from already migrated hosts to hosts 
	 * that are not migrated from the original to the clone switch
	 * 
	 * @param cloneSwitch DPID of the current clone switch
	 * @param originalSwitch DPID of the current original switch
	 * @param matchingMods List of mods that are applicable to this switch
	 * @throws LimeDummyPortNotFoundException 
	 * @throws DPIDLookupException 
	 * @throws SwitchOriginalAndCloneException 
	 */
	private void createHandlerModsCloneToOriginal(DPID cloneSwitch, DPID originalSwitch, List<FVFlowMod> matchingMods, boolean preMigration, short preMigrationPort) throws LimeDummyPortNotFoundException, SwitchOriginalAndCloneException, DPIDLookupException {
		for(FVFlowMod flowMod : matchingMods){
			//need to iterate over each output port of the flow mod
			boolean allMigrated = true;
			for(OFAction action : flowMod.getActions()){
				if(action instanceof OFActionOutput){
					short currentOutputPort = ((OFActionOutput) action).getPort();
					//this if statement should only occur if ALL of the output ports have been migrated
					if(!LimeMigrationUtils.outputPortMigrated(originalSwitch, flowMod, currentOutputPort, migratedHosts) || (preMigration && currentOutputPort == preMigrationPort)){
						allMigrated = false;
					}
				}
			}
			
			if(allMigrated){
				WorkerSwitch cloneSwitchObj = LimeContainer.getAllWorkingSwitches().get(cloneSwitch.getDpidLong());
				LimeMigrationUtils.sendFlowMod(flowMod, cloneSwitchObj);
			}else{
				//recreate the flow mods going in the other direction with the updated state
				createVlanHandlers(flowMod, originalSwitch, cloneSwitch, false, preMigration, preMigrationPort);
			}
		}
		
	}
	
	/**
	 * This functions uses libvirt to migrate a VM. The flowvisor user will need to have access
	 * rights for libvirt on the source and destination hypervisors that are specified in the 
	 * host config, and password-less ssh login be pre-sharing ssh-keys to all hypervisors.
	 * 
	 * @throws LimeDummyPortNotFoundException 
	 * @throws DPIDLookupException 
	 * @throws SwitchOriginalAndCloneException 
	 */
	public boolean migrateVM(LimeHost host) throws LimeDummyPortNotFoundException, SwitchOriginalAndCloneException, DPIDLookupException{
		System.out.println("In migrate vm function");
		//before migration, modify mods so that any packet that is to be output to the host's connected port is also sent out the ghost port
		//and then received and output on the connected port on the clone
		//also need to have the mods to handle packets coming in from that port on the clone set up first, rather than after the migration
		//set this here, as existing functionality can use this state
		migratedHosts.add(host);
		
		ArrayList<FVFlowMod> matchingMods = new ArrayList<FVFlowMod>();
		ArrayList<FVFlowMod> matchingOutputPortMods= new ArrayList<FVFlowMod>();
		DPID originalDpid = null;
		for(DPID dpid : originalFlowMods.keySet()){
			if(dpid.getDpidLong().longValue() == host.getOriginalDpid().getDpidLong().longValue()){
				originalDpid = dpid;
			}
		}
		
		//need to update ALL mods that have input port ==  host.connectedPort() or if a mod has an OUTPUT port == host.connectedPort()
		//and the input port of that mod has been migrated
		for(FVFlowMod flowMod : originalFlowMods.get(host.getOriginalDpid())){
			if(LimeMigrationUtils.hasInputPortWithoutVlan(flowMod, host.getConnectedPort())){
				matchingMods.add(flowMod);
			}
			if(LimeMigrationUtils.inputPortMigrated(flowMod, host.getOriginalDpid(), migratedHosts) && LimeMigrationUtils.hasOutputPortWithoutVlan(flowMod, host.getConnectedPort())){
				matchingMods.add(flowMod);
			}
			if(LimeMigrationUtils.outputPortMigrated(host.getOriginalDpid(), flowMod, host.getConnectedPort(), migratedHosts)){
				matchingOutputPortMods.add(flowMod);
			}
			
		}
		
		createPreMigrationSendingMods(host, matchingOutputPortMods, matchingMods);
		
		boolean migrated = LimeVMMigrater.liveMigrateQemuVM(host.getOriginalHost(), host.getDestinationHost(), host.getLibvirtDomain());
		System.out.println("migrate vm function returned");
		if(migrated){
			//recheck ALL flow mods to make sure that they point to VMs that are on the correct switches
			//create reverse rule. need to have the connected port
			WorkerSwitch originalSwitch = LimeContainer.getAllWorkingSwitches().get(host.getOriginalDpid().getDpidLong());
			WorkerSwitch cloneSwitch = LimeContainer.getAllWorkingSwitches().get(host.getCloneDpid().getDpidLong());
			
			//recreates all mods on the original switch to take into account new state of hosts that have migrated
			createHandlerModsOriginalToClone(originalSwitch, cloneSwitch);
			
			//TODO: need to delete the incorrect rules from the physical switches, but keep the rules in the lime flow table object
			for(FVFlowMod flowMod : matchingMods){
				//delete all non-vlan tag mods from the original switch, bypassing the lime flow table object, assuming this method works
				LimeMigrationUtils.deleteFlowMod(originalSwitch, flowMod);
			}
			
			return true;
		} else{
			//remove the host from the migrated host data structure if this fails, but this should no happen
			//Lime is not defined if a host migration fails, and will likely just result in this host being unreachable
			migratedHosts.remove(host);
		}
		return false;
	}

	/**
	 * Creates lime migration OF mods for a VM immediately before a VM is migrated. Special handler mods
	 * are written that ensure a VM's connectivity during migration by forwarding to the port on the original
	 * switch and clone switch simultaneously. Mods are also written to handle communication from the original
	 * and clone ports of the host.
	 * 
	 * @param host Host information of the host being migrated
	 * @param matchingOutputMods List of all original mods that have the output port of the host's connected port
	 * @param matchingMods List of all original mods that have the input port or output port of the host's connected port
	 * @throws LimeDummyPortNotFoundException
	 * @throws DPIDLookupException 
	 * @throws SwitchOriginalAndCloneException 
	 */
	private void createPreMigrationSendingMods(LimeHost host, List<FVFlowMod> matchingOutputMods, List<FVFlowMod> matchingMods) throws LimeDummyPortNotFoundException, SwitchOriginalAndCloneException, DPIDLookupException {
		createHandlerModsCloneToOriginal(host.getCloneDpid(), host.getOriginalDpid(), matchingMods, true, host.getConnectedPort());
		DPID cloneSwitch = host.getCloneDpid();
		DPID originalSwitch = host.getOriginalDpid();
		for(FVFlowMod originalMatchingMod : matchingOutputMods){
			createVlanHandlers(originalMatchingMod, cloneSwitch, originalSwitch, true, true, host.getConnectedPort());
		}
		
	}
	
	
	
	/**
	 * Signal that a particular switch is done migrating. Deletes all migration mods from 
	 * the original switch and ensures that all of the original flow mods are written to the
	 * clone switch
	 * 
	 * @param activeSwitchDpid The switch that has finished migrating
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
					LimeMigrationUtils.deleteFlowMod(currentSwitch, flowMod);
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
	 * Check if the migration flag has been set
	 * 
	 * @return The state of the migration flag
	 */
	public boolean isMigrating(){
		return migrating;
	}
	
	/**
	 * Record a flow mod in the migration handler data structure
	 * that creates a vlan handler mod and a vlan handler mod.
	 * This stores information about the current migration mod,
	 * including the associated switches and ports and the direction.
	 * 
	 * @param originalMod The orginal flow mod
	 * @param receiverSwitch The switch that receives the recevier mod
	 * @param senderSwitch The switch that receives the sender mod
	 * @param originalToClone The direction of the migration mods. True if the receiver switch is a clone switch and the sender switch is an original switch, false otherwise
	 * @param preMigration True is this migration mod is occurring immediately before a VM migration
	 * @param preMigrationPort The port number of the VM that is about to be migrated. If preMigration is false then this input has no meaning
	 * @return A new LimeVlanTranslationInfo object that stores the input state associated with a migration mod
	 * @throws LimeDummyPortNotFoundException
	 * @throws DPIDLookupException 
	 * @throws SwitchOriginalAndCloneException 
	 */
	public LimeVlanTranslationInfo createVlanHandlers(FVFlowMod originalMod, DPID receiverSwitch, DPID senderSwitch, boolean originalToClone, boolean preMigration, short preMigrationPort) throws LimeDummyPortNotFoundException, SwitchOriginalAndCloneException, DPIDLookupException{
		WorkerSwitch senderSwitchObject = LimeContainer.getAllWorkingSwitches().get(senderSwitch.getDpidLong());
		short senderGhostPort = senderSwitchObject.getGhostPort();
		
		Map<FVFlowMod, List<FVFlowMod>> migrationModPairs = createAndSendVlanMigrationPairsMod(senderGhostPort, originalMod, senderSwitchObject, originalToClone, preMigration, preMigrationPort);
		LimeVlanTranslationInfo currentInfo = new LimeVlanTranslationInfo(migrationModPairs, receiverSwitch, senderSwitch, originalToClone, originalMod, false);
		migrationModTranslationMap.put(originalMod, currentInfo);
		for(FVFlowMod senderMigrationMod : migrationModPairs.keySet()){
			LimeMigrationUtils.addToVlanMap(senderSwitch, senderMigrationMod, vlanHandlerMods);
			for(FVFlowMod receiverMigrationMod : migrationModPairs.get(senderMigrationMod)){
				LimeMigrationUtils.addToVlanMap(receiverSwitch, receiverMigrationMod, vlanHandlerMods);
			}
		}
		return currentInfo;
	}
	
	/**
	 * Create a migration flow mod that receives from the ghost port on a particular switch and performs the action of the original mod.
	 * Should be paired with a corresponding sending mod created by createAndSendVlanSenderMod. The OF mod that is written to the switch
	 * uses vlan tags to determine the traffic being sent from the original switch to the clone switch for a particular OF mod.
	 * This depends on the state of which hosts has been migrated. will only forward to hosts that exist on this switch
	 * 
	 * NOTE: for OVX compatibility, all of the output ports and the dummy port must exist in the OVX network and be started, with either
	 * a mac address connected (a real host does not need to be connected, but the OVX config must have a valid mac address connected to the port),
	 * or the port must be started manually using the startPort OVX API call.
	 * 
	 * @param vlanNumber The vlan tag to be used with this migration mod
	 * @param ghostPort The ghost port to be used with this migration mod. Is the port that connects the sender and receiver switches together
	 * @param originalMod The original mod that is to be analyzed to determine the needed actions. All OFActionOutputs that output to a port that had a host connected before migration need to be considered
	 * @param receiverSwitchObject The WorkerSwitch object for the switch that will receive the receiver mod
	 * @param receivingFromOriginal The direction of the current pair of sender/receiver mods. If the receiver switch is a clone switch, this should be True. False if this switch is an original switch
	 * @param preMigration True is this mod is to be created immediately before a migration occurs. If true, the mod must output to the preMigrationPort on the switch even if the host has not been migrated to that port yet.
	 * @param preMigrationPort The value of the connected port of the host that is about to be migrated. Is only read if preMigration is True. Be sure this port exists and is started in OVX.
	 * @return The receiver mod that was created by this function. Note that this function also writes the mod directly to the switch.
	 * @throws LimeDummyPortNotFoundException
	 * @throws DPIDLookupException 
	 */
	private FVFlowMod createAndSendVlanReceiverMod(short vlanNumber, short ghostPort, FVFlowMod originalMod, WorkerSwitch receiverSwitchObject, boolean receivingFromOriginal, boolean preMigration, short preMigrationPort) throws LimeDummyPortNotFoundException, DPIDLookupException {
		// based off of org.flowvisor.mesage.FVPacketIn.sendDropRule()
		FVFlowMod vlanReceiverMod = (FVFlowMod) FlowVisor.getInstance().getFactory().getMessage(OFType.FLOW_MOD);

		//create match to match packets coming in ghostPort for a particular vlan
		FVMatch match = new FVMatch();
		int wildcards = FVMatch.OFPFW_ALL;
		wildcards &= ~FVMatch.OFPFW_DL_VLAN;
		match.setDataLayerVirtualLan(vlanNumber);
		//need to set input port or ovx has a nullpointerexception. is this part of openflow spec?
		//use a dummy port on each switch as the input port in future
		match.setInputPort(LimeMigrationUtils.getDummyPort(new DPID(receiverSwitchObject.getDPID())));
		
		//NOTE: see openvirtex.messages.actions.OVXActionOutput.java line 171 else statement:
		//if the input port is an edge, e.g. is a link to another switch, like the ghost ports are,
		//then ovx attempts to get the dl_src and dl_dst of the mods, which will not exist if they 
		//are wildcarded-> cannot set to input port, as this causes ovx to set the action as IN_PORT
		match.setWildcards(wildcards);
		vlanReceiverMod.setMatch(match);
		
		//update logic to output to the vlan number. All decision of whether a packet needs to be sent
		//to a remote host is done by the sender mod, since OF mods can have multiple actions applied
		//in order, it turns out
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		FVActionDataLayerSource mod_dl_src = new FVActionDataLayerSource();
		FVActionDataLayerDestination mod_dl_dst = new FVActionDataLayerDestination();
		String srcMac = "ff:ff:ff:ff:ff:ff";
		String destMac;
		try {
			destMac = LimeMigrationUtils.getMacForPort(new DPID(receiverSwitchObject.getDPID()), vlanNumber, LimeContainer.getDpidToMacMap());
			mod_dl_src.setDataLayerAddress(LimeMigrationUtils.convertMacToBytes(srcMac));
			mod_dl_dst.setDataLayerAddress(LimeMigrationUtils.convertMacToBytes(destMac));
			actions.add(mod_dl_src);
			actions.add(mod_dl_dst);
			OFActionOutput vlanOutput = new OFActionOutput();
			//set output port to that of the vlan tag number
			vlanOutput.setPort(vlanNumber);
			actions.add(vlanOutput);
			vlanReceiverMod.setActions(actions);
			vlanReceiverMod.computeLength();
			
			System.out.println("Actions for this mod:");
			for(OFAction action : vlanReceiverMod.getActions()){
				System.out.println(action);
			}
			
			LimeMigrationUtils.sendFlowMod(vlanReceiverMod, receiverSwitchObject);
		} catch (MacLookupException e) {
			//This shouldn't happen. If it does, we for sure do not want to write a mod
		}
		
		return vlanReceiverMod;
	}

	
	/**
	 * Creates a migration flow mod that sends from one switch to another using the ghost port, based on the original flow mod.
	 * Relies on stored state of which hosts have been migrated. Will forward out the ghost port with a vlan tag if there is a
	 * host that has been migrated to the clone switch. If there are hosts that are on the current switch and are output
	 * actions, then will forward to them without adding a vlan tag. If the preMigration flag is set, the migration mod will
	 * output to the local preMigration port and add a vlan tag and output over the ghost port for that port, regardless of the 
	 * migration status of the host connected to that port.
	 * 
	 * This function also now directly creates the receiver mods. The receiver mod matches on the vlan tag == the ouput port on
	 * the remote switch
	 * 
	 * NOTE: for OVX compatibility, all of the output ports and the input port must exist in the OVX network and be started, with either
	 * a mac address connected (a real host does not need to be connected, but the OVX config must have a valid mac address connected to the port),
	 * or the port must be started manually using the startPort OVX API call.
	 * 
	 * @param vlanTagNumber The vlan tag to be used with this migration mod
	 * @param ghostPort The ghost port to be used with this migration mod. Is the port that connects the sender and receiver switches together
	 * @param originalMod The original mod that is to be analyzed to determine the needed actions. All OFActionOutputs that output to a port that had a host connected before migration need to be considered
	 * @param senderSwitchObject The WorkerSwitch object for the switch that will send the sender mod
	 * @param originalToClone The direction of the current pair of sender/receiver mods. If the sender switch is an original switch, this should be True. False if this switch is a clone switch
	 * @param preMigration True is this mod is to be created immediately before a migration occurs. If true, the mod must output to the preMigrationPort on the switch even if the host has not been migrated to that port yet.
	 * @param preMigrationPort The value of the connected port of the host that is about to be migrated. Is only read if preMigration is True. Be sure this port exists and is started in OVX.
	 * @return The sender mod that was created by this function. Note that this function also writes the mod directly to the switch.
	 * @throws SwitchOriginalAndCloneException 
	 * @throws DPIDLookupException 
	 * @throws LimeDummyPortNotFoundException 
	 * 
	 */
	private Map<FVFlowMod, List<FVFlowMod>> createAndSendVlanMigrationPairsMod(short ghostPort, FVFlowMod originalMod, WorkerSwitch senderSwitchObject, boolean originalToClone, boolean preMigration, short preMigrationPort) throws SwitchOriginalAndCloneException, LimeDummyPortNotFoundException, DPIDLookupException {
		FVFlowMod clonedMod = (FVFlowMod) originalMod.clone();
		System.out.println("\nCurrent flowmod: "+originalMod+"\n");
		
		ArrayList<OFActionOutput> localPortsActions = new ArrayList<OFActionOutput>();
		ArrayList<OFActionOutput> remotePortsActions = new ArrayList<OFActionOutput>();
		ArrayList<OFActionOutput> unneededActions = new ArrayList<OFActionOutput>();
		
		//set the wildcards of the new OF mod
		OFMatch match = clonedMod.getMatch();
		int wildcards = match.getWildcards();
		match.setWildcards(wildcards);
		clonedMod.setMatch(match);
		
		DPID currentSwitch = new DPID(senderSwitchObject.getDPID());
		
		ArrayList<Short> remoteOutputPorts = new ArrayList<Short>();
		
		for(OFAction action : clonedMod.getActions()){
			if(action instanceof OFActionOutput){
				short currentOutputPort = ((OFActionOutput) action).getPort();
				try {
					boolean outputPortOriginallyIsHost = LimeMigrationUtils.portIsOriginallyHostConnected(currentSwitch, currentOutputPort);
					//check if a host is connected to this port on the switch, whether am the clone or the original
					boolean hostConnected = LimeMigrationUtils.hostConnected(currentSwitch, currentOutputPort, migratedHosts);
					//if a host was connected to this port originally, then we need to handle migration for it. else we just need to 
					//send it to the clone switch to output on the correct port. at this point, we need to know if we are the clone
					//switch or the original switch. if original, all original ports that are output actions but not H_CONNECTED,
					//then we send to the clone switch with the correct vlan as the output port. if we are the clone switch, we keep
					//all output ports that are not H_CONNECTED to output to the local port
					//if an output port was H_CONNECTED then if we are the original switch, we output to the local port if the host
					//not been migrated or we are in premigation for that port. if the host has been migrated or we are in premigration,
					//we output to the cloneswitch with the correct vlan number for the output port
					//if an output port was H_CONNECTED and we are the clone switch, we output to the local port if the host has been migrated
					//or we are in premigation and the port is the premigration port. if the host has not be migrated then we output to
					//the original switch with the correct vlan number equal to the output port or if the port is the premigration port
					boolean amOriginalSwitch = originalToClone;
					boolean portIsPremigationPort = preMigration && (preMigration && currentOutputPort == preMigrationPort);
					//always output to the port if a host is locally connected or is a premigration port
					//or if am the clone switch and the port is not an H_CONNECTED port
					boolean outputToLocalPort = outputPortOriginallyIsHost && (hostConnected || portIsPremigationPort) || !outputPortOriginallyIsHost && !amOriginalSwitch;
					//always output to remote port if the port is originally h_connected but the host is not connected here or is a premigration port
					//or if am the orinal switch and the host was not originally connected
					boolean outputToRemotePort = outputPortOriginallyIsHost && (!hostConnected || portIsPremigationPort) || !outputPortOriginallyIsHost && amOriginalSwitch;
					if(outputToLocalPort){
						localPortsActions.add((OFActionOutput) action);
					}
					if(outputToRemotePort){
						remotePortsActions.add((OFActionOutput) action);
						remoteOutputPorts.add(((OFActionOutput) action).getPort());
					}
					if(!outputToLocalPort && !outputToRemotePort){
						unneededActions.add((OFActionOutput) action);
					}
				} catch (DPIDLookupException e) {
					//this shouldn't happen. If it does, return null
					return null;
				}
			}
		}
		
		//get rid of all unneeded actions
		clonedMod.getActions().removeAll(unneededActions);
		
		for(OFActionOutput migratedAction : remotePortsActions){
			//if a mod needs to be sent locally and remotely, as if it is a premigation action
			int actionIndex = clonedMod.getActions().indexOf(migratedAction);
			if(localPortsActions.contains(migratedAction)){
				OFActionOutput clonedAction = new OFActionOutput();
				clonedAction.setPort(migratedAction.getPort());
				clonedMod.getActions().add(actionIndex, clonedAction);
			}
			migratedAction.setPort(ghostPort);
			//create vlan tag action
			FVActionVirtualLanIdentifier addedVlanAction = new FVActionVirtualLanIdentifier();
			FVActionStripVirtualLan addedStripVlanAction = new FVActionStripVirtualLan();
			addedVlanAction.setVirtualLanIdentifier(migratedAction.getPort());
			//add vlan tag action to mod
			//insert vlan action before the output action in the action list
			actionIndex = clonedMod.getActions().indexOf(migratedAction);
			clonedMod.getActions().add(actionIndex, addedVlanAction);
			actionIndex = clonedMod.getActions().indexOf(migratedAction);
			//insert the strip vlan action after the output action
			clonedMod.getActions().add(actionIndex + 1, addedStripVlanAction);
		}
		
		clonedMod.computeLength();
		
		System.out.println("Flow after modification: "+clonedMod);
		LimeMigrationUtils.sendFlowMod(clonedMod, senderSwitchObject);
		
		HashMap<FVFlowMod, List<FVFlowMod>> modMap = new HashMap<FVFlowMod, List<FVFlowMod>>();
		ArrayList<FVFlowMod> receiverMods = new ArrayList<FVFlowMod>();
		//create the receiver mods here directly and return a map of the sender mod to the recevier mods
		for(Short remotePort : remoteOutputPorts){
			WorkerSwitch duplicateSwitch = senderSwitchObject.getDuplicateSwitch();
			FVFlowMod receiverMod = createAndSendVlanReceiverMod(remotePort, ghostPort, originalMod, duplicateSwitch, originalToClone, preMigration, preMigrationPort);
			receiverMods.add(receiverMod);
		}
		modMap.put(clonedMod, receiverMods);
		return modMap;
	}

}