package org.flowvisor.message.actions;

import java.util.List;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionVendor;

public class FVActionVendor extends OFActionVendor implements SlicableAction {

	@Override
	public void slice(List<OFAction> approvedActions, OFMatch match,
			WorkerSwitch fvClassifier, OriginalSwitch fvSlicer)
			throws ActionDisallowedException {
		FVLog.log(LogLevel.CRIT, fvSlicer,
				"action slicing unimplemented for type: " + this);
		approvedActions.add(this);
	}

}
