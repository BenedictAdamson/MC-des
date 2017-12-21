package uk.badamson.mc.actor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * <p>
 * Unit tests for objects that implement the {@link Actor} interface.
 * </p>
 */
public class ActorTest {

    public static void assertInvariants(Actor actor) {
	final ActorInterface actorInterface = actor.getActorInterface();

	assertNotNull("Always have an actor interface", actorInterface);// guard
	assertSame("This is the actor of the actor interface of this actor", actor, actorInterface.getActor());
    }

    public static void tellBeginReceivingMessage(Actor actor, MessageTransferInProgress receptionStarted) {
	actor.tellBeginReceivingMessage(receptionStarted);
	assertInvariants(actor);
    }

    public static void tellMessageReceptionProgress(Actor actor, Message previousMessageSoFar,
	    MessageTransferInProgress messageBeingReceived) {
	actor.tellMessageReceptionProgress(previousMessageSoFar, messageBeingReceived);
	assertInvariants(actor);
    }

    public static void tellMessageSendingEnded(Actor actor, MessageTransferInProgress transmissionProgress,
	    Message fullMessage) {
	actor.tellMessageSendingEnded(transmissionProgress, fullMessage);
	assertInvariants(actor);
    }

    public static void tellMessageTransmissionProgress(Actor actor, Message previousMessageSoFar) {
	actor.tellMessageTransmissionProgress(previousMessageSoFar);
	assertInvariants(actor);
    }
}
