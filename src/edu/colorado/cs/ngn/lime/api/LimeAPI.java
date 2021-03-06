package edu.colorado.cs.ngn.lime.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.colorado.cs.ngn.lime.exceptions.DPIDLookupException;
import edu.colorado.cs.ngn.lime.exceptions.LimeDummyPortNotFoundException;
import edu.colorado.cs.ngn.lime.exceptions.SwitchOriginalAndCloneException;
import edu.colorado.cs.ngn.lime.migration.LimeMigrationHandler;
import edu.colorado.cs.ngn.lime.util.DPID;
import edu.colorado.cs.ngn.lime.util.LimeAPIUtils;

/**
 * Class that implements the embedded http api
 * 
 * @author Michael Coughlin
 *
 */
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
	
	@GET
	@Path("/startMigration")
	@Produces(MediaType.TEXT_PLAIN)
	public String startMigration(){
		LimeMigrationHandler migrationHandler = LimeMigrationHandler.getInstance();
		if(migrationHandler == null){
			return "\n A migration handler object must be defined";
		}
		try {
			migrationHandler.init();
			return "\n Migration handler initiated migration";
		} catch (InterruptedException e) {
			return "\n Migration handler encountered an error";
		} catch (LimeDummyPortNotFoundException e) {
			String message = "\n Need to have a dummy port in order to work with OVX";
			return message;
		} catch (SwitchOriginalAndCloneException e) {
			String message = "Caught illegal state where a switch is both an original and clone switch";
			System.out.println(message);
			return message;
		} catch (DPIDLookupException e) {
			String message = "Tried to lookup a dpid that did not exist";
			System.out.println(message);
			return message;
		}
		
	}
	
	@GET
	@Path("/finishMigration/{DPID}")
	@Produces(MediaType.TEXT_PLAIN)
	public String finishMigration(@PathParam("DPID") String DPID){
			//TODO: change this method to have the handler lookup the clone switch of the switch that is done migrating in the table, so only the dpid
	//of the switch done migrating needs to be provided to the api
		LimeMigrationHandler migrationHandler = LimeMigrationHandler.getInstance();
		DPID finishedSwitch = new DPID(DPID);
		migrationHandler.switchDoneMigrating(finishedSwitch);
		return "\n Handler finished migration for switch "+finishedSwitch.getDpidString();
	}
	
	@POST
	@Path("/config")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String config(String data){
		System.out.println("Received data:\n"+data);
		String processedJsonResponse = LimeAPIUtils.parseJsonConfig(data, LimeAPIUtils.JsonFormat.SWITCH);
		if(processedJsonResponse != null){
			return processedJsonResponse;
		} else{
			return "Error encountered";
		}
	}
	
	@POST
	@Path("/migrateVM")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String migrateVM(String data){
		String processedResponse = LimeAPIUtils.parseJsonConfig(data, LimeAPIUtils.JsonFormat.HOST);
		if(processedResponse != null){
			return processedResponse;
		} else{
			return "Error encountered";
		}
	}
}
