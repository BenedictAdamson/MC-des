package uk.badamson.mc.simulation;

import static org.junit.Assert.assertNull;

import java.time.Duration;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ValueHistory} class.
 * </p>
 */
public class ValueHistoryTest {

    private static final Duration DURATION_1 = Duration.ZERO;
    private static final Duration DURATION_2 = Duration.ofSeconds(2);

    public static <VALUE> void assertInvariants(ValueHistory<VALUE> history) {
        ObjectTest.assertInvariants(history);// inherited
    }

    public static <VALUE> void assertInvariants(ValueHistory<VALUE> history1, ValueHistory<VALUE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited
    }

    @Test
    public void constructor_0() {
        final var history = new ValueHistory<Integer>();

        assertInvariants(history);
        assertNull("Value history is null for all points in time [1]", history.get(DURATION_1));
        assertNull("Value history is null for all points in time [2]", history.get(DURATION_2));
    }

}
