/**
 * 
 */
package edu.colorado.cs.ngn.lime.api;

/**
 * Class to run the lime http server
 * 
 * @author Murad Kaplan, Michael Coughlin
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
	}
}
