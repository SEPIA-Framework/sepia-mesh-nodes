package net.b07z.sepia.server.mesh.server;

import static spark.Spark.*;

import net.b07z.sepia.server.core.endpoints.CoreEndpoints;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.mesh.endpoints.ExampleEndpoints;
import net.b07z.sepia.server.mesh.endpoints.PluginEndpoints;

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
		
		//post("/authentication", (request, response) -> 	AuthEndpoints.defaultAuthentication(request, response));
		
		post("/execute-plugin", (request, response) -> 	PluginEndpoints.executePlugin(request, response));
		post("/upload-plugin", (request, response) -> 	PluginEndpoints.uploadPlugin(request, response));
		post("/delete-plugin", (request, response) -> 	PluginEndpoints.deletePlugin(request, response));
		
		//MODIFY THIS AS YOU PLEASE AND ADD YOUR OWN ENDPOINTS :-)
	}

	//Run server
	public static void main(String[] args) {
		//Deactivate SEPIA debugger info messages (we don't need them here usually)
		Debugger.info = false;
		Debugger.log = false;
		
		//Initialize
		MeshNodeInterface node = new StartNode();
		
		//Run
		node.start(args);
	}
}
