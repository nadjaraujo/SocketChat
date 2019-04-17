package ufpb.srjn.socketchat;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
	public static Map<String, ClientInstance> clients = new HashMap();

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
}
