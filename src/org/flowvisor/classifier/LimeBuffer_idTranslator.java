/**
 *
 */
package org.flowvisor.classifier;

import org.flowvisor.slicer.FVSlicer;
import org.openflow.util.LRULinkedHashMap;

/**
 * @author Murad Kaplan
 *
 */
public class LimeBuffer_idTranslator {

	static final int MIN_XID = 256;
	static final int INIT_SIZE = (1 << 12);
	static final int MAX_SIZE = (1 << 14); // must be larger than the max
											// lifetime of an XID * rate of
											// mesgs/sec
	int nextID;
	LRULinkedHashMap<Integer, LimeMsgBuffer_idPair> Buffer_idMap;

	public LimeBuffer_idTranslator() {
		this.nextID = MIN_XID;
		this.Buffer_idMap = new LRULinkedHashMap<Integer, LimeMsgBuffer_idPair>(INIT_SIZE,
				MAX_SIZE);
		
	}

	public LimeMsgBuffer_idPair untranslate(int buffer_id) {
		return Buffer_idMap.get(Integer.valueOf(buffer_id));
	}
	
	

	public int translate(int xid, FVClassifier fvClassifier) {
		int ret = this.nextID++;
		if (nextID < MIN_XID)
			nextID = MIN_XID;
		Buffer_idMap.put(Integer.valueOf(ret), new LimeMsgBuffer_idPair(xid, fvClassifier));
		return ret;
	}
	
}
