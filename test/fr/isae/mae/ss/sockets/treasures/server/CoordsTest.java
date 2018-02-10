/**
 * 
 */
package fr.isae.mae.ss.sockets.treasures.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import fr.isae.mae.ss.sockets.treasures.server.PlayerAction.ActionParseException;

/**
 * CoordsTest class
 * @author Cedric Mayer, 2018
 */
public class CoordsTest {

    @Test
    public void testVect() {
        Coordinates A = new Coordinates(5, 6);
        Coordinates B = new Coordinates(1, 10);
        Coordinates AB = A.vectorTo(B);
        assertEquals(4, AB.y);
        assertEquals(-4, AB.x);
        assertEquals(Math.sqrt(16 * 2), A.distance(B), 1e-15);
        assertEquals(Math.sqrt(16 * 2), AB.distanceTo0(), 1e-15);
    }

    @Test
    public void testInMap() throws ActionParseException {
        Map<Coordinates, String> map = new HashMap<>();
        Coordinates A = new Coordinates(5, 6);
        Coordinates B = new Coordinates(1, 10);
        Coordinates C = new Coordinates(5, 6);
        String totest = "hello";
        map.put(A, totest);
        map.put(B, "another");
        assertTrue(C != A);
        assertEquals(totest, map.get(C));
        assertEquals(C, A);
        assertEquals(A, A);
        assertFalse(B.equals(A));
        assertFalse(B.equals(B.toDir(t("DOWN"))));
        assertFalse(B.equals(null));
        assertFalse(B.equals(new Object()));
        assertFalse(totest.equals(map.get(B)));
    }
    
    private PlayerAction.PlayerParsedAction t(String... action) throws ActionParseException {
    	return new PlayerAction(null, String.join(" ", action)).parse();
    }

    @Test
    public void testDir() throws ActionParseException {
        Coordinates A = new Coordinates(5, 6);
        assertEquals(new Coordinates(5, 5), A.toDir(t("UP")));
        assertEquals(new Coordinates(5, 7), A.toDir(t("DOWN")));
        assertEquals(new Coordinates(4, 6), A.toDir(t("LEFT")));
        assertEquals(new Coordinates(6, 6), A.toDir(t("RIGHT")));
        //assertEquals(new Coordinates(5, 6), A.toDir(t("TELEPORT")));
        //assertEquals(new Coordinates(5, 6), A.toDir(t("NONE")));
        assertEquals(new Coordinates(5, 6), A.toDir(t("TELEPORT", "11", "sbouf")));
        assertEquals(new Coordinates(11, 12), A.toDir(t("TELEPORT", "11", "12")));
    }
}
