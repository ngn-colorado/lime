package org.flowvisor;

import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flowvisor.LimeUtils.JsonFormat;

import org.flowvisor.LimeUtils.JsonFormat;

@Path("")
public class LimeAPI {
	private LimeMigrationHandler migrationHandler = null;
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
		if(migrationHandler == null){
			return "\n A migration handler object must be defined";
		}
		try {
			migrationHandler.init();
			return "\n Migration handler initiated migration";
		} catch (InterruptedException e) {
			return "\n Migration handler encountered an error";
		}
		
	}
	
	@GET
	@Path("/finishMigration/{DPID}")
	@Produces(MediaType.TEXT_PLAIN)
	public String finishMigration(@PathParam("DPID") String DPID){
		if(migrationHandler == null){
			return "\n A migration handler object must be defined";
		}
	//TODO: change this method to have the handler lookup the clone switch of the switch that is done migrating in the table, so only the dpid
	//of the switch done migrating needs to be provided to the api
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
		boolean processedJson = LimeUtils.parseJsonConfig(data, LimeUtils.JsonFormat.SWITCH, migrationHandler);
		String response = "Configuration was processed ";
		return processedJson ? response  + "successfully\n" : response + "unsuccessfully\n";
	}
	
	@POST
	@Path("/migrateVM")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String migrateVM(String data){
		boolean processed = LimeUtils.parseJsonConfig(data, LimeUtils.JsonFormat.HOST, migrationHandler);
		String response = "Machine information was processed ";
		return processed ? response  + "successfully\n" : response + "unsuccessfully\n";
//		if(currentHost == null){
//			return response + "unsuccessfully\n";
//		} else{
//			migrationHandler.migrateVMAsynchronously(currentHost);
//			return response + ;
//		}
	}
	
	public void setMigrationHandler(LimeMigrationHandler handler){
		this.migrationHandler = handler;
	}
}
