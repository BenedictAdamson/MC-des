package uk.badamson.mc.simulation;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Stream;

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

    private static <VALUE> int assertHashCodeInvariants(ValueHistory<VALUE> history) {
        final VALUE firstValue = history.getFirstValue();
        final int firstValueHashCode = firstValue == null ? 0 : firstValue.hashCode();

        final int hashCode = history.hashCode();

        assertEquals("hashCode", firstValueHashCode + history.getTransitions().hashCode(), hashCode);

        return hashCode;

    }

    public static <VALUE> void assertInvariants(ValueHistory<VALUE> history) {
        assertTransitionTimesInvariants(history);
        assertFirstTansitionTimeInvariants(history);
        assertLastTansitionTimeInvariants(history);
        assertFirstValueInvariants(history);
        assertLastValueInvariants(history);
        assertEmptyInvariants(history);
        assertTransitionsInvariants(history);
        assertStreamOfTransitionsInvariants(history);
        assertHashCodeInvariants(history);
    }

    public static <VALUE> void assertInvariants(ValueHistory<VALUE> history, Duration time) {
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        assertTrue(
                "For all points in time not in the set of transition times (except the start of time), the value just before the point in time is equal to the value at the point in time.",
                transitionTimes.contains(time) || time.equals(ValueHistory.START_OF_TIME)
                        || Objects.equals(history.get(time.minusNanos(1L)), history.get(time)));
    }

    public static <VALUE> void assertInvariants(ValueHistory<VALUE> history1, ValueHistory<VALUE> history2) {
        final boolean equals = history1.equals(history2);

        assertFalse("Equality requires equal first values",
                equals && !Objects.equals(history1.getFirstValue(), history2.getFirstValue()));
        assertFalse("Equality requires equal transitions",
                equals && !history1.getTransitions().equals(history2.getTransitions()));
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

        assertEquals("The last value is equal to the value at the end of time.", history.get(ValueHistory.END_OF_TIME),
                lastValue);
        assertTrue("If this history has no transitions, the last value is the same as the first value.",
                lastTansitionTime != null || lastValue == firstValue);
        assertTrue("If this history has transitions, the last value is equal to the value at the last transition.",
                lastTansitionTime == null || Objects.equals(lastValue, valueAtLastTransitionTime));

        return lastValue;
    }

    private static <VALUE> Stream<Map.Entry<Duration, VALUE>> assertStreamOfTransitionsInvariants(
            ValueHistory<VALUE> history) {
        final Set<Duration> transitionTimes = history.getTransitionTimes();

        final Stream<Map.Entry<Duration, VALUE>> stream = history.streamOfTransitions();

        assertNotNull("Always creates a (non null) steam.", stream);// guard
        Map<Duration, VALUE> entries = stream.collect(HashMap::new, (m, e) -> {
            assertNotNull("streamOfTransitions entry", e);// guard
            final Duration when = e.getKey();
            final VALUE value = e.getValue();
            assertNotNull("streamOfTransitions entry key", when);
            assertEquals(
                    "The entries of the stream of transitions have values that are eqaul to the value of this history at the time of their corresponding key.",
                    history.get(when), value);
            m.put(when, value);
        }, HashMap::putAll);

        assertEquals(
                "The stream of transitions contains an entry with a key for each of the transition times of this history.",
                entries.keySet(), transitionTimes);

        return entries.entrySet().stream();
    }

    private static <VALUE> SortedMap<Duration, VALUE> assertTransitionsInvariants(ValueHistory<VALUE> history) {
        final Set<Duration> transitionTimes = history.getTransitionTimes();

        final SortedMap<Duration, VALUE> transitions = history.getTransitions();

        assertNotNull("Always creates a (non null) transitions map.", transitions);// guard
        assertEquals("The keys of the transitions map are equal to the transition times.", transitionTimes,
                transitions.keySet());
        for (var entry : transitions.entrySet()) {
            assertNotNull("streamOfTransitions entry", entry);// guard
            final Duration when = entry.getKey();
            final VALUE value = entry.getValue();
            assertNotNull("streamOfTransitions entry key", when);
            assertEquals(
                    "The entries of the transitions map have values that are eqaul to the value of this history at the time of their corresponding key.",
                    history.get(when), value);
        }
        ;

        return transitions;
    }

    private static <VALUE> SortedSet<Duration> assertTransitionTimesInvariants(ValueHistory<VALUE> history) {
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertNotNull("Always have a set of transition times.", transitionTimes);// guard
        for (Duration transitionTime : transitionTimes) {
            assertNotEquals("There is not a transition at the start of time.", ValueHistory.START_OF_TIME,
                    transitionTime);// guard
            assertThat(
                    "For all points in time in the set of transition times, the value just before the transition is not equal to the value at the transition.",
                    history.get(transitionTime.minusNanos(1L)), not(history.get(transitionTime)));
        }

        return transitionTimes;
    }

    public static <VALUE> Map<Duration, VALUE> getTransitionValues(ValueHistory<VALUE> history) {
        return history.streamOfTransitions().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()),
                HashMap::putAll);
    }

}
