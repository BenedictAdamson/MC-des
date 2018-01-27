package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import uk.badamson.mc.actor.message.SimpleRelativeLocation.Direction;
import uk.badamson.mc.actor.message.SimpleRelativeLocation.Range;

/**
 * <p>
 * Unit tests of the {@link SimpleRelativeLocation} class
 */
public class SimpleRelativeLocationTest {

    public static void assertInvariants(SimpleRelativeLocation location) {
        NounTest.assertInvariants(location);// inherited

        assertNotNull("Not null, direction", location.getDirection());
        assertNotNull("Not null, range", location.getRange());
        assertEquals("informtationContent", SimpleRelativeLocation.INFORMATION_CONTENT,
                location.getInformationContent(), 1.0E-3);
    }

    private static SimpleRelativeLocation getInstance(Direction direction, Range range) {
        final SimpleRelativeLocation location = SimpleRelativeLocation.getInstance(direction, range);

        assertNotNull("Not null, instance", location);// guard
        assertInvariants(location);
        assertSame("direction", direction, location.getDirection());
        assertSame("range", range, location.getRange());

        return location;
    }

    @Test
    public void instances() {
        for (SimpleRelativeLocation.Direction direction : SimpleRelativeLocation.Direction.values()) {
            for (SimpleRelativeLocation.Range range : SimpleRelativeLocation.Range.values()) {
                getInstance(direction, range);
            }
        }
    }

    @Test
    public void values() {
        for (SimpleRelativeLocation location : SimpleRelativeLocation.values()) {
            assertInvariants(location);
        }
    }
}
