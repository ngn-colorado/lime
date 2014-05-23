package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.slicer.OriginalSwitch;

/**
 * Interface for slice specific, message specific rewriting
 *
 * @author capveg
 *
 */

public interface Slicable {
	/**
	 * Send a sliced version of this message to the switch
	 *
	 * Sliced could mean rewritten or not sent at all (if the policy does not
	 * allow it)
	 *
	 * @param fvClassifier
	 *            The potential destination
	 * @param fvSlicer
	 *            The slicing policy and source of the message
	 */
	public void sliceFromController(WorkerSwitch fvClassifier, OriginalSwitch fvSlicer);
}
