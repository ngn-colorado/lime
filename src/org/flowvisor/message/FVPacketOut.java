package org.flowvisor.message;

import java.util.Arrays;
import java.util.List;

import org.flowvisor.LimeContainer;
import org.flowvisor.PortInfo.PortType;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.classifier.LimeBuffer_idTranslator;
import org.flowvisor.classifier.LimeMsgBuffer_idPair;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.lldp.LLDPUtil;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.HexString;

/**
 * Verify that this packet_out operation is allowed by slice definition, in
 * terms of destination port, the flowspace of the embedded packet, the
 * buffer_id, and the actions.
 *
 * Send an error msg back to controller if it's not
 *
 * @author capveg
 *
 *
 * @author Murad
 * since controller only talks with slicer that map to one single active switch
	is active been cloned?
		no, forward
		yes, (might be sent from active or its clone)
			retrieve info based on xid and get swID
			swID is for active?
				yes, edit action list based on LIME algorithem
				send to active
				get mapped clone id
				but buffer id= -1
				what about xid? would the switch accept that?
				edit action list based on LIME algorithem
				send it to clone
			swID is for clone (verifiy that is indeed the clone from ActiveToCloneMap
				yes, edict action list based on LIME algorithem
				send to clone
				get mapped active id
				but buffer id= -1
				what about xid? would the switch accept that?
				edit action list based on LIME algorithem
				send to active

 */

public class FVPacketOut extends OFPacketOut implements Classifiable, Slicable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {

