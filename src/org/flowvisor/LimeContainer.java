package org.flowvisor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.flowvisor.message.FVFeaturesReply;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFeaturesReply;

/**
 * Keep track of created instances of switches and their info
 * @author Murad Kaplan
 *
 */
public class LimeContainer {
	
	private static int testCounter = 0;
	public static final String MainSlice = " muradSlice"; 
	// list of all switches
	private static HashMap<Long, OFFeaturesReply> allNetworkSwitches = new HashMap<>();
	
	// list of original showing switches to OF controller to always use them to map to the controller
	private static Set<Long>originalSeenSwitches = new HashSet<>();   // by their IDs
	
	// This hash includes original and cloned switches
	private static HashMap<Long, OFFeaturesReply> currentActiveSwitches = new HashMap<>();  // by their IDs
	
	// list of all slicers created
	private static HashMap<Long, FVSlicer> allSlicers = new HashMap<>(); // <swId, FVSlicer>
	
	public static synchronized int getTestCounter(){
		return testCounter;
	}
	
	public static synchronized void addTestCounter(){
		testCounter++;
	}
	
	public static HashMap<Long, OFFeaturesReply> getAllNetworkSwitcher(){
		return allNetworkSwitches;
	}
	
	public static synchronized void addToAllNetworkSwitch(long swId, OFFeaturesReply swInfo){
		allNetworkSwitches.put(swId, swInfo);
	}
	
	/**
	 * Get the switch that OF controller can see
	 * @return
	 */
	public static synchronized Set<Long> getOriginalSeenSwitches(){
		return originalSeenSwitches;
	}
	
	public static synchronized void insertOriginalSeenSwitches(Long swId){
		originalSeenSwitches.add(swId);
	}
	
	public static synchronized HashMap<Long, OFFeaturesReply>  getCurrentActiveSwitches(){
		return currentActiveSwitches;
	}
	
	public static synchronized void addToCurrentActiveSwitches(long swId, OFFeaturesReply swInfo){
		currentActiveSwitches.put(swId, swInfo);
	}
	
	public static synchronized void addSlicer(long sName, FVSlicer fvSlicer){
		allSlicers.put(sName, fvSlicer);
		
	}
	public static synchronized HashMap<Long, FVSlicer> getAllSlicers(){
		return allSlicers;
		
	}
	
	
}
