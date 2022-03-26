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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.badamson.dbc.assertions.ObjectVerifier;
import uk.badamson.mc.JsonTest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ModifiableValueHistory}
 * class.
 * </p>
 */
public class ModifiableValueHistoryTest {

    private static final Duration WHEN_1 = Duration.ZERO;
    private static final Duration WHEN_2 = Duration.ofSeconds(2);
    private static final Duration WHEN_3 = Duration.ofSeconds(3);
    private static final Duration WHEN_4 = Duration.ofSeconds(5);

    public static <VALUE> void assertInvariants(final ModifiableValueHistory<VALUE> history) {
        ObjectVerifier.assertInvariants(history);// inherited
        ValueHistoryTest.assertInvariants(history);// inherited
    }

    public static <VALUE> void assertInvariants(final ModifiableValueHistory<VALUE> history1,
                                                final ModifiableValueHistory<VALUE> history2) {
        ObjectVerifier.assertInvariants(history1, history2);// inherited
        ValueHistoryTest.assertInvariants(history1, history2);// inherited
    }

    @Nested
    public class AppendTransition {

        private <VALUE> void appendTransition(final ModifiableValueHistory<VALUE> history, final Duration when,
                                              final VALUE value) throws IllegalStateException {
            final SortedSet<Duration> transitionTimes0 = new TreeSet<>(history.getTransitionTimes());
            final Map<Duration, VALUE> transitionValues0 = ValueHistoryTest.getTransitionValues(history);

            try {
                history.appendTransition(when, value);
            } catch (final IllegalStateException e) {
                // Permitted
                assertInvariants(history);
                final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
                final Map<Duration, VALUE> transitionValues = ValueHistoryTest.getTransitionValues(history);
                assertAll("This history is unchanged if it throws IllegalStateException.",
                        () -> assertEquals(transitionTimes0, transitionTimes, "transitionTimes"),
                        () -> assertEquals(transitionValues0, transitionValues, "transitionValues"));
                throw e;
            }

            assertInvariants(history);
            final Collection<Duration> transitionTimes = history.getTransitionTimes();
            final Map<Duration, VALUE> transitionValues = ValueHistoryTest.getTransitionValues(history);
            assertAll("Appending a transition",
                    () -> assertTrue(transitionTimes.containsAll(transitionTimes0),
                            "Appending a transition does not remove any times from the set of transition times."),
                    () -> assertTrue(transitionValues.entrySet().containsAll(transitionValues0.entrySet()),
                            "Appending a transition does not change the values before the given point in time."),
                    () -> assertEquals(transitionTimes0.size() + 1, transitionTimes.size(),
                            "Appending a transition increments the number of transition times."));
            assertAll("The given becomes",
                    () -> assertSame(history.getLastTransitionTime(), when,
                            "The given point in time becomes the last transition time."),
                    () -> assertSame(history.getLastValue(), value, "The given value becomes the last value."));
        }

        @Nested
        public class Call1 {
            @Test
            public void a() {
                appendTransition_1(WHEN_1, Boolean.TRUE);
            }

            private <VALUE> void appendTransition_1(final Duration when, final VALUE value) {
                final ModifiableValueHistory<VALUE> history0 = new ModifiableValueHistory<>();
                final ModifiableValueHistory<VALUE> history1 = new ModifiableValueHistory<>();
                history1.appendTransition(when, value);
                final ModifiableValueHistory<VALUE> history2 = new ModifiableValueHistory<>();

                appendTransition(history2, when, value);

                assertAll("Invariants", () -> assertInvariants(history0, history2),
                        () -> assertInvariants(history1, history2));

                final SortedSet<Duration> transitionTimes = history2.getTransitionTimes();
                final Map<Duration, VALUE> transitionValues = ValueHistoryTest.getTransitionValues(history2);
                assertAll("Transitions",
                        () -> assertEquals(Collections.singleton(when), transitionTimes, "transitionTimes."),
                        () -> assertEquals(Collections.singletonMap(when, value), transitionValues,
                                "transitionValues."));

                assertAll("Value semantics", () -> assertNotEquals(history0, history2, "before and after"),
                        () -> assertEquals(history1, history2, "same changes"));
            }

