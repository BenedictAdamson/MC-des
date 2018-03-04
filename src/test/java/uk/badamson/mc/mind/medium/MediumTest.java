package uk.badamson.mc.mind.medium;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Unit tests for classes that implement the {@link Medium} interface.
 * </p>
 */
public class MediumTest {

    public static void assertInvariants(Medium medium) {
        final double typicalTransmissionRate = medium.getTypicalTransmissionRate();
        assertTrue("The typical transmission rate is always positive.", 0.0 < typicalTransmissionRate);
        assertTrue("The typical transmission rate is finite.", Double.isFinite(typicalTransmissionRate));
    }

    public static void assertInvariants(Medium medium1, Medium medium2) {
        assertFalse("Value semantics, typical transmission rate", medium1.equals(medium2)
                && medium1.getTypicalTransmissionRate() != medium2.getTypicalTransmissionRate());
    }
}
