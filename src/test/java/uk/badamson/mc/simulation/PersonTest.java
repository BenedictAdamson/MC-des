package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Collections;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.actor.ActorInterfaceTest;
import uk.badamson.mc.actor.ActorTest;
import uk.badamson.mc.actor.MediumUnavailableException;
import uk.badamson.mc.actor.MessageTransferInProgress;
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

    public static void assertInvariants(Person person) {
        ObjectTest.assertInvariants(person);// inherited
        ActorTest.assertInvariants(person);// inherited
        ActorInterfaceTest.assertInvariants(person);// inherited

        assertSame("This actor is its own actor interface.", person, person.getActorInterface());
        assertSame("This actor interface is its own actor.", person, person.getActor());
    }

    public static void assertInvariants(Person person1, Person person2) {
        ObjectTest.assertInvariants(person1, person2);// inherited
        ActorTest.assertInvariants(person1, person2);// inherited
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

    private static void beginSendingMessage_default(SimpleDirectCommand message) {
        final Person person = new Person();
        final Medium medium = HandSignals.INSTANCE;
        try {
            ActorInterfaceTest.beginSendingMessage(person, medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }
        assertInvariants(person);
    }

    private static void beginSendingMessage_default(SimpleStatement message) {
        final Person person = new Person();
        final Medium medium = HandSignals.INSTANCE;
        try {
            ActorInterfaceTest.beginSendingMessage(person, medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }
        assertInvariants(person);
    }

    private static Person constructor() {
        final Person person = new Person();

        assertInvariants(person);
        assertEquals("The media through which this actor can send messages consists of hand signals}.",
                Collections.singleton(HandSignals.INSTANCE), person.getMedia());
        assertEquals("This actor is receiving no messages.", Collections.EMPTY_SET, person.getMessagesBeingReceived());
        assertNull("This actor is not transmitting} a message.", person.getTransmissionInProgress());

        return person;
    }

    public static void tellBeginReceivingMessage(Person person, MessageTransferInProgress receptionStarted) {
        ActorTest.tellBeginReceivingMessage(person, receptionStarted);// inherited

        assertInvariants(person);
    }

    public static void tellMessageReceptionProgress(Person person, Message previousMessageSoFar,
            MessageTransferInProgress messageBeingReceived) {
        ActorTest.tellMessageReceptionProgress(person, previousMessageSoFar, messageBeingReceived);// inherited
        assertInvariants(person);
    }

    public static void tellMessageSendingEnded(Person person, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        ActorTest.tellMessageSendingEnded(person, transmissionProgress, fullMessage);// inherited
        assertInvariants(person);
    }

    public static void tellMessageTransmissionProgress(Person person, Message previousMessageSoFar) {
        ActorTest.tellMessageTransmissionProgress(person, previousMessageSoFar);// inherited
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
    public void constructor_0() {
        constructor();
    }
}
