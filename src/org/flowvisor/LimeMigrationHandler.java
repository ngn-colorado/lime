/**
 * 
 */
package org.flowvisor;

import java.util.HashMap;
import java.util.Map;

import org.flowvisor.PortInfo.PortType;
import org.flowvisor.classifier.WorkerSwitch;



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
	 * Make sure that all required active and clone switches and their ports are available
	 * @throws InterruptedException
	 */
	public void init() throws InterruptedException{ // TODO create LIME exception of missing ports or switches
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

		for (Map.Entry entry : LimeContainer.getActiveToCloneSwitchMap().entrySet()) {
			if((LimeContainer.getAllWorkingSwitches().containsKey(entry.getKey())) &&
					(LimeContainer.getAllWorkingSwitches().containsKey(entry.getValue()))){
				long activeSwID = (long) entry.getKey();
				WorkerSwitch activeSwitch = LimeContainer.getAllWorkingSwitches().get(activeSwID);
				long cloneSwID  = (long) entry.getValue();
				WorkerSwitch cloneSwitch = LimeContainer.getAllWorkingSwitches().get(cloneSwID);

				boolean portMissing = false;
				short ghostPort = -1;
				HashMap<Short, PortInfo> portTable = LimeContainer.getCloneSwitchContainer().get(cloneSwID).getPortTable();
				cloneSwitch.setActivePorts(portTable);
				int emptyPortsCounter = 0;
				for (Map.Entry portEntry : portTable.entrySet()){
					short portNo = (short) portEntry.getKey();
					PortInfo pInfo = (PortInfo) portEntry.getValue();
					if(activeSwitch.getActivePorts().containsKey(portNo)){   //TODO need to check if clone switch has all the required ports
						if(!activeSwitch.getActivePorts().get(portNo).getType().equals(PortType.H_CONNECTED)){// we don't want to change these to empty ports
							//&&!activeSwitch.getActivePorts().get(portNo).getType().equals(PortType.SW_CONNECTED)){ 
							activeSwitch.getActivePorts().get(portNo).setType(pInfo.getType());
						}
						cloneSwitch.getActivePorts().get(portNo).setType(pInfo.getType());
						if(pInfo.getType().equals(PortType.GHOST)){
							ghostPort = portNo; // this should only happens once since we only have one ghost port
						}
						
						if(pInfo.getType().equals(PortType.EMPTY)){
							emptyPortsCounter ++;
						}
						
						
					}
					else{
						portMissing = true;
						System.out.println("MURAD: LimeMigrationHandler, ERROR, port " + portNo+ " is not found for aSW " + activeSwID + " or cSW " + cloneSwID); 
						break;
					}
				}
				cloneSwitch.setConnectedHostCounter(emptyPortsCounter);

				if (!portMissing && ghostPort != -1){
					// setup active switch
					activeSwitch.setDuplicateSwitch(cloneSwitch);
					// setup clone switch
					cloneSwitch.setDuplicateSwitch(activeSwitch);
					// copy FlowMod table from active to switch and push it the switch
					cloneSwitch.insertFlowRuleTableAndSendModified(activeSwitch, ghostPort);  //FIXME we may need to clone this
					
					
				}
				else{
					System.out.println("MURAD: ERROR finding port!!");
					return;
				}

			}
			else{
				System.out.println("MURAD: ERROR finding Active to Clone switches!!!!!!!!!!: " + entry.getKey() + " " + entry.getValue());
				return;
			}
		}
		System.out.println("Initialization was seccuful..");
	}

	public synchronized void switchDoneMigrating(WorkerSwitch cloneSwitch, WorkerSwitch activeSwitch){
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
		}
	}
}
