/**
 * 
 */
package edu.colorado.cs.ngn.lime.api;

/**
 * @author Murad Kaplan
 *
 */

import java.util.concurrent.atomic.AtomicBoolean;

public class LimeServer implements Runnable {
	private AtomicBoolean running;
	private LimeHttpServer server;
	
	public LimeServer(){
		running = new AtomicBoolean(true);
		server = new LimeHttpServer();
	}
	
	public void shutdown(){
		server.stopServer();
		running.set(false);
	}
	
	@Override
	public void run() {
//		System.out.println("MURAD: LimeServer is running...");
//		int portNumber = 8082;
		System.out.println("MICHAEL: Starting Lime HTTP Server");
		Thread thread = new Thread(server);
		thread.start();
		while(running.get()){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Exited lime http server");
		
		
/*		try {
			ServerSocket serverSocket = new ServerSocket(portNumber);
			final LimeMigrationHandler limeMigHandler = new LimeMigrationHandler();
			while(true){
				final Socket clientSocket = serverSocket.accept();
				Runnable handlerTask = new Runnable(){
					@Override
					public void run() {
						System.out.println("Starting new handler thread");
						try {
							PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
							BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
							
							String inputLine;
							while ((inputLine = in.readLine()) != null) {
								System.out.println("Recv from operator: " + inputLine);
								out.println("LIME said: " + inputLine);
								if (inputLine.equals("start")) {
									limeMigHandler.init();
								} else if (inputLine.startsWith("migration finished: ")) {
									// message must be in form: "migration finished:
									// original_dpid clone_dpid
									String[] tokens = inputLine.split(" ");
									DPID originalDPID = new DPID(tokens[2]);
									DPID cloneDPID = new DPID(tokens[3]);
									WorkerSwitch originalSwitch = LimeContainer.getAllWorkingSwitches().get(originalDPID.getDpidLong());
									WorkerSwitch cloneSwitch = LimeContainer.getAllWorkingSwitches().get(cloneDPID.getDpidLong());
									System.out.println("calling done migration function");
									limeMigHandler.switchDoneMigrating(cloneSwitch, originalSwitch);
									System.out.println("migration done function finished");
								}
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				
				Thread handlerThread = new Thread(handlerTask);
				handlerThread.start();
			}
		} catch (IOException e) {
			System.out
			.println("Exception caught when trying to listen on port "
					+ portNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}*/
	}
}
