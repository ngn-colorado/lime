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
public class LimeXidTranslator {

	static final int MIN_XID = 256;
	static final int INIT_SIZE = (1 << 12);
	static final int MAX_SIZE = (1 << 14); // must be larger than the max
											// lifetime of an XID * rate of
											// mesgs/sec
	int nextID;
	LRULinkedHashMap<Integer, LimeXidPair> xidMap;

	public LimeXidTranslator() {
		this.nextID = MIN_XID;
		this.xidMap = new LRULinkedHashMap<Integer, LimeXidPair>(INIT_SIZE,
				MAX_SIZE);
		
	}

	public LimeXidPair untranslate(int xid) {
		return xidMap.get(Integer.valueOf(xid));
	}

	public int translate(int xid, FVClassifier fvClassifier) {
		int ret = this.nextID++;
		if (nextID < MIN_XID)
			nextID = MIN_XID;
		xidMap.put(Integer.valueOf(ret), new LimeXidPair(xid, fvClassifier.getDPID()));
		return ret;
	}
	
}