            @Test
            public void b() {
                appendTransition_1(WHEN_2, Integer.MAX_VALUE);
            }

            @Test
            public void invalidState_valuesNull() {
                final ModifiableValueHistory<Boolean> history = new ModifiableValueHistory<>();

                assertThrows(IllegalStateException.class, () -> appendTransition(history, WHEN_1, null));
            }
        }// class

        @Nested
        public class Call2 {

            @Test
            public void a() {
                appendTransition_2(WHEN_1, Boolean.FALSE, WHEN_2, Boolean.TRUE);
            }

            private <VALUE> void appendTransition_2(final Duration when1, final VALUE value1, final Duration when2,
                                                    final VALUE value2) {
                assert when1.compareTo(when2) < 0;
                final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<>();
                history.appendTransition(when1, value1);

                appendTransition(history, when2, value2);

                final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
                final var timestampedValue0 = history.getTimestampedValue(ValueHistory.START_OF_TIME);
                final var timestampedValue1 = history.getTimestampedValue(when1);
                final var timestampedValue2 = history.getTimestampedValue(when2);
                assertEquals(Set.of(when1, when2), transitionTimes, "transitionTimes.");
                assertAll("timestampedValue",
                        () -> assertThat("at start of time", timestampedValue0,
                                is(new TimestampedValue<>(ValueHistory.START_OF_TIME, when1.minusNanos(1), null))),
                        () -> assertThat("at first", timestampedValue1,
                                is(new TimestampedValue<>(when1, when2.minusNanos(1), value1))),
                        () -> assertThat("at second", timestampedValue2,
                                is(new TimestampedValue<>(when2, ValueHistory.END_OF_TIME, value2))));
            }

            @Test
            public void b() {
                appendTransition_2(WHEN_2, Integer.MIN_VALUE, WHEN_3,
                        Integer.MAX_VALUE);
            }

            @Nested
            public class InvalidState {
                private <VALUE> void appendTransition_2InvalidState(final Duration when1, final VALUE value1,
                                                                    final Duration when2, final VALUE value2) {
                    assert when2.compareTo(when1) <= 0 || Objects.equals(value1, value2);
                    final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<>();
                    history.appendTransition(when1, value1);

                    assertThrows(IllegalStateException.class, () -> appendTransition(history, when2, value2));
                }

                @Test
                public void timesOrder() {
                    appendTransition_2InvalidState(WHEN_2, Boolean.FALSE, WHEN_1, Boolean.TRUE);
                }

                @Test
                public void timesSame() {
                    appendTransition_2InvalidState(WHEN_1, Boolean.TRUE, WHEN_1, Boolean.FALSE);
                }

                @Test
                public void valuesEqual() {
                    // tough test: Integer.valueOf(Integer.MAX_VALUE) is probably not cached
                    appendTransition_2InvalidState(WHEN_1, Integer.MAX_VALUE, WHEN_2, Integer.MAX_VALUE);
                }

                @Test
                public void valuesSame() {
                    appendTransition_2InvalidState(WHEN_1, Boolean.FALSE, WHEN_2, Boolean.FALSE);
                }
            }// class
        }// class
    }

    @Nested
    public class Constructor {

        @Test
        public void args0() {
            final var history1 = new ModifiableValueHistory<Integer>();
            final var history2 = new ModifiableValueHistory<Integer>();

            assertAll("Invariants", () -> assertInvariants(history1), () -> assertInvariants(history1, history2));

            assertNull(history1.getFirstValue(), "The value of this history at the start of time is null.");
            assertTrue(history1.isEmpty(), "This is empty.");
            assertEquals(history1, history2, "Value semantics");

            ValueHistoryTest.assertInvariants(history1, WHEN_1);
            ValueHistoryTest.assertInvariants(history1, WHEN_2);
        }

        private <VALUE> void constructor(final ValueHistory<VALUE> that) {
            final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<>(that);

            assertInvariants(history);
            ValueHistoryTest.assertInvariants(history, that);
            assertEquals(that, history, "This equals the given value history.");

        }

        @Nested
        public class Arg1 {

            @Test
            public void a() {
                constructor_1(Boolean.FALSE);
            }

