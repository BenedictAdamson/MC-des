package uk.badamson.mc.actor;

import static org.junit.Assert.assertTrue;

import uk.badamson.mc.actor.medium.Medium;

/**
 * <p>
 * Unit tests for classes that implement the {@link Medium} interface.
 */
public class MediumTest {

    public static void assertInvariants(Medium medium) {
	final double typicalTransmissionRate = medium.getTypicalTransmissionRate();
	assertTrue("The typical transmission rate is always positive.", 0.0 < typicalTransmissionRate);
	assertTrue("The typical transmission rate is finite.", Double.isFinite(typicalTransmissionRate));
    }
}
