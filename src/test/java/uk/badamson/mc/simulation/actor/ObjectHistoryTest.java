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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.reactivestreams.Publisher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import uk.badamson.dbc.assertions.EqualsSemanticsTest;
import uk.badamson.dbc.assertions.ObjectTest;
import uk.badamson.dbc.assertions.ThreadSafetyTest;
import uk.badamson.mc.JsonTest;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.history.ValueHistoryTest;
import uk.badamson.mc.simulation.TimestampedId;
import uk.badamson.mc.simulation.TimestampedIdTest;
import uk.badamson.mc.simulation.actor.ObjectHistory.TimestampedState;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ObjectHistoryTest {

    @Nested
    public class AddSignal {

        @Nested
        public class One {

            @Test
            public void far() {
                final Duration end = WHEN_A;
                final Duration sent = end.plusDays(365);

                test(SIGNAL_ID_A, OBJECT_A, OBJECT_B, end, sent);
            }

            @Test
            public void reflexive() {
                final Duration end = WHEN_A;
                final Duration sent = end.plusSeconds(1);

                test(SIGNAL_ID_A, OBJECT_A, OBJECT_A, end, sent);
            }

            @Test
            public void sentAtEnd() {
                final Duration end = WHEN_A;
                final Duration sent = end;

                test(SIGNAL_ID_B, OBJECT_A, OBJECT_B, end, sent);
            }

            private void test(@Nonnull final UUID id, @Nonnull final UUID sender, @Nonnull final UUID receiver,
                    @Nonnull final Duration end, @Nonnull final Duration sent) {
                assert end.compareTo(sent) <= 0;
                final Integer state = Integer.valueOf(1);
                final var history = new ObjectHistory<>(receiver, end, state);
                final var signal = new SignalTest.TestSignal(id, new TimestampedId(sender, sent), receiver);
                assert history.getEnd().equals(end);

                addSignal(history, signal);
            }

        }// class

        @Nested
        public class Two {

            @Test
            public void ascendingTime() {
                test(SIGNAL_ID_A, SIGNAL_ID_B, OBJECT_A, OBJECT_B, WHEN_A, WHEN_B);
            }

            @Test
            public void descendingTime() {
                test(SIGNAL_ID_A, SIGNAL_ID_B, OBJECT_A, OBJECT_B, WHEN_B, WHEN_A);
            }

            @Test
            public void simultaneous() {
                test(SIGNAL_ID_A, SIGNAL_ID_B, OBJECT_A, OBJECT_B, WHEN_A, WHEN_A);
            }

            private void test(@Nonnull final UUID signalIdA, @Nonnull final UUID signalIdB, @Nonnull final UUID sender1,
                    @Nonnull final UUID sender2, @Nonnull final Duration whenSent1, @Nonnull final Duration whenSent2) {
                final UUID receiver = OBJECT_B;
                final Duration end = WHEN_A;
                final Integer state = Integer.valueOf(1);
                final var history = new ObjectHistory<>(receiver, end, state);
                final var signal1 = new SignalTest.TestSignal(signalIdA, new TimestampedId(sender1, whenSent1),
                        receiver);
                final var signal2 = new SignalTest.TestSignal(signalIdB, new TimestampedId(sender2, whenSent2),
                        receiver);
                assert history.getEnd().equals(end);
                history.addSignal(signal1);

                addSignal(history, signal2);
            }

        }// class

        @Nested
        public class Unreceivable {

            @Test
            public void beforeStart() {
                final Duration start = WHEN_A;
                final Duration end = WHEN_B;
                final Duration sent = start.minusNanos(1);// tough test
                final Integer state = Integer.valueOf(1);

                test(sent, start, end, state);
                ;
            }

            @Test
            public void receivedAtEnd() {
                final Duration start = WHEN_A;
                final Integer state = Integer.valueOf(1);
                final Duration sent = start.plusSeconds(1);
                final var signal = new SignalTest.TestSignal(SIGNAL_ID_A, new TimestampedId(OBJECT_B, sent), OBJECT_A);
                final Duration end = signal.getWhenReceived(state);// tough test

                test(sent, start, end, state);
                ;
            }

            private void test(@Nonnull final Duration sent, @Nonnull final Duration start, @Nonnull final Duration end,
                    @Nonnull final Integer state) {
                assert start.compareTo(end) < 0;
                final UUID sender = OBJECT_A;
                final UUID receiver = OBJECT_B;
                final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                stateTransitions.put(start, state);
                final Collection<Signal<Integer>> signals = List.of();
                final var history = new ObjectHistory<>(receiver, end, LAST_SIGNAL_APPLIED_A, stateTransitions,
                        signals);
                final var signal = new SignalTest.TestSignal(SIGNAL_ID_A, new TimestampedId(sender, sent), receiver);

                assertThrows(Signal.UnreceivableSignalException.class, () -> addSignal(history, signal));
            }
        }// class

        @Test
        public void duplicate() {
            final UUID id = SIGNAL_ID_A;
            final UUID sender = OBJECT_A;
            final UUID receiver = OBJECT_B;
            final Duration end = WHEN_A;
            final Duration sent = WHEN_B;
            final Integer state = Integer.valueOf(1);
            final var history = new ObjectHistory<>(receiver, end, state);
            final var signal = new SignalTest.TestSignal(id, new TimestampedId(sender, sent), receiver);
            assert history.getEnd().equals(end);
            history.addSignal(signal);

            addSignal(history, signal);
        }

        @RepeatedTest(4)
        public void multipleThreads() {
            final int nThreads = 32;
            final UUID receiver = OBJECT_B;
            final Duration end = WHEN_A;
            final var history = new ObjectHistory<>(receiver, end, Integer.valueOf(1));

            final CountDownLatch ready = new CountDownLatch(1);
            final var random = new Random(0);
            final List<Future<Void>> futures = new ArrayList<>(nThreads);
            for (int t = 0; t < nThreads; ++t) {
                final UUID id = UUID.randomUUID();
                final UUID sender = UUID.randomUUID();
                final Duration sent = end.plusSeconds(1 + random.nextInt(128));
                final var signal = new SignalTest.TestSignal(id, new TimestampedId(sender, sent), receiver);
                futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> history.addSignal(signal)));
            }
            ready.countDown();
            ThreadSafetyTest.get(futures);
            assertInvariants(history);
        }

        @Test
        public void two() {
            final UUID sender = OBJECT_A;
            final UUID receiver = OBJECT_B;
            final Duration end = WHEN_A;
            final Duration sent1 = end.plusSeconds(1);
            final Duration sent2 = end.plusSeconds(2);
            final Integer state = Integer.valueOf(1);
            final var history = new ObjectHistory<>(receiver, end, state);
            final var signal1 = new SignalTest.TestSignal(SIGNAL_ID_A, new TimestampedId(sender, sent1), receiver);
            final var signal2 = new SignalTest.TestSignal(SIGNAL_ID_B, new TimestampedId(sender, sent2), receiver);
            assert history.getEnd().equals(end);
            history.addSignal(signal1);

            addSignal(history, signal2);
        }

    }// class

    @Nested
    public class ApplyNextSignal {

        @Test
        public void a() {
            testOne(OBJECT_A, OBJECT_B, WHEN_A, WHEN_B, Integer.valueOf(1), SIGNAL_ID_A);
        }

        @Test
        public void b() {
            testOne(OBJECT_B, OBJECT_A, WHEN_B, WHEN_C, Integer.valueOf(10), SIGNAL_ID_B);
        }

        @Test
        public void multipleThreads() {
            final int nIterations = 32;
            final int nThreads = 8;

            final UUID receiver = OBJECT_A;
            final UUID sender = receiver;
            final Duration start = WHEN_A;
            final Duration whenSent0 = WHEN_B;
            final Integer state0 = Integer.valueOf(0);
            final UUID signalId0 = SIGNAL_ID_A;
            final boolean strobe = true;// critical

            final TimestampedId lastSignalApplied0 = null;
            final SortedMap<Duration, Integer> stateTransitions0 = new TreeMap<>();
            stateTransitions0.put(start, state0);
            final var signal0 = new SignalTest.TestSignal(signalId0, new TimestampedId(sender, whenSent0), receiver,
                    strobe);
            final Collection<Signal<Integer>> signals0 = List.of(signal0);
            final var whenReceived0 = signal0.getWhenReceived(state0);
            final Duration end = whenReceived0.minusNanos(1);

            final var history = new ObjectHistory<>(receiver, end, lastSignalApplied0, stateTransitions0, signals0);
            final CountDownLatch ready = new CountDownLatch(1);
            final List<Future<Void>> futures = new ArrayList<>(nThreads);

            for (int t = 0; t < nThreads; ++t) {
                futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> {
                    for (int i = 0; i < nIterations; ++i) {
                        final var lastSignalAppliedBefore = history.getLastSignalApplied();

                        final var effect = history.applyNextSignal();

                        if (effect != null) {
                            final var lastSignalAppliedAfter = history.getLastSignalApplied();
                            assertThat("effect", effect, notNullValue());// guard
                            final var signalsEmitted = effect.getSignalsEmitted();
                            assertThat("effect.signalsEmitted", signalsEmitted, allOf(notNullValue(), not(empty())));// guard
                            final var signalEmitted = signalsEmitted.iterator().next();
                            assertThat("signalEmitted.receiver", signalEmitted.getReceiver(), is(receiver));
                            assertThat("Signals applied in order", lastSignalAppliedBefore == null
                                    || lastSignalAppliedBefore.compareTo(lastSignalAppliedAfter) < 0);

                            history.addSignal(signalEmitted);
                        }
                        /* else another thread applied the signal before we could. */
                    } // for

                }));
            }

            ready.countDown();
            ThreadSafetyTest.get(futures);
        }

        @Test
        public void none() {
            final var history = new ObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(0));
            assert history.getSignals().isEmpty();

            final var effect = applyNextSignal(history);

            assertNull(effect, "no effect");
        }

        private void testOne(@Nonnull final UUID receiver, @Nonnull final UUID sender, @Nonnull final Duration start,
                @Nonnull final Duration whenSent, @Nonnull final Integer state0, @Nonnull final UUID signalId) {
            assert start.compareTo(whenSent) <= 0;
            final TimestampedId lastSignalApplied0 = null;
            final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
            stateTransitions.put(start, state0);
            final var signal = new SignalTest.TestSignal(signalId, new TimestampedId(sender, whenSent), receiver);
            final Collection<Signal<Integer>> signals = List.of(signal);
            final var whenReceived = signal.getWhenReceived(state0);
            final var expectedState = signal.receive(state0).getState();
            final Duration end = whenReceived.minusNanos(1);
            final var expectedTimestampedState = new ObjectHistory.TimestampedState<>(whenReceived,
                    ValueHistory.END_OF_TIME, false, expectedState);

            final var history = new ObjectHistory<>(receiver, end, lastSignalApplied0, stateTransitions, signals);
            final var timestampedStatesVerifier = StepVerifier.create(history.observeTimestampedStates());
            final var stateVerifier = StepVerifier.create(history.observeState(whenReceived));// tough test

            final var effect = applyNextSignal(history);

            final var lastSignalApplied = history.getLastSignalApplied();
            assertThat("effect", effect, notNullValue());// guard
            final var resultState = effect.getState();
            assertAll(() -> assertThat("when effect occurred", effect.getWhenOccurred(), is(whenReceived)),
                    () -> assertThat("no state change or the reception time of the signal is the last transition time",
                            state0.equals(resultState)
                                    || whenReceived.equals(history.getStateHistory().getLastTansitionTime())),
                    () -> assertThat("last signal applied ID", lastSignalApplied.getObject(), is(signalId)));

            timestampedStatesVerifier.expectNext(expectedTimestampedState).expectTimeout(Duration.ofMillis(100))
                    .verify();
            stateVerifier.expectNext(Optional.of(state0)).expectNext(Optional.ofNullable(expectedState))
                    .expectTimeout(Duration.ofMillis(100)).verify();
        }

    }// class

    @Nested
    public class CommitTo {

        @Test
        public void before() {
            final Duration end0 = WHEN_A;
            final Duration when = end0.minusNanos(1);// tough test

            test(end0, Integer.valueOf(0), when);
        }

        @Test
        public void endOfTime() {
            final Duration end0 = WHEN_A;
            final Duration when = ValueHistory.END_OF_TIME;// critical

            test(end0, Integer.valueOf(0), when);
        }

        @Test
        public void equal() {
            final long time = 1000;
            final Duration end0 = Duration.ofMillis(time);
            final Duration when = Duration.ofMillis(time);
            assert end0.equals(when);
            assert end0 != when;// tough test

            test(end0, Integer.valueOf(0), when);
        }

        @Test
        public void far() {
            final Duration end0 = WHEN_B;
            final Duration when = end0.plusDays(365);

            test(end0, Integer.valueOf(1), when);
        }

        @RepeatedTest(4)
        public void multipleThreads() {
            final int nThreads = 32;
            final Duration end0 = WHEN_B;
            final var history = new ObjectHistory<>(OBJECT_A, end0, Integer.valueOf(1));

            final CountDownLatch ready = new CountDownLatch(1);
            final var random = new Random(0);
            final List<Future<Void>> futures = new ArrayList<>(nThreads);
            for (int t = 0; t < nThreads; ++t) {
                futures.add(ThreadSafetyTest.runInOtherThread(ready,
                        () -> commitTo(history, end0.plusMillis(random.nextInt(1000)))));
            }
            ready.countDown();
            ThreadSafetyTest.get(futures);
        }

        @Test
        public void near() {
            final Duration end0 = WHEN_A;
            final Duration when = end0.plusNanos(1);// critical

            test(end0, Integer.valueOf(0), when);
        }

        private void test(@Nonnull final Duration end0, final Integer state, @Nonnull final Duration when) {
            final var history = new ObjectHistory<>(OBJECT_A, end0, state);

            commitTo(history, when);
        }

    }// class

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
            public class HasAppliedSignal {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, WHEN_B, SIGNAL_ID_A, SIGNAL_ID_B, Integer.valueOf(0), Integer.valueOf(1));
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, WHEN_C, SIGNAL_ID_B, SIGNAL_ID_A, Integer.valueOf(4), Integer.valueOf(2));
                }

                private void test(@Nonnull final UUID sender1, @Nonnull final Duration whenSent1,
                        @Nonnull final Duration whenSent2, @Nonnull final UUID signalId1, @Nonnull final UUID signalId2,
                        @Nonnull final Integer state2, @Nonnull final Integer state1) {
                    final UUID receiver = OBJECT_A;
                    final var signal1 = new SignalTest.TestSignal(signalId1, new TimestampedId(sender1, whenSent1),
                            receiver);
                    final var signal2 = new SignalTest.TestSignal(signalId2, new TimestampedId(sender1, whenSent2),
                            receiver);
                    final Collection<Signal<Integer>> signals = List.of(signal1, signal2);
                    final var whenReceived1 = signal1.getWhenReceived(state2);
                    final var whenReceived2 = signal2.getWhenReceived(state2);
                    final Duration start = whenSent1;
                    final Duration end = whenReceived1;
                    final TimestampedId lastSignalApplied = new TimestampedId(signalId1, whenReceived1);
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                    stateTransitions.put(start, state1);
                    stateTransitions.put(whenReceived1, state2);
                    assert whenReceived1.compareTo(whenReceived2) <= 0;
                    assert whenReceived1.compareTo(whenReceived2) < 0 || signalId1.compareTo(signalId2) < 0;

                    final var history = constructor(receiver, end, lastSignalApplied, stateTransitions, signals);

                    assertSame(signal2, history.getNextSignalToApply(), "nextSignalToApply");
                }
            }// class

            @Nested
            public class OneSignal {

                @Test
                public void sentAfterEnd() {
                    final var start = WHEN_B;
                    final var end = WHEN_C;
                    final var sentFrom = new TimestampedId(OBJECT_A, end.plusSeconds(5));
                    test(OBJECT_B, start, end, LAST_SIGNAL_APPLIED_A, SIGNAL_ID_B, sentFrom);
                }

                @Test
                public void sentAtEnd() {
                    final var start = WHEN_A;
                    final var end = WHEN_B;
                    final var sentFrom = new TimestampedId(OBJECT_B, end);
                    test(OBJECT_A, start, end, LAST_SIGNAL_APPLIED_B, SIGNAL_ID_A, sentFrom);
                }

                private void test(@Nonnull final UUID object, @Nonnull final Duration start,
                        @Nonnull final Duration end, @Nonnull final TimestampedId lastSignalApplied,
                        @Nonnull final UUID signalId, @Nonnull final TimestampedId sentFrom) {
                    final Integer state = Integer.valueOf(0);
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                    stateTransitions.put(start, state);
                    final Signal<Integer> signal = new SignalTest.TestSignal(signalId, sentFrom, object);
                    final Collection<Signal<Integer>> signals = List.of(signal);

                    constructor(object, end, lastSignalApplied, stateTransitions, signals);
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
                    final Collection<Signal<Integer>> signals = List.of();
                    final var endA = WHEN_A.plusMillis(10);
                    final var endB = WHEN_A.plusMillis(20);

                    final var historyA = new ObjectHistory<>(OBJECT_A, endA, LAST_SIGNAL_APPLIED_A, stateTransitions,
                            signals);
                    final var historyB = new ObjectHistory<>(OBJECT_A, endB, LAST_SIGNAL_APPLIED_B, stateTransitions,
                            signals);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentLastSignalApplied() {
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>(
                            Map.of(WHEN_A, Integer.valueOf(-1)));
                    final Collection<Signal<Integer>> signals = List.of();

                    final var historyA = new ObjectHistory<>(OBJECT_A, WHEN_B, LAST_SIGNAL_APPLIED_A, stateTransitions,
                            signals);
                    final var historyB = new ObjectHistory<>(OBJECT_A, WHEN_B, LAST_SIGNAL_APPLIED_B, stateTransitions,
                            signals);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentObject() {
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>(
                            Map.of(WHEN_A, Integer.valueOf(-1)));
                    final Collection<Signal<Integer>> signals = List.of();
                    final var end = WHEN_A.plusMillis(10);

                    final var historyA = new ObjectHistory<>(OBJECT_A, end, LAST_SIGNAL_APPLIED_A, stateTransitions,
                            signals);
                    final var historyB = new ObjectHistory<>(OBJECT_B, end, LAST_SIGNAL_APPLIED_B, stateTransitions,
                            signals);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentSignals() {
                    final var object = OBJECT_A;
                    final Duration start = WHEN_A;
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>(
                            Map.of(start, Integer.valueOf(-1)));
                    final var end = start.plusMillis(10);
                    final var whenLastSignalApplied = LAST_SIGNAL_APPLIED_A;
                    final Collection<Signal<Integer>> signalsA = List.of();
                    final Collection<Signal<Integer>> signalsB = List
                            .of(new SignalTest.TestSignal(SIGNAL_ID_A, new TimestampedId(OBJECT_B, end), object));
                    assert !signalsA.equals(signalsB);

                    final var historyA = new ObjectHistory<>(object, end, whenLastSignalApplied, stateTransitions,
                            signalsA);
                    final var historyB = new ObjectHistory<>(object, end, whenLastSignalApplied, stateTransitions,
                            signalsB);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentStateTransitions() {
                    final Duration start = WHEN_A;
                    final SortedMap<Duration, Integer> stateTransitionsA = new TreeMap<>(
                            Map.of(start, Integer.valueOf(-1)));
                    final SortedMap<Duration, Integer> stateTransitionsB = new TreeMap<>(
                            Map.of(start, Integer.valueOf(-2)));
                    final Collection<Signal<Integer>> signals = List.of();
                    final var end = start.plusMillis(10);
                    assert !stateTransitionsA.equals(stateTransitionsB);

                    final var historyA = new ObjectHistory<>(OBJECT_A, end, LAST_SIGNAL_APPLIED_A, stateTransitionsA,
                            signals);
                    final var historyB = new ObjectHistory<>(OBJECT_A, end, LAST_SIGNAL_APPLIED_A, stateTransitionsB,
                            signals);

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
                    final var lastSignalAppliedA = new TimestampedId(SIGNAL_ID_A, WHEN_A);
                    final var lastSignalAppliedB = new TimestampedId(SIGNAL_ID_A, WHEN_A);
                    final SortedMap<Duration, Integer> stateTransitionsA = new TreeMap<>(
                            Map.of(Duration.ofMillis(5000), Integer.valueOf(Integer.MAX_VALUE)));
                    final SortedMap<Duration, Integer> stateTransitionsB = new TreeMap<>(stateTransitionsA);
                    final Collection<Signal<Integer>> signalsA = List
                            .of(new SignalTest.TestSignal(SIGNAL_ID_A, new TimestampedId(OBJECT_B, endA), objectA));
                    final Collection<Signal<Integer>> signalsB = new ArrayList<>(signalsA);

                    assert objectA.equals(objectB);
                    assert endA.equals(endB);
                    assert lastSignalAppliedA.equals(lastSignalAppliedB);
                    assert stateTransitionsA.equals(stateTransitionsB);
                    assert signalsA.equals(signalsB);
                    assert objectA != objectB;// tough test
                    assert endA != endB;// tough test
                    assert lastSignalAppliedA != lastSignalAppliedB;// tough test
                    assert stateTransitionsA != stateTransitionsB;// tough test
                    assert signalsA != signalsB;// tough test

                    final var historyA = new ObjectHistory<>(OBJECT_A, endA, lastSignalAppliedA, stateTransitionsA,
                            signalsA);
                    final var historyB = new ObjectHistory<>(OBJECT_A, endB, lastSignalAppliedB, stateTransitionsB,
                            signalsB);

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

                constructor(OBJECT_A, WHEN_A.plusSeconds(2), LAST_SIGNAL_APPLIED_A, stateTransitions, signals);
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
    static final TimestampedId LAST_SIGNAL_APPLIED_A = new TimestampedId(SIGNAL_ID_A, WHEN_A);
    static final TimestampedId LAST_SIGNAL_APPLIED_B = new TimestampedId(SIGNAL_ID_B, WHEN_B);

    private static <STATE> void addSignal(@Nonnull final ObjectHistory<STATE> history,
            @Nonnull final Signal<STATE> signal) throws Signal.UnreceivableSignalException {
        try {
            history.addSignal(signal);
        } catch (final Signal.UnreceivableSignalException e) {
            assertInvariants(history);
            throw e;
        }

        assertInvariants(history);
        assertThat("added", ObjectHistoryTest.count(history.getSignals(), signal) == 1);
    }

    @Nullable
    private static <STATE> Event<STATE> applyNextSignal(@Nonnull final ObjectHistory<STATE> history) {
        final var stateHistory0 = history.getStateHistory();
        final var lastSignalApplied0 = history.getLastSignalApplied();

        final var effect = history.applyNextSignal();

        assertInvariants(history);
        final var stateHistory = history.getStateHistory();
        final var lastSignalApplied = history.getLastSignalApplied();
        if (effect == null) {
            assertThat("State history", stateHistory, is(stateHistory0));
            assertThat("last signal applied", lastSignalApplied, is(lastSignalApplied0));
        } else {
            EventTest.assertInvariants(effect);
            final var resultState = effect.getState();
            final var whenOccurred = effect.getWhenOccurred();// signal reception time
            final var lastTransitionTime = stateHistory.getLastTansitionTime();
            assertThat("The last state in the the state history", stateHistory.getLastValue(), is(resultState));
            assertThat("last transition time", lastTransitionTime,
                    either(nullValue(Duration.class)).or(lessThanOrEqualTo(whenOccurred)));
            assertThat("last signal applied", lastSignalApplied, allOf(notNullValue(), not(is(lastSignalApplied0))));
            assertThat("when last signal applied", lastSignalApplied.getWhen(), is(whenOccurred));
        }

        return effect;
    }

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history) {
        ObjectTest.assertInvariants(history);// inherited

        final var object = history.getObject();
        final var start = history.getStart();
        final var end = history.getEnd();
        final var stateHistory = history.getStateHistory();
        final var stateTransitions = history.getStateTransitions();
        final Collection<Signal<STATE>> signals = history.getSignals();
        final var lastSignalApplied = history.getLastSignalApplied();
        final var nextSignalToApply = history.getNextSignalToApply();

        assertAll("Not null", () -> assertNotNull(object, "object"), () -> assertNotNull(start, "start"), // guard
                () -> assertNotNull(end, "end"), // guard
                () -> assertNotNull(stateHistory, "stateHistory"), // guard
                () -> assertNotNull(stateTransitions, "stateTransitions"), // guard
                () -> assertNotNull(signals, "signals")// guard
        );
        ValueHistoryTest.assertInvariants(stateHistory);
        if (lastSignalApplied != null) {
            TimestampedIdTest.assertInvariants(lastSignalApplied);
        }
        if (nextSignalToApply != null) {
            SignalTest.assertInvariants(nextSignalToApply);
        }

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
                () -> assertAll("signals", createSignalsAssertions(signals, object, start, stateHistory)));

        if (lastSignalApplied == null && !signals.isEmpty()) {
            final var firstSignal = signals.iterator().next();
            assertSame(firstSignal, nextSignalToApply,
                    "If and there is no last signal applied, and the collection of signals is not empty, the next signal to apply will be first of the signals.");
        }
        if (nextSignalToApply != null) {
            assertThat("If there is a next signal to apply, it is one of the signals.", signals,
                    hasItem(nextSignalToApply));// guard
            if (lastSignalApplied != null) {
                final Duration whenReceived = nextSignalToApply.getWhenReceived(stateHistory);
                assertThat(
                        "If there is a next signal to apply, and there was a last signal applied, the next signal to apply will be at or after the last signal applied",
                        whenReceived, greaterThanOrEqualTo(lastSignalApplied.getWhen()));
            }
        }
    }

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history1,
            @Nonnull final ObjectHistory<STATE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited

        assertAll("Value semantics",
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "stateTransitions",
                        h -> h.getStateTransitions()),
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "object", h -> h.getObject()),
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "end", h -> h.getEnd()),
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "lastSignalApplied",
                        h -> h.getLastSignalApplied()),
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "signals", h -> h.getSignals()),
                () -> assertTrue(
                        history1.equals(
                                history2) == (history1.getStateTransitions().equals(history2.getStateTransitions())
                                        && history1.getSignals().equals(history2.getSignals())
                                        && history1.getObject().equals(history2.getObject())
                                        && history1.getEnd().equals(history2.getEnd()) && Objects.equals(
                                                history1.getLastSignalApplied(), history2.getLastSignalApplied())),
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

    private static <STATE> void commitTo(@Nonnull final ObjectHistory<STATE> history, @Nonnull final Duration when) {
        final var end0 = history.getEnd();

        history.commitTo(when);

        assertInvariants(history);
        final var end = history.getEnd();
        assertAll("end", () -> assertThat("does not decrease", end, greaterThanOrEqualTo(end0)),
                () -> assertThat("at least the given value", end, greaterThanOrEqualTo(when)));
    }

    private static <STATE> ObjectHistory<STATE> constructor(@Nonnull final ObjectHistory<STATE> that) {
        final var copy = new ObjectHistory<>(that);

        assertInvariants(copy);
        assertInvariants(copy, that);
        assertEquals(copy, that);
        assertAll("Copied", () -> assertSame(that.getEnd(), copy.getEnd(), "end"),
                () -> assertSame(that.getObject(), copy.getObject(), "object"),
                () -> assertSame(that.getStart(), copy.getStart(), "start"),
                () -> assertEquals(that.getStateHistory(), copy.getStateHistory(), "stateHistory"),
                () -> assertSame(that.getLastSignalApplied(), copy.getLastSignalApplied(), "lastSignalApplied"));

        return copy;
    }

    private static <STATE> ObjectHistory<STATE> constructor(@Nonnull final UUID object, @Nonnull final Duration start,
            @Nonnull final STATE state) {
        final var history = new ObjectHistory<>(object, start, state);

        assertInvariants(history);
        final var stateTransitions = history.getStateTransitions();
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(start, history.getStart(), "start"), () -> assertSame(start, history.getEnd(), "end"),
                () -> assertNull(history.getLastSignalApplied(), "lastSignalApplied"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, Map.of(start, state), "stateTransitions"),
                () -> assertThat("No signals", history.getSignals(), empty()));

        return history;
    }

    private static <STATE> ObjectHistory<STATE> constructor(@Nonnull final UUID object, @Nonnull final Duration end,
            @Nullable final TimestampedId lastSignalApplied, @Nonnull final SortedMap<Duration, STATE> stateTransitions,
            @Nonnull final Collection<Signal<STATE>> signals) {
        final var history = new ObjectHistory<>(object, end, lastSignalApplied, stateTransitions, signals);

        assertInvariants(history);
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(end, history.getEnd(), "end"),
                () -> assertSame(lastSignalApplied, history.getLastSignalApplied(), "lastSignalApplied"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, history.getStateTransitions(), "stateTransitions"),
                () -> assertEquals(signals, history.getSignals(), "signals"));

        return history;
    }

    static <STATE> long count(final Collection<Signal<STATE>> collection, final Signal<STATE> signal) {
        return collection.stream().filter(s -> signal.equals(s)).count();
    }

    private static <STATE> Stream<Executable> createSignalsAssertions(
            @Nonnull final Collection<Signal<STATE>> signalsRecieved, @Nonnull final UUID object,
            @Nonnull final Duration start, @Nonnull final ValueHistory<STATE> stateHistory) {
        final var previousReceptionTime = new AtomicReference<>(ValueHistory.START_OF_TIME);
        return signalsRecieved.stream().sequential().map(signal -> new Executable() {

            @Override
            public void execute() throws Throwable {
                assertNotNull(signal, "signals not null");// guard
                final Duration receptionTime = signal.getWhenReceived(stateHistory);
                assertAll("signal", () -> SignalTest.assertInvariants(signal),
                        () -> assertThat("not duplicated", count(signalsRecieved, signal) == 1),
                        () -> assertThat("sent at or after the start time", signal.getWhenSent(),
                                greaterThanOrEqualTo(start)),
                        () -> assertThat("has the object of this history as their receiver.", signal.getReceiver(),
                                is(object)),
                        () -> assertThat("signals in ascending order of their reception time", receptionTime,
                                greaterThanOrEqualTo(previousReceptionTime.get())));
                previousReceptionTime.set(receptionTime);
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
