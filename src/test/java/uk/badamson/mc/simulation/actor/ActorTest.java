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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uk.badamson.dbc.assertions.ObjectVerifier;
import uk.badamson.dbc.assertions.ThreadSafetyTest;
import uk.badamson.mc.history.ValueHistoryTest;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ActorTest {

    static final Duration WHEN_A = Duration.ofMillis(0);

    static final Duration WHEN_B = Duration.ofMillis(5000);

    static final Duration WHEN_C = Duration.ofMillis(7000);

    static final Duration WHEN_D = Duration.ofMillis(11000);

    public static <STATE> void assertInvariants(@Nonnull final Actor<STATE> actor) {
        ObjectVerifier.assertInvariants(actor);// inherited

        final var events = actor.getEvents();
        final var lastEvent = actor.getLastEvent();
        final var start = actor.getStart();
        final var stateHistory = actor.getStateHistory();
        final var signalsToReceive = actor.getSignalsToReceive();
        final var whenReceiveNextSignal = actor.getWhenReceiveNextSignal();

        assertAll("Not null", () -> assertNotNull(events, "events"), // guard
                () -> assertNotNull(start, "start"), // guard
                () -> assertNotNull(stateHistory, "stateHistory"), // guard
                () -> assertNotNull(signalsToReceive, "signalsToReceive"), // guard
                () -> assertNotNull(whenReceiveNextSignal, "whenReceiveNextSignal")
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
                () -> assertAll("stateHistory", () -> assertSame(start, stateHistory.getFirstTransitionTime(),
                                "The first transition time of the state history is the same as the start time of this history."),
                        () -> assertNull(stateHistory.getFirstValue(),
                                "The state at the start of time of the state history is null."),
                        () -> assertFalse(stateHistory.isEmpty(), "The state history is never empty.")
                ),
                () -> assertAll("whenReceiveNextSignal",
                        () -> assertThat("after start", whenReceiveNextSignal, greaterThan(start)),
                        () -> assertFalse(signalsToReceive.isEmpty() &&
                                !Signal.NEVER_RECEIVED.equals(whenReceiveNextSignal), "NEVER_RECEIVED if no signals to receive")
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
                () -> assertSame(start, actor.getStart(), "start"),
                () -> assertSame(stateTransitions.firstKey(), actor.getStart(), "start"),
                () -> assertEquals(stateTransitions, Map.of(start, state), "stateTransitions"),
                () -> assertThat("events", actor.getEvents(), empty()));

    }

    private static <STATE> Stream<Executable> createEventsAssertions(@Nonnull final Actor<STATE> actor) {
        return actor.getEvents().stream().map(event -> () -> {
            assertNotNull(event, "event");// guard
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
            assertNotNull(signal, "event");// guard
            assertAll("event " + signal,
                    () -> SignalTest.assertInvariants(signal),
                    () -> assertThat("whenSent", signal.getWhenSent(), greaterThan(actor.getStart())),
                    () -> assertThat("receiver", signal.getReceiver(), sameInstance(actor))
            );
        });
    }

    private static <STATE> void addSignalToReceive(@Nonnull final Actor<STATE> actor, @Nonnull final Signal<STATE> signal) {
        actor.addSignalToReceive(signal);

        assertInvariants(actor);
        SignalTest.assertInvariants(signal);
    }

    private static <STATE> Event<STATE> receiveSignal(@Nonnull final Actor<STATE> actor) {
        final Event<STATE> event = actor.receiveSignal();

        assertInvariants(actor);
        if (event != null) {
            EventTest.assertInvariants(event);
            assertThat("the affected object of the event is this", event.getAffectedObject(), sameInstance(actor));
        }
        return event;
    }

    private static <STATE> void removeSignal(@Nonnull final Actor<STATE> actor, @Nonnull final Signal<STATE> signal) {
        actor.removeSignal(signal);

        assertInvariants(actor);
        SignalTest.assertInvariants(signal);
        assertThat("The signal is not one of of the signals to receive.", actor.getSignalsToReceive(), not(hasItem(signal)));
        assertAll("None of the events have the signal as their causing signal.",
                actor.getEvents().stream().map(Event::getCausingSignal).map(eventSignal -> () -> assertThat(eventSignal, not(sameInstance(signal))))
        );
    }

    @Test
    public void concurrentlyRemoveAndReceiveSignals() {
        final Actor<Integer> sender = new Actor<>(WHEN_A, 0);
        final Actor<Integer> receiver = new Actor<>(WHEN_B, 1);
        final int nSignals = 32;
        final List<Signal<Integer>> signals = new ArrayList<>(nSignals);
        for (int s = 0; s < nSignals; s++) {
            final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, WHEN_C.plusSeconds(s), receiver);
            signals.add(signal);
        }
        final CountDownLatch ready = new CountDownLatch(1);
        final List<Future<Void>> futures = new ArrayList<>(nSignals * 2);
        for (final var signal : signals) {
            futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> receiver.removeSignal(signal)));
            futures.add(ThreadSafetyTest.runInOtherThread(ready, receiver::receiveSignal));
        }

        ready.countDown();
        ThreadSafetyTest.get(futures);

        assertInvariants(receiver);
        for (final var signal : signals) {
            SignalTest.assertInvariants(signal);
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

        }// class

    }// class

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
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, whenSent, receiver);

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
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, whenSent, receiver);
                receiver.addSignalToReceive(signal);
                receiver.receiveSignal();

                addSignalToReceive(receiver, signal);

                assertThat("receiver events", receiver.getEvents(), hasSize(1));
            }
        }
    }// class

    @Nested
    public class ReceiveSignal {

        @Test
        public void none() {
            final var actor = new Actor<>(WHEN_A, 0);

            final var event = receiveSignal(actor);

            assertThat("event", event, nullValue());
            assertThat("actor events", actor.getEvents(), empty());// guard
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
                    final var signal = new SignalTest.EchoingTestSignal(sender, WHEN_B, receiver);
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
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, whenSent, receiver);
                receiver.addSignalToReceive(signal);

                final Event<Integer> event = receiveSignal(receiver);

                assertThat("event", event, notNullValue());// guard
                assertThat("event causing signal", event.getCausingSignal(), sameInstance(signal));
                assertThat("event state resulted from receiving the signal", event, is(signal.receive(state0)));
                assertThat("receiver events", receiver.getEvents(), is(Set.of(event)));
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
                final Signal<Integer> signal1 = new SignalTest.SimpleTestSignal(sender, whenSent1, receiver);
                final Duration whenSent2 = signal1.getWhenReceived(state0);
                final Signal<Integer> signal2 = new SignalTest.SimpleTestSignal(sender, whenSent2, receiver);
                receiver.addSignalToReceive(signal2);
                receiver.receiveSignal();
                receiver.addSignalToReceive(signal1);
                receiver.receiveSignal();

                receiveSignal(receiver);

                final var events = receiver.getEvents();
                assertThat("events", events, hasSize(2));// guard
                final var event1 = events.first();
                final var event2 = events.last();
                assertThat("event 1 resulted from receiving signal 1", event1, is(signal1.receive(state0)));
                final Integer state1 = event1.getState();
                assertThat("event 1 result state", state1, notNullValue());// guard
                assertThat("event 2 resulted from receiving signal 2", event2, is(signal2.receive(state1)));
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
                final Signal<Integer> signal = new SignalTest.StrobingTestSignal(actor, whenSent1, actor);
                actor.addSignalToReceive(signal);
                final var whenReceiveNextSignal0 = actor.getWhenReceiveNextSignal();

                final Event<Integer> event = receiveSignal(actor);

                assertThat("event", event, notNullValue());// guard
                assertThat("event causing signal", event.getCausingSignal(), sameInstance(signal));// guard
                assertThat("Has another signal to receive", actor.getSignalsToReceive(), hasSize(1));
                assertThat("State transition is when the signal was received",
                        actor.getStateHistory().getLastTransitionTime(), is(whenReceiveNextSignal0));
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
                final Signal<Integer> signal1 = new SignalTest.EchoingTestSignal(actorA, whenSent1, actorB);
                actorB.addSignalToReceive(signal1);

                receiveSignal(actorB);

                assertInvariants(actorA);
                assertThat("Original sender has another signal to receive", actorA.getSignalsToReceive(), hasSize(1));
                assertThat("Original receiver does not have another signal to receive", actorB.getSignalsToReceive(), empty());
            }
        }


        @Nested
        public class AfterRemovingSignal {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, WHEN_C, 0, 1);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, WHEN_D, 1, 2);
            }

            private void test(@Nonnull final Duration when1, @Nonnull final Duration when2, @Nonnull final Duration when3, @Nonnull final Integer state1, @Nonnull final Integer state2) {
                final Actor<Integer> sender = new Actor<>(when1, state1);
                final Actor<Integer> receiver = new Actor<>(when2, state2);
                final Signal<Integer> signal1 = new SignalTest.SimpleTestSignal(sender, when3, receiver);
                receiver.addSignalToReceive(signal1);
                receiver.receiveSignal();
                final var event1 = receiver.getLastEvent();
                assert event1 != null;
                final Signal<Integer> signal2 = new SignalTest.SimpleTestSignal(sender, event1.getWhen(), receiver);
                receiver.addSignalToReceive(signal2);
                receiver.receiveSignal();
                final var event2 = receiver.getLastEvent();
                assert event2 != null;
                final Signal<Integer> signal3 = new SignalTest.SimpleTestSignal(sender, event2.getWhen(), receiver);
                receiver.addSignalToReceive(signal3);
                receiver.receiveSignal();
                receiver.getWhenReceiveNextSignal();// cause a value to be cached
                receiver.removeSignal(signal2);

                receiveSignal(receiver);

                assertThat("signals to receive", receiver.getSignalsToReceive(), empty());
                final SortedSet<Event<Integer>> events = receiver.getEvents();
                assertThat("events", events, hasSize(2));
                assertThat("event 1 causing signal", events.first().getCausingSignal(), sameInstance(signal1));
                assertThat("event 2 causing signal", events.last().getCausingSignal(), sameInstance(signal3));
            }
        }
    }// class

    @Nested
    public class RemoveSignal {

        @Test
        public void absent() {
            final Actor<Integer> sender = new Actor<>(WHEN_A, 0);
            final Actor<Integer> receiver = new Actor<>(WHEN_B, 1);
            final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, WHEN_C, receiver);

            removeSignal(receiver, signal);

            assertThat("No signals to receive (still)", receiver.getSignalsToReceive(), empty());
            assertThat("No events (still)", receiver.getEvents(), empty());
        }

        @Nested
        public class Unscheduled {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, WHEN_C, 0, 1);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, WHEN_D, 1, 2);
            }

            @Test
            public void concurrent() {
                final Actor<Integer> sender = new Actor<>(WHEN_A, 0);
                final Actor<Integer> receiver = new Actor<>(WHEN_B, 1);
                final int nSignals = 32;
                final List<Signal<Integer>> signals = new ArrayList<>(nSignals);
                for (int s = 0; s < nSignals; s++) {
                    final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, WHEN_C.plusSeconds(s), receiver);
                    signals.add(signal);
                    receiver.addSignalToReceive(signal);
                }
                final CountDownLatch ready = new CountDownLatch(1);
                final List<Future<Void>> futures = new ArrayList<>(nSignals);
                for (final var signal : signals) {
                    futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> receiver.removeSignal(signal)));
                }

                ready.countDown();
                ThreadSafetyTest.get(futures);

                assertInvariants(receiver);
                for (final var signal : signals) {
                    SignalTest.assertInvariants(signal);
                }
                assertThat("No signals to receive", receiver.getSignalsToReceive(), empty());
                assertThat("No events (still)", receiver.getEvents(), empty());
            }

            private void test(@Nonnull final Duration when1, @Nonnull final Duration when2, @Nonnull final Duration when3, @Nonnull final Integer state1, @Nonnull final Integer state2) {
                final Actor<Integer> sender = new Actor<>(when1, state1);
                final Actor<Integer> receiver = new Actor<>(when2, state2);
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, when3, receiver);
                receiver.addSignalToReceive(signal);

                removeSignal(receiver, signal);

                assertThat("No signals to receive", receiver.getSignalsToReceive(), empty());
                assertThat("No events (still)", receiver.getEvents(), empty());
                assertThat("whenReceiveNextSignal", receiver.getWhenReceiveNextSignal(), is(Signal.NEVER_RECEIVED));
            }
        }

        @Nested
        public class Scheduled {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, WHEN_C, 0, 1);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, WHEN_D, 1, 2);
            }

            @Test
            public void concurrent() {
                final Actor<Integer> sender = new Actor<>(WHEN_A, 0);
                final Actor<Integer> receiver = new Actor<>(WHEN_B, 1);
                final int nSignals = 32;
                final List<Signal<Integer>> signals = new ArrayList<>(nSignals);
                for (int s = 0; s < nSignals; s++) {
                    final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, WHEN_C.plusSeconds(s), receiver);
                    signals.add(signal);
                    receiver.addSignalToReceive(signal);
                }
                final CountDownLatch ready = new CountDownLatch(1);
                final List<Future<Void>> futures = new ArrayList<>(nSignals);
                for (final var signal : signals) {
                    futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> receiver.removeSignal(signal)));
                }
                receiver.getWhenReceiveNextSignal();

                ready.countDown();
                ThreadSafetyTest.get(futures);

                assertInvariants(receiver);
                for (final var signal : signals) {
                    SignalTest.assertInvariants(signal);
                }
                assertThat("No signals to receive", receiver.getSignalsToReceive(), empty());
                assertThat("No events (still)", receiver.getEvents(), empty());
            }

            private void test(@Nonnull final Duration when1, @Nonnull final Duration when2, @Nonnull final Duration when3, @Nonnull final Integer state1, @Nonnull final Integer state2) {
                final Actor<Integer> sender = new Actor<>(when1, state1);
                final Actor<Integer> receiver = new Actor<>(when2, state2);
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, when3, receiver);
                receiver.addSignalToReceive(signal);
                receiver.getWhenReceiveNextSignal();

                removeSignal(receiver, signal);

                assertThat("No signals to receive", receiver.getSignalsToReceive(), empty());
                assertThat("No events (still)", receiver.getEvents(), empty());
            }
        }

        @Nested
        public class Received {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, WHEN_C, 0, 1);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, WHEN_D, 1, 2);
            }

            @Test
            public void concurrent() {
                final Actor<Integer> sender = new Actor<>(WHEN_A, 0);
                final Actor<Integer> receiver = new Actor<>(WHEN_B, 1);
                final int nSignals = 32;
                final List<Signal<Integer>> signals = new ArrayList<>(nSignals);
                for (int s = 0; s < nSignals; s++) {
                    final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, WHEN_C.plusSeconds(s), receiver);
                    signals.add(signal);
                    receiver.addSignalToReceive(signal);
                }
                for (int s = 0; s < nSignals; s++) {
                    receiver.receiveSignal();
                }
                final CountDownLatch ready = new CountDownLatch(1);
                final List<Future<Void>> futures = new ArrayList<>(nSignals);
                for (final var signal : signals) {
                    futures.add(ThreadSafetyTest.runInOtherThread(ready, () -> receiver.removeSignal(signal)));
                }
                receiver.getWhenReceiveNextSignal();

                ready.countDown();
                ThreadSafetyTest.get(futures);

                assertInvariants(receiver);
                for (final var signal : signals) {
                    SignalTest.assertInvariants(signal);
                }
                assertThat("No signals to receive", receiver.getSignalsToReceive(), empty());
                assertThat("No events (still)", receiver.getEvents(), empty());
            }

            private void test(@Nonnull final Duration when1, @Nonnull final Duration when2, @Nonnull final Duration when3, @Nonnull final Integer state1, @Nonnull final Integer state2) {
                final Actor<Integer> sender = new Actor<>(when1, state1);
                final Actor<Integer> receiver = new Actor<>(when2, state2);
                final Signal<Integer> signal = new SignalTest.SimpleTestSignal(sender, when3, receiver);
                receiver.addSignalToReceive(signal);
                receiver.receiveSignal();

                removeSignal(receiver, signal);

                assertThat("No signals to receive", receiver.getSignalsToReceive(), empty());
                assertThat("No events", receiver.getEvents(), empty());
            }
        }

        @Nested
        public class SubsequentEvents {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, WHEN_C, 0, 1);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, WHEN_D, 1, 2);
            }

            private void test(@Nonnull final Duration when1, @Nonnull final Duration when2, @Nonnull final Duration when3, @Nonnull final Integer state1, @Nonnull final Integer state2) {
                final Actor<Integer> sender = new Actor<>(when1, state1);
                final Actor<Integer> receiver = new Actor<>(when2, state2);
                final Signal<Integer> signal1 = new SignalTest.SimpleTestSignal(sender, when3, receiver);
                receiver.addSignalToReceive(signal1);
                receiver.receiveSignal();
                final var event1 = receiver.getLastEvent();
                assert event1 != null;
                final Signal<Integer> signal2 = new SignalTest.SimpleTestSignal(sender, event1.getWhen(), receiver);
                receiver.addSignalToReceive(signal2);
                receiver.receiveSignal();
                final var event2 = receiver.getLastEvent();
                assert event2 != null;
                final Signal<Integer> signal3 = new SignalTest.SimpleTestSignal(sender, event2.getWhen(), receiver);
                receiver.addSignalToReceive(signal3);
                receiver.receiveSignal();

                removeSignal(receiver, signal2);

                assertThat("rescheduled subsequent signal", receiver.getSignalsToReceive(), is(Set.of(signal3)));
                assertThat("retains event due to previous signal", receiver.getEvents(), is(Set.of(event1)));
                assertThat("updated when receive next signal", receiver.getWhenReceiveNextSignal(), is(signal3.getWhenReceived(receiver.getStateHistory())));
            }
        }

        @Nested
        public class SubsequentEventsWithEmittedSignals {

            @Test
            public void a() {
                test(WHEN_A, WHEN_B, WHEN_C, 0, 1);
            }

            @Test
            public void b() {
                test(WHEN_B, WHEN_C, WHEN_D, 1, 2);
            }

            private void test(@Nonnull final Duration when1, @Nonnull final Duration when2, @Nonnull final Duration when3, @Nonnull final Integer state1, @Nonnull final Integer state2) {
                final Actor<Integer> actor1 = new Actor<>(when1, state1);
                final Actor<Integer> actor2 = new Actor<>(when2, state2);
                final Signal<Integer> signal1 = new SignalTest.SimpleTestSignal(actor1, when3, actor2);
                actor2.addSignalToReceive(signal1);
                actor2.receiveSignal();
                final var event1 = actor2.getLastEvent();
                assert event1 != null;
                final Signal<Integer> signal2 = new SignalTest.SimpleTestSignal(actor1, event1.getWhen(), actor2);
                actor2.addSignalToReceive(signal2);
                actor2.receiveSignal();
                final var event2 = actor2.getLastEvent();
                assert event2 != null;
                final Signal<Integer> signal3 = new SignalTest.EchoingTestSignal(actor1, event2.getWhen(), actor2);
                actor2.addSignalToReceive(signal3);
                actor2.receiveSignal();
                assert !actor1.getSignalsToReceive().isEmpty();

                removeSignal(actor2, signal2);

                assertThat("rescheduled subsequent signal", actor2.getSignalsToReceive(), is(Set.of(signal3)));
                assertThat("retains event due to previous signal", actor2.getEvents(), is(Set.of(event1)));
                assertThat("updated when receive next signal", actor2.getWhenReceiveNextSignal(), is(signal3.getWhenReceived(actor2.getStateHistory())));
                assertThat("removed emitted signal", actor1.getSignalsToReceive(), empty());
            }
        }
    }

}// class
