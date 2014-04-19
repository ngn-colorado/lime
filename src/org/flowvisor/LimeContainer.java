package org.flowvisor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.sql.rowset.spi.SyncResolver;

import org.flowvisor.message.FVFeaturesReply;
import org.flowvisor.slicer.FVSlicer;

/**
 * Keep track of created instances of switches and their info
 * @author Murad Kaplan
 *
 */
public class LimeContainer {
	
	private static int testCounter = 0;
	// list of all switches
	private static HashMap<String, FVFeaturesReply> allNetworkSwitches = new HashMap<>();
	
	// list of only showing switches to OF controller
	private static Set<String> seenSwitches = new HashSet<>();
	
	// list of all classifiers created
	private static HashMap<String, FVFeaturesReply> allClassifiers = new HashMap<>();
	
	// list of all slicers created
	private static HashMap<String, FVSlicer> allSlicers = new HashMap<>();
	
	public static synchronized int getTestCounter(){
		return testCounter;
	}
	
	public static synchronized void addTestCounter(){
		testCounter++;
	}
	public static HashMap<String, FVFeaturesReply> getAllNetworkSwitcher(){
		return allNetworkSwitches;
	}
	
	public static void addNetworkSwitch(String swId, FVFeaturesReply swInfo){
		
	}
	
	public static Set<String> getSeenSwitches(){
		return seenSwitches;
	}
	
	public static HashMap<String, FVFeaturesReply> getAllClassifiers(){
		return allClassifiers;
	}
	
	public static HashMap<String, FVSlicer> getAllSlicers(){
		return allSlicers;
		
	}
	
	
}
