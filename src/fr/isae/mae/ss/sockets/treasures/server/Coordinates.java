package fr.isae.mae.ss.sockets.treasures.server;

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
    public Coordinates toDir(String... direction) {
        int nx = x;
        int ny = y;
        char dir = ' ';
        try {
            dir = direction[0].toUpperCase().charAt(0);
        } catch (IndexOutOfBoundsException e) {
            // empty string probably
        }
        switch (dir) {
        case 'U':
            ny--;
            break;
        case 'D':
            ny++;
            break;
        case 'L':
            nx--;
            break;
        case 'R':
            nx++;
            break;
        case 'T': // TRIGO will run also teleport... but with no coordinates.
                  // Pfiou.
            if (direction.length >= 3) {
                try {
                    nx = Integer.parseInt(direction[1]);
                    ny = Integer.parseInt(direction[2]);
                } catch (NumberFormatException e) {
                    // do nothing
                    nx = x;
                    ny = y;
                }
            }
            break;
        default:
            break;
        }
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
}