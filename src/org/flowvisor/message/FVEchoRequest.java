/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.slicer.OriginalSwitch;


/**
 * Given an echo request, just send an immediate response from the fv
 *
 * FIXME consider sending these all the way through instead of faking NEED to
 * update regression tests
 *
 * @author capveg
 *
 */
public class FVEchoRequest extends org.openflow.protocol.OFEchoRequest
		implements Classifiable, Slicable, TopologyControllable {

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.
	 * classifier.WorkerSwitch)
	 */
	@Override
	public void classifyFromSwitch(WorkerSwitch fvClassifier) {
		FVEchoReply reply = new FVEchoReply();
		reply.setXid(this.getXid());
		fvClassifier.sendMsg(reply, fvClassifier);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.flowvisor.message.Slicable#sliceFromController(org.flowvisor.classifier
	 * .WorkerSwitch, org.flowvisor.slicer.OriginalSwitch)
	 */
	@Override
	public void sliceFromController(WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {
		fvSlicer.sendMsg(makeReply(), fvSlicer);
	}

	@Override
	public void topologyController(TopologyConnection topologyConnection) {
		//System.out.println("MURAD: Send to controller an Echo!!!!");
		topologyConnection.sendMsg(makeReply(), topologyConnection);
	}

	FVEchoReply makeReply() {
		FVEchoReply reply = new FVEchoReply();
		reply.setLength(this.getLength());
		reply.setXid(this.getXid());
		reply.setPayload(this.getPayload());
		return reply;
	}
	
	@Override
	public String toString() {
		return "FVEchoRequest [ payload=" + this.getPayload() + "]";
	}

}
