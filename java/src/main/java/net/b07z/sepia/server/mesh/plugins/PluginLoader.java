package net.b07z.sepia.server.mesh.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.SandboxClassLoader;
import net.b07z.sepia.server.core.tools.StringTools;
import net.b07z.sepia.server.mesh.server.ConfigNode;

/**
 * Class that loads plugins aka source-code and compiles it at runtime or startup.
 * 
 * @author Florian Quirin
 *
 */
public class PluginLoader {
	
	private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);
	
	public static String defaultSourceFolder = "src/";
	public static String defaultTargetFolder = "compiled/";
	private static SandboxClassLoader pluginClassLoader; 	//all plugins are stored in this class loader
	
	/**
	 * Get a plugin previously loaded (e.g. on start-up or via plugin endpoint).
	 * @param pluginClassName - canonical name of class the plugin was compiled from
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws MalformedURLException 
	 */
	public static Plugin getPlugin(String pluginClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException, MalformedURLException{
		if (pluginClassLoader == null){
			pluginClassLoader = new SandboxClassLoader(
					new File(ConfigNode.pluginsFolder + defaultTargetFolder), ConfigNode.getSandboxBlacklist());
		}
		Plugin plugin = (Plugin) pluginClassLoader.loadClass(pluginClassName).newInstance();
		return plugin;
	}
			
	/**
	 * Load all .java files from default source code folder, read code, compile and store to
	 * default target folder.  
	 * @return number of compiled plugins
	 */
	public static int loadAllPlugins(){
		int pluginsLoaded = -1;
		
		//Get plugins folder
		String sourceCodePath = ConfigNode.pluginsFolder + defaultSourceFolder;
		String compilePath = ConfigNode.pluginsFolder + defaultTargetFolder;
		List<File> files = FilesAndStreams.directoryToFileList(sourceCodePath, null, true);
		
		//Iterate all files and take only source code java files
		for (File f : files){
			if (compileJavaPluginToTarget(f, compilePath)){
				pluginsLoaded++;
			}
		}	
		return pluginsLoaded;
	}
	
	/**
	 * Compile a .java file to target path or throw error.
	 * @param f - File to compile
	 * @param compilePath - Target folder to store resulting .class file
	 * @return
	 */
	public static boolean compileJavaPluginToTarget(File f, String compilePath){
		String fileName = f.getName();
		String fullPathAndName = f.getPath();
		if (fileName.endsWith(".java")){
			log.info("Loading plugin from: " + fullPathAndName);
        	//Get source code
        	String sourceCode;
            try (InputStream input = new FileInputStream(f)){
        		sourceCode = FilesAndStreams.getStringFromStream(input, StandardCharsets.UTF_8, "\n");
        		//Compile file to target folder
            	String classSimpleName = ClassBuilder.getSimpleClassNameFromFileName(fileName);
            	return compileSourceCodeToTarget(classSimpleName, sourceCode, compilePath);
        		
        	}catch (Exception e){
        		log.error("Plugin ERROR - Loading FAILED with msg: " + e.getMessage());
			}
		}
		return false;
	}
	
	/**
	 * Compile class from source code to target path or throw error.
	 * @param classSimpleName - simple name of class
	 * @param sourceCode - source code as string
	 * @param compilePath - target path of compiled .class
	 * @return
	 */
	public static boolean compileSourceCodeToTarget(String classSimpleName, String sourceCode, String compilePath){
    	String packageName = StringTools.findFirstRexEx(sourceCode, "^(\\s+|)(package )(.*?);")
    			.replaceFirst(".*package ", "").replaceFirst(";$", "").trim();
    	if (packageName.isEmpty()){
    		throw new RuntimeException("Package name of class missing in source code or invalid.");
    	}
		String errors = ClassBuilder.compile(packageName + "." + classSimpleName, sourceCode, new File(compilePath));
		if (!errors.isEmpty()){
			throw new RuntimeException("Class '" + classSimpleName + "' - " + errors);
		}
		log.info("Plugin compiled successfully and stored in: " + compilePath);
		return true;
	}
}
