/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures.server;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import fr.isae.mae.ss.sockets.treasures.server.GameMap.GameMapOptions;
import fr.isae.mae.ss.sockets.treasures.server.PlayerAction.ActionParseException;
import fr.isae.mae.ss.sockets.treasures.server.PlayerAction.ActionType;
import fr.isae.mae.ss.sockets.treasures.server.PlayerAction.PlayerParsedAction;
import fr.isae.mae.ss.sockets.treasures.server.ReturnedInfo.TreasureFound;

/**
 * This class controls a game
 * @author Cedric
 *
 */
public class GameController {
    /** The associated map */
    GameMap map;
    /** Replay mode: no notification, no score update... */
    boolean replay;
    
    long STUN_TIME_MS = 200;
    long STUN_COUNTDOWN_MS = 100;
    long FLASH_COUNTDOWN_MS = 100; 

    /**
     * Performs the given action on the map
     * 
     * @throws ActionParseException
     *             if the action cannot be parsed.
     */
    synchronized ReturnedInfo perform(PlayerAction action) throws ActionParseException {
        // store it
        if (!replay) {
            map.actions.add(action);
        }

        ReturnedInfo toreturn = new ReturnedInfo();

        if (map.treasures.size() == 0) {
            return new ReturnedInfo.TreasureFound();
        }
        Coordinates player = map.playersMap.get(action.name);
        PlayerParsedAction parsed = action.parse();
        // update position if correct
        if (parsed.type.isMovement) {
            if (map.enabledActions.get(parsed.type)) {
                // create new coordinates
                Coordinates newCoordinates = player.toDir(parsed);
                // check if coordinates are ok (not a wall...)
                if (map.areCoordinatesOk(newCoordinates)) {
                    map.playersMap.put(action.name, newCoordinates);
                    player = newCoordinates;
                }
            }
        }
        // check if treasure is found
        if (map.treasures.containsKey(player)) {
            // treasure found!
            int gain = map.treasures.get(player);
            if (!replay) {
                Player.ALL_PLAYERS.get(action.name).score.addAndGet(gain);
            }
            // remove the treasure
            map.treasures.remove(player);
            // change return to good type
            toreturn = new ReturnedInfo.TreasureFound();
            ((TreasureFound)toreturn).goldFound = gain;
            ((TreasureFound)toreturn).nbTreasuresLeft = (Boolean) map.options.get(GameMapOptions.SHARED) ? 0 : map.treasures.size();
        }
        // set default values
        map.fillDefaults(toreturn, player);
        
        // perform other actions
        if (map.enabledActions.get(parsed.type) || parsed.type == ActionType.HELP) {
            switch (parsed.type) {
            case EVAL:
                // return the list of treasures and content
                toreturn.moreInfo = String.join(" ", map.treasures.values().stream().map(i -> i.toString()).collect(Collectors.toList()));
                break;
            case TRIGO:
                // return the angle to the best intensity treasure
                Coordinates treasure = map.mostIntensityTreasure(player);
                toreturn.intensity = player.trigo(treasure);
                break;
            case FLASH:
                // the display will have radius 4
                toreturn.display = map.charsAroundPos(player, 4);
                break;
            case HELP:
            	// show available commands
            	Comparator<ActionType> comparator = Comparator.comparing(ActionType::ordinal);
            	toreturn.moreInfo = String.join(" ", map.enabledActions.entrySet().stream()
            			.filter(entry -> entry.getValue())
            			.map(entry -> entry.getKey())
            			.sorted(comparator)
            			.map(act -> act.name()).collect(Collectors.toList()));
            	
            	break;
            case STUN:
            	// stun all players around
            	AtomicInteger nbStunnedPlayers = new AtomicInteger();
            	map.playersMap.entrySet().stream()
            	.filter(entry -> !entry.getKey().equals(action.name))
            	.forEach(entry -> {
            		String otherplayername = entry.getKey();
            		Coordinates playerCoordinates = entry.getValue();
            		if (playerCoordinates.distance(player) >  3) {
            			return;
            		}
            		
            		Player otherplayer = Player.ALL_PLAYERS.get(otherplayername);
            		long nexttime = System.currentTimeMillis() + STUN_TIME_MS;
            		otherplayer.whenNextActivationPossible.put(ActionType.UP, nexttime);
            		otherplayer.whenNextActivationPossible.put(ActionType.DOWN, nexttime);
            		otherplayer.whenNextActivationPossible.put(ActionType.LEFT, nexttime);
            		otherplayer.whenNextActivationPossible.put(ActionType.RIGHT, nexttime);
            		nbStunnedPlayers.incrementAndGet();
            	});
            	toreturn.moreInfo = String.format("%s stunned players", nbStunnedPlayers.get());
            	// augment time for next stun
            	Player.ALL_PLAYERS.get(action.name).whenNextActivationPossible.put(ActionType.STUN, System.currentTimeMillis() + STUN_COUNTDOWN_MS);
            default:
                break;
            }
        }
        // return 
        
        return toreturn;
    }
}
