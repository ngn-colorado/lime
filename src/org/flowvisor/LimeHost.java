package org.flowvisor;
/**
 * Container class to hold information about hosts when they need to be migrated.
 * Will be populated by the api when a host migration needs to occur
 * 
 * Hopefully is immutable
 * 
 * @author michael
 *
 */
public final class LimeHost {
	//required
	private final String originalHost;
	//required
	private final String destinationHost;
	//required
	private final String libvirtDomain;
	//required
	private final DPID originalDpid;
	//required
	private final DPID cloneDpid;
	//required
	private final Short connectedPort;
	//not required for now, will be in future
	private final Short clonePort;
	public LimeHost(String originalHost, String destinationHost,
			String libvirtDomain, DPID originalDpid, DPID cloneDpid,
			Short connectedPort, Short clonePort) {
		super();
		this.originalHost = originalHost;
		this.destinationHost = destinationHost;
		this.libvirtDomain = libvirtDomain;
		this.originalDpid = originalDpid;
		this.cloneDpid = cloneDpid;
		this.connectedPort = connectedPort;
		this.clonePort = clonePort;
	}
	public String getOriginalHost() {
		return originalHost;
	}
	public String getDestinationHost() {
		return destinationHost;
	}
	public String getLibvirtDomain() {
		return libvirtDomain;
	}
	public DPID getOriginalDpid() {
		return originalDpid;
	}
	public DPID getCloneDpid() {
		return cloneDpid;
	}
	public Short getConnectedPort() {
		return connectedPort;
	}
	public Short getClonePort() {
		return clonePort;
	}
}
