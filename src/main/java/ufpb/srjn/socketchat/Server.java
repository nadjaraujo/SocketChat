package ufpb.srjn.socketchat;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 *
 * @author samuel
 */
public class Server {
	// Logger handle
	private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

	// List of open client sockets
	private static ArrayList<ClientInstance> clients = new ArrayList();

	/**
	 *
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
					clients.add(client);
					executor.execute(new ServerThread(client));
				} catch (IOException ex) {
					LOGGER.log(Level.SEVERE, "Failed to open I/O streams after client connected: {0}", ex.getMessage());
				}
			}
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Failed to start server: {0}", ex.getMessage());
		}
	}

	/**
	 *
	 * Sends a UTF-8 message to all connected clients.
	 *
	 * @param msg Message that will be sent.
	 */
	public static void sendGlobally(String msg) {
		sockets.forEach((s) -> {
			try {
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.writeUTF(msg);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Tried sending message to unreachable socket. Client is probably disconnected, removing from list...");
				sockets.remove(s);
			}
		});
	}
}
