package sys.storage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.TimeUnit;

import utils.Random;

public class MultiCastServer {

	String name = "225.4.5.6";
	String myURL;
	
	public MultiCastServer() {
		myURL = generateURL();
	}
	
	public void sendURL() throws IOException, InterruptedException {
		TimeUnit.SECONDS.sleep(5);
		final int port = 9000;
		final InetAddress group = InetAddress.getByName(name);
		
		if(!group.isMulticastAddress()) {
			System.out.println("Not a multicast address(use range : 224.0.0.0 -- 239.255.255.255)");
		}
		
		try(MulticastSocket socket = new MulticastSocket()) {
			byte[] data = myURL.getBytes();
			DatagramPacket request = new DatagramPacket(data, data.length, group, port);
			socket.send(request);
		}
	}
	
	private String generateURL() {
		String localHost = "http://localhost";
		int port = Random.nextInt(8999) + 1;
		String URL = localHost + port + "/";
		return URL;
	}
	
	public String getURL() {
		return myURL;
	}
}
