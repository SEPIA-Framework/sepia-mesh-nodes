package net.b07z.sepia.server.mesh.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.SandboxClassLoader;
import net.b07z.sepia.server.core.users.AuthenticationAssistAPI;
import net.b07z.sepia.server.mesh.endpoints.ExampleEndpoints;

/**
 * Read, write and store Mesh-Node server configuration.
 * 
 * @author Florian Quirin
 *
 */
public class ConfigNode {
	
	private static final Logger log = LoggerFactory.getLogger(ConfigNode.class);

	public static final String SERVERNAME = "SEPIA-Mesh-Node"; 		//public server name
	public static final String apiVersion = "v0.9.9";				//API version
	
	//Server settings (port, web-server, folders etc.)
	public static String settingsFolder = "Settings/";						//folder for settings
	public static String configFile = settingsFolder + "node.properties";	//external configuration file - note: this will be overwritten in "Setup" and "Start"
	public static String webServerFolder = "WebServer/";					//folder for web-server
	public static String pluginsFolder = "Plugins/";						//folder for plugins
	
	public static int serverPort = 20780;									//**server port
	public static boolean enableCORS = true;								//enable CORS (set access-control headers)
	public static boolean useSandboxPolicy = true;							//enable security policy to restrict e.g. access to 'System.exit()'
	public static boolean hostFiles = false;								//use web-server?
	public static String privacyPolicyLink = "http://localhost:20780/privacy-policy.html";		//link to privacy policy in case you host files
	
	public static String accessPin = "123456";						//**user defined access pin for non-critical access e.g. to statistics
	public static String localName = "sepia-mesh-node";						//**user defined local server name
	public static String localSecret = "123456";							//**user defined secret to validate local server
	public static String meshId = "MESHbyW3YLh8jTQPs5uzt2SzbmXZyphW";		//**one step of inter-API communication security - similar to clusterKey
	public static boolean allowInternalCalls = false;				//**allow API-to-API authentication via cluster-key
	public static boolean allowGlobalDevRequests = false;			//**restrict certain developer-specific requests to private network
	
	public static boolean usePlugins = false;						//use code loading on run-time
	public static boolean pluginsRequireAuthentication = false;		//does plugin execution require authentication?
	public static Role pluginsRequiredRole = Role.developer;		//required role to execute a plugin (only used when auth. required)
	public static boolean pluginsRequireLocalhost = false;			//only allow localhost plugin calls? (handy for client controls)
	public static boolean pluginsRequirePin = false;				//ask for PIN when using a plugin?
	
	//Modules and APIs to know
	public static String assistEndpointUrl = "http://localhost:20721/";		//SEPIA Assist-API endpoint URL (e.g. for authentication)
	public static String authenticationModule = AuthenticationAssistAPI.class.getCanonicalName();
	
	//----------- Sandbox Setup -------------
	
	//to be used with SandboxClassLoader
	
	private static List<String> blackList = new ArrayList<>();
	/**
	 * Setup sandbox for {@link SandboxClassLoader}.
	 */
	public static void setupSandbox(){
		blackList.add(ConfigNode.class.getPackage().getName()); 		//server.*
		blackList.add(ExampleEndpoints.class.getPackage().getName());	//endpoints.*
	}
	public static void addToSandboxBlackList(String classOrPackageName){
    	blackList.add(classOrPackageName);
    }
	public static List<String> getSandboxBlacklist(){
		return blackList;
	}
	
	//----------- Modules -----------
	
	public static void setupAuthModule(){
		if (authenticationModule != null) {
			//TODO: setup module if required
		}
	}
	
	//---------- helpers ----------
	
