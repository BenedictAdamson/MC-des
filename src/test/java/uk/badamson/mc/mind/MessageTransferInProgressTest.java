package uk.badamson.mc.mind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.mind.medium.HandSignals;
import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.Message;
import uk.badamson.mc.mind.message.SimpleDirectCommand;

/**
 * <p>
 * Unit tests for the {@link MessageTransferInProgress} class.
 * </p>
 */
public class MessageTransferInProgressTest {

    private static final Medium MEDIUM_1 = HandSignals.INSTANCE;
    private static final Medium MEDIUM_2 = new Medium() {

        @Override
        public final boolean canConvey(Message message) {
            return message == MESSAGE_1 || message == MESSAGE_2;
        }

        @Override
        public final double getTypicalTransmissionRate() {
            return 44.1 * 1024;
        }

    };
    private static final Message MESSAGE_2 = SimpleDirectCommand.RUSH;
    private static final Message MESSAGE_1 = SimpleDirectCommand.HALT;

    public static void assertInvariants(MessageTransferInProgress progress) {
        ObjectTest.assertInvariants(progress);// inherited

        assertNotNull("Not null, medium", progress.getMedium());
        assertNotNull("Not null, messageSofar", progress.getMessageSofar());
    }

    public static void assertInvariants(MessageTransferInProgress progress1, MessageTransferInProgress progress2) {
        ObjectTest.assertInvariants(progress1, progress2);// 1inherited

        final boolean equals = progress1.equals(progress2);
        assertFalse("Value semantics, medium", equals && !progress1.getMedium().equals(progress2.getMedium()));
        assertFalse("Value semantics, messageSofar",
                equals && !progress1.getMessageSofar().equals(progress2.getMessageSofar()));
    }

    private static MessageTransferInProgress constructor(Medium medium, Message messageSofar) {
        final MessageTransferInProgress progress = new MessageTransferInProgress(medium, messageSofar);

        assertInvariants(progress);
        assertSame("medium", medium, progress.getMedium());
        assertSame("messageSofar", messageSofar, progress.getMessageSofar());

        return progress;
    }

    private static void constructor_2Equals(Medium medium, Message messageSofar) {
        final MessageTransferInProgress progress1 = new MessageTransferInProgress(medium, messageSofar);
        final MessageTransferInProgress progress2 = new MessageTransferInProgress(medium, messageSofar);

        assertInvariants(progress1, progress2);
        assertEquals(progress1, progress2);
    }

    @Test
    public void constructor_2DifferentMedium() {
        final MessageTransferInProgress progress1 = new MessageTransferInProgress(MEDIUM_1, MESSAGE_1);
        final MessageTransferInProgress progress2 = new MessageTransferInProgress(MEDIUM_2, MESSAGE_1);

        assertInvariants(progress1, progress2);
        assertNotEquals(progress1, progress2);
    }

    @Test
    public void constructor_2DifferentMessage() {
        final MessageTransferInProgress progress1 = new MessageTransferInProgress(MEDIUM_1, MESSAGE_1);
        final MessageTransferInProgress progress2 = new MessageTransferInProgress(MEDIUM_1, MESSAGE_2);

        assertInvariants(progress1, progress2);
        assertNotEquals(progress1, progress2);
    }

    @Test
    public void constructor_2EqualsA() {
        constructor_2Equals(MEDIUM_1, MESSAGE_1);
    }

    @Test
    public void constructor_2EqualsB() {
        constructor_2Equals(MEDIUM_2, MESSAGE_2);
    }

    @Test
    public void constructor_A() {
        constructor(MEDIUM_1, MESSAGE_1);
    }

    @Test
    public void constructor_B() {
        constructor(MEDIUM_2, MESSAGE_2);
    }
}
