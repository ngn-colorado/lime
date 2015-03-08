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


//import com.sun.net.httpserver.HttpServer;

public class LimeHttpServer implements Runnable{
	public static final String BASE_LIME_URI_STRING = "http://0.0.0.0/";
	public static final int BASE_LIME_PORT = 9000;
	public static final String BASE_LIME_URI_STRING_WITH_PORT = "http://0.0.0.0:"+BASE_LIME_PORT+"/";
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

	private static HttpServer createHttpServer() throws URISyntaxException {
		//print system loader classpath
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		
		URL[] urls = ((URLClassLoader)cl).getURLs();
		
//		for(URL url : urls){
//			System.out.println(url.getFile());
//		}
		
//		HttpServer server = new HttpServer();
//		NetworkListener netListener = new NetworkListener("jaxws-listener", "0.0.0.0", BASE_LIME_PORT);
		System.out.println("in create server method");
		ResourceConfig config = new ResourceConfig(edu.colorado.cs.ngn.lime.api.LimeAPI.class);
		System.out.println("in create server method 2");
//		URI baseUri = UriBuilder.fromUri(BASE_LIME_URI_STRING).port(BASE_LIME_PORT).build();
		URI baseUri = new URI(BASE_LIME_URI_STRING_WITH_PORT);
		System.out.println("in create server method 3");
	    HttpServer server = org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
	    System.out.println("in create server method 4");
//		HttpHandler httpHandler = new JaxwsHandler(new LimeAPI());
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
//				System.out.println("Server running on port: "+BASE_LIME_PORT);
			}
			server.shutdownNow();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
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
	

}
