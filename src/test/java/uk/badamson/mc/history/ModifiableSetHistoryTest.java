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

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ModifiableSetHistory} class.
 * </p>
 */
public class ModifiableSetHistoryTest {

    private static final Duration WHEN_1 = Duration.ZERO;
    private static final Duration WHEN_2 = Duration.ofSeconds(2);
    private static final Duration WHEN_3 = Duration.ofSeconds(3);

    public static <VALUE> void assertInvariants(final ModifiableSetHistory<VALUE> history) {
        ObjectVerifier.assertInvariants(history);// inherited
        SetHistoryTest.assertInvariants(history);// inherited
    }

    public static <VALUE> void assertInvariants(final ModifiableSetHistory<VALUE> history1,
                                                final ModifiableSetHistory<VALUE> history2) {
        ObjectVerifier.assertInvariants(history1, history2);// inherited
        SetHistoryTest.assertInvariants(history1, history2);// inherited
    }

    public static <VALUE> void assertInvariants(final ModifiableSetHistory<VALUE> history, final VALUE value) {
        SetHistoryTest.assertInvariants(history, value);
    }

    private static <VALUE> void remove(final ModifiableSetHistory<VALUE> history, final VALUE value) {
        history.remove(value);

        assertInvariants(history);
        assertInvariants(history, value);
        final var contains = history.contains(value);
        assertAll("This history does not contain the given value at any points in time.",
                () -> assertSame(Boolean.FALSE, contains.getFirstValue(), "[start of time]."),
                () -> assertTrue(contains.isEmpty(), "[no transitions]."));
        assertFalse(history.getUniverse().contains(value), "The universe of this time varying set does not contain the given value.");
    }

    @Test
    public void constructor_0() {
        final var history1 = new ModifiableSetHistory<Integer>();
        final var history2 = new ModifiableSetHistory<Integer>();

        assertInvariants(history1);
        assertInvariants(history1, history2);

        assertEquals(Collections.EMPTY_SET, history1.getTransitionTimes(), "This has no transition times.");
        assertEquals(history1, history2, "Value semantics");

        ValueHistoryTest.assertInvariants(history1, WHEN_1);
        ValueHistoryTest.assertInvariants(history1, WHEN_2);
        assertInvariants(history1, (Integer) null);
        assertInvariants(history1, Integer.MIN_VALUE);
    }

    @Nested
    public class AddFrom {

        private <VALUE> void addFrom(final ModifiableSetHistory<VALUE> history, final Duration when,
                                     final VALUE value) {
            history.addFrom(when, value);

            assertInvariants(history);
            assertInvariants(history, value);
            final ValueHistory<Boolean> contains = history.contains(value);
            assertSame(Boolean.TRUE, contains.getLastValue(),
                    "The last value of the contains history for the given value is TRUE.");
            assertSame(Boolean.TRUE, contains.get(when),
                    "The value at the given time of the contains history for the given value is TRUE.");
            final var lastTransitionTime = contains.getLastTransitionTime();
            assertTrue(lastTransitionTime == null || lastTransitionTime.compareTo(when) <= 0,
                    "The contains history for the given value has its last transition time is at or before the given time.");
        }

        @Nested
        public class Call1 {

            @Test
            public void a() {
                addFrom_1(WHEN_1, Integer.MIN_VALUE);
            }

            private <VALUE> void addFrom_1(final Duration when, final VALUE value) {
                final Map<Duration, Set<VALUE>> expectedTransitions = Collections.singletonMap(when,
                        Collections.singleton(value));
                final Map<Duration, Boolean> expectedContainsTransitions = Collections.singletonMap(when, Boolean.TRUE);
                final ModifiableSetHistory<VALUE> history0 = new ModifiableSetHistory<>();
                final ModifiableSetHistory<VALUE> history1 = new ModifiableSetHistory<>();
                final ModifiableSetHistory<VALUE> history2 = new ModifiableSetHistory<>();
                history2.addFrom(when, value);

                addFrom(history1, when, value);

                assertAll("Invariants", () -> assertInvariants(history0, history1),
                        () -> assertInvariants(history1, history2));

                assertEquals(Collections.EMPTY_SET, history1.getFirstValue(), "Set at start of time");
                assertEquals(expectedTransitions, ValueHistoryTest.getTransitionValues(history1), "Transitions");
                assertEquals(expectedContainsTransitions,
                        ValueHistoryTest.getTransitionValues(history1.contains(value)), "contains transitions");

                assertAll("Value semantics", () -> assertNotEquals(history0, history1),
                        () -> assertEquals(history1, history2));
            }

