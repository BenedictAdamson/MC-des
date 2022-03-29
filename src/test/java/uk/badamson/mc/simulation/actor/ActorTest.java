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
import uk.badamson.mc.history.ValueHistoryTest;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ActorTest {

    static final Duration WHEN_A = Duration.ofMillis(0);
    static final Duration WHEN_B = Duration.ofMillis(5000);
    static final Duration WHEN_C = Duration.ofMillis(7000);

    public static <STATE> void assertInvariants(@Nonnull final Actor<STATE> actor) {
        ObjectVerifier.assertInvariants(actor);// inherited

        final var events = actor.getEvents();
        final var lastEvent = actor.getLastEvent();
        final var start = actor.getStart();
        final var stateHistory = actor.getStateHistory();
        final var stateTransitions = actor.getStateTransitions();

        assertAll("Not null", () -> assertNotNull(events, "events"), // guard
                () -> assertNotNull(start, "start"), // guard
                () -> assertNotNull(stateHistory, "stateHistory"), // guard
                () -> assertNotNull(stateTransitions, "stateTransitions") // guard
        );
        ValueHistoryTest.assertInvariants(stateHistory);

        assertAll(() -> assertAll("events", createEventsAssertions(actor)),
                () -> assertAll("lastEvent",
                        () -> assertThat("is null if, and only if, the sequence of events is empty.",
                                lastEvent == null == events.isEmpty()),
                        () -> assertThat("is either null or is the  last of the sequence of events.",
                                lastEvent == null || lastEvent == events.last())),
                () -> assertAll("stateHistory", () -> assertSame(start, stateHistory.getFirstTransitionTime(),

                        "The first transition time of the state history is the same as the start time of this history."),
                        () -> assertNull(stateHistory.getFirstValue(),
                                "The state at the start of time of the state history is null."),
                        () -> assertFalse(stateHistory.isEmpty(), "The state history is never empty.")),
                () -> assertEquals(stateTransitions, stateHistory.getTransitions(), "stateTransitions"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Actor<STATE> actor1,
                                                @Nonnull final Actor<STATE> actor2) {
        ObjectVerifier.assertInvariants(actor1, actor2);// inherited
    }

    private static <STATE> void constructor(@Nonnull final Duration start,
                                            @Nonnull final STATE state) {
        final var actor = new Actor<>(start, state);

        assertInvariants(actor);
        final var stateTransitions = actor.getStateTransitions();
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
                    () -> assertThat("whenOccurred", event.getWhen(), greaterThan(actor.getStart())),
                    () -> assertThat("affectedObject", event.getAffectedObject(), sameInstance(actor)),
                    () -> assertThat("state is in stateHistory", event.getState(), is(actor.getStateHistory().get(event.getWhen())))
            );
        });
    }

    private static <STATE> void addAffectingSignal(@Nonnull final Actor<STATE> actor, @Nonnull final Signal<STATE> signal) {
        actor.addAffectingSignal(signal);

        assertInvariants(actor);
        SignalTest.assertInvariants(signal);
        final var receptionEventOptional = actor.getEvents().stream().filter(event -> event.getCausingSignal() == signal).findAny();
        assertFalse(receptionEventOptional.isEmpty(), "has a reception event");
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
    public class AddAffectingSignal {

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
                final Signal<Integer> signal = new SignalTest.TestSignal(sender, whenSent, receiver);

                addAffectingSignal(receiver, signal);

                final var events = receiver.getEvents();
                assertThat("events", events, hasSize(1));// guard
                final var event = events.first();
                assertThat("event resulted from receiving the signal", event, is(signal.receive(state0)));
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
                final Signal<Integer> signal1 = new SignalTest.TestSignal(sender, whenSent1, receiver);
                final Duration whenSent2 = signal1.getWhenReceived(state0);
                final Signal<Integer> signal2 = new SignalTest.TestSignal(sender, whenSent2, receiver);
                receiver.addAffectingSignal(signal2);

                addAffectingSignal(receiver, signal1);

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
    }// class

}// class
