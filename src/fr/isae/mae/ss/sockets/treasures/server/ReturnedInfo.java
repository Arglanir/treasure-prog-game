/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures.server;

/**
 * Class representing the data to send to clients.
 * @author Cedric
 */
public class ReturnedInfo {
    /** Template for end of map */
    public final static String END_OF_MAP_TEMPLATE = "Found %s gold. %s treasures left.";
    /** Template for normal information */
    public final static String NORMAL_TEMPLATE = "%s %s %s %s %s";

    /** Subclass when a treasure is found. */
    public static class TreasureFound extends ReturnedInfo {
        /** The found gold */
        int goldFound;
        /** number of treasures left */
        int nbTreasuresLeft; // if nbTreasuresLeft == 0, end of map

        @Override
        public String asMessageString() {
            String first = String.format(END_OF_MAP_TEMPLATE, goldFound, nbTreasuresLeft);
            if (nbTreasuresLeft > 0) {
                return first + "\n" + super.asMessageString();
            } else {
                return first;
            }
        }

        @Override
		public int foundGold() {
        	return goldFound;
        }
        
        @Override
        public boolean endOfMap() {
            return nbTreasuresLeft <= 0;
        }
    }

    /** Abscissa of position */
    int x;
    /** Ordinate of position */
    int y;
    /** The display around */
    String display;
    /** The intensity */
    Double intensity;
    /** more information */
    String moreInfo;

    /**
     * Indicates if the end of map has been reached.
     * 
     * @return <code>true</code> if the map is finished
     */
    public boolean endOfMap() {
        return false;
    }
    
    public int foundGold() {
    	return 0;
    }

    /**
     * @return The string to send to the client
     */
    public String asMessageString() {
        return String.format(NORMAL_TEMPLATE, x, y, display, intensity, moreInfo == null ? "" : moreInfo);
    }
    // You can write here a method to parse a String message
}
