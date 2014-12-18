/**
 * 
 */
package org.flowvisor.api;

/**
 * @author Murad Kaplan
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.flowvisor.DPID;
import org.flowvisor.LimeContainer;
import org.flowvisor.LimeMigrationHandler;
import org.flowvisor.classifier.WorkerSwitch;

import com.sun.net.httpserver.HttpServer;

public class LimeServer implements Runnable {
	private HttpServer httpServer;
	
//	public LimeServer(int apiPort){
//		httpServer
//	}
	
	
	
	@Override
	public void run() {
		System.out.println("MURAD: LimeServer is running...");
		int portNumber = 8082;

		
		
		
		
		try {
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
		}
	}
}
