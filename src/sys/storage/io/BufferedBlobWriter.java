package sys.storage.io;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;

import api.storage.BlobStorage.BlobWriter;
import utils.IO;
import utils.Random;

/*
 * 
 * Implements a ***centralized*** BlobWriter.
 * 
 * Accumulates lines in a list of blocks, avoids splitting a line across blocks.
 * When the BlobWriter is closed, the Blob (and its blocks) is published in the Namenode.
 * 
 */
public class BufferedBlobWriter implements BlobWriter {
	final String name;
	final int blockSize;
	final ByteArrayOutputStream buf;
	WebTarget targetName;
	List<String> dataURIs;

	final List<String> blocks = new LinkedList<>();
	
	public BufferedBlobWriter(String name, int blockSize , String nameUrl, List<String> dataURIs) {
		this.name = name;
		this.dataURIs = dataURIs;
		
		ClientConfig nameConfig = new ClientConfig();
		Client nameClient = ClientBuilder.newClient(nameConfig);

		URI nameBaseURI = UriBuilder.fromUri(nameUrl).build();
		targetName = nameClient.target( nameBaseURI );

		/*URI dataBaseURI = UriBuilder.fromUri(dataUrl).build();
		targetData = dataClient.target( dataBaseURI );*/		
		
		this.blockSize = blockSize;
		this.buf = new ByteArrayOutputStream( blockSize );
	}

	private void flush( byte[] data, boolean eob ) {
		ClientConfig dataConfig = new ClientConfig();
		Client dataClient = ClientBuilder.newClient(dataConfig);
		
		String dataUri = dataURIs.get(Random.nextInt(dataURIs.size()));
		URI dataBaseURI = UriBuilder.fromUri(dataUri).build();
		WebTarget targetData = dataClient.target(dataBaseURI);
		
		//String[] postResp;
		
		Response response = targetData.path("/datanode/")   //ver se preciso de alterar
			    .request()
			    .post( Entity.entity( data, MediaType.APPLICATION_OCTET_STREAM));
				
		if( response.hasEntity() ) {
			String postResp = response.readEntity(String.class); 
			blocks.add(postResp); 
		}else{
			System.err.println( response.getStatus());
		}		
	
		if( eob ) {
			response = targetName.path("/namenode/"+name)  
					.request()
					.post( Entity.entity( blocks, MediaType.APPLICATION_JSON));
			blocks.clear();
		}
	}

	@Override
	public void writeLine(String line) {
		if( buf.size() + line.length() > blockSize - 1 ) {
			this.flush(buf.toByteArray(), false);
			buf.reset();
		}
		IO.write( buf, line.getBytes() );
		IO.write( buf, '\n');
	}

	@Override
	public void close() {
		flush( buf.toByteArray(), true );
	}
}