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

		try {
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Failed to open I/O streams after client connected: {0}", ex.getMessage());
		}

		try {
			while (true) {
				String incoming = in.readUTF();

				// TODO: Process incoming messages.
			}
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "Error while listening for incoming messages from client: {0}", ex.getMessage());
		}
	}
}
