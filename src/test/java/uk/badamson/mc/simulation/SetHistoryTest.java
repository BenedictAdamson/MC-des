package uk.badamson.mc.simulation;

import java.time.Duration;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link SetHistory} interface.
 * </p>
 */
public class SetHistoryTest {

    public static <VALUE> void assertInvariants(SetHistory<VALUE> history) {
        ValueHistoryTest.assertInvariants(history);
    }

    public static <VALUE> void assertInvariants(SetHistory<VALUE> history, Duration time) {
        ValueHistoryTest.assertInvariants(history, time);
    }

    public static <VALUE> void assertInvariants(SetHistory<VALUE> history1, SetHistory<VALUE> history2) {
        ValueHistoryTest.assertInvariants(history1, history2);
    }

}
