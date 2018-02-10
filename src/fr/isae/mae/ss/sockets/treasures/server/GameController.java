/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures.server;

import fr.isae.mae.ss.sockets.treasures.server.GameMap.GameMapOptions;
import fr.isae.mae.ss.sockets.treasures.server.PlayerAction.ActionParseException;
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
	
	/** Performs the given action on the map 
	 * @throws ActionParseException if the action cannot be parsed. */
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
        	// create new coordinates
            Coordinates newCoordinates = player.toDir(parsed);
            // check if coordinates are ok (not a wall...)
            if (map.areCoordinatesOk(newCoordinates)) {
                map.playersMap.put(action.name, newCoordinates);
                player = newCoordinates;
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
        
        // return 
        
        return toreturn;
	}
}
