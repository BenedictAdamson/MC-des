package uk.badamson.mc.mind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.Message;
import uk.badamson.mc.mind.message.UnusableIncompleteMessage;

/**
 * Unit tests for classes that implement the {@link MindInterface} interface
 */
public class MindInterfaceTest {

    public static void assertInvariants(MindInterface mindInterface) {
        final Set<Medium> media = mindInterface.getMedia();
        final MessageTransferInProgress transmissionInProgress = mindInterface.getTransmissionInProgress();
        final Message transmittingMessage = mindInterface.getTransmittingMessage();
        final Set<MessageTransferInProgress> messagesBeingReceived = mindInterface.getMessagesBeingReceived();

        final Message messageSofar = transmissionInProgress == null ? null : transmissionInProgress.getMessageSofar();
        final double messageSofarLength = messageSofar == null ? 0.0 : messageSofar.getInformationContent();
        final double transmittingMessageLength = transmittingMessage == null ? Double.NaN
                : transmittingMessage.getInformationContent();

        assertNotNull("Always have a set of media.", media);// guard
        assertNotNull("Always have a set of messages being received.", messagesBeingReceived);// guard

        for (Medium medium : media) {
            assertNotNull("The set of media does not contain a null element.", medium);
        }

        assertEquals("This has a transmission in progress if, and only if, this has a transmitting message.",
                transmissionInProgress == null, transmittingMessage == null);
        assertTrue("If there is a transmission in progress, its medium is one of the media that the actor can use.",
                transmissionInProgress == null || media.contains(transmissionInProgress.getMedium()));
        assertTrue(
                "If there is a transmission in progress, the length of the message sent so far is "
                        + "less than or equal to the length of the message being sent.",
                transmissionInProgress == null || messageSofarLength <= transmittingMessageLength);
        assertTrue(
                "If there is a transmission in progress, the length of the message sent so far equals"
                        + "the length of the message being sent"
                        + "at the instant that the message sent so far is the same as the message being sent.",
                transmissionInProgress == null || messageSofarLength < transmittingMessageLength
                        || messageSofar == transmittingMessage);

        for (MessageTransferInProgress messageTransfer : messagesBeingReceived) {
            assertNotNull("The set of messages being received " + "does not contain a null element.", messageTransfer);// guard
            MessageTransferInProgressTest.assertInvariants(messageTransfer);
            assertTrue("The medium of each message being received is " + "one of the communication media of the actor.",
                    media.contains(messageTransfer.getMedium()));
        }
    }

    public static void assertInvariants(MindInterface mindInterface1, MindInterface mindInterface2) {
        // Do nothing
    }

    public static void beginSendingMessage(MindInterface mindInterface, Medium medium, Message message)
            throws MediumUnavailableException {
        try {
            mindInterface.beginSendingMessage(medium, message);
        } catch (MediumUnavailableException e) {
            assertInvariants(mindInterface);
            throw e;
        }
        assertInvariants(mindInterface);
        final MessageTransferInProgress transmissionInProgress = mindInterface.getTransmissionInProgress();
        assertNotNull("This has a transmission in progress.", transmissionInProgress);// guard
        MessageTransferInProgressTest.assertInvariants(transmissionInProgress);

        assertSame("The medium of the transmission in progress is the given medium.", medium,
                transmissionInProgress.getMedium());
        assertEquals("The message transmitted so far is an empty unusable message.",
                UnusableIncompleteMessage.EMPTY_MESSAGE, transmissionInProgress.getMessageSofar());
        assertSame("The given message is the current transmitting message.", message,
                mindInterface.getTransmittingMessage());
    }

    public static void haltSendingMessage(MindInterface mindInterface) {
        mindInterface.haltSendingMessage();

        assertInvariants(mindInterface);
        assertNull("This has no transmission in progress.", mindInterface.getTransmissionInProgress());
    }
}
