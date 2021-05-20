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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import uk.badamson.dbc.assertions.ThreadSafetyTest;
import uk.badamson.mc.JsonTest;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.simulation.TimestampedId;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ModifiableObjectHistoryTest {

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
                final var history = new ModifiableObjectHistory<>(receiver, end, state);
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
                final var history = new ModifiableObjectHistory<>(receiver, end, state);
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
                final var history = new ModifiableObjectHistory<>(receiver, end, LAST_SIGNAL_APPLIED_A,
                        stateTransitions, signals);
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
            final var history = new ModifiableObjectHistory<>(receiver, end, state);
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
            final var history = new ModifiableObjectHistory<>(receiver, end, Integer.valueOf(1));

            final CountDownLatch ready = new CountDownLatch(1);
            final var random = new Random(0);
            final List<Future<Void>> futures = new ArrayList<>(nThreads);
            for (int t = 0; t < nThreads; ++t) {
                final UUID id = UUID.randomUUID();
                final UUID sender = UUID.randomUUID();
                final Duration sent = end.plusSeconds(1 + random.nextInt(128));
                final var signal = new SignalTest.TestSignal(id, new TimestampedId(sender, sent), receiver);
                futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> addSignal(history, signal)));
            }
            ready.countDown();
            ThreadSafetyTest.get(futures);
        }

        @Test
        public void two() {
            final UUID sender = OBJECT_A;
            final UUID receiver = OBJECT_B;
            final Duration end = WHEN_A;
            final Duration sent1 = end.plusSeconds(1);
            final Duration sent2 = end.plusSeconds(2);
            final Integer state = Integer.valueOf(1);
            final var history = new ModifiableObjectHistory<>(receiver, end, state);
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

            final var history = new ModifiableObjectHistory<>(receiver, end, lastSignalApplied0, stateTransitions0,
                    signals0);
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
            final var history = new ModifiableObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(0));
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

            final var history = new ModifiableObjectHistory<>(receiver, end, lastSignalApplied0, stateTransitions,
                    signals);
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
            final var history = new ModifiableObjectHistory<>(OBJECT_A, end0, Integer.valueOf(1));

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
            final var history = new ModifiableObjectHistory<>(OBJECT_A, end0, state);

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
                /*
                 * tough test: not a ModifiableObjectHistory
                 */
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
                    final Collection<Signal<Integer>> signals = List.of();

                    constructor(object, end, LAST_SIGNAL_APPLIED_A, stateTransitions, signals);
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
                    final Collection<Signal<Integer>> signals = List.of();

                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, endA, LAST_SIGNAL_APPLIED_A,
                            stateTransitions, signals);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, endB, LAST_SIGNAL_APPLIED_A,
                            stateTransitions, signals);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentLastSignalApplied() {
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>(
                            Map.of(WHEN_A, Integer.valueOf(-1)));
                    final Collection<Signal<Integer>> signals = List.of();

                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, WHEN_B, LAST_SIGNAL_APPLIED_A,
                            stateTransitions, signals);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, WHEN_B, LAST_SIGNAL_APPLIED_B,
                            stateTransitions, signals);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentObject() {
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>(
                            Map.of(WHEN_A, Integer.valueOf(-1)));
                    final var end = WHEN_A.plusMillis(10);
                    final Collection<Signal<Integer>> signals = List.of();

                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, end, LAST_SIGNAL_APPLIED_A,
                            stateTransitions, signals);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_B, end, LAST_SIGNAL_APPLIED_A,
                            stateTransitions, signals);

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
                    final Collection<Signal<Integer>> signalsA = List.of();
                    final Collection<Signal<Integer>> signalsB = List
                            .of(new SignalTest.TestSignal(SIGNAL_ID_A, new TimestampedId(OBJECT_B, end), object));
                    assert !signalsA.equals(signalsB);

                    final var historyA = new ModifiableObjectHistory<>(object, end, LAST_SIGNAL_APPLIED_A,
                            stateTransitions, signalsA);
                    final var historyB = new ModifiableObjectHistory<>(object, end, LAST_SIGNAL_APPLIED_A,
                            stateTransitions, signalsB);

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
                    final var end = start.plusMillis(10);
                    final Collection<Signal<Integer>> signals = List.of();
                    assert !stateTransitionsA.equals(stateTransitionsB);

                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, end, LAST_SIGNAL_APPLIED_A,
                            stateTransitionsA, signals);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, end, LAST_SIGNAL_APPLIED_A,
                            stateTransitionsB, signals);

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
                    final var lastSignalAppliedA = new TimestampedId(OBJECT_B, WHEN_B);
                    final var lastSignalAppliedB = new TimestampedId(OBJECT_B, WHEN_B);
                    final SortedMap<Duration, Integer> stateTransitionsA = new TreeMap<>(
                            Map.of(Duration.ofMillis(5000), Integer.valueOf(Integer.MAX_VALUE)));
                    final SortedMap<Duration, Integer> stateTransitionsB = new TreeMap<>(stateTransitionsA);
                    final Collection<Signal<Integer>> signalsA = List
                            .of(new SignalTest.TestSignal(SIGNAL_ID_A, new TimestampedId(OBJECT_B, endA), objectA));
                    final Collection<Signal<Integer>> signalsB = List.copyOf(signalsA);

                    assert objectA.equals(objectB);
                    assert endA.equals(endB);
                    assert lastSignalAppliedA.equals(lastSignalAppliedB);
                    assert stateTransitionsA.equals(stateTransitionsB);
                    assert objectA != objectB;// tough test
                    assert endA != endB;// tough test
                    assert lastSignalAppliedA != lastSignalAppliedB;// tough test
                    assert stateTransitionsA != stateTransitionsB;// tough test

                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, endA, lastSignalAppliedA,
                            stateTransitionsA, signalsA);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, endB, lastSignalAppliedB,
                            stateTransitionsB, signalsB);

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
                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, WHEN_A, state);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_B, WHEN_A, state);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentStart() {
                    final var state = Integer.valueOf(0);
                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, WHEN_A, state);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, WHEN_B, state);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentState() {
                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(0));
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(1));

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

                    final var historyA = new ModifiableObjectHistory<>(objectA, startA, stateA);
                    final var historyB = new ModifiableObjectHistory<>(objectB, startB, stateB);

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
        public class OneSignal {

            @Test
            public void sentAfterEnd() {
                final var end = WHEN_B;
                final var sentFrom = new TimestampedId(OBJECT_A, end.plusSeconds(5));
                test(OBJECT_B, end, SIGNAL_ID_B, sentFrom);
            }

            @Test
            public void sentAtEnd() {
                final var end = WHEN_A;
                final var sentFrom = new TimestampedId(OBJECT_B, end);
                test(OBJECT_A, end, SIGNAL_ID_A, sentFrom);
            }

            private void test(@Nonnull final UUID object, @Nonnull final Duration end, @Nonnull final UUID signalId,
                    @Nonnull final TimestampedId sentFrom) {
                final Integer state = Integer.valueOf(0);
                final Signal<Integer> signal = new SignalTest.TestSignal(signalId, sentFrom, object);
                final var history = new ModifiableObjectHistory<>(object, end, state);
                history.addSignal(signal);

                final var deserialized = JsonTest.serializeAndDeserialize(history);

                assertInvariants(history);
                assertInvariants(history, deserialized);
                assertAll(() -> assertThat("equals", deserialized, is(history)),
                        () -> assertEquals(history.getObject(), deserialized.getObject(), "object"),
                        () -> assertEquals(history.getStart(), deserialized.getStart(), "start"),
                        () -> assertEquals(history.getEnd(), deserialized.getEnd(), "end"),
                        () -> assertEquals(history.getStateHistory(), deserialized.getStateHistory(), "stateHistory"),
                        () -> assertEquals(history.getSignals(), deserialized.getSignals(), "signals"));
            }

        }// class

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
                final var history = new ModifiableObjectHistory<>(object, start, state);
                final var deserialized = JsonTest.serializeAndDeserialize(history);

                assertInvariants(history);
                assertInvariants(history, deserialized);
                assertAll(() -> assertThat("equals", deserialized, is(history)),
                        () -> assertEquals(history.getObject(), deserialized.getObject(), "object"),
                        () -> assertEquals(history.getStart(), deserialized.getStart(), "start"),
                        () -> assertEquals(history.getEnd(), deserialized.getEnd(), "end"),
                        () -> assertEquals(history.getStateHistory(), deserialized.getStateHistory(), "stateHistory"),
                        () -> assertEquals(history.getSignals(), deserialized.getSignals(), "signals"));
            }

        }// class

    }// class

    private static final UUID OBJECT_A = ObjectHistoryTest.OBJECT_A;
    private static final UUID OBJECT_B = ObjectHistoryTest.OBJECT_B;
    private static final Duration WHEN_A = ObjectHistoryTest.WHEN_A;
    private static final Duration WHEN_B = ObjectHistoryTest.WHEN_B;
    private static final Duration WHEN_C = ObjectHistoryTest.WHEN_C;
    private static final UUID SIGNAL_ID_A = ObjectHistoryTest.SIGNAL_ID_A;
    private static final UUID SIGNAL_ID_B = ObjectHistoryTest.SIGNAL_ID_B;
    private static final TimestampedId LAST_SIGNAL_APPLIED_A = ObjectHistoryTest.LAST_SIGNAL_APPLIED_A;
    private static final TimestampedId LAST_SIGNAL_APPLIED_B = ObjectHistoryTest.LAST_SIGNAL_APPLIED_B;

    private static <STATE> void addSignal(@Nonnull final ModifiableObjectHistory<STATE> history,
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
    private static <STATE> Signal.Effect<STATE> applyNextSignal(@Nonnull final ModifiableObjectHistory<STATE> history) {
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
            SignalTest.EffectTest.assertInvariants(effect);
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

    public static <STATE> void assertInvariants(@Nonnull final ModifiableObjectHistory<STATE> history) {
        ObjectHistoryTest.assertInvariants(history);// inherited
    }

    public static <STATE> void assertInvariants(@Nonnull final ModifiableObjectHistory<STATE> history1,
            @Nonnull final ModifiableObjectHistory<STATE> history2) {
        ObjectHistoryTest.assertInvariants(history1, history2);// inherited
    }

    private static <STATE> void commitTo(@Nonnull final ModifiableObjectHistory<STATE> history,
            @Nonnull final Duration when) {
        final var end0 = history.getEnd();

        history.commitTo(when);

        assertInvariants(history);
        final var end = history.getEnd();
        assertAll("end", () -> assertThat("does not decrease", end, greaterThanOrEqualTo(end0)),
                () -> assertThat("at least the given value", end, greaterThanOrEqualTo(when)));
    }

    private static <STATE> void constructor(@Nonnull final ObjectHistory<STATE> that) {
        final var copy = new ModifiableObjectHistory<>(that);

        assertInvariants(copy);
        ObjectHistoryTest.assertInvariants(copy, that);
        assertEquals(copy, that);
        assertAll("Copied", () -> assertSame(that.getEnd(), copy.getEnd(), "end"),
                () -> assertSame(that.getObject(), copy.getObject(), "object"),
                () -> assertEquals(that.getSignals(), copy.getSignals(), "signals"),
                () -> assertSame(that.getStart(), copy.getStart(), "start"),
                () -> assertEquals(that.getStateHistory(), copy.getStateHistory(), "stateHistory"),
                () -> assertSame(that.getLastSignalApplied(), copy.getLastSignalApplied(), "lastSignalApplied"));
    }

    private static <STATE> void constructor(@Nonnull final UUID object, @Nonnull final Duration start,
            @Nonnull final STATE state) {
        final var history = new ModifiableObjectHistory<>(object, start, state);

        assertInvariants(history);
        final var stateTransitions = history.getStateTransitions();
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(start, history.getStart(), "start"), () -> assertSame(start, history.getEnd(), "end"),
                () -> assertNull(history.getLastSignalApplied(), "lastSignalApplied"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, Map.of(start, state), "stateTransitions"),
                () -> assertThat("No signals", history.getSignals(), empty()));
    }

    private static <STATE> void constructor(@Nonnull final UUID object, @Nonnull final Duration end,
            @Nullable final TimestampedId lastSignalApplied, @Nonnull final SortedMap<Duration, STATE> stateTransitions,
            @Nonnull final Collection<Signal<STATE>> signals) {
        final var history = new ModifiableObjectHistory<>(object, end, lastSignalApplied, stateTransitions, signals);

        assertInvariants(history);
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(end, history.getEnd(), "end"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, history.getStateTransitions(), "stateTransitions"),
                () -> assertEquals(signals, history.getSignals(), "signals"),
                () -> assertSame(lastSignalApplied, history.getLastSignalApplied(), "lastSignalApplied"));
    }

    public static <STATE> Publisher<Optional<STATE>> observeState(@Nonnull final ModifiableObjectHistory<STATE> history,
            @Nonnull final Duration when) {
        final var states = ObjectHistoryTest.observeState(history, when);// inherited

        assertInvariants(history);
        return states;
    }

    public static <STATE> Flux<ObjectHistory.TimestampedState<STATE>> observeTimestampedStates(
            @Nonnull final ModifiableObjectHistory<STATE> history) {
        final var flux = ObjectHistoryTest.observeTimestampedStates(history);// inherited

        assertInvariants(history);
        return flux;
    }
}
