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
public class FVBarrierReply extends org.openflow.protocol.OFBarrierReply
		implements Classifiable, Slicable {

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.
	 * classifier.WorkerSwitch)
	 */
	@Override
	public void classifyFromSwitch(WorkerSwitch fvClassifier) {
		FVMessageUtil.untranslateXidAndSend(this, fvClassifier);
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
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	
}
