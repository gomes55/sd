package sys.storage;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import api.storage.Namenode;

/*
 * Fake NamenodeClient client.
 * 
 * Rather than invoking the Namenode via REST, executes
 * operations locally, in memory.
 * 
 * Uses a trie to perform efficient prefix query operations.
 */
public class NamenodeClient implements Namenode {

	//private static Logger logger = Logger.getLogger(NamenodeClient.class.toString() );
	
	Trie<String, List<String>> names = new PatriciaTrie<>();
	
	public static void main(String [] args) throws IOException {
		
		String URI_BASE = "http://0.0.0.0:9999/v1/";//Different ports

		ResourceConfig config = new ResourceConfig();
		config.register( new NamenodeClient() );

		JdkHttpServerFactory.createHttpServer( URI.create(URI_BASE), config);

		System.err.println("Namenode Server ready....");
	}
	
	@Override
	public List<String> list(String prefix) {
		List<String> data = new ArrayList<>(names.prefixMap( prefix ).keySet());
		if(data.size() != 0) 
			return data;
		else 
			throw new WebApplicationException(Status.NOT_FOUND );   
		
		//return new ArrayList<>(names.prefixMap( prefix ).keySet());
	}

	@Override
	public void create(String name,  List<String> blocks) {
		 if( names.putIfAbsent(name, new ArrayList<>(blocks)) != null)
			 throw new WebApplicationException( Status.CONFLICT );
		
		/*if( names.putIfAbsent(name, new ArrayList<>(blocks)) != null )
			logger.info("CONFLICT");*/
	}

	@Override
	public void delete(String prefix) {
		List<String> keys = new ArrayList<>(names.prefixMap( prefix ).keySet());
		if( ! keys.isEmpty() )
			names.keySet().removeAll( keys );
		else  
			throw new WebApplicationException(Status.NOT_FOUND );
		
		/*List<String> keys = new ArrayList<>(names.prefixMap( prefix ).keySet());
		if( ! keys.isEmpty() )
			names.keySet().removeAll( keys );*/
	}

	@Override
	public void update(String name, List<String> blocks) {
		 if( names.replace( name, blocks ) == null ) {
			 throw new WebApplicationException(Status.NOT_FOUND );            
	     }
		
		/*if( names.putIfAbsent( name, new ArrayList<>(blocks)) == null ) {
			logger.info("NOT FOUND");
		}*/
	}

	@Override
	public List<String> read(String name) {
		List<String> blocks = names.get( name );
		if( blocks == null )
			throw new WebApplicationException(Status.NOT_FOUND );           
		return blocks;
		
		
		/*List<String> blocks = names.get( name );
		if( blocks == null )
			logger.info("NOT FOUND");
		return blocks;*/
	}
}
