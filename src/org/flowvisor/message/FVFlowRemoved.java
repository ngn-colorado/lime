package org.flowvisor.message;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowvisor.classifier.CookiePair;
import org.flowvisor.classifier.CookieTranslator;
import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.LimeMsgTranslator;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

import edu.colorado.cs.ngn.lime.LimeContainer;

public class FVFlowRemoved extends OFFlowRemoved implements Classifiable,
		Slicable {

	/**
	 * Current algorithm: if flow tracking knows who sent this flow, then just
	 * send to them
	 *
	 * If flow tracking doesn't know (or is disabled) send to everyone who
	 * *could* have sent the flow
	 *
	 * FIXME: do the reference counting so that if a flow is expanded three
	 * ways, only send the flow_removed up to the controller if all three flows
	 * have expired
	 */
	@Override
	public void classifyFromSwitch(WorkerSwitch wSwitch){
		OriginalSwitch originalSwitch;
		if(wSwitch.getDuplicateSwitch() != null){
			WorkerSwitch duplicateVFClassifier = LimeContainer.getAllWorkingSwitches().get(wSwitch.getDuplicateSwitch().getDPID());
			if(wSwitch.isActive()){
				originalSwitch = wSwitch.getOriginalSwitchByName(LimeContainer.OriginalSwitch);
			}
			else{ 
				if(duplicateVFClassifier.isActive()){
					originalSwitch = duplicateVFClassifier.getOriginalSwitchByName(LimeContainer.OriginalSwitch);
				}
				else{
					// ignore msg, we don't know this witch
					return;
				}
			}
		}
		else{
			if(wSwitch.isActive()){
				originalSwitch = wSwitch.getOriginalSwitchByName(LimeContainer.OriginalSwitch);
			}
			else{
				// ignore packet, we only forward to controller from active switches when no migration is happening 
				FVMessageUtil.dropUnexpectedMesg(this, wSwitch);
				return;
			}
		}
		
		wSwitch.handleFlowModRemove(this, originalSwitch);			
	}

	@Override
	public void sliceFromController(WorkerSwitch wSwitch, OriginalSwitch fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}
	
	public FVFlowRemoved setMatch(FVMatch match) {
		this.match = match;
		return this;
	}
	
	private CookiePair untanslateCookie(WorkerSwitch wSwitch) {
		CookieTranslator cookieTrans = wSwitch.getCookieTranslator();
		CookiePair pair = cookieTrans.untranslateAndRemove(this.cookie);
		if (pair == null) {
			return null;
		}
		return pair;
	}
	
	@Override
	public OFMatch getMatch() {
		return this.match;
	}

	@Override
	public String toString() {
		return "FVFlowRemoved [match=" + this.getMatch().toString() + "]";
	}

}
