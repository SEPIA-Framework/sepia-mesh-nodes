package net.b07z.sepia.server.mesh.endpoints;

import org.json.simple.JSONObject;
import net.b07z.sepia.server.core.server.BasicStatistics;
import net.b07z.sepia.server.core.server.RequestGetOrFormParameters;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.mesh.server.ConfigNode;
import spark.Request;
import spark.Response;

/**
 * Example endpoints.
 * 
 * @author Florian Quirin
 *
 */
public class ExampleEndpoints {
	
	//private static final Logger log = LoggerFactory.getLogger(ExampleEndpoints.class);
	
	/**
	 * ---HELLO WORLD GET---<br>
	 * Example GET endpoint that returns a JSON object.
	 */
	public static String helloWorld(Request request, Response response){
		//Save some server statistics
		BasicStatistics.addOtherApiHit("ep-hello-world");
		BasicStatistics.addOtherApiTime("ep-hello-world", 1);
		
		//Prepare parameters from form or URL
		RequestParameters params = new RequestGetOrFormParameters(request);
		String name = params.getString("name");
		
		//Generate response
		JSONObject msg = new JSONObject();
		
		if (Is.notNullOrEmpty(name)) {
			JSON.add(msg, "result", "success");
			JSON.add(msg, "hello", name);
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		
		}else {
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", "Missing parameter 'name'");
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 400);
		}
	}
	
	/**
	 * ---STATISTICS POST---<br>
	 * Example POST endpoint that returns a JSON object with server statistics.
	 */
	public static String serverStats(Request request, Response response){
		//Save some server statistics
		BasicStatistics.addOtherApiHit("ep-server-stats");
		BasicStatistics.addOtherApiTime("ep-server-stats", 1);
		
		//Prepare parameters from request body
		RequestParameters params = new RequestPostParameters(request);
		String username = params.getString("username");
		String password = params.getString("password");
		
		//Check parameters
		if (Is.nullOrEmpty(username) && Is.nullOrEmpty("password")) {
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", "Missing parameter 'username' or 'password'.");
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 400);
		}
		
		//Security check
		boolean isValid = (username.equals(ConfigNode.localName) && password.equals(ConfigNode.localSecret));
		
		//Generate response
		JSONObject msg = new JSONObject();
		if (isValid){
			JSON.add(msg, "result", "success");
			JSON.add(msg, "stats", BasicStatistics.getBasicInfo());
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		}else{
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", "Authentication failed! Use 'username: server_local_name, password: server_local_secret'.");
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 401);
		}
	}
}
