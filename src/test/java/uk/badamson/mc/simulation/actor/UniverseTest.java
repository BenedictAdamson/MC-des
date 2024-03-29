package uk.badamson.mc.simulation.actor;
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

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.badamson.dbc.assertions.CollectionVerifier;
import uk.badamson.dbc.assertions.ObjectVerifier;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UniverseTest {


    static final Medium MEDIUM_A = new Medium();

    static final Medium MEDIUM_B = new Medium();

    private static final Duration WHEN_A = Duration.ofMillis(0);

    private static final Duration WHEN_B = Duration.ofMillis(5000);

    private static final Duration WHEN_C = Duration.ofMillis(7000);

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe) {
        ObjectVerifier.assertInvariants(universe);// inherited

        CollectionVerifier.assertForAllElements(universe, actor -> {
            assertThat(actor, notNullValue());
            ActorTest.assertInvariants(actor);
        });
        assertThat(universe.toArray().length, is(universe.size()));
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe1,
                                                @Nonnull final Universe<STATE> universe2) {
        ObjectVerifier.assertInvariants(universe1, universe2);// inherited
    }

    private static <STATE> boolean add(@Nonnull final Universe<STATE> universe, @Nonnull final Actor<STATE> actor) {
        final int size0 = universe.size();

        final boolean added = universe.add(actor);

        assertInvariants(universe);
        ActorTest.assertInvariants(actor);
        assertAll(
                ()->assertThat(universe, hasItem(actor)),
        ()->assertThat(universe, not(empty())),
        ()->assertThat("contains(actor)", universe.contains(actor), is(true)));
        assertThat(universe.iterator().hasNext(), is(true));
        assertThat("parallelStream contains", universe.parallelStream().collect(Collectors.toUnmodifiableList()), hasItem(actor));
        assertThat(universe, either(hasSize(size0 + 1)).or(hasSize(size0)));
        assertThat("stream contains", universe.stream().collect(Collectors.toUnmodifiableList()), hasItem(actor));

        return added;
    }

    private static <STATE> boolean addAll(@Nonnull final Universe<STATE> universe, @Nonnull final Collection<Actor<STATE>> actors) {
        final boolean changed = universe.addAll(actors);

        assertInvariants(universe);
        assertThat(universe.containsAll(actors), is(true));

        return changed;
    }

    private static <STATE> void clear(@Nonnull final Universe<STATE> universe) {
        universe.clear();

        assertInvariants(universe);
        assertThat(universe, empty());
        assertThat(universe, hasSize(0));
    }

    private static <STATE> boolean remove(@Nonnull final Universe<STATE> universe, final Object o) {
        final int size0 = universe.size();

        final boolean removed = universe.remove(o);

        assertInvariants(universe);
        assertThat(universe.contains(o), is(false));
        assertThat(universe, hasSize(removed ? size0 - 1 : size0));

        return removed;
    }

    private static <STATE> boolean removeAll(@Nonnull final Universe<STATE> universe, final Collection<?> objects) {
        final int size0 = universe.size();

        final boolean removed = universe.removeAll(objects);

        assertInvariants(universe);
        assertThat(universe.size(), lessThanOrEqualTo(size0));
        assertThat("size has changed if element removed", removed && universe.size() == size0, is(false));

        return removed;
    }

    private static <STATE> boolean retainAll(@Nonnull final Universe<STATE> universe, @Nonnull final Collection<?> c) {
        final boolean changed = universe.retainAll(c);

        assertInvariants(universe);

        return changed;
    }


    private static <STATE> Future<Actor.AffectedActors<STATE>> advanceTo(@Nonnull final Universe<STATE> universe,
                                                                         @Nonnull final Duration when, @Nonnull final Executor executor
    ) {
        final Future<Actor.AffectedActors<STATE>> future = universe.advanceTo(when, executor);
        assertThat(future, notNullValue());
        return future;
    }

    private static <STATE> void assertAllHaveAdvancedTo(
            @Nonnull final Duration when, @Nonnull final Universe<STATE> universe
    ) {
        assertAll(
                universe.stream().map(actor -> () -> {
                    assertThat(actor, notNullValue());
                    ActorTest.assertInvariants(actor);
                    assertThat(actor.getWhenReceiveNextSignal(), greaterThanOrEqualTo(when));
                })
        );
    }

    @Test
    public void constructor() {
        final var universe = new Universe<>();

        assertInvariants(universe);
        assertThat(universe, empty());
        assertThat(universe.size(), is(0));
        assertThat("iterator.next", !universe.iterator().hasNext());
        assertThat(universe.toArray(), emptyArray());
    }

    @Nested
    public class Add {
        @Nested
        public class Once {

            @Test
            public void a() {
                test(WHEN_A, 0);
            }

            @Test
            public void b() {
                test(WHEN_B, 1);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Integer state0) {
                final Actor<Integer> actor = new Actor<>(start, state0);
                final Universe<Integer> universe = new Universe<>();

                final boolean added = add(universe, actor);

                assertThat("added", added);
                assertThat(universe, hasSize(1));
                assertThat(universe.iterator().next(), sameInstance(actor));
            }
        }

        @Nested
        public class Twice {
            @Test
            public void same() {
                final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actor);

                final boolean added = add(universe, actor);

                assertThat("not added", !added);
                assertThat(universe, hasSize(1));
                assertThat(universe.iterator().next(), sameInstance(actor));
            }

            @Test
            public void distinct() {
                final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
                final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actorA);

                final boolean added = add(universe, actorB);

                assertThat("added", added);
                assertThat(universe, hasSize(2));
                assertThat(universe, containsInAnyOrder(actorA, actorB));
                assertThat(universe.containsAll(List.of(actorA, actorB)), is(true));
            }
        }
    }

    @Nested
    public class AddAll {
        @Test
        public void empty() {
            final Universe<Integer> universe = new Universe<>();
            final Collection<Actor<Integer>> actors = List.of();

            final boolean changed = addAll(universe, actors);

            assertThat(changed, is(false));
            assertThat(universe, Matchers.empty());
        }

        @Test
        public void oneToEmpty() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            final Collection<Actor<Integer>> actors = List.of(actor);

            final boolean changed = addAll(universe, actors);

            assertThat(changed, is(true));
            assertThat(universe, contains(actor));
        }

        @Test
        public void oneToNotEmpty() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            universe.add(actorA);
            final Collection<Actor<Integer>> actors = List.of(actorB);

            final boolean changed = addAll(universe, actors);

            assertThat(changed, is(true));
            assertThat(universe, containsInAnyOrder(actorA, actorB));
        }

        @Test
        public void oneRedundant() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            final Collection<Actor<Integer>> actors = List.of(actor);
            universe.add(actor);

            final boolean changed = addAll(universe, actors);

            assertThat(changed, is(false));
            assertThat(universe, contains(actor));
        }

        @Test
        public void twoToEmpty() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            final Collection<Actor<Integer>> actors = List.of(actorA, actorB);

            final boolean changed = addAll(universe, actors);

            assertThat(changed, is(true));
            assertThat(universe, containsInAnyOrder(actorA, actorB));
        }
    }

    @Nested
    public class Clear {

        @Test
        public void empty() {
            final Universe<Integer> universe = new Universe<>();

            clear(universe);
        }

        @Test
        public void notEmpty() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            universe.add(actorA);
            universe.add(actorB);

            clear(universe);
        }
    }

    @Nested
    public class Remove {

        @Test
        public void nonActor() {
            final Universe<Integer> universe = new Universe<>();

            final boolean removed = remove(universe, new Object());

            assertThat(removed, is(false));
        }

        @Test
        public void absent() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);

            final boolean removed = remove(universe, actor);

            assertThat(removed, is(false));
        }

        @Test
        public void solePresent() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            universe.add(actor);

            final boolean removed = remove(universe, actor);

            assertThat(removed, is(true));
        }

        @Test
        public void present() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            universe.add(actorA);
            universe.add(actorB);

            final boolean removed = remove(universe, actorA);

            assertThat(removed, is(true));
            assertThat(universe, contains(actorB));
        }
    }

    @Nested
    public class RemoveAll {

        @Test
        public void nonActor() {
            final Universe<Integer> universe = new Universe<>();

            final boolean removed = removeAll(universe, List.of(new Object()));

            assertThat(removed, is(false));
        }

        @Test
        public void absent() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);

            final boolean removed = removeAll(universe, List.of(actor));

            assertThat(removed, is(false));
        }

        @Test
        public void solePresent() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            universe.add(actor);

            final boolean removed = removeAll(universe, List.of(actor));

            assertThat(removed, is(true));
        }

        @Test
        public void present() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            universe.add(actorA);
            universe.add(actorB);

            final boolean removed = removeAll(universe, List.of(actorA));

            assertThat(removed, is(true));
            assertThat(universe, contains(actorB));
        }

        @Test
        public void all() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            universe.add(actorA);
            universe.add(actorB);

            final boolean removed = removeAll(universe, List.of(actorA, actorB));

            assertThat(removed, is(true));
            assertThat(universe, empty());
        }
    }

    @Nested
    public class RetainAll {
        @Test
        public void bothEmpty() {
            final Universe<Integer> universe = new Universe<>();

            final boolean changed = retainAll(universe, List.of());

            assertThat(changed, is(false));
            assertThat(universe, empty());
        }

        @Test
        public void notPresent() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);

            final boolean changed = retainAll(universe, List.of(actor));

            assertThat(changed, is(false));
            assertThat(universe, empty());
        }

        @Test
        public void none() {
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            final Universe<Integer> universe = new Universe<>();
            universe.add(actor);

            final boolean changed = retainAll(universe, List.of());

            assertThat(changed, is(true));
            assertThat(universe, empty());
        }

        @Test
        public void all() {
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            final Universe<Integer> universe = new Universe<>();
            universe.add(actor);

            final boolean changed = retainAll(universe, List.of(actor));

            assertThat(changed, is(false));
            assertThat(universe, contains(actor));
        }

        @Test
        public void some() {
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            final Universe<Integer> universe = new Universe<>();
            universe.add(actorA);
            universe.add(actorB);

            final boolean changed = retainAll(universe, List.of(actorA));

            assertThat(changed, is(true));
            assertThat(universe, contains(actorA));
        }
    }


    @Nested
    public class AdvanceTo {
        @Test
        public void noActors() throws Exception {
            final Universe<Integer> universe = new Universe<>();

            final var future = advanceTo(universe, WHEN_A, DIRECT_EXECUTOR);
            final Actor.AffectedActors<Integer> affectedActors = future.get();

            assertInvariants(universe);
            assertThat(affectedActors, notNullValue());
            ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
            assertThat(affectedActors, is(Actor.AffectedActors.emptyInstance()));
        }

        @Nested
        public class NoOp1WithNoSignalsToReceive {

            @Test
            public void just() throws Exception {
                test(WHEN_A, WHEN_A, 0);
            }

            @Test
            public void after() throws Exception {
                test(WHEN_B, WHEN_C, 1);
            }

            private void test(
                    @Nonnull final Duration start, @Nonnull final Duration when, final Integer state
            ) throws Exception {
                assert start.compareTo(when) <= 0;
                final var actor = new Actor<>(start, state);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actor);

                final var future = advanceTo(universe, when, DIRECT_EXECUTOR);
                final Actor.AffectedActors<Integer> affectedActors = future.get();

                assertInvariants(universe);
                assertAllHaveAdvancedTo(when, universe);
                assertThat(universe, contains(actor));
                assertThat(affectedActors, notNullValue());
                ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
                assertThat(affectedActors, is(Actor.AffectedActors.emptyInstance()));

            }
        }

        @Nested
        public class WhenReceivingASignalIsNecessary {

            @Test
            public void near() throws Exception {
                test(WHEN_A, 1, Duration.ofNanos(1L));
            }

            @Test
            public void far() throws Exception {
                test(WHEN_B, 2, Duration.ofDays(365));
            }

            @Test
            public void two() throws Exception {
                final Duration margin = Duration.ofSeconds(1L);
                assert WHEN_A.compareTo(margin) < 0;
                final var sender = new Actor<>(WHEN_A, 1);
                final var actorA = new Actor<>(WHEN_A, 1);
                final var actorB = new Actor<>(WHEN_A, 2);
                final var signalA = new SignalTest.SimpleTestSignal(WHEN_A, sender, actorA, MEDIUM_A);
                final var signalB = new SignalTest.SimpleTestSignal(WHEN_A, sender, actorB, MEDIUM_B);
                actorA.addSignalToReceive(signalA);
                actorB.addSignalToReceive(signalB);
                final Duration whenReceiveNextSignalA = actorA.getWhenReceiveNextSignal();
                final Duration whenReceiveNextSignalB = actorB.getWhenReceiveNextSignal();
                assert whenReceiveNextSignalA.compareTo(Signal.NEVER_RECEIVED.minus(margin)) <= 0;
                assert whenReceiveNextSignalB.compareTo(Signal.NEVER_RECEIVED.minus(margin)) <= 0;
                final var when = Collections.max(Arrays.asList(
                        whenReceiveNextSignalA, whenReceiveNextSignalB
                )).plus(margin);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actorA);
                universe.add(actorB);

                final var future = advanceTo(universe, when, DIRECT_EXECUTOR);
                final Actor.AffectedActors<Integer> affectedActors = future.get();

                assertInvariants(universe);
                ActorTest.assertInvariants(actorA);
                ActorTest.assertInvariants(actorB);
                assertAllHaveAdvancedTo(when, universe);
                assertThat(universe, containsInAnyOrder(actorA, actorB));
                assertThat(affectedActors, notNullValue());
                ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
                assertAll(
                        () -> assertThat(affectedActors.getRemoved(), empty()),
                        () -> assertThat(affectedActors.getAdded(), empty()),
                        () -> assertThat(affectedActors.getChanged(), containsInAnyOrder(actorA, actorB)));
            }

            @Test
            public void addsActor() throws Exception {
                final Duration margin = Duration.ofSeconds(1L);
                assert WHEN_A.compareTo(margin) < 0;
                final var sender = new Actor<>(WHEN_A, 1);
                final var actor = new Actor<>(WHEN_A, 1);
                final Signal<Integer> signal = new SignalTest.ActorCreatingTestSignal(WHEN_A, sender, actor, MEDIUM_A);
                actor.addSignalToReceive(signal);
                final Duration whenReceiveNextSignal = actor.getWhenReceiveNextSignal();
                assert whenReceiveNextSignal.compareTo(Signal.NEVER_RECEIVED.minus(margin)) <= 0;
                final var when = whenReceiveNextSignal.plus(margin);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actor);

                final var future = advanceTo(universe, when, DIRECT_EXECUTOR);
                final Actor.AffectedActors<Integer> affectedActors = future.get();

                assertInvariants(universe);
                ActorTest.assertInvariants(actor);
                assertAllHaveAdvancedTo(when, universe);
                assertThat(universe, hasItem(actor));
                assertThat(affectedActors, notNullValue());
                ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
                final var actorsAdded = affectedActors.getAdded();
                assertAll(
                        () -> assertThat(affectedActors.getRemoved(), empty()),
                        () -> assertThat(actorsAdded, hasSize(1)),
                        () -> assertThat(affectedActors.getChanged(), contains(actor)));
                final var actorAdded = actorsAdded.iterator().next();
                assertThat(universe, containsInAnyOrder(actor, actorAdded));
            }

            @Test
            public void addsInteractingActor() throws Exception {
                final Duration margin = Duration.ofSeconds(20);
                assert WHEN_A.compareTo(margin) < 0;
                final var sender = new Actor<>(WHEN_A, 1);
                final var actor = new Actor<>(WHEN_A, 1);
                final Signal<Integer> signal = new SignalTest.InteractingActorCreatingTestSignal(WHEN_A, sender, actor, MEDIUM_A);
                actor.addSignalToReceive(signal);
                final Duration whenReceiveNextSignal = actor.getWhenReceiveNextSignal();
                assert whenReceiveNextSignal.compareTo(Signal.NEVER_RECEIVED.minus(margin)) <= 0;
                final var when = whenReceiveNextSignal.plus(margin);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actor);

                final var future = advanceTo(universe, when, DIRECT_EXECUTOR);
                final Actor.AffectedActors<Integer> affectedActors = future.get();

                assertInvariants(universe);
                ActorTest.assertInvariants(actor);
                assertAllHaveAdvancedTo(when, universe);
                assertThat(universe, hasItem(actor));
                assertThat(affectedActors, notNullValue());
                ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
                final var actorsAdded = affectedActors.getAdded();
                assertAll(
                        () -> assertThat(affectedActors.getRemoved(), empty()),
                        () -> assertThat(actorsAdded, hasSize(1)),
                        () -> assertThat(affectedActors.getChanged(), contains(actor)));
                final var actorAdded = actorsAdded.iterator().next();
                assertThat(universe, containsInAnyOrder(actor, actorAdded));
            }

            @Test
            public void removesActor() throws Exception {
                final var state0 = 1;
                final var margin = Duration.ofNanos(1);
                final var actor1 = new Actor<>(WHEN_A, 0);
                final var actor2 = new Actor<>(WHEN_A, state0);
                final Signal<Integer> signal1 = new SignalTest.SimpleTestSignal(WHEN_A, actor1, actor2, MEDIUM_A);
                final Duration whenSent2 = signal1.getWhenReceived(state0);
                final Signal<Integer> signal2 = new SignalTest.ActorCreatingTestSignal(whenSent2, actor1, actor2, MEDIUM_B);
                actor2.addSignalToReceive(signal2);
                final var affectedActors2 = actor2.receiveSignal();
                assert affectedActors2.getAdded().size() == 1;
                final var addedActor = affectedActors2.getAdded().iterator().next();
                actor2.addSignalToReceive(signal1);
                final Duration whenReceiveNextSignal = actor2.getWhenReceiveNextSignal();
                assert whenReceiveNextSignal.compareTo(Signal.NEVER_RECEIVED.minus(margin)) <= 0;
                final var when = whenReceiveNextSignal.plus(margin);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actor2);
                universe.add(addedActor);

                final var future = advanceTo(universe, when, DIRECT_EXECUTOR);
                final Actor.AffectedActors<Integer> affectedActors = future.get();

                assertInvariants(universe);
                ActorTest.assertInvariants(actor2);
                ActorTest.assertInvariants(addedActor);
                assertAllHaveAdvancedTo(when, universe);
                assertThat(universe, hasItem(actor2));
                assertThat(affectedActors, notNullValue());
                ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
                final var actorsAdded = affectedActors.getAdded();
                assertAll(
                        () -> assertThat("removed", affectedActors.getRemoved(), contains(addedActor)),
                        () -> assertThat("added", actorsAdded, empty()),
                        () -> assertThat("changed", affectedActors.getChanged(), contains(actor2)));
                assertThat(universe, contains(actor2));
            }

            @Test
            public void chainingSignals() throws Exception {
                final Duration when = Duration.ofSeconds(60);
                final var actor1 = new Actor<>(WHEN_A, new ActorTest.NeighbourActorState(null));
                final var actor2 = new Actor<>(WHEN_A, new ActorTest.NeighbourActorState(actor1));
                final var actor3 = new Actor<>(WHEN_A, new ActorTest.NeighbourActorState(actor2));
                final var actor4 = new Actor<>(WHEN_A, new ActorTest.NeighbourActorState(actor3));
                final var signal = new ActorTest.NeighbourSignal(WHEN_A, actor4, actor3);
                actor3.addSignalToReceive(signal);
                final Universe<ActorTest.NeighbourActorState> universe = new Universe<>();
                universe.add(actor3);

                final var future = advanceTo(universe, when, DIRECT_EXECUTOR);
                final var affectedActors = future.get();

                ActorTest.assertInvariants(actor1);
                ActorTest.assertInvariants(actor2);
                ActorTest.assertInvariants(actor3);
                assertAllHaveAdvancedTo(when, universe);
                assertThat(universe, contains(actor3));
                assertThat(affectedActors, notNullValue());
                ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
                assertAll(
                        () -> assertThat("removed", affectedActors.getRemoved(), empty()),
                        () -> assertThat("added", affectedActors.getAdded(), empty()),
                        () -> assertThat("changed", affectedActors.getChanged(), containsInAnyOrder(actor1, actor2, actor3)));
            }

            @Test
            public void concurrent() throws Exception {
                final Duration margin = Duration.ofSeconds(1L);
                final int nThreads = 16;
                final int nActors = nThreads * 4;
                assert WHEN_A.compareTo(margin) < 0;
                final var sender = new Actor<>(WHEN_A, 1);
                final Universe<Integer> universe = new Universe<>();
                for (int a = 0; a < nActors; ++a) {
                    final var actor = new Actor<>(WHEN_A, a);
                    final var signal = new SignalTest.SimpleTestSignal(WHEN_A, sender, actor, MEDIUM_A);
                    actor.addSignalToReceive(signal);
                    universe.add(actor);
                }
                final Duration whenReceiveNextSignal = universe.stream()
                        .map(Actor::getWhenReceiveNextSignal)
                        .max(Comparator.naturalOrder())
                        .orElseThrow();
                assert whenReceiveNextSignal.compareTo(Signal.NEVER_RECEIVED.minus(margin)) <= 0;
                final var when = whenReceiveNextSignal.plus(margin);
                final Executor executor = Executors.newFixedThreadPool(nThreads);

                final var future = advanceTo(universe, when, executor);
                final Actor.AffectedActors<Integer> affectedActors = future.get();

                assertAllHaveAdvancedTo(when, universe);
                assertThat(affectedActors, notNullValue());
                ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
                assertAll(
                        () -> assertThat(affectedActors.getRemoved(), empty()),
                        () -> assertThat(affectedActors.getAdded(), empty()),
                        () -> assertThat(affectedActors.getChanged(), containsInAnyOrder(universe.toArray())));
            }

            @Test
            public void receiveSignalThrowsException() {
                final Duration margin = Duration.ofSeconds(10);
                assert WHEN_B.compareTo(margin) < 0;
                final var sender = new Actor<>(WHEN_B, 2);
                final var actor = new Actor<>(WHEN_B, 2);
                final var signal = new SignalTest.ThrowingSignal(WHEN_B, sender, actor, MEDIUM_A);
                actor.addSignalToReceive(signal);
                final Duration whenReceiveNextSignal = actor.getWhenReceiveNextSignal();
                assert whenReceiveNextSignal.compareTo(Signal.NEVER_RECEIVED.minus(margin)) <= 0;
                final var when = whenReceiveNextSignal.plus(margin);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actor);

                final var future = advanceTo(universe, when, DIRECT_EXECUTOR);
                final ExecutionException exception = assertThrows(ExecutionException.class, future::get);

                assertThat(exception.getCause(), isA(Actor.SignalException.class));
                assertThat(exception.getCause().getCause(), isA(SignalTest.ThrowingSignal.InevitableException.class));
            }

            private void test(
                    @Nonnull final Duration start, final int state0, @Nonnull @Nonnegative final Duration margin
            ) throws Exception {
                assert start.compareTo(margin) < 0;
                final var sender = new Actor<>(start, state0);
                final var actor = new Actor<>(start, state0);
                final var signal = new SignalTest.SimpleTestSignal(start, sender, actor, MEDIUM_A);
                actor.addSignalToReceive(signal);
                final Duration whenReceiveNextSignal = actor.getWhenReceiveNextSignal();
                assert whenReceiveNextSignal.compareTo(Signal.NEVER_RECEIVED.minus(margin)) <= 0;
                final var when = whenReceiveNextSignal.plus(margin);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actor);

                final var future = advanceTo(universe, when, DIRECT_EXECUTOR);
                final Actor.AffectedActors<Integer> affectedActors = future.get();

                ActorTest.assertInvariants(actor);
                assertAllHaveAdvancedTo(when, universe);
                assertThat(universe, contains(actor));
                assertThat(affectedActors, notNullValue());
                ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
                assertAll(
                        () -> assertThat(affectedActors.getRemoved(), empty()),
                        () -> assertThat(affectedActors.getAdded(), empty()),
                        () -> assertThat(affectedActors.getChanged(), contains(actor)));
            }
        }

        @Nested
        public class WhenReceivingASequenceOfSignalsIsNecessary {

            @Test
            public void a() throws Exception {
                test(WHEN_A, 1, Duration.ofDays(1));
            }

            @Test
            public void b() throws Exception {
                test(WHEN_B, 2, Duration.ofDays(2));
            }

            private void test(@Nonnull final Duration start, final int state0, @Nonnull @Nonnegative final Duration margin) throws Exception {
                assert start.compareTo(margin) < 0;
                final var sender = new Actor<>(start, state0);
                final var actor = new Actor<>(start, state0);
                final var signal = new SignalTest.StrobingTestSignal(start, sender, actor, MEDIUM_A);
                actor.addSignalToReceive(signal);
                final Duration whenReceiveNextSignal = actor.getWhenReceiveNextSignal();
                assert whenReceiveNextSignal.compareTo(Signal.NEVER_RECEIVED.minus(margin)) <= 0;
                final var when = whenReceiveNextSignal.plus(margin);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actor);

                final var future = advanceTo(universe, when, DIRECT_EXECUTOR);
                final Actor.AffectedActors<Integer> affectedActors = future.get();

                ActorTest.assertInvariants(actor);
                assertAllHaveAdvancedTo(when, universe);
                assertThat(universe, contains(actor));
                assertThat(affectedActors, notNullValue());
                ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
                assertAll(
                        () -> assertThat(affectedActors.getRemoved(), empty()),
                        () -> assertThat(affectedActors.getAdded(), empty()),
                        () -> assertThat(affectedActors.getChanged(), contains(actor)));
            }
        }

        @Nested
        public class WhenReceivingASignalIsUnnecessary {

            @Test
            public void near() throws Exception {
                test(WHEN_A, 1, Duration.ofNanos(1L));
            }

            @Test
            public void far() throws Exception {
                test(WHEN_B, 2, Duration.ofDays(365));
            }

            private void test(@Nonnull final Duration start, final int state0, @Nonnull @Nonnegative final Duration margin) throws Exception {
                assert start.compareTo(margin) < 0;
                final var sender = new Actor<>(start, state0);
                final var actor = new Actor<>(start, state0);
                final var signal = new SignalTest.SimpleTestSignal(start, sender, actor, MEDIUM_A);
                actor.addSignalToReceive(signal);
                final Duration whenReceiveNextSignal = actor.getWhenReceiveNextSignal();
                final var when = whenReceiveNextSignal.minus(margin);
                assert !actor.getSignalsToReceive().isEmpty();
                final Universe<Integer> universe = new Universe<>();
                universe.add(actor);

                final var future = advanceTo(universe, when, DIRECT_EXECUTOR);
                final Actor.AffectedActors<Integer> affectedActors = future.get();

                ActorTest.assertInvariants(actor);
                assertAllHaveAdvancedTo(when, universe);
                assertThat(universe, contains(actor));
                assertThat("did not process signal", actor.getSignalsToReceive(), contains(signal));
                assertThat(affectedActors, notNullValue());
                ActorTest.AffectedActorsTest.assertInvariants(affectedActors);
                assertThat(affectedActors, is(Actor.AffectedActors.emptyInstance()));
            }
        }
    }
}
