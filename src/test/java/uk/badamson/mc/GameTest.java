package uk.badamson.mc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import uk.badamson.mc.actor.ActorInterface;
import uk.badamson.mc.simulation.Clock;
import uk.badamson.mc.simulation.ClockTest;
import uk.badamson.mc.simulation.Person;
import uk.badamson.mc.simulation.PersonTest;

/**
 * <p>
 * Unit tests for the {@link Game} class.
 * </p>
 */
public class GameTest {

    public static void assertInvariants(Game game) {
        ObjectTest.assertInvariants(game);// inherited

        final Clock clock = game.getClock();
        final Set<Person> persons = game.getPersons();
        final ActorInterface playerActorInterface = game.getPlayerActorInterface();

        assertNotNull("Not null, clock", clock);// guard
        assertNotNull("Always have a set of persons.", persons);// guard

        ClockTest.assertInvariants(clock);
        for (Person person : persons) {
            assertNotNull("The set of persons does not have a null element.", person);// guard
            PersonTest.assertInvariants(person);
        }
        assertTrue("If the player actor interface is not null, it is one of the simulated persons of this game.",
                playerActorInterface == null || persons.contains(playerActorInterface));
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
