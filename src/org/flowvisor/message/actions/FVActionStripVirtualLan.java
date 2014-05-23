package org.flowvisor.message.actions;

import java.util.Iterator;
import java.util.List;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowSpaceRuleStore;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionStripVirtualLan;

public class FVActionStripVirtualLan extends OFActionStripVirtualLan implements
		SlicableAction {

	@Override
	public void slice(List<OFAction> approvedActions, OFMatch match,
			WorkerSwitch fvClassifier, OriginalSwitch fvSlicer)
			throws ActionDisallowedException {
		FVMatch neoMatch = new FVMatch(match);
		neoMatch.setDataLayerVirtualLan(FlowSpaceRuleStore.ANY_VLAN_ID);
		List<FlowEntry> flowEntries = fvClassifier.getSwitchFlowMap().matches(fvClassifier.getDPID(), neoMatch);
		for (FlowEntry fe : flowEntries) {
			Iterator<OFAction> it = fe.getActionsList().iterator();
			while (it.hasNext()) {
				OFAction act = it.next();
				if (act instanceof SliceAction) {
					SliceAction action = (SliceAction) act;
					if (action.getSliceName().equals(fvSlicer.getSliceName())) {
						FVLog.log(LogLevel.DEBUG, fvSlicer, "Approving " + this + 
								" for " + match);
						approvedActions.add(this);
						return;
					}
				}
			}
		}
		throw new ActionDisallowedException(
				"Slice " + fvSlicer.getSliceName() + " may not strip the vlan tag.", 
				OFBadActionCode.OFPBAC_BAD_ARGUMENT);
	}
	

}
