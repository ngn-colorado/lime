package edu.colorado.cs.ngn.lime;

import java.util.HashMap;
import java.util.Map;

import net.minidev.json.JSONObject;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.slicer.OriginalSwitch;
import org.flowvisor.slicer.LimeMsgData;
import org.openflow.util.LRULinkedHashMap;

import edu.colorado.cs.ngn.lime.util.DPID;
import edu.colorado.cs.ngn.lime.util.PortInfo;

/**
 * Keep track of created instances of switches and their info
 * Original Switches: include switches that OF controller always see
 * Active Switches: include actual switches in the network and they map to Original Switches. Must be the same number as Original Switches
 * Cloned Switches: include switches to be cloned to. every cloned switches must map to one active switch. Number of Cloned switches <= Active Switches 
 * Working Switches: include active and cloned switches
 * Slicer Switches: Switches that talk with controller. Must be the same number as original switches
 * 
 * 
 * @author Murad Kaplan
 *
 */

public class LimeContainer {
	static final int MIN_XID = 256;
	static final int INIT_SIZE = (1 << 12);
	static final int MAX_SIZE = (1 << 14);
	static int nextID  = MIN_XID;
	static LRULinkedHashMap<Integer, LimeMsgData> buffer_idMap  = new LRULinkedHashMap<Integer, LimeMsgData>(INIT_SIZE, MAX_SIZE);


	public static final String OriginalSwitch = "originalSwitch"; 

	// all switches in the network and connecting LIME 
	private static HashMap<Long, WorkerSwitch> allWorkingSwitches = new HashMap<>();

	// list of original showing switches to OF controller to always use them to map to the controller
	private static HashMap<Long, LimeSwitch> originalSwitchContainer = new HashMap<>();

	// list of active switches.
	//private static HashMap<Long, LimeSwitch> activeSwitchContainer = new HashMap<>();

	// list of clone switches. those must be received my the time migration happens. And they must have ghost port identified.
	private static HashMap<Long, LimeSwitch> cloneSwitchContainer = new HashMap<>();

	private static HashMap<Long, Long> activeToOriginalSwitchMap = new HashMap<>();

	private static HashMap<Long, Long> activeToCloneSwitchMap = new HashMap<>();

	// list of all slicers created
	private static HashMap<Long, OriginalSwitch> allSlicers = new HashMap<>(); // <swId (last switch that switch that was using this slice, OriginalSwitch>
	
	// maps dpids of switches to maps of connected host ports to host mac addresses
	private static HashMap<DPID, HashMap<Short, String>> dpidToMacMap = new HashMap<DPID, HashMap<Short, String>>();
	
	// maps dpids to the original port table for the switch
	private static HashMap<DPID, HashMap<Short, PortInfo>> dpidToOriginalPortInfoMap = new HashMap<DPID, HashMap<Short, PortInfo>>();

	public static HashMap<Long, WorkerSwitch> getAllWorkingSwitches(){
		return allWorkingSwitches;
	}

	/**
	 * Get the original switch that OF controller can see
	 * @return
	 */
	public static HashMap<Long, LimeSwitch>  getOriginalSwitchContainer(){
		return originalSwitchContainer;
	}
	
	public static HashMap<DPID, HashMap<Short, String>> getDpidToMacMap(){
		return dpidToMacMap;
	}
	
	public static HashMap<DPID, HashMap<Short, PortInfo>> getDpidToOriginalPortInfoMap(){
		return dpidToOriginalPortInfoMap;
	}

	public static void addOriginalSwitch(long swID, HashMap<Short, PortInfo> portTable){
		originalSwitchContainer.put(swID, new LimeSwitch(portTable));
	}

	/*public static HashMap<Long, LimeSwitch>  getActiveSwitchContainer(){
		return activeSwitchContainer;
	}

	static void addActiveSwitch(long swID, HashMap<Short, PortInfo> portTable){
		activeSwitchContainer.put(swID, new LimeSwitch(portTable));
	}*/

	public static HashMap<Long, LimeSwitch>  getCloneSwitchContainer(){
		return cloneSwitchContainer;
	}

	public static void addCloneSwitch(long swID, HashMap<Short, PortInfo> portTable){
		cloneSwitchContainer.put(swID, new LimeSwitch(portTable));
	}

	public static synchronized void addWorkingSwitch(long swId, WorkerSwitch swClassifier){
		System.out.println("MURAD: LIMEContainer, Added working switch " + swId);
		allWorkingSwitches.put(swId, swClassifier);
	}	

	public static synchronized HashMap<Long,Long> getActiveToOriginalSwitchMap(){
		return activeToOriginalSwitchMap;
	}

	public static synchronized void insertActiveToOriginalSwitchMap(long swActive, long swOriginal){
		if(!originalSwitchContainer.containsKey(swOriginal)){ ////////////
			System.out.println("MURAD: LimeContainer, ERROR!!!!!!!!!!!! Can't add Active Switch " + swActive + " because Original Switch " + swOriginal + " is not found");  // TODO through exception
		}
		else{
			activeToOriginalSwitchMap.put(swActive, swOriginal);
		}
	}


	public static synchronized HashMap<Long,Long> getActiveToCloneSwitchMap(){
		return activeToCloneSwitchMap;
	}

	/**
	 * Return Active switch ID that map to this clone switch
	 * @param cloneSwitchID
	 * @return active switch ID, -1 otherwise
	 */
	public static long getActiveSwitchForThisCloneSwitch(long cloneSwitchID){
		for (Map.Entry entry : activeToCloneSwitchMap.entrySet()){
			if (cloneSwitchID == (long)entry.getValue() )
				return (long)entry.getKey();
		}
		return -1;

	}

	public static synchronized void insertActiveToCloneSwitchMap(long swActive, long swClone){
		if(!activeToOriginalSwitchMap.containsKey(swActive)){
			System.out.println("MURAD: LimeContainer, ERROR!!!!!!!!!!!! Can't fine active Switch " + swActive);  // TODO through exception
		}
		if(!cloneSwitchContainer.containsKey(swClone)){ ////////////
			System.out.println("MURAD: LimeContainer, ERROR!!!!!!! can't find clone switch " +swClone);
		}
		else{
			activeToCloneSwitchMap.put(swActive, swClone);
		}
	}

	public static synchronized void addSlicer(long sName, OriginalSwitch fvSlicer){
		allSlicers.put(sName, fvSlicer);

	}
	public static synchronized HashMap<Long, OriginalSwitch> getAllSlicers(){
		return allSlicers;
	}

	static synchronized void insertActiveToCloneSwitchMap(JSONObject map){

	}

	/**
	 * Translate and add to the map to retrieve later
	 * @param msg
	 * @param fvClassifier
	 */
	/*private static synchronized int translateXid(int buffer_id, WorkerSwitch fvClassifier){
		int ret = nextID++;
		if (nextID < MIN_XID)
			nextID = MIN_XID;
		buffer_idMap.put(Integer.valueOf(ret), new LimeMsgBuffer_idPair(buffer_id, fvClassifier.getDPID()));
		return ret;
	}*/

	/**
	 * Untranslate and delete
	 * @param xid
	 * @return
	 */
	/*private static synchronized LimeMsgBuffer_idPair untranslate(int buffer_id) {
		LimeMsgBuffer_idPair idPair = buffer_idMap.get(Integer.valueOf(buffer_id));
		//xidMap.remove(xid);
		return idPair;
	}*/

}
