package net.b07z.sepia.server.mesh.plugins;

import org.json.simple.JSONObject;

/**
 * Holds a result created via {@link Plugin} execute.
 * 
 * @author Florian Quirin
 *
 */
public class PluginResult {

	JSONObject resultJson;
	
	public PluginResult(JSONObject result){
		this.resultJson = result;
	}
	
	public JSONObject getJson(){
		return resultJson;
	}
}
