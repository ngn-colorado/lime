package org.flowvisor.message;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.LinkAdvertisement;
import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.lldp.LLDPUtil;
import org.flowvisor.ofswitch.DPIDandPort;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.OriginalSwitch;
import org.flowvisor.slicer.LimeMsgTranslator;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;

import edu.colorado.cs.ngn.lime.LimeContainer;

public class FVPacketIn extends OFPacketIn implements Classifiable, Slicable,
TopologyControllable {

	/**
	 * route and rewrite packet_in messages from switch to controller
	 *
	 * if it's lldp, do the lldp decode stuff else, look up the embedded
	 * packet's controller(s) by flowspace and send to them
	 * 
	 * Murad
	 * is originated from active switch?
		yes, is cloned?
			no, forward
			yes, save xid and swID
				forward
		no, originated from cloned switch?
			no, drop
			yes, save xid and swID
				map to active switch, slicer
				forwatd
	 */

	@Override
	public void classifyFromSwitch(WorkerSwitch workerSwitch) {
		// handle LLDP as a special (hackish) case
		if (LLDPUtil.handleLLDPFromSwitch(this, workerSwitch)){
			System.out.println("MURAD: Found LLDP Packet in Packet-In");
			return;
		}
		//System.out.println("MURAD: FVPacketIn, from sw: " + workerSwitch.getName() + " Data-lenght: ");// + this.packetData.length + " and Packet-data: " + this.toVerboseString());
		
		OriginalSwitch fvSlicer;
		if(workerSwitch.getDuplicateSwitch() != null){
			WorkerSwitch duplicateVFClassifier = LimeContainer.getAllWorkingSwitches().get(workerSwitch.getDuplicateSwitch().getDPID());
			if(workerSwitch.isActive()){
				fvSlicer = workerSwitch.getOriginalSwitchByName(LimeContainer.OriginalSwitch);
			}
			else{ 
				if(duplicateVFClassifier.isActive()){
					fvSlicer = duplicateVFClassifier.getOriginalSwitchByName(LimeContainer.OriginalSwitch);
					//workerSwitch = duplicateVFClassifier;
				}
				else{
					// ignore msg, we don't know this witch
					return;
				}
			}
		}
		else{
			if(workerSwitch.isActive()){
				fvSlicer = workerSwitch.getOriginalSwitchByName(LimeContainer.OriginalSwitch);
			}
			else{
				// ignore packet, we only forward to controller from active switches when no migration is happening 
				FVMessageUtil.dropUnexpectedMesg(this, workerSwitch);
				return;
			}
		}
		LimeMsgTranslator buffIdTranslator = fvSlicer.getLimeMsgTranslator();
		this.setBufferId(buffIdTranslator.translate(this.bufferId, workerSwitch, new OFMatch().loadFromPacket(this.packetData, this.inPort)));
//		System.out.println("MURAD: FVPacketIn, sending from sw: " + workerSwitch.getName() + " following P-In: " + this.toVerboseString());
//		System.out.println("MURAD: FVPacketIn,------------------------------------------------------------------------------------------");
		fvSlicer.sendMsg(this, workerSwitch);
		
	}

	private void lookupByFlowSpace(WorkerSwitch workerSwitch) {
		SliceAction sliceAction;
		int perms;
		// grab single matching rule: only one because it's a point in flowspace
		FlowEntry flowEntry = workerSwitch.getSwitchFlowMap().matches(
				workerSwitch.getSwitchInfo().getDatapathId(), this.getInPort(),
				this.getPacketData());
		if (flowEntry == null) {
			FVLog.log(LogLevel.DEBUG, workerSwitch,
					"dropping unclassifiable msg: " + this.toVerboseString());
			return;
		}

		boolean foundHome = false;
		// foreach slice in that rule
		for (OFAction ofAction : flowEntry.getActionsList()) {
			sliceAction = (SliceAction) ofAction;
			perms = sliceAction.getSlicePerms();
			if ((perms & (SliceAction.READ | SliceAction.WRITE)) != 0) {
				// lookup slice and send msg to them
				// TODO record buffer id for later validation
				OriginalSwitch fvSlicer = workerSwitch.getOriginalSwitchByName(sliceAction
						.getSliceName());
				if (fvSlicer == null) {
					FVLog.log(LogLevel.WARN, workerSwitch,
							"tried to send msg to non-existant slice: "
									+ sliceAction.getSliceName()
									+ " corrupted flowspace?:: "
									+ this.toVerboseString());
					continue;
				}
				if (fvSlicer.isConnected()) {
					if ((perms & SliceAction.WRITE) != 0)
						fvSlicer.setBufferIDAllowed(this.getBufferId());
					fvSlicer.sendMsg(this, workerSwitch);
					/**
					 * TODO : come back and decide if we should uncomment this
					 * i.e., should a rule get squashed if it's only recipient
					 * is read only
					 *
					 * if yes, then tests-readonly.py needs to be changed
					 *
					 */

					// if ((perms & SliceAction.WRITE) != 0)
					foundHome = true;
				} else {
					sendDropRule(workerSwitch, flowEntry, fvSlicer.getSliceName(), (short) 0, (short) 1);
				}
				foundHome = true;
			}
			/*
			 * ash: not sure if this is the intended behavior, but I am guessing
			 * that if this packet hasn't gotten handled anywhere we should at least
			 * default to adding a drop rule for unless we want to be flooded.
			 */
			if (!foundHome)
				sendDropRule(workerSwitch, flowEntry, "exact", (short) 0, (short) 1);
		}
	}

	/**
	 * Tell the classifier to drop packets that look like this
	 *
	 * @param workerSwitch
	 * @param flowEntry
	 * @param hardTimeout
	 * @param idleTimeout
	 */

	private void sendDropRule(WorkerSwitch workerSwitch, FlowEntry flowEntry,
			String sliceName, short hardTimeout, short idleTimeout) {
		FVFlowMod flowMod = (FVFlowMod) FlowVisor.getInstance().getFactory()
				.getMessage(OFType.FLOW_MOD);
		// block this exact flow
		OFMatch match = new OFMatch();
		match.loadFromPacket(this.packetData, this.inPort);
		// different from previous policty of block by rule
		// flowMod.setMatch(flowEntry.getRuleMatch());

		String drop_policy = null;
		try {
			drop_policy = FVConfig.getDropPolicy(sliceName);
		} catch (ConfigError e) {
			FVLog.log(LogLevel.ALERT, workerSwitch, "Failed to retrieve drop policy from config."
					+ "\nDefauting to exact drop_policy");
			drop_policy = "exact";
		}
		if (drop_policy.equals("exact"))
			flowMod.setMatch(match);
		else if (drop_policy.equals("rule"))
			flowMod.setMatch(flowEntry.getRuleMatch());
		else
			// Should never happen
			FVLog.log(LogLevel.CRIT, workerSwitch, "Error in configuration!");
		flowMod.setCommand(FVFlowMod.OFPFC_ADD);
		flowMod.setActions(new LinkedList<OFAction>()); // send to zero-length
		// list, i.e., DROP
		flowMod.setLengthU(OFFlowMod.MINIMUM_LENGTH);
		flowMod.setHardTimeout(hardTimeout);
		flowMod.setIdleTimeout(idleTimeout);
		flowMod.setPriority((short) 0); // set to lowest priority
		flowMod.setFlags((short) 1);
		// send removed msg (1), not the check overlap (2), or
		// emergency flow cache (4)

		FVLog.log(LogLevel.WARN, workerSwitch, "inserting drop (hard="
				+ hardTimeout + ",idle=" + idleTimeout + ") rule for "
				+ flowEntry);
		workerSwitch.sendMsg(flowMod, workerSwitch);
	}

	private String toVerboseString() {
		String pkt;
		if (this.packetData != null)
			pkt = new OFMatch().loadFromPacket(this.packetData, this.inPort)
			.toString();
		else
			pkt = "empty";
		return this.toString() + ";pkt=" + pkt;
	}

	@Override
	public void sliceFromController(WorkerSwitch workerSwitch, OriginalSwitch fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	@Override
	public FVPacketIn setPacketData(byte[] packetData) {
		if (packetData == null)
			this.length = (short) (MINIMUM_LENGTH);
		else
			this.length = (short) (MINIMUM_LENGTH + packetData.length);
		this.packetData = packetData;
		return this;
	}

	/**
	 * The topologyController handles LLDP messages and ignores everything else
	 */
	@Override
	public void topologyController(TopologyConnection topologyConnection) {
		synchronized (topologyConnection) {
			DPIDandPort dpidandport = TopologyConnection.parseLLDP(this
					.getPacketData());
			if (dpidandport == null) {
				FVLog
				.log(LogLevel.DEBUG, topologyConnection,
						"ignoring non-lldp packetin: "
								+ this.toVerboseString());
				return;
			}
			OFFeaturesReply featuresReply = topologyConnection
					.getFeaturesReply();
			if (featuresReply == null) {
				FVLog.log(LogLevel.WARN, topologyConnection,
						"ignoring packet_in: no features_reply yet");
				return;
			}
			LinkAdvertisement linkAdvertisement = new LinkAdvertisement(
					dpidandport.getDpid(), dpidandport.getPort(), featuresReply
					.getDatapathId(), this.inPort);
			topologyConnection.getTopologyController().reportProbe(
					linkAdvertisement);
			topologyConnection.signalFastPort(this.inPort);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FVPacketIn [reason=" + 
				this.getReason().toString() + ", " +
				" inPort=" + inPort + ", packetData=" +
				Arrays.toString(packetData) + "]";
	}


	public static void main(String args[]) throws FileNotFoundException, ConfigError {
		if (args.length < 3) {
			System.err.println("Usage: <config.xml> <dpid> <ofmatch>");
			System.exit(1);
		}

		FVConfig.readFromFile(args[0]);
		long dpid = FlowSpaceUtil.parseDPID(args[1]);
		FVMatch packet = new FVMatch();
		packet.fromString(args[2]);

		System.err.println("Looking up packet '" + packet + "' on dpid="
				+ FlowSpaceUtil.dpidToString(dpid));
		List<FlowEntry> entries = FVConfig.getFlowSpaceFlowMap().matches(dpid,
				packet);

		System.err.println("Matches found: " + entries.size());
		if (entries.size() > 1)
			System.err.println("WARN: only sending to the first match");
		for (FlowEntry flowEntry : entries) {
			System.out.println(flowEntry);
		}

	}
}
