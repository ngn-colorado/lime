package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.slicer.OriginalSwitch;

public class FVEchoReply extends org.openflow.protocol.OFEchoReply implements
		Slicable, Classifiable {

	@Override
	public void sliceFromController(WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {
		fvSlicer.registerPong();
	}

	@Override
	public void classifyFromSwitch(WorkerSwitch fvClassifier) {
		fvClassifier.registerPong();
	}
	
	
	@Override
	public String toString() {
		return "FVEchoReply [ payload=" + this.getPayload() + "]";
	}
	
}
