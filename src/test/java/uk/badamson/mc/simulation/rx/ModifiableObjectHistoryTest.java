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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
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
import uk.badamson.dbc.assertions.ThreadSafetyTest;
import uk.badamson.mc.JsonTest;
import uk.badamson.mc.simulation.TimestampedId;
import uk.badamson.mc.simulation.rx.EventTest.TestEvent;
import uk.badamson.mc.simulation.rx.ObjectHistory.TimestampedState;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ModifiableObjectHistoryTest {

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
                final var event0 = new TestEvent(new TimestampedId(object, when0), state0, Map.of());
                final var event1 = new TestEvent(new TimestampedId(object, when1), state1, Map.of());
                final var history = new ModifiableObjectHistory<>(event0);

                Append.this.test(history, event1);
            }

        }// class

        @RepeatedTest(32)
        public void multiThreaded() {
            final var event0 = new TestEvent(new TimestampedId(OBJECT_A, WHEN_A), Integer.valueOf(0), Map.of());
            final var event1 = new TestEvent(new TimestampedId(OBJECT_A, WHEN_A.plusMillis(1)), Integer.valueOf(1),
                    Map.of());
            final var event2 = new TestEvent(new TimestampedId(OBJECT_A, WHEN_A.plusMillis(2)), Integer.valueOf(2),
                    Map.of());
            final var history = new ModifiableObjectHistory<>(event0);

            final CountDownLatch ready = new CountDownLatch(1);
            final var future1 = testInOtherThread(history, event1, ready);
            final var future2 = testInOtherThread(history, event2, ready);
            ready.countDown();
            ThreadSafetyTest.get(future1);
            ThreadSafetyTest.get(future2);

            assertInvariants(history);
            assertSame(event2, history.getLastEvent(), "lastEvent");
        }

        private <STATE> void test(@Nonnull final ModifiableObjectHistory<STATE> history,
                @Nonnull final Event<STATE> event) {
            final var object0 = history.getObject();
            final var start0 = history.getStart();

            history.append(event);

            assertInvariants(history);
            assertAll("Does not change constants", () -> assertSame(object0, history.getObject(), "object"),
                    () -> assertSame(start0, history.getStart(), "start"));
            assertSame(event, history.getLastEvent(), "lastEvent");
        }

        private Future<Void> testInOtherThread(final ModifiableObjectHistory<Integer> history, final TestEvent event,
                final CountDownLatch ready) {
            return ThreadSafetyTest.runInOtherThread(ready, () -> {
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
            final var event0 = new TestEvent(new TimestampedId(OBJECT_A, WHEN_A), Integer.valueOf(0), Map.of());
            final var event1 = new TestEvent(new TimestampedId(OBJECT_A, WHEN_A.plusMillis(1)), Integer.valueOf(1),
                    Map.of());
            final var event2 = new TestEvent(new TimestampedId(OBJECT_A, WHEN_A.plusMillis(2)), Integer.valueOf(2),
                    Map.of());
            final var history = new ModifiableObjectHistory<>(event0);
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
                final ModifiableObjectHistory<STATE> history = new ModifiableObjectHistory<>(expectedLastEvent);

                final boolean success = CompareAndAppend.this.test(history, expectedLastEvent, event);

                assertTrue(success, "success");
            }

            private void test(final UUID object, final Duration time1, final Integer state1, final Duration time2,
                    final Integer state2) {
                final var event1 = new TestEvent(new TimestampedId(object, time1), state1, Map.of());
                final var event2 = new TestEvent(new TimestampedId(object, time2), state2, Map.of());

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
                final ModifiableObjectHistory<STATE> history = new ModifiableObjectHistory<>(lastEvent);

                final boolean success = CompareAndAppend.this.test(history, expectedLastEvent, event);

                assertFalse(success, "failure");
            }

            private void test(final UUID object, final Duration lastEventTime, final Integer lastEventState,
                    final Duration expectedLastEventTime, final Integer expectedLastEventState,
                    final Duration eventTime, final Integer eventState) {
                final var lastEvent = new TestEvent(new TimestampedId(object, lastEventTime), lastEventState, Map.of());
                final var expectedLastEvent = new TestEvent(new TimestampedId(object, expectedLastEventTime),
                        expectedLastEventState, Map.of());
                final var event = new TestEvent(new TimestampedId(object, eventTime), eventState, Map.of());

                test(lastEvent, expectedLastEvent, event);
            }

        }// class

        private <STATE> boolean test(@Nonnull final ModifiableObjectHistory<STATE> history,
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
                    final var history = new ModifiableObjectHistory<>(event);

                    Copy.this.test(history);
                }

                private void test(final UUID object, final Duration start, final Integer state,
                        final Map<UUID, Duration> nextEventDependencies) {
                    final var event = new TestEvent(new TimestampedId(object, start), state, nextEventDependencies);

                    test(event);
                }

            }// class

            private <STATE> void test(@Nonnull final ModifiableObjectHistory<STATE> that) {
                final var copy = new ModifiableObjectHistory<>(that);

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
                    final var event = new TestEvent(new TimestampedId(object, start), state, Map.of());

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
                    final var event = new TestEvent(new TimestampedId(object, end), state2, Map.of());

                    History.this.test(previousStateTransitions, event);
                }

            }// class

            private <STATE> void test(@Nonnull final SortedMap<Duration, STATE> previousStateTransitions,
                    @Nonnull final Event<STATE> lastEvent) {
                final var history = new ModifiableObjectHistory<>(previousStateTransitions, lastEvent);

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
                final var event = new TestEvent(new TimestampedId(OBJECT_A, WHEN_C), Integer.valueOf(3), Map.of());

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
                final var history = new ModifiableObjectHistory<>(event);

                assertInvariants(history);
                assertSame(event, history.getLastEvent(), "lastEvent");
            }

            private void test(final UUID object, final Duration start, final Integer state,
                    final Map<UUID, Duration> nextEventDependencies) {
                final var event = new TestEvent(new TimestampedId(object, start), state, nextEventDependencies);
                test(event);
            }

        }// class

    }// class

    @Nested
    public class JSON {

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
                final var history = new ModifiableObjectHistory<>(event);

                JSON.this.test(history);
            }

            private void test(final UUID object, final Duration start, final Integer state,
                    final Map<UUID, Duration> nextEventDependencies) {
                final var event = new TestEvent(new TimestampedId(object, start), state, nextEventDependencies);

                test(event);
            }

        }// class

        @Nested
        public class TwoTransitions {

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
                final var event = new TestEvent(new TimestampedId(object, end), state2, Map.of());
                final var history = new ModifiableObjectHistory<>(previousStateTransitions, event);

                JSON.this.test(history);
            }

        }// class

        private <STATE> void test(@Nonnull final ModifiableObjectHistory<STATE> history) {
            final var deserialized = JsonTest.serializeAndDeserialize(history);

            assertInvariants(history);
            assertInvariants(history, deserialized);
            assertEquals(history, deserialized);
            assertAll(() -> assertEquals(history.getEnd(), deserialized.getEnd(), "end"),
                    () -> assertEquals(history.getLastEvent(), deserialized.getLastEvent(), "lastEvent"),
                    () -> assertEquals(history.getObject(), deserialized.getObject(), "object"),
                    () -> assertEquals(history.getStart(), deserialized.getStart(), "start"),
                    () -> assertEquals(history.getStateHistory(), deserialized.getStateHistory(), "stateHistory"));
        }

    }// class

    @Nested
    public class ObserveEventsAfterAppend {

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
            final var event0 = new TestEvent(new TimestampedId(object, WHEN_A), Integer.valueOf(0), Map.of());
            final var event1 = new TestEvent(new TimestampedId(object, WHEN_A.plusMillis(10)), null, Map.of());
            final var history = new ModifiableObjectHistory<>(event0);
            history.append(event1);

            final var flux = observeEvents(history);

            StepVerifier.create(flux).expectNext(event1).expectComplete().verify(Duration.ofMillis(100));
        }

        private void testNonDestruction(final UUID object, final Duration when0, final Integer state0,
                final Duration when1, final Integer state1) {
            final var event0 = new TestEvent(new TimestampedId(object, when0), state0, Map.of());
            final var event1 = new TestEvent(new TimestampedId(object, when1), state1, Map.of());
            final var history = new ModifiableObjectHistory<>(event0);
            history.append(event1);

            final var flux = observeEvents(history);

            StepVerifier.create(flux).expectNext(event1).expectTimeout(Duration.ofMillis(100)).verify();
        }

    }// class

    @Nested
    public class ObserveState {

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
                final var event0 = new TestEvent(new TimestampedId(OBJECT_A, time0), state0, Map.of());
                final var event1 = new TestEvent(new TimestampedId(OBJECT_A, time1), state1, Map.of());
                final var history = new ModifiableObjectHistory<>(event0);

                final var states = observeState(history, when);

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
                final var event0 = new TestEvent(new TimestampedId(OBJECT_A, time0), state0, Map.of());
                final var event1 = new TestEvent(new TimestampedId(OBJECT_A, time1), state1, Map.of());
                final var event2 = new TestEvent(new TimestampedId(OBJECT_A, time2), state2, Map.of());
                final var history = new ModifiableObjectHistory<>(event0);
                history.append(event1);
                history.append(event2);

                final var states = observeState(history, when);

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
                final var event0 = new TestEvent(new TimestampedId(OBJECT_A, time0), state0, Map.of());
                final var event1 = new TestEvent(new TimestampedId(OBJECT_A, time1), state1, Map.of());
                final var event2 = new TestEvent(new TimestampedId(OBJECT_A, time2), state2, Map.of());
                final var history = new ModifiableObjectHistory<>(event0);

                final var states = observeState(history, when);

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
                final var event0 = new TestEvent(new TimestampedId(OBJECT_A, time0), state0, Map.of());
                final var event1 = new TestEvent(new TimestampedId(OBJECT_A, time1), state1, Map.of());
                final var event2 = new TestEvent(new TimestampedId(OBJECT_A, time2), state2, Map.of());
                final var history = new ModifiableObjectHistory<>(event0);

                final var states = observeState(history, when);

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
                final var event0 = new TestEvent(new TimestampedId(OBJECT_A, time0), state0, Map.of());
                final var event1 = new TestEvent(new TimestampedId(OBJECT_A, time1), state1, Map.of());
                final var history = new ModifiableObjectHistory<>(event0);

                final var states = observeState(history, when);

                StepVerifier.create(states).expectNext(expectedState0).then(() -> history.append(event1))
                        .expectNext(expectedState1).expectComplete().verify(Duration.ofMillis(100));
            }

        }// class

    }// class

    @Nested
    public class ObserveStateTransitionsAfterAppend {

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
            final var expectedStateTransition = new ModifiableObjectHistory.TimestampedState<>(when1, state1);
            final var event0 = new TestEvent(new TimestampedId(object, when1.minusMillis(10)), Integer.valueOf(0),
                    Map.of());
            final var event1 = new TestEvent(new TimestampedId(object, when1), state1, Map.of());
            final var history = new ModifiableObjectHistory<>(event0);
            history.append(event1);

            final var flux = observeStateTransitions(history);

            StepVerifier.create(flux).expectNext(expectedStateTransition).expectComplete()
                    .verify(Duration.ofMillis(100));
        }

        private void testNonDestruction(final UUID object, final Duration when0, final Integer state0,
                final Duration when1, final Integer state1) {
            final var expectedStateTransition = new ModifiableObjectHistory.TimestampedState<>(when1, state1);
            final var event0 = new TestEvent(new TimestampedId(object, when0), state0, Map.of());
            final var event1 = new TestEvent(new TimestampedId(object, when1), state1, Map.of());
            final var history = new ModifiableObjectHistory<>(event0);
            history.append(event1);

            final var flux = observeStateTransitions(history);

            StepVerifier.create(flux).expectNext(expectedStateTransition).expectTimeout(Duration.ofMillis(100))
                    .verify();
        }

    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);
    private static final Duration WHEN_C = Duration.ofMillis(7000);

    public static <STATE> void assertInvariants(@Nonnull final ModifiableObjectHistory<STATE> history) {
        ObjectHistoryTest.assertInvariants(history);// inherited
    }

    public static <STATE> void assertInvariants(@Nonnull final ModifiableObjectHistory<STATE> history1,
            @Nonnull final ModifiableObjectHistory<STATE> history2) {
        ObjectHistoryTest.assertInvariants(history1, history2);// inherited
    }

    private static <STATE> Flux<Event<STATE>> observeEvents(@Nonnull final ModifiableObjectHistory<STATE> history) {
        final var flux = ObjectHistoryTest.observeEvents(history);// inherited

        assertInvariants(history);
        assertNotNull(flux, "Not null, result");
        return flux;
    }

    private static <STATE> Publisher<Optional<STATE>> observeState(
            @Nonnull final ModifiableObjectHistory<STATE> history, @Nonnull final Duration when) {
        final var states = ObjectHistoryTest.observeState(history, when);// inherited

        assertInvariants(history);
        assertNotNull(states, "Not null, states");// guard

        return states;
    }

    private static <STATE> Flux<TimestampedState<STATE>> observeStateTransitions(
            @Nonnull final ModifiableObjectHistory<STATE> history) {
        final var flux = ObjectHistoryTest.observeStateTransitions(history);// inherited

        assertInvariants(history);
        assertNotNull(flux, "Not null, result");
        return flux;
    }
}
