package org.flowvisor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.xmlrpc.webserver.WebServer;
import org.flowvisor.api.APIServer;
import org.flowvisor.api.JettyServer;
import org.flowvisor.config.ConfDBHandler;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.FVConfigurationController;
import org.flowvisor.config.FlowSpaceImpl;
import org.flowvisor.config.FlowvisorImpl;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.config.SwitchImpl;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.log.StderrLogger;
import org.flowvisor.log.ThreadLogger;
import org.flowvisor.message.FVMessageFactory;
import org.flowvisor.ofswitch.OFSwitchAcceptor;
import org.flowvisor.ofswitch.TopologyController;
import org.flowvisor.resources.SlicerLimits;
import org.openflow.example.cli.Option;
import org.openflow.example.cli.Options;
import org.openflow.example.cli.ParseException;
import org.openflow.example.cli.SimpleCLI;

import edu.colorado.cs.ngn.lime.api.LimeHttpServer;
import edu.colorado.cs.ngn.lime.api.LimeServer;
import edu.colorado.cs.ngn.lime.util.PortInfo.PortType;
import sun.io.MalformedInputException;

public class FlowVisor {
	// VENDOR EXTENSION ID
	public final static int FLOWVISOR_VENDOR_EXTENSION = 0x80000001;

	// VERSION
	public final static String FLOWVISOR_VERSION = "flowvisor-1.4.0";


	public final static int FLOWVISOR_DB_VERSION = 2;


	// Max slicename len ; used in LLDP for now; needs to be 1 byte
	public final static int MAX_SLICENAME_LEN = 255;

	/********/
	String configFile = null;
	List<FVEventHandler> handlers;

	private int port;
	private int jettyPort = -1;

	private WebServer apiServer;
	static FlowVisor instance;
	private SlicerLimits sliceLimits;

	FVMessageFactory factory;

	private static final Options options = Options.make(new Option[] {
			new Option("d", "debug", LogLevel.NOTE.toString(),
					"Override default logging threshold in config"),
					new Option("l", "logging", "Log to stderr instead of syslog"),
					new Option("p", "port", 0, "Override OpenFlow port from config"),
					new Option("h", "help", "Print help"),
					new Option("j", "JSON web api port",8081, "Override JSON API port from config"),

	});

	public FlowVisor() {
		this.port = 0;
		this.handlers = new ArrayList<FVEventHandler>();
		this.factory = new FVMessageFactory();
	}

	/*
	 * Unregister this event handler from the system
	 */

	/**
	 * @return the configFile
	 */
	public String getConfigFile() {
		return configFile;
	}