            @Test
            public void b() {
                constructor_1(Integer.MIN_VALUE);
            }

            private <VALUE> void constructor_1(final VALUE value) {
                final var history1 = new ModifiableValueHistory<>(value);
                final var history2 = new ModifiableValueHistory<>(value);

                assertAll("Invariants", () -> assertInvariants(history1), () -> assertInvariants(history1, history2));

                assertSame(value, history1.getFirstValue(),
                        "The value of this history at the start of time is the given value.");
                assertTrue(history1.isEmpty(), "This is empty.");
                assertEquals(history1, history2, "Value semantics");

                ValueHistoryTest.assertInvariants(history1, WHEN_1);
                ValueHistoryTest.assertInvariants(history1, WHEN_2);
            }

            @Test
            public void nullArg() {
                constructor_1((Boolean) null);
            }

        }// class

        @Nested
        public class Copy {
            @Test
            public void hasTransition() {
                final ModifiableValueHistory<Integer> that = new ModifiableValueHistory<>(0);
                that.appendTransition(WHEN_1, 1);

                constructor(that);
            }

            @Test
            public void nonNullAlways() {
                final ValueHistory<Boolean> that = new ModifiableValueHistory<>(Boolean.FALSE);

                constructor(that);
            }

            @Test
            public void nullAlways() {
                final ValueHistory<Boolean> that = new ModifiableValueHistory<>();

                constructor(that);
            }

        }// class

        @Nested
        public class Transitions {

            @Test
            public void none_non_nll() {
                test(0, new TreeMap<>());
            }

            @Test
            public void none_null() {
                test((Integer) null, new TreeMap<>());
            }

            @Test
            public void one() {
                final SortedMap<Duration, Integer> transitions = new TreeMap<>();
                transitions.put(WHEN_1, 1);

                test(0, transitions);
            }

            private <VALUE> void test(@Nullable final VALUE firstValue,
                                      @Nonnull final SortedMap<Duration, VALUE> transitions) {
                final var history = new ModifiableValueHistory<>(firstValue, transitions);

                assertInvariants(history);
                assertAll(() -> assertSame(firstValue, history.getFirstValue(), "firstValue"),
                        () -> assertEquals(transitions, history.getTransitions(), "transitions"));
            }

            @Test
            public void two() {
                final SortedMap<Duration, Integer> transitions = new TreeMap<>();
                transitions.put(WHEN_2, 3);
                transitions.put(WHEN_3, 2);

                test(1, transitions);
            }
        }// class
    }// class

    @Nested
    public class JSON {

        @Test
        public void empty() {
            final var history = new ModifiableValueHistory<Integer>();

            test(history);
        }

        @Test
        public void one_nonnull() {
            final var history = new ModifiableValueHistory<>(0);
            history.appendTransition(WHEN_1, 1);

            test(history);
        }

        @Test
        public void one_null() {
            final var history = new ModifiableValueHistory<Integer>();
            history.appendTransition(WHEN_1, 0);

            test(history);
        }

        private <VALUE> void test(@Nonnull final ModifiableValueHistory<VALUE> history) {
            final var deserialized = JsonTest.serializeAndDeserialize(history);

            assertInvariants(history);
            assertInvariants(history, deserialized);
            assertEquals(history, deserialized);
        }

        @Test
        public void two() {
            final var history = new ModifiableValueHistory<>(3);
            history.appendTransition(WHEN_2, 2);
            history.appendTransition(WHEN_3, 1);

            test(history);
        }
    }// class

    @Nested
    public class RemoveTransitionsFrom {

        @Test
        public void atLast() {
            final Duration when = WHEN_1;
            test(when, when);
        }

        private <VALUE> void removeTransitionsFrom(final ModifiableValueHistory<VALUE> history, final Duration when) {
            final VALUE firstValue0 = history.getFirstValue();
            final SortedMap<Duration, VALUE> transitions0 = new TreeMap<>(history.getTransitions());

            history.removeTransitionsFrom(when);

            assertInvariants(history);
            final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
            final SortedMap<Duration, VALUE> transitions = history.getTransitions();
            final Set<Entry<Duration, VALUE>> transitionsEntries = transitions.entrySet();

            assertSame(firstValue0, history.getFirstValue(), "The first value of the history is unchanged.");
            assertTrue(transitionTimes.isEmpty() || transitionTimes.last().compareTo(when) < 0,
                    "The set of state transitions contains no times at or after the given time.");
            for (final var entry0 : transitions0.entrySet()) {
                final Duration t = entry0.getKey();
                assertTrue(when.compareTo(t) <= 0 || transitionsEntries.contains(entry0),
                        "Removing state transitions from a given point  in time does not change the transitions before the point in time.");
            }
        }

