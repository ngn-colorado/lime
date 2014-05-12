/**
 *
 */
package org.flowvisor.slicer;

import java.util.LinkedHashMap;
import java.util.Map;

import org.flowvisor.classifier.FVClassifier;
import org.openflow.protocol.OFMatch;
import org.openflow.util.LRULinkedHashMap;

/**
 * @author Murad Kaplan
 *
 */
public class LimeMsgTranslator {

	static final int MIN_XID = 256;
	static final int INIT_SIZE = (1 << 12);
	static final int MAX_SIZE = (1 << 14); // must be larger than the max
											// lifetime of an XID * rate of
											// mesgs/sec
	int nextID;
	private LRULinkedHashMap<Integer, LimeMsgData> buffer_idMap;

	public LimeMsgTranslator() {
		this.nextID = MIN_XID;
		this.buffer_idMap = new LRULinkedHashMap<Integer, LimeMsgData>(INIT_SIZE,
				MAX_SIZE);
		
	}

	/**
	 * Return MsgData that matches this buffer_id
	 * @param buffer_i
	 * @return MsgData if found, null otherwise
	 */
	public LimeMsgData untranslate(int buffer_id) {
		return buffer_idMap.get(Integer.valueOf(buffer_id));
	}
	
	/**
	 * Search into map and return MsgData that contains the passed OFMatch
	 * @return LimeMsgData if found, null otherwise
	 */
	public  LimeMsgData untranslateByMatch(OFMatch match){
		LimeMsgData msgData;;
		for (Map.Entry<Integer, LimeMsgData> entry : buffer_idMap.entrySet()){
			msgData = (LimeMsgData)entry.getValue();
			if(msgData.getMatch().equals(match)){
				return msgData;
			}
		}
		return null;
	}
	
	
	public int translate(int buffer_id, FVClassifier fvClassifier, OFMatch ofMatch) {
		int ret = this.nextID++;
		if (nextID < MIN_XID)
			nextID = MIN_XID;
		buffer_idMap.put(Integer.valueOf(ret), new LimeMsgData(buffer_id, fvClassifier, ofMatch));
		return ret;
	}
	
}
