package uk.badamson.mc.history;
/*
 * © Copyright Benedict Adamson 2018,2021-22.
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

import uk.badamson.dbc.assertions.EqualsSemanticsVerifier;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ValueHistory} interface.
 * </p>
 */
public class ValueHistoryTest {

    public static <VALUE> void assertInvariants(final ValueHistory<VALUE> history) {
        final VALUE firstValue = history.getFirstValue();
        final VALUE lastValue = history.getLastValue();
        final Duration lastTransitionTime = history.getLastTransitionTime();
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        final SortedMap<Duration, VALUE> transitions = history.getTransitions();
        final Stream<Map.Entry<Duration, VALUE>> streamOfTransitions = history.streamOfTransitions();

        assertAll(
                () -> assertThat("transitionTimes", transitionTimes, notNullValue()),
                () -> assertThat("transitions", transitions, notNullValue()),
                () -> assertThat("streamOfTransitions", streamOfTransitions, notNullValue()));
        assertAll(() -> assertAll("transitionTimes",
                        transitionTimes.stream().map(transitionTime -> () -> {
                                    assertThat("transitionTime", transitionTime, not(ValueHistory.START_OF_TIME));
                                    assertAll("transitionTime " + transitionTime,
                                            () -> assertThat("The value just before the transition is not equal to the value at the transition.",
                                                    history.get(transitionTime.minusNanos(1L)), not(history.get(transitionTime))),
                                            () -> assertThat("The transition time is the next transition time ar or after the transition time",
                                                    history.getTransitionTimeAtOrAfter(transitionTime), is(transitionTime)));
                                }
                        )),
                () -> assertThat("The first value of the set of transition times (if it is not empty) is the same as the first transition time.",
                        history.getFirstTransitionTime(), sameInstance(transitionTimes.isEmpty() ? null : transitionTimes.first())),
                () -> assertThat("The last value of the set of transition times (if it is not empty) is the same as the last transition time.",
                        lastTransitionTime, sameInstance(transitionTimes.isEmpty() ? null : transitionTimes.last())
                ),
                () -> assertThat("firstValue", firstValue, is(history.get(ValueHistory.START_OF_TIME))),
                () -> assertAll("lastValue",
                        () -> assertThat("equal to the value at the end of time.", lastValue, is(history.get(ValueHistory.END_OF_TIME))),
                        () -> assertThat("equal to the first value if this history has no transitions",
                                lastTransitionTime != null || Objects.equals(lastValue, firstValue), is(true)
                        ),
                        () -> assertThat("equal to the value at the last transition, if this history has transitions",
                                lastTransitionTime == null || Objects.equals(lastValue, history.get(lastTransitionTime)))),
                () -> assertThat("A value history is empty if, and only if, it has no transitions.",
                        transitionTimes.isEmpty() == history.isEmpty(), is(true)),
                () -> {
                    assertThat("The keys of the transitions map are equal to the transition times.", (Set<Duration>) transitionTimes, is(transitions.keySet()));
                    assertAll("transitionTime.entrySet entry",
                            transitions.entrySet().stream().map(entry -> () -> {
                                assertThat(entry, notNullValue());
                                final Duration when = entry.getKey();
                                final VALUE value = entry.getValue();
                                assertAll(() -> assertThat("key", when, notNullValue()),
                                        () -> assertThat("value is the history value at the transition time", value,
                                                is(history.get(when))
                                        ));
                            }));
                },
                () -> assertThat("hashCode", history.hashCode(), is((firstValue == null ? 0 : firstValue.hashCode()) + history.getTransitions().hashCode()))
        );
        assertAll("streamOfTransitions entry",
                streamOfTransitions.map(entry -> () -> {
                    assertThat(entry, notNullValue());
                    final Duration when = entry.getKey();
                    final VALUE value = entry.getValue();
                    assertThat("key", when, allOf(notNullValue(), in(transitionTimes)));
                    assertThat("value is the history value at the transition time", value, is(history.get(when)));
                })
        );
    }

    public static <VALUE> void assertInvariants(final ValueHistory<VALUE> history, final Duration time) {
        final var transitionTimes = history.getTransitionTimes();
        final var transitionTimeAtOrAfter = history.getTransitionTimeAtOrAfter(time);
        final var timestampedValue = history.getTimestampedValue(time);

        assertThat("timestampedValue", timestampedValue, notNullValue());
        TimestampedValueTest.assertInvariants(timestampedValue);

        assertAll("at time " + time,
                () -> assertThat(
                        "Value different from previous moment only if a transition",
                        transitionTimes.contains(time) || time.equals(ValueHistory.START_OF_TIME)
                                || Objects.equals(history.get(time.minusNanos(1L)), history.get(time)), is(true)),
                () -> assertAll("transitionTimeAtOrAfter",
                        () -> assertThat(
                                "A (non null) transition time at or after the given time is at or after the given time.",
                                transitionTimeAtOrAfter, anyOf(nullValue(Duration.class), greaterThanOrEqualTo(time))),
                        () -> assertThat(
                                "A (non null) transition time at or after the given time is one of the transition times.",
                                transitionTimeAtOrAfter, anyOf(nullValue(Duration.class), in(transitionTimes)))),
                () -> assertAll("timestampedValue",
                        () -> assertThat("start", timestampedValue.getStart(), lessThanOrEqualTo(time)),
                        () -> assertThat("end", timestampedValue.getEnd(), greaterThanOrEqualTo(time)),
                        () -> assertThat("value", timestampedValue.getValue(), is(history.get(time)))));
    }

    public static <VALUE> void assertInvariants(final ValueHistory<VALUE> history1,
                                                final ValueHistory<VALUE> history2) {
        assertAll(
                () -> EqualsSemanticsVerifier.assertValueSemantics(history1, history2, "firstValue",
                        ValueHistory::getFirstValue),
                () -> EqualsSemanticsVerifier.assertValueSemantics(history1, history2, "transitions",
                        ValueHistory::getTransitions));
    }

    public static <VALUE> Map<Duration, VALUE> getTransitionValues(final ValueHistory<VALUE> history) {
        return history.streamOfTransitions().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()),
                HashMap::putAll);
    }

}
