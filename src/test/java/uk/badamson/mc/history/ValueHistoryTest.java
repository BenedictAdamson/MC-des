package uk.badamson.mc.history;
/*
 * © Copyright Benedict Adamson 2018,2021.
 *
 * This file is part of MC-des.
 *
 * MC-des is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MC-des is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MC-des.  If not, see <https://www.gnu.org/licenses/>.
 */

import uk.badamson.dbc.assertions.EqualsSemanticsTest;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ValueHistory} interface.
 * </p>
 */
public class ValueHistoryTest {

    private static <VALUE> void assertEmptyInvariants(final ValueHistory<VALUE> history) {

        assertEquals(history.getTransitionTimes().isEmpty(), history.isEmpty(), "A value history is empty if, and only if, it has no transitions.");

    }

    private static <VALUE> void assertFirstTransitionTimeInvariants(final ValueHistory<VALUE> history) {
        final Duration firstTransitionTime = history.getFirstTransitionTime();
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertSame(firstTransitionTime, transitionTimes.isEmpty() ? null : transitionTimes.first(),
                "The first value of the set of transition times (if it is not empty) is the same as the first transition time.");
    }

    private static <VALUE> void assertFirstValueInvariants(final ValueHistory<VALUE> history) {
        assertEquals(history.get(ValueHistory.START_OF_TIME), history.getFirstValue(),
                "The first value is equal to the value at the start of time.");
    }

    private static <VALUE> void assertHashCodeInvariants(final ValueHistory<VALUE> history) {
        final VALUE firstValue = history.getFirstValue();
        final int firstValueHashCode = firstValue == null ? 0 : firstValue.hashCode();
        final int hashCode = history.hashCode();

        assertEquals(firstValueHashCode + history.getTransitions().hashCode(), hashCode, "hashCode");
    }

    public static <VALUE> void assertInvariants(final ValueHistory<VALUE> history) {
        assertAll("ValueHistory", () -> assertTransitionTimesInvariants(history),
                () -> assertFirstTransitionTimeInvariants(history), () -> assertLastTransitionTimeInvariants(history),
                () -> assertFirstValueInvariants(history), () -> assertLastValueInvariants(history),
                () -> assertEmptyInvariants(history), () -> assertTransitionsInvariants(history),
                () -> assertStreamOfTransitionsInvariants(history), () -> assertHashCodeInvariants(history));
    }

    public static <VALUE> void assertInvariants(final ValueHistory<VALUE> history, final Duration time) {
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        assertAll(() -> assertTrue(
                transitionTimes.contains(time) || time.equals(ValueHistory.START_OF_TIME)
                        || Objects.equals(history.get(time.minusNanos(1L)), history.get(time)),
                "For all points in time not in the set of transition times (except the start of time), "
                        + "the value just before the point in time is equal to the value at the point in time."),
                () -> assertTransitionTimeAtOrAfterInvariants(history, time),
                () -> assertTimestampedValueInvariants(history, time));
    }

