package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.OFFeaturesRequest;

public class FVFeaturesRequest extends OFFeaturesRequest implements
		Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(WorkerSwitch fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	@Override
	public void sliceFromController(WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {
		FVMessageUtil.translateXidAndSend(this, fvClassifier, fvSlicer);
	}
	
	@Override
	public String toString() {
		return "FVFeaturesRequest []"; 
	}

	
}