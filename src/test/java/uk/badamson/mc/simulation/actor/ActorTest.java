package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2021-22.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uk.badamson.dbc.assertions.EqualsSemanticsVerifier;
import uk.badamson.dbc.assertions.ObjectVerifier;
import uk.badamson.dbc.assertions.ThreadSafetyTest;
import uk.badamson.mc.history.ValueHistoryTest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ActorTest {

    static final Duration WHEN_A = Duration.ofMillis(0);

    static final Duration WHEN_B = Duration.ofMillis(5000);

    static final Duration WHEN_C = Duration.ofMillis(7000);

    static final Medium MEDIUM_A = new Medium();

    static final Medium MEDIUM_B = new Medium();

    public static <STATE> void assertInvariants(@Nonnull final Actor<STATE> actor) {
        ObjectVerifier.assertInvariants(actor);// inherited

        final var events = actor.getEvents();
        final var lastEvent = actor.getLastEvent();
        final var start = actor.getStart();
        final var stateHistory = actor.getStateHistory();
        final var signalsToReceive = actor.getSignalsToReceive();
        final var whenReceiveNextSignal = actor.getWhenReceiveNextSignal();

        assertAll(() -> assertThat("events", events, notNullValue()),
                () -> assertThat("start", start, notNullValue()),
                () -> assertThat("stateHistory", stateHistory, notNullValue()),
                () -> assertThat("signalsToReceive", signalsToReceive, notNullValue()),
                () -> assertThat("whenReceiveNextSignal", whenReceiveNextSignal, notNullValue())
        );
        ValueHistoryTest.assertInvariants(stateHistory);

        assertAll(() -> assertAll("events", createEventsAssertions(actor)),
                () -> assertAll("signalsToReceive", createSignalsToReceiveAssertions(actor)),
                () -> assertAll("lastEvent",
                        () -> assertThat("is null if, and only if, the sequence of events is empty.",
                                lastEvent == null == events.isEmpty()),
                        () -> assertThat("is either null or is the  last of the sequence of events.",
                                lastEvent == null || lastEvent == events.last())
                ),
                () -> assertAll("stateHistory",
                        () -> assertThat("firstTransitionTime", stateHistory.getFirstTransitionTime(), is(start)),
                        () -> assertThat("firstValue", stateHistory.getFirstValue(), nullValue()),
                        () -> assertThat("empty", stateHistory.isEmpty(), is(false))
                ),
                () -> assertAll("whenReceiveNextSignal",
                        () -> assertThat("after start", whenReceiveNextSignal, greaterThan(start)),
                        () -> assertThat("NEVER_RECEIVED if no signals to receive", signalsToReceive.isEmpty() &&
                                !Signal.NEVER_RECEIVED.equals(whenReceiveNextSignal), is(false))
                )
        );
    }

    public static <STATE> void assertInvariants(@Nonnull final Actor<STATE> actor1,
                                                @Nonnull final Actor<STATE> actor2) {
        ObjectVerifier.assertInvariants(actor1, actor2);// inherited
    }

    private static <STATE> void constructor(@Nonnull final Duration start,
                                            @Nonnull final STATE state) {
        final var actor = new Actor<>(start, state);

        assertInvariants(actor);
        final var stateTransitions = actor.getStateHistory().getTransitions();
        assertAll(
                () -> assertThat("start", actor.getStart(), sameInstance(start)),
                () -> assertThat("stateTransitions", stateTransitions, is(Map.of(start, state))),
                () -> assertThat("stateTransitions.firstKey", stateTransitions.firstKey(), sameInstance(actor.getStart())),
                () -> assertThat("events", actor.getEvents(), empty()));

    }

    private static <STATE> Stream<Executable> createEventsAssertions(@Nonnull final Actor<STATE> actor) {
        return actor.getEvents().stream().map(event -> () -> {
            assertThat(event, notNullValue());
            assertAll("event " + event,
                    () -> EventTest.assertInvariants(event),
                    () -> assertThat("when", event.getWhen(), greaterThan(actor.getStart())),
                    () -> assertThat("affectedObject", event.getAffectedObject(), sameInstance(actor)),
                    () -> assertThat("state is in stateHistory", event.getState(), is(actor.getStateHistory().get(event.getWhen())))
            );
        });
    }

    private static <STATE> Stream<Executable> createSignalsToReceiveAssertions(@Nonnull final Actor<STATE> actor) {
        return actor.getSignalsToReceive().stream().map(signal -> () -> {
            assertThat("signal", signal, notNullValue());
            assertAll("event " + signal,
                    () -> SignalTest.assertInvariants(signal),
                    () -> assertThat("whenSent", signal.getWhenSent(), greaterThanOrEqualTo(actor.getStart())),
                    () -> assertThat("receiver", signal.getReceiver(), sameInstance(actor))
            );
        });
    }

    private static <STATE> void addSignalToReceive(@Nonnull final Actor<STATE> actor, @Nonnull final Signal<STATE> signal) {
        actor.addSignalToReceive(signal);

        assertInvariants(actor);
        SignalTest.assertInvariants(signal);
    }

    @Nonnull
    private static <STATE> Actor.AffectedActors<STATE> receiveSignal(@Nonnull final Actor<STATE> actor) {
        final Actor.AffectedActors<STATE> affectedActors = actor.receiveSignal();

        assertInvariants(actor);
        assertThat(affectedActors, notNullValue());
        AffectedActorsTest.assertInvariants(affectedActors);
        return affectedActors;
    }

    private static <STATE> void addEvent(@Nonnull final Actor<STATE> actor, @Nonnull final Event<STATE> event) {
        final int size0 = actor.getEvents().size();

        actor.addEvent(event);

        assertInvariants(actor);
        assertThat(actor.getEvents(), hasSize(size0 + 1));
        assertThat(actor.getLastEvent(), sameInstance(event));
    }


    private static <STATE> void clearEventsBefore(@Nonnull final Actor<STATE> actor, @Nonnull final Duration when) {
        actor.clearEventsBefore(when);

        assertInvariants(actor);
    }

    @Test
    public void concurrentlyReceiveSignals() {
        final Actor<Integer> sender = new Actor<>(WHEN_A, 0);
        final Actor<Integer> receiver = new Actor<>(WHEN_B, 1);
        final int nSignals = 32;
        final CountDownLatch ready = new CountDownLatch(1);
        final List<Signal<Integer>> signals = new ArrayList<>(nSignals);
        final List<Future<Void>> futures = new ArrayList<>(nSignals);
        for (int s = 0; s < nSignals; s++) {
            final Signal<Integer> signal = new SignalTest.SimpleTestSignal(WHEN_C.plusSeconds(s), sender, receiver, MEDIUM_A);
            signals.add(signal);
            receiver.addSignalToReceive(signal);
            futures.add(ThreadSafetyTest.runInOtherThread(ready, receiver::receiveSignal));
        }

        ready.countDown();
        ThreadSafetyTest.get(futures);

        assertInvariants(receiver);
        for (final var signal : signals) {
            SignalTest.assertInvariants(signal);
        }
    }

    public static class AffectedActorsTest {

        public static <STATE> void assertInvariants(@Nonnull final Actor.AffectedActors<STATE> affected) {
            final Set<Actor<STATE>> added = affected.getAdded();
            final Set<Actor<STATE>> changed = affected.getChanged();
            final Set<Actor<STATE>> removed = affected.getRemoved();
            assertAll(
                    () -> assertThat(added, notNullValue()),
                    () -> assertThat(changed, notNullValue()),
                    () -> assertThat(removed, notNullValue()));
            assertAll(added.stream().map(actor -> () -> {
                assertThat(actor, notNullValue());
                ActorTest.assertInvariants(actor);
                assertThat("added and changed are distinct", changed, not(hasItem(actor)));
                assertThat("added and removed are distinct", removed, not(hasItem(actor)));
            }));
            assertAll(changed.stream().map(actor -> () -> {
                assertThat(actor, notNullValue());
                ActorTest.assertInvariants(actor);
                assertThat("changed and removed are distinct", removed, not(hasItem(actor)));
            }));
            assertAll(removed.stream().map(actor -> () -> {
                assertThat(actor, notNullValue());
                ActorTest.assertInvariants(actor);
            }));
            assertThat(affected.isEmpty(), is(added.isEmpty() && changed.isEmpty() && removed.isEmpty()));
        }

        public static <STATE> void assertInvariants(
                @Nonnull final Actor.AffectedActors<STATE> affected1,
                @Nonnull final Actor.AffectedActors<STATE> affected2
        ) {
            ObjectVerifier.assertInvariants(affected1, affected2);
            EqualsSemanticsVerifier.assertValueSemantics(
                    affected1, affected2, "added", Actor.AffectedActors::getAdded
            );
            EqualsSemanticsVerifier.assertValueSemantics(
                    affected1, affected2, "changed", Actor.AffectedActors::getChanged
            );
            EqualsSemanticsVerifier.assertValueSemantics(
                    affected1, affected2, "removed", Actor.AffectedActors::getRemoved
            );
        }

        private static <STATE> void constructor(
                @Nonnull final Set<Actor<STATE>> changed,
                @Nonnull final Set<Actor<STATE>> added,
                @Nonnull final Set<Actor<STATE>> removed) {
            final var affected = new Actor.AffectedActors<>(changed, added, removed);

            assertInvariants(affected);
            assertThat(affected.getChanged(), is(changed));
            assertThat(affected.getAdded(), is(added));
            assertThat(affected.getRemoved(), is(removed));
        }

        @Nonnull
        private static <STATE> Actor.AffectedActors<STATE> plus(
                @Nonnull final Actor.AffectedActors<STATE> affectedA,
                @Nonnull final Actor.AffectedActors<STATE> affectedB) {
            final Actor.AffectedActors<STATE> resultAB = affectedA.plus(affectedB);
            final Actor.AffectedActors<STATE> resultBA = affectedB.plus(affectedA);

            assertAll(
                    () -> assertThat(resultAB, notNullValue()),
                    () -> assertThat(resultBA, notNullValue()));
            assertAll(
                    () -> assertInvariants(affectedA),
                    () -> assertInvariants(affectedB),
                    () -> assertInvariants(resultAB),
                    () -> assertInvariants(resultBA));
            assertAll(
                    () -> assertInvariants(affectedA, affectedB),
                    () -> assertInvariants(affectedA, resultAB),
                    () -> assertInvariants(affectedA, resultBA),
                    () -> assertInvariants(affectedB, resultAB),
                    () -> assertInvariants(affectedB, resultBA),
                    () -> assertInvariants(resultAB, resultBA));
            assertThat("symmetric", resultAB, is(resultBA));

            return resultAB;
        }

        @Nested
        public class One {
            @Test
            public void emptySets() {
                constructor(Set.of(), Set.of(), Set.of());
            }

            @Test
            public void hasChanged() {
                final Actor<Integer> actor = new Actor<>(WHEN_A, 1);
                constructor(Set.of(actor), Set.of(), Set.of());
            }

            @Test
            public void hasAdded() {
                final Actor<Integer> actor = new Actor<>(WHEN_A, 1);
                constructor(Set.of(), Set.of(actor), Set.of());
            }

            @Test
            public void hasRemoved() {
                final Actor<Integer> actor = new Actor<>(WHEN_A, 1);
                constructor(Set.of(), Set.of(), Set.of(actor));
            }

        }

        @Nested
        public class Two {

            @Test
            public void differentChanged() {
                final Actor<Integer> actorA = new Actor<>(WHEN_A, 1);
                final Actor<Integer> actorB = new Actor<>(WHEN_B, 2);
                testDifferent(
                        Set.of(actorA), Set.of(), Set.of(),
                        Set.of(actorB), Set.of(), Set.of()
                );
            }

            @Test
            public void differentAdded() {
                final Actor<Integer> actorA = new Actor<>(WHEN_A, 1);
                final Actor<Integer> actorB = new Actor<>(WHEN_B, 2);
                testDifferent(
                        Set.of(), Set.of(actorA), Set.of(),
                        Set.of(), Set.of(actorB), Set.of()
                );
            }

            @Test
            public void differentRemoved() {
                final Actor<Integer> actorA = new Actor<>(WHEN_A, 1);
                final Actor<Integer> actorB = new Actor<>(WHEN_B, 2);
                testDifferent(
                        Set.of(), Set.of(), Set.of(actorA),
                        Set.of(), Set.of(), Set.of(actorB)
                );
            }

            @Test
            public void equivalentEmpty() {
                testEquivalent(
                        Set.of(), Set.of(), Set.of(),
                        Set.of(), Set.of(), Set.of()
                );
            }

            @Test
            public void equivalentNotEmpty() {
                final Actor<Integer> actorA = new Actor<>(WHEN_A, 1);
                final Actor<Integer> actorB = new Actor<>(WHEN_B, 2);
                final Actor<Integer> actorC = new Actor<>(WHEN_C, 3);
                testEquivalent(
                        Set.of(actorA), Set.of(actorB), Set.of(actorC),
                        Set.of(actorA), Set.of(actorB), Set.of(actorC)
                );
            }

            private <STATE> void testDifferent(
                    @Nonnull final Set<Actor<STATE>> changedA,
                    @Nonnull final Set<Actor<STATE>> addedA,
                    @Nonnull final Set<Actor<STATE>> removedA,
                    @Nonnull final Set<Actor<STATE>> changedB,
                    @Nonnull final Set<Actor<STATE>> addedB,
                    @Nonnull final Set<Actor<STATE>> removedB) {
                final var affectedA = new Actor.AffectedActors<>(changedA, addedA, removedA);
                final var affectedB = new Actor.AffectedActors<>(changedB, addedB, removedB);

                assertInvariants(affectedA, affectedB);
                assertThat(affectedA, not(is(affectedB)));
            }

            private <STATE> void testEquivalent(
                    @Nonnull final Set<Actor<STATE>> changedA,
                    @Nonnull final Set<Actor<STATE>> addedA,
                    @Nonnull final Set<Actor<STATE>> removedA,
                    @Nonnull final Set<Actor<STATE>> changedB,
                    @Nonnull final Set<Actor<STATE>> addedB,
                    @Nonnull final Set<Actor<STATE>> removedB) {
                final var affectedA = new Actor.AffectedActors<>(changedA, addedA, removedA);
                final var affectedB = new Actor.AffectedActors<>(changedB, addedB, removedB);

                assertInvariants(affectedA, affectedB);
                assertThat(affectedA, is(affectedB));
            }
        }

        @Nested
        public class Plus {

            @Test
            public void bothEmpty() {
                final Actor.AffectedActors<Integer> empty = Actor.AffectedActors.emptyInstance();
                final var result = plus(empty, empty);
                assertThat(result.isEmpty(), is(true));
            }

            @Test
            public void differentChanged() {
                final var actorA = new Actor<>(WHEN_A, 1);
                final var actorB = new Actor<>(WHEN_B, 2);
                final var affectedA = new Actor.AffectedActors<>(Set.of(actorA), Set.of(), Set.of());
                final var affectedB = new Actor.AffectedActors<>(Set.of(actorB), Set.of(), Set.of());

                final var result = plus(affectedA, affectedB);

                assertThat(result, is(new Actor.AffectedActors<>(Set.of(actorA, actorB), Set.of(), Set.of())));
            }

            @Test
            public void differentAdded() {
                final var actorA = new Actor<>(WHEN_A, 1);
                final var actorB = new Actor<>(WHEN_B, 2);
                final var affectedA = new Actor.AffectedActors<>(Set.of(), Set.of(actorA), Set.of());
                final var affectedB = new Actor.AffectedActors<>(Set.of(), Set.of(actorB), Set.of());

                final var result = plus(affectedA, affectedB);

                assertThat(result, is(new Actor.AffectedActors<>(Set.of(), Set.of(actorA, actorB), Set.of())));
            }

            @Test
            public void differentRemoved() {
                final var actorA = new Actor<>(WHEN_A, 1);
                final var actorB = new Actor<>(WHEN_B, 2);
                final var affectedA = new Actor.AffectedActors<>(Set.of(), Set.of(), Set.of(actorA));
                final var affectedB = new Actor.AffectedActors<>(Set.of(), Set.of(), Set.of(actorB));

                final var result = plus(affectedA, affectedB);

                assertThat(result, is(new Actor.AffectedActors<>(Set.of(), Set.of(), Set.of(actorA, actorB))));
            }

            @Test
            public void addedAndChanged() {
                final var actor = new Actor<>(WHEN_A, 1);
                final var affectedA = new Actor.AffectedActors<>(Set.of(actor), Set.of(), Set.of());
                final var affectedB = new Actor.AffectedActors<>(Set.of(), Set.of(actor), Set.of());

                final var result = plus(affectedA, affectedB);

                assertThat(
                        "treat as added", result,
                        is(new Actor.AffectedActors<>(Set.of(), Set.of(actor), Set.of())));
            }

            @Test
            public void addedAndRemoved() {
                final var actor = new Actor<>(WHEN_A, 1);
                final var affectedA = new Actor.AffectedActors<>(Set.of(), Set.of(actor), Set.of());
                final var affectedB = new Actor.AffectedActors<>(Set.of(), Set.of(), Set.of(actor));

                final var result = plus(affectedA, affectedB);

                assertThat(
                        "treat as no-op", result,
                        is(new Actor.AffectedActors<>(Set.of(), Set.of(), Set.of())));
            }

            @Test
            public void changedAndRemoved() {
                final var actor = new Actor<>(WHEN_A, 1);
                final var affectedA = new Actor.AffectedActors<>(Set.of(actor), Set.of(), Set.of());
                final var affectedB = new Actor.AffectedActors<>(Set.of(), Set.of(), Set.of(actor));

                final var result = plus(affectedA, affectedB);

                assertThat(
                        "treat as changed", result,
                        is(new Actor.AffectedActors<>(Set.of(), Set.of(), Set.of(actor))));
            }

            @Nested
            public class OneEmpty {

                @Test
                public void changed() {
                    test(Set.of(new Actor<>(WHEN_A, 1)), Set.of(), Set.of());
                }

                @Test
                public void added() {
                    test(Set.of(), Set.of(new Actor<>(WHEN_A, 1)), Set.of());
                }

                @Test
                public void removed() {
                    test(Set.of(), Set.of(), Set.of(new Actor<>(WHEN_A, 1)));
                }

                private void test(@Nonnull final Set<Actor<Integer>> changed, @Nonnull final Set<Actor<Integer>> added, @Nonnull final Set<Actor<Integer>> removed) {
                    final Actor.AffectedActors<Integer> empty = Actor.AffectedActors.emptyInstance();
                    final Actor.AffectedActors<Integer> notEmpty = new Actor.AffectedActors<>(changed, added, removed);

                    final var result = plus(empty, notEmpty);

                    assertThat(result, is(notEmpty));
                }
            }
        }
    }

    @Immutable
    static final class NeighbourActorState {
        @Nullable
        private final Actor<NeighbourActorState> neighbour;

        NeighbourActorState(@Nullable final Actor<NeighbourActorState> neighbour) {
            this.neighbour = neighbour;
        }

        @Nullable
        public Actor<NeighbourActorState> getNeighbour() {
            return neighbour;
        }
    }

    @Immutable
    static final class NeighbourSignal extends Signal<NeighbourActorState> {
        public NeighbourSignal(
                @Nonnull final Duration whenSent,
                @Nullable final Actor<NeighbourActorState> sender,
                @Nonnull final Actor<NeighbourActorState> receiver) {
            super(whenSent, sender, receiver, MEDIUM_A);
        }

        @Nonnull
        @Override
        protected Duration getPropagationDelay(@Nonnull final NeighbourActorState receiverState) {
            return Duration.ofSeconds(1);
        }

        @Nonnull
        @Override
        protected Event<NeighbourActorState> receive(
                @Nonnull final Duration when,
                @Nonnull final NeighbourActorState receiverState) throws UnreceivableSignalException {
            final Actor<NeighbourActorState> neighbour = receiverState.getNeighbour();
            final Set<Signal<NeighbourActorState>> signalsEmitted;
            if (neighbour == null) {
                signalsEmitted = Set.of();
            } else {
                signalsEmitted = Set.of(new NeighbourSignal(when, getReceiver(), neighbour));
            }
            return new Event<>(this, when, receiverState, signalsEmitted, Set.of());
        }
    }

    @Nested
    public class Constructor {

        @Nested
        public class InitialState {

            @Test
            public void a() {
                constructor(WHEN_A, 0);
            }

            @Test
            public void b() {
                constructor(WHEN_B, 1);
            }

        }

    }

    @Nested
    public class AddSignalToReceive {

        @Nested
        public class First {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, 0);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, 1);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent, @Nonnull final Integer state0) {
                final var sender = new Actor<>(start, 0);
                final var receiver = new Actor<>(start, state0);
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(whenSent, sender, receiver, MEDIUM_A);

                addSignalToReceive(receiver, signal);

                assertThat("added signal", receiver.getSignalsToReceive(), hasItem(signal));
            }

        }

        @Nested
        public class AlreadyReceived {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, 0);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, 1);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent, @Nonnull final Integer state0) {
                final var sender = new Actor<>(start, 0);
                final var receiver = new Actor<>(start, state0);
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(whenSent, sender, receiver, MEDIUM_A);
                receiver.addSignalToReceive(signal);
                receiver.receiveSignal();

                addSignalToReceive(receiver, signal);

                assertThat("receiver events", receiver.getEvents(), hasSize(1));
            }
        }
    }

    @Nested
    public class ReceiveSignal {

        @Test
        public void none() {
            final var actor = new Actor<>(WHEN_A, 0);

            final var affectedActors = receiveSignal(actor);

            assertThat(actor.getEvents(), empty());
            assertThat(affectedActors.getAdded(), empty());
            assertThat(affectedActors.getChanged(), empty());
            assertThat(affectedActors.getRemoved(), empty());
        }

        @Test
        public void concurrent() {
            final int nActors = 16;
            final List<Actor<Integer>> actors = new ArrayList<>(nActors);
            for (int a = 0; a < nActors; a++) {
                actors.add(new Actor<>(WHEN_A, a));
            }
            for (int s = 0; s < nActors; s++) {
                final var sender = actors.get(s);
                for (int r = 0; r < nActors; r++) {
                    if (s == r) continue;
                    final var receiver = actors.get(r);
                    final var signal = new SignalTest.EchoingTestSignal(WHEN_B, sender, receiver, MEDIUM_A);
                    receiver.addSignalToReceive(signal);
                }
            }
            final CountDownLatch ready = new CountDownLatch(1);
            final List<Future<Void>> futures = new ArrayList<>(nActors);
            for (final var actor : actors) {
                futures.add(ThreadSafetyTest.runInOtherThread(ready, actor::receiveSignal));
            }

            ready.countDown();
            ThreadSafetyTest.get(futures);

            for (final var actor : actors) {
                assertInvariants(actor);
            }
        }

        @Nested
        public class First {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, 0);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, 1);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent, @Nonnull final Integer state0) {
                final var sender = new Actor<>(start, 0);
                final var receiver = new Actor<>(start, state0);
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(whenSent, sender, receiver, MEDIUM_A);
                receiver.addSignalToReceive(signal);

                final var affectedActors = receiveSignal(receiver);

                final var events = receiver.getEvents();
                assertThat("events", events, hasSize(1));
                final var event = events.first();
                assertThat("event causing signal", event.getCausingSignal(), sameInstance(signal));
                assertThat("event state resulted from receiving the signal", event, is(signal.receive(state0)));
                assertThat("receiver events", receiver.getEvents(), is(Set.of(event)));
                assertThat(affectedActors.getAdded(), empty());
                assertThat(affectedActors.getRemoved(), empty());
                assertThat(affectedActors.getChanged(), contains(receiver));
            }
        }

        @Nested
        public class Invalidating {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, 0);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, 1);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent1, @Nonnull final Integer state0) {
                final var sender = new Actor<>(start, 0);
                final var receiver = new Actor<>(start, state0);
                final Signal<Integer> signal1 = new SignalTest.SimpleTestSignal(whenSent1, sender, receiver, MEDIUM_A);
                final Duration whenSent2 = signal1.getWhenReceived(state0);
                final Signal<Integer> signal2 = new SignalTest.SimpleTestSignal(whenSent2, sender, receiver, MEDIUM_B);
                receiver.addSignalToReceive(signal2);
                receiver.receiveSignal();
                receiver.addSignalToReceive(signal1);
                receiver.receiveSignal();

                final var affectedActors = receiveSignal(receiver);

                final var events = receiver.getEvents();
                assertThat("events", events, hasSize(2));
                final var event1 = events.first();
                final var event2 = events.last();
                final Integer state1 = event1.getState();
                assertThat("event 1 resulted from receiving signal 1", event1, is(signal1.receive(state0)));
                assertThat("event 1 result state", state1, notNullValue());
                assertAll(
                        () -> assertThat("event 2 resulted from receiving signal 2", event2, is(signal2.receive(state1))),
                        () -> assertThat("added", affectedActors.getAdded(), empty()),
                        () -> assertThat("empty", affectedActors.getRemoved(), empty()),
                        () -> assertThat("changed", affectedActors.getChanged(), contains(receiver)));
            }
        }

        @Nested
        public class AddEvent {

            @Test
            public void continuation() {
                test(WHEN_A, WHEN_B, 1, 2);
            }

            @Test
            public void destruction() {
                test(WHEN_B, WHEN_C, 3, null);
            }

            private void test(
                    @Nonnull final Duration start, @Nonnull final Duration when,
                    @Nonnull final Integer state0, @Nullable final Integer state) {
                final var actor = new Actor<>(start, state0);
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(start, actor, actor, MEDIUM_A);
                final Event<Integer> event = new Event<>(signal, when, state);

                addEvent(actor, event);
            }
        }

        @Nested
        public class InvalidatingEmittedSignal {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, 0);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, 1);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent1, @Nonnull final Integer state0) {
                final var actor1 = new Actor<>(start, 0);
                final var actor2 = new Actor<>(start, state0);
                final Signal<Integer> signal1 = new SignalTest.SimpleTestSignal(whenSent1, actor1, actor2, MEDIUM_A);
                final Duration whenSent2 = signal1.getWhenReceived(state0);
                final Signal<Integer> signal2 = new SignalTest.EchoingTestSignal(whenSent2, actor1, actor2, MEDIUM_B);
                actor2.addSignalToReceive(signal2);
                actor2.receiveSignal();
                actor2.addSignalToReceive(signal1);
                actor2.receiveSignal();

                final var affectedActors = receiveSignal(actor2);

                final var events = actor2.getEvents();
                assertThat("events", events, hasSize(2));
                final var event1 = events.first();
                final var event2 = events.last();
                final Integer state1 = event1.getState();
                assertThat("event 1 result state", state1, notNullValue());
                assertAll(
                        () -> assertThat("event 1 resulted from receiving signal 1", event1, is(signal1.receive(state0))),
                        () -> assertThat("event 2 resulted from receiving signal 2", event2, is(signal2.receive(state1))),
                        () -> assertThat("added", affectedActors.getAdded(), empty()),
                        () -> assertThat("removed", affectedActors.getRemoved(), empty()),
                        () -> assertThat("changed", affectedActors.getChanged(), containsInAnyOrder(actor1, actor2)));
            }
        }

        @Nested
        public class InvalidatingCreatedActor {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, 0);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, 1);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent1, @Nonnull final Integer state0) {
                final var actor1 = new Actor<>(start, 0);
                final var actor2 = new Actor<>(start, state0);
                final Signal<Integer> signal1 = new SignalTest.SimpleTestSignal(whenSent1, actor1, actor2, MEDIUM_A);
                final Duration whenSent2 = signal1.getWhenReceived(state0);
                final Signal<Integer> signal2 = new SignalTest.ActorCreatingTestSignal(whenSent2, actor1, actor2, MEDIUM_B);
                actor2.addSignalToReceive(signal2);
                final var affectedActors2 = actor2.receiveSignal();
                assert affectedActors2.getAdded().size() == 1;
                final var addedActor = affectedActors2.getAdded().iterator().next();
                actor2.addSignalToReceive(signal1);

                final var affectedActors1 = receiveSignal(actor2);

                final var events = actor2.getEvents();
                assertThat(events, hasSize(1));
                assertAll(
                        () -> assertThat("event 1 resulted from receiving signal 1",
                                events.first(), is(signal1.receive(state0))),
                        () -> assertThat("added", affectedActors1.getAdded(), empty()),
                        () -> assertThat("changed", affectedActors1.getChanged(), contains(actor2)),
                        () -> assertThat("removed", affectedActors1.getRemoved(), contains(addedActor)));
            }
        }

        @Nested
        public class EmittingSignalToSelf {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, 0);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, 1);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent1, @Nonnull final Integer state0) {
                final var actor = new Actor<>(start, state0);
                final Signal<Integer> signal = new SignalTest.StrobingTestSignal(whenSent1, actor, actor, MEDIUM_A);
                actor.addSignalToReceive(signal);
                final var whenReceiveNextSignal0 = actor.getWhenReceiveNextSignal();

                final var affectedActors = receiveSignal(actor);

                assertThat("Has another signal to receive", actor.getSignalsToReceive(), hasSize(1));
                assertThat("State transition is when the signal was received",
                        actor.getStateHistory().getLastTransitionTime(), is(whenReceiveNextSignal0));
                assertThat(affectedActors.getAdded(), empty());
                assertThat(affectedActors.getRemoved(), empty());
                assertThat(affectedActors.getChanged(), contains(actor));
            }
        }

        @Nested
        public class EchoingSignal {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, 0);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, 1);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent1, @Nonnull final Integer state0) {
                final var actorA = new Actor<>(start, state0);
                final var actorB = new Actor<>(start, state0);
                final Signal<Integer> signal1 = new SignalTest.EchoingTestSignal(whenSent1, actorA, actorB, MEDIUM_A);
                actorB.addSignalToReceive(signal1);

                final var affectedActors = receiveSignal(actorB);

                assertInvariants(actorA);
                assertThat("Original sender has another signal to receive", actorA.getSignalsToReceive(), hasSize(1));
                assertThat("Original receiver does not have another signal to receive", actorB.getSignalsToReceive(), empty());
                assertThat(affectedActors.getAdded(), empty());
                assertThat(affectedActors.getRemoved(), empty());
                assertThat(affectedActors.getChanged(), containsInAnyOrder(actorA, actorB));
            }
        }

        @Nested
        public class ActorCreatingSignal {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, 1);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, 2);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent1, @Nonnull final Integer state0) {
                final var actorA = new Actor<>(start, state0);
                final var actorB = new Actor<>(start, state0);
                final Signal<Integer> signal = new SignalTest.ActorCreatingTestSignal(whenSent1, actorA, actorB, MEDIUM_A);
                actorB.addSignalToReceive(signal);

                final var affectedActors = receiveSignal(actorB);

                assertInvariants(actorA);
                assertThat(affectedActors.getRemoved(), empty());
                assertThat(affectedActors.getChanged(), contains(actorB));
                final var added = affectedActors.getAdded();
                assertThat("created a new actor", added, Matchers.<Collection<Actor<Integer>>>allOf(
                        hasSize(1), not(hasItem(actorA)), not(hasItem(actorB))
                ));
            }
        }
    }

    @Nested
    public class ClearEventsBefore {

        @Nested
        public class NoEvents {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C);
            }

            @Test
            public void atStart() {
                test(WHEN_A, WHEN_A);
            }

            @Test
            public void beforeStart() {
                test(WHEN_C, WHEN_B);
            }

            private void test(@Nonnull final Duration start0, @Nonnull final Duration when) {
                final var actor = new Actor<>(start0, 1);

                clearEventsBefore(actor, when);

                assertAll("No-op",
                        () -> assertThat("start", actor.getStart(), sameInstance(start0)),
                        () -> assertThat("events", actor.getEvents(), empty()));
            }

        }

        @Nested
        public class EventAfter {

            @Test
            public void near() {
                test(WHEN_A, Duration.ofNanos(1), 1, 2);
            }

            @Test
            public void far() {
                test(WHEN_B, Duration.ofDays(365), 3, 4);
            }

            private void test(
                    @Nonnull final Duration start, @Nonnull final Duration margin,
                    @Nonnull final Integer state0, @Nullable final Integer eventState) {
                final var actor = new Actor<>(start, state0);
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(start, actor, actor, MEDIUM_A);
                final var whenEvent = signal.getWhenReceived(state0);
                final var when = whenEvent.minus(margin);
                final var event = new Event<>(signal, whenEvent, eventState);
                actor.addEvent(event);

                clearEventsBefore(actor, when);

                assertAll("No-op",
                        () -> assertThat("start", actor.getStart(), sameInstance(start)),
                        () -> assertThat("events", actor.getEvents(), contains(event)));
            }
        }

        @Nested
        public class EventBefore {

            @Test
            public void near() {
                test(WHEN_A, Duration.ofNanos(1), 1, 2);
            }

            @Test
            public void far() {
                test(WHEN_B, Duration.ofDays(365), 3, 4);
            }

            private void test(
                    @Nonnull final Duration start, @Nonnull final Duration margin,
                    @Nonnull final Integer state0, @Nullable final Integer eventState) {
                final var actor = new Actor<>(start, state0);
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(start, actor, actor, MEDIUM_A);
                final var whenEvent = signal.getWhenReceived(state0);
                final var when = whenEvent.plus(margin);
                final var event = new Event<>(signal, whenEvent, eventState);
                actor.addEvent(event);

                clearEventsBefore(actor, when);

                final var stateHistory = actor.getStateHistory();
                assertAll(
                        () -> assertThat("start", actor.getStart(), sameInstance(whenEvent)),
                        () -> assertThat("events", actor.getEvents(), empty()),
                        () -> assertThat("stateHistory transitionTimes", stateHistory.getTransitionTimes(), contains(whenEvent)),
                        () -> assertThat("stateHistory value", stateHistory.get(whenEvent), is(eventState)));
            }
        }


    }

}
