package net.b07z.sepia.server.mesh.endpoints;

import static spark.Spark.*;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.server.core.server.BasicStatistics;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.server.Validate;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.users.Account;
import net.b07z.sepia.server.mesh.server.ConfigNode;
import spark.Request;
import spark.Response;

/**
 * Endpoints used for authentication. Usually this is done via redirect to SEPIA Assist-API
 * but also support custom modules via modification of {@link ConfigNode}.authenticationModule.
 * 
 * @author Florian Quirin
 *
 */
public class AuthEndpoints {
	
	private static final Logger log = LoggerFactory.getLogger(AuthEndpoints.class);
	
	/**
	 * ---AUTHENTICATE POST---<br>
	 * Authenticate via SEPIA Assist-API or custom {@link ConfigNode}.authenticationModule.
	 * @param request - Spark {@link Request}
	 * @param response - Spark {@link Response}
	 */
	public static String defaultAuthentication(Request request, Response response){
		//Save some server statistics (A)
		long tic = System.currentTimeMillis();
		
		//Prepare parameters from request body
		RequestParameters params = new RequestPostParameters(request);
		
		//Authenticate and get account data
		Account account = authenticate(params, request, response);
		boolean isValid = account.getAccessLevel() >= 0; 		//-1 would be 'fail'
		
		//Generate response
		JSONObject msg = new JSONObject();
		if (isValid){
			//Save some server statistics (B1)
			BasicStatistics.addOtherApiHit("ep-authentication");
			BasicStatistics.addOtherApiTime("ep-authentication", tic);
			
			JSON.add(msg, "result", "success");
			JSON.add(msg, "data", account.exportJSON());
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		
		}else{
			//Save some server statistics (B2)
			BasicStatistics.addOtherApiHit("ep-authentication-error");
			BasicStatistics.addOtherApiTime("ep-authentication-error", tic);
			
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", "401 - Authentication failed!");
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 401);
		}
	}
	
	
	//-------- Authentication --------
	
	/**
	 * Default authentication method via SEPIA Assist-API. 
	 * @param params - request body, e.g. via {@link RequestPostParameters}
	 * @param request - Spark {@link Request}
	 * @param response - Spark {@link Response}
	 * @return {@link Account}
	 */
	public static Account authenticate(RequestParameters params, Request request, Response response){
		//Save some server statistics (A)
		long tic = System.currentTimeMillis();

		//Class that holds the data AND does authentication against Assist-API (via 'ConfigDefaults.defaultAuthModule')
		Account userAccount = new Account();
		
		//check for intra-API call that does not require authentication again
		boolean isInternalCall = ConfigNode.allowInternalCalls && 
				Validate.validateInternalCall(request, params.getString("sKey"), ConfigNode.clusterKey); 
		if (isInternalCall){
			//NOTE:
			
			//- This should not be sent in GET calls and maybe only with SSL!
			//- Consider adding a white-list of endpoints that allow internal calls.
			//- It is a potential risk if someone hacks the secure key and uses any user ID he wants :-(
			//- ... so USE CAREFULLY and maybe just leave 'allowInternalCalls=false'
			
			//user data must be submitted by request in this case
			String accountS = params.getString("userData");
			JSONObject accountJS = JSON.parseString(accountS);
			if (accountJS == null){
				log.warn("Invalid internal API call from " + request.ip());
				halt(SparkJavaFw.returnResult(request, response, "{\"result\":\"fail\",\"error\":\"401 not authorized - invalid userData\"}", 401));
			}else{
				//log.info("Successful internal API call from " + request.ip());
				userAccount.importJSON(accountJS);
			}
			//Save some server statistics (B1)
			BasicStatistics.addOtherApiHit("auth-request-internal");
			BasicStatistics.addOtherApiTime("auth-request-internal", tic);

		//else do database authentication
		}else if (!userAccount.authenticate(params)){
			//Save some server statistics (B2)
			BasicStatistics.addOtherApiHit("auth-request-error");
			BasicStatistics.addOtherApiTime("auth-request-error", tic);
			haltWithAuthError(request, response);
		
		}else{
			//Save some server statistics (B3)
			BasicStatistics.addOtherApiHit("auth-request");
			BasicStatistics.addOtherApiTime("auth-request", tic);
			
		}
		return userAccount;
	}
	private static void haltWithAuthError(Request request, Response response) {
		halt(SparkJavaFw.returnResult(request, response, JSON.make(
						"result", "fail", 
						"error", "401 not authorized (or connection issue)"
		).toJSONString(), 401));
	}
}
