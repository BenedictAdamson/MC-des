package uk.badamson.mc.mind;

import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.Message;

/**
 * <p>
 * Unit tests for objects that implement the {@link Mind} interface.
 * </p>
 */
public class MindTest {

    public static void assertInvariants(Mind mind) {
        // Do nothing
    }

    public static void assertInvariants(Mind mind1, Mind mind2) {
        // Do nothing
    }

    public static void tellBeginReceivingMessage(Mind mind, Medium medium) {
        mind.tellBeginReceivingMessage(medium);
        assertInvariants(mind);
    }

    public static void tellMessageReceptionProgress(Mind mind, MessageTransferInProgress messageBeingReceived) {
        mind.tellMessageReceptionProgress(messageBeingReceived);
        assertInvariants(mind);
    }

    public static void tellMessageSendingEnded(Mind mind, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        mind.tellMessageSendingEnded(transmissionProgress, fullMessage);
        assertInvariants(mind);
    }

    public static void tellMessageTransmissionProgress(Mind mind, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        mind.tellMessageTransmissionProgress(transmissionProgress, fullMessage);
        assertInvariants(mind);
    }
}
