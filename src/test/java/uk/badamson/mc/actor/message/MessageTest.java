package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Unit tests of classes that implement the {@link Message} interface.
 * </p>
 */
public class MessageTest {

    public static void assertInvariants(Message message) {
	final double length = message.getInformationContent();

	assertTrue("The length of a message is positive.", 0.0 < length);
	assertTrue("The length of a message is finite.", Double.isFinite(length));
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
