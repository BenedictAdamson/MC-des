package uk.badamson.mc.actor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import uk.badamson.mc.actor.medium.Medium;
import uk.badamson.mc.actor.message.Message;

/**
 * Unit tests for classes that implement the {@link ActorInterface} interface
 */
public class ActorInterfaceTest {

    public static void assertInvariants(ActorInterface actorInterface) {
	final Actor actor = actorInterface.getActor();
	final Set<Medium> media = actorInterface.getMedia();
	final MessageTransferInProgress transmissionInProgress = actorInterface.getTransmissionInProgress();
	final Message transmittingMessage = actorInterface.getTransmittingMessage();
	final Set<MessageTransferInProgress> messagesBeingReceived = actorInterface.getMessagesBeingReceived();

	final Message messageSofar = transmissionInProgress == null ? null : transmissionInProgress.getMessageSofar();
	final double messageSofarLength = messageSofar == null ? Double.NaN : messageSofar.getInformationContent();
	final double transmittingMessageLength = transmittingMessage == null ? Double.NaN
		: transmittingMessage.getInformationContent();

	assertNotNull("Always have an actor", actor);// guard
	assertNotNull("Always have a set of media.", media);// guard
	assertNotNull("Always have a set of messages being received.", messagesBeingReceived);// guard

	assertSame("This is the actor interface of the actor of this actor interface", actorInterface,
		actor.getActorInterface());

	for (Medium medium : media) {
	    assertNotNull("The set of media does not contain a null element.", medium);
	}

	assertEquals("This has a transmission in progress if, and only if, this has a transmitting message.",
		transmissionInProgress == null, transmittingMessage == null);
	assertTrue("If there is a transmission in progress, its medium is one of the media that the actor can use.",
		transmissionInProgress == null || media.contains(transmissionInProgress.getMedium()));
	assertTrue(
		"If there is a transmission in progress, the length of the message sent so far is"
			+ "less than or equal to the length of the message being sent.",
		transmissionInProgress == null || messageSofarLength <= transmittingMessageLength);
	assertTrue(
		"If there is a transmission in progress, the length of the message sent so far equals"
			+ "the length of the message being sent"
			+ "at the instant that the message sent so far is the same as the message being sent.",
		transmissionInProgress == null || messageSofarLength < transmittingMessageLength
			|| messageSofar == transmittingMessage);

	for (MessageTransferInProgress messageTransfer : messagesBeingReceived) {
	    assertNotNull("The set of messages being received " + "does not contain a null element.", messageTransfer);// guard
	    MessageTransferInProgressTest.assertInvariants(messageTransfer);
	    assertTrue("The medium of each message being received is " + "one of the communication media of the actor.",
		    media.contains(messageTransfer.getMedium()));
	}
    }

    public static void assertInvariants(ActorInterface actorInterface1, ActorInterface actorInterface2) {
	// Do nothing
    }

    public static void beginSendingMessage(ActorInterface actorInterface, Medium medium, Message message)
	    throws MediumUnavailableException {
	try {
	    actorInterface.beginSendingMessage(medium, message);
	} catch (MediumUnavailableException e) {
	    assertInvariants(actorInterface);
	    throw e;
	}
	assertInvariants(actorInterface);
	final MessageTransferInProgress transmissionInProgress = actorInterface.getTransmissionInProgress();
	assertNotNull("This has a transmission in progress.", transmissionInProgress);// guard
	MessageTransferInProgressTest.assertInvariants(transmissionInProgress);

	assertSame("The medium of the transmission in progress is the given medium.", medium,
		transmissionInProgress.getMedium());
	assertNull("The message transmitted so far of the transmission in progress.",
		transmissionInProgress.getMessageSofar());
	assertSame("The given message is the current transmitting message.", message,
		actorInterface.getTransmittingMessage());
    }
}
