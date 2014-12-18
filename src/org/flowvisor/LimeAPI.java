package org.flowvisor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("")
public class LimeAPI {
	@GET
	@Path("")
	@Produces(MediaType.TEXT_PLAIN)
	public String index(){
		return "\n Welcome to the Lime API Http Server";
	}
	
	@GET
	@Path("viewTopology")
	@Produces(MediaType.TEXT_PLAIN)
	public String viewTopology(){
		return "\n Will contain the current topology at some point";
	}
	
	@GET
	@Path("viewTopology/test")
	@Produces(MediaType.TEXT_PLAIN)
	public String viewTopologyTest(){
		return "\n Test of jaxws second level directory";
	}
	
	@GET
	@Path("/{pathParam}")
	@Produces(MediaType.TEXT_PLAIN)
	public String testPathParam(@PathParam("pathParam") String pathParam){
		//if used, escape characters. try apache commons escape characters jar if needed
		return "\n Test of REST path parameters. The received path parameter was: "+pathParam;
	}
}
