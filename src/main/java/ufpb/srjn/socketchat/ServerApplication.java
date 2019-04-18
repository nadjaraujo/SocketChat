package ufpb.srjn.socketchat;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * The ServerApplication class is responsible for listening for incoming
 * connections. Every time a new client connects, a ServerThread is created to
 * treat it.
 *
 * @author samuel
 */
public class ServerApplication {
	// Logger handle
	private static final Logger LOGGER = Logger.getLogger(ServerApplication.class.getName());

	// List of open client sockets
	private static Map<String, ClientInstance> clients = new HashMap();

	/**
	 * Server entry point.
	 * 
	 * @param args Command-line parameters.
	 */
	public static void main(String[] args) {
		// Create thread pool to handle multiple clients
		ExecutorService executor = Executors.newCachedThreadPool();

		try {
			// Starts TCP server on predefined port
			LOGGER.info("Starting server...");
			ServerSocket server = new ServerSocket(27888);

			// Waits for incoming user connections
			while (true) {
				try {
					// Create client instance from open socket.
					Socket socket = server.accept();
					ClientInstance client = new ClientInstance(socket);

					if (!clients.containsKey(client.username)) {
						clients.put(client.username, client);
						executor.execute(new ServerThread(client));
					} else {
						// Another client with this username is already connected.
						LOGGER.log(Level.INFO, "User tried to login with already existing username: {0}", client.username);
						client.out.writeUTF("ERROR: This username is already taken.");
						client.out.flush();
						client.socket.close();
					}

				} catch (IOException ex) {
					LOGGER.log(Level.SEVERE, "Failed to open I/O streams after client connected: {0}", ex.getMessage());
				} catch (Exception ex) {
					LOGGER.log(Level.WARNING, "Error during client connection: {0}", ex.getMessage());
				}
			}
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Failed to start server: {0}", ex.getMessage());
		}
	}

	/**
	 * Sends a UTF-8 message to all connected clients.
	 * 
	 * @param msg Message that will be sent.
	 */
	public static void sendGlobally(String msg) {
		Iterator it = clients.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			try {
				ClientInstance client = (ClientInstance) pair.getValue();
				client.out.writeUTF(msg);
			} catch (IOException e) {
				LOGGER.log(Level.INFO, "Tried sending message to unreachable socket. Client is probably disconnected, removing from list...");
				clients.remove((String) pair.getKey());
			}
		}
	}
	
	/**
	 * Sends a UTF-8 message to a specific client.
	 * 
	 * @param msg Message that will be sent.
	 * @param username Which user to send the message to.
	 * @throws java.io.IOException
	 */
	public synchronized static void sendToClient(String msg, String username) throws IOException {
		// Check if desired user exists before trying to send.
		if (!ServerApplication.clients.containsKey(username)) {
			throw new NoSuchElementException("The username " + username + " does not exist.");
		}

		// Send to desired client.
		clients.get(username).out.writeUTF(msg);
	}
	
	/**
	 * Removes a client from the server.
	 * 
	 * @param username Client to remove.
	 */
	public synchronized static void removeClient(String username) {
		// Check if client exists first
		ClientInstance client = clients.get(username);
		if (client == null) {
			throw new NoSuchElementException("Username does not exist on the server.");
		}
		
		try {
			// Tell client to disconnect and close his socket, incase he's still here.
			client.out.writeUTF("DISCONNECT");
			client.out.flush();
			client.socket.close();
		} catch (IOException ex) {
			// Error while telling client to disconnect, he's probably already gone.
		}
		
		// Remove from server's client list.
		clients.remove(username);
	}
	
	/**
	 * Renames a client in the server.
	 * 
	 * @param old_username Original username.
	 * @param new_username New username.
	 * @throws java.io.IOException
	 */
	public synchronized static void renameClient(String old_username, String new_username) throws IOException {
		ClientInstance client = clients.get(old_username);
		
		if (ServerApplication.clients.containsKey(new_username)) {
			client.out.writeUTF("ERROR: This username is already taken.");
			return;
		}

		// Send username update to client
		client.out.writeUTF("RENAME " + new_username);
		client.username = new_username;

		// Update username on server's HashMap
		clients.remove(old_username);
		clients.put(client.username, client);
		
		// Announce username change to everyone
		sendGlobally("*** " + old_username + " changed username to " + new_username);
	}
	
	/**
	 * Returns a list of all usernames currently connected.
	 * @return List of usernames.
	 */
	public synchronized static List<String> getUsernameList() {
		List<String> list = new ArrayList<>();
		
		clients.keySet().forEach((username) -> {
			list.add(username);
		});
		
		return list;
	}
}
