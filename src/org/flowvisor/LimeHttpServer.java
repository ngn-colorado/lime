package org.flowvisor;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;


//import com.sun.net.httpserver.HttpServer;

public class LimeHttpServer implements Runnable{
	public static final String BASE_LIME_URI_STRING = "http://0.0.0.0/";
	public static final int BASE_LIME_PORT = 9000;
	public static final String BASE_LIME_URI_STRING_WITH_PORT = "http://localhost:"+BASE_LIME_PORT+"/";
	private AtomicBoolean continueServer;
	
	public LimeHttpServer(){
		continueServer = new AtomicBoolean(true);
	}
	
	public void stopServer(){
		continueServer.set(false);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static HttpServer createHttpServer() {
//		HttpServer server = new HttpServer();
//		NetworkListener netListener = new NetworkListener("jaxws-listener", "0.0.0.0", BASE_LIME_PORT);
		ResourceConfig config = new ResourceConfig(LimeAPI.class);
		URI baseUri = UriBuilder.fromUri(BASE_LIME_URI_STRING).port(BASE_LIME_PORT).build();
	    HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
//		HttpHandler httpHandler = new JaxwsHandler(new LimeAPI());
	    return server;
	}

	@Override
	public void run() {
		HttpServer server = createHttpServer();
		try {
			server.start();
			while(continueServer.get()){
				Thread.sleep(1000);
				System.out.println("Server running on port: "+BASE_LIME_PORT);
			}
			server.shutdownNow();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
