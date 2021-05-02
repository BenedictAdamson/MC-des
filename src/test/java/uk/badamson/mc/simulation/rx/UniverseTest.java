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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
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
    public class AdvanceState {

        @Nested
        public class DependencyAlsoAdvanced {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0), OBJECT_B, WHEN_A.minusSeconds(1), Integer.valueOf(1));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(4), OBJECT_A, WHEN_B.minusDays(1), Integer.valueOf(2));
            }

            private void test(@Nonnull final UUID advancedObject, @Nonnull final Duration advancedObjectTime0,
                    @Nonnull final Integer advancedObjectState, @Nonnull final UUID dependentObject,
                    @Nonnull final Duration dependencyTime, @Nonnull final Integer dependentState) {
                assert !advancedObject.equals(dependentObject);
                assert dependencyTime.compareTo(advancedObjectTime0) < 0;
                final var dependentObjectTime = dependencyTime.minusNanos(1);// tough test
                final var dependentObjectInitialEvent = new TestEvent(
                        new ObjectStateId(dependentObject, dependentObjectTime), dependentState, Map.of());
                final var advancedObjectInitialEvent = new TestEvent(
                        new ObjectStateId(advancedObject, advancedObjectTime0), advancedObjectState,
                        Map.of(dependentObject, dependencyTime));
                final var nextEvent = advancedObjectInitialEvent
                        .computeNextEvent(Map.of(dependentObject, dependentState));
                final var when = nextEvent.getWhen().minusNanos(1);

                final Universe<Integer> universe = new Universe<>();
                universe.addObject(dependentObjectInitialEvent);
                universe.addObject(advancedObjectInitialEvent);
                final var states = universe.observeState(advancedObject, when);

                StepVerifier.create(states).expectNext(Optional.of(advancedObjectState))
                        .then(() -> universe.advanceState(advancedObject))
                        .then(() -> universe.advanceState(dependentObject)).expectComplete().verify();
            }

        }// class

        @Nested
        public class DependencyWithProvisionalState {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0), OBJECT_B, WHEN_A.minusSeconds(1), Integer.valueOf(1));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(4), OBJECT_A, WHEN_B.minusDays(1), Integer.valueOf(2));
            }

            private void test(@Nonnull final UUID advancedObject, @Nonnull final Duration advancedObjectTime0,
                    @Nonnull final Integer advancedObjectState, @Nonnull final UUID dependentObject,
                    @Nonnull final Duration dependencyTime, @Nonnull final Integer dependentState) {
                assert !advancedObject.equals(dependentObject);
                assert dependencyTime.compareTo(advancedObjectTime0) < 0;
                final var dependentObjectTime = dependencyTime.minusNanos(1);// tough test
                final var dependentObjectInitialEvent = new TestEvent(
                        new ObjectStateId(dependentObject, dependentObjectTime), dependentState, Map.of());
                final var advancedObjectInitialEvent = new TestEvent(
                        new ObjectStateId(advancedObject, advancedObjectTime0), advancedObjectState,
                        Map.of(dependentObject, dependencyTime));
                final var nextEvent = advancedObjectInitialEvent
                        .computeNextEvent(Map.of(dependentObject, dependentState));
                final var when = nextEvent.getWhen();

                final Universe<Integer> universe = new Universe<>();
                universe.addObject(dependentObjectInitialEvent);
                universe.addObject(advancedObjectInitialEvent);
                final var states = universe.observeState(advancedObject, when);

                StepVerifier.create(states).expectNext(Optional.of(advancedObjectState))
                        .then(() -> universe.advanceState(advancedObject)).expectTimeout(Duration.ofMillis(100))
                        .verify();
            }

        }// class

        @Nested
        public class NoDependencies {

            @Nested
            public class ProvisionalStateConfirmedAsReliable {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, Integer.valueOf(0));
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, Integer.valueOf(1));
                }

                private void test(@Nonnull final UUID object, @Nonnull final Duration time0,
                        @Nonnull final Integer state0) {
                    final var id0 = new ObjectStateId(object, time0);
                    final var event0 = new TestEvent(id0, state0, Map.of());
                    final var event1 = event0.computeNextEvent(Map.of());
                    final var time1 = event1.getWhen();
                    final var when = time1.minusNanos(1);// tough test

                    final var universe = new Universe<Integer>();
                    universe.addObject(event0);

                    AdvanceState.this.test(universe, object);

                    final var states = universe.observeState(object, when);
                    StepVerifier.create(states).expectNext(Optional.of(state0)).expectComplete()
                            .verify(Duration.ofMillis(100));
                }

            }// class

            @Nested
            public class UpdatesProvisionalState {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, Integer.valueOf(0));
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, Integer.valueOf(1));
                }

                private void test(@Nonnull final UUID object, @Nonnull final Duration time0,
                        @Nonnull final Integer state0) {
                    final var id0 = new ObjectStateId(object, time0);
                    final var event0 = new TestEvent(id0, state0, Map.of());
                    final var event1 = event0.computeNextEvent(Map.of());
                    final var time1 = event1.getWhen();
                    final var state1 = event1.getState();
                    final var when = time1;// tough test
                    assert !state0.equals(state1);

                    final var universe = new Universe<Integer>();
                    universe.addObject(event0);

                    final var states = universe.observeState(object, when);
                    StepVerifier.create(states).expectNext(Optional.of(state0))
                            .then(() -> universe.advanceState(object)).expectNext(Optional.of(state1)).expectComplete()
                            .verify(Duration.ofMillis(100));
                }
            }// class
        }// class

        private <STATE> void test(@Nonnull final Universe<STATE> universe, @Nonnull final UUID object) {
            universe.advanceState(object);

            assertInvariants(universe);
        }
    }// class

    @Nested
    public class ObserveNextEvent {

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
                final var expectedNextEvent = event.computeNextEvent(Map.of());
                final Universe<Integer> universe = new Universe<>();
                universe.addObject(event);

                final var events = ObserveNextEvent.this.test(universe, event);

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
                final var expectedNextEvent = event.computeNextEvent(Map.of(dependentObject, dependentState));
                assert !expectedNextEvent.equals(event.computeNextEvent(Map.of()));// critical

                final Universe<Integer> universe = new Universe<>();
                universe.addObject(dependentObjectInitialEvent);
                universe.addObject(event);

                final var events = ObserveNextEvent.this.test(universe, event);

                StepVerifier.create(events).expectNext(expectedNextEvent).expectComplete().verify();
            }

        }// class

        private <STATE> Mono<Event<STATE>> test(@Nonnull final Universe<STATE> universe,
                @Nonnull final Event<STATE> event) {
            final var events = universe.observeNextEvent(event);

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

    @Test
    public void constructor() {
        final var universe = new Universe<>();

        assertInvariants(universe);
        assertThat("The set of objects is empty.", universe.getObjects(), empty());
    }
}