        private void test(final Duration t1, final Duration t2) {
            assert t1.compareTo(t2) <= 0;
            final ModifiableValueHistory<Boolean> history = new ModifiableValueHistory<>(Boolean.FALSE);
            history.appendTransition(t2, Boolean.TRUE);
            final ModifiableValueHistory<Boolean> expected = new ModifiableValueHistory<>(Boolean.FALSE);

            removeTransitionsFrom(history, t1);

            assertEquals(expected, history, "Truncated");
        }

        @Nested
        public class AfterLastTransition {
            @Test
            public void a() {
                test(WHEN_1, WHEN_2);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3);
            }

            private void test(final Duration t1, final Duration t2) {
                assert t1.compareTo(t2) < 0;
                final ModifiableValueHistory<Boolean> history = new ModifiableValueHistory<>(Boolean.FALSE);
                history.appendTransition(t1, Boolean.TRUE);
                final ModifiableValueHistory<Boolean> history0 = new ModifiableValueHistory<>(history);

                removeTransitionsFrom(history, t2);

                assertEquals(history0, history, "Unchanged");
            }

        }// class

        @Nested
        public class BeforeLast {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3);
            }

            @Test
            public void withBefore() {
                final ModifiableValueHistory<Integer> history = new ModifiableValueHistory<>(1);
                history.appendTransition(WHEN_2, 2);
                history.appendTransition(WHEN_4, 3);

                removeTransitionsFrom(history, WHEN_3);

            }

        }// class

        @Nested
        public class Empty {
            @Test
            public void isNull() {
                final ModifiableValueHistory<Boolean> history = new ModifiableValueHistory<>();

                removeTransitionsFrom(history, WHEN_1);
            }

            @Test
            public void nonNull() {
                final ModifiableValueHistory<Boolean> history = new ModifiableValueHistory<>(Boolean.FALSE);

                removeTransitionsFrom(history, WHEN_1);
            }

        }// class

    }// class

    @Nested
    public class SetValueFrom {

        private <VALUE> void setValueFrom(final ModifiableValueHistory<VALUE> history, final Duration when,
                                          final VALUE value) {
            final VALUE firstValue0 = history.getFirstValue();

            history.setValueFrom(when, value);

            assertAll(() -> assertInvariants(history), () -> ValueHistoryTest.assertInvariants(history, when));

            final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
            assertAll(() -> assertTrue(
                    when.equals(ValueHistory.START_OF_TIME) || Objects.equals(firstValue0, history.getFirstValue()),
                    "Setting the value from a given time does not change the values before the given point in time [first value]"),
                    () -> assertEquals(value, history.get(when),
                            "The given value is equal to the value at the given time."),
                    () -> assertTrue(transitionTimes.isEmpty() || transitionTimes.last().compareTo(when) <= 0,
                            "If this has any transitions, the last transition time is at or before the given time."));
        }

        @Nested
        public class Call1 {

            @Test
            public void a() {
                setValueFrom_1(Boolean.FALSE, WHEN_1, Boolean.TRUE);
            }

            @Test
            public void b() {
                setValueFrom_1(Integer.MIN_VALUE, WHEN_2, Integer.MAX_VALUE);
            }

            @Test
            public void endOfTime() {
                setValueFrom_1(Boolean.FALSE, ValueHistory.END_OF_TIME, Boolean.TRUE);
            }

            @Test
            public void noOp() {
                setValueFrom_1(Boolean.FALSE, WHEN_1, Boolean.FALSE);
            }

            @Test
            public void noOpNull() {
                setValueFrom_1(null, WHEN_1, (Boolean) null);
            }

            @Test
            public void setNull() {
                setValueFrom_1(Boolean.FALSE, WHEN_1, null);
            }

            private <VALUE> void setValueFrom_1(final VALUE firstValue, final Duration when,
                                                final VALUE value) {
                final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<>(firstValue);

                setValueFrom(history, when, value);

            }

            @Test
            public void startOfTime() {
                setValueFrom_1(Boolean.FALSE, ValueHistory.START_OF_TIME, Boolean.TRUE);
            }

        }// class

        @Nested
        public class Call2 {
            @Test
            public void append_A() {
                setValueFrom_2(1, WHEN_1, 2, WHEN_2, 3);
            }

            @Test
            public void append_B() {
                setValueFrom_2(5, WHEN_2, 7, WHEN_3, 11);
            }

            @Test
            public void before() {
                setValueFrom_2(1, WHEN_2, 2, WHEN_1, 3);
            }

            @Test
            public void replace() {
                final Duration when = WHEN_1;
                setValueFrom_2(1, when, 2, when, 3);
            }

            private <VALUE> void setValueFrom_2(final VALUE firstValue, final Duration when1,
                                                final VALUE value1, final Duration when2, final VALUE value2) {
                final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<>(firstValue);
                history.setValueFrom(when1, value1);

                setValueFrom(history, when2, value2);
            }

        }// class
    }// class

    @Nested
    public class SetValueUntil {

        private <VALUE> void setValueUntil(final ModifiableValueHistory<VALUE> history, final Duration when,
                                           final VALUE value) {
            final VALUE lastValue0 = history.getLastValue();

            history.setValueUntil(when, value);

            assertInvariants(history);
            final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
            assertTrue(when.equals(ValueHistory.END_OF_TIME) || Objects.equals(lastValue0, history.getLastValue()),
                    "Setting the value until a given time does not change the values after the given point in time [last value]");
            assertEquals(value, history.get(when), "The given value is equal to the value at the given time.");
            assertTrue(transitionTimes.isEmpty() || when.compareTo(transitionTimes.first()) <= 0,
                    "If this has any transitions, the first transition time is at or after the given time.");
        }

        @Nested
        public class Call1 {

            @Test
            public void a() {
                setValueUntil_1(Boolean.FALSE, WHEN_1, Boolean.TRUE);
            }

            @Test
            public void b() {
                setValueUntil_1(Integer.MIN_VALUE, WHEN_2, Integer.MAX_VALUE);
            }

            @Test
            public void endOfTime() {
                setValueUntil_1(Boolean.FALSE, ValueHistory.END_OF_TIME, Boolean.TRUE);
            }

            @Test
            public void noOp() {
                setValueUntil_1(Boolean.FALSE, WHEN_1, Boolean.FALSE);
            }

            @Test
            public void noOpNull() {
                setValueUntil_1((Boolean) null, WHEN_1, null);
            }

            @Test
            public void setNull() {
                setValueUntil_1(Boolean.FALSE, WHEN_1, null);
            }

            private <VALUE> void setValueUntil_1(final VALUE firstValue, final Duration when,
                                                 final VALUE value) {
                final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<>(firstValue);

                setValueUntil(history, when, value);
            }

            @Test
            public void startOfTime() {
                setValueUntil_1(Boolean.FALSE, ValueHistory.START_OF_TIME, Boolean.TRUE);
            }

        }// class

        @Nested
        public class Call2 {

            @Test
            public void after() {
                setValueUntil_2(1, WHEN_1, 2, WHEN_2, 3);
            }

            @Test
            public void prepend_A() {
                setValueUntil_2(1, WHEN_2, 2, WHEN_1, 3);
            }

            @Test
            public void prepend_B() {
                setValueUntil_2(5, WHEN_3, 7, WHEN_2, 11);
            }

            @Test
            public void replace() {
                final Duration when = WHEN_1;
                setValueUntil_2(1, when, 2, when, 3);
            }

            private <VALUE> void setValueUntil_2(final VALUE firstValue, final Duration when1,
                                                 final VALUE value1, final Duration when2, final VALUE value2) {
                final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<>(firstValue);
                history.setValueUntil(when1, value1);

                setValueUntil(history, when2, value2);
            }

        }// class
    }// class
}
