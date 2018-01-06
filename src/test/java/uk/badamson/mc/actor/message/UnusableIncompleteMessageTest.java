package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

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

    public static void assertInvariants(UnusableIncompleteMessage message1, UnusableIncompleteMessage message2) {
	ObjectTest.assertInvariants(message1, message2);// inherited
	final boolean equals = message1.equals(message2);
	assertFalse("Equality require equivalent lengths",
		equals && message1.getInformationContent() != message2.getInformationContent());
    }

    private static UnusableIncompleteMessage constructor(double length) {
	final UnusableIncompleteMessage message = new UnusableIncompleteMessage(length);

	assertInvariants(message);
	assertEquals("length", length, message.getInformationContent(), 1E-3);

	return message;
    }

    private static void constructor_2Equivalent(double length) {
	final UnusableIncompleteMessage message1 = new UnusableIncompleteMessage(length);
	final UnusableIncompleteMessage message2 = new UnusableIncompleteMessage(length);

	assertInvariants(message1, message2);
	assertEquals(message1, message2);
    }

    private static void constructor_2NotEquivalent(double length1, double length2) {
	assert length1 != length2;
	final UnusableIncompleteMessage message1 = new UnusableIncompleteMessage(length1);
	final UnusableIncompleteMessage message2 = new UnusableIncompleteMessage(length2);

	assertInvariants(message1, message2);
	assertNotEquals(message1, message2);
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
    public void constructor_2EquivalentA() {
	constructor_2Equivalent(LENGTH_1);
    }

    @Test
    public void constructor_2EquivalentB() {
	constructor_2Equivalent(LENGTH_2);
    }

    @Test
    public void constructor_2NotEquivalentA() {
	constructor_2NotEquivalent(LENGTH_1, LENGTH_2);
    }

    @Test
    public void constructor_2NotEquivalentB() {
	constructor_2NotEquivalent(LENGTH_2, LENGTH_3);
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
