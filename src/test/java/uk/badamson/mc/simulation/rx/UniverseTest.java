package uk.badamson.mc.simulation.rx;
/*
 * © Copyright Benedict Adamson 2018.
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.simulation.ObjectStateId;
import uk.badamson.mc.simulation.rx.EventTest.TestEvent;

public class UniverseTest {

    @Nested
    public class AddObject {

        @Nested
        public class One {

            @Test
            public void a() {
                test(EVENT_A);
            }

            @Test
            public void b() {
                test(EVENT_B);
            }

            private <STATE> void test(@Nonnull final Event<STATE> event) {
                final Universe<STATE> universe = new Universe<>();

                AddObject.this.test(universe, event);
            }

        }// class

        private <STATE> void test(@Nonnull final Universe<STATE> universe, @Nonnull final Event<STATE> event) {
            final var objects0 = universe.getObjects();

            universe.addObject(event);

            assertInvariants(universe);
            final var objects = universe.getObjects();
            assertAll("objects",
                    () -> assertTrue(objects.containsAll(objects0),
                            "Does not remove any objects from the set of objects."),
                    () -> assertThat("The set of objects contains the object of the event.", objects,
                            hasItem(event.getObject())),
                    () -> assertThat("Adds one object to the set of objects.", objects, hasSize(objects0.size() + 1)));
        }

        @Test
        public void two() {
            final Universe<Integer> universe = new Universe<>();
            universe.addObject(EVENT_A);

            test(universe, EVENT_B);
        }
    }// class

    @Nested
    public class AdvanceStatesTo {

        @Nested
        public class One {

            @Test
            public void atFirstEvent() {
                final Duration start = WHEN_A;
                final Duration when = start;// critical

                test(OBJECT_A, start, Integer.valueOf(0), when);
            }

            @Test
            public void beforeFirstEvent() {
                final Duration start = WHEN_A;
                final Duration when = start.minusNanos(1);// tough test

                test(OBJECT_A, start, Integer.valueOf(0), when);
            }

            @Test
            public void destruction() {
                final Duration start = WHEN_A;
                final Duration when = start.plusSeconds(5);
                final var state0 = Integer.valueOf(Integer.MAX_VALUE);// magic number

                test(OBJECT_A, start, state0, when);
            }

            @Test
            public void far() {
                final Duration start = WHEN_A;
                final Duration when = start.plusSeconds(5);

                test(OBJECT_A, start, Integer.valueOf(0), when);
            }

            @Test
            public void justAfterFirstEvent_A() {
                final Duration start = WHEN_A;
                final Duration when = start.plusNanos(1);// critical

                test(OBJECT_A, start, Integer.valueOf(0), when);
            }

            @Test
            public void justAfterFirstEvent_B() {
                final Duration start = WHEN_B;
                final Duration when = start.plusNanos(1);// critical

                test(OBJECT_A, start, Integer.valueOf(1), when);
            }

            private void test(@Nonnull final UUID object, @Nonnull final Duration start, @Nonnull final Integer state0,
                    @Nonnull final Duration when) {
                final var event0 = new TestEvent(new ObjectStateId(object, start), state0, Map.of());
                final var universe = new Universe<Integer>();
                universe.addObject(event0);

                final var sequence = AdvanceStatesTo.this.test(universe, when, 1);

                StepVerifier.create(sequence).expectComplete().verify(Duration.ofMillis(100));
                StepVerifier.create(universe.observeState(object, when)).expectNextCount(1).expectComplete()
                        .verify(Duration.ofMillis(100));
            }

        }// class

        @Test
        public void creation() {
            final var object1 = OBJECT_A;
            final Duration start = WHEN_A;
            final Duration when = start.plusSeconds(5);
            final var state0 = Integer.valueOf(Integer.MIN_VALUE);// magic number
            final var event0 = new TestEvent(new ObjectStateId(object1, start), state0, Map.of());
            final var universe = new Universe<Integer>();
            universe.addObject(event0);

            final var sequence = AdvanceStatesTo.this.test(universe, when, 1);

            StepVerifier.create(sequence).expectComplete().verify(Duration.ofMillis(100));
            StepVerifier.create(universe.observeState(object1, when)).expectNextCount(1).expectComplete()
                    .verify(Duration.ofMillis(100));

            assertEquals(2, universe.getObjects().size(), "Number of objects");
        }

        @Test
        public void empty() {
            final var universe = new Universe<Integer>();

            final var sequence = test(universe, WHEN_A, 1);

            StepVerifier.create(sequence).expectComplete().verify();
        }

        @Test
        public void manyInParallel() {
            final int nObjects = 256;
            final int nThreads = 4;
            final var universe = new Universe<Integer>();
            for (int s = 0; s < nObjects; ++s) {
                final var state = Integer.valueOf(s);
                final var event = new TestEvent(new ObjectStateId(UUID.randomUUID(), WHEN_A), state, Map.of());
                universe.addObject(event);
            }

            final var sequence = AdvanceStatesTo.this.test(universe, WHEN_A.plusSeconds(16), nThreads);
            StepVerifier.create(sequence).expectComplete().verify(Duration.ofMillis(100));
            assertInvariants(universe);
        }

        private <STATE> Mono<Void> test(@Nonnull final Universe<STATE> universe, @Nonnull final Duration when,
                final int nThreads) {
            final var sequence = universe.advanceStatesTo(when, nThreads);

            assertInvariants(universe);
            assertNotNull(sequence, "Not null, sequence");

            return sequence;
        }
    }// class

    @Nested
    public class Constructor {

        @Nested
        public class Copy {

            @Nested
            public class OneEvent {

                @Test
                public void a() {
                    test(EVENT_A);
                }

                @Test
                public void b() {
                    test(EVENT_B);
                }

                private <STATE> void test(@Nonnull final Event<STATE> event) {
                    final Universe<STATE> universe = new Universe<>();
                    universe.addObject(event);

                    Copy.this.test(universe);
                }
            }// class

            @Test
            public void empty() {
                final var universe = new Universe<Integer>();

                test(universe);
            }

            private <STATE> void test(@Nonnull final Universe<STATE> that) {
                final var copy = new Universe<>(that);

                assertInvariants(copy);
                assertInvariants(that);
                assertInvariants(copy, that);

                assertThat("objects", copy.getObjects(), is(that.getObjects()));
            }

            @Test
            public void whileAdvancing() {
                final int nObjects = 256;
                final int nThreads = 4;
                final var universe = new Universe<Integer>();
                for (int s = 0; s < nObjects; ++s) {
                    final var state = Integer.valueOf(s);
                    final var event = new TestEvent(new ObjectStateId(UUID.randomUUID(), WHEN_A), state, Map.of());
                    universe.addObject(event);
                }
                final var end = WHEN_A.plusSeconds(16);

                final var sequence = universe.advanceStatesTo(end, nThreads);
                StepVerifier.create(sequence).then(() -> test(universe)).expectComplete()
                        .verify(Duration.ofMillis(100));
                assertInvariants(universe);
            }

            @RepeatedTest(64)
            public void whileCreatingObjects() {
                final var object1 = OBJECT_A;
                final Duration start = WHEN_A;
                final var timeout = Duration.ofMillis(100);
                final Duration when = start.plusSeconds(32);
                final var state0 = Integer.valueOf(Integer.MIN_VALUE);// magic number
                final var event0 = new TestEvent(new ObjectStateId(object1, start), state0, Map.of());
                final var universe = new Universe<Integer>();
                universe.addObject(event0);
                final var copy = new AtomicReference<Universe<Integer>>();

                final var sequence1 = universe.advanceStatesTo(when, 1);

                StepVerifier.create(sequence1).then(() -> copy.set(new Universe<>(universe))).expectComplete()
                        .verify(timeout);

                final var sequence2 = copy.get().advanceStatesTo(when, 1);
                StepVerifier.create(sequence2).expectComplete().verify(timeout);

                assertEquals(universe.getObjects().size(), copy.get().getObjects().size(),
                        "copy can have created objects");
            }
        }// class

        @Test
        public void noArgs() {
            final var universe = new Universe<>();

            assertInvariants(universe);
            assertThat("The set of objects is empty.", universe.getObjects(), empty());
        }

    }// class

    @Nested
    public class ObserveNextEvents {

        @Nested
        public class NoDependencies {

            @Test
            public void a() {
                test(OBJECT_STATE_ID_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_STATE_ID_B, Integer.valueOf(1));
            }

            private void test(@Nonnull final ObjectStateId id, @Nonnull final Integer state) {
                final var event = new TestEvent(id, state, Map.of());
                final var expectedNextEvent = event.computeNextEvents(Map.of());
                final Universe<Integer> universe = new Universe<>();
                universe.addObject(event);

                final var events = ObserveNextEvents.this.test(universe, event);

                StepVerifier.create(events).expectNext(expectedNextEvent).expectComplete().verify();
            }
        }// class

        @Nested
        public class OneDependency {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0), OBJECT_B, WHEN_A.minusNanos(1), Integer.valueOf(2));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(3), OBJECT_A, WHEN_A.minusDays(1), Integer.valueOf(20));
            }

            private void test(@Nonnull final UUID eventObject, @Nonnull final Duration eventTime,
                    @Nonnull final Integer eventState, @Nonnull final UUID dependentObject,
                    @Nonnull final Duration dependentObjectTime, @Nonnull final Integer dependentState) {
                assert !eventObject.equals(dependentObject);
                assert dependentObjectTime.compareTo(eventTime) < 0;
                final var dependentObjectInitialEvent = new TestEvent(
                        new ObjectStateId(dependentObject, dependentObjectTime), dependentState, Map.of());
                final var event = new TestEvent(new ObjectStateId(eventObject, eventTime), eventState,
                        Map.of(dependentObject, dependentObjectTime));
                final var expectedNextEvent = event.computeNextEvents(Map.of(dependentObject, dependentState));
                assert !expectedNextEvent.equals(event.computeNextEvents(Map.of()));// critical

                final Universe<Integer> universe = new Universe<>();
                universe.addObject(dependentObjectInitialEvent);
                universe.addObject(event);

                final var events = ObserveNextEvents.this.test(universe, event);

                StepVerifier.create(events).expectNext(expectedNextEvent).expectComplete().verify();
            }

        }// class

        private <STATE> Mono<Map<UUID, Event<STATE>>> test(@Nonnull final Universe<STATE> universe,
                @Nonnull final Event<STATE> event) {
            final var events = universe.observeNextEvents(event);

            assertInvariants(universe);
            assertNotNull(events, "Not null, result");
            return events;
        }

    }// class

    @Nested
    public class ObserveState {

        @Nested
        public class InitialState {

            @Test
            public void a() {
                test(EVENT_A);
            }

            @Test
            public void b() {
                test(EVENT_B);
            }

            private <STATE> void test(@Nonnull final Event<STATE> event) {
                final var state = event.getState();
                assert state != null;
                final Universe<STATE> universe = new Universe<>();
                universe.addObject(event);

                final var states = ObserveState.this.test(universe, event.getObject(), event.getWhen());

                StepVerifier.create(states).expectNext(Optional.of(state)).expectComplete();
            }
        }// class

        private <STATE> Publisher<Optional<STATE>> test(@Nonnull final Universe<STATE> universe,
                @Nonnull final UUID object, @Nonnull final Duration when) {
            final var states = universe.observeState(object, when);

            assertInvariants(universe);
            assertNotNull(states, "Not null, result");

            return states;
        }

    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();

    private static final UUID OBJECT_B = UUID.randomUUID();
    private static final Duration WHEN_A = Duration.ofMillis(0);

    private static final Duration WHEN_B = Duration.ofMillis(5000);
    private static final ObjectStateId OBJECT_STATE_ID_A = new ObjectStateId(OBJECT_A, WHEN_A);

    private static final ObjectStateId OBJECT_STATE_ID_B = new ObjectStateId(OBJECT_B, WHEN_B);
    private static final TestEvent EVENT_A = new TestEvent(OBJECT_STATE_ID_A, Integer.valueOf(0), Map.of());

    private static final TestEvent EVENT_B = new TestEvent(OBJECT_STATE_ID_B, Integer.valueOf(1), Map.of());

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe) {
        ObjectTest.assertInvariants(universe);// inherited

        final Set<UUID> objects = universe.getObjects();
        assertNotNull(objects, "Not null, objects");// guard
        assertFalse(objects.stream().anyMatch(id -> id == null), "The set of object IDs does not contain a null.");
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe1,
            @Nonnull final Universe<STATE> universe2) {
        ObjectTest.assertInvariants(universe1, universe2);// inherited
    }
}
