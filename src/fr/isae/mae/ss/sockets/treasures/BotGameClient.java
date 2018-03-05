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
import java.util.Random;

/**
 * MyClientSocket class
 * @author Cedric Mayer, 2018
 */
public class BotGameClient {

    /**
     * @param args
     */
    public static void main(String[] args) {

        final String hostName;
        final int portNumber;
        // helped by
        // https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
        if (args.length != 2) {
            // System.exit(1);
            hostName = "localhost";
            portNumber = 8081;
        } else {
            hostName = args[0];
            portNumber = Integer.parseInt(args[1]);
        }

        try (Socket clientSocket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {
            out.println("Cedric-bot");
            Object[][] currentMap = new Object[200][200];
            int score = 0; 
            while (true) {
                String serverReturned = in.readLine();
                if (serverReturned == null) {
                    // server lost
                    break;
                }
                System.out.println(serverReturned);
                //System.out.println(serverReturned);
                if (serverReturned.startsWith("Found")) {
                	
                	
                	
                	// increment the score
                	score += Integer.parseInt(serverReturned.split(" ")[1]);
                	System.out.println("New score: "+score);
                	
                	
                	
                	
                	
                    // treasure found. Next line is the good one
                    serverReturned = in.readLine();
                    if (serverReturned == null) {
                        // server lost
                        break;
                    }
                    System.out.println(serverReturned);
                }
                
                // display nicely
                String[] returned = serverReturned.split(" ");
                for (int i = 0; i < 3; i++) {
                	System.out.println(returned[2].substring(3*i, 3*i+3));
                }
                
                
                
                
                int x = Integer.parseInt(returned[0]);
                int y = Integer.parseInt(returned[1]);
                double intensity = Double.parseDouble(returned[3]);
                
                
                
                // read and send
                //String userInput = stdIn.readLine();
                Random random = new Random();
                String[] actions = new String[] {"UP", "DOWN", "LEFT", "RIGHT"}; 
                int i = random.nextInt(actions.length);
                out.println(actions[i]);
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
