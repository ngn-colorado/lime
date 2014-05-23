package org.flowvisor.message.statistics;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.statistics.OFVendorStatistics;

public class FVVendorStatistics extends OFVendorStatistics implements
		SlicableStatistic, ClassifiableStatistic {



	@Override
	public void classifyFromSwitch(FVStatisticsReply msg,
			WorkerSwitch fvClassifier) {
		FVMessageUtil.untranslateXidAndSend(msg, fvClassifier);
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg,
			WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {
		FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
		
	}

}
