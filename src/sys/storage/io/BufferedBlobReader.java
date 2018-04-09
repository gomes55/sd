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
	List<String> dataURIs;
	
	final Iterator<String> blocks;

	final LazyBlockReader lazyBlockIterator;
	
	public BufferedBlobReader( String name,String nameUrl,List<String> dataURIs) {
		this.name = name;
		this.dataURIs = dataURIs;
	
		ClientConfig nameConfig = new ClientConfig();
		Client nameClient = ClientBuilder.newClient(nameConfig);

		URI nameBaseURI = UriBuilder.fromUri(nameUrl).build();
		targetName = nameClient.target( nameBaseURI );
		
		/*ClientConfig dataConfig = new ClientConfig();
		Client dataClient = ClientBuilder.newClient(dataConfig);

		URI dataBaseURI = UriBuilder.fromUri(dataUrl).build();
		targetData = dataClient.target( dataBaseURI );*/
		
		Response response = targetName.path("/namenode/" + name)
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

	@SuppressWarnings("unchecked")
	private List<String> fetchBlockLines(String block) {
		
		ClientConfig dataConfig = new ClientConfig();
		Client dataClient = ClientBuilder.newClient(dataConfig);
		byte[] blockData = null;
		
		Response response = targetName.path("/namenode/list/").queryParam("prefix", name) //get all blocks
			    .request()
			    .get();
		
		List<String> data = null;
		if(response.hasEntity()) {
			System.out.println("RECEIVED");
			data = response.readEntity(List.class);
		}else{
			System.err.println(response.getStatus());
		}
		
		if(data != null) {
			for(String blob : data) {
				response = targetName.path("/namenode/" + blob)
						.request()
						.get();
				
				List<String> blobs = response.readEntity(List.class);				
				for(String blocks : blobs) {
					String[] tokens = block.split(" ");
					if(tokens[0].equals(block)) {
						URI baseURI = UriBuilder.fromUri(tokens[1]).build();
						WebTarget targetData = dataClient.target(baseURI);
						
						response = targetData.path("/datanode/" + blocks)
								.request()
								.get();
					}
					
					if(response.hasEntity()) {
						System.out.println("RECEIVED DATRAtasSDASDASDAS");
						blockData = response.readEntity(byte[].class);
					}else {
						System.err.println(response.getStatus());
					}
				}
			}
		}
		return Arrays.asList( new String(blockData).split("\\R"));
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