	/**
	 * Load server settings from properties file. 
	 */
	public static void loadSettings(String confFile){
		if (confFile == null || confFile.isEmpty())	confFile = configFile;
		
		try{
			Properties settings = FilesAndStreams.loadSettings(confFile);
			//server
			accessPin = settings.getProperty("server_access_pin");
			localName = settings.getProperty("server_local_name");
			localSecret = settings.getProperty("server_local_secret");
			serverPort = Integer.valueOf(settings.getProperty("server_port"));
			meshId = settings.getProperty("mesh_id");
			allowInternalCalls = Boolean.valueOf(settings.getProperty("allow_internal_calls"));
			allowGlobalDevRequests = Boolean.valueOf(settings.getProperty("allow_global_dev_requests"));
			enableCORS = Boolean.valueOf(settings.getProperty("enable_CORS"));
			
			//plugin stuff
			usePlugins = Boolean.valueOf(settings.getProperty("use_plugins"));
			pluginsRequireAuthentication = Boolean.valueOf(settings.getProperty("plugins_require_authentication"));
			String pluginsRequiredRoleString = settings.getProperty("plugins_required_user_role");
			if (Is.notNullOrEmpty(pluginsRequiredRoleString)){
				pluginsRequiredRole = Role.valueOf(pluginsRequiredRoleString);
			}
			pluginsRequireLocalhost = Boolean.valueOf(settings.getProperty("plugins_require_localhost", "false"));
			pluginsRequirePin = Boolean.valueOf(settings.getProperty("plugins_require_pin", "false"));
			
			//webserver
			hostFiles = Boolean.valueOf(settings.getProperty("host_files"));
			privacyPolicyLink = settings.getProperty("privacy_policy");
			
			//security and policies
			useSandboxPolicy = Boolean.valueOf(settings.getProperty("use_sandbox_security_policy"));
			
			//connectors and modules
			assistEndpointUrl = settings.getProperty("assist_endpoint_url");
			authenticationModule = settings.getProperty("module_authentication");
			
			log.info("loading settings from " + confFile + "... done.");
		
		}catch (Exception e){
			log.error("loading settings from " + confFile + "... failed!");
		}
	}
	/**
	 * Save server settings to file. Skip security relevant fields.
	 */
	public static void saveSettings(String confFile){
		if (confFile == null || confFile.isEmpty())	confFile = configFile;
		
		//save all personal parameters
		Properties settings = new Properties();
		//server
		settings.setProperty("server_access_pin", accessPin);
		settings.setProperty("server_local_name", localName);
		settings.setProperty("server_local_secret", localSecret);
		settings.setProperty("server_port", Integer.toString(serverPort));
		settings.setProperty("mesh_id", meshId);
		settings.setProperty("allow_internal_calls", Boolean.toString(allowInternalCalls));
		settings.setProperty("allow_global_dev_requests", Boolean.toString(allowGlobalDevRequests));
		settings.setProperty("enable_CORS", Boolean.toString(enableCORS));
		
		//plugins stuff
		settings.setProperty("use_plugins", Boolean.toString(usePlugins));
		settings.setProperty("plugins_require_authentication", Boolean.toString(pluginsRequireAuthentication));
		settings.setProperty("plugins_required_user_role", pluginsRequiredRole.name());
		settings.setProperty("plugins_require_localhost", Boolean.toString(pluginsRequireLocalhost));
		settings.setProperty("plugins_require_pin", Boolean.toString(pluginsRequirePin));
		
		//webserver
		settings.setProperty("host_files", Boolean.toString(hostFiles));
		settings.setProperty("privacy_policy", privacyPolicyLink);
		
		//security and policies
		settings.setProperty("use_sandbox_security_policy", Boolean.toString(useSandboxPolicy));
		
		//connectors and modules
		settings.setProperty("assist_endpoint_url", assistEndpointUrl);
		settings.setProperty("module_authentication", authenticationModule);
		
		
		try{
			FilesAndStreams.saveSettings(confFile, settings);
			log.info("saving settings to " + confFile + "... done.");
			
		}catch (Exception e){
			log.error("saving settings to " + confFile + "... failed!");
		}
	}
}
