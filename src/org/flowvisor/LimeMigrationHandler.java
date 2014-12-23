/**
 * 
 */
package org.flowvisor;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.derby.impl.sql.compile.HasVariantValueNodeVisitor;
import org.flowvisor.PortInfo.PortType;
import org.flowvisor.classifier.LimeFlowTable;
import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.message.FVFlowMod;



/**
 * @author Murad Kaplan
 *
 */
public class LimeMigrationHandler {
	private int cloneSwitchCounter;
	
	public LimeMigrationHandler(){
		cloneSwitchCounter = 0;
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
					System.out.println("Starting flowmod migration function");
					WorkerSwitch.insertFlowRuleTableAndSendModified(activeSwitch, cloneSwitch, activeSwitch.getFlowTable().getFlowTable());  //FIXME we may need to clone this
					
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
	}
	
	/**
	 * This will use libvirt to migrate the vms. Will need to have access rights for libvirt
	 * to the hosts, the ip address of the libvirt hosts/hypervisors, the destination libvirt host/hypervisor ip, and the libvirt names of the hosts
	 */
	public boolean migrateVM(LimeHost host){
		boolean migrated = LimeVMMigrater.liveMigrateQemuVM(host.getOriginalHost(), host.getDestinationHost(), host.getLibvirtDomain());
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
			for(FVFlowMod flowMod : originalSwitch.getFlowTable().getFlowTable()){
				if(WorkerSwitch.hasOutputPortWithoutVlan(flowMod, host.getConnectedPort())){
					matchingMods.add(flowMod);
				}
				
			}
			WorkerSwitch.insertFlowRuleTableAndSendModified(cloneSwitch, originalSwitch, matchingMods);
			return true;
		}
		return false;
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
		
		// remove ghost port
		// this will happen automatically when OVX remove port
		
		// check switch counter
		if (cloneSwitchCounter == LimeContainer.getCloneSwitchContainer().size()){
			// send to ovx to delete all old active switches
			LimeContainer.getCloneSwitchContainer().clear();
			LimeContainer.getActiveToCloneSwitchMap().clear();
			//TODO: need to put correct flow tables into the new switches
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
}

























