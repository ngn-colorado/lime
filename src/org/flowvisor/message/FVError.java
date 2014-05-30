/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.OFMessage;

/**
 * @author capveg
 *
 */
public class FVError extends org.openflow.protocol.OFError implements
		Classifiable, Slicable {

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.flowvisor.message.Classifiable#classifyFromSwitch(org.flowvisor.
	 * classifier.WorkerSwitch)
	 */
	@Override
	public void classifyFromSwitch(WorkerSwitch workerSwitch) {
		System.out.println("MURAD:, FVError, from " + workerSwitch.getName());
		if (this.errorType == (short) OFErrorType.OFPET_BAD_ACTION.ordinal()){
			System.out.println("MURAD:, FVError, because its bad action");
		}
		if (this.errorType == (short) OFErrorType.OFPET_FLOW_MOD_FAILED.ordinal()) {
			System.out.println("MURAD:, FVError, because FLOW MOD failed");
		}
		OriginalSwitch fvSlicer = FVMessageUtil.untranslateXid(this, workerSwitch);
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, workerSwitch,
					"dropping msg with unknown xid: " + this);
			return;
		}
		if (this.errorType == (short) OFErrorType.OFPET_BAD_ACTION.ordinal() 
				|| this.errorType == (short) OFErrorType.OFPET_FLOW_MOD_FAILED.ordinal()) {
			fvSlicer.decrementFlowRules();
		}
		fvSlicer.sendMsg(this, workerSwitch);
	};

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.flowvisor.message.Slicable#sliceFromController(org.flowvisor.classifier
	 * .WorkerSwitch, org.flowvisor.slicer.OriginalSwitch)
	 */
	@Override
	public void sliceFromController(WorkerSwitch workerSwitch, OriginalSwitch fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String ret = /*super.toString() + */  "c=" + this.getErrorCode() + ";t="
				+ getErrorType();
		
		OFMessage offendingMsg = null;
		if (!isErrorIsAscii() && (offendingMsg = getOffendingMsg()) != null)
			ret += ";msg=" + offendingMsg.toString();
		
		if (error != null) {
			if (errorIsAscii)
				ret += ";err=" + new String(error);
			else
				ret += ";err=[" + error.length + "]";
		} else
			ret += ";msg=NONE(!?)";
		return ret;
	}

}
