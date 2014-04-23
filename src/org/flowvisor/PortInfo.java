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
	
	public String getType(){
		return portType.getType();
	}


	public enum PortType {
		CONNECTED("C"),
		EMPTY("E"),
		GHOST("G");

		protected String type;
		private PortType(String type) {
			this.type = type;
		}
		public String getType() {
			return type;
		}        
	}

	public void useEnum(){
		PortType pInfo = PortType.CONNECTED;
		pInfo.getType();
	}
}
