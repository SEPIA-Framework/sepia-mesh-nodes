package net.b07z.sepia.server.mesh.plugins;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.JSON;

/**
 * The simplest plugin possible. Uses the 'name' field from the request data and returns hello 'name'.
 * 
 * @author Florian Quirin
 */
public class HelloPlugin implements Plugin {

	@Override
	public PluginResult execute(JSONObject data) {
		//Get name and return hello
		String name = JSON.getString(data, "name");
		PluginResult result = new PluginResult(JSON.make(
				"status", "success",
				"hello", name
		));
		return result;
	}

}
