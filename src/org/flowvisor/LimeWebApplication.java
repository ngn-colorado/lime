package org.flowvisor;

import org.glassfish.jersey.server.ResourceConfig;

public class LimeWebApplication extends ResourceConfig {
	public LimeWebApplication(){
		packages("org.flowvisor");
	}
}
