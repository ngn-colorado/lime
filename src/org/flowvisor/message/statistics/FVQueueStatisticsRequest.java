package org.flowvisor.message.statistics;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.statistics.OFQueueStatisticsRequest;

public class FVQueueStatisticsRequest extends OFQueueStatisticsRequest
		implements ClassifiableStatistic, SlicableStatistic, Cloneable {

	
	 @Override
     public void classifyFromSwitch(FVStatisticsReply msg, WorkerSwitch fvClassifier) {
             FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
                             + msg);
     }

     @Override
     public void sliceFromController(FVStatisticsRequest msg, WorkerSwitch fvClassifier,
                     OriginalSwitch fvSlicer) {
             FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
     }


}