	/**
	 * @param configFile
	 *            the configFile to set
	 */
	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	public int getJettyPort(){
		return jettyPort;
	}
	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		try {
			FlowvisorImpl.getProxy().setListenPort(port);
		} catch (ConfigError e) {
			FVLog.log(LogLevel.WARN, null, "Failed to set api port");
		}
	}

	public void setJettyPort(int port){
		try {
			FlowvisorImpl.getProxy().setJettyPort(port);
		} catch (ConfigError e) {
			FVLog.log(LogLevel.WARN, null, "Failed to set jetty port");
		}
	}

	/**
	 * @return the factory
	 */
	public FVMessageFactory getFactory() {
		return factory;
	}

	/**
	 * @param factory
	 *            the factory to set
	 */
	public void setFactory(FVMessageFactory factory) {
		this.factory = factory;
	}

	public synchronized boolean unregisterHandler(FVEventHandler handler) {
		if (handlers.contains(handler)) {
			handlers.remove(handler);
			return true;
		}
		return false;
	}

	public void run() throws ConfigError, IOException, UnhandledEvent {
		FVLog.log(LogLevel.DEBUG, null, "HALLO");
		FlowVisor.setInstance(this);
		Runtime.getRuntime().addShutdownHook(new ShutdownHook());
		// init polling loop
		FVLog.log(LogLevel.INFO, null, "initializing poll loop");
		FVEventLoop pollLoop = new FVEventLoop();
		sliceLimits = new SlicerLimits();

		JettyServer.spawnJettyServer(FVConfig.getJettyPort());//jettyPort);

		if (port == 0)
			port = FVConfig.getListenPort();

		// init topology discovery, if configured for it
		if (TopologyController.isConfigured()){
			System.out.println("MURAD: TopologyController is configured");
			handlers.add(TopologyController.spawn(pollLoop));
		}
		else{
			System.out.println("MURAD: TopologyController is NOT configured");
		}

		// get switches from configured slices

		// TODO the following for-loop just for testing and it is assuming this topology
		
		// top switch only has two ports and they are connected to switches (SW_CONNECTED)
//		HashMap<Short, PortInfo> portTable = new HashMap<>();
//		portTable.put((short) 1, new PortInfo(PortType.H_CONNECTED, null, null));
//		portTable.put((short) 2, new PortInfo(PortType.H_CONNECTED, null, null));

		
		
		//in ovx-> physical hosts are connected to virtual switches based on host mac -> do not need to know the physical port # on the ovs
		
		
//		ubuntulime-2 br0 DPID:
//		00003e5286851a47
//		00:00:3e:52:86:85:1a:47
		
//		port table:
//		port 1: vnet0 -> first running vm -> mac: fe:54:00:83:4d:44
//		port 2: br0.tap -> br0.tap mac: 0a:dc:f8:03:34:6e
//		port 3: guest.gre -> ghost port? mac: b6:85:27:d9:fc:3e
//		port 4: vnet1 -> second running vm mac: fe:54:00:c4:90:dd
//		specify port number
		
//		ubuntulime-3 br0 DPID:
//		0000f2222aa1e448
//		00:00:f2:22:2a:a1:e4:48
		
//		port table:
//		
//		port 2: br0.tap -> mac: 42:43:99:38:5f:1a
//		port 3: guest.gre -> ghost port? mac: be:74:af:6f:01:10
//		port 4: vnet0 -> 
		
		
//		ubuntu-int1 mac: 52:54:00:c4:90:dd
//		ubuntu-int2 mac: 52:54:00:83:4d:44
		
//		DPID ubuntulime2 = new DPID("00003e5286851a47");
//		00:00:3e:52:86:85:1a:47
//		DPID ubuntulime3 = new DPID("0000f2222aa1e448");
//		00:00:f2:22:2a:a1:e4:48
		
//		DPID of original ovx Vswitch: "00:a4:23:05:00:00:00:01"
//		DPID of clone ovx Vswitch: "00:a4:23:05:00:00:00:01"
		
//		create flow rules, as learning switch in floodlight does not work:
//		curl -d '{"switch": "00:a4:23:05:00:00:00:01", "name":"flow-mod-1", "cookie":"0", "priority":"32768", "ingress-port":"1","active":"true", "actions":"output=2"}' http://172.16.1.6:8080/wm/staticflowentrypusher/json
//		curl -d '{"switch": "00:a4:23:05:00:00:00:01", "name":"flow-mod-2", "cookie":"0", "priority":"32768", "ingress-port":"2","active":"true", "actions":"output=1"}' http://172.16.1.6:8080/wm/staticflowentrypusher/json
		
//		ovs on ubuntulime-2 and ubuntulime-3 must have ovx vm (172.16.1.5) as the of controller
//		ovs vm must have flowvisor/lime vm as controller (172.16.1.4)
//		flowvisor/lime must know that floodlight vm (172.16.1.6) is the top level controller
//		set the ip of the floodlight controller in org.flowvisor.slicer.OriginalSwitch as the hostname variable in the init function
		
//		DPID ubuntu-ngn-r720-2: 0000fe58101cc94f
//		00:00:fe:58:10:1c:c9:4f
//		DIPD ubuntu-ngn-r720-3: 0000eeee35e82748
//		00:00:ee:ee:35:e8:27:48
		
//		first vSwitch in ovx is: 00:a4:23:05:00:00:00:01
//		DPID originalVswitch = new DPID("00:a4:23:05:00:00:00:01");
//		second vSwitch in ovx is: 00:a4:23:05:00:00:00:02
//		DPID cloneVswitch = new DPID("00:a4:23:05:00:00:00:02");
		
//		HashMap<Short, PortInfo> originalVswitchporttable = new HashMap<>();
//		originalVswitchporttable.put((short)1, new PortInfo(PortType.GHOST, null, null));
//		originalVswitchporttable.put((short)2, new PortInfo(PortType.H_CONNECTED, null, null));
//		originalVswitchporttable.put((short)3, new PortInfo(PortType.H_CONNECTED, null, null));
//		
//		HashMap<Short, PortInfo> cloneVswitchporttable = new HashMap<>();
//		cloneVswitchporttable.put((short)1, new PortInfo(PortType.GHOST, null, null));
//		cloneVswitchporttable.put((short)2, new PortInfo(PortType.EMPTY, null, null));
//		cloneVswitchporttable.put((short)3, new PortInfo(PortType.EMPTY, null, null));
		
		//TODO: check/convert LimeContainer to a singleton
		
//		LimeContainer.addOriginalSwitch(originalVswitch.getDpidLong(), originalVswitchporttable);
//		LimeContainer.insertActiveToOriginalSwitchMap(originalVswitch.getDpidLong(), originalVswitch.getDpidLong());
//		System.out.println("MICHAEL: Original switch: "+originalVswitch.getDpidLong());
		
		
//		LimeContainer.addOriginalSwitch(cloneVswitch.getDpidLong(), cloneVswitchporttable);
//		LimeContainer.insertActiveToOriginalSwitchMap(cloneVswitch.getDpidLong(), cloneVswitch.getDpidLong());
//		System.out.println("MICHAEL: Clone switch: "+cloneVswitch.getDpidLong());
//		LimeContainer.addCloneSwitch(cloneVswitch.getDpidLong(), cloneVswitchporttable);
//		LimeContainer.insertActiveToCloneSwitchMap(originalVswitch.getDpidLong(), cloneVswitch.getDpidLong());
		
		
		
		/*LimeContainer.addOriginalSwitch(46200400562356225L, portTable);  // this represnts the switch 00:a4:23:05:00:00:00:01 in OVX
		LimeContainer.insertActiveToOriginalSwitchMap(46200400562356225L, 46200400562356225L);
		System.out.println("MURAD: Original Top-level Switch: " + 46200400562356225L);*/
		
		/*LimeContainer.addOriginalSwitch(1, portTable);
		LimeContainer.insertActiveToOriginalSwitchMap(1, 1);
		System.out.println("MURAD: Original Top-level Switch: " + 1);*/
		
//		LimeContainer.addOriginalSwitch(256, portTable);
//		LimeContainer.insertActiveToOriginalSwitchMap(256, 256);
//		System.out.println("MURAD: Original Top-level Switch: " + 256);
//		these dpids are: 00:00:A4:23:05:00:00:00:02 to 00:00:A4:23:05:00:00:00:04
		//for(long j=46200400562356226L; j<46200400562356228L; j++){
		//for(long j=2; j<4; j++){
		/*for(long j=512; j<769; j=j+256){	
			portTable = new HashMap<>();
			for(short i= 1; i<3; i++){
				portTable.put(i, new PortInfo(PortType.H_CONNECTED, null, null));
			}
			portTable.put((short) 3, new PortInfo(PortType.SW_CONNECTED, null, null));
			
			
			LimeContainer.addOriginalSwitch(j, portTable);
			LimeContainer.insertActiveToOriginalSwitchMap(j, j);
			System.out.println("MURAD: Original Second-level Switch: " + j);
		}*/
		
		

		//Set flows = FlowSpaceUtil.getFlowMap(1).getRules();
		//Iterator it = flows.iterator();
		/*while (it.hasNext()) {
			// Get element
			FlowEntry element = (FlowEntry) it.next();
			System.out.println("Murad: Original Seen Switch: " + element.getDpid());
			//LimeContainer.insertOriginalSeenSwitches(element.getDpid()); // TODO, inset switch info. not only ID

			LimeContainer.insertActiveToOriginalSwitchMap(element.getDpid(), element.getDpid());
		}*/
		
		// start Lime server
//		LimeServer lServer = new LimeServer();
//		new Thread(lServer).start();
		
		LimeHttpServer httpServer = new LimeHttpServer();
		Thread serverThread = new Thread(httpServer);
		serverThread.start();

		// init switchAcceptor
		OFSwitchAcceptor acceptor = new OFSwitchAcceptor(pollLoop, port, 16);
		acceptor.setSlicerLimits(sliceLimits);
		handlers.add(acceptor);
		// start XMLRPC UserAPI server; FIXME not async!
		try {
			this.apiServer = APIServer.spawn();
		} catch (Exception e) {
			FVLog.log(LogLevel.FATAL, null, "failed to spawn APIServer");
			e.printStackTrace();
			System.exit(-1);
		}

		// print some system state
		boolean flowdb = false;
		try {
			if (FVConfig.getFlowTracking())
				flowdb = true;
		} catch (ConfigError e) {
			// assume off if not set
			FVConfig.setFlowTracking(false);
			this.checkPointConfig();
		}
		if (!flowdb)
			FVLog.log(LogLevel.INFO, null, "flowdb: Disabled");

		// start event processing
		pollLoop.doEventLoop();
	}
	
	
	
	

	
	/**
	 * FlowVisor Daemon Executable Main
	 *
	 * Takes a config file as only parameter
	 *
	 * @param args
	 *            config file
	 * @throws Throwable
	 */

	public static void main(String args[]) throws Throwable {

		ThreadLogger threadLogger = new ThreadLogger();
		Thread.setDefaultUncaughtExceptionHandler(threadLogger);		
		long lastRestart = System.currentTimeMillis();
		FVConfigurationController.init(new ConfDBHandler());
		while (true) {
			FlowVisor fv = new FlowVisor();
			fv.parseArgs(args);

			try {
				// load config from file
				updateDB();
				if (fv.configFile != null)
					FVConfig.readFromFile(fv.configFile);
				else 
					// Set temp file for config checkpointing.
					fv.configFile = "/tmp/flowisor";

				System.err.println("MURAD: Running FV");
				fv.run(); 
				System.err.println("MURAD: After Running FV");
				
			}  catch (NullPointerException e) {
				System.out.println("MURAD: Startup failed : ");
				 e.printStackTrace();
				System.exit(1);
			} catch (Throwable e) {
				e.printStackTrace();
				FVLog.log(LogLevel.CRIT, null, "MAIN THREAD DIED!!!");
				FVLog.log(LogLevel.CRIT, null, "----------------------------");
				threadLogger.uncaughtException(Thread.currentThread(), e);
				FVLog.log(LogLevel.CRIT, null, "----------------------------");
				if ((lastRestart + 5000) > System.currentTimeMillis()) {
					System.err.println("respawning too fast -- DYING");
					FVLog.log(LogLevel.CRIT, null,
							"respawning too fast -- DYING");
					fv.tearDown();
					throw e;
				} else {
					FVLog.log(LogLevel.CRIT, null,
							"restarting after main thread died");
					lastRestart = System.currentTimeMillis();
					fv.tearDown();
				}
				fv = null;
				System.gc(); // give the system a bit to clean up after itself
				Thread.sleep(1000);
			} 
		}
	}



	private void parseArgs(String[] args) {
		SimpleCLI cmd = null;
		try {
			cmd = SimpleCLI.parse(options, args);

		} catch (ParseException e) {
			usage("ParseException: " + e.toString());
		}
		if (cmd == null)
			usage("need to specify arguments");
		int i = cmd.getOptind();
		if (i >= args.length)
			setConfigFile(null);
		else
			setConfigFile(args[i]);

		if (cmd.hasOption("d")) {
			FVLog.setThreshold(LogLevel.valueOf(cmd.getOptionValue("d")));
			System.err.println("Set default logging threshold to "
					+ FVLog.getThreshold());
		}
		if (cmd.hasOption("l")) {
			System.err.println("Setting debugging mode: all logs to stderr");
			FVLog.setDefaultLogger(new StderrLogger());
		}
		if (cmd.hasOption("p")) {
			int p = Integer.valueOf(cmd.getOptionValue("p"));
			setPort(p);
			System.err.println("Writting port to config: setting to "
					+ p);
		}
		if(cmd.hasOption("j")){
			int jp = Integer.valueOf(cmd.getOptionValue("j"));
			setJettyPort(jp);
			System.err.println("Writting jetty port to config: setting to "
					+ jp);
		}

		if(cmd.hasOption("h")){
			usage("FlowVisor Help");
			System.exit(0);
		}

	}

	private void tearDown() {
		if (this.apiServer != null)
			this.apiServer.shutdown(); // shutdown the API Server
		List<FVEventHandler> tmp = this.handlers;
		this.handlers = new LinkedList<FVEventHandler>();
		for (Iterator<FVEventHandler> it = tmp.iterator(); it.hasNext();) {
			FVEventHandler handler = it.next();
			it.remove();
			handler.tearDown();
		}
	}

	/**
	 * Print usage message and warning string then exit
	 *
	 * @param string
	 *            warning
	 */

	private static void usage(String string) {
		System.err.println("FlowVisor version: " + FLOWVISOR_VERSION);
		System.err
		.println("Ali Al-Shabibi: ali.al-shabibi@onlab.us");
		System.err
		.println("---------------------------------------------------------------");
		System.err.println("\n msg: " + string + "\n");
		SimpleCLI.printHelp("FlowVisor [options] [config.json]",
				FlowVisor.getOptions());
		System.exit(-1);
	}

	private static Options getOptions() {

		return FlowVisor.options;
	}

	/**
	 * Get the running fv instance
	 *
	 * @return
	 */
	public static FlowVisor getInstance() {
		return instance;
	}

	/**
	 * Set the running fv instance
	 *
	 * @param instance
	 */
	public static void setInstance(FlowVisor instance) {
		FlowVisor.instance = instance;
	}

	/**
	 * Returns a unique, shallow copy of the list of event handlers registered
	 * in the flowvisor
	 *
	 * Is unique to prevent concurrency problems, i.e., when wakling through the
	 * list and a handler gets deleted
	 *
	 * @return
	 */
	public synchronized ArrayList<FVEventHandler> getHandlersCopy() {
		return new ArrayList<FVEventHandler>(handlers);
	}

	public void addHandler(FVEventHandler handler) {
		this.handlers.add(handler);
	}

	public void removeHandler(FVEventHandler handler) {
		this.handlers.remove(handler);
	}

	public void setHandlers(ArrayList<FVEventHandler> handlers) {
		this.handlers = handlers;
	}

	/**
	 * Save the running config back to disk
	 *
	 * Write to a temp file and only if it succeeds, move it into place
	 *
	 * FIXME: add versioning
	 */
	public void checkPointConfig() {
		// FIXME dump db file!!


		String tmpFile = this.configFile + ".tmp"; // assumes no one else can
		// write to same dir
		// else security problem

		// do we want checkpointing?
		try {
			if (!FVConfig.getCheckPoint())
				return;
		} catch (ConfigError e1) {
			FVLog.log(LogLevel.WARN, null,
					"Checkpointing config not set: assuming you want checkpointing");
		}

		try {
			FVConfig.writeToFile(tmpFile);
		} catch (FileNotFoundException e) {
			FVLog.log(LogLevel.CRIT, null,
					"failed to save config: tried to write to '" + tmpFile
					+ "' but got FileNotFoundException");
			return;
		}
		// sometimes, Java has the stoopidest ways of doing things :-(
		File tmp = new File(tmpFile);
		if (tmp.length() == 0) {
			FVLog.log(LogLevel.CRIT, null,
					"failed to save config: tried to write to '" + tmpFile
					+ "' but wrote empty file");
			return;
		}

		tmp.renameTo(new File(this.configFile));
		FVLog.log(LogLevel.INFO, null, "Saved config to disk at "
				+ this.configFile);
	}

	public String getInstanceName() {
		// TODO pull from FVConfig; needed for slice stiching
		return "magic flowvisor1";
	}



	private static void updateDB() {
		int db_version = FlowvisorImpl.getProxy().fetchDBVersion();
		if (db_version == FLOWVISOR_DB_VERSION)
			return;
		if (db_version > FLOWVISOR_DB_VERSION)
			FVLog.log(LogLevel.WARN, null, "Your FlowVisor comes from the future.");
		FlowvisorImpl.getProxy().updateDB(db_version);
		SliceImpl.getProxy().updateDB(db_version);
		FlowSpaceImpl.getProxy().updateDB(db_version);
		SwitchImpl.getProxy().updateDB(db_version);

	}



}
