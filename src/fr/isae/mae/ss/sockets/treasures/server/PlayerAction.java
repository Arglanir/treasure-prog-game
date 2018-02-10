package fr.isae.mae.ss.sockets.treasures.server;

import java.util.ArrayList;
import java.util.List;

/**
 * A PlayerAction represents an action by a player.
 */
public class PlayerAction {
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
     * The type of action
     * @author Cedric
     */
    public static enum ActionType {
    	UP(true, false, false, false, 0),
    	DOWN(true, false, false, false, 0),
    	LEFT(true, false, false, false, 0),
    	RIGHT(true, false, false, false, 0),
    	TELEPORT(true, false, false, false, 2),
    	CALL(false, false, false, true, 0),
    	FLASH(false, false, true, false, 0),
    	STUN(false, false, false, false, 0),
    	TRIGO(false, true, false, false, 0),
    	EVAL(false, true, false, false, 0),
    	;
    	/** a movement action */
    	final boolean isMovement;
    	/** a change intensity */
		final boolean changeIntensity;
    	/** a change of display */
		final boolean changeDisplay;
		/** some info will be provided after the result */
		final boolean infoAfter;
		/** number of arguments */
		final int nbArgs;
		/** Constructor */
    	private ActionType(boolean movement, boolean changeIntensity, boolean changeDisplay, boolean infoAfter, int nbArgs) {
			this.isMovement = movement;
			this.changeIntensity = changeIntensity;
			this.changeDisplay = changeDisplay;
			this.infoAfter = infoAfter;
			this.nbArgs = nbArgs;
		}
    }
    /** Parse the current action */
	public PlayerParsedAction parse() throws ActionParseException {
		// split line
		String[] actionLine = action.split("\\s+");
		// get action type
		ActionType type;
		try {
			type = ActionType.valueOf(actionLine[0].toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new ActionParseException();
		}
		// prepare parsed argument actions
		StringBuilder commentsBuilder = new StringBuilder();
		List<String> arguments = new ArrayList<>(type.nbArgs);
		// store every argument to either arguments or commentsBuilder
		for (int i = 1; i < actionLine.length; i++) {
			if (arguments.size() < type.nbArgs) {
				arguments.add(actionLine[i]);
			} else {
				if (commentsBuilder.length() > 0) {
					commentsBuilder.append(' ');
				}
				commentsBuilder.append(actionLine[i]);
			}
		}
		if (arguments.size() < type.nbArgs) {
			throw new ActionParseException();
		}
		// create parsed action
		return new PlayerParsedAction(type, arguments, commentsBuilder.toString());
	}
    
    public static class ActionParseException extends Exception {

		/**
		 * generated
		 */
		private static final long serialVersionUID = 5469041399353922244L;
    	
    	
    }
    /** Class representing a parsed action, created by {@link PlayerAction#parse()} */
    public class PlayerParsedAction {
    	final ActionType type;
    	final List<String> arguments;
    	final String comments;
		/**
		 * constructor
		 */
		private PlayerParsedAction(ActionType type, List<String> arguments, String comments) {
			super();
			this.type = type;
			this.arguments = arguments;
			this.comments = comments;
		}
    }
}