            @Test
            public void b() {
                addFrom_1(WHEN_2, "value");
            }

        }// class

        @Nested
        public class Call2 {

            private <VALUE> void addFrom_2_differentValues(final Duration when1, final VALUE value1,
                                                           final Duration when2, final VALUE value2) {
                assert when1.compareTo(when2) < 0;
                final Set<Duration> expectedTransitionTimes = Set.of(when1, when2);
                final Map<Duration, Set<VALUE>> expectedTransitions = Map.of(when1, Collections.singleton(value1),
                        when2, Set.of(value1, value2));
                final Map<Duration, Boolean> expectedContainsTransitions1 = Collections.singletonMap(when1,
                        Boolean.TRUE);
                final Map<Duration, Boolean> expectedContainsTransitions2 = Collections.singletonMap(when2,
                        Boolean.TRUE);

                final ModifiableSetHistory<VALUE> history = new ModifiableSetHistory<>();
                history.addFrom(when1, value1);

                addFrom(history, when2, value2);

                assertEquals(Collections.EMPTY_SET, history.getFirstValue(), "Set at start of time");
                assertEquals(expectedTransitionTimes, history.getTransitionTimes(), "transitionTimes");
                assertEquals(expectedTransitions, ValueHistoryTest.getTransitionValues(history), "Transitions");

                assertAll("contains transitions",
                        () -> assertEquals(expectedContainsTransitions1,
                                ValueHistoryTest.getTransitionValues(history.contains(value1)), "[1]"),
                        () -> assertEquals(expectedContainsTransitions2,
                                ValueHistoryTest.getTransitionValues(history.contains(value2)), "[2]"));
            }

            private <VALUE> void addFrom_2_sameValue(final Duration when1, final Duration when2, final VALUE value) {
                final Duration whenEarliest = when1.compareTo(when2) <= 0 ? when1 : when2;
                final Map<Duration, Set<VALUE>> expectedTransitions = Collections.singletonMap(whenEarliest,
                        Collections.singleton(value));
                final Map<Duration, Boolean> expectedContainsTransitions = Collections.singletonMap(whenEarliest,
                        Boolean.TRUE);

                final ModifiableSetHistory<VALUE> history = new ModifiableSetHistory<>();
                history.addFrom(when1, value);

                addFrom(history, when2, value);

                assertEquals(Collections.EMPTY_SET, history.getFirstValue(), "Set at start of time");
                assertEquals(expectedTransitions, ValueHistoryTest.getTransitionValues(history), "Transitions");
                assertEquals(expectedContainsTransitions, ValueHistoryTest.getTransitionValues(history.contains(value)),
                        "contains transitions");
            }

            @Test
            public void differentValues_A() {
                addFrom_2_differentValues(WHEN_1, 1, WHEN_2, 2);
            }

            @Test
            public void differentValues_B() {
                addFrom_2_differentValues(WHEN_1, 2, WHEN_2, 1);
            }

            @Test
            public void differentValues_C() {
                addFrom_2_differentValues(WHEN_2, "value 1", WHEN_3, "value 2");
            }

            @Test
            public void sameValue_A() {
                addFrom_2_sameValue(WHEN_1, WHEN_2, Integer.MIN_VALUE);
            }

            @Test
            public void sameValue_B() {
                addFrom_2_sameValue(WHEN_2, WHEN_3, Integer.MAX_VALUE);
            }

            @Test
            public void sameValue_C() {
                addFrom_2_sameValue(WHEN_2, WHEN_1, Integer.MIN_VALUE);
            }

