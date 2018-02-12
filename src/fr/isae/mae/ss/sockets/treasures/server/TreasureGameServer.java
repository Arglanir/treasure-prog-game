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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
                player.connectedThread = Thread.currentThread();
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
            } catch (NullPointerException e) {
                // Problem of map
                System.err.println("No more map for player " + name);
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                // end of communication
                System.out.println("End communication with " + clientSocket.getRemoteSocketAddress() + " (" + name + ")");
                if (player != null)
                    player.connectedThread = null;
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
            portNumber = 1256;
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
        // the different server commands
        Map<String, Consumer<List<String>>> serverCommands = new HashMap<>();
        // getting list of players
        serverCommands.put("who",
                sl -> {
                    AtomicInteger nbconnected = new AtomicInteger();
                    Player.ALL_PLAYERS.values().forEach(player -> {
                        nbconnected.addAndGet(player.connectedThread != null ? 1 : 0);
                        System.out.println(String.format("%s: c=%s m=%s s=%s", player.name, player.connectedThread != null, player.maps,
                                player.score));
                    });
                    System.out.println(Player.ALL_PLAYERS.size() + " players, " + nbconnected.get() + " connected.");
                });
        // help command
        serverCommands.put("help",
                sl -> serverCommands.keySet().forEach(System.out::println));
        // reload list of maps
        serverCommands.put("reload", sl -> provider.reload());
        // remove a player
        serverCommands.put("remove",
                sl -> {
                    try {
                        String toremove = sl.get(1);
                        Player.ALL_PLAYERS.remove(toremove);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.err.println("Please give the name of the player...");
                    }
                });
        // remove a player
        serverCommands.put("disconnect", sl -> {
            try {
                String toremove = sl.get(1);
                Player.ALL_PLAYERS.get(toremove).connectedThread.interrupt();
            } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
                System.err.println("Please give the name of the player...");
            }
        });
        
        // reading for stdin for commands
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = stdIn.readLine()) != null) {
            List<String> arguments = Arrays.asList(line.split("\\s+"));
            Consumer<List<String>> torun = serverCommands.get(line.split("\\s+")[0].toLowerCase());
            if (torun != null) {
                torun.accept(arguments);
            }
        }

    }

}
