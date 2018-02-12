/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures.server;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides maps to play on
 * 
 * @author Cedric
 */
public class MapProvider {
    private static class WaitingPlayerInfo {
        // name of player
        String name;
        // gain of player (if need to choose another map
        Integer gain;
        // the map they are waiting on
        String bestMap;

        /**
         * @param name
         * @param gain
         * @param bestMap
         */
        private WaitingPlayerInfo(String name, Integer gain, String bestMap) {
            super();
            this.name = name;
            this.gain = gain;
            this.bestMap = bestMap;
        }
    }

    /** available maps to the string content */
    Map<String, String> mapName2content = new HashMap<>();
    /** players that are waiting with their gain */
    List<WaitingPlayerInfo> waitingPlayers = new LinkedList<>();

    final static long MAXIMUM_WAIT = 10000;// 10s

    // when a map is opened, the following are filled:
    /** Set of players that can play on the given map */
    Set<String> nextMapIsForTheNextPlayers = new HashSet<>();
    /** The map they can play on */
    GameMap openedMap = null;

    /** The player capacity of the server. Don't wait for more people. */
    private int playerCap;

    /** Activated options */
    String commonOptionLine;

    /** The pattern on file names */
    final static Pattern PATTERN_MAP_FILE_NAME = Pattern.compile("(\\d+)-(\\d+)-(\\d+)-(\\d+)-.*");
    final static int GROUP_MIN_GAIN = 1;
    final static int GROUP_MAX_GAIN = 2;
    final static int GROUP_MIN_PLAYERS = 3;
    final static int GROUP_MAX_PLAYERS = 4;

    /** Constructor that loads every map in memory (not parsed yet) */
    public MapProvider(int playerCap) {
        this.playerCap = playerCap;
        reload();
    }

    /**
     * Reloads the list of maps
     */
    public synchronized void reload() {
        URL listurl = getClass().getResource("/maplist.txt");
        File file;
        try {
            file = new File(listurl.toURI());
        } catch (URISyntaxException e) {
            file = new File(listurl.getPath());
        }
        int counter = 0;
        if (file.exists()) { // real file found: we can read the folder directly
            System.out.println("Reading maps from " + file.getAbsoluteFile().getParent());
            for (File mapFile : file.getAbsoluteFile().getParentFile().listFiles()) {
                if (PATTERN_MAP_FILE_NAME.matcher(mapFile.getName()).matches()) {
                    try {
                        String content = new String(Files.readAllBytes(mapFile.toPath()));
                        GameMap.createFromString(content);
                        mapName2content.put(mapFile.getName(), content);
                        counter += 1;
                    } catch (Throwable e) {
                        System.err.println("Problem for " + mapFile);
                        e.printStackTrace();
                    }
                }
            }
        } else {
            // the file is in a jar: read it and get files
            // TODO
            throw new UnsupportedOperationException("Loading maps from a jar is not supported yet.");
        }
        System.out.println(counter + " maps loaded.");
    }

    /** {@link #wait(long)} that throws {@link RuntimeException} */
    private void waiti(long timeout) {
        try {
            wait();
        } catch (InterruptedException e) {
            // will only happen if the server is killed
            throw new RuntimeException(e);
        }
    }

    public synchronized GameMap provideMap(String player, int currentGain) {
        return provideMapMono(player, currentGain);
    }

    private synchronized GameMap provideMapMono(String player, int currentGain) {
        // select mono map according to gain
        String bestMap = null;
        int bestCriteria = Integer.MIN_VALUE;
        for (String mapString : mapName2content.keySet()) {
            Matcher matcher = parseMapName(mapString);
            // check if gain is ok
            if (Integer.parseInt(matcher.group(GROUP_MIN_GAIN)) > currentGain)
                continue;
            if (Integer.parseInt(matcher.group(GROUP_MAX_GAIN)) <= currentGain)
                continue;
            if (Integer.parseInt(matcher.group(GROUP_MIN_PLAYERS)) >= 2)
                continue;
            // ok, now we have a map with corresponding gain and mono player
            // find map with possible maximum maximum player
            int criteria = -Integer.parseInt(matcher.group(GROUP_MAX_GAIN));
            if (bestCriteria < criteria || (bestCriteria == criteria && new Random().nextBoolean())) {
                bestMap = mapString;
                bestCriteria = criteria;
            }
        }
        return startMapWithPlayers(player, bestMap, null);

    }

