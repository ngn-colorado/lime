package org.flowvisor.message;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowvisor.LimeContainer;
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
	public void classifyFromSwitch(WorkerSwitch wSwitch) {
		//System.out.println("MURAD: FVFlowRamoder-37, Recv florRemove type: " + this.getReason().name() + " from sw " + wSwitch.getName());
		
		OriginalSwitch fvSlicer;
		if(wSwitch.getDuplicateSwitch() != null){
			WorkerSwitch duplicateVFClassifier = LimeContainer.getAllWorkingSwitches().get(wSwitch.getDuplicateSwitch());
			if(wSwitch.isActive()){
				fvSlicer = wSwitch.getOriginalSwitchByName(LimeContainer.OriginalSwitch);
			}
			else{ 
				if(duplicateVFClassifier.isActive()){
					fvSlicer = duplicateVFClassifier.getOriginalSwitchByName(LimeContainer.OriginalSwitch);
					wSwitch = duplicateVFClassifier;
				}
				else{
					// ignore msg, we don't know this witch
					return;
				}
			}
		}
		else{
			if(wSwitch.isActive()){
				fvSlicer = wSwitch.getOriginalSwitchByName(LimeContainer.OriginalSwitch);
			}
			else{
				// ignore packet, we only forward to controller from active switches when no migration is happening 
				FVMessageUtil.dropUnexpectedMesg(this, wSwitch);
				return;
			}
		}
		
		wSwitch.handleFlowModRemove(this, fvSlicer);	
		
		/*FlowMap flowSpace = wSwitch.getSwitchFlowMap();
		Set<String> slicesToUpdate = new HashSet<String>();
		
		String sliceName = wSwitch.getFlowDB().processFlowRemoved(this,
				wSwitch.getDPID());
		
		CookiePair pair = untanslateCookie(wSwitch);
		
		//FVLog.log(LogLevel.DEBUG, wSwitch, slicerFromCookie);
		
		
		
		
		
		
		if (sliceName != null)
			slicesToUpdate.add(sliceName);
		else if (pair != null) 
			slicesToUpdate.add(pair.getSliceName());
		else {
			// flow tracking either disabled or broken
			// just fall back to everyone who *could* have inserted this flow
			List<FlowEntry> flowEntries = flowSpace.matches(
					wSwitch.getDPID(), new FVMatch(getMatch()));
			for (FlowEntry flowEntry : flowEntries) {
				for (OFAction ofAction : flowEntry.getActionsList()) {
					if (ofAction instanceof SliceAction) {
						SliceAction sliceAction = (SliceAction) ofAction;
						if ((sliceAction.getSlicePerms() & SliceAction.WRITE) != 0) {
							slicesToUpdate.add(sliceAction.getSliceName());
						}
					}
				}
			}
		}
		// forward this msg to each of them
		FVLog.log(LogLevel.DEBUG, wSwitch, slicesToUpdate.toString());
		for (String slice : slicesToUpdate) {
			OriginalSwitch fvSlicer = wSwitch.getOriginalSwitchByName(slice);
			if (fvSlicer == null) {
				FVLog.log(LogLevel.CRIT, wSwitch,
						"inconsistent state: missing fvSliver entry for: "
								+ slice);
				continue;
			}
			fvSlicer.decrementFlowRules();
			fvSlicer.getFlowRewriteDB().processFlowRemoved(this);
			if (pair != null)
				this.setCookie(pair.getCookie());
			fvSlicer.sendMsg(this, wSwitch);
		}*/
		
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
