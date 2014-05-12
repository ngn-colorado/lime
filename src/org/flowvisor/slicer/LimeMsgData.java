/**
 *
 */
package org.flowvisor.slicer;

import org.flowvisor.classifier.FVClassifier;
import org.openflow.protocol.OFMatch;


/**
 * @author Murad Kaplan
 *
 */
public class LimeMsgData {
	int 			buffer_id;
	FVClassifier 	classifierID;
	OFMatch 		ofMatch;

	public LimeMsgData(int bid, FVClassifier fvClassifier, OFMatch match) {
		this.buffer_id 		= bid;
		this.classifierID 	= fvClassifier;
		this.ofMatch		= match;
	}

	public int getBuffer_id() {
		return buffer_id;
	}

	/*public void setBuffer_id(int xid) {
		this.buffer_id = xid;
	}*/

	public OFMatch getMatch(){
		return ofMatch;
	}
	/**
	 * @return the classifier ID
	 */
	public FVClassifier getClassifier() {
		return classifierID;
	}
}
