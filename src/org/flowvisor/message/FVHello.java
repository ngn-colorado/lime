/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.slicer.OriginalSwitch;

/**
 * @author capveg
 *
 */
public class FVHello extends org.openflow.protocol.OFHello implements
		Classifiable, Slicable, TopologyControllable {

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.
	 * classifier.WorkerSwitch)
	 */
	@Override
	public void classifyFromSwitch(WorkerSwitch fvClassifier) {
		// silently drop all Hello msgs
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.flowvisor.message.Slicable#sliceFromController(org.flowvisor.classifier
	 * .WorkerSwitch, org.flowvisor.slicer.OriginalSwitch)
	 */
	@Override
	public void sliceFromController(WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {
		// silently drop all Hello msgs
	}

	@Override
	public void topologyController(TopologyConnection topologyConnection) {
		// silently drop all Hello msgs
	}

}
