package sys.storage.io;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;

import api.storage.BlobStorage.BlobReader;
import api.storage.Datanode;
import api.storage.Namenode;

/*
 * 
 * Implements BlobReader.
 * 
 * Allows reading or iterating the lines of Blob one at a time, by fetching each block on demand.
 * 
 * Intended to allow streaming the lines of a large blob (with mamy (large) blocks) without reading it all first into memory.
 */
public class BufferedBlobReader implements BlobReader {

	final String name;
	WebTarget targetName;
	WebTarget targetData;
	
	final Iterator<String> blocks;

	final LazyBlockReader lazyBlockIterator;
	
	public BufferedBlobReader( String name,String nameUrl,String dataUrl) {
		this.name = name;
	
		ClientConfig nameConfig = new ClientConfig();
		Client nameClient = ClientBuilder.newClient(nameConfig);

		URI nameBaseURI = UriBuilder.fromUri(nameUrl).build();
		targetName = nameClient.target( nameBaseURI );
		
		ClientConfig dataConfig = new ClientConfig();
		Client dataClient = ClientBuilder.newClient(dataConfig);

		URI dataBaseURI = UriBuilder.fromUri(dataUrl).build();
		targetData = dataClient.target( dataBaseURI );
		
		Response response = targetName.path("/namenode/"+name)
			    .request()
			    .get();
		
		@SuppressWarnings("unchecked")
		List<String> data = response.readEntity(List.class);
		
		this.blocks = data.iterator();
		this.lazyBlockIterator = new LazyBlockReader();
	}
	
	@Override
	public String readLine() {
		return lazyBlockIterator.hasNext() ? lazyBlockIterator.next() : null ;
	}
	
	@Override
	public Iterator<String> iterator() {
		return lazyBlockIterator;
	}
	
	private Iterator<String> nextBlockLines() {
		if( blocks.hasNext() )
			return fetchBlockLines( blocks.next() ).iterator();
		else 
			return Collections.emptyIterator();
	} 

	private List<String> fetchBlockLines(String block) {

		Response response = targetData.path("/datanode/"+block)
			    .request()
			    .get();
		
		@SuppressWarnings("unchecked")
		byte[] data = response.readEntity(byte[].class);
		return Arrays.asList( new String(data).split("\\R"));
	}
	
	private class LazyBlockReader implements Iterator<String> {
		
		Iterator<String> lines;
		
		LazyBlockReader() {
			this.lines = nextBlockLines();
		}
		
		@Override
		public String next() {
			return lines.next();
		}

		@Override
		public boolean hasNext() {
			return lines.hasNext() || (lines = nextBlockLines()).hasNext();
		}	
	}
}

