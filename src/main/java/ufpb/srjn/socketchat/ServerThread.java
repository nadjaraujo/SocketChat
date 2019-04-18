package ufpb.srjn.socketchat;

import java.net.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.NoSuchElementException;
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
		// Announce new connection
		LOGGER.info("New incoming connection...");
		ServerApplication.sendGlobally("*** " + client.username + " has connected.");
		
		// Client-handling loop.
		try {
			while (true) {
				// Read incoming data from client and split by spaces.
				String incoming = client.in.readUTF();
				String[] words = incoming.split(" ");

				// Switch based on which command the client sent.
				switch (words[0]) {
					case "bye":
						// Malformed command.
						if (words.length != 1) {
							client.out.writeUTF("ERROR: bye does not take any extra parameters.");
							break;
						}
						
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
								
								// Make sure client is not sending himself a message.
								if (client.username.equals(desired_user)) {
									client.out.writeUTF("ERROR: You can't send a private message to yourself.");
								}

								String message_contents = String.join(" ", Arrays.copyOfRange(words, 3, words.length));
								String message = client.socket.getInetAddress() + ":" + client.socket.getPort() + "/~" + client.username + " to " + desired_user + " (private): " + message_contents + " " + LocalDateTime.now();

								try {
									// Send to desired client...
									ServerApplication.sendToClient(message, desired_user);
									
									// ...and echo back to the client that sent it
									client.out.writeUTF(message);
								} catch (NoSuchElementException ex) {
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
						// Malformed command.
						if (words.length != 1) {
							client.out.writeUTF("ERROR: list does not take any extra parameters.");
							break;
						}
						
						// Client asked for a list of connected clients.
						String message = "*** Connected clients: ";
						for (String username : ServerApplication.getUsernameList()) {
							message += username + " ";
						}
						client.out.writeUTF(message);

						break;
					case "rename":
						// Malformed command.
						if (words.length != 2) {
							client.out.writeUTF("ERROR: Malformed command. Proper syntax is: rename <new name>");
							break;
						}

						// Client requested a rename.
						String new_username = words[1];
						String old_username = client.username;
						ServerApplication.renameClient(old_username, new_username);					
						break;
					default:
						// Client sent some other command.
						client.out.writeUTF("ERROR: Unknown command.");
				}
			}
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "I/O exception while listening for incoming messages from client: {0}", ex.getMessage());
			ServerApplication.removeClient(client.username);
			ServerApplication.sendGlobally("*** " + client.username + " has disconnected from the server.");
		}
	}
}
