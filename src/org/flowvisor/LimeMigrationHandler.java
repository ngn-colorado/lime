/**
 * 
 */
package org.flowvisor;

import java.util.HashMap;
import java.util.Map;

import org.flowvisor.PortInfo.PortType;
import org.flowvisor.classifier.FVClassifier;
import org.openflow.protocol.OFFeaturesRequest;

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
		for(long j=5; j<8; j++){
			HashMap<Short, PortInfo> portTable = new HashMap<>();
			for(short i= 1; i<4; i++){
				PortInfo pInfo = new PortInfo(PortType.EMPTY, null, null);
				portTable.put(i, pInfo);
			}

			PortInfo pInfo = new PortInfo(PortType.GHOST, null, null);
			portTable.put((short) 4, pInfo);

			LimeContainer.addCloneSwitch(j, portTable);

			// should receive list of cloned switches and their mapping from active ones
			// Create a fake active to clone map
			System.out.println("Murad: Clone Switch to be added: " + j);
			LimeContainer.insertActiveToCloneSwitchMap(j-3, j);
		}

		// loop through classifier to make sure that all needed switches (active/cloned) are there
		// make sure that all required ports are there from active-port table in classifier 
		// just loop and make sure port number is there, if so, change its type to what its in clone one
		// don't change CONNECTED switch, trigger error!

		for (Map.Entry entry : LimeContainer.getActiveToCloneSwitchMap().entrySet()) {
			if((LimeContainer.getAllWorkingSwitches().containsKey(entry.getKey())) &&
					(LimeContainer.getAllWorkingSwitches().containsKey(entry.getValue()))){
				long activeSwID = (long) entry.getKey();
				FVClassifier activeFVClassifier = LimeContainer.getAllWorkingSwitches().get(activeSwID);
				long cloneSwID  = (long) entry.getValue();
				FVClassifier cloneFVClassifier = LimeContainer.getAllWorkingSwitches().get(cloneSwID);
				boolean portMissing = false;
				HashMap<Short, PortInfo> portTable = LimeContainer.getCloneSwitchContainer().get(cloneSwID).getPortTable();
				for (Map.Entry portEntry : portTable.entrySet()){
					short portNo = (short) portEntry.getKey();
					PortInfo pInfo = (PortInfo) portEntry.getValue();
					if(activeFVClassifier.getAcrivePorts().containsKey(portNo) && cloneFVClassifier.getAcrivePorts().containsKey(portNo)){
						if(!activeFVClassifier.getAcrivePorts().get(portNo).getType().equals(PortType.CONNECTED)){
							activeFVClassifier.getAcrivePorts().get(portNo).setType(pInfo.getType());
						}
					}
					else{
						portMissing = true;
						System.out.println("MURAD: LimeMigrationHandler, ERROR, port " + portNo+ " is not found"); 
						break;
					}
				}
				if (!portMissing){
					// setup active switch
					LimeContainer.getAllWorkingSwitches().get(activeSwID).setDuplicateSwitch(cloneSwID);
					LimeContainer.getAllWorkingSwitches().get(activeSwID).startClone();

					// setup clone switch
					LimeContainer.getAllWorkingSwitches().get(cloneSwID).setDuplicateSwitch(activeSwID);
				}
				else{
					System.out.println("MURAD: ERROR finding port!!");
					break;
				}

			}
			else{
				System.out.println("MURAD: ERROR finding Active to Clone switches!!!!!!!!!!: " + entry.getKey() + " " + entry.getValue());
				break;
			}
		}
	}
	
	private void updatePort(FVClassifier fvClassifier, short port, PortType pType){
		
	}
}
