package uk.badamson.mc.actor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import uk.badamson.mc.actor.ActorInterface.MessageSendingEndCallback;

/**
 * Unit tests for classes that implement the {@link ActorInterface} interface
 */
public class ActorInterfaceTest {

    public static class MessageSendingEndCallbackTest {

	public static void assertInvariants(MessageSendingEndCallback callback) {
	    // Do nothing
	}

	public static void run(MessageSendingEndCallback callback, ActorInterface actorInterface, Medium medium,
		Message message, double amountSent) {
	    callback.run(actorInterface, medium, message, amountSent);
	    assertInvariants(callback);
	}
    }// class

    public static void assertInvariants(ActorInterface actorInterface) {
	final Medium sendingMedium = actorInterface.getSendingMedium();
	final Message sendingMessage = actorInterface.getSendingMessage();
	final double amountOfMessageSent = actorInterface.getAmountOfMessageSent();

	assertEquals("The sending message is null if, and only if, the sending medium is null.", sendingMessage == null,
		sendingMedium == null);
	assertTrue("The amount of message sent is never negative.", 0.0 <= amountOfMessageSent);
	assertFalse("The amount of message sent is zero if the actor is not currently sending a message.",
		0.0 < amountOfMessageSent && sendingMessage == null);
	assertTrue(
		"The amount of message sent is less or equal to than the length of the message being sent, "
			+ "if a message is being sent.",
		sendingMessage == null || amountOfMessageSent <= sendingMessage.getLength());
    }

    public static void beginSendingMessage(ActorInterface actorInterface, Medium medium, Message message,
	    MessageSendingEndCallback sendingEndCallBack) throws MediumUnavailableException {
	try {
	    actorInterface.beginSendingMessage(medium, message, sendingEndCallBack);
	} catch (MediumUnavailableException e) {
	    assertInvariants(actorInterface);
	    throw e;
	}
	assertInvariants(actorInterface);
	assertSame("The given medium is the current sending medium.", medium, actorInterface.getSendingMedium());
	assertSame("The given message is the current sending message.", message, actorInterface.getSendingMessage());
    }
}
