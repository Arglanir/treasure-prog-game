/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * MyClientSocket class
 * @author Cedric Mayer, 2018
 */
public class ManualGameConsoleClient {

    /**
     * @param args
     */
    public static void main(String[] args) {

        final String hostName;
        final int portNumber;
        // helped by
        // https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
        if (args.length != 2) {
            System.err.println("Usage: java " + ManualGameConsoleClient.class.getName() + " <host name> <port number>");
            // System.exit(1);
            hostName = "localhost";
            portNumber = 8081;
        } else {
            hostName = args[0];
            portNumber = Integer.parseInt(args[1]);
        }

        // connect and open streams
        try (Socket clientSocket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {
        	// ask for name
            System.out.println("Write your name: ");
            out.println(stdIn.readLine() + "-manual");
            System.out.println("Wait a little for a map to start...");
            // main loop
            while (true) {
                String serverReturned = in.readLine();
                if (serverReturned == null) {
                    // server lost
                    break;
                }
                System.out.println(serverReturned);
                if (serverReturned.startsWith("Found")) {
                    // treasure found. Next line is the good one
                    serverReturned = in.readLine();
                    if (serverReturned == null) {
                        // server lost
                        break;
                    }
                    System.out.println(serverReturned);
                }
                // maybe you could parse serverReturned to display the map in the console directly?
                
                // read and send
                String userInput = stdIn.readLine();
                out.println(userInput);
            }
            System.out.println("End of communication with the server.");
            // some common problems
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }

    }

}
