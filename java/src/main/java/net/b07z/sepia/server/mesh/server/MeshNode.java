package net.b07z.sepia.server.mesh.server;

import static spark.Spark.*;

import java.security.Policy;
import java.util.Date;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.server.core.endpoints.CoreEndpoints;
import net.b07z.sepia.server.core.server.ConfigDefaults;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.SandboxSecurityPolicy;
import net.b07z.sepia.server.mesh.endpoints.AuthEndpoints;
import net.b07z.sepia.server.mesh.endpoints.ExampleEndpoints;

/**
 * Default Mesh-Node server with handling of configuration and server setup. 
 * Exposes a set of basic 'Hello World' endpoints.<br> 
 * Extend this class and override {@link #loadEndpoints()} to build your own node.
 * 
 * @author Florian Quirin
 *
 */
public class MeshNode implements MeshNodeInterface {
	
	private static final Logger log = LoggerFactory.getLogger(MeshNode.class);
	private static String startGMT = "";
	
	public static final String LIVE_SERVER = "live";
	public static final String TEST_SERVER = "test";
	public static final String CUSTOM_SERVER = "custom";
	public static String serverType = "";
	
	public static boolean isSSL = false;
	private static String keystorePwd = "13371337";
	
	//---------------------------------------------------
	
	@Override
	public void loadEndpoints(){
		//Some default server endpoints:
		
		get("/ping", (request, response) -> 			CoreEndpoints.ping(request, response, ConfigNode.SERVERNAME));
		get("/validate", (request, response) -> 		CoreEndpoints.validateServer(request, response,	ConfigNode.SERVERNAME, 
															ConfigNode.apiVersion, ConfigNode.localName, ConfigNode.localSecret));
		
		get("/hello-world", (request, response) -> 		ExampleEndpoints.helloWorld(request, response));
		post("/server-stats", (request, response) -> 	ExampleEndpoints.serverStats(request, response));
		
		post("/authentication", (request, response) -> 	AuthEndpoints.defaultAuthentication(request, response));
	}

	@Override
	public void start(String[] args) {
		//load settings
		loadSettings(args);
		
		//load statics and setup modules (loading stuff to memory etc.)
		setupModules();
				
		//setup server with port, CORS and error handling etc. 
		setupServer();
		
		//SERVER END-POINTS
		loadEndpoints();
	}
	
	//---------------------------------------------------
	
	/**
	 * Check arguments and load settings correspondingly.
	 * @param args - parameters submitted to main method
	 */
	public void loadSettings(String[] args){
		//check arguments
		serverType = TEST_SERVER;
		for (String arg : args){
			if (arg.equals("--test")){
				//Test system
				serverType = TEST_SERVER;
			}else if (arg.equals("--live")){
				//Live system
				serverType = LIVE_SERVER;
			}else if (arg.equals("--my") || arg.equals("--custom")){
				//Custom system
				serverType = CUSTOM_SERVER;
			}else if (arg.equals("--ssl")){
				//SSL
				isSSL = true;
			}else if (arg.startsWith("keystorePwd=")){
				//Java key-store password - TODO: maybe not the best way to load the pwd ...
				keystorePwd = arg.replaceFirst(".*?=", "").trim();
			}
		}
		
		//load configuration
		loadConfigFile(serverType);
		log.info("--- Running " + ConfigNode.SERVERNAME + " with " + serverType.toUpperCase() + " settings ---");
		
		//set security
		if (ConfigNode.useSandboxPolicy) {
			Policy.setPolicy(new SandboxSecurityPolicy());
			System.setSecurityManager(new SecurityManager());
			ConfigNode.setupSandbox();
		}
		if (isSSL){
			secure(ConfigNode.settingsFolder + "ssl-keystore.jks", keystorePwd, null, null);
		}
				
		//host files?
		if (ConfigNode.hostFiles){
			staticFiles.externalLocation(ConfigNode.webServerFolder);
			log.info("Web-server is active and uses folder: " + ConfigNode.webServerFolder);
		}
		
		//SETUP CORE-TOOLS
		JSONObject coreToolsConfig = JSON.make(
				"defaultAssistAPI", ConfigNode.assistEndpointUrl,
				"defaultAuthModule", ConfigNode.authenticationModule,
				"privacyPolicy", ConfigNode.privacyPolicyLink,
				"clusterKey", ConfigNode.clusterKey
		);
		JSON.put(coreToolsConfig, "defaultTeachAPI", "---"); 		//not required here but we need to set it
		JSON.put(coreToolsConfig, "defaultAssistantUserId", "---");	//not required here but we need to set it
		//part 2 (optional)
		/*
		long clusterTic = Timer.tic();
		JSONObject assistApiClusterData = ConfigDefaults.getAssistantClusterData();
		if (assistApiClusterData == null){
			throw new RuntimeException("Core-tools are NOT set properly! AssistAPI could not be reached!");
		}else{
			log.info("Received cluster-data from AssistAPI after " + Timer.toc(clusterTic) + "ms");
		}
		*/
		ConfigDefaults.setupCoreTools(coreToolsConfig);
		//Check core-tools settings
		if (!ConfigDefaults.areCoreToolsSet()){
			throw new RuntimeException("Core-tools are NOT set properly!");
		}
	}
	
	/**
	 * Load configuration file.
	 * @param serverType - live, test, custom
	 */
	public void loadConfigFile(String serverType){
		if (serverType.equals(TEST_SERVER)){
			ConfigNode.configFile = ConfigNode.settingsFolder + "node.test.properties";
		}else if (serverType.equals(CUSTOM_SERVER)){
			ConfigNode.configFile = ConfigNode.settingsFolder + "node.custom.properties";
		}else if (serverType.equals(LIVE_SERVER)){
			ConfigNode.configFile = ConfigNode.settingsFolder + "node.properties";
		}else{
			throw new RuntimeException("INVALID SERVER TYPE: " + serverType);
		}
		ConfigNode.loadSettings(ConfigNode.configFile);
	}
	
	/**
	 * Setup server with port, CORS (cross-origin access), error-handling etc.. 
	 */
	public void setupServer(){
		//start by getting GMT date
		Date date = new Date();
		startGMT = DateTime.getGMT(date, "dd.MM.yyyy' - 'HH:mm:ss' - GMT'");
		log.info("Starting " + ConfigNode.SERVERNAME + " " + ConfigNode.apiVersion + " (" + serverType + ")");
		log.info("date: " + startGMT);
				
		/*
		//TODO: do we need to set this? https://wiki.eclipse.org/Jetty/Howto/High_Load
		int maxThreads = 8;
		int minThreads = 2;
		int timeOutMillis = 30000;
		threadPool(maxThreads, minThreads, timeOutMillis)
		*/
		
		try {
			port(Integer.valueOf(System.getenv("PORT")));
			log.info("server running on port: " + Integer.valueOf(System.getenv("PORT")));
		}catch (Exception e){
			int port = ConfigNode.serverPort; 	//default is 20721
			port(port);
			log.info("server running on port "+ port);
		}
		
		//set access-control headers to enable CORS
		if (ConfigNode.enableCORS){
			SparkJavaFw.enableCORS("*", "*", "*");
		}

		//do something before end-point evaluation - e.g. authentication
		before((request, response) -> {
			//System.out.println("BEFORE TEST 1"); 		//DEBUG
		});
		
		//ERROR handling - TODO: improve
		SparkJavaFw.handleError();
	}
	
	/**
	 * All kinds of things that should be loaded on startup.
	 */
	public void setupModules(){
		ConfigNode.setupAuthModule();	//Authentication module
	}

}
