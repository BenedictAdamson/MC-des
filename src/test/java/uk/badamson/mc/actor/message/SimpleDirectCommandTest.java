package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
	final Pronoun subject = command.getSubject();

	assertEquals("The information content exceeds the total for the message elements by the same extra amount.",
		SimpleDirectCommand.EXTRA_INFORMATION_CONTENT + subject.getInformationContent()
			+ command.getVerb().getInformationContent()
			+ SentenceTest.totalInformationContent(command.getObjects()),
		command.getInformationContent(), 1.0E-3);
	assertTrue("The subject is either you or we.", subject == Pronoun.YOU || subject == Pronoun.WE);
    }

    public static final SimpleDirectCommand getAssembleInstance(SimpleRelativeLocation location) {
	final SimpleDirectCommand command = SimpleDirectCommand.getAssembleInstance(location);

	assertNotNull("Always returns an instance", command);// guard
	assertInvariants(command);
	assertSame("subject", Pronoun.YOU, command.getSubject());
	assertSame("verb", SimpleVerb.ASSEMBLE, command.getVerb());
	assertEquals("object", Collections.singleton(location), command.getObjects());

	return command;
    }

    public static SimpleDirectCommand getChangeFormationInstance(SimpleFormationName formation) {
	final SimpleDirectCommand command = SimpleDirectCommand.getChangeFormationInstance(formation);

	assertNotNull("Always returns a command", command);// guard
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.CHANGE_FORMATION, command.getVerb());
	assertEquals("object", Collections.singleton(formation), command.getObjects());

	return command;
    }

    public static UnusableIncompleteMessage getPartialMessage(SimpleDirectCommand message, double partLength) {
	final UnusableIncompleteMessage partialMessage = (UnusableIncompleteMessage) MessageTest
		.getPartialMessage(message, partLength);

	assertInvariants(message);
	UnusableIncompleteMessageTest.assertInvariants(partialMessage);

	return partialMessage;
    }

    public static SimpleDirectCommand getPerformBattleDrillInstance(BattleDrillName drill) {
	final SimpleDirectCommand command = SimpleDirectCommand.getPerformBattleDrillInstance(drill);

	assertNotNull("Always returns a command", command);// guard
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.PERFORM_BATTLE_DRILL, command.getVerb());
	assertEquals("object", Collections.singleton(drill), command.getObjects());

	return command;
    }

    @Test
    public void getAssembleInstance_all() {
	for (SimpleRelativeLocation location : SimpleRelativeLocation.values()) {
	    getAssembleInstance(location);
	}
    }

    @Test
    public void getChangeFormationInstance_all() {
	for (SimpleFormationName formation : SimpleFormationName.values()) {
	    getChangeFormationInstance(formation);
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
    public void getPerformBattleDrillInstance_all() {
	for (BattleDrillName drill : BattleDrillName.values()) {
	    getPerformBattleDrillInstance(drill);
	}
    }

    @Test
    public void static_DISPERSE() {
	final SimpleDirectCommand command = SimpleDirectCommand.DISPERSE;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.CHANGE_FORMATION, command.getVerb());
	assertEquals("object", Collections.singleton(SimpleFormationName.DISPERSED), command.getObjects());
    }

    @Test
    public void static_FIX_BAYONET() {
	final SimpleDirectCommand command = SimpleDirectCommand.FIX_BAYONET;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.FIX_BAYONET, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
    }

    @Test
    public void static_JOIN_ME() {
	final SimpleDirectCommand command = SimpleDirectCommand.JOIN_ME;
	assertInvariants(command);
	assertSame("subject", Pronoun.YOU, command.getSubject());
	assertSame("verb", SimpleVerb.JOIN, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.ME), command.getObjects());
    }

    @Test
    public void static_RUSH() {
	final SimpleDirectCommand command = SimpleDirectCommand.RUSH;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.RUSH, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
    }
}
