package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests of the {@link SimpleDirectCommand} class and its enumerated
 * objects.
 * </p>
 */
public class SimpleDirectCommandTest {

    public static void assertInvariants(SimpleDirectCommand command) {
	ObjectTest.assertInvariants(command);// inherited
	CommandTest.assertInvariants(command);// inherited

	final Noun subject = command.getSubject();

	assertEquals("The information content exceeds the total for the message elements by the same extra amount.",
		SimpleDirectCommand.EXTRA_INFORMATION_CONTENT + subject.getInformationContent()
			+ command.getVerb().getInformationContent()
			+ SentenceTest.totalInformationContent(command.getObjects()),
		command.getInformationContent(), 1.0E-3);
	assertTrue("One of the finite array of values of the type",
		Arrays.asList(SimpleDirectCommand.values()).indexOf(command) != -1);
    }

    public static void assertInvariants(SimpleDirectCommand command1, SimpleDirectCommand command2) {
	ObjectTest.assertInvariants(command1, command2);// inherited
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

    public static SimpleDirectCommand getRoleForwardInstance(MilitaryRole role) {
	final SimpleDirectCommand command = SimpleDirectCommand.getRoleForwardInstance(role);

	assertNotNull("Always returns a command", command);// guard
	assertInvariants(command);
	assertSame("subject", role, command.getSubject());
	assertSame("verb", SimpleVerb.JOIN, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.ME), command.getObjects());

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
    public void getPartialMessage_A() {
	getPartialMessage(SimpleDirectCommand.RUSH, SimpleDirectCommand.RUSH.getInformationContent() * 0.5);
    }

    @Test
    public void getPartialMessage_B() {
	getPartialMessage(SimpleDirectCommand.RUSH, SimpleDirectCommand.RUSH.getInformationContent() * 0.25);
    }

    @Test
    public void getPerformBattleDrillInstance_all() {
	for (BattleDrillName drill : BattleDrillName.values()) {
	    getPerformBattleDrillInstance(drill);
	}
    }

    @Test
    public void getRoleForwardInstance_all() {
	for (MilitaryRole role : MilitaryRole.values()) {
	    getRoleForwardInstance(role);
	}
    }

    @Test
    public void static_CHECK_MAP() {
	final SimpleDirectCommand command = SimpleDirectCommand.CHECK_MAP;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.CHECK_MAP, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
    }

    @Test
    public void static_CHECK_NUMER_PRESENT() {
	final SimpleDirectCommand command = SimpleDirectCommand.CHECK_NUMER_PRESENT;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.CHECK_NUMER_PRESENT, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
    }

    @Test
    public void static_CHECK_PACES() {
	final SimpleDirectCommand command = SimpleDirectCommand.CHECK_PACES;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.CHECK_PACES, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
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
    public void static_HALT() {
	final SimpleDirectCommand command = SimpleDirectCommand.HALT;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.HALT, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
    }

    @Test
    public void static_HALT_AND_FREEZE() {
	final SimpleDirectCommand command = SimpleDirectCommand.HALT_AND_FREEZE;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.HALT_AND_FREEZE, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
    }

    @Test
    public void static_HALT_AND_GO_PRONE() {
	final SimpleDirectCommand command = SimpleDirectCommand.HALT_AND_GO_PRONE;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.HALT_AND_GO_PRONE, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
    }

    @Test
    public void static_HALT_AND_TAKE_A_KNEE() {
	final SimpleDirectCommand command = SimpleDirectCommand.HALT_AND_TAKE_A_KNEE;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.HALT_AND_TAKE_A_KNEE, command.getVerb());
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
    public void static_QUICK_TIME() {
	final SimpleDirectCommand command = SimpleDirectCommand.QUICK_TIME;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.QUICK_TIME, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
    }

    @Test
    public void static_RUSH() {
	final SimpleDirectCommand command = SimpleDirectCommand.RUSH;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.RUSH, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
    }

    @Test
    public void static_TAKE_COVER() {
	final SimpleDirectCommand command = SimpleDirectCommand.TAKE_COVER;
	assertInvariants(command);
	assertSame("subject", Pronoun.WE, command.getSubject());
	assertSame("verb", SimpleVerb.TAKE_COVER, command.getVerb());
	assertEquals("object", Collections.singleton(Pronoun.IT), command.getObjects());
    }

    @Test
    public void values() {
	final SimpleDirectCommand[] values = SimpleDirectCommand.values();

	assertNotNull("Always returns an array of values.", values);// guard
	for (int i = 0; i < values.length; ++i) {
	    final SimpleDirectCommand value = values[i];
	    assertNotNull("The array of values has no null elements.", value);// guard
	    assertInvariants(value);
	    for (int j = i; j < values.length; ++j) {
		final SimpleDirectCommand value2 = values[j];
		assertInvariants(value, value2);
		assertEquals("The array of values has no duplicate elements<" + value + ", " + value2 + ">.", i == j,
			value.equals(value2));
	    }
	}
    }
}
