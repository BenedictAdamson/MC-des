package uk.badamson.mc.simulation;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ValueHistory} class.
 * </p>
 */
public class ValueHistoryTest {

    public static void assertInvariants(ValueHistory history) {
        ObjectTest.assertInvariants(history);// inherited
    }

    public static void assertInvariants(ValueHistory history1, ValueHistory history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited
    }

    @Test
    public void constructor() {
        final ValueHistory history = new ValueHistory();

        assertInvariants(history);
    }
}
