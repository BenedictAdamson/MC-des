package uk.badamson.mc.mind.ai;

import org.junit.Test;

import uk.badamson.mc.mind.AbstractMindTest;
import uk.badamson.mc.mind.MessageTransferInProgress;
import uk.badamson.mc.mind.message.Message;

/**
 * <p>
 * Unit tests of the {@link AI} class.
 * </p>
 */
public class AITest {

    public static void assertInvariants(AI ai) {
        AbstractMindTest.assertInvariants(ai);// inherited
    }

    public static void assertInvariants(AI ai1, AI ai2) {
        AbstractMindTest.assertInvariants(ai1, ai2);// inherited
    }

    private static AI constructor() {
        final AI ai = new AI();

        assertInvariants(ai);

        return ai;
    }

    public static void tellBeginReceivingMessage(AI ai, MessageTransferInProgress receptionStarted) {
        AbstractMindTest.tellBeginReceivingMessage(ai, receptionStarted);// inherited
        assertInvariants(ai);
    }

    public static void tellMessageReceptionProgress(AI ai, MessageTransferInProgress messageBeingReceived) {
        AbstractMindTest.tellMessageReceptionProgress(ai, messageBeingReceived);
        assertInvariants(ai);
    }

    public static void tellMessageSendingEnded(AI ai, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        AbstractMindTest.tellMessageSendingEnded(ai, transmissionProgress, fullMessage);
        assertInvariants(ai);
    }

    public static void tellMessageTransmissionProgress(AI ai, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        AbstractMindTest.tellMessageTransmissionProgress(ai, transmissionProgress, fullMessage);
        assertInvariants(ai);
    }

    @Test
    public void constructor_A() {
        constructor();
    }

}
