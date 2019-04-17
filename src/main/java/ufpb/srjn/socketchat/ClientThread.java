package ufpb.srjn.socketchat;

import java.net.*;
import java.io.*;
import java.util.logging.*;
import java.util.Scanner;

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

	public ClientThread(ClientInstance client, ClientJFrame jframe) {
		this.client = client;
		this.jframe = jframe;
	}

	/**
	 * Client thread entry point.
	 */
	public void run() {
		try {
			String server_incoming;
			while (true) {
				server_incoming = client.in.readUTF();
				//System.out.print(server_incoming);
				jframe.sendToTextField(server_incoming);

				// Server told us to leave
				if (server_incoming.startsWith("DISCONNECT")) {
					System.exit(0);
				}

				// Server told us to change names
				if (server_incoming.startsWith("RENAME")) {
					client.username = server_incoming.split(" ")[1];
					System.out.println("*** Username changed to " + client.username);
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
			client.in.close();
			client.out.close();
			client.socket.close();
		} catch (IOException ex) {
			// Exception while closing sockets, don't need to do anything
		}
	}
}
