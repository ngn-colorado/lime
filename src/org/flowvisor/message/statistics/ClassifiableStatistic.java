/**
 *
 */
package org.flowvisor.message.statistics;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.message.FVStatisticsReply;

/**
 * @author capveg
 *
 */
public interface ClassifiableStatistic {
	/**
	 * Given this stat and the slicer and classifier, figure out which slice this message is
	 * for, rewrite anything as necessary, and send it onto the slice's
	 * controller
	 *
	 * @param approuvedStats
	 * @param fvClassifier
	 * @param fvSlicer
	 */
	public void classifyFromSwitch(FVStatisticsReply msg, WorkerSwitch fvClassifier);
	
	
	/*public void classifyFromSwitch(OFMessage original, List<OFStatistics> approvedStats, WorkerSwitch fvClassifier,
			OriginalSwitch fvSlicer) throws StatDisallowedException;*/
}
