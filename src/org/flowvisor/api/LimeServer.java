/**
 * 
 */
package org.flowvisor.api;

/**
 * @author Murad Kaplan
 *
 */

import java.net.*;
import java.io.*;

import org.flowvisor.LimeMigrationHandler;

public  class LimeServer implements Runnable{
	@Override
	public void run(){

		int portNumber = 8082;

		try (
				ServerSocket serverSocket =
				new ServerSocket(portNumber);
				Socket clientSocket = serverSocket.accept();
				PrintWriter out =
						new PrintWriter(clientSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				) {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				System.out.println("Recv from operator: " + inputLine);
				out.println("LIME said: " + inputLine);
				if(inputLine.equals("start")){
					LimeMigrationHandler limeMigHandler = new LimeMigrationHandler();
					limeMigHandler.init();
				}
				
			}
		} catch (IOException | InterruptedException e) {
			System.out.println("Exception caught when trying to listen on port "
					+ portNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}
	}
}


