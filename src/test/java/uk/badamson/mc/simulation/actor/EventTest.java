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
import uk.badamson.dbc.assertions.ComparableVerifier;
import uk.badamson.dbc.assertions.ObjectVerifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class EventTest {

    private static final Duration WHEN_A = Duration.ofMillis(0);

    private static final Duration WHEN_B = Duration.ofMillis(5000);

    private static final Actor<Integer> ACTOR_A = new Actor<>(WHEN_A, 0);

    private static final Actor<Integer> ACTOR_B = new Actor<>(WHEN_B, 1);

    private static final Medium MEDIUM_A = new Medium();

    private static final Signal<Integer> SIGNAL_A = new SignalTest.SimpleTestSignal(WHEN_A, ACTOR_A, ACTOR_B, MEDIUM_A);

    private static final Medium MEDIUM_B = new Medium();

    private static final Signal<Integer> SIGNAL_B = new SignalTest.SimpleTestSignal(WHEN_B, ACTOR_B, ACTOR_A, MEDIUM_B);

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event) {
        ObjectVerifier.assertInvariants(event);// inherited
        ComparableVerifier.assertInvariants(event);// inherited

        final var affectedObject = event.getAffectedObject();
        final var causingSignal = event.getCausingSignal();
        final var signalsEmitted = event.getSignalsEmitted();
        final var createdActors = event.getCreatedActors();
        final var when = event.getWhen();
        final var indirectlyAffectedObjects = event.getIndirectlyAffectedObjects();

        assertAll("Not null", () -> assertNotNull(affectedObject, "affectedObject"),
                () -> assertNotNull(causingSignal, "causingSignal"),
                () -> assertNotNull(signalsEmitted, "signalsEmitted"), // guard
                () -> assertNotNull(when, "when"),
                () -> assertThat(createdActors, notNullValue()),
                () -> assertThat(indirectlyAffectedObjects, notNullValue()));

        assertAll("signalsEmitted",
                signalsEmitted.stream().map(signal -> () -> {
                    assertNotNull(signal, "signal");
                    SignalTest.assertInvariants(signal);
                    assertSame(affectedObject, signal.getSender(), "sender");
                    assertSame(when, signal.getWhenSent(), "whenSent");
                    assertThat(indirectlyAffectedObjects, hasItem(signal.getReceiver()));
                }));
        assertAll(
                createdActors.stream().map(actor -> () -> {
                    assertThat(actor, notNullValue());
                    assertAll(
                            ()->assertThat(actor.getStart(), is(when)),
                            ()->assertThat(actor, not(is(affectedObject))));
                })
        );
        assertThat(indirectlyAffectedObjects, hasItem(affectedObject));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event1,
                                                @Nonnull final Event<STATE> event2) {
        ObjectVerifier.assertInvariants(event1, event2);// inherited
        ComparableVerifier.assertInvariants(event1, event2);// inherited
        ComparableVerifier.assertNaturalOrderingIsConsistentWithEquals(event1, event2);// inherited

        final int compareTo = event1.compareTo(event2);
        final int compareToWhen = event1.getWhen().compareTo(event2.getWhen());
        assertFalse(compareToWhen < 0 && 0 <= compareTo, "natural ordering first sorts by when (<)");
        assertFalse(compareToWhen > 0 && 0 >= compareTo, "natural ordering first sorts by when (>)");
    }

    private static <STATE> void constructor(
            @Nonnull final Signal<STATE> causingSignal,
            @Nonnull final Duration when,
            @Nonnull final Actor<STATE> affectedObject,
            @Nullable final STATE state) {
        final var event = new Event<>(causingSignal, when, affectedObject, state);

        assertInvariants(event);
        assertAll("Attributes", () -> assertSame(causingSignal, event.getCausingSignal(), "causingSignal"),
                () -> assertSame(when, event.getWhen(), "when"),
                () -> assertSame(affectedObject, event.getAffectedObject(), "affectedObject"),
                () -> assertSame(state, event.getState(), "state"),
                () -> assertThat(event.getSignalsEmitted(), empty()),
                () -> assertThat(event.getCreatedActors(), empty()));

    }

    private static <STATE> void constructor(
            @Nonnull final Signal<STATE> causingSignal,
            @Nonnull final Duration when,
            @Nonnull final Actor<STATE> affectedObject,
            @Nullable final STATE state,
            @Nonnull final Set<Signal<STATE>> signalsEmitted,
            @Nonnull final Set<Actor<STATE>> createdActors) {
        final var event = new Event<>(causingSignal, when, affectedObject, state, signalsEmitted, createdActors);

        assertInvariants(event);
        assertAll("Attributes", () -> assertSame(causingSignal, event.getCausingSignal(), "causingSignal"),
                () -> assertSame(when, event.getWhen(), "when"),
                () -> assertSame(affectedObject, event.getAffectedObject(), "affectedObject"),
                () -> assertSame(state, event.getState(), "state"),
                () -> assertThat(event.getSignalsEmitted(), containsInAnyOrder(signalsEmitted.toArray())),
                () -> assertThat(event.getCreatedActors(), containsInAnyOrder(createdActors.toArray())));

    }

    @Test
    public void destruction() {
        constructor(SIGNAL_A, WHEN_A, ACTOR_A, null);
    }

    @Test
    public void noSignalsEmitted_A() {
        constructor(SIGNAL_A, WHEN_A, ACTOR_A, 0);
    }

    @Test
    public void noSignalsEmitted_B() {
        constructor(SIGNAL_B, WHEN_B, ACTOR_B, 1);
    }

    @Test
    public void signalEmitted() {
        final var when = WHEN_A;
        final var receiver = ACTOR_A;
        final Set<Signal<Integer>> signalsEmitted = Set.of(new SignalTest.SimpleTestSignal(when, receiver, ACTOR_B, MEDIUM_A));
        constructor(SIGNAL_A, when, receiver, 0, signalsEmitted, Set.of());
    }

    @Test
    public void actorsCreated() {
        final var when = WHEN_A;
        final var actorCreated = new Actor<>(when, 2);
        constructor(SIGNAL_B, when, ACTOR_A, 1, Set.of(), Set.of(actorCreated));
    }

    @Nested
    public class Two {

        @Test
        public void differentWhen() {
            final var event1 = new Event<>(SIGNAL_A, WHEN_A, ACTOR_A, 0);
            final var event2 = new Event<>(SIGNAL_A, WHEN_B, ACTOR_A, 0);
            assertInvariants(event1, event2);
        }

        @Test
        public void differentCausingSignal() {
            final var event1 = new Event<>(SIGNAL_A, WHEN_A, ACTOR_A, 0);
            final var event2 = new Event<>(SIGNAL_B, WHEN_A, ACTOR_A, 0);
            assertInvariants(event1, event2);
        }

        @Test
        public void equivalent() {
            final var event1 = new Event<>(SIGNAL_A, WHEN_A, ACTOR_A, 0);
            final var event2 = new Event<>(SIGNAL_A, WHEN_A, ACTOR_A, 0);
            assertInvariants(event1, event2);
        }

    }// class

}// class