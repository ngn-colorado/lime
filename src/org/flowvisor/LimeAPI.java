package org.flowvisor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("api")
public class LimeAPI {
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String get(){
		return "\n Welcome to the Lime API Http Server";
	}
}
