package sys.storage;
//changed
import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;

import api.storage.BlobStorage;
import api.storage.Datanode;
import api.storage.Namenode;
import sys.storage.io.BufferedBlobReader;
import sys.storage.io.BufferedBlobWriter;

public class LocalBlobStorage implements BlobStorage {
	private static final int BLOCK_SIZE=512;

	/*Namenode namenode;
	Datanode[] datanodes;*/
	
	WebTarget targetName;
	WebTarget targetData;

	public LocalBlobStorage() {
		/*this.namenode = new NamenodeClient();
		this.datanodes = new Datanode[] { new DatanodeClient() };*/
		
		ClientConfig nameConfig = new ClientConfig();
		Client nameClient = ClientBuilder.newClient(nameConfig);

		URI nameBaseURI = UriBuilder.fromUri("http://localhost:9999/v1").build();
		targetName = nameClient.target( nameBaseURI );
		
		
		ClientConfig dataConfig = new ClientConfig();
		Client dataClient = ClientBuilder.newClient(dataConfig);

		URI dataBaseURI = UriBuilder.fromUri("http://localhost:9990/v2").build();
		targetData = dataClient.target( dataBaseURI );
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
				response = targetData.path("/datanode/"+block)
						.request()
						.delete();	
			}	
		}
		}
		
		targetName.path("/namenode/list/").queryParam("prefix", prefix)
		.request()
		.delete();	
		
		/*namenode.list( prefix ).forEach( blob -> {
			namenode.read( blob ).forEach( block -> {
				datanodes[0].deleteBlock(block);
			});
		});
		namenode.delete(prefix);*/
	}

	@Override
	public BlobReader readBlob(String name) {
		return new BufferedBlobReader( name,"http://localhost:9999/v1","http://localhost:9990/v2");
	}

	@Override
	public BlobWriter blobWriter(String name) {
		return new BufferedBlobWriter( name, BLOCK_SIZE,"http://localhost:9999/v1","http://localhost:9990/v2");
		//return new BufferedBlobWriter( name, namenode, datanodes, BLOCK_SIZE);
	}
}
