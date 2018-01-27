package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.actor.AbstractActor;
import uk.badamson.mc.actor.Actor;
import uk.badamson.mc.actor.ActorInterfaceTest;
import uk.badamson.mc.actor.MediumUnavailableException;
import uk.badamson.mc.actor.medium.HandSignals;
import uk.badamson.mc.actor.medium.Medium;
import uk.badamson.mc.actor.message.Message;
import uk.badamson.mc.actor.message.SimpleDirectCommand;
import uk.badamson.mc.actor.message.SimpleStatement;

/**
 * <p>
 * Unit tests of the {@link Person} class.
 * </p>
 */
public class PersonTest {
    private static final long TIME_1 = ClockTest.TIME_1;
    private static final long TIME_2 = ClockTest.TIME_2;

    public static void assertInvariants(Person person) {
        ObjectTest.assertInvariants(person);// inherited
        ActorInterfaceTest.assertInvariants(person);// inherited

        assertNotNull("Not null, clock", person.getClock());
    }
    public static void assertInvariants(Person person1, Person person2) {
        ObjectTest.assertInvariants(person1, person2);// inherited
        ActorInterfaceTest.assertInvariants(person1, person2);// inherited
    }

    public static void beginSendingMessage(Person person, Medium medium, Message message)
            throws MediumUnavailableException {
        try {
            ActorInterfaceTest.beginSendingMessage(person, medium, message);// inherited
        } catch (MediumUnavailableException e) {
            assertInvariants(person);
            throw e;
        }
        assertInvariants(person);
    }

    private static Person constructor(Clock clock) {
        final Person person = new Person(clock);

        assertInvariants(person);
        assertSame("clock", clock, person.getClock());
        assertNull("actor", person.getActor());
        assertEquals("The media through which this actor can send messages consists of hand signals}.",
                Collections.singleton(HandSignals.INSTANCE), person.getMedia());
        assertEquals("This actor is receiving no messages.", Collections.EMPTY_SET, person.getMessagesBeingReceived());
        assertNull("This actor is not transmitting} a message.", person.getTransmissionInProgress());

        return person;
    }

    public static void setActor(Person person, Actor actor) {
        person.setActor(actor);

        assertInvariants(person);
        assertSame("actor", actor, person.getActor());
    }

    private Clock clock1;

    private Clock clock2;

    private void beginSendingMessage_default(SimpleDirectCommand message) {
        final Person person = new Person(clock1);
        final Medium medium = HandSignals.INSTANCE;
        try {
            ActorInterfaceTest.beginSendingMessage(person, medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }
        assertInvariants(person);
    }

    private void beginSendingMessage_default(SimpleStatement message) {
        final Person person = new Person(clock1);
        final Medium medium = HandSignals.INSTANCE;
        try {
            ActorInterfaceTest.beginSendingMessage(person, medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }
        assertInvariants(person);
    }

    @Test
    public void beginSendingMessage_defaultSimpleDirectCommands() {
        for (SimpleDirectCommand message : SimpleDirectCommand.values()) {
            beginSendingMessage_default(message);
        }
    }

    @Test
    public void beginSendingMessage_defaultSimpleStatements() {
        for (SimpleStatement message : SimpleStatement.values()) {
            beginSendingMessage_default(message);
        }
    }

    @Test
    public void constructor_A() {
        constructor(clock1);
    }

    @Test
    public void constructor_B() {
        constructor(clock2);
    }

    private void setActor() {
        final Person person = new Person(clock1);
        final Actor actor = new AbstractActor(person);

        setActor(person, actor);
    }

    @Test
    public void setActor_A() {
        setActor();
    }

    @Test
    public void setActor_B() {
        setActor();
    }

    @Before
    public void setup() {
        clock1 = new Clock(TimeUnit.MILLISECONDS, TIME_1);
        clock2 = new Clock(TimeUnit.MILLISECONDS, TIME_2);
    }
}