		// make sure that this slice can access this bufferID
		if (! fvSlicer.isBufferIDAllowed(this.getBufferId())) {
			FVLog.log(LogLevel.WARN, fvSlicer,
					"EPERM buffer_id ", this.getBufferId(), " disallowed: "
					, this.toVerboseString());
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
					OFBadRequestCode.OFPBRC_BUFFER_UNKNOWN, this), fvSlicer);
			return;
		}

		// if it's LLDP, pass off to the LLDP hack
		if (LLDPUtil.handleLLDPFromController(this, fvClassifier, fvSlicer)){
			//System.out.println("MURAD: Found LLDP Packet in Packet-Out");
			return;
		}
		// look at the original class to see how the matching is happening to use it later

		System.out.println("MURAD: FVPacket_OUT, buf_id: " + this.bufferId + " xid: " + this.getXid() +" Packet-data: " + this.toVerboseString());



		//MURAD added bellow
		int originalBufferId = this.bufferId;
		if (originalBufferId == -1){
			if(fvClassifier.getDuplicateSwitch() != -1){
				FVClassifier duplicateFVClassifier = LimeContainer.getAllWorkingSwitches().get(fvClassifier.getDuplicateSwitch());
				sendPacketOut(fvClassifier, -1, originalBufferId);
				sendPacketOut(duplicateFVClassifier,-1, originalBufferId);
			}
			else{
				sendPacketOut(fvClassifier, -1, originalBufferId);
			}
		}
		else{
			LimeBuffer_idTranslator translator = fvSlicer.getLimeXidTranslator();
			LimeMsgBuffer_idPair pair = translator.untranslate(this.bufferId);
			if (pair != null){ // we only add to table packets during migration, so duplicate switch should never be null
				FVClassifier senderFVClassifier, cloneFVClassifier;
				senderFVClassifier = pair.getClassifier();
				FVClassifier duplicateFVClassifier = LimeContainer.getAllWorkingSwitches().get(senderFVClassifier.getDuplicateSwitch());
				sendPacketOut(senderFVClassifier, pair.getBuffer_id(), originalBufferId);
				sendFlowMod(duplicateFVClassifier);
			}
			else{
				// for now, drop the message because we don't know who sent it
				FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
			}
		}
	}

	/**
	 * Send packet after modifying port and buffer id and return sitting to what was in the origin 
	 * @param fvClassifier
	 * @param sender
	 * @param clone
	 * @param fvSlicer
	 * @param bufferId
	 * @param originalBufferId
	 */
	private void sendPacketOut(FVClassifier fvClassifier, int bufferId, int originalBufferId){
		short originalPort = -1;
		for (OFAction action : this.getActions()){
			if(action instanceof OFActionOutput){
				if(fvClassifier.getActivePorts().containsKey(((OFActionOutput) action).getPort())){
					if (fvClassifier.getActivePorts().get(((OFActionOutput) action).getPort()).getType().equals(PortType.EMPTY)){ 
						originalPort = ((OFActionOutput) action).getPort();
						((OFActionOutput) action).setPort(fvClassifier.getGhostPort());
						if (originalBufferId != -1){ // then it was in translator, we need the first buffer_id assigned which = pck_in's buffer_id
							this.setBufferId(bufferId);
						}
						fvClassifier.sendMsg(this, fvClassifier);

						// return the packet back as we received in this method
						this.setBufferId(originalBufferId);
						((OFActionOutput) action).setPort(originalPort);
						return;
						//break; //Assuming that there is only one output port...	
					}
				}
			}
		}
		// if we are here, then no change happened to action list
		if (originalBufferId != -1){ 
			this.setBufferId(bufferId);
		}
		
		fvClassifier.sendMsg(this, fvClassifier);
		this.setBufferId(originalBufferId);
	}


	/**
	 * Create FlowMod with same actions and matches from PacketOput and send to only duplicate FVClassifier
	 * Set bufferId to -1
	 * if switch is clone, then save original actions list to its table
	 * @param fvClassifier
	 * @param clone
	 * @param fvSlicer
	 * @param bufferId
	 * @param originalBufferId
	 */
	private void sendFlowMod(FVClassifier duplicateFVClassifier){
		short originalPort = -1;
		FVFlowMod fvFlowMod = new FVFlowMod();
		fvFlowMod.setCommand(FVFlowMod.OFPFC_ADD);
		fvFlowMod.setMatch(new OFMatch().loadFromPacket(this.packetData, this.inPort));
		fvFlowMod.setActions(this.getActions());
		fvFlowMod.setBufferId(-1);

		for (OFAction action : fvFlowMod.getActions()){
			if(action instanceof OFActionOutput){
				if(duplicateFVClassifier.getActivePorts().containsKey(((OFActionOutput) action).getPort())){
					if (duplicateFVClassifier.getActivePorts().get(((OFActionOutput) action).getPort()).getType().equals(PortType.EMPTY)){ 
						originalPort = ((OFActionOutput) action).getPort();
						((OFActionOutput) action).setPort(duplicateFVClassifier.getGhostPort());
						break; //Assuming that there is only one output port...	
					}
				}
			}
		}

		duplicateFVClassifier.sendMsg(fvFlowMod, duplicateFVClassifier);

		// return the original port 
		if (!duplicateFVClassifier.isActive()){  // then this is a clone switch and we need to save this flowmod
			if(originalPort != -1){
				for (OFAction action : fvFlowMod.getActions()){
					if(action instanceof OFActionOutput){
						if(((OFActionOutput) action).getPort() == duplicateFVClassifier.getGhostPort()){
							((OFActionOutput) action).setPort(originalPort);
							duplicateFVClassifier.addLimeFlowRule(originalPort, fvFlowMod.clone());
							break;
						}
					}
				}
			}
		}
	}



	// convenience function that Derickso doesn't want in main openflow.jar
	@Override
	public FVPacketOut setPacketData(byte[] packetData) {
		if (packetData == null)
			this.length = (short) (MINIMUM_LENGTH + actionsLength);
		else
			this.length = (short) (MINIMUM_LENGTH + actionsLength + packetData.length);
		this.packetData = packetData;
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FVPacketOut [actions="
				+ FVMessageUtil.actionsToString(this.getActions()) + ", actionsLength=" + actionsLength + ", " +
				" inPort=" + inPort + ", packetData=" +
				Arrays.toString(packetData) + "]";
	}


	private String toVerboseString() {
		String pkt;
		if (this.packetData != null && (this.packetData.length > 0))
			pkt = new OFMatch().loadFromPacket(this.packetData, this.inPort)
			.toString();
		else
			pkt = "empty";
		return this.toString() + ";pkt=" + pkt;
	}

}
