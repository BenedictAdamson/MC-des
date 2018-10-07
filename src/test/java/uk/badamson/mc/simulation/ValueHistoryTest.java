package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ValueHistory} class.
 * </p>
 */
public class ValueHistoryTest {

    private static final Duration WHEN_1 = Duration.ZERO;
    private static final Duration WHEN_2 = Duration.ofSeconds(2);
    private static final Duration WHEN_3 = Duration.ofSeconds(3);

    private static <VALUE> void appendTransition(ValueHistory<VALUE> history, Duration when, VALUE value)
            throws IllegalStateException {
        final SortedSet<Duration> transitionTimes0 = new TreeSet<>(history.getTransitionTimes());
        final Map<Duration, VALUE> transitionValues0 = transitionTimes0.stream()
                .collect(Collectors.toMap(t -> t, t -> history.get(t)));

        try {
            history.appendTransition(when, value);
        } catch (final IllegalStateException e) {
            // Permitted
            assertInvariants(history);
            final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
            final Map<Duration, VALUE> transitionValues = transitionTimes.stream()
                    .collect(Collectors.toMap(t -> t, t -> history.get(t)));
            assertEquals("This history is unchanged if it throws IllegalStateException [transitionTimes].",
                    transitionTimes0, transitionTimes);
            assertEquals("This history is unchanged if it throws IllegalStateException [transitionValues].",
                    transitionValues0, transitionValues);
            throw e;
        }

        assertInvariants(history);
        final Collection<Duration> transitionTimes = history.getTransitionTimes();
        final Map<Duration, VALUE> transitionValues = transitionTimes.stream()
                .collect(Collectors.toMap(t -> t, t -> history.get(t)));
        assertTrue("Appending a transition does not remove any times from the set of transition times.",
                transitionTimes.containsAll(transitionTimes0));
        assertTrue("Appending a transition does not change the values before the given point in time.",
                transitionValues.entrySet().containsAll(transitionValues0.entrySet()));
        assertEquals("Appending a transition increments the number of transition times.", transitionTimes0.size() + 1,
                transitionTimes.size());
        assertSame("The given point in time becomes the last transition time.", history.getLastTansitionTime(), when);
        assertSame("The given value becomes the last value.", history.getLastValue(), value);
    }

    private static <VALUE> void appendTransition_1(Duration when, VALUE value) {
        final ValueHistory<VALUE> history = new ValueHistory<>();

        appendTransition(history, when, value);

        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        final Map<Duration, VALUE> transitionValues = transitionTimes.stream()
                .collect(Collectors.toMap(t -> t, t -> history.get(t)));
        assertEquals("transitionTimes.", Collections.singleton(when), transitionTimes);
        assertEquals("transitionValues.", Collections.singletonMap(when, value), transitionValues);
    }

    private static <VALUE> void appendTransition_2(Duration when1, VALUE value1, Duration when2, VALUE value2) {
        assert when1.compareTo(when2) < 0;
        final ValueHistory<VALUE> history = new ValueHistory<>();
        history.appendTransition(when1, value1);

        appendTransition(history, when2, value2);

        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        assertEquals("transitionTimes.", Set.of(when1, when2), transitionTimes);
    }

    private static <VALUE> void appendTransition_2InvalidState(Duration when1, VALUE value1, Duration when2,
            VALUE value2) throws IllegalStateException {
        assert when2.compareTo(when1) <= 0 || Objects.equals(value1, value2);
        final ValueHistory<VALUE> history = new ValueHistory<>();
        history.appendTransition(when1, value1);

        appendTransition(history, when2, value2);
    }

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
        ObjectTest.assertInvariants(history);// inherited

        assertTransitionTimesInvariants(history);
        assertFirstTansitionTimeInvariants(history);
        assertLastTansitionTimeInvariants(history);
        assertFirstValueInvariants(history);
        assertLastValueInvariants(history);
        assertEmptyInvariants(history);
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

    @Test
    public void appendTransition_1A() {
        appendTransition_1(WHEN_1, Boolean.TRUE);
    }

    @Test
    public void appendTransition_1B() {
        appendTransition_1(WHEN_2, Integer.MAX_VALUE);
    }

    @Test(expected = IllegalStateException.class)
    public void appendTransition_1InvalidState_valuesNull() {
        final Boolean value = null;
        final ValueHistory<Boolean> history = new ValueHistory<>();

        appendTransition(history, WHEN_1, value);
    }

    @Test
    public void appendTransition_2A() {
        appendTransition_2(WHEN_1, Boolean.FALSE, WHEN_2, Boolean.TRUE);
    }

    @Test
    public void appendTransition_2B() {
        appendTransition_2(WHEN_2, Integer.MIN_VALUE, WHEN_3, Integer.MAX_VALUE);
    }

    @Test(expected = IllegalStateException.class)
    public void appendTransition_2InvalidState_timesOrder() {
        appendTransition_2InvalidState(WHEN_2, Boolean.FALSE, WHEN_1, Boolean.TRUE);
    }

    @Test(expected = IllegalStateException.class)
    public void appendTransition_2InvalidState_timesSame() {
        appendTransition_2InvalidState(WHEN_1, Boolean.FALSE, WHEN_1, Boolean.TRUE);
    }

    @Test(expected = IllegalStateException.class)
    public void appendTransition_2InvalidState_valuesEqual() {
        final String value1 = "Value";
        final String value2 = new String(value1);
        assert value1.equals(value2);
        assert value1 != value2;// tough test
        appendTransition_2InvalidState(WHEN_1, Boolean.FALSE, WHEN_2, Boolean.FALSE);
    }

    @Test(expected = IllegalStateException.class)
    public void appendTransition_2InvalidState_valuesSame() {
        appendTransition_2InvalidState(WHEN_1, Boolean.FALSE, WHEN_2, Boolean.FALSE);
    }

    @Test
    public void constructor_0() {
        final var history = new ValueHistory<Integer>();

        assertInvariants(history);
        assertInvariants(history, WHEN_1);
        assertInvariants(history, WHEN_2);
        assertNull("The value of this history at the start of time is null.", history.getFirstValue());
        assertEquals("This has no transition times.", Collections.EMPTY_SET, history.getTransitionTimes());
    }
}
