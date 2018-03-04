package uk.badamson.mc.mind.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import uk.badamson.mc.mind.AbstractMind;
import uk.badamson.mc.mind.AbstractMindTest;
import uk.badamson.mc.mind.MessageTransferInProgress;
import uk.badamson.mc.mind.Mind;
import uk.badamson.mc.mind.medium.HandSignals;
import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.Message;
import uk.badamson.mc.mind.message.SimpleDirectCommand;
import uk.badamson.mc.mind.message.UnusableIncompleteMessage;
import uk.badamson.mc.simulation.Clock;
import uk.badamson.mc.simulation.ClockTest;

/**
 * <p>
 * Unit tests of the {@link AI} class.
 * </p>
 */
public class AITest {

    private static final class PlayerSpy extends AbstractMind {

        private int nCallsTellBeginReceivingMessage;
        private int nCallsTellMessageReceptionProgress;
        private int nCallsTellMessageSendingEnded;
        private int nCallsTellMessageTransmissionProgress;

        private Medium medium;
        private MessageTransferInProgress messageBeingReceived;
        private MessageTransferInProgress transmissionProgress;
        private Message fullMessage;

        void assertCalled_tellBeginReceivingMessage(int nCalls, Medium medium) {
            assertEquals("Called tellBeginReceivingMessage, number of calls", nCalls, nCallsTellBeginReceivingMessage);
            assertSame("Called tellBeginReceivingMessage, medium", medium, this.medium);
        }

        void assertCalled_tellMessageReceptionProgress(int nCalls, MessageTransferInProgress messageBeingReceived) {
            assertEquals("Called tellMessageReceptionProgress, number of calls", nCalls,
                    nCallsTellMessageReceptionProgress);
            assertSame("Called tellMessageReceptionProgress, messageBeingReceived", messageBeingReceived,
                    this.messageBeingReceived);
        }

        void assertCalled_tellMessageSendingEnded(int nCalls, MessageTransferInProgress transmissionProgress,
                Message fullMessage) {
            assertEquals("Called tellMessageSendingEnded, number of calls", nCalls, nCallsTellMessageSendingEnded);
            assertSame("Called tellMessageSendingEnded, transmissionProgress", transmissionProgress,
                    this.transmissionProgress);
            assertSame("Called tellMessageSendingEnded, fullMessage", fullMessage, this.fullMessage);
        }

        void assertCalled_tellMessageTransmissionProgress(int nCalls, MessageTransferInProgress transmissionProgress,
                Message fullMessage) {
            assertEquals("Called tellMessageTransmissionProgress, number of calls", nCalls,
                    nCallsTellMessageTransmissionProgress);
            assertSame("Called tellMessageTransmissionProgress, transmissionProgress", transmissionProgress,
                    this.transmissionProgress);
            assertSame("Called tellMessageTransmissionProgress, fullMessage", fullMessage, this.fullMessage);
        }

        @Override
        public void tellBeginReceivingMessage(Medium medium) {
            super.tellBeginReceivingMessage(medium);
            ++nCallsTellBeginReceivingMessage;
            this.medium = medium;
        }

        @Override
        public void tellMessageReceptionProgress(MessageTransferInProgress messageBeingReceived) {
            super.tellMessageReceptionProgress(messageBeingReceived);
            ++nCallsTellMessageReceptionProgress;
            this.messageBeingReceived = messageBeingReceived;
        }

        @Override
        public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage) {
            super.tellMessageSendingEnded(transmissionProgress, fullMessage);
            ++nCallsTellMessageSendingEnded;
            this.transmissionProgress = transmissionProgress;
            this.fullMessage = fullMessage;
        }

