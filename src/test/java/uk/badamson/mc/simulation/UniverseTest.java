package uk.badamson.mc.simulation;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the {@link Universe} class.
 * </p>
 */
public class UniverseTest {

    public static void assertInvariants(Universe universe) {
        ObjectTest.assertInvariants(universe);// inherited
    }

    public static void assertInvariants(Universe universe1, Universe universe2) {
        ObjectTest.assertInvariants(universe1, universe2);// inherited
    }

    @Test
    public void constructor() {
        final Universe universe = new Universe();

        assertInvariants(universe);
    }

}
