/**
 * 
 */
package org.flowvisor;

/**
 * @author Murad Kaplan
 *
 */
public class PortInfo {

	private PortType portType;
	private String attachmentMAC;  // Host or switch connected this port
	private String attachmentIP; // IP of the device connected tto this host

	public PortInfo(PortType pType, String attMAC, String attIP){
		this.portType 		= pType;
		this.attachmentMAC 	= attMAC;
		this.attachmentIP 	= attIP;
	}
	
	public PortType getType(){
		return portType;
	}

	public void setType(PortType pType) {
		this.portType = pType;
		
	}
	
	public String getAttachmentMAC() {
		return attachmentMAC;
	}

	public String getAttachmentIP() {
		return attachmentIP;
	}

	public enum PortType {
		SW_CONNECTED, // connected to another switch from its own active/clone group of switches)
		H_CONNECTED, // connected to host
		EMPTY,  // not connected to a host
		GHOST, // connected to its duplicate switch
		DUMMY, //port needed for OVX hack for input port on migration mods
		UKNOWN;
    
	}
}
