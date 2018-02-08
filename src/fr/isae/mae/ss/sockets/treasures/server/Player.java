/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Player class
 * @author Cedric Mayer, 2018
 */
public class Player {

    /** Name of the player */
    final String name;
    
    /** Number of maps finished (0 means no map) */
    final AtomicInteger maps = new AtomicInteger(0);

    /**
     * Flag indicating if the player is connected, and on which map.
     */
    volatile Integer onMap = null;

    /** Score of the player */
    final AtomicInteger score = new AtomicInteger(0);

    /** Map of all players */
    final static Map<String, Player> ALL_PLAYERS = new HashMap<>();

    /**
     * Constructor of Player
     * 
     * @param name
     */
    private Player(String name) {
        super();
        this.name = name;
    }

    /**
     * Find a player name
     * 
     * @param name
     *            The name of a player
     * @return The player
     */
    public synchronized static Player get(String name) {
        Player toreturn = ALL_PLAYERS.get(name);
        if (toreturn == null) {
            ALL_PLAYERS.put(name, toreturn = new Player(name));
        }
        return toreturn;
    }

}
