package uk.badamson.mc.simulation.rx;
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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.ThreadTest;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.history.ValueHistoryTest;
import uk.badamson.mc.simulation.ObjectStateId;
import uk.badamson.mc.simulation.rx.EventTest.TestEvent;
import uk.badamson.mc.simulation.rx.ObjectHistory.TimestampedState;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ObjectHistoryTest {

    @Nested
    public class Append {

        @Nested
        public class One {
            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0), WHEN_A.plusMillis(10), Integer.valueOf(1));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(3), WHEN_B.plusMillis(1500), Integer.valueOf(2));
            }

            @Test
            public void destruction() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0), WHEN_A.plusMillis(10), null);
            }

            @Test
            public void equalStates() {
                final var state0 = Integer.valueOf(0);
                final var state1 = state0;// critical
                test(OBJECT_A, WHEN_A, state0, WHEN_A.plusMillis(10), state1);
            }

            private void test(final UUID object, final Duration when0, final Integer state0, final Duration when1,
                    final Integer state1) {
                final var event0 = new TestEvent(new ObjectStateId(object, when0), state0, Map.of());
                final var event1 = new TestEvent(new ObjectStateId(object, when1), state1, Map.of());
                final var history = new ObjectHistory<>(event0);

                Append.this.test(history, event1);
            }

        }// class

        @RepeatedTest(32)
        public void multiThreaded() {
            final var event0 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A), Integer.valueOf(0), Map.of());
            final var event1 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A.plusMillis(1)), Integer.valueOf(1),
                    Map.of());
            final var event2 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A.plusMillis(2)), Integer.valueOf(2),
                    Map.of());
            final var history = new ObjectHistory<>(event0);

            final CountDownLatch ready = new CountDownLatch(1);
            final var future1 = testInOtherThread(history, event1, ready);
            final var future2 = testInOtherThread(history, event2, ready);
            ready.countDown();
            ThreadTest.get(future1);
            ThreadTest.get(future2);

            assertInvariants(history);
            assertSame(event2, history.getLastEvent(), "lastEvent");
        }

        private <STATE> void test(@Nonnull final ObjectHistory<STATE> history, @Nonnull final Event<STATE> event) {
            final var object0 = history.getObject();
            final var start0 = history.getStart();

            history.append(event);

            assertInvariants(history);
            assertAll("Does not change constants", () -> assertSame(object0, history.getObject(), "object"),
                    () -> assertSame(start0, history.getStart(), "start"));
            assertSame(event, history.getLastEvent(), "lastEvent");
        }

        private Future<Void> testInOtherThread(final ObjectHistory<Integer> history, final TestEvent event,
                final CountDownLatch ready) {
            return ThreadTest.runInOtherThread(ready, () -> {
                final var object0 = history.getObject();
                final var start0 = history.getStart();

                try {
                    history.append(event);
                } catch (final IllegalStateException e) {
                    // Can happen because of the data race
                }

                assertAll("Does not change constants", () -> assertSame(object0, history.getObject(), "object"),
                        () -> assertSame(start0, history.getStart(), "start"));
            });
        }

        @Test
        public void two() {
            final var event0 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A), Integer.valueOf(0), Map.of());
            final var event1 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A.plusMillis(1)), Integer.valueOf(1),
                    Map.of());
            final var event2 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A.plusMillis(2)), Integer.valueOf(2),
                    Map.of());
            final var history = new ObjectHistory<>(event0);
            history.append(event1);

            test(history, event2);
        }

    }// class

    @Nested
    public class CompareAndAppend {

        @Nested
        public class AsExpected {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0), WHEN_A.plusMillis(1), Integer.valueOf(1));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(3), WHEN_B.plusMillis(1000), Integer.valueOf(2));
            }

            private <STATE> void test(@Nonnull final Event<STATE> expectedLastEvent,
                    @Nonnull final Event<STATE> event) {
                final ObjectHistory<STATE> history = new ObjectHistory<>(expectedLastEvent);

                final boolean success = CompareAndAppend.this.test(history, expectedLastEvent, event);

                assertTrue(success, "success");
            }

            private void test(final UUID object, final Duration time1, final Integer state1, final Duration time2,
                    final Integer state2) {
                final var event1 = new TestEvent(new ObjectStateId(object, time1), state1, Map.of());
                final var event2 = new TestEvent(new ObjectStateId(object, time2), state2, Map.of());

                test(event1, event2);
            }

        }// class

        @Nested
        public class NotAsExpected {

            @Test
            public void earlier() {
                final var lastEventTime = WHEN_A;
                final var expectedLastEventTime = lastEventTime.minusMillis(1);
                assert lastEventTime.compareTo(expectedLastEventTime) > 0;

                test(OBJECT_A, lastEventTime, Integer.valueOf(0), expectedLastEventTime, Integer.valueOf(1),
                        lastEventTime.plusMillis(1), Integer.valueOf(2));
            }

            @Test
            public void equals() {
                final var lastEventTime = WHEN_B;
                final var expectedLastEventTime = lastEventTime;// tough test
                final var lastEventState = Integer.valueOf(0);
                final var expectedLastEventState = lastEventState;// tough test

                test(OBJECT_A, lastEventTime, lastEventState, expectedLastEventTime, expectedLastEventState,
                        lastEventTime.plusMillis(1), Integer.valueOf(2));
            }

            @Test
            public void later() {
                final var lastEventTime = WHEN_B;
                final var expectedLastEventTime = lastEventTime.plusMillis(1);
                assert lastEventTime.compareTo(expectedLastEventTime) < 0;

                test(OBJECT_A, lastEventTime, Integer.valueOf(0), expectedLastEventTime, Integer.valueOf(1),
                        expectedLastEventTime.plusMillis(1), Integer.valueOf(2));
            }

            @Test
            public void sameTime() {
                final var lastEventTime = WHEN_B;
                final var expectedLastEventTime = lastEventTime;// tough test

                test(OBJECT_A, lastEventTime, Integer.valueOf(0), expectedLastEventTime, Integer.valueOf(1),
                        lastEventTime.plusMillis(1), Integer.valueOf(2));
            }

            private <STATE> void test(@Nonnull final Event<STATE> lastEvent,
                    @Nonnull final Event<STATE> expectedLastEvent, @Nonnull final Event<STATE> event) {
                assert lastEvent != expectedLastEvent;
                final ObjectHistory<STATE> history = new ObjectHistory<>(lastEvent);

                final boolean success = CompareAndAppend.this.test(history, expectedLastEvent, event);

                assertFalse(success, "failure");
            }

            private void test(final UUID object, final Duration lastEventTime, final Integer lastEventState,
                    final Duration expectedLastEventTime, final Integer expectedLastEventState,
                    final Duration eventTime, final Integer eventState) {
                final var lastEvent = new TestEvent(new ObjectStateId(object, lastEventTime), lastEventState, Map.of());
                final var expectedLastEvent = new TestEvent(new ObjectStateId(object, expectedLastEventTime),
                        expectedLastEventState, Map.of());
                final var event = new TestEvent(new ObjectStateId(object, eventTime), eventState, Map.of());

                test(lastEvent, expectedLastEvent, event);
            }

        }// class

        private <STATE> boolean test(@Nonnull final ObjectHistory<STATE> history,
                @Nonnull final Event<STATE> expectedLastEvent, @Nonnull final Event<STATE> event) {
            final var object0 = history.getObject();
            final var start0 = history.getStart();
            final var lastEvent0 = history.getLastEvent();

            final boolean success = history.compareAndAppend(expectedLastEvent, event);

            assertInvariants(history);
            assertAll("Does not change constants", () -> assertSame(object0, history.getObject(), "object"),
                    () -> assertSame(start0, history.getStart(), "start"));
            assertAll(() -> assertFalse(success && history.getLastEvent() != event,
                    "If the method returns true, it has the same effect as if append(Event) had been called with event."),
                    () -> assertFalse(!success && history.getLastEvent() != lastEvent0,
                            "If the method returns false, it has no effect."));
            return success;
        }

    }// class

    @Nested
    public class Constructor {

        @Nested
        public class Copy {

            @Nested
            public class OneTransition {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, Integer.valueOf(0), Map.of());
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, Integer.valueOf(1), Map.of(OBJECT_A, WHEN_B.minusMillis(10)));
                }

                private <STATE> void test(@Nonnull final Event<STATE> event) {
                    final var history = new ObjectHistory<>(event);

                    Copy.this.test(history);
                }

                private void test(final UUID object, final Duration start, final Integer state,
                        final Map<UUID, Duration> nextEventDependencies) {
                    final var event = new TestEvent(new ObjectStateId(object, start), state, nextEventDependencies);

                    test(event);
                }

            }// class

            private <STATE> void test(@Nonnull final ObjectHistory<STATE> that) {
                final var copy = new ObjectHistory<>(that);

                assertInvariants(copy);
                assertInvariants(copy, that);
                assertAll("Copied", () -> assertSame(that.getEnd(), copy.getEnd(), "end"),
                        () -> assertSame(that.getLastEvent(), copy.getLastEvent(), "lastEvent"),
                        () -> assertSame(that.getObject(), copy.getObject(), "object"),
                        () -> assertSame(that.getStart(), copy.getStart(), "start"),
                        () -> assertEquals(that.getStateHistory(), copy.getStateHistory(), "stateHistory"));
            }
        }// class

        @Nested
        public class History {

            @Nested
            public class NoPreviousStateTransitions {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, Integer.valueOf(0));
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, Integer.valueOf(1));
                }

                private void test(final UUID object, final Duration start, final Integer state) {
                    final SortedMap<Duration, Integer> previousStateTransitions = Collections.emptySortedMap();
                    final var event = new TestEvent(new ObjectStateId(object, start), state, Map.of());

                    History.this.test(previousStateTransitions, event);
                }

            }// class

            @Nested
            public class OnePreviousStateTransition {

                @Test
                public void destruction() {
                    test(OBJECT_A, WHEN_A, Integer.valueOf(0), WHEN_B, null);
                }

                @Test
                public void far() {
                    test(OBJECT_A, WHEN_A, Integer.valueOf(0), WHEN_B, Integer.valueOf(1));
                }

                @Test
                public void near() {
                    final var start = WHEN_B;
                    final var end = start.plusNanos(1);
                    test(OBJECT_A, start, Integer.valueOf(3), end, Integer.valueOf(2));
                }

                private void test(final UUID object, final Duration start, final Integer state1, final Duration end,
                        final Integer state2) {
                    final SortedMap<Duration, Integer> previousStateTransitions = new TreeMap<>();
                    previousStateTransitions.put(start, state1);
                    final var event = new TestEvent(new ObjectStateId(object, end), state2, Map.of());

                    History.this.test(previousStateTransitions, event);
                }

            }// class

            private <STATE> void test(@Nonnull final SortedMap<Duration, STATE> previousStateTransitions,
                    @Nonnull final Event<STATE> lastEvent) {
                final var history = new ObjectHistory<>(previousStateTransitions, lastEvent);

                assertInvariants(history);
                assertAll(() -> assertSame(lastEvent, history.getLastEvent(), "lastEvent"),
                        () -> assertEquals(previousStateTransitions, history.getPreviousStateTransitions(),
                                "previousStateTransitions"));
            }

            @Test
            public void twoPreviosStateTransitions() {
                final SortedMap<Duration, Integer> previousStateTransitions = new TreeMap<>();
                previousStateTransitions.put(WHEN_A, Integer.valueOf(0));
                previousStateTransitions.put(WHEN_B, Integer.valueOf(1));
                final var event = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_C), Integer.valueOf(3), Map.of());

                History.this.test(previousStateTransitions, event);
            }

        }// class

        @Nested
        public class LastEvent {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0), Map.of());
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(1), Map.of(OBJECT_A, WHEN_B.minusMillis(10)));
            }

            private <STATE> void test(@Nonnull final Event<STATE> event) {
                final var history = new ObjectHistory<>(event);

                assertInvariants(history);
                assertSame(event, history.getLastEvent(), "lastEvent");
            }

            private void test(final UUID object, final Duration start, final Integer state,
                    final Map<UUID, Duration> nextEventDependencies) {
                final var event = new TestEvent(new ObjectStateId(object, start), state, nextEventDependencies);
                test(event);
            }

        }// class

    }// class

    @Nested
    public class ObserveEvents {

        @Nested
        public class AfterAppend {

            @Test
            public void a() {
                testNonDestruction(OBJECT_A, WHEN_A, Integer.valueOf(0), WHEN_A.plusMillis(10), Integer.valueOf(1));
            }

            @Test
            public void b() {
                testNonDestruction(OBJECT_B, WHEN_B, Integer.valueOf(3), WHEN_B.plusMillis(1500), Integer.valueOf(2));
            }

            @Test
            public void destruction() {
                final UUID object = OBJECT_A;
                final var event0 = new TestEvent(new ObjectStateId(object, WHEN_A), Integer.valueOf(0), Map.of());
                final var event1 = new TestEvent(new ObjectStateId(object, WHEN_A.plusMillis(10)), null, Map.of());
                final var history = new ObjectHistory<>(event0);
                history.append(event1);

                final var flux = ObserveEvents.this.test(history);

                StepVerifier.create(flux).expectNext(event1).expectComplete().verify(Duration.ofMillis(100));
            }

            private void testNonDestruction(final UUID object, final Duration when0, final Integer state0,
                    final Duration when1, final Integer state1) {
                final var event0 = new TestEvent(new ObjectStateId(object, when0), state0, Map.of());
                final var event1 = new TestEvent(new ObjectStateId(object, when1), state1, Map.of());
                final var history = new ObjectHistory<>(event0);
                history.append(event1);

                final var flux = ObserveEvents.this.test(history);

                StepVerifier.create(flux).expectNext(event1).expectTimeout(Duration.ofMillis(100)).verify();
            }

        }// class

        @Nested
        public class AfterConstructor {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(1));
            }

            private void test(final UUID object, final Duration start, final Integer state) {
                final var event = new TestEvent(new ObjectStateId(object, start), state, Map.of());
                final var history = new ObjectHistory<>(event);

                final var flux = ObserveEvents.this.test(history);

                StepVerifier.create(flux).expectNext(event).expectTimeout(Duration.ofMillis(100)).verify();
            }

        }// class

        private <STATE> Flux<Event<STATE>> test(@Nonnull final ObjectHistory<STATE> history) {
            final var flux = history.observeEvents();

            assertInvariants(history);
            assertNotNull(flux, "Not null, result");
            return flux;
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

            private void test(@Nonnull final Duration start, @Nonnull final Integer state) {
                final var event = new TestEvent(new ObjectStateId(OBJECT_A, start), state, Map.of());
                test(event);
            }

            private <STATE> void test(@Nonnull final Event<STATE> event) {
                final Optional<STATE> expectedState = Optional.of(event.getState());
                final var history = new ObjectHistory<>(event);
                final Duration when = history.getStart();

                final var states = ObserveState.this.test(history, when);

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
                final var event = new TestEvent(new ObjectStateId(OBJECT_A, start), Integer.valueOf(0), Map.of());

                test(event, when);
            }

            private <STATE> void test(@Nonnull final Event<STATE> event, @Nonnull final Duration when) {
                final Optional<STATE> expectedState = Optional.empty();
                final var history = new ObjectHistory<>(event);
                assert when.compareTo(history.getStart()) < 0;

                final var states = ObserveState.this.test(history, when);

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
                final Duration when = time0.plusNanos(1);// tough test

                test(time0, when, Integer.valueOf(0));
            }

            private void test(@Nonnull final Duration time0, @Nonnull final Duration when,
                    @Nonnull final Integer state0) {
                assert time0.compareTo(when) < 0;// provisional
                final var expectedState = Optional.of(state0);
                final var event0 = new TestEvent(new ObjectStateId(OBJECT_A, time0), state0, Map.of());
                final var history = new ObjectHistory<>(event0);

                final var states = ObserveState.this.test(history, when);

                StepVerifier.create(states).expectNext(expectedState).expectTimeout(Duration.ofMillis(100)).verify();
            }

        }// class

        @Nested
        public class ProvisionalConfirmedAsReliable {

            @Test
            public void far() {
                final Duration time0 = WHEN_B;
                final Duration when = time0.plusDays(365);
                final Duration time1 = when.plusDays(365);

                test(time0, when, time1, Integer.valueOf(3), Integer.valueOf(2));
            }

            @Test
            public void near() {
                final Duration time0 = WHEN_A;
                final Duration when = time0.plusNanos(1);// tough test
                final Duration time1 = when.plusNanos(1);// tough test

                test(time0, when, time1, Integer.valueOf(0), Integer.valueOf(1));
            }

            private void test(@Nonnull final Duration time0, @Nonnull final Duration when,
                    @Nonnull final Duration time1, @Nonnull final Integer state0, @Nonnull final Integer state1) {
                assert time0.compareTo(when) < 0;// provisional
                assert when.compareTo(time1) <= 0;// becomes reliable
                final var expectedState = Optional.of(state0);
                final var event0 = new TestEvent(new ObjectStateId(OBJECT_A, time0), state0, Map.of());
                final var event1 = new TestEvent(new ObjectStateId(OBJECT_A, time1), state1, Map.of());
                final var history = new ObjectHistory<>(event0);

                final var states = ObserveState.this.test(history, when);

                StepVerifier.create(states).expectNext(expectedState).then(() -> history.append(event1))
                        .expectComplete().verify();
            }

        }// class

        @Nested
        public class ReliableBetweenEvents {

            @Test
            public void atTransition() {
                final Duration time0 = WHEN_A;
                final Duration time1 = time0.plusSeconds(1);
                final Duration when = time1;// critical
                final Duration time2 = when.plusSeconds(1);

                test(time0, time1, when, time2, Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2));
            }

            @Test
            public void betweenTransitions() {
                final Duration time0 = WHEN_A;
                final Duration time1 = time0.plusSeconds(1);
                final Duration when = time1.plusSeconds(1);
                final Duration time2 = when.plusSeconds(1);

                test(time0, time1, when, time2, Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2));
            }

            @Test
            public void justBeforeTransition() {
                final Duration time0 = WHEN_A;
                final Duration time1 = time0.plusSeconds(1);
                final Duration time2 = time1.plusSeconds(1);
                final Duration when = time2.minusNanos(1);// critical

                test(time0, time1, when, time2, Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2));
            }

            private void test(@Nonnull final Duration time0, @Nonnull final Duration time1,
                    @Nonnull final Duration when, @Nonnull final Duration time2, @Nonnull final Integer state0,
                    @Nonnull final Integer state1, @Nullable final Integer state2) {
                assert time0.compareTo(time1) < 0;
                assert time1.compareTo(when) <= 0;// between events
                assert when.compareTo(time2) < 0;
                final var expectedState = Optional.of(state1);
                final var event0 = new TestEvent(new ObjectStateId(OBJECT_A, time0), state0, Map.of());
                final var event1 = new TestEvent(new ObjectStateId(OBJECT_A, time1), state1, Map.of());
                final var event2 = new TestEvent(new ObjectStateId(OBJECT_A, time2), state2, Map.of());
                final var history = new ObjectHistory<>(event0);
                history.append(event1);
                history.append(event2);

                final var states = ObserveState.this.test(history, when);

                StepVerifier.create(states).expectNext(expectedState).expectComplete().verify();
            }
        }// class

        @Nested
        public class UpdatedProvisionalConfirmedAsReliable {

            @Test
            public void duplicateStates() {
                final Duration time0 = WHEN_A;
                final Duration time1 = time0.plusDays(365);
                final Duration when = time1.plusDays(365);
                final Duration time2 = when.plusDays(365);
                final var state0 = Integer.valueOf(3);
                final var state1 = state0;// critical
                final var state2 = Integer.valueOf(1);

                final var expectedState0 = Optional.of(state0);
                final var event0 = new TestEvent(new ObjectStateId(OBJECT_A, time0), state0, Map.of());
                final var event1 = new TestEvent(new ObjectStateId(OBJECT_A, time1), state1, Map.of());
                final var event2 = new TestEvent(new ObjectStateId(OBJECT_A, time2), state2, Map.of());
                final var history = new ObjectHistory<>(event0);

                final var states = ObserveState.this.test(history, when);

                StepVerifier.create(states).expectNext(expectedState0).then(() -> history.append(event1))
                        .then(() -> history.append(event2)).expectComplete().verify();
            }

            @Test
            public void far() {
                final Duration time0 = WHEN_B;
                final Duration time1 = time0.plusDays(365);
                final Duration when = time1.plusDays(365);
                final Duration time2 = when.plusDays(365);

                testDistinct(time0, time1, when, time2, Integer.valueOf(3), Integer.valueOf(2), Integer.valueOf(1));
            }

            @Test
            public void near() {
                final Duration time0 = WHEN_A;
                final Duration time1 = time0.plusNanos(1);// tough test
                final Duration when = time1;// tough test
                final Duration time2 = when.plusNanos(1);// tough test

                testDistinct(time0, time1, when, time2, Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2));
            }

            private void testDistinct(@Nonnull final Duration time0, @Nonnull final Duration time1,
                    @Nonnull final Duration when, @Nonnull final Duration time2, @Nonnull final Integer state0,
                    @Nonnull final Integer state1, @Nullable final Integer state2) {
                assert time0.compareTo(time1) < 0;
                assert time1.compareTo(time2) < 0;
                assert time0.compareTo(when) < 0;// provisional
                assert time1.compareTo(when) <= 0;// updated provisional
                assert when.compareTo(time2) < 0;// becomes reliable
                final var expectedState0 = Optional.of(state0);
                final var expectedState1 = Optional.of(state1);
                final var event0 = new TestEvent(new ObjectStateId(OBJECT_A, time0), state0, Map.of());
                final var event1 = new TestEvent(new ObjectStateId(OBJECT_A, time1), state1, Map.of());
                final var event2 = new TestEvent(new ObjectStateId(OBJECT_A, time2), state2, Map.of());
                final var history = new ObjectHistory<>(event0);

                final var states = ObserveState.this.test(history, when);

                StepVerifier.create(states).expectNext(expectedState0).then(() -> history.append(event1))
                        .expectNext(expectedState1).then(() -> history.append(event2)).expectComplete().verify();
            }

        }// class

        @Nested
        public class UpdateIsReliable {

            @Test
            public void far() {
                final Duration time0 = WHEN_B;
                final Duration time1 = time0.plusDays(365);

                test(time0, time1, Integer.valueOf(3), Integer.valueOf(2));
            }

            @Test
            public void near() {
                final Duration time0 = WHEN_A;
                final Duration time1 = time0.plusNanos(1);// tough test

                test(time0, time1, Integer.valueOf(0), Integer.valueOf(1));
            }

            private void test(@Nonnull final Duration time0, @Nonnull final Duration time1,
                    @Nonnull final Integer state0, @Nonnull final Integer state1) {
                assert time0.compareTo(time1) < 0;
                final var when = time1;// the update is reliable
                final var expectedState0 = Optional.of(state0);
                final var expectedState1 = Optional.of(state1);
                final var event0 = new TestEvent(new ObjectStateId(OBJECT_A, time0), state0, Map.of());
                final var event1 = new TestEvent(new ObjectStateId(OBJECT_A, time1), state1, Map.of());
                final var history = new ObjectHistory<>(event0);

                final var states = ObserveState.this.test(history, when);

                StepVerifier.create(states).expectNext(expectedState0).then(() -> history.append(event1))
                        .expectNext(expectedState1).expectComplete().verify(Duration.ofMillis(100));
            }

        }// class

        private <STATE> Publisher<Optional<STATE>> test(@Nonnull final ObjectHistory<STATE> history,
                @Nonnull final Duration when) {
            final var states = history.observeState(when);

            assertInvariants(history);
            assertNotNull(states, "Not null, states");// guard

            return states;
        }

    }// class

    @Nested
    public class ObserveStateTransitions {

        @Nested
        public class AfterAppend {

            @Test
            public void a() {
                testNonDestruction(OBJECT_A, WHEN_A, Integer.valueOf(0), WHEN_A.plusMillis(10), Integer.valueOf(1));
            }

            @Test
            public void b() {
                testNonDestruction(OBJECT_B, WHEN_B, Integer.valueOf(3), WHEN_B.plusMillis(1500), Integer.valueOf(2));
            }

            @Test
            public void destruction() {
                final var object = OBJECT_A;
                final var when1 = WHEN_A;
                final Integer state1 = null;// critical
                final var expectedStateTransition = new ObjectHistory.TimestampedState<>(when1, state1);
                final var event0 = new TestEvent(new ObjectStateId(object, when1.minusMillis(10)), Integer.valueOf(0),
                        Map.of());
                final var event1 = new TestEvent(new ObjectStateId(object, when1), state1, Map.of());
                final var history = new ObjectHistory<>(event0);
                history.append(event1);

                final var flux = ObserveStateTransitions.this.test(history);

                StepVerifier.create(flux).expectNext(expectedStateTransition).expectComplete()
                        .verify(Duration.ofMillis(100));
            }

            private void testNonDestruction(final UUID object, final Duration when0, final Integer state0,
                    final Duration when1, final Integer state1) {
                final var expectedStateTransition = new ObjectHistory.TimestampedState<>(when1, state1);
                final var event0 = new TestEvent(new ObjectStateId(object, when0), state0, Map.of());
                final var event1 = new TestEvent(new ObjectStateId(object, when1), state1, Map.of());
                final var history = new ObjectHistory<>(event0);
                history.append(event1);

                final var flux = ObserveStateTransitions.this.test(history);

                StepVerifier.create(flux).expectNext(expectedStateTransition).expectTimeout(Duration.ofMillis(100))
                        .verify();
            }

        }// class

        @Nested
        public class AfterConstructor {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(1));
            }

            private void test(final UUID object, final Duration start, final Integer state) {
                final var expectedStateTransition = new ObjectHistory.TimestampedState<>(start, state);
                final var event = new TestEvent(new ObjectStateId(object, start), state, Map.of());
                final var history = new ObjectHistory<>(event);

                final var flux = ObserveStateTransitions.this.test(history);

                StepVerifier.create(flux).expectNext(expectedStateTransition).expectTimeout(Duration.ofMillis(100))
                        .verify();
            }

        }// class

        private <STATE> Flux<TimestampedState<STATE>> test(@Nonnull final ObjectHistory<STATE> history) {
            final var flux = history.observeStateTransitions();

            assertInvariants(history);
            assertNotNull(flux, "Not null, result");
            return flux;
        }
    }// class

    public static class TimestampedStateTest {

        @Nested
        public class Constructor {

            @Nested
            public class Two {

                @Test
                public void differentState() {
                    final String stateA = "A";
                    final String stateB = "B";

                    final var timestampedA = new ObjectHistory.TimestampedState<>(WHEN_A, stateA);
                    final var timestampedB = new ObjectHistory.TimestampedState<>(WHEN_A, stateB);

                    assertInvariants(timestampedA, timestampedB);
                    assertNotEquals(timestampedA, timestampedB);
                }

                @Test
                public void differentWhen() {
                    final Duration whenA = Duration.ofMillis(1000);
                    final Duration whenB = Duration.ofMillis(2000);
                    final String state = "State";

                    final var timestampedA = new ObjectHistory.TimestampedState<>(whenA, state);
                    final var timestampedB = new ObjectHistory.TimestampedState<>(whenB, state);

                    assertInvariants(timestampedA, timestampedB);
                    assertNotEquals(timestampedA, timestampedB);
                }

                @Test
                public void equivalent() {
                    final Duration whenA = Duration.ofMillis(1000);
                    final Duration whenB = Duration.ofMillis(1000);
                    final String stateA = "State";
                    final String stateB = new String(stateA);
                    assert whenA.equals(whenB);
                    assert stateA.equals(stateB);
                    assert whenA != whenB;// tough test
                    assert stateA != stateB;// tough test

                    final var timestampedA = new ObjectHistory.TimestampedState<>(whenA, stateA);
                    final var timestampedB = new ObjectHistory.TimestampedState<>(whenB, stateB);

                    assertInvariants(timestampedA, timestampedB);
                    assertEquals(timestampedA, timestampedB);
                }
            }// class

            @Test
            public void a() {
                test(WHEN_A, "State");
            }

            @Test
            public void b() {
                test(WHEN_B, Integer.valueOf(0));
            }

            @Test
            public void nullState() {
                test(WHEN_A, (Integer) null);
            }

            private <STATE> void test(@Nonnull final Duration when, @Nullable final STATE state) {
                final var timestamped = new ObjectHistory.TimestampedState<>(when, state);

                assertInvariants(timestamped);
                assertAll(() -> assertSame(when, timestamped.getWhen(), "when"),
                        () -> assertSame(state, timestamped.getState(), "state"));
            }

        }// class

        public static <STATE> void assertInvariants(@Nonnull final ObjectHistory.TimestampedState<STATE> timestamped) {
            ObjectTest.assertInvariants(timestamped);// inherited

            assertNotNull(timestamped.getWhen(), "Not null, when");
        }

        public static <STATE> void assertInvariants(@Nonnull final ObjectHistory.TimestampedState<STATE> timestamped1,
                @Nonnull final ObjectHistory.TimestampedState<STATE> timestamped2) {
            ObjectTest.assertInvariants(timestamped1, timestamped2);// inherited

            final var state1 = timestamped1.getState();
            final var state2 = timestamped2.getState();
            if (state1 != null && state2 != null) {
                ObjectTest.assertInvariants(state1, state2);
            }
            assertTrue(timestamped1.equals(timestamped2) == (timestamped1.getWhen().equals(timestamped2.getWhen())
                    && Objects.equals(state1, state2)), "equals has value semantics");
        }
    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);
    private static final Duration WHEN_C = Duration.ofMillis(7000);

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history) {
        ObjectTest.assertInvariants(history);// inherited

        final var object = history.getObject();
        final var start = history.getStart();
        final var end = history.getEnd();
        final var lastEvent = history.getLastEvent();
        final var stateHistory = history.getStateHistory();
        final var previousStateTransitions = history.getPreviousStateTransitions();

        assertAll("Not null", () -> assertNotNull(object, "object"), () -> assertNotNull(start, "start"), // guard
                () -> assertNotNull(end, "end"), // guard
                () -> assertNotNull(lastEvent, "lastEvent"), // guard
                () -> assertNotNull(stateHistory, "stateHistory"), // guard
                () -> assertNotNull(previousStateTransitions, "previousStateTransitions")// guard
        );
        assertAll("Maintains invariants of attributes and aggregates", () -> EventTest.assertInvariants(lastEvent),
                () -> ValueHistoryTest.assertInvariants(stateHistory));
        assertAll(
                () -> assertAll("lastEvent",
                        () -> assertSame(object, lastEvent.getObject(),
                                "The object of the last event is the same has the object of this history."),
                        () -> assertThat("The time of the last event is at or after the start of this history.",
                                lastEvent.getWhen(), greaterThanOrEqualTo(start))),
                () -> assertAll("end",
                        () -> assertThat("The end time is at or after the start time.", end,
                                greaterThanOrEqualTo(start)),
                        () -> assertFalse(lastEvent.getState() == null && end != ValueHistory.END_OF_TIME,
                                "If the state transitioned to by the last event is null, the end time is the end of time."),
                        () -> assertFalse(lastEvent.getState() != null && end != lastEvent.getWhen(),
                                "If the state transitioned to by the last event is not null, the end time is the time of that event.")),
                () -> assertAll("stateHistory", () -> assertSame(start, stateHistory.getFirstTansitionTime(),
                        "The first transition time of the state history is the same as the start time of this history."),
                        () -> assertNull(stateHistory.getFirstValue(),
                                "The state at the start of time of the state history is null."),
                        () -> assertSame(lastEvent.getState(), stateHistory.getLastValue(),
                                "The state at the end of time of the state history is the same as the the last event of this history."),
                        () -> assertFalse(stateHistory.isEmpty(), "The state history is never empty.")),
                () -> assertEquals(previousStateTransitions,
                        stateHistory.getTransitions().headMap(stateHistory.getTransitions().lastKey()),
                        "previousStateTransitions"));
    }

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history1,
            @Nonnull final ObjectHistory<STATE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited
    }
}
