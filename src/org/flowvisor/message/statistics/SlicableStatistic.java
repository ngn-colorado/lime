/**
 *
 */
package org.flowvisor.message.statistics;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.OriginalSwitch;

/**
 * @author capveg
 *
 */
public interface SlicableStatistic {

	/**
	 * Given this stat, classifier, and slicer decide how this statistic should
	 * be rewritten coming from the controller
	 *
	 * @param approuvedStats
	 * @param fvClassifier
	 * @param fvSlicer
	 */
	
	public void sliceFromController(FVStatisticsRequest msg, WorkerSwitch fvClassifier, OriginalSwitch fvSlicer);

	/*public void sliceFromController(List<OFStatistics> approvedStats, WorkerSwitch fvClassifier,
			OriginalSwitch fvSlicer) throws StatDisallowedException;*/
}
