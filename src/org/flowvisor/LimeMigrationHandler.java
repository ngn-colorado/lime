/**
 * 
 */
package org.flowvisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.PortInfo.PortType;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.message.FVFlowMod;
import org.openflow.protocol.OFFeaturesRequest;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;

/**
 * @author Murad Kaplan
 *
 */
public class LimeMigrationHandler {
	/**
	 * Make sure that all required active and clone switches and their ports are available
	 * @throws InterruptedException
	 */
	public void init() throws InterruptedException{ // TODO create LIME exception of missing ports or switches
		System.out.println("MURAD: LimeMigration, inititlizing migration process");
		// this should be received from operator, for testing, we just assume that we have them

		// top switch only has two ports and they are connected to switches (SW_CONNECTED)
		HashMap<Short, PortInfo> portTable1 = new HashMap<>();
		portTable1.put((short) 1, new PortInfo(PortType.SW_CONNECTED, null, null));
		portTable1.put((short) 2, new PortInfo(PortType.SW_CONNECTED, null, null));
		portTable1.put((short) 3, new PortInfo(PortType.GHOST, null, null));
		LimeContainer.addCloneSwitch(4, portTable1);
		LimeContainer.insertActiveToCloneSwitchMap(46200400562356228L-3, 46200400562356228L);
		System.out.println("MURAD: Clone Top-level Switch: " + 46200400562356228L);

		for(long j=46200400562356229L; j<46200400562356231L; j++){
			portTable1 = new HashMap<>();
			for(short i= 1; i<3; i++){
				portTable1.put(i, new PortInfo(PortType.EMPTY, null, null));
			}	
			portTable1.put((short) 3, new PortInfo(PortType.SW_CONNECTED, null, null));
			portTable1.put((short) 4, new PortInfo(PortType.GHOST, null, null));
			LimeContainer.addCloneSwitch(j, portTable1);
			LimeContainer.insertActiveToCloneSwitchMap(j-3, j);
			System.out.println("MURAD: Clone Second-level Switch: " + j);
		}

		// loop through classifier to make sure that all needed switches (active/cloned) are there
		// make sure that all required ports are there from active-port table in active classifier 
		// clone switches should not be connected to any VMs!!

		// just loop and make sure port number is there, if so, change its type to what its in clone one
		// don't change CONNECTED switch, trigger error!
		// add rules for both active and clone switches to handle packets in and out ghost ports
		// For any packet coming from port G 
		// 		remove vlanid 
		// 		forward to port = to vlanid


		for (Map.Entry entry : LimeContainer.getActiveToCloneSwitchMap().entrySet()) {
			if((LimeContainer.getAllWorkingSwitches().containsKey(entry.getKey())) &&
					(LimeContainer.getAllWorkingSwitches().containsKey(entry.getValue()))){
				long activeSwID = (long) entry.getKey();
				FVClassifier activeFVClassifier = LimeContainer.getAllWorkingSwitches().get(activeSwID);
				long cloneSwID  = (long) entry.getValue();
				FVClassifier cloneFVClassifier = LimeContainer.getAllWorkingSwitches().get(cloneSwID);

				boolean portMissing = false;
				short ghostPort = -1;
				HashMap<Short, PortInfo> portTable = LimeContainer.getCloneSwitchContainer().get(cloneSwID).getPortTable();
				cloneFVClassifier.setActivePorts(portTable);

				for (Map.Entry portEntry : portTable.entrySet()){
					short portNo = (short) portEntry.getKey();
					PortInfo pInfo = (PortInfo) portEntry.getValue();
					if(activeFVClassifier.getActivePorts().containsKey(portNo)){
						if(!activeFVClassifier.getActivePorts().get(portNo).getType().equals(PortType.H_CONNECTED) &&
								!activeFVClassifier.getActivePorts().get(portNo).getType().equals(PortType.SW_CONNECTED)){ // we don't want to touch these ports that reflect original switch ports
							activeFVClassifier.getActivePorts().get(portNo).setType(pInfo.getType());
						}
						cloneFVClassifier.getActivePorts().get(portNo).setType(pInfo.getType());
						if(pInfo.getType().equals(PortType.GHOST)){
							ghostPort = portNo; // this should only happens once since we only have one ghost port
						}
					}
					else{
						portMissing = true;
						System.out.println("MURAD: LimeMigrationHandler, ERROR, port " + portNo+ " is not found for aSW " + activeSwID + " or cSW " + cloneSwID); 
						break;
					}
				}

				if (!portMissing && ghostPort != -1){
					// setup active switch
					activeFVClassifier.setDuplicateSwitch(cloneSwID);
					activeFVClassifier.ereaseFlowTable();
					// flush LimeFlowTable for active 

					// setup clone switch
					cloneFVClassifier.setDuplicateSwitch(activeSwID);
					// copy FlowMod table from active to switch and push it the switch
					cloneFVClassifier.insertFlowRuleTableAndSend(activeFVClassifier.getFlowRuleTable(), ghostPort);  //FIXME we may need to clone this
					LinkedList<OFFlowMod> flowModList = cloneFVClassifier.getFlowRuleTable();
					HashMap<Short, ArrayList<FVFlowMod>> cloneLimeFlowTable = new HashMap<>(); 
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

	private void updatePort(FVClassifier fvClassifier, short port, PortType pType){

	}
}
