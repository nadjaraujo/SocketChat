package ufpb.srjn.socketchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * This class represents a single client instance and its attributes.
 *
 * @author samuel
 */
public class ClientInstance {
	
	// Instance attributes.
	public Socket socket;
	public DataInputStream in;
	public DataOutputStream out;
	public String username;

	/**
	 * Constructor used by the ClientApplication.
	 *
	 * @param socket The open socket associated with this client.
	 * @param in The DataInputStream associated with this client's socket.
	 * @param out The DataOutputStream associated with this client's socket.
	 * @param username The username by which this client is identified.
	 */
	public ClientInstance(Socket socket, DataInputStream in, DataOutputStream out, String username) throws IOException {
		this.socket = socket;
		this.in = in;
		this.out = out;
		this.username = username;

		// Announce our username to the server as soon as we connect
		out.writeUTF("RENAME " + username);
	}

	/**
	 * Constructor used by the ServerApplication.
	 *
	 * @param socket The open socket associated with this client.
	 * @throws java.io.IOException
	 */
	public ClientInstance(Socket socket) throws IOException, Exception {
		this.socket = socket;
		this.in = new DataInputStream(socket.getInputStream());
		this.out = new DataOutputStream(socket.getOutputStream());

		// Retrieve username as soon as client is connected.
		String[] incoming = in.readUTF().split(" ");
		if (!"RENAME".equals(incoming[0])) {
			throw new Exception("First message must be a RENAME command, got: " + String.join(" ", incoming));
		}

		if (incoming.length != 2) {
			throw new Exception("Malformed RENAME command received during client connection.");
		}

		this.username = incoming[1];
	}
}
