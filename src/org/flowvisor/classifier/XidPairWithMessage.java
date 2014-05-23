package org.flowvisor.classifier;

import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.OFMessage;

public class XidPairWithMessage extends XidPair {

	OFMessage msg;
	OriginalSwitch fvSlicer;

	public XidPairWithMessage(XidPair xidPair, OFMessage ofMessage) {
		super(xidPair.xid, xidPair.sliceName);
		this.msg = ofMessage;
	}
	
	public OFMessage getOFMessage() {
		return msg;
	}

	public void setSlicer(OriginalSwitch slicer) {
		this.fvSlicer = slicer;
		
	}

	public OriginalSwitch getSlicer() {
		return fvSlicer;
	}
	
	

}
