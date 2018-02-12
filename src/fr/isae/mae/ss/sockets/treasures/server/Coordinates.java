package fr.isae.mae.ss.sockets.treasures.server;

import fr.isae.mae.ss.sockets.treasures.server.PlayerAction.PlayerParsedAction;

/** Class representing coordinates (point or vector) */
public class Coordinates {
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Coordinates other = (Coordinates) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        return true;
    }

    /** Absissa */
    final int x;
    /** Ordinate */
    final int y;

    public Coordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** create coordinates to the given direction */
    public Coordinates toDir(PlayerParsedAction parsed) {
        int nx = x;
        int ny = y;
        //switch on type
        switch (parsed.type) {
        case UP:
            ny--;
            break;
        case DOWN:
            ny++;
            break;
        case LEFT:
            nx--;
            break;
        case RIGHT:
            nx++;
            break;
        case TELEPORT:
            try {
                nx = Integer.parseInt(parsed.arguments.get(0));
                ny = Integer.parseInt(parsed.arguments.get(1));
            } catch (NumberFormatException e) {
                // do nothing
                nx = x;
                ny = y;
            }
            break;
        default:
            break;
        }
        // return new coordinates
        return new Coordinates(nx, ny);
    }

    /** Creates a vector to other coordinates */
    public Coordinates vectorTo(Coordinates coords) {
        return new Coordinates(coords.x - x, coords.y - y);
    }

    /** Get the distance to 0 */
    public double distanceTo0() {
        return Math.sqrt(x * x + y * y);
    }

    /** Distance to other point */
    public double distance(Coordinates coords) {
        return vectorTo(coords).distanceTo0();
    }

    /** Angle in degree to other point (0: to the left, 90: to the bottom) */
    public double trigo(Coordinates coords) {
        Coordinates vect = vectorTo(coords);
        double abs = vect.distanceTo0();
        double dx = vect.x / abs;
        double dy = vect.y / abs;
        return Math.toDegrees(Math.atan2(dy, dx));

    }
}