package net.b07z.sepia.server.mesh.plugins;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.RuntimeInterface;
import net.b07z.sepia.server.core.tools.RuntimeInterface.RuntimeResult;

/**
 * A plugin that can call system runtime (if sandbox settings and security policy allows it).
 * 
 * @author Florian Quirin
 */
public class RuntimePlugin implements Plugin {

	@Override
	public PluginResult execute(JSONObject data) {
		//Get runtime command
		JSONArray cmd = JSON.getJArray(data, "command");
		long timeout = JSON.getLongOrDefault(data, "timeout", 5000);
		
		if (Is.notNullOrEmpty(cmd)){
			boolean restrictCode = false;	//NOTE: change?
			RuntimeResult cmdResult = RuntimeInterface.runCommand(Converters.jsonArrayToStringList(cmd), timeout, restrictCode);
			
			//Command finished without errors
			if (cmdResult.getStatusCode() == 0){
				PluginResult result = new PluginResult(JSON.make(
						"status", "success",
						"command", cmd.toString(),
						"data", cmdResult.getOutput()
				));
				return result;
			
			//Command had errors
			}else{
				PluginResult result = new PluginResult(JSON.make(
						"status", "fail",
						"command", cmd.toString(),
						"error", cmdResult.getException(),
						"code", cmdResult.getStatusCode(),
						"data", cmdResult.getOutput()
				));
				return result;
			}
			
		//No command
		}else{
			PluginResult result = new PluginResult(JSON.make(
					"status", "fail",
					"error", "'command' missing"
			));
			return result;
		}
	}

}
