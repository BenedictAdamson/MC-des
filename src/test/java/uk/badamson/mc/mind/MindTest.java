package uk.badamson.mc.mind;

import uk.badamson.mc.mind.Mind;
import uk.badamson.mc.mind.MessageTransferInProgress;
import uk.badamson.mc.mind.message.Message;

/**
 * <p>
 * Unit tests for objects that implement the {@link Mind} interface.
 * </p>
 */
public class MindTest {

    public static void assertInvariants(Mind actor) {
        // Do nothing
    }

    public static void assertInvariants(Mind actor1, Mind actor2) {
        // Do nothing
    }

    public static void tellBeginReceivingMessage(Mind actor, MessageTransferInProgress receptionStarted) {
        actor.tellBeginReceivingMessage(receptionStarted);
        assertInvariants(actor);
    }

    public static void tellMessageReceptionProgress(Mind actor, MessageTransferInProgress messageBeingReceived) {
        actor.tellMessageReceptionProgress(messageBeingReceived);
        assertInvariants(actor);
    }

    public static void tellMessageSendingEnded(Mind actor, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        actor.tellMessageSendingEnded(transmissionProgress, fullMessage);
        assertInvariants(actor);
    }

    public static void tellMessageTransmissionProgress(Mind actor, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        actor.tellMessageTransmissionProgress(transmissionProgress, fullMessage);
        assertInvariants(actor);
    }
}
