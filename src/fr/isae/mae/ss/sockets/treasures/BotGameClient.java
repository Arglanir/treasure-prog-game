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
            System.err.println("Usage: java "+BotGameClient.class.getName()+" <host name> <port number>");
            // System.exit(1);
            hostName = "localhost"; // maybe write here the hostname of the teacher's computer
            portNumber = 8081;
        } else {
            hostName = args[0];
            portNumber = Integer.parseInt(args[1]);
        }
        // ask for the name... or you might want to hard-code it
        String botName = System.getProperty("user.name")+"-bot";
        try (
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Write your name (default: "+botName+"): ");
            String read = stdIn.readLine();
            botName = read.length() > 0 ? read : botName;
        } catch (IOException e) {
            System.err.println("Unable to get a new name, continuing with " + botName);
        }

        // connect to server
        try (Socket clientSocket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
            // send bot name
            out.println(botName);
            // now good things come to pass
            System.out.println("Wait a little for a map to start...");
            while (true) {
                // reading from server
                String serverReturned = in.readLine();
                if (serverReturned == null) {
                    // server lost
                    break;
                }
                // show what the server sent
                System.out.println(serverReturned);
                if (serverReturned.startsWith("Found")) {
                    String[] splitted = serverReturned.split(" ");
                    int foundGold = Integer.parseInt(splitted[1]);
                    int treasuresLeft = Integer.parseInt(splitted[3]);
                    
                    // note : if treasuresLeft is 0, it means that you will soon be in another map
                    // foundGold can be 0 if the map is finished
                    
                    // treasure found. Next line is the good one
                    serverReturned = in.readLine();
                    if (serverReturned == null) {
                        // server lost
                        break;
                    }
                    // show what the server sent
                    System.out.println(serverReturned);
                }
                // reading what the server returns:
                String[] splitted = serverReturned.split(" ");
                int X = Integer.parseInt(splitted[0]);
                int Y = Integer.parseInt(splitted[1]);
                String pop = splitted[2];
                double intensity = Double.parseDouble(splitted[3]);
                
                // you may also parse CALL response (playerName X Y)
                // or EVAL (treasure1 treasure2...)
                
                // now compute an action
                String[] actions = new String[] {"LEFT", "RIGHT", "UP", "DOWN"};
                String userInput = actions[(int) (Math.random()*actions.length)];
                
                // and send it
                System.out.println(userInput);
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
