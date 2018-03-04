package uk.badamson.mc.mind.ai;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import uk.badamson.mc.mind.AbstractMind;
import uk.badamson.mc.mind.AbstractMindTest;
import uk.badamson.mc.mind.MessageTransferInProgress;
import uk.badamson.mc.mind.Mind;
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
        assertNull("player", ai.getPlayer());

        return ai;
    }

    public static void setPlayer(AI ai, Mind player) {
        ai.setPlayer(player);

        assertInvariants(ai);
        assertSame("player", player, ai.getPlayer());
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

    private void setPlayer() {
        final AI ai = new AI();
        final Mind player = new AbstractMind();

        setPlayer(ai, player);
    }

    @Test
    public void setPlayer_A() {
        setPlayer();
    }

    @Test
    public void setPlayer_B() {
        setPlayer();
    }
}
