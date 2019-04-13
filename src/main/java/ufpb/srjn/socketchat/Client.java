package ufpb.srjn.socketchat;

import java.net.*;
import java.io.*;
import java.util.logging.*;
import java.util.Scanner;

/**
 *
 * @author samuel
 */
public class Client {
	// Logger handle
	private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

	// Socket
	private static Socket socket;

	// I/O streams
	private static DataInputStream in;
	private static DataOutputStream out;
	
	// Username
	private static String username;

	/**
	 *
	 * Client application entry point.
	 *
	 * @param args Command-line parameters: [IP address] [port] [username]
	 */
	public static void main(String[] args) {
		// Connect to server
		try {
			socket = new Socket(args[0], Integer.parseInt(args[1]));
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException ex) {
			System.out.println("*** Error while connecting to server: " + ex.getMessage());
		} catch (NumberFormatException ex) {
			System.out.println("*** Error: " + args[1] + " is not a valid port number.");
		}

		// Run
		username = args[2];
		Scanner sc = new Scanner(System.in);
		String user_input, server_incoming;

		try {
			while (true) {
				server_incoming = in.readUTF();
				System.out.print(server_incoming);

				// Server told us to leave
				if (server_incoming.equals("DISCONNECT"))
					break;
				
				// Server told us to change names
				if (server_incoming.startsWith("RENAME"))
					username = server_incoming.split(" ")[1];

				// Message ended with ": ", server is expecting user input
				// FIXME: This will not work. Think of some way to be able to
				// receive both user and server input at the same time.
				if (server_incoming.endsWith(": ")) {
					user_input = sc.nextLine();
					out.writeUTF(user_input);
				}
			}
		} catch (IOException ex) {
			System.out.println("*** Error during communication with server: " + ex.getMessage());
		}

		try {
			// Exit
			in.close();
			out.close();
			socket.close();
		} catch (IOException ex) {
			// Exception while closing sockets, don't need to do anything
		}

		sc.close();
	}
}
