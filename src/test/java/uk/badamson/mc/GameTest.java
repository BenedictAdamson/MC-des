package uk.badamson.mc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import uk.badamson.mc.mind.AbstractMind;
import uk.badamson.mc.mind.MediumUnavailableException;
import uk.badamson.mc.mind.Mind;
import uk.badamson.mc.mind.MindTest;
import uk.badamson.mc.mind.medium.HandSignals;
import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.Message;
import uk.badamson.mc.mind.message.SimpleDirectCommand;
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
        final Person playedPerson = game.getPlayedPerson();

        assertNotNull("Not null, clock", clock);// guard
        assertNotNull("Always have a set of persons.", persons);// guard

        ClockTest.assertInvariants(clock);
        for (Person person : persons) {
            assertNotNull("The set of persons does not have a null element.", person);// guard
            PersonTest.assertInvariants(person);
            assertSame("The clock of each person is the clock of this game.", clock, person.getClock());
        }
        assertTrue("If the played person is not null, it is one of the simulated persons of this game.",
                playedPerson == null || persons.contains(playedPerson));
    }

    public static void assertInvariants(Game game1, Game game2) {
        ObjectTest.assertInvariants(game1, game2);// inherited
    }

    private static Game constructor() {
        final Game game = new Game();

        assertInvariants(game);
        assertEquals("This game has no simulated persons.", Collections.EMPTY_SET, game.getPersons());

        return game;
    }

    public static final Person createPerson(Game game) {
        final Set<Person> persons0 = new HashSet<>(game.getPersons());

        final Person person = game.createPerson();

        assertInvariants(game);
        assertNotNull("Always creates a person.", person);// guard
        final Set<Person> persons = game.getPersons();
        PersonTest.assertInvariants(person);
        assertNull("The created person does not have an actor.", person.getPlayer());
        assertTrue("The created person is one of the persons of this game.", persons.contains(person));
        assertTrue("Does not remove any persons from this game.", persons.containsAll(persons0));
        assertEquals("Adds one person to this game.", persons0.size() + 1, persons.size());

        return person;
    }

    public static final void releaseControl(Game game) {
        final Person playedPerson0 = game.getPlayedPerson();

        game.releaseControl();

        assertInvariants(game);
        assertTrue("If this had a played person, the actor of that person becomes null.",
                playedPerson0 == null || playedPerson0.getPlayer() == null);
        assertNull("playedPerson", game.getPlayedPerson());
    }

    public static final void takeControl(Game game, Mind player, Person person) {
        game.takeControl(player, person);

        assertInvariants(game);
        MindTest.assertInvariants(player);
        PersonTest.assertInvariants(person);

        assertSame("playedPerson", person, game.getPlayedPerson());
        assertSame("The player of the given person becomes the given player.", player, person.getPlayer());
    }

    @Test
    public void constructor_A() {
        constructor();
    }

    @Test
    public void createPerson_1() {
        final Game game = new Game();

        createPerson(game);
    }

    @Test
    public void createPerson_2() {
        final Game game = new Game();
        game.createPerson();

        createPerson(game);
    }

    @Test
    public void personSendsMessage() {
        final Message message = SimpleDirectCommand.CHECK_MAP;
        final Game game = new Game();
        final Clock clock = game.getClock();
        final Person person = game.createPerson();
        final Medium medium = HandSignals.INSTANCE;
        final double informationInMessage = message.getInformationContent();
        final double transmissionTime = informationInMessage / medium.getTypicalTransmissionRate();
        try {
            person.beginSendingMessage(medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }

        clock.advanceSeconds(transmissionTime * 20);

        assertNull("No transmission in progress.", person.getTransmissionInProgress());
    }

    @Test
    public void releaseControl() {
        final Game game = new Game();
        final Person person = game.createPerson();
        final Mind player = new AbstractMind();
        game.takeControl(player, person);

        releaseControl(game);
        PersonTest.assertInvariants(person);
    }

    @Test
    public void releaseControl_noOp() {
        final Game game = new Game();

        releaseControl(game);
    }

    @Test
    public void takeControl_1() {
        final Game game = new Game();
        final Person person = game.createPerson();
        final Mind player = new AbstractMind();

        takeControl(game, player, person);
    }
}
