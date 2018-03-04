package uk.badamson.mc.mind.message;

import uk.badamson.mc.mind.message.Verb;

/**
 * <p>
 * Unit tests of classes that implement the {@link Verb} interface.
 * </p>
 */
public class VerbTest {

    public static void assertInvariants(Verb verb) {
        MessageElementTest.assertInvariants(verb);// inherited
        // Do nothing
    }
}
