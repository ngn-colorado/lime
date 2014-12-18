package org.flowvisor;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.ProcessingException;
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
		HttpServer httpServer;
		try {
			httpServer = createHttpServer();
			//not needed when using JdkHttpServer
//			httpServer.start();
			System.out.println("Started embedded Jersey server");
		} catch (ProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static HttpServer createHttpServer() throws ProcessingException, URISyntaxException {
		ResourceConfig limeResourceConfig = new ResourceConfig(LimeWebApplication.class);
		URI baseUri = UriBuilder.fromUri(BASE_LIME_URI_STRING).port(BASE_LIME_PORT).build();
//		return JdkHttpServerFactory.createHttpServer(baseUri, limeResourceConfig);
//		HttpServer server = HttpServer.createSimpleServer(BASE_LIME_URI_STRING);
		return GrizzlyHttpServerFactory.createHttpServer(baseUri);//, limeResourceConfig);
//		NetworkListener netListener = new NetworkListener("jaxws-listener", "0.0.0.0", BASE_LIME_PORT);
		
	}
	

}
