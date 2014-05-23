/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.slicer.OriginalSwitch;


/**
 * @author capveg
 *
 */
public class FVBarrierRequest extends org.openflow.protocol.OFBarrierRequest
		implements Slicable, Classifiable {

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.flowvisor.message.Slicable#sliceFromController(org.flowvisor.classifier
	 * .WorkerSwitch, org.flowvisor.slicer.OriginalSwitch)
	 */
	@Override
	public void sliceFromController(WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {
		FVMessageUtil.translateXidAndSend(this, fvClassifier, fvSlicer);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.
	 * classifier.WorkerSwitch)
	 */
	@Override
	public void classifyFromSwitch(WorkerSwitch fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	
	@Override
	public String toString() {
		return "FVBarrierRequest []";
	}
}
