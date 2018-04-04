package sys.storage;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import api.storage.Datanode;
import utils.Random;

/*
 * Fake Datanode client.
 * 
 * Rather than invoking the Datanode via REST, executes
 * operations locally, in memory.
 * 
 */
public class DatanodeClient implements Datanode {
	//private static Logger logger = Logger.getLogger(Datanode.class.toString() );

	private static final int INITIAL_SIZE = 32;
	private Map<String, byte[]> blocks = new HashMap<>(INITIAL_SIZE);
	
	public static void main(String [] args) throws IOException {
		
		String URI_BASE = "http://0.0.0.0:9990/v2/"; //Different ports

		ResourceConfig config = new ResourceConfig();
		config.register( new DatanodeClient() );

		JdkHttpServerFactory.createHttpServer( URI.create(URI_BASE), config);

		System.err.println("Datanode Server ready....");
	}
	
	
	@Override
	public String createBlock(byte[] data) {
		String id = Random.key64(); //Return string with URI + id
		
		if( blocks.putIfAbsent(id, data) != null)
			throw new WebApplicationException( Status.CONFLICT );
	    else
	        return id;
		/*blocks.put( id, data);
		return id;*/
	}

	@Override
	public void deleteBlock(String block) {
		if( blocks.remove( block ) == null ) {
            throw new WebApplicationException(Status.NOT_FOUND );
		}    
		//blocks.remove(block);
	}

	@Override
	public byte[] readBlock(String block) {
		byte[] data =  blocks.get(block);
		if( data != null )
			return data;
		else
			throw new RuntimeException("NOT FOUND");
	}
}
