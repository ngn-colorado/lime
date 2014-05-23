package org.flowvisor.message.statistics;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;

public class FVPortStatisticsRequest extends OFPortStatisticsRequest implements
		ClassifiableStatistic, SlicableStatistic {



	@Override
	public void sliceFromController(FVStatisticsRequest msg,
			WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {
		//TODO: implement port stats
		FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
	}

	@Override
	public void classifyFromSwitch(FVStatisticsReply msg,
			WorkerSwitch fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
		
	}

}
