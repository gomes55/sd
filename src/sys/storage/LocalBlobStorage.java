package sys.storage;
import java.io.IOException;
//changed
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;

import api.storage.BlobStorage;
import sys.storage.io.BufferedBlobReader;
import sys.storage.io.BufferedBlobWriter;

public class LocalBlobStorage implements BlobStorage {
	
	private static final int BLOCK_SIZE=512;
	WebTarget targetName;
	MultiCastClient helper;
	List<String> dataURIs;
	String nameURI;

	public LocalBlobStorage() throws IOException, InterruptedException{
		
		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);
		
		helper = new MultiCastClient();
		helper.getURL(); //why this
		
		List<String> urls = helper.getURLS();
		while(urls.size() == 0) {
			TimeUnit.SECONDS.sleep(2);
			urls = helper.getURLS();
		}
		nameURI = urls.remove(0);
		URI baseURI = UriBuilder.fromUri(nameURI).build();
		targetName = client.target(baseURI); //abrir primeiro o nameClient depois os data
		
		//why this again
		while(urls.size() == 0) {
			TimeUnit.SECONDS.sleep(2);
			urls = helper.getURLS();
		}
		//alterar isto para so ficar com a lista isto e so para experimentar
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> listBlobs(String prefix) {
		
		List<String>data = null;
		
		Response response = targetName.path("/namenode/list/").queryParam("prefix", prefix)
			    .request()
			    .get();
			      
		if( response.hasEntity() ) {
			System.out.println( "RECEIVED" );
			data = response.readEntity(List.class); 
		}else{
			System.err.println(response.getStatus());
		}
			
		return data;
		
		//return namenode.list(prefix);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void deleteBlobs(String prefix) {
		
		ClientConfig dataConfig = new ClientConfig();
		Client dataClient = ClientBuilder.newClient(dataConfig);
		
		Response response = targetName.path("/namenode/list/").queryParam("prefix", prefix)
			    .request()
			    .get();
		List<String> data = null;
		if( response.hasEntity() ) {
			System.out.println( "RECEIVED" );
			data = response.readEntity(List.class); 
		}else{
			System.err.println( response.getStatus());
			
		}
		
		if(data!=null) {
			for(String blob : data) {
				response = targetName.path("/namenode/"+blob)
						.request()
						.get();
			
				List<String> blobs = response.readEntity(List.class);
		
				for(String block : blobs) {
					String[] tokens = block.split(" ");
					URI baseURI = UriBuilder.fromUri(tokens[1]).build();
					WebTarget targetData = dataClient.target(baseURI);
				
					response = targetData.path("/datanode/"+block)
							.request()
							.delete();	
				}	
			}
		}
		
		targetName.path("/namenode/list/" + prefix)
			.request()
			.delete();
		
		/*targetName.path("/namenode/list/").queryParam("prefix", prefix)
		.request()
		.delete();*/	
		
		/*namenode.list( prefix ).forEach( blob -> {
			namenode.read( blob ).forEach( block -> {
				datanodes[0].deleteBlock(block);
			});
		});
		namenode.delete(prefix);*/
	}

	@Override
	public BlobReader readBlob(String name) {
		return new BufferedBlobReader( name, nameURI, dataURIs);
	}

	@Override
	public BlobWriter blobWriter(String name) {
		dataURIs = helper.getURLS(); //dar update pois o thread pode ter recebido mais URIs
		return new BufferedBlobWriter( name, BLOCK_SIZE, nameURI, dataURIs);
		//return new BufferedBlobWriter( name, namenode, datanodes, BLOCK_SIZE);
	}
}
