package net.b07z.sepia.server.mesh.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.Is;
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
	 * Remove all cached classes from plugin class-loader.
	 */
	public static void resetClassLoader(){
		pluginClassLoader = null;
		log.info("Plugin class-loader has been reset.");
	}
	/**
	 * Clean-up plugins folder by removing all compiled classes and reset class-loader afterwards.
	 * Usually this would be followed by a reload of classes with e.g.: {@link #loadAllPlugins()}.
	 * @return true or throw file exceptions
	 */
	public static boolean cleanUpPluginsFolder(){
		String compilePath = ConfigNode.pluginsFolder + defaultTargetFolder;
		List<File> foldersInTargetDir = FilesAndStreams.getDirectoriesAtPath(compilePath, null);
		if (Is.notNullOrEmpty(foldersInTargetDir)){
			for (File folder : foldersInTargetDir){
				log.info("Cleaning folder '" + folder.getName() + "' from plugin target directory.");
				//folder.delete(); 		//NOTE: can throw error
				FilesAndStreams.deleteFolder(folder);
			}
		}
		//Don't forget to reset the class-loader:
		resetClassLoader();
		return true;
	}
			
	/**
	 * Load all .java files from default source code folder, read code, compile and store to
	 * default target folder.  
	 * @param cleanUpBefore - remove compiled class-files before?
	 * @return number of compiled plugins
	 */
	public static int loadAllPlugins(boolean cleanUpBefore){
		int pluginsLoaded = -1;
		
		//Get plugins folder
		String sourceCodePath = ConfigNode.pluginsFolder + defaultSourceFolder;
		String compilePath = ConfigNode.pluginsFolder + defaultTargetFolder;
		List<File> files = FilesAndStreams.directoryToFileList(sourceCodePath, null, true);
		
		//Clean-up
		if (cleanUpBefore){
			cleanUpPluginsFolder();
		}
		
		//Iterate all files and take only source code java files
		if (files != null){
			pluginsLoaded++; 	//no error, start at 0
			for (File f : files){
				if (compileJavaPluginToTarget(f, compilePath)){
					pluginsLoaded++;
				}
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
            	return compileSourceCodeToTarget(classSimpleName, sourceCode, compilePath, false);
        		
        	}catch (Exception e){
        		log.error("Plugin ERROR - Loading FAILED with msg: " + e.getMessage());
			}
		}
		return false;
	}
	
	/**
	 * Compile class from source code and store in default folders or throw error.
	 * @param classSimpleName - simple name of class
	 * @param sourceCode - source code as string
	 * @return
	 */
	public static boolean compileAndStoreSourceCode(String classSimpleName, String sourceCode){
		return compileSourceCodeToTarget(
				classSimpleName, sourceCode,
				ConfigNode.pluginsFolder + defaultTargetFolder, true
		);
	}
	
	/**
	 * Compile class from source code to target path or throw error.
	 * @param classSimpleName - simple name of class
	 * @param sourceCode - source code as string
	 * @param compilePath - target path of compiled .class
	 * @param storeCode - store source code in default source path. NOTE: will overwrite existing!
	 * @return
	 */
	public static boolean compileSourceCodeToTarget(String classSimpleName, String sourceCode, String compilePath, boolean storeCode){
    	String packageName = StringTools.findFirstRexEx(sourceCode, "^(\\s+|)(package )(.*?);")
    			.replaceFirst(".*package ", "").replaceFirst(";$", "").trim();
    	if (packageName.isEmpty()){
    		throw new RuntimeException("Package name of class missing in source code or invalid.");
    	}
		String errors = ClassBuilder.compile(packageName + "." + classSimpleName, sourceCode, new File(compilePath));
		if (!errors.isEmpty()){
			throw new RuntimeException("Class '" + classSimpleName + "' - " + errors);
		}
		if (storeCode){
			String storePath = ConfigNode.pluginsFolder + defaultSourceFolder + classSimpleName + ".java";
			boolean wrote = FilesAndStreams.writeFileFromList(storePath, Arrays.asList(sourceCode.split("\\R")));
			if (wrote){
				log.info("Plugin compiled successfully to target folder and stored as: " + storePath);
			}else{
				//try to clean up here?
				throw new RuntimeException("FAILED to store Java-file '" + storePath + "' - Reason unknown.");
			}
		}else{
			log.info("Plugin successfully compiled to: " + compilePath);
		}
		return true;
	}
	
	/**
	 * Delete all plugin source files that match given class name.
	 * @param classSimpleName - Plugins should always be named after their main class (simple name)
	 * @return
	 */
	public static int deletePluginSourceFile(String classSimpleName){
		List<File> potentialPlugins = FilesAndStreams.directoryToFileList(
				ConfigNode.pluginsFolder + defaultSourceFolder, null, true);
		//Iterate all files and take only source code java files
		int deletedFiles = 0;
		if (potentialPlugins != null){
			for (File f : potentialPlugins){
				String fileName = f.getName();
				if (fileName.endsWith(".java")){
					if (fileName.replaceFirst("\\.java$", "").trim().equals(classSimpleName)){
						f.delete();
						log.info("Plugin deleted from source directory: " + fileName);
						deletedFiles++;
					}
				}
			}
		}
		return deletedFiles;
	}
}
