package uk.badamson.mc;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import uk.badamson.mc.simulation.Clock;
import uk.badamson.mc.simulation.ClockTest;

/**
 * <p>
 * Unit tests for the {@link Game} class.
 * </p>
 */
public class GameTest {

    public static void assertInvariants(Game game) {
        ObjectTest.assertInvariants(game);// inherited

        final Clock clock = game.getClock();
        assertNotNull("Not null, clock", clock);// guard

        ClockTest.assertInvariants(clock);
    }

    public static void assertInvariants(Game game1, Game game2) {
        ObjectTest.assertInvariants(game1, game2);// inherited
    }

    private static Game constructor() {
        final Game game = new Game();

        assertInvariants(game);

        return game;
    }

    @Test
    public void constructor_A() {
        constructor();
    }
}
