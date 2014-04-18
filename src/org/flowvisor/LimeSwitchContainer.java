package org.flowvisor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.flowvisor.message.FVFeaturesReply;
import org.flowvisor.slicer.FVSlicer;

/**
 * Keep track of created instances of switches and their info
 * @author Murad Kaplan
 *
 */
public class LimeSwitchContainer {
	// list of all switches
	private static HashMap<String, FVFeaturesReply> allNetworkSwitches = new HashMap<>();
	
	// list of only showing switches to OF controller
	private static Set<String> showingSwitches = new HashSet<>();
	
	// list of all classifiers created
	private static HashMap<String, FVFeaturesReply> allNetworkClassifiers = new HashMap<>();
	
	// list of all slicers created
	private static HashMap<String, FVSlicer> allNetworkSlicers = new HashMap<>();
	
}