            @Test
            public void sameValue_sameTime() {
                final Duration when = WHEN_1;
                addFrom_2_sameValue(when, when, Integer.MIN_VALUE);
            }

        }// class
    }// class

    @Nested
    public class AddUntil {

        private <VALUE> void addUntil(final ModifiableSetHistory<VALUE> history, final Duration when,
                                      final VALUE value) {
            history.addUntil(when, value);

            assertInvariants(history);
            assertInvariants(history, value);
            final ValueHistory<Boolean> contains = history.contains(value);
            assertSame(Boolean.TRUE, contains.getFirstValue(),
                    "The first value of the contains history for the given value is TRUE.");
            assertSame(Boolean.TRUE, contains.get(when),
                    "The value at the given time of the contains history for the given value is TRUE.");
            final var firstTransitionTime = contains.getFirstTransitionTime();
            assertTrue(firstTransitionTime == null || when.compareTo(firstTransitionTime) < 0,
                    "The contains history for the given value has its first transition time after the given time.");
        }

        @Nested
        public class Call1 {

            private <VALUE> void addUntil_1(final Duration when, final VALUE value) {
                final Duration justAfter = when.plusNanos(1L);
                final Map<Duration, Set<VALUE>> expectedTransitions = Collections.singletonMap(justAfter,
                        Collections.emptySet());
                final Map<Duration, Boolean> expectedContainsTransitions = Collections.singletonMap(justAfter,
                        Boolean.FALSE);
                final ModifiableSetHistory<VALUE> history0 = new ModifiableSetHistory<>();
                final ModifiableSetHistory<VALUE> history1 = new ModifiableSetHistory<>();
                final ModifiableSetHistory<VALUE> history2 = new ModifiableSetHistory<>();
                history2.addUntil(when, value);

                addUntil(history1, when, value);

                assertInvariants(history0, history1);
                assertInvariants(history1, history2);

                assertEquals(Collections.EMPTY_SET, history1.getLastValue(), "Set at end of time");
                assertEquals(expectedTransitions, ValueHistoryTest.getTransitionValues(history1), "Transitions");
                assertEquals(expectedContainsTransitions,
                        ValueHistoryTest.getTransitionValues(history1.contains(value)), "contains transitions");

                assertAll("Value semantics", () -> assertNotEquals(history0, history1),
                        () -> assertEquals(history1, history2));
            }

            @Test
            public void addUntil_1A() {
                addUntil_1(WHEN_1, Integer.MIN_VALUE);
            }

            @Test
            public void addUntil_1B() {
                addUntil_1(WHEN_2, "value");
            }

        }// class

        @Nested
        public class Call2 {

            private <VALUE> void addUntil_2_differentValues(final Duration when1, final VALUE value1,
                                                            final Duration when2, final VALUE value2) {
                assert when1.compareTo(when2) < 0;
                final Duration justAfter1 = when1.plusNanos(1L);
                final Duration justAfter2 = when2.plusNanos(1L);
                final Set<Duration> expectedTransitionTimes = Set.of(justAfter1, justAfter2);
                final Map<Duration, Set<VALUE>> expectedTransitions = Map.of(justAfter1, Collections.singleton(value2),
                        justAfter2, Collections.emptySet());
                final Map<Duration, Boolean> expectedContainsTransitions1 = Collections.singletonMap(justAfter1,
                        Boolean.FALSE);
                final Map<Duration, Boolean> expectedContainsTransitions2 = Collections.singletonMap(justAfter2,
                        Boolean.FALSE);

                final ModifiableSetHistory<VALUE> history = new ModifiableSetHistory<>();
                history.addUntil(when1, value1);

                addUntil(history, when2, value2);

                assertEquals(Set.of(value1, value2), history.getFirstValue(), "Set at start of time");
                assertEquals(expectedTransitionTimes, history.getTransitionTimes(), "transitionTimes");
                assertEquals(expectedTransitions, ValueHistoryTest.getTransitionValues(history), "Transitions");

                assertAll("contains transitions",
                        () -> assertEquals(expectedContainsTransitions1,
                                ValueHistoryTest.getTransitionValues(history.contains(value1)), "[1]"),
                        () -> assertEquals(expectedContainsTransitions2,
                                ValueHistoryTest.getTransitionValues(history.contains(value2)), "[2]"));
            }

