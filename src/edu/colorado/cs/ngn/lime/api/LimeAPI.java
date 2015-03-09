package edu.colorado.cs.ngn.lime.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.colorado.cs.ngn.lime.exceptions.LimeDummyPortNotFoundException;
import edu.colorado.cs.ngn.lime.migration.LimeMigrationHandler;
import edu.colorado.cs.ngn.lime.util.DPID;
import edu.colorado.cs.ngn.lime.util.LimeAPIUtils;
import edu.colorado.cs.ngn.lime.util.PortInfo.PortType;

/**
 * Class that implements the embedded HTTP API
 * 
 * API documentation available in the method javadocs. Assumes that the http api is running on http://localhost:9000
 * 
 * Replace localhost:9000 with the url and port used in production, if different, to make api calls
 * 
 * @author Michael Coughlin
 *
 */
@Path("")
public class LimeAPI {
	/**
	 * HTTP GET request
	 * http://localhost:9000/
	 * 
	 * Simple welcome message
	 * 
	 * @return String to print to web page
	 */
	@GET
	@Path("")
	@Produces(MediaType.TEXT_PLAIN)
	public String index(){
		return "\n Welcome to the Lime API Http Server";
	}
	
	/**
	 * HTTP GET request
	 * http://localhost:9000/viewTopology
	 * 
	 * This API call is unimplemented. Will eventually hold a view of the topology
	 * 
	 * @return String to print to webpage
	 */
	@GET
	@Path("viewTopology")
	@Produces(MediaType.TEXT_PLAIN)
	public String viewTopology(){
		return "\n Will contain the current topology at some point";
	}
	
	/**
	 * HTTP GET request
	 * http://localhost:9000/startMigration
	 * 
	 * Use this API call to begin the migration of a network
	 * 
	 * @return String to print to webpage
	 */
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
		}
		
	}
	
	/**
	 * HTTP GET request
	 * http://localhost:9000/finishMigration/<dpid>
	 * 
	 * Use this API call to signal that a given switch DPID is finished migrating. Replace <dpid> in the path
	 * with the hex string of the given switch's DPID. Example: http://localhost:9000/finishMigration/00:00:00:00:00:00:00:00
	 * 
	 * @param DPID The dpid to signal that migration has finished on
	 * @return String to print to webpage
	 */
	@GET
	@Path("/finishMigration/{DPID}")
	@Produces(MediaType.TEXT_PLAIN)
	public String finishMigration(@PathParam("DPID") String DPID){
		LimeMigrationHandler migrationHandler = LimeMigrationHandler.getInstance();
		DPID finishedSwitch = new DPID(DPID);
		migrationHandler.switchDoneMigrating(finishedSwitch);
		return "\n Handler finished migration for switch "+finishedSwitch.getDpidString();
	}
	
	/**
	 * HTTP POST request
	 * http://localhost:9000/config
	 * 
	 * Use this API call to pass JSON configuration strings to Lime. Note that the config is passed as the data
	 * in an HTTP POST request.
	 * 
	 * Currently, this API call only handles switch configurations, but may be expanded to other options in the future.
	 * To configure a switch, the JSON configuration must be of the form:
	 *  <br />
	 *  { <br />
	 *  	"<switch_dpid>": 							<-- Replace <switch_dpid> with the DPID of the switch to configure<br />
	 *  	{<br />
	 *  		"ports": <br />
	 *  		{<br />
	 *  			"<port_number>" : 					<-- Replace <port_number> with the value of the switch port to configure<br />
	 *  			{<br />
	 *  				"type" : "<PortType>",	    	<--Must be one of the valid values in the enum {@link edu.colorado.cs.ngn.lime.util.PortType}. Replace <PortType> with this value<br />
	 *  				"mac" : "<MAC_address>"   		<--optional. Must be provided if the type == H_CONNECTED, otherwise is ignored<br />
	 *  			},<br />
	 *  			"<port_number>" :<br /> 
	 *  			{<br />
	 *  				"type" : "DUMMY"				<--Note that a dummy port must always be provided<br />
	 *  			}<br />
	 *  		}<br />
	 *  		"original":"<original_switch_dpid>"		<--Optional. Used to identify the original switch of a clone switch. If this is provided, the switch <switch_dpid> is a clone. Otherwise is an original. Replace <original_switch_dpid> with the switch DPID.<br />
	 *  	}<br />
	 *  }<br />
	 * 
	 * @param data The JSON configuration data
	 * @return String to print to webpage
	 */
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
	
	/**
	 * HTTP POST request
	 * http://localhost:9000/migrateVM
	 * 
	 * Use this API call to initiate a VM migration. Note that the config is passed as the data
	 * in an HTTP POST request.
	 * 
	 * The JSON string passed to migrate a VM must be of this form:
	 * <br />
	 * {<br />
	 * 		"originalHost" : "<original_hypervisor_ip_address>",			<-- replace <original_hypervisor_ip_address> with the IP address of the source libvirt hypervisor<br />
	 * 		"destinationHost" : "<destination_hypervisor_ip_address>",		<-- replace <destination_hypervisor_ip_address> with the IP address of the destination libvirt hypervisor<br />
	 * 		"domain" : "<libvirt_domain>",									<-- replace <libvirt_domain> with the name of the libvirt domain to be migrated <br />
	 * 		"originalDpid" : "<original_switch_dpid>",						<-- replace <original_switch_dpid> with the DPID that the domain was originally connected to<br />
	 * 		"cloneDpid" : "<clone_switch_dpid>",							<-- replace <clone_switch_dpid> with the DPID that the domain will be connected to after migration<br />
	 * 		"connectedPort" : "<original_connected_port>",					<-- replace <original_connected_port> with the port that the domain was connected to on the original switch before migration<br />
	 * 		"clonePort" : "<clone_connected_port>"							<-- NOTE that this parameter is inconsistently used currently, but is needed for compatibility and will be used in future. replace <clone_connected_port> with the port the domain will be connected to on the clone switch after migration. please ensure that it is the same value as <connectedPort> for now<br />
	 * }<br />
	 * 
	 * @param data The JSON configuration data
	 * @return String to print to webpage
	 */
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
