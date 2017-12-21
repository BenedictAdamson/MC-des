package uk.badamson.mc.actor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests of the class {@link UnusableIncompleteMessage}
 */
public class UnusableIncompleteMessageTest {

    private static final double LENGTH_1 = 1;
    private static final double LENGTH_2 = 1024;
    private static final double LENGTH_3 = 4 * 1024 * 1024;

    public static void assertInvariants(UnusableIncompleteMessage message) {
	ObjectTest.assertInvariants(message);// inherited
	MessageTest.assertInvariants(message);// inherited
    }

    private static UnusableIncompleteMessage constructor(double length) {
	final UnusableIncompleteMessage message = new UnusableIncompleteMessage(length);

	assertInvariants(message);
	assertEquals("length", length, message.getLength(), 1E-3);

	return message;
    }

    private static void getPartialMessage(double partLength, double length) {
	final UnusableIncompleteMessage message = new UnusableIncompleteMessage(length);
	getPartialMessage(message, partLength);
    }

    private static UnusableIncompleteMessage getPartialMessage(UnusableIncompleteMessage message, double partLength) {
	final UnusableIncompleteMessage partialMessage = (UnusableIncompleteMessage) MessageTest
		.getPartialMessage(message, partLength);

	assertInvariants(partialMessage);

	return partialMessage;
    }

    @Test
    public void constructor_A() {
	constructor(LENGTH_1);
    }

    @Test
    public void constructor_B() {
	constructor(LENGTH_2);
    }

    @Test
    public void getPartialMessage_A() {
	getPartialMessage(LENGTH_1, LENGTH_2);
    }

    @Test
    public void getPartialMessage_B() {
	getPartialMessage(LENGTH_2, LENGTH_3);
    }
}
