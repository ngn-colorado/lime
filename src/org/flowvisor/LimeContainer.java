package org.flowvisor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFeaturesReply;

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
	public static final String MainSlice = "LIMESlice"; 

	// list of all switches
	private static Set<Long> allWorkingSwitches = new HashSet<>();

	// list of original showing switches to OF controller to always use them to map to the controller
	private static Set<Long>originalSeenSwitches = new HashSet<>();   // by their IDs


	private static HashMap<Long, Long> activeToOriginalSwitchMap = new HashMap<>();


	private static HashMap<Long, Long> activeToCloneSwitchMap = new HashMap<>();

	// list of all slicers created
	private static HashMap<Long, FVSlicer> allSlicers = new HashMap<>(); // <swId, FVSlicer>

	public static Set<Long> getAllWorkingSwitcher(){
		return allWorkingSwitches;
	}

	public static synchronized void addWorkingSwitch(long swId){
		allWorkingSwitches.add(swId);
	}

	/**
	 * Get the original switch that OF controller can see
	 * @return
	 */
	public static synchronized Set<Long> getOriginalSeenSwitches(){
		return originalSeenSwitches;
	}

	static synchronized void insertOriginalSeenSwitches(Long swId){
		originalSeenSwitches.add(swId);
	}

	public static synchronized HashMap<Long,Long> getActiveToOriginalSwitchMap(){
		return activeToOriginalSwitchMap;
	}

	public static synchronized void insertActiveToOriginalSwitchMap(long swActive, long swOriginal){
		if(!originalSeenSwitches.contains(swOriginal)){
			System.out.println("ERROR!!!!!!!!!!!! Can't add Active Switch. Original Switch is not found");  // TODO through exception
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


}
