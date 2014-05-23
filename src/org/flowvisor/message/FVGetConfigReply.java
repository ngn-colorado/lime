package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.OFGetConfigReply;

public class FVGetConfigReply extends OFGetConfigReply implements Classifiable,
		Slicable {

	@Override
	public void classifyFromSwitch(WorkerSwitch fvClassifier) {
		OriginalSwitch fvSlicer = FVMessageUtil.untranslateXid(this, fvClassifier);
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable xid in GetConfigReply: " + this);
			return;
		}
		this.setMissSendLength(fvSlicer.getMissSendLength());
		fvSlicer.sendMsg(this, fvClassifier);
	}

	@Override
	public void sliceFromController(WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}
	
	
}
