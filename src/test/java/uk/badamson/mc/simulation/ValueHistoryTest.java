package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;

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

        assertTransitionTimesInvariants(history);
        assertLastTansitionTimeInvariants(history);
    }

    private static <VALUE> void assertInvariants(ValueHistory<VALUE> history, Duration time) {
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        assertTrue(
                "For all points in time not in the set of transition times (except the start of time), the value just before the point in time is equal to the value at the point in time.",
                transitionTimes.contains(time) || time.equals(ValueHistory.START_OF_TIME)
                        || Objects.equals(history.get(time.minusNanos(1L)), history.get(time)));
    }

    public static <VALUE> void assertInvariants(ValueHistory<VALUE> history1, ValueHistory<VALUE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited
    }

    private static <VALUE> Duration assertLastTansitionTimeInvariants(ValueHistory<VALUE> history) {
        final Duration lastTansitionTime = history.getLastTansitionTime();
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertSame(
                "The last value of the set of transition times (if it is not empty) is the same as the last transition time.",
                lastTansitionTime, transitionTimes.isEmpty() ? null : transitionTimes.last());

        return lastTansitionTime;
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

    /**
     * <p>
     * The last point in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>A null last transition time indicates that this history has no
     * transitions. That is, the value is constant for all time.</li>
     * <li>The point in time is represented as the duration since an (implied)
     * epoch.</li>
     * <li>The {@linkplain SortedSet#last() last} value of the
     * {@linkplain #getTransitionTimes() set of transition times} is the same as the
     * last transition time.</li>
     * </ul>
     * 
     * @return the last transition time.
     */

    @Test
    public void constructor_0() {
        final var history = new ValueHistory<Integer>();

        assertInvariants(history);
        assertInvariants(history, DURATION_1);
        assertInvariants(history, DURATION_2);
        assertNull("The value of this history at the start of time is null.", history.get(ValueHistory.START_OF_TIME));
        assertEquals("This has no transition times.", Collections.EMPTY_SET, history.getTransitionTimes());
    }

}
