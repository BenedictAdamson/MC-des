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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.reactivestreams.Publisher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import uk.badamson.dbc.assertions.EqualsSemanticsTest;
import uk.badamson.dbc.assertions.ObjectTest;
import uk.badamson.mc.JsonTest;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.history.ValueHistoryTest;
import uk.badamson.mc.simulation.ObjectStateId;
import uk.badamson.mc.simulation.actor.ObjectHistory.TimestampedState;

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
        public class GivenHistoryAndSignals {

            @Nested
            public class OneSignal {

                @Test
                public void sentAfterEnd() {
                    final var start = WHEN_B;
                    final var end = WHEN_C;
                    final var sentFrom = new ObjectStateId(OBJECT_A, end.plusSeconds(5));
                    test(OBJECT_B, start, end, SIGNAL_ID_B, sentFrom);
                }

                @Test
                public void sentAtEnd() {
                    final var start = WHEN_A;
                    final var end = WHEN_B;
                    final var sentFrom = new ObjectStateId(OBJECT_B, end);
                    test(OBJECT_A, start, end, SIGNAL_ID_A, sentFrom);
                }

                private void test(@Nonnull final UUID object, @Nonnull final Duration start,
                        @Nonnull final Duration end, @Nonnull final UUID signalId,
                        @Nonnull final ObjectStateId sentFrom) {
                    final Integer state = Integer.valueOf(0);
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                    stateTransitions.put(start, state);
                    final Signal<Integer> signal = new SignalTest.TestSignal(signalId, sentFrom, object);
                    final Collection<Signal<Integer>> signals = List.of(signal);

                    constructor(object, end, stateTransitions, signals);
                }

            }// class

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
                final Collection<Signal<Integer>> signals = List.of();

                constructor(OBJECT_A, WHEN_A.plusSeconds(2), stateTransitions, signals);
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
    public class ObserveTimestampedStates {

        @Nested
        public class AfterConstructorGivenHistory {

            @Nested
            public class ReliablyDestroyed {

                @Test
                public void far() {
                    final Duration start = WHEN_B;
                    final Duration destructionTime = start.plusDays(365);

                    test(start, destructionTime, Integer.valueOf(1));
                }

                @Test
                public void near() {
                    final Duration start = WHEN_A;
                    final Duration destructionTime = start.plusNanos(1);// critical

                    test(start, destructionTime, Integer.valueOf(0));
                }

                private void test(final Duration start, final Duration destructionTime, final Integer state0) {
                    assert start.compareTo(destructionTime) < 0;
                    assert state0 != null;
                    final UUID object = OBJECT_A;
                    final Duration end = ValueHistory.END_OF_TIME;// critical
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                    stateTransitions.put(start, state0);
                    stateTransitions.put(destructionTime, null);
                    final var history = new ObjectHistory<>(object, end, stateTransitions);

                    final var flux = observeTimestampedStates(history);

                    assertNoMoreTimestampedStates(flux);
                }

            }// class

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, WHEN_A.plusSeconds(5), Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, WHEN_B.plusDays(5), Integer.valueOf(1));
            }

            private void test(final UUID object, final Duration start, final Duration end, final Integer state) {
                final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                stateTransitions.put(start, state);
                final var history = new ObjectHistory<>(object, end, stateTransitions);

                final var flux = observeTimestampedStates(history);

                StepVerifier.create(flux).expectTimeout(Duration.ofMillis(100)).verify();
            }

        }// class

        @Nested
        public class AfterConstructorGivenInitialState {

            @Nested
            public class MoreStatesPossible {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, Integer.valueOf(0));
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, Integer.valueOf(1));
                }

                private void test(final UUID object, final Duration start, final Integer state) {
                    final var history = new ObjectHistory<>(object, start, state);

                    final var flux = observeTimestampedStates(history);

                    StepVerifier.create(flux).expectTimeout(Duration.ofMillis(100)).verify();
                }

            }// class

            @Test
            public void noMoreStatesPossible() {
                final Duration start = ValueHistory.END_OF_TIME;// critical
                final var history = new ObjectHistory<>(OBJECT_A, start, Integer.valueOf(0));

                final var flux = observeTimestampedStates(history);

                assertNoMoreTimestampedStates(flux);
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

            @Test
            public void reliablyDestroyed() {
                final UUID object = OBJECT_A;
                final Duration start = WHEN_A;
                final Duration end = ValueHistory.END_OF_TIME;// critical
                final Duration destructionTime = start.plusDays(365);
                final Integer state0 = Integer.valueOf(1);

                final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                stateTransitions.put(start, state0);
                stateTransitions.put(destructionTime, null);
                final var history = new ObjectHistory<>(object, end, stateTransitions);
                final var copy = new ObjectHistory<>(history);

                final var flux = observeTimestampedStates(copy);

                assertNoMoreTimestampedStates(flux);
            }

            private void test(final UUID object, final Duration start, final Integer state) {
                final var history = new ObjectHistory<>(object, start, state);
                final var copy = new ObjectHistory<>(history);

                final var flux = observeTimestampedStates(copy);

                StepVerifier.create(flux).expectTimeout(Duration.ofMillis(100)).verify();
            }

        }// class
    }// class

    public static class TimestampedStateTest {

        @Nested
        public class Constructor {

            @Nested
            public class Two {

                @Test
                public void differentEnd() {
                    final Duration start = Duration.ofMillis(1000);
                    final Duration endA = Duration.ofMillis(3000);
                    final Duration endB = Duration.ofMillis(4000);
                    final String state = "State";
                    final boolean reliable = true;

                    final var timestampedA = new ObjectHistory.TimestampedState<>(start, endA, reliable, state);
                    final var timestampedB = new ObjectHistory.TimestampedState<>(start, endB, reliable, state);

                    assertInvariants(timestampedA, timestampedB);
                    assertNotEquals(timestampedA, timestampedB);
                }

                @Test
                public void differentReliable() {
                    final String state = "State";
                    final boolean reliableA = true;
                    final boolean reliableB = false;

                    final var timestampedA = new ObjectHistory.TimestampedState<>(WHEN_A, WHEN_B, reliableA, state);
                    final var timestampedB = new ObjectHistory.TimestampedState<>(WHEN_A, WHEN_B, reliableB, state);

                    assertInvariants(timestampedA, timestampedB);
                    assertNotEquals(timestampedA, timestampedB);
                }

                @Test
                public void differentStart() {
                    final Duration startA = Duration.ofMillis(1000);
                    final Duration startB = Duration.ofMillis(2000);
                    final Duration end = Duration.ofMillis(3000);
                    final String state = "State";
                    final boolean reliable = true;

                    final var timestampedA = new ObjectHistory.TimestampedState<>(startA, end, reliable, state);
                    final var timestampedB = new ObjectHistory.TimestampedState<>(startB, end, reliable, state);

                    assertInvariants(timestampedA, timestampedB);
                    assertNotEquals(timestampedA, timestampedB);
                }

                @Test
                public void differentState() {
                    final String stateA = "A";
                    final String stateB = "B";
                    final boolean reliable = true;

                    final var timestampedA = new ObjectHistory.TimestampedState<>(WHEN_A, WHEN_B, reliable, stateA);
                    final var timestampedB = new ObjectHistory.TimestampedState<>(WHEN_A, WHEN_B, reliable, stateB);

                    assertInvariants(timestampedA, timestampedB);
                    assertNotEquals(timestampedA, timestampedB);
                }

                @Test
                public void equivalent() {
                    final Duration startA = Duration.ofMillis(1000);
                    final Duration startB = Duration.ofMillis(1000);
                    final Duration endA = Duration.ofMillis(3000);
                    final Duration endB = Duration.ofMillis(3000);
                    final String stateA = "State";
                    final String stateB = new String(stateA);
                    final boolean reliable = true;
                    assert startA.equals(startB);
                    assert endA.equals(endB);
                    assert stateA.equals(stateB);
                    assert startA != startB;// tough test
                    assert endA != endB;// tough test
                    assert stateA != stateB;// tough test

                    final var timestampedA = new ObjectHistory.TimestampedState<>(startA, endA, reliable, stateA);
                    final var timestampedB = new ObjectHistory.TimestampedState<>(startB, endB, reliable, stateB);

                    assertInvariants(timestampedA, timestampedB);
                    assertEquals(timestampedA, timestampedB);
                }
            }// class

            @Test
            public void nullState() {
                constructor(WHEN_A, WHEN_B, true, (Integer) null);
            }

            @Test
            public void provisional() {
                constructor(WHEN_B, WHEN_C, false, Integer.valueOf(0));
            }

            @Test
            public void reliable() {
                constructor(WHEN_A, WHEN_B, true, "State");
            }

        }// class

        public static <STATE> void assertInvariants(@Nonnull final ObjectHistory.TimestampedState<STATE> timestamped) {
            ObjectTest.assertInvariants(timestamped);// inherited

            assertNotNull(timestamped.getStart(), "Not null, start");
        }

        public static <STATE> void assertInvariants(@Nonnull final ObjectHistory.TimestampedState<STATE> timestamped1,
                @Nonnull final ObjectHistory.TimestampedState<STATE> timestamped2) {
            ObjectTest.assertInvariants(timestamped1, timestamped2);// inherited

            final var state1 = timestamped1.getState();
            final var state2 = timestamped2.getState();
            if (state1 != null && state2 != null) {
                ObjectTest.assertInvariants(state1, state2);
            }
            assertTrue(
                    timestamped1.equals(timestamped2) == (timestamped1.isReliable() == timestamped2.isReliable()
                            && timestamped1.getStart().equals(timestamped2.getStart())
                            && timestamped1.getEnd().equals(timestamped2.getEnd()) && Objects.equals(state1, state2)),
                    "value semantics");
        }

        private static <STATE> void constructor(@Nonnull final Duration start, @Nonnull final Duration end,
                final boolean reliable, @Nullable final STATE state) {
            final var timestamped = new ObjectHistory.TimestampedState<>(start, end, reliable, state);

            assertInvariants(timestamped);
            assertAll(() -> assertSame(start, timestamped.getStart(), "start"),
                    () -> assertSame(end, timestamped.getEnd(), "end"),
                    () -> assertSame(state, timestamped.getState(), "state"),
                    () -> assertTrue(reliable == timestamped.isReliable(), "reliable"));
        }
    }// class

    static final UUID OBJECT_A = UUID.randomUUID();
    static final UUID OBJECT_B = UUID.randomUUID();

    static final Duration WHEN_A = Duration.ofMillis(0);
    static final Duration WHEN_B = Duration.ofMillis(5000);
    static final Duration WHEN_C = Duration.ofMillis(7000);
    static final UUID SIGNAL_ID_A = UUID.randomUUID();
    static final UUID SIGNAL_ID_B = UUID.randomUUID();

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history) {
        ObjectTest.assertInvariants(history);// inherited

        final var object = history.getObject();
        final var start = history.getStart();
        final var end = history.getEnd();
        final var stateHistory = history.getStateHistory();
        final var stateTransitions = history.getStateTransitions();
        final Collection<Signal<STATE>> signals = history.getSignals();

        assertAll("Not null", () -> assertNotNull(object, "object"), () -> assertNotNull(start, "start"), // guard
                () -> assertNotNull(end, "end"), // guard
                () -> assertNotNull(stateHistory, "stateHistory"), // guard
                () -> assertNotNull(stateTransitions, "stateTransitions"), // guard
                () -> assertNotNull(signals, "signals")// guard
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
                () -> assertEquals(stateTransitions, stateHistory.getTransitions(), "stateTransitions"),
                () -> assertAll("signals", createSignalsAssertions(signals, history.getObject(), history.getStart())));
    }

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history1,
            @Nonnull final ObjectHistory<STATE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited

        assertAll("Value semantics",
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "stateTransitions",
                        h -> h.getStateTransitions()),
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "object", h -> h.getObject()),
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "end", h -> h.getEnd()),
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "signals", h -> h.getSignals()),
                () -> assertTrue(history1
                        .equals(history2) == (history1.getStateTransitions().equals(history2.getStateTransitions())
                                && history1.getSignals().equals(history2.getSignals())
                                && history1.getObject().equals(history2.getObject())
                                && history1.getEnd().equals(history2.getEnd())),
                        "equals"));
    }

    private static <STATE> void assertNoMoreTimestampedStates(final Flux<TimestampedState<STATE>> timestampedStates) {
        try {
            StepVerifier.create(timestampedStates).expectComplete().verify(Duration.ofMillis(100));
        } catch (final AssertionError e) {
            throw new AssertionError(
                    "If the object is known to have no further states, there can be no further time-stamped states.",
                    e);
        }
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
            @Nonnull final SortedMap<Duration, STATE> stateTransitions,
            @Nonnull final Collection<Signal<STATE>> signals) {
        final var history = new ObjectHistory<>(object, end, stateTransitions, signals);

        assertInvariants(history);
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(end, history.getEnd(), "end"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, history.getStateTransitions(), "stateTransitions"),
                () -> assertEquals(signals, history.getSignals(), "signals"));
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

    static <STATE> long count(final Collection<Signal<STATE>> collection, final Signal<STATE> signal) {
        return collection.stream().filter(s -> signal.equals(s)).count();
    }

    private static <STATE> Stream<Executable> createSignalsAssertions(
            @Nonnull final Collection<Signal<STATE>> signalsRecieved, @Nonnull final UUID object,
            @Nonnull final Duration start) {
        return signalsRecieved.stream().map(signal -> new Executable() {

            @Override
            public void execute() throws Throwable {
                assertNotNull(signal, "signals not null");// guard
                assertAll("signal", () -> SignalTest.assertInvariants(signal),
                        () -> assertThat("not duplicated", count(signalsRecieved, signal) == 1),
                        () -> assertThat("sent at or after the start time", signal.getWhenSent(),
                                greaterThanOrEqualTo(start)),
                        () -> assertThat("has the object of this history as their receiver.", signal.getReceiver(),
                                is(object)));
            }
        });
    }

    public static <STATE> Publisher<Optional<STATE>> observeState(@Nonnull final ObjectHistory<STATE> history,
            @Nonnull final Duration when) {
        final var states = history.observeState(when);

        assertInvariants(history);
        assertNotNull(states, "Not null, states");// guard

        return states;
    }

    public static <STATE> Flux<ObjectHistory.TimestampedState<STATE>> observeTimestampedStates(
            @Nonnull final ObjectHistory<STATE> history) {
        final var flux = history.observeTimestampedStates();

        assertInvariants(history);
        assertNotNull(flux, "Not null, result");
        return flux;
    }
}// class
