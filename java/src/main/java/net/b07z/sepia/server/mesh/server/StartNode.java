package net.b07z.sepia.server.mesh.server;

import static spark.Spark.*;

import net.b07z.sepia.server.core.endpoints.CoreEndpoints;
import net.b07z.sepia.server.mesh.endpoints.AuthEndpoints;
import net.b07z.sepia.server.mesh.endpoints.ExampleEndpoints;

/**
 * Build your own Mesh-Node server by modifying this class.
 */
public class StartNode extends MeshNode {
	
	@Override
	/**
	 * Defines the end-points that this server supports.
	 */
	public void loadEndpoints(){
		//Server endpoints:
		
		get("/ping", (request, response) -> 			CoreEndpoints.ping(request, response, ConfigNode.SERVERNAME));
		get("/validate", (request, response) -> 		CoreEndpoints.validateServer(request, response,	ConfigNode.SERVERNAME, 
															ConfigNode.apiVersion, ConfigNode.localName, ConfigNode.localSecret));
		
		get("/hello-world", (request, response) -> 		ExampleEndpoints.helloWorld(request, response));
		post("/server-stats", (request, response) -> 	ExampleEndpoints.serverStats(request, response));
		
		post("/authentication", (request, response) -> 	AuthEndpoints.defaultAuthentication(request, response));
		
		//MODIFY THIS AS YOU PLEASE AND ADD YOUR OWN ENDPOINTS :-)
	}

	//Run server
	public static void main(String[] args) {
		//Initialize
		MeshNodeInterface node = new StartNode();
		
		//Run
		node.start(args);
	}
}