            private <VALUE> void addUntil_2_sameValue(final Duration when1, final Duration when2, final VALUE value) {
                final Duration whenLatest = when1.compareTo(when2) <= 0 ? when2 : when1;
                final Duration justAfter = whenLatest.plusNanos(1L);
                final Map<Duration, Set<VALUE>> expectedTransitions = Collections.singletonMap(justAfter,
                        Collections.emptySet());
                final Map<Duration, Boolean> expectedContainsTransitions = Collections.singletonMap(justAfter,
                        Boolean.FALSE);

                final ModifiableSetHistory<VALUE> history = new ModifiableSetHistory<>();
                history.addUntil(when1, value);

                addUntil(history, when2, value);

                assertEquals(Collections.singleton(value), history.getFirstValue(), "Set at start of time");
                assertEquals(expectedTransitions, ValueHistoryTest.getTransitionValues(history), "Transitions");
                assertEquals(expectedContainsTransitions, ValueHistoryTest.getTransitionValues(history.contains(value)),
                        "contains transitions");
            }

            @Test
            public void differentValues_A() {
                addUntil_2_differentValues(WHEN_1, 1, WHEN_2, 2);
            }

            @Test
            public void differentValues_B() {
                addUntil_2_differentValues(WHEN_1, 2, WHEN_2, 1);
            }

            @Test
            public void differentValues_C() {
                addUntil_2_differentValues(WHEN_2, "value 1", WHEN_3, "value 2");
            }

            @Test
            public void sameValue_A() {
                addUntil_2_sameValue(WHEN_1, WHEN_2, Integer.MIN_VALUE);
            }

            @Test
            public void sameValue_B() {
                addUntil_2_sameValue(WHEN_2, WHEN_3, Integer.MAX_VALUE);
            }

            @Test
            public void sameValue_C() {
                addUntil_2_sameValue(WHEN_2, WHEN_1, Integer.MIN_VALUE);
            }

            @Test
            public void sameValue_sameTime() {
                final Duration when = WHEN_1;
                addUntil_2_sameValue(when, when, Integer.MIN_VALUE);
            }
        }// class
    }// class

    @Nested
    public class Remove {

        @Test
        public void absentA() {
            remove_absent(WHEN_1, 1, 2);
        }

        @Test
        public void absentB() {
            remove_absent(WHEN_2, 13, 7);
        }

        @Test
        public void empty() {
            final ModifiableSetHistory<Integer> history = new ModifiableSetHistory<>();

            remove(history, 1);
        }

        @Test
        public void presentFromA() {
            remove_presentFrom(WHEN_1, 1);
        }

        @Test
        public void presentFromB() {
            remove_presentFrom(WHEN_2, 2);
        }

        @Test
        public void presentUntilA() {
            remove_presentUntil(WHEN_1, 1);
        }

        @Test
        public void presentUntilB() {
            remove_presentUntil(WHEN_2, 2);
        }

        private void remove_absent(final Duration when, final Integer value1, final Integer value2) {
            final ModifiableSetHistory<Integer> history = new ModifiableSetHistory<>();
            history.addFrom(when, value1);
            final var contains10 = new ModifiableValueHistory<>(history.contains(value1));

            remove(history, value2);

            assertEquals(contains10, history.contains(value1),
                    "Whether this history contains other values is unchanged [1].");
        }

        private void remove_presentFrom(final Duration when, final Integer value) {
            final ModifiableSetHistory<Integer> history = new ModifiableSetHistory<>();
            history.addFrom(when, value);

            remove(history, value);
        }

        private void remove_presentUntil(final Duration when, final Integer value) {
            final ModifiableSetHistory<Integer> history = new ModifiableSetHistory<>();
            history.addUntil(when, value);

            remove(history, value);
        }

    }// class
}
