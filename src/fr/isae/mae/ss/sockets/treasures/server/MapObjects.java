package fr.isae.mae.ss.sockets.treasures.server;

/** What can be represented */
public enum MapObjects {
    /** unknown */
    UNKNOWN("?"),
    /** spawn (for map files) */
    SPAWNPOINT("S"),
    /** a wall */
    WALL("#"),
    /** nothing, empty space */
    NOTHING("."),
    /** a treasure */
    TREASURE("T");
    /** The character to represent it */
    public String asChar;

    /** Constructor */
    private MapObjects(String ch) {
        asChar = ch;
    }
    
}