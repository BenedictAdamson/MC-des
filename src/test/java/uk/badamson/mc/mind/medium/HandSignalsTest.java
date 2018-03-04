package uk.badamson.mc.mind.medium;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the {@link HandSignals} class.
 * </p>
 */
public class HandSignalsTest {

    public static void assertInvariants(HandSignals handSignals) {
        ObjectTest.assertInvariants(handSignals);// inherited
        MediumTest.assertInvariants(handSignals);// inherited
    }

    public static void assertInvariants(HandSignals handSignals1, HandSignals handSignals2) {
        ObjectTest.assertInvariants(handSignals1, handSignals2);// inherited
    }

    @Test
    public void static_INSTANCE() {
        assertInvariants(HandSignals.INSTANCE);
    }
}
