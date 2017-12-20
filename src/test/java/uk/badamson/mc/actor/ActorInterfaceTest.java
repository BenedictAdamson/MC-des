package uk.badamson.mc.actor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for classes that implement the {@link ActorInterface} interface
 */
public class ActorInterfaceTest {

    public static void assertInvariants(ActorInterface actorInterface) {
	final Actor actor = actorInterface.getActor();
	final MessageTransferInProgress transmissionInProgress = actorInterface.getTransmissionInProgress();
	final Message messageSofar = transmissionInProgress == null ? null : transmissionInProgress.getMessageSofar();
	final double messageSofarLength = messageSofar == null ? Double.NaN : messageSofar.getLength();
	final Message transmittingMessage = actorInterface.getTransmittingMessage();
	final double transmittingMessageLength = transmittingMessage == null ? Double.NaN
		: transmittingMessage.getLength();

	assertNotNull("Always have an actor", actor);// guard

	assertSame("This is the actor interface of the actor of this actor interface", actorInterface,
		actor.getActorInterface());
	assertEquals("This has a transmission in progress if, and only if, this has a transmitting message.",
		transmissionInProgress == null, transmittingMessage == null);
	assertTrue("If there is a transmission in progress, its medium is one of the media that the actor can use.",
		transmissionInProgress == null
			|| actorInterface.getMedia().contains(transmissionInProgress.getMedium()));
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
