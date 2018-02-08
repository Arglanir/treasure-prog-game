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
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.isae.mae.ss.sockets.treasures.server.GameMap.PlayerAction;

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
            name = "someone";
            // create buffered streams
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
                // read the name as first line
                name = in.readLine();
                Thread.currentThread().setName("Server-Thread-For-" + name);
                System.out.println(name + " is connected on " + clientSocket.getRemoteSocketAddress());
                Player player = Player.get(name);
                GameMap current = GameMap.ALL_MAPS.get(player.onMap);
                if (current == null || player.onMap == null) {
                    synchronized (GameMap.class) {
                        // need to place him on a starting map
                        GameMap first = current = GameMap.createFromString("Compete 100\n" +
                                "      T      \n"+
                                "              \n"+
                                "              \n"+
                                "              \n"+
                                "  T   S   T  \n" +
                                "             \n"+
                                "              \n"+
                                "              \n"+
                                "      T       \n"
                        );
                        // get random first position
                        Random random = new Random();
                        int i = random.nextInt(first.spawnPoints.size());
                        first.playersMap.put(name, (Coordinates) first.spawnPoints.toArray()[i]);

                    }
                }
                // send some information
                String tosend = current.getDefaultLine(name);
                out.println(tosend);
                // main loop
                while (true) {
                    String saction = in.readLine();
                    PlayerAction action = new PlayerAction(name, saction);
                    current.actions.add(action);
                    String toreturn = action.perform(current, false);
                    out.println(toreturn);
                    if (toreturn.startsWith(GameMap.END_OF_MAP_TEMPLATE.substring(0, 5))
                            && toreturn.split("\n").length > 1) {
                        // treasure found and end of map - need to select new
                        // one

                    }
                }

            } catch (IOException|NumberFormatException e) {
                // problem?
                System.out.println(e + " for " + clientSocket.getRemoteSocketAddress() + " (" + name + ")");
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                // end of communication
                System.out
                        .println("End communication with " + clientSocket.getRemoteSocketAddress() + " (" + name + ")");
            }
        }

    }


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

    }

}
