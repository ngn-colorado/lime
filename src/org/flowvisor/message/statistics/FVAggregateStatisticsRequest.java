package org.flowvisor.message.statistics;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.OriginalSwitch;

public class FVAggregateStatisticsRequest extends
		org.openflow.protocol.statistics.OFAggregateStatisticsRequest implements
		SlicableStatistic, ClassifiableStatistic {

	


	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, WorkerSwitch fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg, WorkerSwitch fvClassifier,
			OriginalSwitch fvSlicer) {
		FVMessageUtil.translateXidMsg(msg,fvClassifier, fvSlicer);
		if (!fvClassifier.pollFlowTableStats(msg))
			fvClassifier.sendAggStatsResp(fvSlicer, msg);
		
	}




}
