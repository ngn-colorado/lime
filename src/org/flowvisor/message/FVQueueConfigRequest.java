package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFQueueConfigRequest;


public class FVQueueConfigRequest extends OFQueueConfigRequest implements
		Classifiable, Slicable  {

	@Override
	public void sliceFromController(WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {
		if (!fvSlicer.portInSlice(this.port)) {
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
					OFBadRequestCode.OFPBRC_EPERM, this), fvClassifier);
			return;
		}
		
		FVMessageUtil.translateXidAndSend(this, fvClassifier, fvSlicer);
			
	}

	@Override
	public void classifyFromSwitch(WorkerSwitch fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

}
