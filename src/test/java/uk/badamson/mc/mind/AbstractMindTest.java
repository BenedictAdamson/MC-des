package uk.badamson.mc.mind;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.mind.AbstractMind;
import uk.badamson.mc.mind.MessageTransferInProgress;
import uk.badamson.mc.mind.message.Message;

/**
 * <p>
 * Unit tests of the {@link AbstractMind} class.
 * </p>
 */
public class AbstractMindTest {

    public static void assertInvariants(AbstractMind actor) {
        ObjectTest.assertInvariants(actor);// inherited
        MindTest.assertInvariants(actor);// inherited
    }

    public static void assertInvariants(AbstractMind actor1, AbstractMind actor2) {
        ObjectTest.assertInvariants(actor1, actor2);// inherited
        MindTest.assertInvariants(actor1, actor2);// inherited
    }

    public static void tellBeginReceivingMessage(AbstractMind actor, MessageTransferInProgress receptionStarted) {
        MindTest.tellBeginReceivingMessage(actor, receptionStarted);// inherited
        assertInvariants(actor);
    }

    public static void tellMessageReceptionProgress(AbstractMind actor,
            MessageTransferInProgress messageBeingReceived) {
        MindTest.tellMessageReceptionProgress(actor, messageBeingReceived);
        assertInvariants(actor);
    }

    public static void tellMessageSendingEnded(AbstractMind actor, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        MindTest.tellMessageSendingEnded(actor, transmissionProgress, fullMessage);
        assertInvariants(actor);
    }

    public static void tellMessageTransmissionProgress(AbstractMind actor,
            MessageTransferInProgress transmissionProgress, Message fullMessage) {
        MindTest.tellMessageTransmissionProgress(actor, transmissionProgress, fullMessage);
        assertInvariants(actor);
    }
}
