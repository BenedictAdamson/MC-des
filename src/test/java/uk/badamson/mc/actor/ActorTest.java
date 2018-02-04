package uk.badamson.mc.actor;

import uk.badamson.mc.actor.message.Message;

/**
 * <p>
 * Unit tests for objects that implement the {@link Actor} interface.
 * </p>
 */
public class ActorTest {

    public static void assertInvariants(Actor actor) {
        // Do nothing
    }

    public static void assertInvariants(Actor actor1, Actor actor2) {
        // Do nothing
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

    public static void tellMessageTransmissionProgress(Actor actor, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        actor.tellMessageTransmissionProgress(transmissionProgress, fullMessage);
        assertInvariants(actor);
    }
}
