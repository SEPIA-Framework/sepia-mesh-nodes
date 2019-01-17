package net.b07z.sepia.server.mesh.endpoints;

import javax.servlet.MultipartConfigElement;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.server.core.server.BasicStatistics;
import net.b07z.sepia.server.core.server.RequestGetOrFormParameters;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
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
	
	public static final String UPLOAD_FILE_KEY = "upload_file";
	public static final String UPLOAD_CODE_KEY = "upload_code";
	public static final String UPLOAD_CODE_CLASS_NAME = "upload_code_class_name"; 	//simple class name

	/**
	 * --- EXECUTE PLUGIN POST ---<br>
	 * Execute a plugin and return result as JSON.
	 * @param request - Spark {@link Request}
	 * @param response - Spark {@link Response}
	 */
	public static String executePlugin(Request request, Response response){
		//Save some server statistics (A)
		long tic = System.currentTimeMillis();
		
		//Plugins allowed?
		if (!ConfigNode.usePlugins){
			return pluginsDeactivatedResponse(request, response);
		}
		
		//Prepare parameters from request body
		RequestParameters params = new RequestPostParameters(request);
		
		//Authenticate
		String userId = "anonymous";
		boolean isAllowed = true;
		if (ConfigNode.pluginsRequireAuthentication){
			//test account
			Account account = AuthEndpoints.authenticate(params, request, response);
			isAllowed = isAllowed(account);
			if (isAllowed){
				userId = account.getUserID();
			}else{
  				return notAllowedResponse(request, response);
  			}
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
	
	/**-- UPLOAD PLUGIN POST --<br>
	 * End-point to send plugin code to.  
	 * @param request - Spark {@link Request}
	 * @param response - Spark {@link Response}
	 */
	public static String uploadPlugin(Request request, Response response){
		//Save some server statistics (A)
		long tic = System.currentTimeMillis();
		
		//Plugins allowed?
		if (!ConfigNode.usePlugins){
			return pluginsDeactivatedResponse(request, response);
		}
		
		RequestParameters params = new RequestGetOrFormParameters(request);
		//?? - required to read parameters properly:
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
	    
	    //this endpoint requires a certain content type
	    String contentType = request.headers("Content-type");
	    if (!contentType.toLowerCase().contains("multipart/form-data")){
	    	JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "Plugins upload endpoint requires content-type 'multipart/form-data' but saw: " + contentType);
			return SparkJavaFw.returnResult(request, response, result.toJSONString(), 400);
	    }
		
	    //Authenticate
  		String userId = "anonymous";
  		boolean isAllowed = true;
  		if (ConfigNode.pluginsRequireAuthentication){
  			//test account
  			Account account = AuthEndpoints.authenticate(params, request, response);
  			isAllowed = isAllowed(account);
  			if (isAllowed){
  				userId = account.getUserID();
  			}else{
  				return notAllowedResponse(request, response);
  			}
  		}
	    
		try{
			//Get source code and class name
			String sourceCode = params.getString(UPLOAD_CODE_KEY);
			String sourceCodeClassName = params.getString(UPLOAD_CODE_CLASS_NAME);
			if (Is.nullOrEmpty(sourceCode) || Is.nullOrEmpty(sourceCodeClassName)){
				JSONObject result = new JSONObject();
				JSON.add(result, "result", "fail");
				JSON.add(result, "error", "Plugins upload endpoint requires parameters '" + UPLOAD_CODE_KEY 
						+ "' and '" + UPLOAD_CODE_CLASS_NAME + "'.");
				return SparkJavaFw.returnResult(request, response, result.toJSONString(), 400);
			}
			//compile source code and store class file(s)
			boolean compiledAndStored = PluginLoader.compileAndStoreSourceCode(sourceCodeClassName, sourceCode);
			
        	//reset class loader and reload all services
			boolean cleanedTargetFolder = PluginLoader.cleanUpPluginsFolder();
			int loadedPlugins = PluginLoader.loadAllPlugins(false);
        	
        	//stats
			BasicStatistics.addOtherApiHit("upload-plugin");
			BasicStatistics.addOtherApiTime("upload-plugin", tic);
          	
			//Generate response
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "success");
			JSON.add(msg, "user", userId);
			JSON.add(msg, "compiled", compiledAndStored);
			JSON.add(msg, "plugins_reloaded", cleanedTargetFolder);
			JSON.add(msg, "plugins_active", loadedPlugins);
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		
		}catch(Exception e){
			log.error("upload-plugin - " + e.getMessage());
	      	Debugger.printStackTrace(e, 3);
	      	
			//stats
			BasicStatistics.addOtherApiHit("upload-plugin-error");
			BasicStatistics.addOtherApiTime("upload-plugin-error", tic);
	      	
			JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "500 - Internal plugin error: " + e.getMessage());
			return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
		}
	}
	
	/**
	 * --- DELETE PLUGIN POST ---<br>
	 * Delete a plugin from source folder and reload all remaining.
	 * @param request - Spark {@link Request}
	 * @param response - Spark {@link Response}
	 */
	public static String deletePlugin(Request request, Response response){
		//Save some server statistics (A)
		long tic = System.currentTimeMillis();
		
		//Plugins allowed?
		if (!ConfigNode.usePlugins){
			return pluginsDeactivatedResponse(request, response);
		}
		
		//Prepare parameters from request body
		RequestParameters params = new RequestPostParameters(request);
		
		//Authenticate
		String userId = "anonymous";
		boolean isAllowed = true;
		if (ConfigNode.pluginsRequireAuthentication){
			//test account
			Account account = AuthEndpoints.authenticate(params, request, response);
			isAllowed = isAllowed(account);
			if (isAllowed){
				userId = account.getUserID();
			}else{
  				return notAllowedResponse(request, response);
  			}
		}
		try{
			//delete file(s)
			String classSimpleName = params.getString("simpleName");
			int deletedFiles = 0;
			if (Is.notNullOrEmpty(classSimpleName)){
				deletedFiles = PluginLoader.deletePluginSourceFile(classSimpleName);
			}
			
			//reset class loader and reload all services
			boolean cleanedTargetFolder = PluginLoader.cleanUpPluginsFolder();
			int loadedPlugins = PluginLoader.loadAllPlugins(false);
        	
        	//stats
			BasicStatistics.addOtherApiHit("delete-plugin");
			BasicStatistics.addOtherApiTime("delete-plugin", tic);
			
			//Generate response
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "success");
			JSON.add(msg, "user", userId);
			JSON.add(msg, "plugins_deleted", deletedFiles);
			JSON.add(msg, "plugins_reloaded", cleanedTargetFolder);
			JSON.add(msg, "plugins_active", loadedPlugins);
			if (deletedFiles == 0){
				JSON.add(msg, "note", "0 files deleted due to missing 'simpleName' parameter or no file found.");
			}
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			
		}catch(Exception e){
			log.error("delete-plugin - " + e.getMessage());
	      	Debugger.printStackTrace(e, 3);
	      	
			//stats
			BasicStatistics.addOtherApiHit("delete-plugin-error");
			BasicStatistics.addOtherApiTime("delete-plugin-error", tic);
	      	
			JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "500 - Internal plugin error: " + e.getMessage());
			return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
		}
	}
	
	//--------------------------------------
	
	private static String pluginsDeactivatedResponse(Request request, Response response){
		JSONObject msg = new JSONObject();
		JSON.add(msg, "result", "fail");
		JSON.add(msg, "error", "Plugins are deactivated! Check settings file.");
		return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 400);
	}
	
	private static boolean isAllowed(Account account){
		boolean isAllowed = account.getAccessLevel() >= 0; 		//-1 would be 'fail'
		//check user role
		if (isAllowed && ConfigNode.pluginsRequiredRole != null){
			isAllowed = account.hasRole(ConfigNode.pluginsRequiredRole.name());
		}
		return isAllowed;
	}
	private static String notAllowedResponse(Request request, Response response){
		JSONObject msg = new JSONObject();
		JSON.add(msg, "result", "fail");
		JSON.add(msg, "error", "401 - Authentication failed or user is missing role: " + ConfigNode.pluginsRequiredRole);
		return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 401);
	}
}
