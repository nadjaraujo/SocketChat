package ufpb.srjn.socketchat;

import java.net.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.logging.*;

/**
 * ServerThread dedicated to handling a single client connection. The main
 * ServerApplication creates a new instance of this class every time a new
 * client connects to the server.
 *
 * @author samuel
 */
public class ServerThread implements Runnable {
	
	// Logger handle
	private static final Logger LOGGER = Logger.getLogger(ServerThread.class.getName());

	// Connected client instance
	private final ClientInstance client;

	/**
	 * Constructor.
	 * 
	 * @param client Client instance with an already-open socket.
	 */
	public ServerThread(ClientInstance client) {
		this.client = client;
	}

	/**
	 * Handles a single client.
	 */
	@Override
	public void run() {
		LOGGER.info("New incoming connection...");

		// Client-handling loop.
		try {
			while (true) {
				// Read incoming data from client and split by spaces.
				String incoming = client.in.readUTF();
				String[] words = incoming.split(" ");

				// Switch based on which command the client sent.
				switch (words[0]) {
					case "bye":
						// Client wants to disconnect.
						client.out.writeUTF("DISCONNECT");
						break;
					case "send":
						// Client wants to send a message.
						try {
							// Message format: <IP>:<PORT>/~<username> : <message> <hour-date>
							if ("-all".equals(words[1])) {
								// Send to all users.
								String message_contents = String.join(" ", Arrays.copyOfRange(words, 2, words.length));
								String message = client.socket.getInetAddress() + ":" + client.socket.getPort() + "/~" + client.username + ": " + message_contents + " " + LocalDateTime.now();

								ServerApplication.sendGlobally(message);
							} else if ("-user".equals(words[1])) {
								// Send to a specific user.
								String desired_user = words[2];

								// Check if desired user exists before trying to send.
								if (ServerApplication.clients.containsKey(desired_user)) {
									String message_contents = String.join(" ", Arrays.copyOfRange(words, 3, words.length));
									String message = client.socket.getInetAddress() + ":" + client.socket.getPort() + "/~" + client.username + ": " + message_contents + " " + LocalDateTime.now();

									ServerApplication.clients.get(desired_user).out.writeUTF(message);
								} else {
									client.out.writeUTF("ERROR: The username " + desired_user + " does not exist.");
								}
							} else {
								// Second parameter was neither -all nor -user, throw exception.
								throw new Exception("Missing or wrong parameters for send command.");
							}
						} catch (ArrayIndexOutOfBoundsException ex) {
							// Some parameters were missing from the command.
							client.out.writeUTF("ERROR: Malformed command.");
						} catch (Exception ex) {
							client.out.writeUTF("ERROR: " + ex.getMessage());
						}
						break;
					case "list":
						// Client asked for a list of connected clients.
						String message = "*** Connected clients: ";
						for (String username : ServerApplication.clients.keySet()) {
							message += username + " ";
						}
						client.out.writeUTF(message);

						break;
					case "rename":
						// Client requested an username change.
						if (words.length != 2) {
							client.out.writeUTF("ERROR: Malformed command. Proper syntax is: rename <new name>");
							break;
						}

						String new_username = words[1];
						String old_username = client.username;
						if (ServerApplication.clients.containsKey(new_username)) {
							client.out.writeUTF("ERROR: This username is already taken.");
							break;
						}

						// Send username update to client
						client.out.writeUTF("RENAME " + new_username);
						client.username = new_username;
						
						// Update username on server's HashMap
						ServerApplication.clients.remove(old_username);
						ServerApplication.clients.put(client.username, client);
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
