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
	private DataInputStream in;
	private DataOutputStream out;
	public String username;
	public Authenticator auth;

	/**
	 * Constructor used by the ClientApplication.
	 *
	 * @param socket The open socket associated with this client.
	 * @param in The DataInputStream associated with this client's socket.
	 * @param out The DataOutputStream associated with this client's socket.
	 * @param username The username by which this client is identified.
	 * @throws java.io.IOException
	 */
	public ClientInstance(Socket socket, DataInputStream in, DataOutputStream out, String username) throws IOException {
		this.socket = socket;
		this.in = in;
		this.out = out;
		this.username = username;

		// Send the hash to the server
		out.writeUTF("HASH " + Authenticator.getHashedPassword());
		
		// Announce our username to the server as soon as we connect
		out.writeUTF(Authenticator.encrypt("RENAME " + username));
		
		// Check if we connected correctly. If not, throw new exception
		String response = in.readUTF();
		if (!"OK".equals(response)) {
			throw new IOException(response);
		}
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

		// Retrieve hash and test against server password.
		String[] incoming = in.readUTF().split(" ");
		if (!"HASH".equals(incoming[0])) {
			throw new Exception("First message must be a HASH command, got: " + String.join(" ", incoming));
		}
		
		String client_hash = incoming[1];
		if (!client_hash.equals(Authenticator.getHashedPassword())) {
			this.out.writeUTF("Invalid password.");
			this.close();
		}
		
		// Retrieve client's username
		incoming = Authenticator.decrypt(in.readUTF()).split(" ");
		if (!"RENAME".equals(incoming[0])) {
			throw new Exception("Second message must be a RENAME command, got: " + String.join(" ", incoming));
		}

		if (incoming.length != 2) {
			throw new Exception("Malformed RENAME command received during client connection.");
		}

		this.username = incoming[1];
		
		// Connected successfully!
		this.out.writeUTF("OK");
	}
	
	/**
	 * Writes an encrypted message to the client's output stream.
	 * @param msg
	 * @throws IOException 
	 */
	public void writeOut(String msg) throws IOException {
		out.writeUTF(Authenticator.encrypt(msg));
	}
	
	/**
	 * Reads an encrypted message from client's socket and returns it decrypted.
	 * @return Decrypted incoming message.
	 * @throws IOException 
	 */
	public String readIn() throws IOException {
		return Authenticator.decrypt(in.readUTF());
	}
	
	/**
	 * Close client's sockets.
	 * @throws java.io.IOException
	 */
	public void close() throws IOException {
		in.close();
		out.flush();
		out.close();
		socket.close();
	}
}
