package uk.badamson.mc.actor.message;

/**
 * <p>
 * Unit tests of classes that implement the {@link Command} interface.
 * </p>
 */
public class CommandTest {

    public static void assertInvariants(Command command) {
	SentenceTest.assertInvariants(command);// inherited
    }
}
