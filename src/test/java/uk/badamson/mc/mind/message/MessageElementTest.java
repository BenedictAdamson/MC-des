package uk.badamson.mc.mind.message;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import uk.badamson.mc.mind.message.MessageElement;

/**
 * <p>
 * Unit tests of classes that implement the {@link MessageElement} interface.
 * </p>
 */
public class MessageElementTest {

    public static void assertInvariants(MessageElement element) {
        final double informationContent = element.getInformationContent();

        assertTrue("The information content of a message element is not negative.", 0.0 <= informationContent);
        assertTrue("The information content of a message element is finite.", Double.isFinite(informationContent));
    }

    public static void assertInvariants(MessageElement element1, MessageElement element2) {
        assertFalse("Value semantics (informationContent)",
                element1.equals(element2) && element1.getInformationContent() != element2.getInformationContent());
    }
}
