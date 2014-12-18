package org.flowvisor;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;


//import com.sun.net.httpserver.HttpServer;

public class LimeHttpServer {
	public static final String BASE_LIME_URI_STRING = "http://localhost/";
	public static final int BASE_LIME_PORT = 9000;
	public static final String BASE_LIME_URI_STRING_WITH_PORT = "http://localhost:"+BASE_LIME_PORT+"/";
	
	public static void main(String[] args){
		System.out.println("Starting embedded Jersey http server");
		HttpServer server = createHttpServer();
		try {
			server.start();
			System.out.println("Press any key to end application....");
			System.in.read();
			server.shutdownNow();
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
	

}
