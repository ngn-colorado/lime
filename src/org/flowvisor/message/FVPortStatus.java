package org.flowvisor.message;

import org.flowvisor.classifier.WorkerSwitch;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.slicer.OriginalSwitch;
import org.openflow.protocol.OFPortStatus;
import org.openflow.util.HexString;

import edu.colorado.cs.ngn.lime.LimeContainer;
import edu.colorado.cs.ngn.lime.LimeSwitch;
import edu.colorado.cs.ngn.lime.util.PortInfo;

/**
 * Send the port status message to each slice that uses this port
 *
 * @author capveg
 *
 */

public class FVPortStatus extends OFPortStatus implements Classifiable,
Slicable, TopologyControllable {

	@Override
	public void classifyFromSwitch(WorkerSwitch workerSwitch) {
		Short port = Short.valueOf(this.getDesc().getPortNumber());
		byte reason = this.getReason();

		boolean updateSlicers = false;

		if (reason == OFPortReason.OFPPR_ADD.ordinal()) {
			System.out.println("MURAD: FVPortStatus, sw: " + workerSwitch.getSwitchName() + " dynamically adding port " + this.getDesc().getPortNumber() + " because its state " + this.getDesc().getState() );

			FVLog.log(LogLevel.INFO, workerSwitch, "dynamically adding port "
					+ port);
			workerSwitch.addPort(this.getDesc(), true); // new port dynamically added
			updateSlicers = true;
		} else if (reason == OFPortReason.OFPPR_DELETE.ordinal()) {
			System.out.println("MURAD: FVPortStatus, sw: " + workerSwitch.getSwitchName() + "dynamically removing port " + this.getDesc().getPortNumber() + " because its state " + this.getDesc().getState() );
			FVLog.log(LogLevel.INFO, workerSwitch, "dynamically removing port "
					+ port);
			workerSwitch.removePort(this.getDesc());
			updateSlicers = true;
		} else if (reason == OFPortReason.OFPPR_MODIFY.ordinal()) {
			// replace/update the port definition
			System.out.println("MURAD: FVPortStatus, sw: " + workerSwitch.getSwitchName() + " dynamically modifying port "+ this.getDesc().getPortNumber() + " because its state " + this.getDesc().getState() );
			FVLog.log(LogLevel.INFO, workerSwitch, "modifying port " + port);
			//workerSwitch.removePort(this.getDesc());
			/*
			 * ash: addPort actually removes the port first.
			 */
			workerSwitch.addPort(this.getDesc(), false);
		} else {
			System.out.println("MURAD: FVPortStatus, sw: " + workerSwitch.getSwitchName() + " dynamically unknown reason " + this.getDesc().getPortNumber() + " because its state " + this.getDesc().getState() );
			FVLog.log(LogLevel.CRIT, workerSwitch, "unknown reason " + reason
					+ " in port_status msg: " + this);
		}

		if (updateSlicers) {
			for (OriginalSwitch originalSwitch : workerSwitch.getSlicers()) {
				/*
				 * Ugly call to update flowspace when using a linear flowspace
				 * this WILL go when the linear flowspace goes.
				 */
				originalSwitch.updateFlowSpace();
			}
		}
		
		// during migration we don't want the controller to know about port changes 
		if((workerSwitch.isActive()) && (workerSwitch.getDuplicateSwitch() == null)){
			// if this adding port not specified in the original switch table, then don't tell controller about it
			LimeSwitch origSwitch;
			origSwitch = LimeContainer.getOriginalSwitchContainer().get(LimeContainer.getActiveToOriginalSwitchMap().get(workerSwitch.getDPID()));
			if(origSwitch.getPortTable().get(this.getDesc().getPortNumber()) != null){
				for (OriginalSwitch originalSwitch : workerSwitch.getSlicers()) {
					if (originalSwitch.portInSlice(port)) {

						originalSwitch.sendMsg(this, workerSwitch);
					}
				}
			}
		}
	}

	@Override
	public void sliceFromController(WorkerSwitch workerSwitch, OriginalSwitch originalSwitch) {
		FVMessageUtil.dropUnexpectedMesg(this, originalSwitch);
	}

	/**
	 * Got a dynamically added/removed port,e.g., from an HP add or remove it
	 * from the list of things we poke for topology
	 */
	@Override
	public void topologyController(TopologyConnection topologyConnection) {
		if (this.reason == OFPortReason.OFPPR_ADD.ordinal())
			topologyConnection.addPort(this.getDesc());
		else if (this.reason == OFPortReason.OFPPR_DELETE.ordinal())
			topologyConnection.removePort(this.getDesc());
	}
}
