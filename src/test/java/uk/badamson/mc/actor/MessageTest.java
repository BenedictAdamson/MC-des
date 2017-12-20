package uk.badamson.mc.actor;

import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Unit tests of classes that implement the {@link Message} interface.
 */
public class MessageTest {

	public static void assertInvariants(Message message) {
		final double length = message.getLength();

		assertTrue("The length of a message is positive.", 0.0 < length);
		assertTrue("The length of a message is finite.", Double.isFinite(length));
	}
}
