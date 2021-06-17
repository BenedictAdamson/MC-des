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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.reactivestreams.Publisher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.test.StepVerifier;
import uk.badamson.dbc.assertions.CollectionTest;
import uk.badamson.dbc.assertions.EqualsSemanticsTest;
import uk.badamson.dbc.assertions.ObjectTest;
import uk.badamson.dbc.assertions.ThreadSafetyTest;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.history.ValueHistoryTest;
import uk.badamson.mc.simulation.TimestampedId;
import uk.badamson.mc.simulation.actor.ObjectHistory.Continuation;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ObjectHistoryTest {

    @Nested
    public class AddIncomingSignal {

        @Nested
        public class One {

            @Test
            public void a() {
                test(OBJECT_A, OBJECT_B, WHEN_A, WHEN_B, SIGNAL_ID_A);
            }

            @Test
            public void b() {
                test(OBJECT_B, OBJECT_A, WHEN_B, WHEN_C, SIGNAL_ID_B);
            }

            private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver, @Nonnull final Duration start,
                    @Nonnull final Duration whenSent, @Nonnull final UUID signalId) {
                final var state = Integer.valueOf(0);
                final var history = new ObjectHistory<>(receiver, start, state);
                final var sentFrom = new TimestampedId(sender, whenSent);
                final var signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);

                addIncomingSignal(history, signal);

                assertThat("incomingSignals", history.getIncomingSignals(), is(Set.of(signal)));
            }
        }// class

        @RepeatedTest(4)
        public void multipleThreads() {
            final int nThreads = 16;
            final int nSignalsPerThread = 64;

            final UUID receiver = OBJECT_A;
            final UUID sender = OBJECT_B;
            final Duration end = WHEN_A;
            final Integer state0 = Integer.valueOf(0);
            final Duration start = end;
            final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);

            final CountDownLatch ready = new CountDownLatch(1);
            final List<Future<Void>> futures = new ArrayList<>(nThreads);

            for (int t = 0; t < nThreads; ++t) {
                futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> {
                    final var random = new Random();
                    for (int s = 0; s < nSignalsPerThread; ++s) {
                        final UUID signalId = UUID.randomUUID();
                        final Duration whenSent = end.plusMillis(1 + random.nextInt(10_000));
                        final var sentFrom = new TimestampedId(sender, whenSent);
                        final var signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);

                        history.addIncomingSignal(signal);
                        assertThat("incomingSignals", history.getIncomingSignals(), hasItem(signal));
                    } // for
                }));
            } // for

            ready.countDown();
            ThreadSafetyTest.get(futures);
            assertInvariants(history);
        }

    }// class

    @Nested
    public class AdvanceEnd {

        @Nested
        public class Event {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, Duration.ofSeconds(10), Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, Duration.ofSeconds(100), Integer.valueOf(1));
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent,
                    @Nonnull final Duration relativeEnd, @Nonnull final Integer state0) {
                final UUID sender = OBJECT_A;
                final UUID receiver = OBJECT_B;
                final UUID signalId = SIGNAL_ID_A;
                final var sentFrom = new TimestampedId(sender, whenSent);
                final var signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);

                final var history = new ObjectHistory<>(receiver, start, state0);
                final Medium<Integer> medium = new MediumTest.RecordingMedium<>();
                medium.addAll(List.of(signal));
                history.addIncomingSignal(signal);
                history.receiveNextSignal(medium);
                final var event = history.getEvents().last();
                final var eventTime = event.getWhenOccurred();
                final var eventState = event.getState();
                final var end = eventTime.plus(relativeEnd);
                final var stateVerifierBeforeEvent = StepVerifier.create(history.observeState(eventTime.minusNanos(1)));
                final var stateVerifierAtEvent = StepVerifier.create(history.observeState(eventTime));
                final var stateVerifierAtEnd = StepVerifier.create(history.observeState(end));
                final var stateVerifierAfterEnd = StepVerifier.create(history.observeState(end.plusNanos(1)));

                advanceEnd(history, end);

                assertThat("end (changed)", history.getEnd(), sameInstance(end));
                stateVerifierBeforeEvent.expectNext(Optional.of(state0)).expectComplete().verify(FLUX_TIMEOUT);
                stateVerifierAtEvent.expectNext(Optional.of(eventState)).expectComplete().verify(FLUX_TIMEOUT);
                stateVerifierAtEnd.expectNext(Optional.of(eventState)).expectComplete().verify(FLUX_TIMEOUT);
                stateVerifierAfterEnd.expectNext(Optional.of(eventState)).verifyTimeout(FLUX_TIMEOUT);
            }

        }// class

        @Nested
        public class NoEvents {

            @Test
            public void close() {
                test(WHEN_A, WHEN_A.plusNanos(1));
            }

            @Test
            public void far() {
                test(WHEN_A, WHEN_B);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration end) {
                assert start.compareTo(end) < 0;
                final UUID object = OBJECT_A;
                final Integer state = Integer.valueOf(0);
                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(object, start, state);
                final var stateVerifierAtEnd = StepVerifier.create(history.observeState(end));
                final var stateVerifierAfterEnd = StepVerifier.create(history.observeState(end.plusNanos(1)));

                advanceEnd(history, end);

                assertThat("end (changed)", history.getEnd(), sameInstance(end));
                stateVerifierAtEnd.expectNext(Optional.of(state)).expectComplete().verify();
                stateVerifierAfterEnd.expectNext(Optional.of(state)).verifyTimeout(FLUX_TIMEOUT);
            }

        }// class

        @Nested
        public class TooEarly {

            @Test
            public void close() {
                test(WHEN_A, WHEN_A.plusNanos(1));
            }

            @Test
            public void far() {
                test(WHEN_A, WHEN_B);
            }

            @Test
            public void same() {
                test(WHEN_A, WHEN_A);
            }

            private void test(@Nonnull final Duration end, @Nonnull final Duration start) {
                assert end.compareTo(start) <= 0;
                final UUID object = OBJECT_A;
                final Integer state = Integer.valueOf(0);
                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(object, start, state);

                advanceEnd(history, end);

                assertThat("end (no change)", history.getEnd(), sameInstance(start));
            }

        }// class
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
    public class CompareAndAddEvent {

        @Nested
        public class EmptyFailure {

            @Test
            public void expectationFailed() {
                final var receiver = OBJECT_A;
                final var sender = OBJECT_B;
                final var signalId = SIGNAL_ID_B;
                final var state0 = Integer.valueOf(0);
                final Duration end = WHEN_A;
                final Duration whenPreviousOccurred = end.plusDays(1);
                final Event<Integer> expectedPreviousEvent = new Event<Integer>(
                        new TimestampedId(SIGNAL_ID_A, whenPreviousOccurred), receiver, Integer.valueOf(1), Set.of());
                final Duration whenSent = end.plusDays(1);
                final Duration whenOccurred = whenSent.plusDays(1);
                final Signal<Integer> signal = new SignalTest.TestSignal(signalId, new TimestampedId(sender, whenSent),
                        receiver);
                final Event<Integer> event = new Event<Integer>(new TimestampedId(signalId, whenOccurred), receiver,
                        Integer.valueOf(2), Set.of());

                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, end, state0);
                history.addIncomingSignal(signal);// tough test
                final var stateVerifier = StepVerifier.create(history.observeState(whenOccurred));

                final var result = compareAndAddEvent(history, expectedPreviousEvent, event, signal);

                assertThat("result indicates failure", result, nullValue());
                stateVerifier.expectNext(Optional.of(state0)).verifyTimeout(FLUX_TIMEOUT);
            }

            @Test
            public void tooEarly() {
                final var receiver = OBJECT_A;
                final var sender = OBJECT_B;
                final var signalId = SIGNAL_ID_A;
                final Duration end = WHEN_A;
                final Event<Integer> expectedPreviousEvent = null;// tough test
                final Duration whenOccurred = end;// tough test
                final Duration whenSent = whenOccurred.minusSeconds(1);
                final Event<Integer> event = new Event<Integer>(new TimestampedId(signalId, whenOccurred), OBJECT_A,
                        Integer.valueOf(1), Set.of());
                final Signal<Integer> signal = new SignalTest.TestSignal(signalId, new TimestampedId(sender, whenSent),
                        receiver);
                final var state0 = Integer.valueOf(0);
                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, end, state0);
                history.addIncomingSignal(signal);// tough test
                final var stateVerifier = StepVerifier.create(history.observeState(whenOccurred));

                final var result = compareAndAddEvent(history, expectedPreviousEvent, event, signal);

                assertThat("result indicates failure", result, nullValue());
                stateVerifier.expectNext(Optional.of(state0)).expectComplete().verify();
            }

            @Test
            public void unknownSignal() {
                final var receiver = OBJECT_A;
                final var sender = OBJECT_B;
                final var signalId = SIGNAL_ID_A;
                final Duration end = WHEN_A;
                final Event<Integer> expectedPreviousEvent = null;// tough test
                final Duration whenOccurred = end.plusSeconds(2);
                final Duration whenSent = whenOccurred.minusSeconds(1);
                final Event<Integer> event = new Event<Integer>(new TimestampedId(signalId, whenOccurred), OBJECT_A,
                        Integer.valueOf(1), Set.of());
                final Signal<Integer> signal = new SignalTest.TestSignal(signalId, new TimestampedId(sender, whenSent),
                        receiver);

                final var state0 = Integer.valueOf(0);
                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, end, state0);
                assert !history.getIncomingSignals().contains(signal);// critical
                assert end.compareTo(whenOccurred) < 0;// tough test
                final var stateVerifier = StepVerifier.create(history.observeState(whenOccurred));

                final var result = compareAndAddEvent(history, expectedPreviousEvent, event, signal);

                assertThat("result indicates failure", result, nullValue());
                stateVerifier.expectNext(Optional.of(state0)).verifyTimeout(FLUX_TIMEOUT);
            }
        }// class

        @Nested
        public class EmptySuccess {

            @Test
            public void a() {
                final var end = WHEN_A;
                final var whenOccurred = end.plusSeconds(1);

                test(OBJECT_A, OBJECT_B, end, Integer.valueOf(0), SIGNAL_ID_A, whenOccurred, Integer.valueOf(1));
            }

            @Test
            public void close() {
                final var end = WHEN_B;
                final var whenOccurred = end.plusNanos(1);// critical

                test(OBJECT_B, OBJECT_A, end, Integer.valueOf(3), SIGNAL_ID_B, whenOccurred, Integer.valueOf(2));
            }

            @Test
            public void sameState() {
                final var end = WHEN_A;
                final var whenOccurred = end.plusSeconds(1);
                final var state = Integer.valueOf(0);

                test(OBJECT_A, OBJECT_B, end, state, SIGNAL_ID_A, whenOccurred, state);
            }

            private void test(@Nonnull final UUID receiver, @Nonnull final UUID sender, @Nonnull final Duration end,
                    @Nonnull final Integer state0, @Nonnull final UUID signalId, @Nonnull final Duration whenOccurred,
                    @Nullable final Integer state) {
                assert end.compareTo(whenOccurred) < 0;
                final Duration start = end;
                final Event<Integer> expectedPreviousEvent = null;
                final Duration whenSent = whenOccurred.minusSeconds(1);
                final Signal<Integer> signal = new SignalTest.TestSignal(signalId, new TimestampedId(sender, whenSent),
                        receiver);
                final Event<Integer> event = new Event<Integer>(new TimestampedId(signalId, whenOccurred), receiver,
                        state, Set.of());

                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);
                history.addIncomingSignal(signal);
                final var stateVerifier = StepVerifier.create(history.observeState(whenOccurred));

                final var result = compareAndAddEvent(history, expectedPreviousEvent, event, signal);

                final var stateHistory = history.getStateHistory();
                final var receivedSignals = history.getReceivedSignals();
                final var incomingSignals = history.getIncomingSignals();
                assertAll("result", () -> assertThat("indicates success", result, notNullValue()),
                        () -> assertThat("no events invalidated", result, empty()));
                assertAll("stateHistory", () -> assertThat("at start (unchanged)", stateHistory.get(start), is(state0)),
                        () -> assertThat("at whenOccurred", stateHistory.get(whenOccurred), is(state)),
                        () -> assertThat("added transition iff state changed",
                                stateHistory.getTransitionTimes().contains(whenOccurred) || state0.equals(state)));
                assertAll("receivedSignals", () -> assertThat(receivedSignals, hasItem(signal)),
                        () -> assertThat(receivedSignals, hasSize(1)));
                assertThat("incomingSignals", incomingSignals, empty());

                stateVerifier.expectNext(Optional.of(state)).verifyTimeout(FLUX_TIMEOUT);

            }

        }// class

        @Nested
        public class Invalidating {

            @Test
            public void a() {
                test(WHEN_A, Integer.valueOf(0), SIGNAL_ID_A, WHEN_B, Integer.valueOf(1), SIGNAL_ID_B, WHEN_C,
                        Integer.valueOf(2));
            }

            @Test
            public void close() {
                final var end = WHEN_B;
                final var whenOccurred1 = end.plusNanos(1);// critical
                final var whenOccurred2 = whenOccurred1.plusNanos(1);// critical
                test(end, Integer.valueOf(0), SIGNAL_ID_B, whenOccurred1, Integer.valueOf(1), SIGNAL_ID_A,
                        whenOccurred2, Integer.valueOf(2));
            }

            @Test
            public void sameState() {
                final var state0 = Integer.valueOf(0);
                final var state1 = state0;// critical
                final var state2 = state1;// critical
                test(WHEN_A, state0, SIGNAL_ID_A, WHEN_B, state1, SIGNAL_ID_B, WHEN_C, state2);
            }

            private void test(@Nonnull final Duration end, @Nonnull final Integer state0, @Nonnull final UUID signalId1,
                    @Nonnull final Duration whenOccurred1, @Nullable final Integer state1,
                    @Nonnull final UUID signalId2, @Nonnull final Duration whenOccurred2,
                    @Nullable final Integer state2) {
                assert end.compareTo(whenOccurred1) < 0;
                assert whenOccurred1.compareTo(whenOccurred2) < 0;
                final UUID receiver = OBJECT_A;
                final UUID sender = OBJECT_B;
                final Duration start = end;
                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);
                final Event<Integer> expectedPreviousEvent = null;
                final Duration whenSent1 = whenOccurred1.minusSeconds(1);
                final Signal<Integer> signal1 = new SignalTest.TestSignal(signalId1,
                        new TimestampedId(sender, whenSent1), receiver);
                final Event<Integer> event1 = new Event<Integer>(new TimestampedId(signalId1, whenOccurred1), receiver,
                        state1, Set.of());
                final Duration whenSent2 = whenOccurred2.minusSeconds(1);
                final Signal<Integer> signal2 = new SignalTest.TestSignal(signalId2,
                        new TimestampedId(sender, whenSent2), receiver);
                final Event<Integer> event2 = new Event<Integer>(new TimestampedId(signalId2, whenOccurred2), receiver,
                        state2, Set.of());
                history.addIncomingSignal(signal2);
                history.compareAndAddEvent(expectedPreviousEvent, event2, signal2);
                history.addIncomingSignal(signal1);
                final var stateVerifier = StepVerifier.create(history.observeState(whenOccurred2));

                final var result = compareAndAddEvent(history, expectedPreviousEvent, event1, signal1);

                final var stateHistory = history.getStateHistory();
                final var receivedSignals = history.getReceivedSignals();
                final var incomingSignals = history.getIncomingSignals();
                assertAll("result", () -> assertThat("indicates success", result, notNullValue()),
                        () -> assertThat("invalidated event", result, hasItem(event2)),
                        () -> assertThat("number invalidated", result, hasSize(1)));
                assertAll("stateHistory", () -> assertThat("at start (unchanged)", stateHistory.get(start), is(state0)),
                        () -> assertThat("at whenOccurred", stateHistory.get(whenOccurred1), is(state1)),
                        () -> assertThat("added transition iff state changed",
                                stateHistory.getTransitionTimes().contains(whenOccurred1) || state0.equals(state1)));
                assertAll("receivedSignals", () -> assertThat(receivedSignals, hasItem(signal1)),
                        () -> assertThat(receivedSignals, hasSize(1)));
                assertAll("incomingSignals", () -> assertThat(incomingSignals, hasItem(signal2)),
                        () -> assertThat(incomingSignals, hasSize(1)));
                stateVerifier.expectNext(Optional.of(state1)).verifyTimeout(FLUX_TIMEOUT);
            }

        }// class

        @Nested
        public class Two {

            @Test
            public void a() {
                test(WHEN_A, Integer.valueOf(0), SIGNAL_ID_A, WHEN_B, Integer.valueOf(1), SIGNAL_ID_B, WHEN_C,
                        Integer.valueOf(2));
            }

            @Test
            public void close() {
                final var end = WHEN_B;
                final var whenOccurred1 = end.plusNanos(1);// critical
                final var whenOccurred2 = whenOccurred1.plusNanos(1);// critical
                test(end, Integer.valueOf(3), SIGNAL_ID_B, whenOccurred1, Integer.valueOf(2), SIGNAL_ID_A,
                        whenOccurred2, Integer.valueOf(1));
            }

            private void test(@Nonnull final Duration end, @Nonnull final Integer state0, @Nonnull final UUID signalId1,
                    @Nonnull final Duration whenOccurred1, @Nullable final Integer state1,
                    @Nonnull final UUID signalId2, @Nonnull final Duration whenOccurred2,
                    @Nullable final Integer state2) {
                assert end.compareTo(whenOccurred1) < 0;
                assert whenOccurred1.compareTo(whenOccurred2) < 0;
                assert !Objects.equals(state0, state1);
                assert !Objects.equals(state1, state2);

                final UUID receiver = OBJECT_A;
                final UUID sender = OBJECT_B;
                final Duration start = end;
                final Event<Integer> expectedPreviousEvent1 = null;
                final Duration whenSent1 = whenOccurred1.minusSeconds(1);
                final Signal<Integer> signal1 = new SignalTest.TestSignal(signalId1,
                        new TimestampedId(sender, whenSent1), receiver);
                final Event<Integer> event1 = new Event<Integer>(new TimestampedId(signalId1, whenOccurred1), receiver,
                        state1, Set.of());
                final Event<Integer> expectedPreviousEvent2 = event1;
                final Duration whenSent2 = whenOccurred1.minusSeconds(1);
                final Signal<Integer> signal2 = new SignalTest.TestSignal(signalId2,
                        new TimestampedId(sender, whenSent2), receiver);
                final Event<Integer> event2 = new Event<Integer>(new TimestampedId(signalId2, whenOccurred2), receiver,
                        state2, Set.of());

                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);
                final var stateVerifier = StepVerifier.create(history.observeState(whenOccurred2));
                history.addIncomingSignal(signal1);
                history.compareAndAddEvent(expectedPreviousEvent1, event1, signal1);
                history.addIncomingSignal(signal2);

                final var result = compareAndAddEvent(history, expectedPreviousEvent2, event2, signal2);

                final var stateHistory = history.getStateHistory();
                final var receivedSignals = history.getReceivedSignals();
                final var incomingSignals = history.getIncomingSignals();
                assertAll("result", () -> assertThat("indicates success", result, notNullValue()),
                        () -> assertThat("no events invalidated", result, empty()));
                assertAll("stateHistory", () -> assertThat("at start (unchanged)", stateHistory.get(start), is(state0)),
                        () -> assertThat("at whenOccurred1 (unchanged)", stateHistory.get(whenOccurred1), is(state1)),
                        () -> assertThat("at whenOccurred2", stateHistory.get(whenOccurred2), is(state2)),
                        () -> assertThat("transitionTimes", stateHistory.getTransitionTimes(),
                                allOf(hasItem(whenOccurred1), hasItem(whenOccurred2))));
                assertThat("receivedSignals", receivedSignals, allOf(hasItem(signal1), hasItem(signal2)));
                assertThat("incomingSignals", incomingSignals, empty());

                stateVerifier.expectNext(Optional.of(state2)).verifyTimeout(FLUX_TIMEOUT);
            }

        }// class

        @RepeatedTest(4)
        public void multipleThreads() {
            final int nThreads = 16;
            final int nEventsPerThread = 64;

            final UUID receiver = OBJECT_A;
            final UUID sender = OBJECT_B;
            final Duration end = WHEN_A;
            final Integer state0 = Integer.valueOf(0);
            final Duration start = end;
            final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);

            final CountDownLatch ready = new CountDownLatch(1);
            final List<Future<Void>> futures = new ArrayList<>(nThreads);

            for (int t = 0; t < nThreads; ++t) {
                futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> {
                    final var random = new Random();
                    for (int e = 0; e < nEventsPerThread; ++e) {
                        final UUID signalId = UUID.randomUUID();
                        final var events0 = history.getEvents();
                        final var hasPrevious = events0.isEmpty();
                        final Event<Integer> expectedPreviousEvent = hasPrevious ? null : events0.last();
                        final Duration whenPrevious = expectedPreviousEvent == null ? end
                                : expectedPreviousEvent.getWhenOccurred();
                        final Duration whenOccurred = whenPrevious.plusMillis(1 + random.nextInt(10_000));
                        final Integer state = Integer.valueOf(random.nextInt());
                        final Duration whenSent = whenOccurred.minusSeconds(1);
                        final Signal<Integer> signal = new SignalTest.TestSignal(signalId,
                                new TimestampedId(sender, whenSent), receiver);
                        final Event<Integer> event = new Event<Integer>(new TimestampedId(signalId, whenOccurred),
                                receiver, state, Set.of());

                        history.addIncomingSignal(signal);
                        history.compareAndAddEvent(expectedPreviousEvent, event, signal);
                    } // for
                }));
            } // for

            ready.countDown();
            ThreadSafetyTest.get(futures);
            assertInvariants(history);
        }

    }// class

    @Nested
    public class ComputeContinuation {

        @Nested
        public class Empty {

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

                final var continuation = computeContinuation(history);

                assertThat("continuation", continuation, nullValue());
            }
        }// class

        @Nested
        public class FirstSignal {

            @Test
            public void a() {
                test(OBJECT_A, OBJECT_B, WHEN_A, WHEN_B, SIGNAL_ID_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, OBJECT_A, WHEN_B, WHEN_C, SIGNAL_ID_B, Integer.valueOf(1));
            }

            private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver, @Nonnull final Duration start,
                    @Nonnull final Duration whenSent, @Nonnull final UUID signalId, @Nonnull final Integer state) {
                final var sentFrom = new TimestampedId(sender, whenSent);
                final var signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);
                final Duration expectedWhenReceived = signal.getWhenReceived(state);

                final var history = new ObjectHistory<>(receiver, start, state);
                history.addIncomingSignal(signal);

                final ObjectHistory.Continuation<Integer> continuation = computeContinuation(history);

                assertThat("continuation", continuation, notNullValue());// guard
                assertAll(() -> assertThat("nextSignal", continuation.nextSignal, sameInstance(signal)),
                        () -> assertThat("previousEvent", continuation.previousEvent, nullValue()),
                        () -> assertThat("state", continuation.state, sameInstance(state)),
                        () -> assertThat("expectedWhenReceived", continuation.whenNextSignalReceived,
                                is(expectedWhenReceived)));
            }
        }// class

        @Nested
        public class PreviousEvent {

            @Test
            public void a() {
                test(WHEN_A, Integer.valueOf(0), WHEN_B);
            }

            @Test
            public void b() {
                test(WHEN_B, Integer.valueOf(1), WHEN_C);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Integer state0,
                    @Nonnull final Duration whenSent1) {
                final UUID sender = OBJECT_A;
                final UUID receiver = OBJECT_B;

                final var history = new ObjectHistory<>(receiver, start, state0);

                final var sentFrom1 = new TimestampedId(sender, whenSent1);
                final var signal1 = new SignalTest.TestSignal(SIGNAL_ID_A, sentFrom1, receiver);
                history.addIncomingSignal(signal1);
                final var event1 = signal1.receive(state0);
                final var state1 = event1.getState();
                history.compareAndAddEvent(null, event1, signal1);

                final Duration whenSent2 = event1.getWhenOccurred();// ensure order
                final var sentFrom2 = new TimestampedId(sender, whenSent2);
                final var signal2 = new SignalTest.TestSignal(SIGNAL_ID_B, sentFrom2, receiver);
                history.addIncomingSignal(signal2);
                final Duration expectedWhenReceived = signal2.getWhenReceived(state1);

                final ObjectHistory.Continuation<Integer> continuation = computeContinuation(history);

                assertThat("continuation", continuation, notNullValue());// guard
                assertAll(() -> assertThat("nextSignal", continuation.nextSignal, sameInstance(signal2)),
                        () -> assertThat("previousEvent", continuation.previousEvent, sameInstance(event1)),
                        () -> assertThat("state", continuation.state, sameInstance(state1)),
                        () -> assertThat("expectedWhenReceived", continuation.whenNextSignalReceived,
                                is(expectedWhenReceived)));
            }
        }// class

        @Nested
        public class TwoIncoming {

            @Test
            public void a() {
                final Duration start = WHEN_A;
                final Duration whenSent1 = WHEN_B;
                final Duration whenSent2 = WHEN_C;
                test(OBJECT_A, OBJECT_B, OBJECT_C, start, Integer.valueOf(0), whenSent1, SIGNAL_ID_A, whenSent2,
                        SIGNAL_ID_B);
            }

            @Test
            public void b() {
                final Duration start = WHEN_A;
                final Duration whenSent1 = WHEN_B;
                final Duration whenSent2 = WHEN_C;
                test(OBJECT_A, OBJECT_B, OBJECT_C, start, Integer.valueOf(0), whenSent1, SIGNAL_ID_B, whenSent2,
                        SIGNAL_ID_A);
            }

            @Test
            public void close() {
                final Duration start = WHEN_B;
                final Duration whenSent1 = start;
                final Duration whenSent2 = whenSent1.plusNanos(1);// critical
                test(OBJECT_B, OBJECT_C, OBJECT_A, start, Integer.valueOf(1), whenSent1, SIGNAL_ID_B, whenSent2,
                        SIGNAL_ID_A);
            }

            @Test
            public void simultaneousReception() {
                final Duration start = WHEN_B;
                final Duration whenSent1 = start;
                final Duration whenSent2 = start;// critical
                final UUID signal1;
                final UUID signal2;
                if (SIGNAL_ID_A.compareTo(SIGNAL_ID_B) < 0) {
                    signal1 = SIGNAL_ID_A;
                    signal2 = SIGNAL_ID_B;
                } else {
                    signal1 = SIGNAL_ID_B;
                    signal2 = SIGNAL_ID_A;
                }
                test(OBJECT_A, OBJECT_B, OBJECT_C, start, Integer.valueOf(0), whenSent1, signal1, whenSent2, signal2);
            }

            private void test(@Nonnull final UUID sender1, @Nonnull final UUID sender2, @Nonnull final UUID receiver,
                    @Nonnull final Duration start, @Nonnull final Integer state, @Nonnull final Duration whenSent1,
                    @Nonnull final UUID signalId1, @Nonnull final Duration whenSent2, @Nonnull final UUID signalId2) {
                final var sentFrom1 = new TimestampedId(sender1, whenSent1);
                final var signal1 = new SignalTest.TestSignal(signalId1, sentFrom1, receiver);
                final var sentFrom2 = new TimestampedId(sender2, whenSent2);
                final var signal2 = new SignalTest.TestSignal(signalId2, sentFrom2, receiver);
                final Duration expectedWhenReceived = signal1.getWhenReceived(state);

                final var history = new ObjectHistory<>(receiver, start, state);
                history.addIncomingSignal(signal1);
                history.addIncomingSignal(signal2);

                final ObjectHistory.Continuation<Integer> continuation = computeContinuation(history);

                assertThat("continuation", continuation, notNullValue());// guard
                assertAll(() -> assertThat("nextSignal", continuation.nextSignal, sameInstance(signal1)),
                        () -> assertThat("previousEvent", continuation.previousEvent, nullValue()),
                        () -> assertThat("state", continuation.state, sameInstance(state)),
                        () -> assertThat("expectedWhenReceived", continuation.whenNextSignalReceived,
                                is(expectedWhenReceived)));
            }
        }// class

        @RepeatedTest(4)
        public void multipleThreads() {
            final int nThreads = 16;
            final int nCallsPerThread = 8;
            final int nSignals = nThreads * nCallsPerThread;

            final UUID receiver = OBJECT_A;
            final UUID sender = OBJECT_B;
            final Duration end = WHEN_A;
            final Integer state0 = Integer.valueOf(0);
            final Duration start = end;
            final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);
            final var random = new Random();
            for (int s = 0; s < nSignals; ++s) {
                final UUID signalId = UUID.randomUUID();
                final Duration whenSent = end.plusMillis(1 + random.nextInt(10_000));
                final var sentFrom = new TimestampedId(sender, whenSent);
                final var signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);

                history.addIncomingSignal(signal);
            } // for

            final CountDownLatch ready = new CountDownLatch(1);
            final List<Future<Void>> futures = new ArrayList<>(nThreads);

            for (int t = 0; t < nThreads; ++t) {
                futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> {
                    for (int c = 0; c < nCallsPerThread; ++c) {
                        final var continuation = history.computeContinuation();
                        if (continuation != null) {
                            final var event = continuation.nextSignal.receive(continuation.whenNextSignalReceived,
                                    continuation.state);
                            history.compareAndAddEvent(continuation.previousEvent, event, continuation.nextSignal);
                        }
                    } // for
                }));
            } // for

            ready.countDown();
            ThreadSafetyTest.get(futures);
            assertInvariants(history);
        }

        @Test
        public void outOfOrderSignals() {
            final UUID sender = OBJECT_A;
            final UUID receiver = OBJECT_B;
            final Duration start = WHEN_A;
            final Integer state0 = Integer.valueOf(0);
            final Duration whenSent1 = WHEN_B;
            final Duration whenSent2 = WHEN_B.plusDays(365);

            final var history = new ObjectHistory<>(receiver, start, state0);

            final var sentFrom2 = new TimestampedId(sender, whenSent2);
            final var signal2 = new SignalTest.TestSignal(SIGNAL_ID_B, sentFrom2, receiver);
            history.addIncomingSignal(signal2);
            final var event2 = signal2.receive(state0);
            history.compareAndAddEvent(null, event2, signal2);

            final var sentFrom1 = new TimestampedId(sender, whenSent1);
            final var signal1 = new SignalTest.TestSignal(SIGNAL_ID_A, sentFrom1, receiver);
            history.addIncomingSignal(signal1);
            assert signal1.getWhenReceived(state0).compareTo(signal2.getWhenReceived(state0)) < 0;
            final Duration expectedWhenReceived = signal1.getWhenReceived(state0);

            final ObjectHistory.Continuation<Integer> continuation = computeContinuation(history);

            assertThat("continuation", continuation, notNullValue());// guard
            assertAll(() -> assertThat("nextSignal", continuation.nextSignal, sameInstance(signal1)),
                    () -> assertThat("previousEvent", continuation.previousEvent, nullValue()),
                    () -> assertThat("state", continuation.state, sameInstance(state0)),
                    () -> assertThat("expectedWhenReceived", continuation.whenNextSignalReceived,
                            is(expectedWhenReceived)));
        }

    }// class

    @Nested
    public class Constructor {

        @Nested
        public class Copy {

            @Nested
            public class IncomingSignal {

                @Test
                public void a() {
                    test(OBJECT_A, OBJECT_B, SIGNAL_ID_A, WHEN_A, WHEN_B);
                }

                @Test
                public void b() {
                    test(OBJECT_B, OBJECT_C, SIGNAL_ID_B, WHEN_B, WHEN_C);
                }

                private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver,
                        @Nonnull final UUID signalId, @Nonnull final Duration start, @Nonnull final Duration whenSet) {
                    final Integer state0 = Integer.valueOf(0);
                    final var history = new ObjectHistory<>(receiver, start, state0);
                    final TimestampedId sentFrom = new TimestampedId(sender, whenSet);
                    final Signal<Integer> signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);
                    history.addIncomingSignal(signal);
                    assert !history.getIncomingSignals().isEmpty();

                    final var copy = constructor(history);

                    assertThat("incomingSignals", copy.getIncomingSignals(), is(Set.of(signal)));
                }

            }// class

            @Nested
            public class NoSignalsNoEvents {

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
            public class ReceivedSignal {

                @Test
                public void a() {
                    test(OBJECT_A, OBJECT_B, SIGNAL_ID_A, WHEN_A, WHEN_B);
                }

                @Test
                public void b() {
                    test(OBJECT_B, OBJECT_C, SIGNAL_ID_B, WHEN_B, WHEN_C);
                }

                private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver,
                        @Nonnull final UUID signalId, @Nonnull final Duration start, @Nonnull final Duration whenSet) {
                    final Integer state0 = Integer.valueOf(0);
                    final var history = new ObjectHistory<>(receiver, start, state0);
                    final TimestampedId sentFrom = new TimestampedId(sender, whenSet);
                    final Signal<Integer> signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);
                    history.addIncomingSignal(signal);
                    final var medium = new MediumTest.RecordingMedium<Integer>();
                    history.receiveNextSignal(medium);
                    assert !history.getReceivedSignals().isEmpty();

                    final var copy = constructor(history);

                    assertAll(() -> assertThat("receivedSignals", copy.getReceivedSignals(), is(Set.of(signal))),
                            () -> assertThat("events", copy.getEvents(), is(history.getEvents())));
                }

            }// class

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

    public static class ContinuationTest {

        static <STATE> void assertInvariants(@Nonnull final ObjectHistory.Continuation<STATE> continuation) {
            ObjectTest.assertInvariants(continuation);// inherited
            assertAll("Not null", () -> assertThat("nextSignal", continuation.nextSignal, notNullValue()),
                    () -> assertThat("whenNextSignalReceived", continuation.whenNextSignalReceived, notNullValue()));
        }

        static <STATE> void assertInvariants(@Nonnull final ObjectHistory.Continuation<STATE> continuation1,
                @Nonnull final ObjectHistory.Continuation<STATE> continuation2) {
            ObjectTest.assertInvariants(continuation1, continuation2);// inherited
            assertAll("Value semantrics",
                    () -> EqualsSemanticsTest.assertValueSemantics(continuation1, continuation2, "nextSignal",
                            c -> c.nextSignal),
                    () -> EqualsSemanticsTest.assertValueSemantics(continuation1, continuation2, "previousEvent",
                            c -> c.previousEvent),
                    () -> EqualsSemanticsTest.assertValueSemantics(continuation1, continuation2, "state", c -> c.state),
                    () -> EqualsSemanticsTest.assertValueSemantics(continuation1, continuation2,
                            "whenNextSignalReceived", c -> c.whenNextSignalReceived));
        }

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

                StepVerifier.create(states).expectNext(expectedState).expectTimeout(FLUX_TIMEOUT).verify();
            }

        }// class

    }// class

    @Nested
    public class ReceiveNextSignal {

        @Nested
        public class First {
            @Test
            public void a() {
                test(OBJECT_A, OBJECT_B, WHEN_A, WHEN_B, SIGNAL_ID_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, OBJECT_A, WHEN_B, WHEN_C, SIGNAL_ID_B, Integer.valueOf(1));
            }

            private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver, @Nonnull final Duration start,
                    @Nonnull final Duration whenSent, @Nonnull final UUID signalId, @Nonnull final Integer state0) {
                final boolean strobe = true;// tough test
                final var sentFrom = new TimestampedId(sender, whenSent);
                final var signal = new SignalTest.TestSignal(signalId, sentFrom, receiver, strobe);
                final var expectedEvent = signal.receive(state0);
                final var expectedWhenOccurred = expectedEvent.getWhenOccurred();
                final var expectedState = expectedEvent.getState();

                final var history = new ObjectHistory<>(receiver, start, state0);
                final Medium<Integer> medium = new MediumTest.RecordingMedium<>();
                medium.addAll(List.of(signal));
                history.addIncomingSignal(signal);
                final var signals0 = medium.getSignals();
                final var stateVerifier = StepVerifier.create(history.observeState(expectedWhenOccurred));

                final var didWork = receiveNextSignal(history, medium);

                final var stateHistory = history.getStateHistory();
                final var signals = medium.getSignals();
                final var invalidatedSignals = difference(signals0, signals);
                assertAll("transmitted signals", () -> assertThat("none invalidated", invalidatedSignals, empty()),
                        () -> assertThat("added emitted signal", signals,
                                hasSize(1 + expectedEvent.getSignalsEmitted().size())));
                assertThat("events", history.getEvents(), hasItem(expectedEvent));
                assertAll("stateHistory", () -> assertThat("at start (unchanged)", stateHistory.get(start), is(state0)),
                        () -> assertThat("at event.whenOccurred", stateHistory.get(expectedWhenOccurred),
                                is(expectedState)),
                        () -> assertThat("added transition iff state changed",
                                stateHistory.getTransitionTimes().contains(expectedWhenOccurred)
                                        || state0.equals(expectedState)));
                assertThat("incomingSignals", history.getIncomingSignals(), empty());
                assertThat("receivedSignals", history.getReceivedSignals(), hasItem(signal));
                assertThat("did some work", didWork);

                stateVerifier.expectNext(Optional.of(expectedState)).verifyTimeout(FLUX_TIMEOUT);
            }
        }// class

        @Nested
        public class Invalidating {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, WHEN_C);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, WHEN_D);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent1,
                    @Nonnull final Duration whenSent2) {
                assert start.compareTo(whenSent1) < 0;
                assert whenSent1.compareTo(whenSent2) < 0;
                final UUID sender = OBJECT_A;
                final UUID receiver = OBJECT_B;
                final Integer state0 = Integer.valueOf(0);
                final boolean strobe1 = false;// simplification
                final boolean strobe2 = true;// tough test: emitted signals

                final var sentFrom1 = new TimestampedId(sender, whenSent1);
                final var signal1 = new SignalTest.TestSignal(SIGNAL_ID_A, sentFrom1, receiver, strobe1);
                final var sentFrom2 = new TimestampedId(sender, whenSent2);
                final var signal2 = new SignalTest.TestSignal(SIGNAL_ID_B, sentFrom2, receiver, strobe2);

                final var history = new ObjectHistory<>(receiver, start, state0);
                final Medium<Integer> medium = new MediumTest.RecordingMedium<>();
                medium.addAll(List.of(signal1, signal2));
                history.addIncomingSignal(signal2);
                history.receiveNextSignal(medium);
                history.addIncomingSignal(signal1);
                final var signals0 = medium.getSignals();

                final boolean didWork = receiveNextSignal(history, medium);

                final var signals = medium.getSignals();
                final var invalidatedSignals = difference(signals0, signals);
                assertAll(() -> assertThat("invalidated some signals", invalidatedSignals, not(empty())),
                        () -> assertThat("events", history.getEvents(), hasSize(1)),
                        () -> assertThat("incomingSignals", history.getIncomingSignals(), is(Set.of(signal2))),
                        () -> assertThat("receivedSignals", history.getReceivedSignals(), is(Set.of(signal1))));
                assertThat("did some work", didWork);
            }
        }// class

        @Nested
        public class Second {

            @Test
            public void a() {
                test(WHEN_A, Integer.valueOf(0), WHEN_B);
            }

            @Test
            public void b() {
                test(WHEN_B, Integer.valueOf(1), WHEN_C);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Integer state0,
                    @Nonnull final Duration whenSent1) {
                final UUID sender = OBJECT_A;
                final UUID receiver = OBJECT_B;

                final var history = new ObjectHistory<>(receiver, start, state0);
                final Medium<Integer> medium = new MediumTest.RecordingMedium<>();
                final var sentFrom1 = new TimestampedId(sender, whenSent1);
                final var signal1 = new SignalTest.TestSignal(SIGNAL_ID_A, sentFrom1, receiver);
                medium.addAll(List.of(signal1));
                history.addIncomingSignal(signal1);
                history.receiveNextSignal(medium);
                final Duration whenSent2 = history.getEvents().last().getWhenOccurred();// ensure order
                final var sentFrom2 = new TimestampedId(sender, whenSent2);
                final var signal2 = new SignalTest.TestSignal(SIGNAL_ID_B, sentFrom2, receiver);
                medium.addAll(List.of(signal2));
                history.addIncomingSignal(signal2);
                final var signals0 = medium.getSignals();

                final var didWork = receiveNextSignal(history, medium);

                final var signals = medium.getSignals();
                final var invalidatedSignals = difference(signals0, signals);
                final var stateHistory = history.getStateHistory();
                assertThat("invalidated signals", invalidatedSignals, empty());
                assertThat("events", history.getEvents(), hasSize(2));
                CollectionTest.assertForAllElements("stateHistory at when events occurred", history.getEvents(), e -> {
                    assertThat(stateHistory.get(e.getWhenOccurred()), sameInstance(e.getState()));
                });
                assertThat("incomingSignals", history.getIncomingSignals(), empty());
                assertThat("receivedSignals", history.getReceivedSignals(), allOf(hasItem(signal1), hasItem(signal2)));
                assertThat("did some work", didWork);
            }
        }// class

        @Nested
        public class Simultaneous {

            @Test
            public void a() {
                test(SIGNAL_ID_A, SIGNAL_ID_B);
            }

            @Test
            public void b() {
                test(SIGNAL_ID_B, SIGNAL_ID_A);
            }

            private void test(@Nonnull final UUID signalIdA, @Nonnull final UUID signalIdB) {
                assert !signalIdA.equals(signalIdB);
                final UUID sender1 = OBJECT_A;
                final UUID sender2 = OBJECT_B;
                final UUID receiver = OBJECT_C;
                final Duration start = WHEN_A;
                final Duration whenSent = WHEN_B;
                final Integer state0 = Integer.valueOf(0);

                final UUID firstSignal = signalIdA.compareTo(signalIdB) < 0 ? signalIdA : signalIdB;
                final var sentFrom1 = new TimestampedId(sender1, whenSent);
                final var sentFrom2 = new TimestampedId(sender2, whenSent);
                final var signal1 = new SignalTest.TestSignal(signalIdA, sentFrom1, receiver);
                final var signal2 = new SignalTest.TestSignal(signalIdB, sentFrom2, receiver);

                final var history = new ObjectHistory<>(receiver, start, state0);
                final Medium<Integer> medium = new MediumTest.RecordingMedium<>();
                medium.addAll(List.of(signal1, signal2));
                history.addIncomingSignal(signal1);
                history.addIncomingSignal(signal2);
                history.receiveNextSignal(medium);

                final var didWork = receiveNextSignal(history, medium);

                final var events = history.getEvents();
                assertThat("events", events, hasSize(2));// guard
                assertThat("the signal of first event is the first signal", events.first().getCausingSignal(),
                        sameInstance(firstSignal));
                assertThat("did some work", didWork);
            }

        }// class

        @Nested
        public class SimultaneousDistinct {

            @Test
            public void a() {
                test(SIGNAL_ID_A, SIGNAL_ID_B);
            }

            @Test
            public void b() {
                test(SIGNAL_ID_B, SIGNAL_ID_A);
            }

            private void test(@Nonnull final UUID signalIdA, @Nonnull final UUID signalIdB) {
                assert !signalIdA.equals(signalIdB);
                final UUID sender1 = OBJECT_A;
                final UUID sender2 = OBJECT_B;
                final UUID receiver = OBJECT_C;
                final Duration start = WHEN_A;
                final Duration whenSent = WHEN_B;
                final Integer state0 = Integer.valueOf(0);

                final UUID firstSignal = signalIdA.compareTo(signalIdB) < 0 ? signalIdA : signalIdB;
                final var sentFrom1 = new TimestampedId(sender1, whenSent);
                final var sentFrom2 = new TimestampedId(sender2, whenSent);
                final var signal1 = new SignalTest.TestSignal(signalIdA, sentFrom1, receiver);
                final var signal2 = new SignalTest.TestSignal(signalIdB, sentFrom2, receiver);

                final var history = new ObjectHistory<>(receiver, start, state0);
                final Medium<Integer> medium = new MediumTest.RecordingMedium<>();
                medium.addAll(List.of(signal1, signal2));
                history.addIncomingSignal(signal1);
                /*
                 * critical: receive the first signal before adding the second signal, so the
                 * stateHistory used for deciding when signal2 is received includes the effect
                 * of signal1.
                 */
                history.receiveNextSignal(medium);
                history.addIncomingSignal(signal2);

                final var didWork = receiveNextSignal(history, medium);

                final var events = history.getEvents();
                assertThat("events", events, either(hasSize(2)).or(hasSize(1)));// guard
                assertThat("the signal of first event is the first signal", events.first().getCausingSignal(),
                        sameInstance(firstSignal));
                assertThat("did some work", didWork);
            }

        }// class

        @RepeatedTest(4)
        public void multipleThreads() {
            final int nThreads = 16;
            final int nSignalsPerThread = 8;

            final UUID receiver = OBJECT_A;
            final UUID sender = OBJECT_B;
            final Duration end = WHEN_A;
            final Integer state0 = Integer.valueOf(0);
            final Duration start = end;
            final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);
            final Medium<Integer> medium = new MediumTest.RecordingMedium<>();

            final CountDownLatch ready = new CountDownLatch(1);
            final List<Future<Void>> futures = new ArrayList<>(nThreads);

            for (int t = 0; t < nThreads; ++t) {
                futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> {
                    final var random = new Random();
                    for (int s = 0; s < nSignalsPerThread; ++s) {
                        final UUID signalId = UUID.randomUUID();
                        final Duration whenSent = end.plusMillis(1 + random.nextInt(10_000));
                        final var sentFrom = new TimestampedId(sender, whenSent);
                        final var signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);
                        medium.addAll(List.of(signal));
                        history.addIncomingSignal(signal);

                        history.receiveNextSignal(medium);
                    } // for
                }));
            } // for

            ready.countDown();
            ThreadSafetyTest.get(futures);
            assertInvariants(history);
        }

        @Test
        public void none() {
            final var history = new ObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(0));
            final Medium<Integer> medium = new MediumTest.RecordingMedium<>();

            final boolean didWork = receiveNextSignal(history, medium);

            assertThat("result", !didWork);
        }
    }// class

    @Nested
    public class RemoveSignals {

        @Nested
        public class Absent {

            @Test
            public void a() {
                test(SIGNAL_ID_A, SIGNAL_ID_B, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(SIGNAL_ID_B, SIGNAL_ID_A, Integer.valueOf(1));
            }

            private void test(@Nonnull final UUID presentSignal, @Nonnull final UUID signalToRemove,
                    final Integer state0) {
                assert !presentSignal.equals(signalToRemove);
                final UUID receiver = OBJECT_A;
                final UUID sender = OBJECT_B;
                final Duration end = WHEN_A;
                final Duration start = end;
                final Duration whenOccurred = end.plusSeconds(1);
                final Integer state = Integer.valueOf(1);
                final Duration whenSent = whenOccurred.minusSeconds(1);
                final Signal<Integer> signal = new SignalTest.TestSignal(presentSignal,
                        new TimestampedId(sender, whenSent), receiver);
                final Event<Integer> event = new Event<Integer>(new TimestampedId(presentSignal, whenOccurred),
                        receiver, state, Set.of());

                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);
                history.addIncomingSignal(signal);
                history.compareAndAddEvent(null, event, signal);
                final var receivedSignals0 = history.getReceivedSignals();
                final var incomingSignals0 = history.getIncomingSignals();
                final var events0 = history.getEvents();
                assert !events0.isEmpty();

                final var result = removeSignals(history, Set.of(signalToRemove));

                assertAll("Unchanged",
                        () -> assertThat("receivedSignals", history.getReceivedSignals(), is(receivedSignals0)),
                        () -> assertThat("incomingSignals", history.getIncomingSignals(), is(incomingSignals0)),
                        () -> assertThat("events", history.getEvents(), is(events0)));
                assertThat("removed emitted signals", result, empty());
            }

        }// class

        @Nested
        public class Empty {

            @Test
            public void none() {
                test(Set.of(), Integer.valueOf(0));
            }

            @Test
            public void one() {
                test(Set.of(SIGNAL_ID_A), Integer.valueOf(1));
            }

            private void test(@Nonnull final Set<UUID> signals, @Nonnull final Integer state0) {
                final var history = new ObjectHistory<>(OBJECT_A, WHEN_A, state0);
                final var stateVerifier = StepVerifier.create(history.observeState(ValueHistory.END_OF_TIME));

                final var removed = removeSignals(history, signals);

                assertThat("removed emitted signals", removed, empty());
                stateVerifier.expectNext(Optional.of(state0)).verifyTimeout(FLUX_TIMEOUT);
            }

            @Test
            public void two() {
                test(Set.of(SIGNAL_ID_A, SIGNAL_ID_B), Integer.valueOf(0));
            }

        }// class

        @Nested
        public class Present {

            @Test
            public void a() {
                test(SIGNAL_ID_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(SIGNAL_ID_B, Integer.valueOf(1));
            }

            private void test(@Nonnull final UUID signalId, @Nonnull final Integer state0) {
                final UUID receiver = OBJECT_A;
                final UUID sender = OBJECT_A;
                final Duration end = WHEN_A;
                final Duration start = end;
                final Duration whenSent = end.plusSeconds(2);
                final boolean strobe = true;// tough test
                final Signal<Integer> signal = new SignalTest.TestSignal(signalId, new TimestampedId(sender, whenSent),
                        receiver, strobe);

                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);
                final Medium<Integer> medium = new MediumTest.RecordingMedium<>();
                history.addIncomingSignal(signal);
                history.receiveNextSignal(medium);
                final var event = history.getEvents().last();
                final var emittedSignals = event.getSignalsEmitted();
                final var eventTime = event.getWhenOccurred();
                final var stateVerifier = StepVerifier.create(history.observeState(eventTime));

                final var removedEmittedSignals = removeSignals(history, Set.of(signalId));

                assertAll("Removed from", () -> assertThat("receivedSignals", history.getReceivedSignals(), empty()),
                        () -> assertThat("incomingSignals", history.getIncomingSignals(), empty()),
                        () -> assertThat("events", history.getEvents(), empty()));
                assertThat("removed emitted signals", removedEmittedSignals, is(emittedSignals));
                stateVerifier.expectNext(Optional.of(state0)).verifyTimeout(FLUX_TIMEOUT);
            }

        }// class

        @Nested
        public class WithSubsequentSignal {

            @Test
            public void a() {
                test(WHEN_A, SIGNAL_ID_A, WHEN_B, SIGNAL_ID_B, WHEN_C);
            }

            @Test
            public void close() {
                final var end = WHEN_B;
                final var whenOccurred1 = end.plusNanos(1);// critical
                final var whenOccurred2 = whenOccurred1.plusNanos(1);// critical
                test(end, SIGNAL_ID_B, whenOccurred1, SIGNAL_ID_A, whenOccurred2);
            }

            private void test(@Nonnull final Duration end, @Nonnull final UUID signalId1,
                    @Nonnull final Duration whenOccurred1, @Nonnull final UUID signalId2,
                    @Nonnull final Duration whenOccurred2) {
                assert !signalId1.equals(signalId2);
                assert end.compareTo(whenOccurred1) < 0;
                assert whenOccurred1.compareTo(whenOccurred2) < 0;

                final UUID receiver = OBJECT_A;
                final UUID sender = OBJECT_B;
                final Integer state0 = Integer.valueOf(0);
                final Integer state1 = Integer.valueOf(1);
                final Integer state2 = Integer.valueOf(2);
                final Duration start = end;

                final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);
                final Duration whenSent1 = whenOccurred1.minusSeconds(1);
                final Signal<Integer> signal1 = new SignalTest.TestSignal(signalId1,
                        new TimestampedId(sender, whenSent1), receiver);
                final Event<Integer> event1 = new Event<Integer>(new TimestampedId(signalId1, whenOccurred1), receiver,
                        state1, Set.of());
                final Duration whenSent2 = whenOccurred2.minusSeconds(1);
                final Signal<Integer> signal2 = new SignalTest.TestSignal(signalId2,
                        new TimestampedId(sender, whenSent2), receiver);
                final Event<Integer> event2 = new Event<Integer>(new TimestampedId(signalId2, whenOccurred2), receiver,
                        state2, Set.of());
                history.addIncomingSignal(signal1);
                history.compareAndAddEvent(null, event1, signal1);
                history.addIncomingSignal(signal2);
                history.compareAndAddEvent(event1, event2, signal2);
                final var signals = Set.of(signalId1);
                assert !signals.contains(signalId2);

                removeSignals(history, signals);

                assertThat("removed both events", history.getEvents(), empty());
                assertThat("rescheduled reception of signal", history.getIncomingSignals(), is(Set.of(signal2)));
            }

        }// class

        @Test
        public void alsoRemovingAnEmittedSignal() {
            final UUID receiver = OBJECT_A;
            final UUID sender = OBJECT_A;
            final UUID signalId = SIGNAL_ID_A;
            final Duration end = WHEN_A;
            final Duration start = end;
            final Duration whenSent = end.plusSeconds(2);
            final boolean strobe = true;// tough test
            final Integer state0 = Integer.valueOf(0);
            final Signal<Integer> signal = new SignalTest.TestSignal(signalId, new TimestampedId(sender, whenSent),
                    receiver, strobe);

            final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);
            final Medium<Integer> medium = new MediumTest.RecordingMedium<>();
            history.addIncomingSignal(signal);
            history.receiveNextSignal(medium);
            final var emittedSignals = history.getEvents().last().getSignalsEmitted();
            final var emittedSignalId = emittedSignals.iterator().next().getId();
            final Set<UUID> signals = Set.of(signalId, emittedSignalId);

            final var furtherSignals = removeSignals(history, signals);

            assertAll("Removed from", () -> assertThat("receivedSignals", history.getReceivedSignals(), empty()),
                    () -> assertThat("incomingSignals", history.getIncomingSignals(), empty()),
                    () -> assertThat("events", history.getEvents(), empty()));
            assertThat("further signals to remove", furtherSignals, empty());
        }

        @RepeatedTest(4)
        public void multipleThreads() {
            final int nThreads = 16;
            final int nEventsPerThread = 64;

            final UUID receiver = OBJECT_A;
            final UUID sender = OBJECT_B;
            final Duration end = WHEN_A;
            final Integer state0 = Integer.valueOf(0);
            final Duration start = end;
            final ObjectHistory<Integer> history = new ObjectHistory<Integer>(receiver, start, state0);

            final CountDownLatch ready = new CountDownLatch(1);
            final List<Future<Void>> futures = new ArrayList<>(nThreads);

            for (int t = 0; t < nThreads; ++t) {
                futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> {
                    final var random = new Random();
                    for (int e = 0; e < nEventsPerThread; ++e) {
                        final UUID signalId = UUID.randomUUID();
                        final var events0 = history.getEvents();
                        final var hasPrevious = events0.isEmpty();
                        final Event<Integer> expectedPreviousEvent = hasPrevious ? null : events0.last();
                        final Duration whenPrevious = expectedPreviousEvent == null ? end
                                : expectedPreviousEvent.getWhenOccurred();
                        final Duration whenOccurred = whenPrevious.plusMillis(1 + random.nextInt(10_000));
                        final Duration whenSent = whenOccurred.minusSeconds(1);
                        final Integer state = Integer.valueOf(random.nextInt());
                        final Signal<Integer> signal = new SignalTest.TestSignal(signalId,
                                new TimestampedId(sender, whenSent), receiver);
                        final Event<Integer> event = new Event<Integer>(new TimestampedId(signalId, whenOccurred),
                                receiver, state, Set.of());
                        history.addIncomingSignal(signal);
                        history.compareAndAddEvent(expectedPreviousEvent, event, signal);
                        Thread.yield();
                        history.removeSignals(Set.of(signalId));
                    } // for
                }));
            } // for

            ready.countDown();
            ThreadSafetyTest.get(futures);
            assertInvariants(history);
        }
    }// class

    static final UUID OBJECT_A = UUID.randomUUID();
    static final UUID OBJECT_B = UUID.randomUUID();
    static final UUID OBJECT_C = UUID.randomUUID();

    private static final Duration FLUX_TIMEOUT = Duration.ofMillis(20);

    static final Duration WHEN_A = Duration.ofMillis(0);
    static final Duration WHEN_B = Duration.ofMillis(5000);
    static final Duration WHEN_C = Duration.ofMillis(7000);
    static final Duration WHEN_D = Duration.ofMillis(13000);
    static final UUID SIGNAL_ID_A = UUID.randomUUID();
    static final UUID SIGNAL_ID_B = UUID.randomUUID();
    static final TimestampedId LAST_SIGNAL_APPLIED_A = new TimestampedId(SIGNAL_ID_A, WHEN_A);
    static final TimestampedId LAST_SIGNAL_APPLIED_B = new TimestampedId(SIGNAL_ID_B, WHEN_B);

    private static <STATE> void addIncomingSignal(@Nonnull final ObjectHistory<STATE> history,
            @Nonnull final Signal<STATE> signal) {
        history.addIncomingSignal(signal);

        assertInvariants(history);
        assertThat("incomingSignals", history.getIncomingSignals(), hasItem(signal));
    }

    private static <STATE> void advanceEnd(@Nonnull final ObjectHistory<STATE> history, @Nonnull final Duration end) {
        history.advanceEnd(end);

        assertInvariants(history);
        assertThat("end", history.getEnd(), greaterThanOrEqualTo(end));
    }

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history) {
        ObjectTest.assertInvariants(history);// inherited

        final var events = history.getEvents();
        final var end = history.getEnd();
        final var incomingSignals = history.getIncomingSignals();
        final var lastEvent = history.getLastEvent();
        final var object = history.getObject();
        final var receivedSignals = history.getReceivedSignals();
        final var receivedAndIncomingSignals = history.getReceivedAndIncomingSignals();
        final var start = history.getStart();
        final var stateHistory = history.getStateHistory();
        final var stateTransitions = history.getStateTransitions();

        assertAll("Not null", () -> assertNotNull(events, "events"), // guard
                () -> assertNotNull(object, "object"), () -> assertNotNull(start, "start"), // guard
                () -> assertNotNull(end, "end"), // guard
                () -> assertNotNull(incomingSignals, "incomingSignals"), // guard
                () -> assertNotNull(receivedSignals, "signalsReceived"), // guard
                () -> assertNotNull(receivedAndIncomingSignals, "receivedAndIncomingSignals"), // guard
                () -> assertNotNull(stateHistory, "stateHistory"), // guard
                () -> assertNotNull(stateTransitions, "stateTransitions") // guard
        );
        ValueHistoryTest.assertInvariants(stateHistory);

        assertAll(() -> assertAll("events", createEventsAssertions(events, object, start, stateHistory)),
                () -> assertAll("incomingSignals", createIncomingSignalsAssertions(incomingSignals, object)),
                () -> assertThat("The end time is at or after the start time.", end, greaterThanOrEqualTo(start)),
                () -> assertThat("incomingSignals and receivedSignals are distinct",
                        Collections.disjoint(incomingSignals, receivedSignals)),
                () -> assertAll("lastEvent",
                        () -> assertThat("is null if, and only if, the sequence of events is empty.",
                                lastEvent == null == events.isEmpty()),
                        () -> assertThat("is either null or is the  last of the sequence of events.",
                                lastEvent == null || lastEvent == events.last())),
                () -> assertAll("receivedAndIncomingSignals ",
                        () -> assertThat("include all of incomingSignals",
                                receivedAndIncomingSignals.containsAll(incomingSignals)),
                        () -> assertThat("include all of receivedSignals",
                                receivedAndIncomingSignals.containsAll(receivedSignals)),
                        () -> assertThat("include only incomingSignals and receivedSignals", receivedAndIncomingSignals,
                                hasSize(incomingSignals.size() + receivedSignals.size()))),
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

        assertAll("Value semantics",
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "stateTransitions",
                        h -> h.getStateTransitions()),
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "object", h -> h.getObject()),
                () -> EqualsSemanticsTest.assertValueSemantics(history1, history2, "end", h -> h.getEnd()),
                () -> assertTrue(history1
                        .equals(history2) == (history1.getStateTransitions().equals(history2.getStateTransitions())
                                && history1.getObject().equals(history2.getObject())
                                && history1.getEnd().equals(history2.getEnd())),
                        "equals"));
    }

    private static <STATE> void commitTo(@Nonnull final ObjectHistory<STATE> history, @Nonnull final Duration when) {
        final var end0 = history.getEnd();

        history.commitTo(when);

        assertInvariants(history);
        final var end = history.getEnd();
        assertAll("end", () -> assertThat("does not decrease", end, greaterThanOrEqualTo(end0)),
                () -> assertThat("at least the given value", end, greaterThanOrEqualTo(when)));
    }

    @Nullable
    private static <STATE> SortedSet<Event<STATE>> compareAndAddEvent(@Nonnull final ObjectHistory<STATE> history,
            @Nullable final Event<STATE> expectedPreviousEvent, @Nonnull final Event<STATE> event,
            @Nonnull final Signal<STATE> signal) {
        final SortedSet<Event<STATE>> events0 = history.getEvents();

        final var result = history.compareAndAddEvent(expectedPreviousEvent, event, signal);

        assertInvariants(history);
        final SortedSet<Event<STATE>> events = history.getEvents();
        if (result == null) {
            assertThat("events unchanged", events, is(events0));
        } else {
            assertThat("events has event", events, hasItem(event));
            assertThat("events has no events after event", events.tailSet(event), iterableWithSize(1));
            assertAll("Invalidated events",
                    createAssertPostconditionsOfInvalidatedEvents(result, event, history.getObject(), events0));
        }

        return result;
    }

    @Nullable
    private static <STATE> Continuation<STATE> computeContinuation(@Nonnull final ObjectHistory<STATE> history) {
        final var incomingSignals = history.getIncomingSignals();

        final Continuation<STATE> continuation = history.computeContinuation();

        assertInvariants(history);
        assertThat("The continuation is null if, and only if, there are no more incoming signals.",
                continuation == null == incomingSignals.isEmpty());
        if (continuation != null) {
            ContinuationTest.assertInvariants(continuation);
            assertThat("nextSignal", continuation.nextSignal, in(incomingSignals));
            assertThat("state", continuation.state,
                    is(history.getStateHistory().get(continuation.whenNextSignalReceived)));
        }
        return continuation;
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
                () -> assertEquals(that.getIncomingSignals(), copy.getIncomingSignals(), "incomingSignals"),
                () -> assertEquals(that.getReceivedSignals(), copy.getReceivedSignals(), "receivedSignals"),
                () -> assertEquals(that.getEvents(), copy.getEvents(), "events"));

        return copy;
    }

    private static <STATE> ObjectHistory<STATE> constructor(@Nonnull final UUID object, @Nonnull final Duration start,
            @Nonnull final STATE state) {
        final var history = new ObjectHistory<>(object, start, state);

        assertInvariants(history);
        final var stateTransitions = history.getStateTransitions();
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(start, history.getStart(), "start"), () -> assertSame(start, history.getEnd(), "end"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, Map.of(start, state), "stateTransitions"),
                () -> assertThat("events", history.getEvents(), empty()));

        return history;
    }

    static <STATE> long count(final Collection<Signal<STATE>> collection, final Signal<STATE> signal) {
        return collection.stream().filter(s -> signal.equals(s)).count();
    }

    @Nonnull
    private static <STATE> Stream<Executable> createAssertPostconditionsOfInvalidatedEvents(
            @Nonnull final SortedSet<Event<STATE>> invalidatedEvents, @Nonnull final Event<STATE> eventAdded,
            @Nonnull final UUID object, final SortedSet<Event<STATE>> events0) {
        return invalidatedEvents.stream().map(event -> new Executable() {

            @Override
            public void execute() throws AssertionError {
                EventTest.assertInvariants(event);
                assertAll(() -> assertThat("event.affectedObject", event.getAffectedObject(), is(object)),
                        () -> assertThat("event after eventAdded", event, greaterThan(eventAdded)),
                        () -> assertThat("event is in events0", event, is(in(events0))));
            }
        });
    }

    private static <STATE> Stream<Executable> createEventsAssertions(@Nonnull final SortedSet<Event<STATE>> events,
            @Nonnull final UUID object, @Nonnull final Duration start,
            @Nonnull final ValueHistory<STATE> stateHistory) {
        return events.stream().map(event -> new Executable() {

            @Override
            public void execute() throws AssertionError {
                final Duration whenOccurred = event.getWhenOccurred();
                EventTest.assertInvariants(event);
                assertAll("event [" + event + "]",
                        () -> assertThat("event.affectedObject", event.getAffectedObject(), is(object)),
                        () -> assertThat("event.whenOccurred", whenOccurred, greaterThan(start)));
            }
        });
    }

    private static <STATE> Stream<Executable> createIncomingSignalsAssertions(
            @Nonnull final Set<Signal<STATE>> incomingSignals, @Nonnull final UUID object) {
        return incomingSignals.stream().map(signal -> new Executable() {

            @Override
            public void execute() throws AssertionError {
                assertThat(signal, notNullValue());// guard
                SignalTest.assertInvariants(signal);
                assertThat("signal.receiver", signal.getReceiver(), is(object));
            }
        });
    }

    @Nonnull
    private static <T> Set<T> difference(@Nonnull final Set<T> x, @Nonnull final Set<T> y) {
        return x.stream().filter(e -> !y.contains(e)).collect(toUnmodifiableSet());
    }

    public static <STATE> Publisher<Optional<STATE>> observeState(@Nonnull final ObjectHistory<STATE> history,
            @Nonnull final Duration when) {
        final var states = history.observeState(when);

        assertInvariants(history);
        assertNotNull(states, "Not null, states");// guard

        return states;
    }

    @Nonnull
    private static <STATE> boolean receiveNextSignal(@Nonnull final ObjectHistory<STATE> history,
            @Nonnull final Medium<STATE> medium) {
        final boolean didWork = history.receiveNextSignal(medium);

        assertInvariants(history);
        MediumTest.assertInvariants(medium);
        return didWork;
    }

    @Nonnull
    private static <STATE> Set<Signal<STATE>> removeSignals(@Nonnull final ObjectHistory<STATE> history,
            @Nonnull final Set<UUID> signals) {
        final var removedEmittedSignals = history.removeSignals(signals);

        assertInvariants(history);
        final var receivedSignals = history.getReceivedSignals();
        final var incomingSignals = history.getIncomingSignals();
        final Set<UUID> receivedSignalIds = receivedSignals.stream().map(signal -> signal.getId())
                .collect(toUnmodifiableSet());
        final Set<UUID> incomingSignalIds = incomingSignals.stream().map(signal -> signal.getId())
                .collect(toUnmodifiableSet());
        assertThat("removedEvents", removedEmittedSignals, notNullValue());// guard
        assertAll(
                () -> assertThat(
                        "The set of signals received does not include signals with any of the given signal IDs.",
                        Collections.disjoint(receivedSignalIds, signals)),
                () -> assertThat(
                        "The set of incoming signals does not include signals with any of the given signal IDs.",
                        Collections.disjoint(incomingSignalIds, signals)));

        return removedEmittedSignals;
    }

}// class
