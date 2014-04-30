/**
 *
 */
package org.flowvisor.classifier;


/**
 * @author Murad Kaplan
 *
 */
public class LimeXidPair {
	int xid;
	long classifierID;

	public LimeXidPair(int xid, long classifierID) {
		this.xid = xid;
		this.classifierID = classifierID;
	}

	public int getXid() {
		return xid;
	}

	public void setXid(int xid) {
		this.xid = xid;
	}

	/**
	 * @return the classifier ID
	 */
	public long getClassifierID() {
		return classifierID;
	}
}
