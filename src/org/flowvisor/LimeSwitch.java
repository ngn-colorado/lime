package org.flowvisor;

public class LimeSwitch {
	
	
	 enum type{
		PENDING("P"), ACTIVE("A"), INACTIVE("I"), DELETED("D");
		 
		private String statusCode;
	 
		private UserStatus(String s) {
			statusCode = s;
		}
	 
		public String getStatusCode() {
			return statusCode;
		}
}
}
	
	