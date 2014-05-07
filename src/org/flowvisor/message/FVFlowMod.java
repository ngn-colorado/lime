package org.flowvisor.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.flowvisor.LimeContainer;
import org.flowvisor.PortInfo.PortType;
import org.flowvisor.classifier.CookieTranslator;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.LimeBuffer_idTranslator;
import org.flowvisor.classifier.LimeMsgBuffer_idPair;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowIntersect;
import org.flowvisor.flows.FlowSpaceRuleStore;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionEnqueue;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;

public class FVFlowMod extends org.openflow.protocol.OFFlowMod implements
Classifiable, Slicable, Cloneable {

	private HashMap<String,FVFlowMod> sliceModMap = new HashMap<String,FVFlowMod>();;
	private FVMatch mat;

	private HashMap<Integer,Integer> priorityMap = new HashMap<Integer,Integer>();
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	/**
	 * FlowMod slicing
	 *
	 * 1) make sure all actions are ok
	 *
	 * 2) expand this FlowMod to the intersection of things in the given match
	 * and the slice's flowspace
	 * 
	 * @author Murad
	 * since controller only talks with active
		is active been cloned?
		no, forward
		yes, (send this to both active and clone switches)
		get map of who sent it (active/clone) based on buffer_id mapping

		edit action list based on LIME algorithm
				forward to active
				get mapped clone switch
				forward to clone
				send to active
	 */

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		System.out.println("MURAD: FV_MOD, buf_id: " + this.bufferId + " Packet-data: " + this.toString());
		FVLog.log(LogLevel.DEBUG, fvSlicer, "recv from controller: ", this);
		FVMessageUtil.translateXid(this, fvClassifier, fvSlicer);
		translateCookie(fvClassifier, fvSlicer);


		//MURAD added bellow
		if(fvClassifier.getDuplicateSwitch() != -1){
			LimeBuffer_idTranslator translator = fvSlicer.getLimeXidTranslator();
			int buffer_id = this.bufferId;
			FVClassifier duplicateFVClassifier = LimeContainer.getAllWorkingSwitches().get(fvClassifier.getDuplicateSwitch());
			LimeMsgBuffer_idPair pair = translator.untranslate(buffer_id);
			FVClassifier senderFVClassifier = pair.getClassifier();

			if (senderFVClassifier != null){
				if (senderFVClassifier.getDPID() != fvClassifier.getDPID()){
					if(senderFVClassifier.getDPID() != duplicateFVClassifier.getDPID()){
						// drop msg, we don't know who sent it first place
						return;
					}
					else{
						duplicateFVClassifier = fvClassifier;
					}
				}

				short originalPort = -1;
				short cloneOriginalPort = -1;
				short originalPriority = -1;
				// check to see if this is an output port action
				for (OFAction action : this.getActions()){
					if(action instanceof OFActionOutput){
						originalPort = ((OFActionOutput) action).getPort(); 
						if (senderFVClassifier.getActivePorts().get(originalPort).getType().equals(PortType.EMPTY)){
							((OFActionOutput) action).setPort(senderFVClassifier.getGhostPort());
						}
						senderFVClassifier.sendMsg(this, fvSlicer);

						if (duplicateFVClassifier.getActivePorts().get(originalPort).getType().equals(PortType.EMPTY)){ // this should never return null pointer exception, if so this is a serious problem! 
							cloneOriginalPort = originalPort; 
							originalPriority = this.getPriority();
							this.setPriority((short) (originalPriority + 1)); // so later we will send OFPFC_DELETE_STRICT to remove them. 
							// TODO we want to handle if the original is the maximum priority allowed by OF
							((OFActionOutput) action).setPort(duplicateFVClassifier.getGhostPort());
						}
						duplicateFVClassifier.sendMsg(this, fvSlicer);
						break; //Assuming that there is only one output port...	
					}
				}

				if(cloneOriginalPort != -1){
					// now add this to LimeFlowTable
					duplicateFVClassifier.addLimeFlowRule(cloneOriginalPort, this);		
					// return the original port and priority
					for (OFAction action : this.getActions()){
						if(action instanceof OFActionOutput){
							((OFActionOutput) action).setPort(cloneOriginalPort);
							this.setPriority(originalPriority);
							duplicateFVClassifier.addFlowRule(this);
							duplicateFVClassifier.sendMsg(this, fvSlicer);
							break;
						}
					}
				}

			}
			else{
				// send it as it is, buffer_id might be generated before cloning start 
			}

		}

		else{
			// save this FlowMod to list
			fvClassifier.addFlowRule(this);
			fvClassifier.sendMsg(this, fvSlicer);
		}
	}


	private Integer getNewPriority(int oldPriority, Integer intersectPrio, FVSlicer fvSlicer){
		if(oldPriority > 65535){
			FVLog.log(LogLevel.CRIT, null, "The range of priority is between 0 & 65535");
		}
		FVLog.log(LogLevel.DEBUG,null,"FVFlowMod oldPriority:",oldPriority);

		HashMap<Integer,ArrayList<Integer>> prioRangeMap 
		= fvSlicer.getFlowSpace().getPriorityRangeMap();

		Integer rangeStart=0;
		Integer rangeEnd=0;
		Integer range;
		//Check if the priority of the intersected flow space entry
		//is present in the prioRangeMap
		if(prioRangeMap.containsKey(intersectPrio)){
			rangeStart = (prioRangeMap.get(intersectPrio)).get(0);
			rangeEnd = (prioRangeMap.get(intersectPrio)).get(1);	
		}
		range = rangeEnd - rangeStart;
		Integer nwPrio = ((oldPriority*range)/65536) + rangeStart;
		Integer nwPrioFirstHalf = nwPrio & 0xFF00;
		Integer nwPrioSecHalf = nwPrio & 0x00FF;
		Integer oldPrioSecHalf = oldPriority & 0x00FF;
		Integer newPrioSecHalf = nwPrioSecHalf ^ oldPrioSecHalf;
		Integer newPriority = nwPrioFirstHalf | newPrioSecHalf;
		if(priorityMap.containsValue(newPriority)==false)
			priorityMap.put(oldPriority, newPriority);
		else{
			while(priorityMap.containsValue(newPriority)){
				newPriority++;
			}
			if(newPriority>65535)
				newPriority = 65535;
			priorityMap.put(oldPriority, newPriority);
		}
		FVLog.log(LogLevel.DEBUG,null,"FVFlowMod priorityMap:",priorityMap);

		return newPriority;
	}

	private void applyForceEnqueue(FVFlowMod newFlowMod, FlowEntry flowEntry) {
		if (!flowEntry.forcesEnqueue())
			return;
		List<OFAction> neoActions = new LinkedList<OFAction>();
		int length = 0;
		for (OFAction action : newFlowMod.actions) {
			if (action instanceof OFActionOutput) {
				OFActionOutput output = (OFActionOutput) action;
				OFActionEnqueue repl = new OFActionEnqueue();
				repl.setPort(output.getPort());
				repl.setQueueId((int)flowEntry.getForcedQueue());
				neoActions.add(repl);
				length += repl.getLengthU();
			} else {
				neoActions.add(action);
				length += action.getLengthU();
			}
		}
		newFlowMod.setActions(neoActions);
		newFlowMod.setLengthU(FVFlowMod.MINIMUM_LENGTH + length);
	}


	private void translateCookie(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		CookieTranslator cookieTrans = fvClassifier.getCookieTranslator();
		long newCookie = cookieTrans.translate(this.cookie, fvSlicer);
		this.setCookie(newCookie);
		FVLog.log(LogLevel.DEBUG,null,"translateCookie newCookie:",newCookie);
	}



	public FVFlowMod setMatch(FVMatch match) {
		this.match = match;
		return this;
	}

	@Override
	public OFMatch getMatch() {
		return this.match;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */

	@Override
	public String toString() {
		return "FVFlowMod [ actions="
				+ FVMessageUtil.actionsToString(this.getActions()) + ", command=" + command
				+ ", cookie=" + cookie + ", flags=" + flags + ", hardTimeout="
				+ hardTimeout + ", idleTimeout=" + idleTimeout + ", match="
				+ match + ", outPort=" + outPort + ", priority=" + priority
				+ ", length=" + length + ", type=" + type + ", version="
				+ version + "]";
	}

}
