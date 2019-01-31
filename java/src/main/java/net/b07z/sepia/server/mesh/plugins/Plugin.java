package net.b07z.sepia.server.mesh.plugins;

import org.json.simple.JSONObject;

public interface Plugin {

	/**
	 * Execute a plugin using the data given as JSON.
	 * @param data - JSONObject with any data you need for the plugin
	 * @return {@link PluginResult}
	 */
	public PluginResult execute(JSONObject data);
}
