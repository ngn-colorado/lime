package org.flowvisor;

import java.util.HashMap;
import java.util.Map;

import net.minidev.json.JSONObject;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.LimeXidPair;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.util.LRULinkedHashMap;

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
	static LRULinkedHashMap<Integer, LimeXidPair> xidMap  = new LRULinkedHashMap<Integer, LimeXidPair>(INIT_SIZE, MAX_SIZE);
	

	public static final String MainSlice = "slice1"; 

	// all switches in the network and connecting LIME 
	private static HashMap<Long, FVClassifier> allWorkingSwitches = new HashMap<>();
	
	// list of original showing switches to OF controller to always use them to map to the controller
	private static HashMap<Long, LimeSwitch> originalSwitchContainer = new HashMap<>();

	// list of active switches.
	private static HashMap<Long, LimeSwitch> activeSwitchContainer = new HashMap<>();
		
	// list of clone switches. those must be received my the time migration happens. And they must have ghost port identified.
	private static HashMap<Long, LimeSwitch> cloneSwitchContainer = new HashMap<>();
		
	private static HashMap<Long, Long> activeToOriginalSwitchMap = new HashMap<>();

	private static HashMap<Long, Long> activeToCloneSwitchMap = new HashMap<>();

	private static HashMap<Integer, Long> xIDMapper = new HashMap<>(); // to map message to the classifier
	
	// list of all slicers created
	private static HashMap<Long, FVSlicer> allSlicers = new HashMap<>(); // <swId (last switch that switch that was using this slice, FVSlicer> 

	public static HashMap<Long, FVClassifier> getAllWorkingSwitches(){
		return allWorkingSwitches;
	}

	/**
	 * Get the original switch that OF controller can see
	 * @return
	 */
	public static HashMap<Long, LimeSwitch>  getOriginalSwitchContainer(){
		return originalSwitchContainer;
	}
	
	static void addOriginalSwitch(long swID, HashMap<Short, PortInfo> portTable){
		originalSwitchContainer.put(swID, new LimeSwitch(portTable));
	}
	
	public static HashMap<Long, LimeSwitch>  getActiveSwitchContainer(){
		return activeSwitchContainer;
	}
	
	static void addActiveSwitch(long swID, HashMap<Short, PortInfo> portTable){
		activeSwitchContainer.put(swID, new LimeSwitch(portTable));
	}
	
	public static HashMap<Long, LimeSwitch>  getCloneSwitchContainer(){
		return cloneSwitchContainer;
	}
	
	static void addCloneSwitch(long swID, HashMap<Short, PortInfo> portTable){
		cloneSwitchContainer.put(swID, new LimeSwitch(portTable));
	}
	
	public static synchronized void addWorkingSwitch(long swId, FVClassifier swClassifier){
		System.out.println("MURAD: Added working switch " + swId);
		allWorkingSwitches.put(swId, swClassifier);
	}	

	public static synchronized HashMap<Long,Long> getActiveToOriginalSwitchMap(){
		return activeToOriginalSwitchMap;
	}

	public static synchronized void insertActiveToOriginalSwitchMap(long swActive, long swOriginal){
		if(!originalSwitchContainer.containsKey(swOriginal)){ ////////////
			System.out.println("MURAD: ERROR!!!!!!!!!!!! Can't add Active Switch " + swActive + " Original Switch " + swOriginal + " is not found");  // TODO through exception
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
	public static long getActiveSwitchForThisCloneSwithc(long cloneSwitchID){
		for (Map.Entry entry : activeToCloneSwitchMap.entrySet()){
			if (cloneSwitchID == (long)entry.getValue() )
				return (long)entry.getKey();
		}
		return -1;
		
	}
	
	static synchronized void insertActiveToCloneSwitchMap(long swActive, long swClone){
		if((!activeToOriginalSwitchMap.containsKey(swActive)) || (!cloneSwitchContainer.containsKey(swClone))){ ////////////
			System.out.println("MURAD: ERROR!!!!!!!!!!!! Can't add Clone Switch " + swClone + " and Active Switch " + swActive);  // TODO through exception
		}
		else{
			activeToCloneSwitchMap.put(swActive, swClone);
		}
	}

	public static synchronized void addSlicer(long sName, FVSlicer fvSlicer){
		allSlicers.put(sName, fvSlicer);

	}
	public static synchronized HashMap<Long, FVSlicer> getAllSlicers(){
		return allSlicers;
	}
	
	static synchronized void insertActiveToCloneSwitchMap(JSONObject map){
		
	}

	/**
	 * Translate and add to the map to retrieve later
	 * @param msg
	 * @param fvClassifier
	 */
	/*public static synchronized int translateXid(int xid, FVClassifier fvClassifier){
		int ret = nextID++;
		if (nextID < MIN_XID)
			nextID = MIN_XID;
		xidMap.put(Integer.valueOf(ret), new LimeXidPair(xid, fvClassifier.getDPID()));
		return ret;
	}*/
	
	/**
	 * Untranslate and delete
	 * @param xid
	 * @return
	 */
	/*public static synchronized LimeXidPair untranslate(int xid) {
		LimeXidPair idPair = xidMap.get(Integer.valueOf(xid));
		//xidMap.remove(xid);
		return idPair;
	}*/

}
