package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * <p>
 * Unit tests of classes that implement the {@link Message} interface.
 * </p>
 */
public class MessageTest {

    public static void assertInvariants(Message message) {
        MessageElementTest.assertInvariants(message);// inherited
    }

    public static void assertInvariants(Message message1, Message message2) {
        MessageElementTest.assertInvariants(message1, message2);// inherited
    }

    public static Message getPartialMessage(Message message, double partLength) {
        final Message partialMessage = message.getPartialMessage(partLength);

        assertNotNull("Always returns a message.", partialMessage);// guard
        assertInvariants(message);
        assertInvariants(partialMessage);
        assertEquals("The length of the returned message is equal to the given part length.", partLength,
                partialMessage.getInformationContent(), 1E-3);
        assertNotEquals("The returned message is never equivalent to this message.", partialMessage, message);

        return partialMessage;
    }
}
