package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertNotNull;
import java.util.Set;

/**
 * <p>
 * Unit tests of classes that implement the {@link Command} interface.
 * </p>
 */
public class CommandTest {

    public static void assertInvariants(Command command) {
	MessageTest.assertInvariants(command);// inherited

	final Noun subject = command.getSubject();
	final Verb verb = command.getVerb();
	final Set<Noun> objects = command.getObjects();

	assertNotNull("Not null, subject", subject);// guard
	assertNotNull("Not null, verb", verb);// guard
	assertNotNull("Not null, objects", objects);// guard

	NounTest.assertInvariants(subject);
	VerbTest.assertInvariants(verb);
	for (final Noun object : objects) {
	    assertNotNull("Not null, object", object);// guard
		NounTest.assertInvariants(object);
	}
    }
}
