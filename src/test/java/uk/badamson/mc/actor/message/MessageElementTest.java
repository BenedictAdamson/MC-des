package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Unit tests of classes that implement the {@link MessageElement} interface.
 * </p>
 */
public class MessageElementTest {

    public static void assertInvariants(MessageElement element) {
	final double informationContent = element.getInformationContent();

	assertTrue("The information content of a message element is positive.", 0.0 < informationContent);
	assertTrue("The information content of a message element is finite.", Double.isFinite(informationContent));
    }
}
