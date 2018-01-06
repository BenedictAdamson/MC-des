package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.Collections;

import org.junit.Test;

/**
 * <p>
 * Unit tests of the {@link SimpleDirectCommand} class and its enumerated
 * objects.
 * </p>
 */
public class SimpleDirectCommandTest {

    public static void assertInvariants(SimpleDirectCommand command) {
	CommandTest.assertInvariants(command);// inherited

	assertEquals("The information content exceeds the total for the message elements by the same extra amount.",
		SimpleDirectCommand.EXTRA_INFORMATION_CONTENT + command.getSubject().getInformationContent()
			+ command.getVerb().getInformationContent()
			+ SentenceTest.totalInformationContent(command.getObjects()),
		command.getInformationContent(), 1.0E-3);
	assertSame("subject", Pronoun.YOU, command.getSubject());
    }

    public static final SimpleDirectCommand getAssembleInstance(SimpleRelativeLocation location) {
	final SimpleDirectCommand command = SimpleDirectCommand.getAssembleInstance(location);

	assertNotNull("Always returns an instance", command);// guard
	assertInvariants(command);
	assertSame("verb", SimpleVerb.ASSEMBLE, command.getVerb());
	assertEquals("object", Collections.singleton(location), command.getObjects());

	return command;
    }

    public static UnusableIncompleteMessage getPartialMessage(SimpleDirectCommand message, double partLength) {
	final UnusableIncompleteMessage partialMessage = (UnusableIncompleteMessage) MessageTest
		.getPartialMessage(message, partLength);

	assertInvariants(message);
	UnusableIncompleteMessageTest.assertInvariants(partialMessage);

	return partialMessage;
    }

    @Test
    public void getAssembleInstance_all() {
	for (SimpleRelativeLocation location : SimpleRelativeLocation.values()) {
	    getAssembleInstance(location);
	}
    }

    @Test
    public void getPartialMessage_DISPERSE_A() {
	getPartialMessage(SimpleDirectCommand.DISPERSE, SimpleDirectCommand.DISPERSE.getInformationContent() * 0.5);
    }

    @Test
    public void getPartialMessage_DISPERSE_B() {
	getPartialMessage(SimpleDirectCommand.DISPERSE, SimpleDirectCommand.DISPERSE.getInformationContent() * 0.25);
    }

    @Test
    public void static_DISPERSE() {
	final SimpleDirectCommand command = SimpleDirectCommand.DISPERSE;
	assertInvariants(command);
	assertSame("verb", SimpleVerb.CHANGE_FORMATION, command.getVerb());
	assertEquals("object", Collections.singleton(SimpleFormationName.DISPERSE), command.getObjects());
    }

    @Test
    public void static_JOIN_ME() {
	final SimpleDirectCommand command = SimpleDirectCommand.JOIN_ME;
	assertInvariants(command);
	assertSame("verb", SimpleVerb.JOIN, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.ME), command.getObjects());
    }
}