        @Override
        public void tellMessageTransmissionProgress(MessageTransferInProgress transmissionProgress,
                Message fullMessage) {
            super.tellMessageTransmissionProgress(transmissionProgress, fullMessage);
            ++nCallsTellMessageTransmissionProgress;
            this.transmissionProgress = transmissionProgress;
            this.fullMessage = fullMessage;
        }

    }// class

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

    public static void tellBeginReceivingMessage(AI ai, Medium medium) {
        AbstractMindTest.tellBeginReceivingMessage(ai, medium);// inherited
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

    @Test
    public void tellBeginReceivingMessage_basic() {
        final AI ai = new AI(clock1);
        final HandSignals medium = HandSignals.INSTANCE;

        tellBeginReceivingMessage(ai, medium);
    }

    @Test
    public void tellBeginReceivingMessage_withPlayer() {
        final HandSignals medium = HandSignals.INSTANCE;
        final MessageTransferInProgress receptionStarted = new MessageTransferInProgress(medium,
                UnusableIncompleteMessage.EMPTY_MESSAGE);
        final AI ai = new AI(clock1);
        final PlayerSpy player = new PlayerSpy();
        ai.setPlayer(player);

        tellBeginReceivingMessage(ai, medium);

        player.assertCalled_tellBeginReceivingMessage(1, medium);
    }

    @Test
    public void tellMessageReceptionProgress_basic() {
        final AI ai = new AI(clock1);
        final MessageTransferInProgress messageBeingReceived = new MessageTransferInProgress(HandSignals.INSTANCE,
                SimpleDirectCommand.CHECK_MAP);

        tellMessageReceptionProgress(ai, messageBeingReceived);
    }

    private void tellMessageReceptionProgress_withPlayer(final Message fullMessage, final double fractionReceived) {
        final MessageTransferInProgress messageBeingReceived = new MessageTransferInProgress(HandSignals.INSTANCE,
                fullMessage.getPartialMessage(fullMessage.getInformationContent() * fractionReceived));
        final AI ai = new AI(clock1);
        final PlayerSpy player = new PlayerSpy();
        ai.setPlayer(player);

        tellMessageReceptionProgress(ai, messageBeingReceived);

        player.assertCalled_tellMessageReceptionProgress(1, messageBeingReceived);
    }

    @Test
    public void tellMessageReceptionProgress_withPlayerA() {
        final Message fullMessage = SimpleDirectCommand.CHECK_MAP;
        final double fractionReceived = 0.5;
        tellMessageReceptionProgress_withPlayer(fullMessage, fractionReceived);
    }

    @Test
    public void tellMessageReceptionProgress_withPlayerB() {
        final Message fullMessage = SimpleDirectCommand.HALT;
        final double fractionReceived = 0.25;
        tellMessageReceptionProgress_withPlayer(fullMessage, fractionReceived);
    }

    @Test
    public void tellMessageSendingEnded_basic() {
        final AI ai = new AI(clock1);
        final Message fullMessage = SimpleDirectCommand.CHECK_MAP;
        final MessageTransferInProgress transmissionProgress = new MessageTransferInProgress(HandSignals.INSTANCE,
                fullMessage.getPartialMessage(fullMessage.getInformationContent() * 0.5));

        tellMessageSendingEnded(ai, transmissionProgress, fullMessage);
    }

    private void tellMessageSendingEnded_withPlayer(final Message fullMessage,
            final MessageTransferInProgress transmissionProgress) {
        final AI ai = new AI(clock1);
        final PlayerSpy player = new PlayerSpy();
        ai.setPlayer(player);

        tellMessageSendingEnded(ai, transmissionProgress, fullMessage);

        player.assertCalled_tellMessageSendingEnded(1, transmissionProgress, fullMessage);
    }

    @Test
    public void tellMessageSendingEnded_withPlayerA() {
        final Message fullMessage = SimpleDirectCommand.HALT;
        final MessageTransferInProgress transmissionProgress = new MessageTransferInProgress(HandSignals.INSTANCE,
                fullMessage);
        tellMessageSendingEnded_withPlayer(fullMessage, transmissionProgress);
    }

    @Test
    public void tellMessageSendingEnded_withPlayerB() {
        final Message fullMessage = SimpleDirectCommand.CHECK_MAP;
        final MessageTransferInProgress transmissionProgress = new MessageTransferInProgress(HandSignals.INSTANCE,
                fullMessage.getPartialMessage(fullMessage.getInformationContent() * 0.5));
        tellMessageSendingEnded_withPlayer(fullMessage, transmissionProgress);
    }

    @Test
    public void tellMessageTransmissionProgress_basic() {
        final AI ai = new AI(clock1);
        final Message fullMessage = SimpleDirectCommand.CHECK_MAP;
        final MessageTransferInProgress transmissionProgress = new MessageTransferInProgress(HandSignals.INSTANCE,
                fullMessage.getPartialMessage(fullMessage.getInformationContent() * 0.5));

        tellMessageTransmissionProgress(ai, transmissionProgress, fullMessage);
    }

    private void tellMessageTransmissionProgress_withPlayer(final Message fullMessage,
            final double fractionTransmitted) {
        final Message partialMessage = fullMessage
                .getPartialMessage(fullMessage.getInformationContent() * fractionTransmitted);
        final MessageTransferInProgress transmissionProgress = new MessageTransferInProgress(HandSignals.INSTANCE,
                partialMessage);
        final AI ai = new AI(clock1);
        final PlayerSpy player = new PlayerSpy();
        ai.setPlayer(player);

        tellMessageTransmissionProgress(ai, transmissionProgress, fullMessage);

        player.assertCalled_tellMessageTransmissionProgress(1, transmissionProgress, fullMessage);
    }

    @Test
    public void tellMessageTransmissionProgress_withPlayerA() {
        final Message fullMessage = SimpleDirectCommand.CHECK_MAP;
        final double fractionTransmitted = 0.5;
        tellMessageTransmissionProgress_withPlayer(fullMessage, fractionTransmitted);
    }

    @Test
    public void tellMessageTransmissionProgress_withPlayerB() {
        final Message fullMessage = SimpleDirectCommand.HALT;
        final double fractionTransmitted = 0.25;
        tellMessageTransmissionProgress_withPlayer(fullMessage, fractionTransmitted);
    }
}
