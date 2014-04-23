package org.flowvisor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import net.minidev.json.JSONObject;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;

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

	private static int testCounter = 0;
	public static final String MainSlice = "slice1"; 

	// all switches in the network and connecting LIME 
	private static HashMap<Long, FVClassifier> allWorkingSwitches = new HashMap<>();
	
	// list of original showing switches to OF controller to always use them to map to the controller
	private static Hashtable<Long, LimeSwitch> originalSwitchContainer = new Hashtable<>();

	private static HashMap<Long, Long> activeToOriginalSwitchMap = new HashMap<>();


	private static HashMap<Long, Long> activeToCloneSwitchMap = new HashMap<>();

	// list of all slicers created
	private static HashMap<Long, FVSlicer> allSlicers = new HashMap<>(); // <swId (last switch that switch that was using this slice, FVSlicer> 

	public static HashMap<Long, FVClassifier> getAllWorkingSwitcher(){
		return allWorkingSwitches;
	}

	/**
	 * Get the original switch that OF controller can see
	 * @return
	 */
	public static Hashtable<Long, LimeSwitch>  getOriginalSwitchContainer(){
		return originalSwitchContainer;
	}
	
	static void addOriginalSwitch(long swID, Hashtable<Integer, PortInfo> portTable){
		originalSwitchContainer.put(swID, new LimeSwitch(portTable));
	}
	
	
	public static synchronized void addWorkingSwitch(long swId, FVClassifier swClassifier){
		System.out.println("MURAD: Added working switch " + swId);
		allWorkingSwitches.put(swId, swClassifier);
	}	

	public static synchronized HashMap<Long,Long> getActiveToOriginalSwitchMap(){
		return activeToOriginalSwitchMap;
	}

	public static synchronized void insertActiveToOriginalSwitchMap(long swActive, long swOriginal){
		if(!originalSwitchContainer.contains(swOriginal)){
			System.out.println("MURAD: ERROR!!!!!!!!!!!! Can't add Active Switch " + swActive + " Original Switch " + swOriginal + " is not found");  // TODO through exception
		}
		else{
			activeToOriginalSwitchMap.put(swActive, swOriginal);
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


}
