/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;

/**
 * The interface for classifying this message and sending it on to the correct
 * OriginalSwitch instance
 *
 * Does switch-specific, slice agnostic rewriting
 *
 * @author capveg
 *
 */
public interface Classifiable {

	/**
	 * Given a message from a switch, send it to the appropriate OriginalSwitch
	 * instance(s)
	 *
	 * Possibly do some rewriting, record state, or even drop
	 *
	 * @param fvClassifier
	 *            Switch state
	 */
	public void classifyFromSwitch(WorkerSwitch fvClassifier);
}
