package uk.badamson.mc.history;
/*
 * © Copyright Benedict Adamson 2018.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ValueHistory} interface.
 * </p>
 */
public class ValueHistoryTest {

    private static <VALUE> boolean assertEmptyInvariants(final ValueHistory<VALUE> history) {
        final boolean empty = history.isEmpty();

        assertEquals(history.getTransitionTimes().isEmpty(), empty,
                "A value history is empty if, and only if, it has no transitions.");

        return empty;

    }

    private static <VALUE> Duration assertFirstTansitionTimeInvariants(final ValueHistory<VALUE> history) {
        final Duration firstTansitionTime = history.getFirstTansitionTime();
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertSame(

                firstTansitionTime, transitionTimes.isEmpty() ? null : transitionTimes.first(),
                "The first value of the set of transition times (if it is not empty) is the same as the first transition time.");

        return firstTansitionTime;
    }

    private static <VALUE> VALUE assertFirstValueInvariants(final ValueHistory<VALUE> history) {
        final VALUE firstValue = history.getFirstValue();

        assertEquals(history.get(ValueHistory.START_OF_TIME), firstValue,
                "The first value is the equal to the value at the start of time.");

        return firstValue;
    }

    private static <VALUE> int assertHashCodeInvariants(final ValueHistory<VALUE> history) {
        final VALUE firstValue = history.getFirstValue();
        final int firstValueHashCode = firstValue == null ? 0 : firstValue.hashCode();

        final int hashCode = history.hashCode();

        assertEquals(firstValueHashCode + history.getTransitions().hashCode(), hashCode, "hashCode");

        return hashCode;

    }

    public static <VALUE> void assertInvariants(final ValueHistory<VALUE> history) {
        assertAll("ValueHistory", () -> assertTransitionTimesInvariants(history),
                () -> assertFirstTansitionTimeInvariants(history), () -> assertLastTansitionTimeInvariants(history),
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
                () -> assertTansitionTimeAtOrAfterInvariants(history, time));
    }

    public static <VALUE> void assertInvariants(final ValueHistory<VALUE> history1,
            final ValueHistory<VALUE> history2) {
        final boolean equals = history1.equals(history2);

        assertAll("equals",
                () -> assertFalse(equals && !Objects.equals(history1.getFirstValue(), history2.getFirstValue()),
                        "Equality requires equal first values"),
                () -> assertFalse(equals && !history1.getTransitions().equals(history2.getTransitions()),
                        "Equality requires equal transitions"));
    }

    private static <VALUE> Duration assertLastTansitionTimeInvariants(final ValueHistory<VALUE> history) {
        final Duration lastTansitionTime = history.getLastTansitionTime();
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertSame(

                lastTansitionTime, transitionTimes.isEmpty() ? null : transitionTimes.last(),
                "The last value of the set of transition times (if it is not empty) is the same as the last transition time.");

        return lastTansitionTime;
    }

    private static <VALUE> VALUE assertLastValueInvariants(final ValueHistory<VALUE> history) {
        final VALUE lastValue = history.getLastValue();
        final Duration lastTansitionTime = history.getLastTansitionTime();
        final VALUE valueAtLastTransitionTime = lastTansitionTime == null ? null : history.get(lastTansitionTime);
        final VALUE firstValue = history.getFirstValue();

        assertAll("lastValue",
                () -> assertEquals(history.get(ValueHistory.END_OF_TIME), lastValue,
                        "The last value is equal to the value at the end of time."),
                () -> assertTrue(lastTansitionTime != null || Objects.equals(lastValue, firstValue),
                        "If this history has no transitions, the last value is equal to the first value."),
                () -> assertTrue(lastTansitionTime == null || Objects.equals(lastValue, valueAtLastTransitionTime),
                        "If this history has transitions, the last value is equal to the value at the last transition."));

        return lastValue;
    }

    private static <VALUE> Stream<Map.Entry<Duration, VALUE>> assertStreamOfTransitionsInvariants(
            final ValueHistory<VALUE> history) {
        final Set<Duration> transitionTimes = history.getTransitionTimes();

        final Stream<Map.Entry<Duration, VALUE>> stream = history.streamOfTransitions();

        assertNotNull(stream, "Always creates a (non null) steam of transitions.");// guard
        final Map<Duration, VALUE> entries = stream.collect(HashMap::new, (m, e) -> {
            assertNotNull(e, "streamOfTransitions entry");// guard
            final Duration when = e.getKey();
            final VALUE value = e.getValue();
            assertAll("streamOfTransitions", () -> assertNotNull(when, "streamOfTransitions entry key"),
                    () -> assertEquals(history.get(when), value,
                            "The entries of the stream of transitions have values that are eqaul to the value of this history at the time of their corresponding key."));
            m.put(when, value);
        }, HashMap::putAll);

        assertEquals(

                entries.keySet(), transitionTimes,
                "The stream of transitions contains an entry with a key for each of the transition times of this history.");

        return entries.entrySet().stream();
    }

    private static <VALUE> Duration assertTansitionTimeAtOrAfterInvariants(final ValueHistory<VALUE> history,
            @NonNull final Duration when) {
        final Duration transitionTime = history.getTansitionTimeAtOrAfter(when);

        assertAll(
                () -> assertThat(
                        "A (non null) transition time at or after the given time is at or after the given time.",
                        transitionTime, anyOf(nullValue(Duration.class), greaterThanOrEqualTo(when))),
                () -> assertThat(
                        "A (non null) transition time at or after the given time is one of the transition times.",
                        transitionTime, anyOf(nullValue(Duration.class), isIn(history.getTransitionTimes()))));

        return transitionTime;
    }

    private static <VALUE> SortedMap<Duration, VALUE> assertTransitionsInvariants(final ValueHistory<VALUE> history) {
        final Set<Duration> transitionTimes = history.getTransitionTimes();

        final SortedMap<Duration, VALUE> transitions = history.getTransitions();

        assertNotNull(transitions, "Always creates a (non null) transitions map.");// guard
        assertEquals(transitionTimes, transitions.keySet(),
                "The keys of the transitions map are equal to the transition times.");
        for (final var entry : transitions.entrySet()) {
            assertNotNull(entry, "streamOfTransitions entry");// guard
            final Duration when = entry.getKey();
            final VALUE value = entry.getValue();
            assertAll("transitions", () -> assertNotNull(when, "streamOfTransitions entry key"), () -> assertEquals(
                    history.get(when), value,
                    "The entries of the transitions map have values that are eqaul to the value of this history at the time of their corresponding key."));
        }

        return transitions;
    }

    private static <VALUE> SortedSet<Duration> assertTransitionTimesInvariants(final ValueHistory<VALUE> history) {
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();

        assertNotNull(transitionTimes, "Always have a set of transition times.");// guard
        for (final Duration transitionTime : transitionTimes) {
            assertNotEquals(ValueHistory.START_OF_TIME, transitionTime,
                    "There is not a transition at the start of time.");// guard
            assertAll(() -> assertThat("For all points in time in <" + transitionTime
                    + "> the set of transition times, the value just before the transition is not equal to the value at the transition.",
                    history.get(transitionTime.minusNanos(1L)), not(history.get(transitionTime))),
                    () -> assertEquals(transitionTime, history.getTansitionTimeAtOrAfter(transitionTime),
                            "The transition time at or after a time that equals one of the transition times equals that transition time."));
        }

        return transitionTimes;
    }

    public static <VALUE> Map<Duration, VALUE> getTransitionValues(final ValueHistory<VALUE> history) {
        return history.streamOfTransitions().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()),
                HashMap::putAll);
    }

}
