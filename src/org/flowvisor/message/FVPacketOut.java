package org.flowvisor.message;

import java.util.Arrays;
import java.util.List;

import org.flowvisor.LimeContainer;
import org.flowvisor.classifier.FVClassifier;
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
		//System.out.println("MURAD: sending Packet_out to switch: " + fvClassifier.getDPID());



		//MURAD added bellow
		if(fvClassifier.isBeenCloned()){
			if (LimeContainer.untranslate(xid) != null){
				long switchID = LimeContainer.untranslate(xid).getClassifierID();
				FVClassifier senderFVClassifier = LimeContainer.getAllWorkingSwitches().get(switchID);
				if(senderFVClassifier.isActive()){
					
				}
			}
			// TODO MURAD modify packet (based on LIME/Eric algorithm) and send to active switch

			// TODO MURAD modify packet (based on LIME/Eric algorithm) and send to cloned switch
		}
		else{
			fvClassifier.sendMsg(this, fvSlicer);
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
