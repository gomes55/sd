package sys.storage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.LinkedList;
import java.util.List;

public class MultiCastClient {
	
	private static Thread client;
	
	String name = "224.4.5.6";
	List<String> serversURL;
	
	public MultiCastClient() {
		serversURL = new LinkedList<>();
	}
	
	public void listen() throws IOException, InterruptedException {
		final int MAX_DATAGRAM_SIZE = 65536;
		final InetAddress group = InetAddress.getByName(name);
		
		if(!group.isMulticastAddress()) {
			System.out.println("Not a multicast address(use range : 224.0.0.0 -- 239.255.255.255)");
			System.exit(1);
		}
		
		try(MulticastSocket socket = new MulticastSocket(9000)) {
			socket.joinGroup(group);
			
			while(true) {
				byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				socket.receive(request);
				String data = new String(request.getData(), 0, request.getLength());
				
				if(!serversURL.contains(data))
					serversURL.add(data);
				else;
					System.out.println("RECEIVED URL --- " + data);
			}
		}
	}
	
	public void getURL() {
		client = new Thread(() -> {
			try {
				listen();
			}catch(Exception e) {
				System.out.println("Server Exception");
			}
		});
		client.start();
	}
	
	public List<String> getURLS() {
		return serversURL;
	}
}
