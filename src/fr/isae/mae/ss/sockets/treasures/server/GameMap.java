/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 * Represents a game map.
 * 
 * @author Cedric Mayer, 2018
 */
public class GameMap implements Cloneable {

    /** Maximum size of grid */
    public final static int MAXXY = 200;

    /** Map identifiers generator */
    final static AtomicInteger IDENTIFIER_GENERATOR = new AtomicInteger(0);

    /** Map of all created maps */
    public final static Map<Integer, GameMap> ALL_MAPS = new HashMap<>();

    public final static String END_OF_MAP_TEMPLATE = "Found %s gold. %s treasures left.";

    /** The intensity function */
    final static BiFunction<Integer, Double, Double> INTENSITY_FUNCTION = (quantity, distance) -> quantity / distance;

    /** Identifier of the map */
    final int identifier = IDENTIFIER_GENERATOR.incrementAndGet();

    /** Map of players to their coordinate */
    final Map<String, Coordinates> playersMap = new HashMap<>();

    /** Map of treasures */
    final Map<Coordinates, Integer> treasures = new HashMap<>();

    /** Real size of map */
    final int sizeX;
    /** Real size of map */
    final int sizeY;

    /** Set of walls */
    final Set<Coordinates> walls = new HashSet<>();

    /** Set of spawnPoints */
    final Set<Coordinates> spawnPoints = new HashSet<>();

    /**
     * Option of map: shared treasure. Every player share the same treasures.
     * Finding one goes to next map.
     */
    final boolean optionSharedTreasure;

    /** Constructor */
    public GameMap(boolean optionSharedTreasure, int sizeX, int sizeY,
            Collection<Coordinates> walls,
            Collection<Coordinates> spawn, Map<Coordinates, Integer> treasures) {
        super();
        this.optionSharedTreasure = optionSharedTreasure;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.walls.addAll(walls);
        this.spawnPoints.addAll(spawn);
        this.treasures.putAll(treasures);
        ALL_MAPS.put(identifier, this);
    }

    /*
     * [Shared|Compete] TreasureValue1 TreasureValue2...
     *  ....
     *  .#S.
     *  .S#. 
     *  ...T
     */
    /**
     * Create a GameMap given a String with the format above
     * 
     * @return a new map
     */
    public static GameMap createFromString(String map) {
        // initialize
        final Set<Coordinates> walls = new HashSet<>();
        final Set<Coordinates> spawnPoints = new HashSet<>();
        final Map<Coordinates, Integer> treasures = new HashMap<>();
        // split into lines
        String[] lines = map.split("\r?\n");
        if (lines[lines.length - 1].length() == 0) {
            // remove last empty line
            lines = Arrays.copyOf(lines, lines.length - 1);
        }
        // split first line
        String[] firstLine = lines[0].split("\\s+");
        boolean optionSharedTreasure = firstLine[0].toUpperCase().charAt(0) == 'S';
        // get size of map
        int sizeY = lines.length - 1;
        int sizeX = lines[lines.length - 1].length();
        int nextTreasureValueIndex = 1;
        int treasureValue = 100;
        // read each cell of map
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                Coordinates c = new Coordinates(x, y);
                try {
                    switch (lines[y + 1].charAt(x)) {
                    case 'T':
                        if (nextTreasureValueIndex < firstLine.length) {
                            treasureValue = Integer.parseInt(firstLine[nextTreasureValueIndex]);
                            nextTreasureValueIndex++;
                        }
                        treasures.put(c, treasureValue);
                        break;
                    case '#':
                        walls.add(c);
                        break;
                    case 'S':
                        spawnPoints.add(c);
                        break;

                    default:
                        break;
                    }
                } catch (IndexOutOfBoundsException e) {
                    // do nothing, a line has not enough element: consider it
                    // empty
                }
            }
        }
        
        return new GameMap(optionSharedTreasure, sizeX, sizeY, walls,
                spawnPoints, treasures);
    }

    /**
     * Check the coordinates
     * 
     * @param coords
     *            The coordinates to check
     * @return <code>true</code> if the coordinates represents an empty space
     */
    public boolean areCoordinatesOk(Coordinates coords) {
        // check with size
        boolean posOk = coords.x >= 0 && coords.y >= 0 && coords.x < MAXXY && coords.y < MAXXY;
        // check with wall
        posOk &= !walls.contains(coords);
        // return
        return posOk;
    }

    /** Computes the intensity */
    public Double computeIntensity(Coordinates coords) {
        return treasures.entrySet().stream()
                .map(entry -> INTENSITY_FUNCTION.apply(entry.getValue(), entry.getKey().distance(coords)))
                .max(Double::compare).get();
    }

    /** What character represents the given position? */
    public MapObjects mapElementAtPos(Coordinates coordinates) {
        if (!areCoordinatesOk(coordinates)) {
            return MapObjects.WALL;
        }
        if (treasures.containsKey(coordinates)) {
            return MapObjects.TREASURE;
        }
        return MapObjects.NOTHING;
    }

    /** Return the chars to display around the position */
    public String charsAroundPos(Coordinates coordinates, int radius) {
        StringBuilder builder = new StringBuilder();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                builder.append(mapElementAtPos(new Coordinates(coordinates.x + dx, coordinates.y + dy)).asChar);
            }
        }
        return builder.toString();
    }

    /** Return the default line */
    public String getDefaultLine(String playerName) {
        Coordinates player = playersMap.get(playerName);
        String posline = player.x + " " + player.y + " " + charsAroundPos(player, 1) + " " + computeIntensity(player);
        return posline;
    }

    /**
     * A PlayerAction represents an action by a player, useful in order to redo
     * a game.
     */
    public static class PlayerAction {
        /** When the action has been created */
        final Long when = System.currentTimeMillis();
        /** name of player doing the action */
        final String name;

        /** The action sent */
        final String action;

        /** Constructor */
        public PlayerAction(String name, String action) {
            super();
            this.name = name;
            this.action = action;
        }

        /**
         * Performs an action
         * 
         * @param map
         *            The map to work on
         * @param replay
         *            Boolean indicating the replay (if <code>true</code>, no
         *            player object is updated)
         */
        String perform(GameMap map, boolean replay) {
            String toreturn = "";

            if (map.treasures.size() == 0) {
                // no more treasure, try another map please
                return String.format(END_OF_MAP_TEMPLATE, 0, 0);
            }

            synchronized (map) {
                Coordinates player = map.playersMap.get(name);
                String[] actionLine = action.split("\\s+");
                // maybe update position
                Coordinates newCoordinates = player.toDir(actionLine);
                if (map.areCoordinatesOk(newCoordinates)) {
                    map.playersMap.put(name, newCoordinates);
                    player = newCoordinates;
                }
                if (map.treasures.containsKey(player)) {
                    // treasure found!
                    int gain = map.treasures.get(player);
                    if (!replay) {
                        Player.ALL_PLAYERS.get(name).score.addAndGet(gain);
                        map.treasures.remove(player);
                    }
                    toreturn = String.format(END_OF_MAP_TEMPLATE, gain,
                            map.optionSharedTreasure ? 0 : map.treasures.size());
                    if (map.optionSharedTreasure || map.treasures.size() == 0) {
                        // end of map
                        return toreturn;
                    } else {
                        toreturn += "\n";
                    }
                }
                // perform other actions
                // TODO
                // toreturn
                String posline = map.getDefaultLine(name);
                toreturn += posline;
            }
            return toreturn;
        }
    }

    /** List that stores every action on the map, in order */
    final List<PlayerAction> actions = new LinkedList<>();

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
