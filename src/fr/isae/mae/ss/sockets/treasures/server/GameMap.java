/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents a game map.
 * 
 * @author Cedric Mayer, 2018
 */
public class GameMap implements Cloneable {
    /** Game map options */
    public static enum GameMapOptions {
        /**
         * Indicates if the treasure is shared with every one. If one treasure
         * is found, the map is finished.
         */
        SHARED(false);
        /** default value */
        Object defaut;

        private GameMapOptions(Object def) {
            defaut = def;
        }
    }

    /** Maximum size of grid */
    public final static int MAXXY = 200;

    /** Map identifiers generator */
    final static AtomicInteger IDENTIFIER_GENERATOR = new AtomicInteger(0);

    /** Map of all created maps */
    public final static Map<Integer, GameMap> ALL_MAPS = new HashMap<>();

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
    final List<Coordinates> spawnPoints = new LinkedList<>();

    /** Options of the map */
    final Map<GameMapOptions, Object> options = new EnumMap<>(GameMapOptions.class);
    /** Options of the map */
    final Map<PlayerAction.ActionType, Boolean> enabledActions = new EnumMap<>(PlayerAction.ActionType.class);
    
    GameController controller = new GameController();

    /** Constructor */
    public GameMap(int sizeX, int sizeY,
            Collection<Coordinates> walls,
            Collection<Coordinates> spawn, Map<Coordinates, Integer> treasures) {
        super();
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.walls.addAll(walls);
        this.spawnPoints.addAll(spawn);
        this.treasures.putAll(treasures);
        ALL_MAPS.put(identifier, this);
        // fill map of options
        for (GameMapOptions option: GameMapOptions.values()) {
            options.put(option, option.defaut);
        }
        // fill map of enabledActions
        for (PlayerAction.ActionType actionType: PlayerAction.ActionType.values()) {
            enabledActions.put(actionType, actionType.ordinal() < 4);
        }
        controller.map = this;
    }
    
    /** Add a player to the game, at the first position, then rotate the positions */
    public void addPlayer(String player) {
        if (!playersMap.containsKey(player)) {
            Coordinates coords = spawnPoints.get(0);
            spawnPoints.add(spawnPoints.remove(0));
            playersMap.put(player, coords);
        }
    }

    /*
     * ActivatedOption...
     * TreasureValue1 TreasureValue2...
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
        List<String> lines = new ArrayList<>(Arrays.asList(map.split("\r?\n")));
        String optionLine = lines.remove(0);
        if (lines.get(lines.size()-1).length() == 0) {
            // remove last empty line
            lines.remove(lines.size()-1);
        }
        List<Integer> treasureLine = Arrays.stream(lines.remove(0).split("\\s+")).map(Integer::parseInt).collect(Collectors.toList());
        
        // get size of map
        int sizeY = lines.size();
        int sizeX = lines.stream().map(line -> line.length()).max(Integer::compare).get();
        if (treasureLine.size() == 0) {
            treasureLine = Collections.singletonList(100);
        }
        // read each cell of map
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                Coordinates c = new Coordinates(x, y);
                try {
                    switch (lines.get(y).charAt(x)) {
                    case 'T':
                        int treasureValue = treasureLine.get(0);
                        if (treasureLine.size() > 1) {
                            treasureLine.remove(0);
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
        // create map
        GameMap toreturn = new GameMap(sizeX, sizeY, walls,
                spawnPoints, treasures);
        // handle options
        for (String option: optionLine.split("\\s+")) {
            if (option.length() == 0)
                continue;
            // every action is authorized
            if ("allActions".equals(option)) {
                toreturn.enabledActions.forEach((action, enabled) -> toreturn.enabledActions.put(action, true));
                continue;
            }
            String[] optionValue = option.split("=");
            try { // try if it is a map option
                GameMapOptions realOption = GameMapOptions.valueOf(optionValue[0].toUpperCase());
                toreturn.options.put(realOption, optionValue.length == 1);
            } catch (IllegalArgumentException e) {
                // may be for an action
                try {
                    PlayerAction.ActionType actionType = PlayerAction.ActionType.valueOf(optionValue[0].toUpperCase());
                    toreturn.enabledActions.put(actionType, true);
                } catch (IllegalArgumentException e2) {
                    System.err.println("Unable to parse option " + option);
                }

            }

        }
        // randomize spawn points
        IntStream.range(0, new Random().nextInt(toreturn.spawnPoints.size()))
                .forEach(i -> toreturn.spawnPoints.add(toreturn.spawnPoints.remove(0)));
        return toreturn;
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
                .max(Double::compare).orElse(null);
    }

    /** Computes the intensity */
    public Coordinates mostIntensityTreasure(Coordinates coords) {
        Comparator<Entry<Coordinates, Integer>> comp = (o1, o2) -> {
            double io1 = INTENSITY_FUNCTION.apply(o1.getValue(), o1.getKey().distance(coords));
            double io2 = INTENSITY_FUNCTION.apply(o2.getValue(), o2.getKey().distance(coords));
            return Double.compare(io1, io2);
        };
        // apply this comparator
        Entry<Coordinates, Integer> entry = treasures.entrySet().stream().max(comp).orElse(null);
        Coordinates toreturn = new Coordinates(0, 0);
        if (entry != null) {
            toreturn = entry.getKey();
        }
        return toreturn;
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

    /** Fills the given {@link ReturnedInfo} with the data present at given {@link Coordinates} */
    public void fillDefaults(ReturnedInfo returned, Coordinates player) {
        returned.x = player.x;
        returned.y = player.y;
        returned.display = charsAroundPos(player, 1);
        returned.intensity = computeIntensity(player);
    }

    /** List that stores every action on the map, in order */
    final List<PlayerAction> actions = new LinkedList<>();

    @Override
    public GameMap clone() {
        // clone the map for a replay
        try {
            GameMap toreturn = (GameMap) super.clone();
            // add new controller in replay mode
            toreturn.controller = new GameController();
            toreturn.controller.replay = true;
            return toreturn;
        } catch (CloneNotSupportedException e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }
}
