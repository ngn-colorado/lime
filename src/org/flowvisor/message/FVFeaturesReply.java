package org.flowvisor.message;

import java.util.ArrayList;
import java.util.List;

import org.flowvisor.LimeContainer;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFPhysicalPort;

public class FVFeaturesReply extends org.openflow.protocol.OFFeaturesReply
implements Classifiable, Slicable, TopologyControllable {

	/**
	 * Prune the listed ports to only those that appear in the slice
	 */
	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(this, fvClassifier);
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					" dropping msg with un-untranslatable xid: " + this);
			return;
		}
		this.prunePorts(fvSlicer); // remove ports that are not part of slice
		// TODO: rewrite DPID if this is a virtual switch
		
		//MURAD added bellow if and else statement
		if(fvClassifier.isActive()){
			// get original switch from ActiveToOriginal map 
			// insert the original switchId in this reply
			if (LimeContainer.getActiveToOriginalSwitchMap().containsKey(this.datapathId)){
				this.datapathId = LimeContainer.getActiveToOriginalSwitchMap().get(this.datapathId);
				fvSlicer.sendMsg(this, fvClassifier);
			}
			else{
				System.out.println("MURAD: ERROR can't reply features beacuse no original map exisit!!!");
			}
		}
		else{
			if(fvClassifier.getduplicateSwitch() != 0){
				if (LimeContainer.getActiveToOriginalSwitchMap().containsKey(fvClassifier.getduplicateSwitch())){
					this.datapathId = LimeContainer.getActiveToOriginalSwitchMap().get(fvClassifier.getduplicateSwitch());
					fvSlicer.sendMsg(this, fvClassifier);
				}
				else{
					System.out.println("MURAD: ERROR can't reply features beacuse no original map exisit!!!");
				}
			}
		}


	}

	// rewrite the ports list to only the set of ports allowed by the slice
	// definition
	private void prunePorts(FVSlicer fvSlicer) {
		List<OFPhysicalPort> newPorts = new ArrayList<OFPhysicalPort>();
		for (OFPhysicalPort phyPort : this.getPorts()) {
			if (fvSlicer.getPorts().contains(phyPort.getPortNumber()))
				newPorts.add(phyPort);
		}
		this.setPorts(newPorts);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	@Override
	public String toString() {
		return "FVFeaturesReply [ dpid=" +  FlowSpaceUtil.dpidToString(this.datapathId)
				+ ",ports=" + this.getPorts().size() + "]";
	}

	/**
	 * If a topologyConnection gets this message, then register it
	 *
	 */
	@Override
	public void topologyController(TopologyConnection topologyConnection) {
		topologyConnection.setFeaturesReply(this);
	}
}