    public static <VALUE> void assertInvariants(final ValueHistory<VALUE> history1,
                                                final ValueHistory<VALUE> history2) {
        assertAll("Value semantics",
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "firstValue",
                        ValueHistory::getFirstValue),
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "transitions",
                        ValueHistory::getTransitions));
    }

    private static <VALUE> void assertLastTransitionTimeInvariants(final ValueHistory<VALUE> history) {
        final Duration lastTransitionTime = history.getLastTransitionTime();
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertSame(lastTransitionTime, transitionTimes.isEmpty() ? null : transitionTimes.last(),
                "The last value of the set of transition times (if it is not empty) is the same as the last transition time.");
    }

    private static <VALUE> void assertLastValueInvariants(final ValueHistory<VALUE> history) {
        final VALUE lastValue = history.getLastValue();
        final Duration lastTransitionTime = history.getLastTransitionTime();
        final VALUE valueAtLastTransitionTime = lastTransitionTime == null ? null : history.get(lastTransitionTime);
        final VALUE firstValue = history.getFirstValue();

        assertAll("lastValue",
                () -> assertEquals(history.get(ValueHistory.END_OF_TIME), lastValue,
                        "The last value is equal to the value at the end of time."),
                () -> assertTrue(lastTransitionTime != null || Objects.equals(lastValue, firstValue),
                        "If this history has no transitions, the last value is equal to the first value."),
                () -> assertTrue(lastTransitionTime == null || Objects.equals(lastValue, valueAtLastTransitionTime),
                        "If this history has transitions, the last value is equal to the value at the last transition."));
    }

    private static <VALUE> void assertStreamOfTransitionsInvariants(
            final ValueHistory<VALUE> history) {
        final Set<Duration> transitionTimes = history.getTransitionTimes();

        final Stream<Map.Entry<Duration, VALUE>> stream = history.streamOfTransitions();

        assertNotNull(stream, "Always creates a (non null) steam of transitions.");// guard
        final Map<Duration, VALUE> entries = stream.collect(HashMap::new, (m, e) -> assertStreamOfTransitionsEntryInvariants(history, m, e), HashMap::putAll);

        assertEquals(
                entries.keySet(), transitionTimes,
                "The stream of transitions contains an entry with a key for each of the transition times of this history.");
    }

    private static <VALUE> void assertStreamOfTransitionsEntryInvariants(final ValueHistory<VALUE> history, final HashMap<Duration, VALUE> m, final Map.Entry<Duration, VALUE> e) {
        assertNotNull(e, "streamOfTransitions entry");// guard
        final Duration when = e.getKey();
        final VALUE value = e.getValue();
        assertAll("streamOfTransitions", () -> assertNotNull(when, "streamOfTransitions entry key"),
                () -> assertEquals(history.get(when), value,
                        "The entries of the stream of transitions have values that are equal to the value of this history at the time of their corresponding key."));
        m.put(when, value);
    }

    private static <VALUE> void assertTransitionTimeAtOrAfterInvariants(final ValueHistory<VALUE> history,
                                                                        @Nonnull final Duration when) {
        final Duration transitionTime = history.getTransitionTimeAtOrAfter(when);

        assertAll(
                () -> assertThat(
                        "A (non null) transition time at or after the given time is at or after the given time.",
                        transitionTime, anyOf(nullValue(Duration.class), greaterThanOrEqualTo(when))),
                () -> assertThat(
                        "A (non null) transition time at or after the given time is one of the transition times.",
                        transitionTime, anyOf(nullValue(Duration.class), in(history.getTransitionTimes()))));
    }

    private static <VALUE> void assertTimestampedValueInvariants(
            @Nonnull final ValueHistory<VALUE> history, @Nonnull final Duration when) {
        final var result = history.getTimestampedValue(when);

        assertNotNull(result, "Not null, result");// guard
        assertInvariants(history);
        TimestampedValueTest.assertInvariants(result);

        final var start = result.getStart();
        final var end = result.getEnd();
        final var value = result.getValue();
        assertAll("Consistent with arguments", () -> assertThat("start", start, lessThanOrEqualTo(when)),
                () -> assertThat("end", end, greaterThanOrEqualTo(when)));
        assertEquals(value, history.get(when), "Value consistent with history");
    }

    private static <VALUE> void assertTransitionsInvariants(final ValueHistory<VALUE> history) {
        final Set<Duration> transitionTimes = history.getTransitionTimes();

        final SortedMap<Duration, VALUE> transitions = history.getTransitions();

        assertNotNull(transitions, "Always creates a (non null) transitions map.");// guard
        assertEquals(transitionTimes, transitions.keySet(),
                "The keys of the transitions map are equal to the transition times.");
        for (final var entry : transitions.entrySet()) {
            assertTransitionsEntryInvariants(history, entry);
        }
    }

    private static <VALUE> void assertTransitionsEntryInvariants(final ValueHistory<VALUE> history, final Map.Entry<Duration, VALUE> entry) {
        assertNotNull(entry, "streamOfTransitions entry");// guard
        final Duration when = entry.getKey();
        final VALUE value = entry.getValue();
        assertAll("transitions", () -> assertNotNull(when, "streamOfTransitions entry key"), () -> assertEquals(
                history.get(when), value,
                "The entries of the transitions map have values that are equal to the value of this history at the time of their corresponding key."));
    }

    private static <VALUE> void assertTransitionTimesInvariants(final ValueHistory<VALUE> history) {
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertNotNull(transitionTimes, "Always have a set of transition times.");// guard
        for (final Duration transitionTime : transitionTimes) {
            assertTransitionTimesEntryInvariants(history, transitionTime);
        }
    }

    private static <VALUE> void assertTransitionTimesEntryInvariants(final ValueHistory<VALUE> history, final Duration transitionTime) {
        assertNotEquals(ValueHistory.START_OF_TIME, transitionTime,
                "There is not a transition at the start of time.");// guard
        assertAll(() -> assertThat("For all points in time in <" + transitionTime
                        + "> the set of transition times, the value just before the transition is not equal to the value at the transition.",
                history.get(transitionTime.minusNanos(1L)), not(history.get(transitionTime))),
                () -> assertEquals(transitionTime, history.getTransitionTimeAtOrAfter(transitionTime),
                        "The transition time at or after a time that equals one of the transition times equals that transition time."));
    }

    public static <VALUE> Map<Duration, VALUE> getTransitionValues(final ValueHistory<VALUE> history) {
        return history.streamOfTransitions().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()),
                HashMap::putAll);
    }

}
