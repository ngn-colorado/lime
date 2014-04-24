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
				out.println("CM said: " + inputLine);
			}
		} catch (IOException e) {
			System.out.println("Exception caught when trying to listen on port "
					+ portNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}
	}
}


