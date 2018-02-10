/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * Flag indicating if the player is connected
     */
    volatile boolean connected = false;

    /** Score of the player */
    final AtomicInteger score = new AtomicInteger(0);

    /** If the player can win gain on map */
    final AtomicBoolean canWin = new AtomicBoolean(true);

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
    
    public void incrementScore(int by) {
    	if (canWin.get()) score.addAndGet(by);
    }

    /**
     * Find or create a player
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