    private synchronized GameMap provideMapMulti(String player, int currentGain) {
        /*
         * Algorithm: try to find the map for the maximum min players with the
         * current gain if waitingPlayers has enough players with the right
         * level, start a new map with this player and other players, notify
         * them with notifyAll() otherwise wait some time When awaken, if the
         * awaited map is opened and we are the first in the waitingPlayers,
         * then return it. Otherwise start the best map with the current waiting
         * players
         */
        // wait for openedMap to be free
        while (openedMap != null) {
            waiti(0);
        }

        String bestMap = null;
        bestMap = findMapMaxPlayers(currentGain);
        if (bestMap == null) {
            // should never happen...
            // but in this case, let's take the best map without the gain
            bestMap = findMapMaxPlayersNoMax(currentGain);
        }
        Matcher matcher = parseMapName(bestMap);
        int maxPlayers = extractIntGroup(matcher, GROUP_MAX_PLAYERS);
        int minPlayers = extractIntGroup(matcher, GROUP_MIN_PLAYERS);
        if (maxPlayers > playerCap) {
            maxPlayers = playerCap;
        }
        // check if enough people waiting on the same map
        List<String> otherPlayers = whoWaitsForMap(bestMap);
        if (otherPlayers.size() + 1 >= maxPlayers) {
            // the map is full! let's play it
            return startMapWithPlayers(player, bestMap, otherPlayers);
        }
        // map not yet full. Let's wait a little.
        WaitingPlayerInfo myOwnInfo = new WaitingPlayerInfo(player, currentGain, bestMap);
        waitingPlayers.add(myOwnInfo);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < MAXIMUM_WAIT) {
            waiti(MAXIMUM_WAIT - System.currentTimeMillis() + start + 1);
            if (nextMapIsForTheNextPlayers.contains(player)) {
                // ok i can play
                nextMapIsForTheNextPlayers.remove(player);
                GameMap toreturn = openedMap;
                if (nextMapIsForTheNextPlayers.isEmpty()) {
                    openedMap = null; // clear it
                }
                // no need to renotify?
                return toreturn;
            }
        }
        // I have not been chosen yet, but I'm waiting for too long
        // check if min players are enough
        otherPlayers = whoWaitsForMap(bestMap); // i should be in the list
        if (otherPlayers.size() >= minPlayers) {
            // there are enough players! let's play it
            return startMapWithPlayers(null, bestMap, otherPlayers);
        }
        // not enough players though, try to start a map with other players
        waitingPlayers.remove(myOwnInfo);
        bestMap = null;
        int bestCrit = Integer.MIN_VALUE;
        for (WaitingPlayerInfo info : waitingPlayers) {
            matcher = parseMapName(info.bestMap);
            minPlayers = extractIntGroup(matcher, GROUP_MIN_PLAYERS);
            int nbwaiting = whoWaitsForMap(info.bestMap).size();
            if (minPlayers > nbwaiting)
                continue;
            // this map can be played. Find a one that I can play on.
            int crit = 0;
            if (extractIntGroup(matcher, GROUP_MAX_GAIN) >= currentGain) {
                crit += 1000; // otherwise I will not gain anything
            }
            if (extractIntGroup(matcher, GROUP_MIN_GAIN) > currentGain) {
                crit -= 500; // I may not have the level
            }
            crit += nbwaiting * 50;

            // store this one
            if (bestCrit < crit) {
                bestCrit = crit;
                bestMap = info.bestMap;
            }
        }
        if (bestMap != null) {
            // Go go go
            otherPlayers = whoWaitsForMap(bestMap); // i should be in the list
            return startMapWithPlayers(player, bestMap, otherPlayers);
        }
        // nothing. Try to find a map to do alone
        bestCrit = Integer.MIN_VALUE;
        for (String mapString : mapName2content.keySet()) {
            matcher = parseMapName(mapString);
            // check if nb players is ok
            if (Integer.parseInt(matcher.group(GROUP_MIN_PLAYERS)) > 1)
                continue;
            // ok, now we have a map that can be played alone
            int crit = 0;
            int mingain = Integer.parseInt(matcher.group(GROUP_MIN_GAIN)); // minimum
                                                                           // level
            if (mingain > currentGain)
                crit -= 500; // I may not have the level
            if (Integer.parseInt(matcher.group(GROUP_MAX_GAIN)) >= currentGain)
                // I can win something
                crit += 1000;
            crit += (currentGain - mingain) / 100;
            // find map with possible maximum maximum player
            if (bestCrit < crit) {
                bestCrit = crit;
                bestMap = mapString;
            }
        }
        if (bestMap != null) {
            // Go go go, but alone
            return startMapWithPlayers(player, bestMap, Collections.emptyList());
        }
        // end of game?
        throw new IllegalStateException("Impossible to find a map for player " + player + " with score " + currentGain);
    }

    /**
     * @param matcher
     * @param groupToExtract
     * @return
     */
    private int extractIntGroup(Matcher matcher, int groupToExtract) {
        return Integer.parseInt(matcher.group(groupToExtract));
    }

    /**
     * @param bestMap
     * @return
     */
    private List<String> whoWaitsForMap(String bestMap) {
        List<String> otherPlayers = new ArrayList<>(2 * playerCap);
        for (WaitingPlayerInfo info : waitingPlayers) { // I am not yet on it
            if (info.bestMap.equals(bestMap)) {
                otherPlayers.add(info.name);
            }
        }
        return otherPlayers;
    }

    /**
     * @param player
     * @param bestMap
     * @param otherPlayers
     * @return
     */
    private GameMap startMapWithPlayers(String player, String bestMap, List<String> otherPlayers) {
        // create map
        openedMap = GameMap.createFromString(mapName2content.get(bestMap));
        openedMap.applyOptions(commonOptionLine);
        if (player != null) {
            openedMap.addPlayer(player);
        }
        if (otherPlayers != null) {
            if (player != null) {
                // don't add the player in the waiting list
                otherPlayers.remove(player);
            }
            otherPlayers.forEach(openedMap::addPlayer);
            nextMapIsForTheNextPlayers.addAll(otherPlayers);
        }
        notifyAll();
        GameMap toreturn = openedMap;
        if (nextMapIsForTheNextPlayers.size() == 0) {
            openedMap = null; // no new player awaited
        }
        System.out.println("Map " + bestMap + " started for " + player + " " + otherPlayers);
        return toreturn;
    }

    /**
     * @param currentGain
     * @return
     */
    private String findMapMaxPlayers(int currentGain) {
        String bestMap = null;
        int maxMaxPlayers = Integer.MIN_VALUE;
        for (String mapString : mapName2content.keySet()) {
            Matcher matcher = parseMapName(mapString);
            // check if gain is ok
            if (Integer.parseInt(matcher.group(GROUP_MIN_GAIN)) > currentGain)
                continue;
            if (Integer.parseInt(matcher.group(GROUP_MAX_GAIN)) < currentGain)
                continue;
            // ok, now we have a map with corresponding gain
            // find map with possible maximum maximum player
            int maxPlayers = Integer.parseInt(matcher.group(GROUP_MAX_PLAYERS));
            if (maxMaxPlayers < maxPlayers) {
                bestMap = mapString;
                maxMaxPlayers = maxPlayers;
            }
        }
        return bestMap;
    }

    /**
     * @param mapString
     * @return
     */
    private Matcher parseMapName(String mapString) {
        Matcher matcher = PATTERN_MAP_FILE_NAME.matcher(mapString);
        matcher.matches();
        return matcher;
    }

    /**
     * @param currentGain
     * @return
     */
    private String findMapMaxPlayersNoMax(int currentGain) {
        String bestMap = null;
        int maxMaxPlayers = Integer.MIN_VALUE;
        for (String mapString : mapName2content.keySet()) {
            Matcher matcher = parseMapName(mapString);
            // check if gain is ok
            if (Integer.parseInt(matcher.group(GROUP_MIN_GAIN)) > currentGain)
                continue;
            // if (Integer.parseInt(matcher.group(GROUP_MAX_GAIN)) <
            // currentGain) continue;
            // ok, now we have a map with corresponding gain
            // find map with maximum maximum player
            int maxPlayers = Integer.parseInt(matcher.group(GROUP_MAX_PLAYERS));
            if (maxMaxPlayers < maxPlayers) {
                bestMap = mapString;
                maxMaxPlayers = maxPlayers;
            }
        }
        return bestMap;
    }
}
