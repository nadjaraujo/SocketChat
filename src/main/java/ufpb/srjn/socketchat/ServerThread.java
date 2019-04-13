package ufpb.srjn.socketchat;

import java.net.*;
import java.io.*;
import java.util.logging.*;

/**
 *
 * @author samuel
 */
public class ServerThread implements Runnable {
	// Logger handle
	private static final Logger LOGGER = Logger.getLogger(ServerThread.class.getName());

	// Client's socket
	private final Socket socket;

	// Client's I/O streams
	private DataInputStream in;
	private DataOutputStream out;

	/**
	 *
	 * Constructor.
	 *
	 * @param socket Socket that's already connected to the client.
	 */
	public ServerThread(Socket socket) {
		this.socket = socket;
	}

	/**
	 *
	 * Handles a single client.
	 *
	 */
	@Override
	public void run() {
		LOGGER.info("New incoming connection...");

		// Open socket I/O streams.
		try {
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Failed to open I/O streams after client connected: {0}", ex.getMessage());
		}

		// Client-handling loop.
		try {
			while (true) {
				String incoming = in.readUTF();
				String[] words = incoming.split(" ");
				
				switch (words[0]) {
					case "bye":
						// Client wants to disconnect.
						out.writeUTF("DISCONNECT");
						break;
					case "send":
						try {
							// Message format: <IP>:<PORT>/~<username> : <message> <hour-date>
							if ("-all".equals(words[1])) {
								// TODO: Send message to everyone.
								String message = "";
								Server.sendGlobally(message);
							} else if ("-user".equals(words[1])) {
								// TODO: Send message to a specific user.
							} else {
								// Second parameter was neither -all nor -user, throw exception.
								throw new Exception("Missing or wrong parameters for send command.");
							}
						} catch (ArrayIndexOutOfBoundsException ex) {
							// Parameters were missing from the command.
							out.writeUTF("ERROR: Malformed command.");
						} catch (Exception ex) {
							out.writeUTF("ERROR: " + ex.getMessage());
						}
						break;
					case "list":
						// TODO: List all users in server.
						break;
					case "rename":
						// Client requested username change.
						if (words.length != 2) {
							out.writeUTF("ERROR: Malformed command. Proper syntax is: rename <new name>");
							break;
						}
						
						// TODO: Prevent two users from having the same username.
						String new_username = words[1];
						out.writeUTF("RENAME " + new_username);
						break;
					default:
						// Client sent some other command.
						out.writeUTF("ERROR: Unknown command.");
				}
			}
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "Error while listening for incoming messages from client: {0}", ex.getMessage());
		}
	}
}
