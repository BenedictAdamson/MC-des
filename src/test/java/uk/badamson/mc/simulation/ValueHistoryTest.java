package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Objects;
import java.util.SortedSet;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ValueHistory} interface.
 * </p>
 */
public class ValueHistoryTest {

    private static <VALUE> boolean assertEmptyInvariants(ValueHistory<VALUE> history) {
        final boolean empty = history.isEmpty();

        assertEquals("A value history is empty if, and only if, it has no transitions.",
                history.getTransitionTimes().isEmpty(), empty);

        return empty;

    }

    private static <VALUE> Duration assertFirstTansitionTimeInvariants(ValueHistory<VALUE> history) {
        final Duration firstTansitionTime = history.getFirstTansitionTime();
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertSame(
                "The first value of the set of transition times (if it is not empty) is the same as the first transition time.",
                firstTansitionTime, transitionTimes.isEmpty() ? null : transitionTimes.first());

        return firstTansitionTime;
    }

    private static <VALUE> VALUE assertFirstValueInvariants(ValueHistory<VALUE> history) {
        final VALUE firstValue = history.getFirstValue();

        assertSame("The first value is the same as the value at the start of time.",
                history.get(ValueHistory.START_OF_TIME), firstValue);

        return firstValue;
    }

    public static <VALUE> void assertInvariants(ValueHistory<VALUE> history) {
        assertTransitionTimesInvariants(history);
        assertFirstTansitionTimeInvariants(history);
        assertLastTansitionTimeInvariants(history);
        assertFirstValueInvariants(history);
        assertLastValueInvariants(history);
        assertEmptyInvariants(history);
    }

    public static <VALUE> void assertInvariants(ValueHistory<VALUE> history, Duration time) {
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        assertTrue(
                "For all points in time not in the set of transition times (except the start of time), the value just before the point in time is equal to the value at the point in time.",
                transitionTimes.contains(time) || time.equals(ValueHistory.START_OF_TIME)
                        || Objects.equals(history.get(time.minusNanos(1L)), history.get(time)));
    }

    public static <VALUE> void assertInvariants(ValueHistory<VALUE> history1, ValueHistory<VALUE> history2) {
        // Do nothing
    }

    private static <VALUE> Duration assertLastTansitionTimeInvariants(ValueHistory<VALUE> history) {
        final Duration lastTansitionTime = history.getLastTansitionTime();
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertSame(
                "The last value of the set of transition times (if it is not empty) is the same as the last transition time.",
                lastTansitionTime, transitionTimes.isEmpty() ? null : transitionTimes.last());

        return lastTansitionTime;
    }

    private static <VALUE> VALUE assertLastValueInvariants(ValueHistory<VALUE> history) {
        final VALUE lastValue = history.getLastValue();
        final Duration lastTansitionTime = history.getLastTansitionTime();
        final VALUE valueAtLastTransitionTime = lastTansitionTime == null ? null : history.get(lastTansitionTime);
        final VALUE firstValue = history.getFirstValue();

        assertSame("The last value is the same as the value at the end of time.", history.get(ValueHistory.END_OF_TIME),
                lastValue);
        assertTrue("If this history has no transitions, the last value is the same as the first value.",
                lastTansitionTime != null || lastValue == firstValue);
        assertTrue("If this history has transitions, the last value is the same as the value at the last transition.",
                lastTansitionTime == null || lastValue == valueAtLastTransitionTime);

        return lastValue;
    }

    private static <VALUE> SortedSet<Duration> assertTransitionTimesInvariants(ValueHistory<VALUE> history) {
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertNotNull("Always have a set of transition times.", transitionTimes);// guard
        for (Duration transitionTime : transitionTimes) {
            assertNotEquals("There is not a transition at the start of time.", ValueHistory.START_OF_TIME,
                    transitionTime);// guard
            assertNotEquals(
                    "For all points in time in the set of transition times, the value just before the transition is not equal to the value at the transition.",
                    history.get(transitionTime.minusNanos(1L)), history.get(transitionTime));
        }

        return transitionTimes;
    }

}
