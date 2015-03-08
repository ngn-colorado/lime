package edu.colorado.cs.ngn.lime.migration;

import java.util.List;
import java.util.Map;

import org.flowvisor.message.FVFlowMod;

import edu.colorado.cs.ngn.lime.util.DPID;

/**
 * Class to hold state of vlan migration OF mods
 * 
 * @author Michael Coughlin
 *
 */
public final class LimeVlanTranslationInfo {
	private final Map<FVFlowMod, List<FVFlowMod>> migrationModPairs;
	private final DPID receiverSwitch;
	private final DPID senderSwitch;
	private final boolean originalToClone;
	private final FVFlowMod originalMod;
	private final boolean receiverTargetMigrated;
	public LimeVlanTranslationInfo(
			Map<FVFlowMod, List<FVFlowMod>> migrationModPairs,
			DPID receiverSwitch, DPID senderSwitch, boolean originalToClone,
			FVFlowMod originalMod, boolean receiverTargetMigrated) {
		super();
		this.migrationModPairs = migrationModPairs;
		this.receiverSwitch = receiverSwitch;
		this.senderSwitch = senderSwitch;
		this.originalToClone = originalToClone;
		this.originalMod = originalMod;
		this.receiverTargetMigrated = receiverTargetMigrated;
	}
	public Map<FVFlowMod, List<FVFlowMod>> getMigrationModPairs() {
		return migrationModPairs;
	}
	public DPID getReceiverSwitch() {
		return receiverSwitch;
	}
	public DPID getSenderSwitch() {
		return senderSwitch;
	}
	public boolean isOriginalToClone() {
		return originalToClone;
	}
	public FVFlowMod getOriginalMod() {
		return originalMod;
	}
	public boolean isReceiverTargetMigrated() {
		return receiverTargetMigrated;
	}
	@Override
	public String toString() {
		return "LimeVlanTranslationInfo [migrationModPairs="
				+ migrationModPairs + ", receiverSwitch=" + receiverSwitch
				+ ", senderSwitch=" + senderSwitch + ", originalToClone="
				+ originalToClone + ", originalMod=" + originalMod
				+ ", receiverTargetMigrated="
				+ receiverTargetMigrated + "]";
	}
	
}
