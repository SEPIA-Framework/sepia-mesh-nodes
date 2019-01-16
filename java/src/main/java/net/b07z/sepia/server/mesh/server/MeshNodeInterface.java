package net.b07z.sepia.server.mesh.server;

/**
 * Each Mesh-Node has to implement this interface.
 * 
 * @author Florian Quirin
 *
 */
public interface MeshNodeInterface {
	
	/**
	 * Defines the end-points that this mesh node supports.
	 */
	public void loadEndpoints();

	/**
	 * Start the mesh node server.
	 * @param args - arguments like "--test" or "--live" to switch server config etc.
	 */
	public void start(String[] args);
}
