package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2021.
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import uk.badamson.mc.JsonTest;
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.history.TimestampedValue;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.history.ValueHistoryTest;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ObjectHistoryTest {

    @Nested
    public class Constructor {

        @Nested
        public class Copy {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(1));
            }

            private <STATE> void test(@Nonnull final UUID object, @Nonnull final Duration start,
                    @Nonnull final STATE state) {
                final var history = new ObjectHistory<>(object, start, state);

                constructor(history);
            }

        }// class

        @Nested
        public class History {

            @Nested
            public class OneStateTransition {
                @Test
                public void far() {
                    test(OBJECT_A, WHEN_A, WHEN_A.plusDays(365), Integer.valueOf(0));
                }

                @Test
                public void near() {
                    final var start = WHEN_B;
                    final var end = start.plusNanos(1);// critical
                    test(OBJECT_A, start, end, Integer.valueOf(3));
                }

                private void test(final UUID object, final Duration start, final Duration end, final Integer state) {
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                    stateTransitions.put(start, state);

                    constructor(object, end, stateTransitions);
                }

            }// class

            @Nested
            public class Two {

                @Test
                public void differentEnd() {
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>(
                            Map.of(WHEN_A, Integer.valueOf(-1)));
                    final var endA = WHEN_A.plusMillis(10);
                    final var endB = WHEN_A.plusMillis(20);

                    final var historyA = new ObjectHistory<>(OBJECT_A, endA, stateTransitions);
                    final var historyB = new ObjectHistory<>(OBJECT_A, endB, stateTransitions);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentObject() {
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>(
                            Map.of(WHEN_A, Integer.valueOf(-1)));
                    final var end = WHEN_A.plusMillis(10);

                    final var historyA = new ObjectHistory<>(OBJECT_A, end, stateTransitions);
                    final var historyB = new ObjectHistory<>(OBJECT_B, end, stateTransitions);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentstateTransitions() {
                    final Duration start = WHEN_A;
                    final SortedMap<Duration, Integer> stateTransitionsA = new TreeMap<>(
                            Map.of(start, Integer.valueOf(-1)));
                    final SortedMap<Duration, Integer> stateTransitionsB = new TreeMap<>(
                            Map.of(start, Integer.valueOf(-2)));
                    final var end = start.plusMillis(10);
                    assert !stateTransitionsA.equals(stateTransitionsB);

                    final var historyA = new ObjectHistory<>(OBJECT_A, end, stateTransitionsA);
                    final var historyB = new ObjectHistory<>(OBJECT_A, end, stateTransitionsB);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void equivalent() {
                    final var objectA = OBJECT_A;
                    final var objectB = new UUID(objectA.getMostSignificantBits(), objectA.getLeastSignificantBits());
                    final long endMillis = 6000;
                    final var endA = Duration.ofMillis(endMillis);
                    final var endB = Duration.ofMillis(endMillis);
                    final SortedMap<Duration, Integer> stateTransitionsA = new TreeMap<>(
                            Map.of(Duration.ofMillis(5000), Integer.valueOf(Integer.MAX_VALUE)));
                    final SortedMap<Duration, Integer> stateTransitionsB = new TreeMap<>(stateTransitionsA);

                    assert objectA.equals(objectB);
                    assert endA.equals(endB);
                    assert stateTransitionsA.equals(stateTransitionsB);
                    assert objectA != objectB;// tough test
                    assert endA != endB;// tough test
                    assert stateTransitionsA != stateTransitionsB;// tough test

                    final var historyA = new ObjectHistory<>(OBJECT_A, endA, stateTransitionsA);
                    final var historyB = new ObjectHistory<>(OBJECT_A, endA, stateTransitionsA);

                    assertInvariants(historyA, historyB);
                    assertThat("equals", historyA, is(historyB));
                }

            }// class

            @Test
            public void twoStateTransitions() {
                final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                stateTransitions.put(WHEN_A, Integer.valueOf(0));
                stateTransitions.put(WHEN_A.plusSeconds(1), Integer.valueOf(1));

                constructor(OBJECT_A, WHEN_A.plusSeconds(2), stateTransitions);
            }

        }// class

        @Nested
        public class InitialState {

            @Nested
            public class Two {

                @Test
                public void differentObject() {
                    final var state = Integer.valueOf(0);
                    final var historyA = new ObjectHistory<>(OBJECT_A, WHEN_A, state);
                    final var historyB = new ObjectHistory<>(OBJECT_B, WHEN_A, state);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentStart() {
                    final var state = Integer.valueOf(0);
                    final var historyA = new ObjectHistory<>(OBJECT_A, WHEN_A, state);
                    final var historyB = new ObjectHistory<>(OBJECT_A, WHEN_B, state);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentState() {
                    final var historyA = new ObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(0));
                    final var historyB = new ObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(1));

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void equivalent() {
                    final var objectA = OBJECT_A;
                    final var objectB = new UUID(objectA.getMostSignificantBits(), objectA.getLeastSignificantBits());
                    final long startMillis = 6000;
                    final var startA = Duration.ofMillis(startMillis);
                    final var startB = Duration.ofMillis(startMillis);
                    final var stateA = Integer.valueOf(Integer.MAX_VALUE);
                    final var stateB = Integer.valueOf(Integer.MAX_VALUE);

                    assert objectA.equals(objectB);
                    assert startA.equals(startB);
                    assert stateA.equals(stateB);
                    assert objectA != objectB;// tough test
                    assert startA != startB;// tough test
                    assert stateA != stateB;// tough test

                    final var historyA = new ObjectHistory<>(objectA, startA, stateA);
                    final var historyB = new ObjectHistory<>(objectB, startB, stateB);

                    assertInvariants(historyA, historyB);
                    assertThat("equals", historyA, is(historyB));
                }
            }// class

            @Test
            public void a() {
                constructor(OBJECT_A, WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                constructor(OBJECT_B, WHEN_B, Integer.valueOf(1));
            }

        }// class

    }// class

    @Nested
    public class JSON {

        @Nested
        public class OneTransition {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(1));
            }

            private <STATE> void test(@Nonnull final UUID object, @Nonnull final Duration start,
                    @Nonnull final STATE state) {
                final var history = new ObjectHistory<>(object, start, state);
                final var deserialized = JsonTest.serializeAndDeserialize(history);

                assertInvariants(history);
                assertInvariants(history, deserialized);
                assertAll(() -> assertThat("equals", deserialized, is(history)),
                        () -> assertEquals(history.getObject(), deserialized.getObject(), "object"),
                        () -> assertEquals(history.getStart(), deserialized.getStart(), "start"),
                        () -> assertEquals(history.getEnd(), deserialized.getEnd(), "end"),
                        () -> assertEquals(history.getStateHistory(), deserialized.getStateHistory(), "stateHistory"));
            }

        }// class

    }// class

    @Nested
    public class ObserveState {

        @Nested
        public class AtStart {

            @Test
            public void a() {
                test(WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(WHEN_B, Integer.valueOf(1));
            }

            private <STATE> void test(@Nonnull final Duration start, @Nonnull final STATE state) {
                final Optional<STATE> expectedState = Optional.of(state);
                final var history = new ObjectHistory<>(OBJECT_A, start, state);
                final Duration when = start;

                final var states = observeState(history, when);

                StepVerifier.create(states).expectNext(expectedState).expectComplete().verify();
            }

        }// class

        @Nested
        public class BeforeStart {

            @Test
            public void far() {
                final Duration start = WHEN_B;
                final Duration when = start.minusDays(365);
                test(start, when);
            }

            @Test
            public void near() {
                final Duration start = WHEN_A;
                final Duration when = start.minusNanos(1);// tough test
                test(start, when);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration when) {
                assert when.compareTo(start) < 0;
                final var expectedState = Optional.<Integer>empty();
                final var history = new ObjectHistory<>(OBJECT_A, start, Integer.valueOf(0));

                final var states = observeState(history, when);

                StepVerifier.create(states).expectNext(expectedState).expectComplete().verify();
            }

        }// class

        @Nested
        public class Provisional {

            @Test
            public void far() {
                final Duration time0 = WHEN_B;
                final Duration when = time0.plusDays(365);

                test(time0, when, Integer.valueOf(1));
            }

            @Test
            public void near() {
                final Duration time0 = WHEN_A;
                final Duration when = time0.plusNanos(1);// critical

                test(time0, when, Integer.valueOf(0));
            }

            private void test(@Nonnull final Duration end, @Nonnull final Duration when, @Nonnull final Integer state) {
                assert end.compareTo(when) < 0;// provisional
                final var expectedState = Optional.of(state);
                final var history = new ObjectHistory<>(OBJECT_A, end, state);

                final var states = observeState(history, when);

                StepVerifier.create(states).expectNext(expectedState).expectTimeout(Duration.ofMillis(100)).verify();
            }

        }// class

    }// class

    @Nested
    public class ObserveStateTransitions {

        @Nested
        public class AfterConstructorGivenHistory {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, WHEN_A.plusSeconds(5), Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, WHEN_B.plusDays(5), Integer.valueOf(1));
            }

            private void test(final UUID object, final Duration start, final Duration end, final Integer state) {
                final var expectedStateTransition = new TimestampedValue<>(start, state);
                final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                stateTransitions.put(start, state);
                final var history = new ObjectHistory<>(object, end, stateTransitions);

                final var flux = observeStateTransitions(history);

                StepVerifier.create(flux).expectNext(expectedStateTransition).expectTimeout(Duration.ofMillis(100))
                        .verify();
            }

        }// class

        @Nested
        public class AfterConstructorGivenInitialState {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(1));
            }

            private void test(final UUID object, final Duration start, final Integer state) {
                final var expectedStateTransition = new TimestampedValue<>(start, state);
                final var history = new ObjectHistory<>(object, start, state);

                final var flux = observeStateTransitions(history);

                StepVerifier.create(flux).expectNext(expectedStateTransition).expectTimeout(Duration.ofMillis(100))
                        .verify();
            }

        }// class

        @Nested
        public class AfterCopyConstructor {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(1));
            }

            private void test(final UUID object, final Duration start, final Integer state) {
                final var expectedStateTransition = new TimestampedValue<>(start, state);
                final var history = new ObjectHistory<>(object, start, state);
                final var copy = new ObjectHistory<>(history);

                final var flux = observeStateTransitions(copy);

                StepVerifier.create(flux).expectNext(expectedStateTransition).expectTimeout(Duration.ofMillis(100))
                        .verify();
            }

        }// class
    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history) {
        ObjectTest.assertInvariants(history);// inherited

        final var object = history.getObject();
        final var start = history.getStart();
        final var end = history.getEnd();
        final var stateHistory = history.getStateHistory();
        final var stateTransitions = history.getStateTransitions();

        assertAll("Not null", () -> assertNotNull(object, "object"), () -> assertNotNull(start, "start"), // guard
                () -> assertNotNull(end, "end"), // guard
                () -> assertNotNull(stateHistory, "stateHistory"), // guard
                () -> assertNotNull(stateTransitions, "stateTransitions")// guard
        );
        ValueHistoryTest.assertInvariants(stateHistory);
        assertAll(() -> assertThat("The end time is at or after the start time.", end, greaterThanOrEqualTo(start)),
                () -> assertAll("stateHistory", () -> assertSame(start, stateHistory.getFirstTansitionTime(),
                        "The first transition time of the state history is the same as the start time of this history."),
                        () -> assertNull(stateHistory.getFirstValue(),
                                "The state at the start of time of the state history is null."),
                        () -> assertFalse(stateHistory.isEmpty(), "The state history is never empty."),
                        () -> assertThat(
                                "If reliable state information indicates that the simulated object was destroyed, it is guaranteed that the simulated object will never be recreated.",
                                !(stateHistory.get(end) == null && !ValueHistory.END_OF_TIME.equals(end)))),
                () -> assertEquals(stateTransitions, stateHistory.getTransitions(), "stateTransitions"));
    }

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history1,
            @Nonnull final ObjectHistory<STATE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited

        assertTrue(history1.equals(history2) == (history1.getStateTransitions().equals(history2.getStateTransitions())
                && history1.getObject().equals(history2.getObject()) && history1.getEnd().equals(history2.getEnd())),
                "value semantics");
    }

    private static <STATE> void constructor(@Nonnull final ObjectHistory<STATE> that) {
        final var copy = new ObjectHistory<>(that);

        assertInvariants(copy);
        assertInvariants(copy, that);
        assertEquals(copy, that);
        assertAll("Copied", () -> assertSame(that.getEnd(), copy.getEnd(), "end"),
                () -> assertSame(that.getObject(), copy.getObject(), "object"),
                () -> assertSame(that.getStart(), copy.getStart(), "start"),
                () -> assertEquals(that.getStateHistory(), copy.getStateHistory(), "stateHistory"));
    }

    private static <STATE> void constructor(@Nonnull final UUID object, @Nonnull final Duration end,
            @Nonnull final SortedMap<Duration, STATE> stateTransitions) {
        final var history = new ObjectHistory<>(object, end, stateTransitions);

        assertInvariants(history);
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(end, history.getEnd(), "end"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, history.getStateTransitions(), "stateTransitions"));
    }

    private static <STATE> void constructor(@Nonnull final UUID object, @Nonnull final Duration start,
            @Nonnull final STATE state) {
        final var history = new ObjectHistory<>(object, start, state);

        assertInvariants(history);
        final var stateTransitions = history.getStateTransitions();
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(start, history.getStart(), "start"), () -> assertSame(start, history.getEnd(), "end"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, Map.of(start, state), "stateTransitions"));
    }

    public static <STATE> Publisher<Optional<STATE>> observeState(@Nonnull final ObjectHistory<STATE> history,
            @Nonnull final Duration when) {
        final var states = history.observeState(when);

        assertInvariants(history);
        assertNotNull(states, "Not null, states");// guard

        return states;
    }

    public static <STATE> Flux<TimestampedValue<STATE>> observeStateTransitions(
            @Nonnull final ObjectHistory<STATE> history) {
        final var flux = history.observeStateTransitions();

        assertInvariants(history);
        assertNotNull(flux, "Not null, result");
        return flux;
    }
}// class