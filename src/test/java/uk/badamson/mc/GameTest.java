package uk.badamson.mc;

import org.junit.Test;

/**
 * <p>
 * Unit tests for the {@link Game} class.
 * </p>
 */
public class GameTest {

    public static void assertInvariants(Game game) {
        ObjectTest.assertInvariants(game);// inherited
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
