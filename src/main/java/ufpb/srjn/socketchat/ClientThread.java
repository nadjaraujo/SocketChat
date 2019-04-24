package ufpb.srjn.socketchat;

import java.io.*;
import java.util.logging.*;
import javax.swing.JOptionPane;

/**
 * This class runs alongside the client window and reads incoming messages from
 * the server.
 *
 * @author samuel
 */
public class ClientThread implements Runnable {

	// Logger handle
	private static final Logger LOGGER = Logger.getLogger(ClientThread.class.getName());

	// Client instance
	private ClientInstance client;

	// JFrame instance that started this thread
	private ClientJFrame jframe;

	/**
	 * Constructor: receives a ClientInstance that's already connected to a server
	 * and the client's JFrame.
	 * @param client A ClientInstance that's already connected to a server.
	 * @param jframe The Client's JFrame.
	 */
	public ClientThread(ClientInstance client, ClientJFrame jframe) {
		this.client = client;
		this.jframe = jframe;
	}

	/**
	 * Client thread entry point.
	 */
	@Override
	public void run() {
		try {
			String server_incoming;
			while (true) {
				server_incoming = client.readIn();
				LOGGER.log(Level.INFO, server_incoming);

				// Server told us to leave
				if (server_incoming.startsWith("DISCONNECT")) {
					System.exit(0);
				}

				// Server told us to change names
				else if (server_incoming.startsWith("RENAME")) {
					client.username = server_incoming.split(" ")[1];
				}
				
				// Normal message incoming
				else {
					jframe.sendToTextField(server_incoming);
				}
			}
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(
				jframe,
				"Lost connection to server.",
				"Error",
				JOptionPane.ERROR_MESSAGE
				);
			System.exit(0);
		}

		try {
			// Exit
			client.close();
		} catch (IOException ex) {
			// Exception while closing sockets, don't need to do anything
		}
	}
}
