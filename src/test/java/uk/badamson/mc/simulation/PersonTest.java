package uk.badamson.mc.simulation;

import static org.junit.Assert.assertSame;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.actor.ActorInterfaceTest;
import uk.badamson.mc.actor.ActorTest;
import uk.badamson.mc.actor.MediumUnavailableException;
import uk.badamson.mc.actor.MessageTransferInProgress;
import uk.badamson.mc.actor.medium.Medium;
import uk.badamson.mc.actor.message.Message;

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

    private static Person constructor() {
	final Person person = new Person();

	assertInvariants(person);

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
    public void constructor_0() {
	constructor();
    }
}
