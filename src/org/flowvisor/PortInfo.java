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
	
	public enum PortType {
		CONNECTED, // connected to switch or host
		EMPTY,  // not connected to anything
		GHOST,
		UKNOWN;
    
	}
}
