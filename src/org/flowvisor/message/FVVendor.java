package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.OFVendor;

public class FVVendor extends OFVendor implements Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(WorkerSwitch fvClassifier) {
		// Just blindly forward vendor messages
		FVMessageUtil.untranslateXidAndSend(this, fvClassifier);
	}

	@Override
	public void sliceFromController(WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {
		// Just blindly forward vendor messages
		FVMessageUtil.translateXidAndSend(this, fvClassifier, fvSlicer);
	}

}
