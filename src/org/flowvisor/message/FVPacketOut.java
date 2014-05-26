package org.flowvisor.message;

import java.util.Arrays;

import org.flowvisor.LimeContainer;
import org.flowvisor.PortInfo.PortType;
import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.lldp.LLDPUtil;
import org.flowvisor.slicer.OriginalSwitch;
import org.flowvisor.slicer.LimeMsgData;
import org.flowvisor.slicer.LimeMsgTranslator;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;

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
	public void classifyFromSwitch(WorkerSwitch fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	@Override
	public void sliceFromController(WorkerSwitch fvClassifier, OriginalSwitch fvSlicer) {

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
		if (originalBufferId == -1){ // send it to either active or clone. Since we always receive to active, we just forward to active to save time
			sendPacketOut(fvClassifier, -1, originalBufferId);
		}
		else{
			LimeMsgTranslator translator = fvSlicer.getLimeMsgTranslator();
			LimeMsgData pair = translator.untranslate(this.bufferId);
			if (pair != null){
				if(fvClassifier.getDuplicateSwitch() != null){
					WorkerSwitch senderWorkerSwitch; //, cloneWorkerSwitch;
					senderWorkerSwitch = pair.getClassifier();
					//WorkerSwitch duplicateWorkerSwitch = LimeContainer.getAllWorkingSwitches().get(senderWorkerSwitch.getDuplicateSwitch());
					sendPacketOut(senderWorkerSwitch, pair.getBuffer_id(), originalBufferId);
					//sendFlowMod(duplicateWorkerSwitch);
				}
				else{
					sendPacketOut(fvClassifier, pair.getBuffer_id(), originalBufferId);
				}
			}
			else{ //TODO is this possible to happen??
				if(fvClassifier.getDuplicateSwitch() != null){
					sendPacketOut(fvClassifier, originalBufferId, originalBufferId); // its in cloning process so we need to modify the action
				}
				else{
					fvClassifier.sendMsg(this, fvSlicer); // send it as it is
				}

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
	private void sendPacketOut(WorkerSwitch fvClassifier, int bufferId, int originalBufferId){
		short originalPort = -1;int listCounter = 0;
		OFAction action;
		for(int i = 0; i<this.getActions().size(); i++ ){
			action = this.getActions().get(i);
			if(action instanceof OFActionOutput){
				if(fvClassifier.getActivePorts().containsKey(((OFActionOutput) action).getPort())){
					if (fvClassifier.getActivePorts().get(((OFActionOutput) action).getPort()).getType().equals(PortType.EMPTY)){ 
						originalPort = ((OFActionOutput) action).getPort();
						OFActionVirtualLanIdentifier addedVlanAction = new OFActionVirtualLanIdentifier(originalPort);
						this.getActions().add(i, addedVlanAction);
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
			listCounter++;
		}
		// if we are here, then no change happened to action list
		if (originalBufferId != -1){ 
			this.setBufferId(bufferId);
		}

		fvClassifier.sendMsg(this, fvClassifier);
		this.setBufferId(originalBufferId);
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
