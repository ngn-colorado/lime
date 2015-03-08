package edu.colorado.cs.ngn.lime.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Embedded http server used by the Lime api. Utilizes an embedded grizzly2 server.
 * The default port is 9000.
 * 
 * @author Michael Coughlin
 *
 */
public class LimeHttpServer implements Runnable{
	public static final String BASE_LIME_URI_STRING = "http://0.0.0.0/";
	public static final int BASE_LIME_PORT = 9000;
	public static final String BASE_LIME_URI_STRING_WITH_PORT = "http://0.0.0.0:"+BASE_LIME_PORT+"/";
	private AtomicBoolean continueServer;
	
	public LimeHttpServer(){
		continueServer = new AtomicBoolean(true);
	}
	
	/**
	 * Set the flag to stop the server
	 */
	public void stopServer(){
		continueServer.set(false);
	}
	
	/**
	 * Create the http server
	 * 
	 * @return A reference to the created http server
	 * @throws URISyntaxException Thrown when the uri syntax is incorrect
	 */
	private static HttpServer createHttpServer() throws URISyntaxException {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		URL[] urls = ((URLClassLoader)cl).getURLs();
		ResourceConfig config = new ResourceConfig(edu.colorado.cs.ngn.lime.api.LimeAPI.class);
		URI baseUri = new URI(BASE_LIME_URI_STRING_WITH_PORT);
	    HttpServer server = org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
	    return server;
	}

	@Override
	public void run() {
		System.out.println("In http server run method");
		
		try {
			HttpServer server = createHttpServer();
			server.start();
			System.out.println("Starting embedded Jersey http server");
			while(continueServer.get()){
				Thread.sleep(1000);
			}
			server.shutdownNow();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (NoClassDefFoundError e){
			System.out.println("Couldn't find a class");
			e.printStackTrace();
			System.exit(-1);
		} catch (Exception e){
			System.out.println("Some other error occurred");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void main(String[] args){
		LimeHttpServer server =  new LimeHttpServer();
		Thread thread = new Thread(server);
		thread.start();
		try {
			System.out.println("Starting embedded Jersey http server");
			System.out.println("Press any key to end application....");
			System.in.read();
			server.stopServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
