package uk.badamson.mc.mind.ai;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import uk.badamson.mc.mind.AbstractMind;
import uk.badamson.mc.mind.AbstractMindTest;
import uk.badamson.mc.mind.MessageTransferInProgress;
import uk.badamson.mc.mind.Mind;
import uk.badamson.mc.mind.message.Message;
import uk.badamson.mc.simulation.Clock;
import uk.badamson.mc.simulation.ClockTest;

/**
 * <p>
 * Unit tests of the {@link AI} class.
 * </p>
 */
public class AITest {

    private static final long TIME_1 = ClockTest.TIME_1;

    private static final long TIME_2 = ClockTest.TIME_2;

    public static void assertInvariants(AI ai) {
        AbstractMindTest.assertInvariants(ai);// inherited
    }

    public static void assertInvariants(AI ai1, AI ai2) {
        AbstractMindTest.assertInvariants(ai1, ai2);// inherited
    }

    private static AI constructor(Clock clock) {
        final AI ai = new AI(clock);

        assertInvariants(ai);
        assertSame("clock", clock, ai.getClock());
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

    private Clock clock1;

    private Clock clock2;

    @Test
    public void constructor_A() {
        constructor(clock1);
    }

    @Test
    public void constructor_B() {
        constructor(clock2);
    }

    private void setPlayer() {
        final AI ai = new AI(clock1);
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

    @Before
    public void setup() {
        clock1 = new Clock(TimeUnit.MILLISECONDS, TIME_1);
        clock2 = new Clock(TimeUnit.MILLISECONDS, TIME_2);
    }
}
