package uk.badamson.mc.mind;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.Message;

/**
 * <p>
 * Unit tests of the {@link AbstractMind} class.
 * </p>
 */
public class AbstractMindTest {

    public static void assertInvariants(AbstractMind mind) {
        ObjectTest.assertInvariants(mind);// inherited
        MindTest.assertInvariants(mind);// inherited
    }

    public static void assertInvariants(AbstractMind mind1, AbstractMind mind2) {
        ObjectTest.assertInvariants(mind1, mind2);// inherited
        MindTest.assertInvariants(mind1, mind2);// inherited
    }

    public static void tellBeginReceivingMessage(AbstractMind mind, Medium medium) {
        MindTest.tellBeginReceivingMessage(mind, medium);// inherited
        assertInvariants(mind);
    }

    public static void tellMessageReceptionProgress(AbstractMind mind, MessageTransferInProgress messageBeingReceived,
            boolean complete) {
        MindTest.tellMessageReceptionProgress(mind, messageBeingReceived, complete);
        assertInvariants(mind);
    }

    public static void tellMessageSendingEnded(AbstractMind mind, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        MindTest.tellMessageSendingEnded(mind, transmissionProgress, fullMessage);
        assertInvariants(mind);
    }

    public static void tellMessageTransmissionProgress(AbstractMind mind,
            MessageTransferInProgress transmissionProgress, Message fullMessage) {
        MindTest.tellMessageTransmissionProgress(mind, transmissionProgress, fullMessage);
        assertInvariants(mind);
    }
}
