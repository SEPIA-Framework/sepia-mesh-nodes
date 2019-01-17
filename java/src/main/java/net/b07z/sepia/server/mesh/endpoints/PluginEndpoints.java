package net.b07z.sepia.server.mesh.endpoints;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.server.core.server.BasicStatistics;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.users.Account;
import net.b07z.sepia.server.mesh.plugins.Plugin;
import net.b07z.sepia.server.mesh.plugins.PluginLoader;
import net.b07z.sepia.server.mesh.plugins.PluginResult;
import net.b07z.sepia.server.mesh.server.ConfigNode;
import spark.Request;
import spark.Response;

/**
 * Load new plugins and delete old ones and execute a plugin.
 * 
 * @author Florian Quirin
 *
 */
public class PluginEndpoints {
	
private static final Logger log = LoggerFactory.getLogger(PluginEndpoints.class);
	
	/**
	 * ---EXECUTE PLUGIN POST---<br>
	 * Execute a plugin and return result as JSON.
	 * @param request - Spark {@link Request}
	 * @param response - Spark {@link Response}
	 */
	public static String executePlugin(Request request, Response response){
		//Save some server statistics (A)
		long tic = System.currentTimeMillis();
		
		//Plugins allowed?
		if (!ConfigNode.usePlugins){
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", "Plugins are deactivated! Check settings file.");
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 400);
		}
		
		//Prepare parameters from request body
		RequestParameters params = new RequestPostParameters(request);
		
		//Authenticate
		String userId = "anonymous";
		boolean isValid = false;
		if (ConfigNode.pluginsRequireAuthentication){
			//get account
			Account account = AuthEndpoints.authenticate(params, request, response);
			isValid = account.getAccessLevel() >= 0; 		//-1 would be 'fail'
			if (isValid){
				userId = account.getUserID();
			}
			
			//check user role
			if (isValid && ConfigNode.pluginsRequiredRole != null){
				isValid = account.hasRole(ConfigNode.pluginsRequiredRole.name());
			}
		}else{
			isValid = true;
		}
		if (!isValid){
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", "401 - Authentication failed or user is missing role: " + ConfigNode.pluginsRequiredRole);
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 401);
		}
		
		//Now all is good ... run the plugin:
		
		//What plugin?
		try{
			String pluginCanonicalName = params.getString("canonicalName");
			JSONObject pluginData = params.getJson("data");
			
			Plugin plugin = PluginLoader.getPlugin(pluginCanonicalName);
			PluginResult pluginResult = plugin.execute(pluginData);
			
			//Save some server statistics (B1)
			BasicStatistics.addOtherApiHit("ep-execute-plugin");
			BasicStatistics.addOtherApiTime("ep-execute-plugin", tic);
			
			log.info("Plugin success. User '" + userId + "' called: " + pluginCanonicalName);
			
			//Generate response
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "success");
			JSON.add(msg, "user", userId);
			JSON.add(msg, "plugin", pluginCanonicalName);
			JSON.add(msg, "data", pluginResult.getJson());
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			
		//Plugin error
		}catch (Exception e){
			log.error("Plugin error! User '" + userId + "' created exception: " + e.getMessage());
			Debugger.printStackTrace(e, 3);
			
			//Save some server statistics (B2)
			BasicStatistics.addOtherApiHit("ep-execute-plugin-error");
			BasicStatistics.addOtherApiTime("ep-execute-plugin-error", tic);
			
			//Generate response
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", "500 - Internal plugin error! Please check your request body for valid 'canonicalName' and 'data'");
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 500);
		}
	}
}
