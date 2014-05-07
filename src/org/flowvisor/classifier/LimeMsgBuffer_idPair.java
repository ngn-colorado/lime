/**
 *
 */
package org.flowvisor.classifier;


/**
 * @author Murad Kaplan
 *
 */
public class LimeMsgBuffer_idPair {
	int Buffer_id;
	FVClassifier classifierID;

	public LimeMsgBuffer_idPair(int xid, FVClassifier fvClassifier) {
		this.Buffer_id = xid;
		this.classifierID = fvClassifier;
	}

	public int getBuffer_id() {
		return Buffer_id;
	}

	public void setBuffer_id(int xid) {
		this.Buffer_id = xid;
	}

	/**
	 * @return the classifier ID
	 */
	public FVClassifier getClassifier() {
		return classifierID;
	}
}
