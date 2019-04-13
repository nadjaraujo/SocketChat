package ufpb.srjn.socketchat;

import java.net.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.logging.*;

/**
 *
 * @author samuel
 */
public class ServerThread implements Runnable {
	// Logger handle
	private static final Logger LOGGER = Logger.getLogger(ServerThread.class.getName());

	// Connected client instance
	private final ClientInstance client;

	/**
	 *
	 * Constructor.
	 *
	 * @param client Client instance with an already-open socket.
	 */
	public ServerThread(ClientInstance client) {
		this.client = client;
	}

	/**
	 *
	 * Handles a single client.
	 *
	 */
	@Override
	public void run() {
		LOGGER.info("New incoming connection...");

		// Client-handling loop.
		try {
			while (true) {
				String incoming = client.in.readUTF();
				String[] words = incoming.split(" ");
				
				switch (words[0]) {
					case "bye":
						// Client wants to disconnect.
						client.out.writeUTF("DISCONNECT");
						break;
					case "send":
						try {
							// Message format: <IP>:<PORT>/~<username> : <message> <hour-date>
							String message_contents = String.join(" ", Arrays.copyOfRange(words, 2, words.length)); // TODO: Test if this line works.
							String message = client.socket.getInetAddress() + ":" + client.socket.getPort() + "/~" + client.username + ": " + message_contents + " " + LocalDateTime.now();
							
							if ("-all".equals(words[1])) {
								Server.sendGlobally(message);
							} else if ("-user".equals(words[1])) {
								// TODO: Send message to a specific user.
							} else {
								// Second parameter was neither -all nor -user, throw exception.
								throw new Exception("Missing or wrong parameters for send command.");
							}
						} catch (ArrayIndexOutOfBoundsException ex) {
							// Parameters were missing from the command.
							client.out.writeUTF("ERROR: Malformed command.");
						} catch (Exception ex) {
							client.out.writeUTF("ERROR: " + ex.getMessage());
						}
						break;
					case "list":
						String message = "*** Connected clients: ";
						for (String username : Server.clients.keySet()) {
							message += username + " ";
						}
						client.out.writeUTF(message);
						
						break;
					case "rename":
						// Client requested username change.
						if (words.length != 2) {
							client.out.writeUTF("ERROR: Malformed command. Proper syntax is: rename <new name>");
							break;
						}
						
						String new_username = words[1];
						if (Server.clients.containsKey(new_username)) {
							client.out.writeUTF("ERROR: This username is already taken.");
							break;
						}
						
						client.out.writeUTF("RENAME " + new_username);
						break;
					default:
						// Client sent some other command.
						client.out.writeUTF("ERROR: Unknown command.");
				}
			}
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "Error while listening for incoming messages from client: {0}", ex.getMessage());
		}
	}
}
