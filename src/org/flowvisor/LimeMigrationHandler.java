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
		for(long j=4; j<7; j++){
			HashMap<Short, PortInfo> portTable = new HashMap<>();
			PortInfo pInfo = new PortInfo(PortType.SW_CONNECTED, null, null);
			portTable.put((short) 1, pInfo);
			for(short i= 2; i<4; i++){
				pInfo = new PortInfo(PortType.EMPTY, null, null);
				portTable.put(i, pInfo);
			}

			pInfo = new PortInfo(PortType.GHOST, null, null);
			portTable.put((short) 4, pInfo);

			LimeContainer.addCloneSwitch(j, portTable);

			// should receive list of cloned switches and their mapping from active ones
			// Create a fake active to clone map
			System.out.println("Murad: Clone Switch to be added: " + j);
			LimeContainer.insertActiveToCloneSwitchMap(j-3, j);
		}

		// loop through classifier to make sure that all needed switches (active/cloned) are there
		// make sure that all required ports are there from active-port table in active classifier 
		// clone switches should not be connected to any VMs!!
		
		// just loop and make sure port number is there, if so, change its type to what its in clone one
		// don't change CONNECTED switch, trigger error!
		// add rules for both active and clone switches to handle packets in and out ghost ports
			// For any packet coming from port G and out to port C
			// 		forward to port C
		// copy tables from active to cloned after adding the high priority rules to both active and clone switches

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
							ghostPort = portNo;
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
					activeFVClassifier.startClone();
					activeFVClassifier.ereaseLimeFlowTable();
					// flush LimeFlowTable for active 

					// setup clone switch
					cloneFVClassifier.setDuplicateSwitch(activeSwID);
					// copy FlowMod table from active to switch and push it the switch
					cloneFVClassifier.insertFlowRuleTable(activeFVClassifier.getFlowRuleTable());
					LinkedList<FVFlowMod> flowModList = cloneFVClassifier.getFlowRuleTable();
					HashMap<Short, ArrayList<FVFlowMod>> cloneLimeFlowTable = new HashMap<>(); 

					// loop through the table to create LimeFlowTable and push it
					for(FVFlowMod flowMod: flowModList){
						short originalPort = -1;
						short originalPriority;
						// check to see if this is an output port action
						for (OFAction action : flowMod.getActions()){
							if(action instanceof OFActionOutput){
								if (cloneFVClassifier.getActivePorts().get(((OFActionOutput) action).getPort()).getType().equals(PortType.EMPTY)){ // this should never return null pointer exception, if so this is a serious problem! 
									originalPort = ((OFActionOutput) action).getPort(); //TODO, make sure that this is clone and won't be affected by its change
									((OFActionOutput) action).setPort(ghostPort);
								}
								break; //Assuming that there is only one output port...	
							}
						}
						
						if(originalPort != -1){
							originalPriority = flowMod.getPriority();
							flowMod.setPriority((short) (originalPriority + 1));
							cloneFVClassifier.sendMsg(flowMod, cloneFVClassifier);
							// now add this to LimeFlowTable
							cloneFVClassifier.addLimeFlowRule(originalPort, flowMod);
							
							// return the original port and priority
							for (OFAction action : flowMod.getActions()){
								if(action instanceof OFActionOutput){
									((OFActionOutput) action).setPort(originalPort);
									break;
								}
							}
							flowMod.setPriority(originalPriority);
						}
						cloneFVClassifier.sendMsg(flowMod, cloneFVClassifier);
					}
					
					// send ghost output rules to both active and clone switches
					OFFlowMod ofFlowMod = new OFFlowMod();
					OFMatch ofMatch = new OFMatch();
					ofMatch.setInputPort(ghostPort);
					ofFlowMod.setMatch(new OFMatch());
					List<OFAction> actionList = new ArrayList<>();
					OFActionOutput ofAction = new OFActionOutput();
					ofAction.setPort(OFPort.OFPP_NORMAL.getValue());
					actionList.add(ofAction);
					ofFlowMod.setActions(actionList);
					activeFVClassifier.sendMsg(ofFlowMod, activeFVClassifier);
					cloneFVClassifier.sendMsg(ofFlowMod, cloneFVClassifier);	
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
