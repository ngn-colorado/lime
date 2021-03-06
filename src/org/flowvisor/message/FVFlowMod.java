package org.flowvisor.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.classifier.CookieTranslator;
import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.OriginalSwitch;
import org.flowvisor.slicer.LimeMsgData;
import org.flowvisor.slicer.LimeMsgTranslator;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionEnqueue;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;

import edu.colorado.cs.ngn.lime.LimeContainer;
import edu.colorado.cs.ngn.lime.util.PortInfo.PortType;

public class FVFlowMod extends org.openflow.protocol.OFFlowMod implements
Classifiable, Slicable, Cloneable {

	private HashMap<String,FVFlowMod> sliceModMap = new HashMap<String,FVFlowMod>();;
	private FVMatch mat;
	private short originalOutputPort = -1; // in case we edit flowmod while migrating, we need to to know the original out port to return it later
	private HashMap<Integer,Integer> priorityMap = new HashMap<Integer,Integer>();
	@Override
	public void classifyFromSwitch(WorkerSwitch workerSwitch) {
		FVMessageUtil.dropUnexpectedMesg(this, workerSwitch);
	}

	/**
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

	public void setOriginalOutputPort(short port){
		this.originalOutputPort = port;
	}
	
	public short getOriginalOutputPort(){
		return this.originalOutputPort;
	}
	
	@Override
	public void sliceFromController(WorkerSwitch workerSwitch, OriginalSwitch fvSlicer) {
//		System.out.println("MURAD: FV_MOD,------------------");
//		System.out.println("MURAD: FV_MOD, receieved: " + this.toString());
		// we always want to set OFPFF_SEND_FLOW_REM flag, but without changing the other two flags
		this.setFlags((short) (this.getFlags() | OFFlowMod.OFPFF_SEND_FLOW_REM));
		FVLog.log(LogLevel.DEBUG, fvSlicer, "recv from controller: ", this);
		FVMessageUtil.translateXid(this, workerSwitch, fvSlicer);
		translateCookie(workerSwitch, fvSlicer);

		int originalBufferId = this.bufferId;
		if(originalBufferId == -1){
			if(workerSwitch.getDuplicateSwitch() != null){
				WorkerSwitch duplicateWorkerSwitch = LimeContainer.getAllWorkingSwitches().get(workerSwitch.getDuplicateSwitch().getDPID());
				sendFlowMod(workerSwitch, -1, originalBufferId);
				sendFlowMod(duplicateWorkerSwitch,-1, originalBufferId);
			}
			else{
				workerSwitch.handleFlowModAndSend(this); // no need to modify it
			}
		}

		else{
			// we need to send to sender the original bufer_id and to duplicate buffer_if of -1
			LimeMsgTranslator translator = fvSlicer.getLimeMsgTranslator();
			LimeMsgData pair = translator.untranslate(this.bufferId);
			if (pair != null){
				WorkerSwitch senderWorkerSwitch; //, cloneWorkerSwitch;
				senderWorkerSwitch = pair.getClassifier();
				if(senderWorkerSwitch.getDuplicateSwitch() != null){
					WorkerSwitch duplicateWorkerSwitch = LimeContainer.getAllWorkingSwitches().get(senderWorkerSwitch.getDuplicateSwitch().getDPID());
					sendFlowMod(senderWorkerSwitch, pair.getBuffer_id(), originalBufferId);
					sendFlowMod(duplicateWorkerSwitch, -1, -1);
				}
				else{
					sendFlowMod(senderWorkerSwitch, pair.getBuffer_id(), originalBufferId);
				}				
			}
			else{ //TODO do we need to send this unknown FlowMod ?
				if(workerSwitch.getDuplicateSwitch() != null){
					WorkerSwitch duplicateWorkerSwitch = LimeContainer.getAllWorkingSwitches().get(workerSwitch.getDuplicateSwitch().getDPID());
					sendFlowMod(workerSwitch, originalBufferId, originalBufferId);
					sendFlowMod(duplicateWorkerSwitch, -1, -1);
				}
				else{
					workerSwitch.handleFlowModAndSend(this);
				}	
			}
		}
	}

	/**
	 * Send packet after modifying port and buffer id and return sitting to what was in the origin 
	 * @param workerSwitch
	 * @param sender
	 * @param clone
	 * @param fvSlicer
	 * @param bufferId
	 * @param originalBufferId
	 */
	private void sendFlowMod(WorkerSwitch workerSwitch, int bufferId, int originalBufferId){
		short originalPort = -1;
		OFAction action;
	
		for(int i = 0; i<this.getActions().size(); i++ ){
			action = this.getActions().get(i);
			if(action instanceof OFActionOutput){
				if(workerSwitch.getActivePorts().containsKey(((OFActionOutput) action).getPort())){
					if (workerSwitch.getActivePorts().get(((OFActionOutput) action).getPort()).getType().equals(PortType.EMPTY)){ 
						originalPort = ((OFActionOutput) action).getPort();
						//OFActionVirtualLanIdentifier addedVlanAction = new OFActionVirtualLanIdentifier(originalPort);
						//this.getActions().add(i, addedVlanAction);
						((OFActionOutput) action).setPort(workerSwitch.getGhostPort());
						if (originalBufferId != -1){ // then it was in translator, we need the first buffer_id assigned which = pck_in's buffer_id
							this.setBufferId(bufferId);
						}
						this.setOriginalOutputPort(originalPort);
						workerSwitch.handleFlowModAndSend(this);

						// return the packet back as we received in this method
						this.setBufferId(originalBufferId);
						((OFActionOutput) action).setPort(originalPort);
						this.getActions().remove(i);  // removing vlan tag
						
						return; //Assuming that there is only one output port...	
					}
				}
			}
		}
		// if we are here, then no change happened to action list
		if (originalBufferId != -1){ 
			this.setBufferId(bufferId);
		}


		workerSwitch.handleFlowModAndSend(this);

		//return everything in place in case we want to use this method more than once
		this.setBufferId(originalBufferId);
		
		// Murad, I don't think we will need the below if statement 
		/*if(originalPort != -1){
			OFAction action2;
			for(int i = 0; i<this.getActions().size(); i++ ){
				action2 = this.getActions().get(i);
				if(action2 instanceof OFActionOutput){
					if(((OFActionOutput) action2).getPort() == workerSwitch.getGhostPort()){
						((OFActionOutput) action2).setPort(originalPort);
						break;
					}
				}
			}
		}*/
	}


	private Integer getNewPriority(int oldPriority, Integer intersectPrio, OriginalSwitch fvSlicer){
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


	private void translateCookie(WorkerSwitch workerSwitch, OriginalSwitch fvSlicer) {
		CookieTranslator cookieTrans = workerSwitch.getCookieTranslator();
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
