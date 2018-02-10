/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.isae.mae.ss.sockets.treasures.server.PlayerAction.ActionParseException;

/**
 * 
 * 
 * @author Cedric Mayer, 2018
 */
public class TreasureGameServer {


    /**
     * ServerThread class, that handles the connection with a user.
     * 
     * @author Cedric Mayer, 2018
     */
    public static class ServerThread implements Runnable {
        /** The socket */
        private Socket clientSocket;

        /** Name of the user on this connection */
        private String name;

        /** Constructor */
        ServerThread(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Server-Thread-For-?");
            name = null;
            String lastAction = null;
            Player player = null;
            // create buffered streams
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
                // read the name as first line
                name = in.readLine().replaceAll("\\s", "");
                Thread.currentThread().setName("Server-Thread-For-" + name);
                System.out.println(name + " is connected on " + clientSocket.getRemoteSocketAddress());
                player = Player.get(name);
                player.connected = true;
                GameMap current = GameMap.ALL_MAPS.get(player.onMap);
                if (current == null || player.onMap == null) {
                	// may wait
                    current = provider.provideMap(name, player.score.get());
                    player.onMap = current.identifier;
                }
                // send some information
                ReturnedInfo returned = new ReturnedInfo();
                current.fillDefaults(returned, current.playersMap.get(name));
                out.println(returned.asMessageString());
                // main loop
                while (true) {
                    String saction = lastAction = in.readLine();
                    PlayerAction action = new PlayerAction(name, saction);
                    ReturnedInfo toreturn = current.controller.perform(action);
                    out.println(toreturn.asMessageString());
                    if (toreturn.endOfMap()) {
                        player.onMap = null;
                    	player.maps.incrementAndGet();
                        // treasure found and end of map - need to select new one
                    	// may wait
                        current = provider.provideMap(name, player.score.get());
                        player.onMap = current.identifier;
                        returned = new ReturnedInfo();
                        current.fillDefaults(returned, current.playersMap.get(name));
                        out.println(returned.asMessageString());
                    }
                }

            } catch (IOException|NumberFormatException|ActionParseException e) {
                // connection or message problem?
                System.err.println(e + " for " + clientSocket.getRemoteSocketAddress() + " (" + name + ") (last action: " + lastAction + ")");
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                // end of communication
                System.out.println("End communication with " + clientSocket.getRemoteSocketAddress() + " (" + name + ")");
                if (player != null) player.connected = false;
            }
        }

    }

    private static MapProvider provider;

    /**
     * Main method
     * 
     * @param args
     * @throws IOException
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException, IOException {
        final ExecutorService pool = Executors.newCachedThreadPool();
        final int portNumber;
        // helped by https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
        if (args.length != 1) {
            System.err.println("Usage: java " + TreasureGameServer.class.getName() + " <port number>");
            // System.exit(1);
            portNumber = 8765;
        } else {
            portNumber = Integer.parseInt(args[0]);
        }
        
        provider = new MapProvider(5);

        Thread serverListeningThread = new Thread("Server-Waiting-Thread") {
            public void run() {
                try (
                        // creating a sever socket
                        ServerSocket serverSocket = new ServerSocket(portNumber);) {
                    while (true) {
                        System.out.println("Waiting for a new connection on port " + portNumber + "...");
                        // waiting for a connexion
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("New connection from " + clientSocket.getRemoteSocketAddress());
                        // create a thread to handle the messages
                        pool.submit(new ServerThread(clientSocket));
                    }

                } catch (IOException e) {
                    System.out.println("Exception caught when trying to listen on port " + portNumber
                            + " or listening for a connection");
                    System.out.println(e.getMessage());
                }
            }
        };
        serverListeningThread.start();
        // reading for stdin for commands
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = stdIn.readLine()) != null) {
        	if (line.startsWith("who")) {
        		Player.ALL_PLAYERS.values().forEach(player -> System.out.println(String.format("%s: c=%s m=%s s=%s", player.name, player.connected, player.maps, player.score)));
        	} else if (line.startsWith("remove")) {
        		try {
        			String toremove = line.split("\\s+")[1];
        			Player.ALL_PLAYERS.remove(toremove);
        		} catch (ArrayIndexOutOfBoundsException e) {
					System.err.println("Please give the name of the player...");
				}
        		
        	}
        }

    }

}
