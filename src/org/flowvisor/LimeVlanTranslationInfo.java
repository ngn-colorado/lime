package org.flowvisor;

import org.flowvisor.message.FVFlowMod;

public final class LimeVlanTranslationInfo {
	private final FVFlowMod receiverMod;
	private final FVFlowMod senderMod;
	private final DPID receiverSwitch;
	private final DPID senderSwitch;
	private final boolean originalToClone;
	private final FVFlowMod originalMod;
	private final int vlanNumber;
	@Override
	public String toString() {
		return "LimeVlanTranslationInfo [receiverMod=" + receiverMod
				+ ", senderMod=" + senderMod + ", receiverSwitch="
				+ receiverSwitch + ", senderSwitch=" + senderSwitch
				+ ", originalToClone=" + originalToClone + ", originalMod="
				+ originalMod + ", vlanNumber=" + vlanNumber + "]";
	}
	public FVFlowMod getReceiverMod() {
		return receiverMod;
	}
	public FVFlowMod getSenderMod() {
		return senderMod;
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
	public int getVlanNumber() {
		return vlanNumber;
	}
	public LimeVlanTranslationInfo(FVFlowMod receiverMod, FVFlowMod senderMod,
			DPID receiverSwitch, DPID senderSwitch, boolean originalToClone,
			FVFlowMod originalMod, int vlanNumber) {
		super();
		this.receiverMod = receiverMod;
		this.senderMod = senderMod;
		this.receiverSwitch = receiverSwitch;
		this.senderSwitch = senderSwitch;
		this.originalToClone = originalToClone;
		this.originalMod = originalMod;
		this.vlanNumber = vlanNumber;
	}
	
}
