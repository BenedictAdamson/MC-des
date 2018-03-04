package uk.badamson.mc.mind;

import uk.badamson.mc.mind.Actor;
import uk.badamson.mc.mind.MessageTransferInProgress;
import uk.badamson.mc.mind.message.Message;

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

    public static void tellMessageReceptionProgress(Actor actor, MessageTransferInProgress messageBeingReceived) {
        actor.tellMessageReceptionProgress(messageBeingReceived);
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
