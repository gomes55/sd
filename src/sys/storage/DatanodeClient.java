package sys.storage;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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

	private static final int INITIAL_SIZE = 32;
	private Map<String, byte[]> blocks = new HashMap<>(INITIAL_SIZE);
	
	static String myURI;
	
	public static void main(String [] args) throws IOException, InterruptedException {
		
		MultiCastServer helper = new MultiCastServer();
		
		ResourceConfig config = new ResourceConfig();
		config.register( new DatanodeClient() );
		JdkHttpServerFactory.createHttpServer( URI.create(helper.getURL()), config);
		
		helper.sendURL();
		myURI = helper.getURL();

		System.err.println("Datanode Server ready @ " + myURI);
	}
	
	
	@Override
	public String createBlock(byte[] data) {
		String id = Random.key64() + " " + myURI; //Return string with URI + id
		System.out.println(id);
		
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
