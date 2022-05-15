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
import uk.badamson.dbc.assertions.EqualsSemanticsVerifier;
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

    private static final Medium MEDIUM_B = new Medium();

    private static final Signal<Integer> SIGNAL_A = new SignalTest.SimpleTestSignal(WHEN_A, ACTOR_A, ACTOR_B, MEDIUM_A);

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
        final var id = event.getId();

        assertAll("Not null", () -> assertNotNull(affectedObject, "affectedObject"),
                () -> assertNotNull(causingSignal, "causingSignal"),
                () -> assertNotNull(signalsEmitted, "signalsEmitted"), // guard
                () -> assertNotNull(when, "when"),
                () -> assertThat(createdActors, notNullValue()),
                () -> assertThat(indirectlyAffectedObjects, notNullValue()),
                () -> assertThat(id, notNullValue()));
        IdTest.assertInvariants(id);

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
                            () -> assertThat(actor.getStart(), is(when)),
                            () -> assertThat(actor, not(is(affectedObject))));
                })
        );
        assertThat(indirectlyAffectedObjects, hasItem(affectedObject));
        assertThat(when, sameInstance(id.getWhen()));
        assertThat(causingSignal, sameInstance(id.getCausingSignal()));
        assertThat(affectedObject, sameInstance(causingSignal.getReceiver()));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event1,
                                                @Nonnull final Event<STATE> event2) {
        ObjectVerifier.assertInvariants(event1, event2);// inherited
        EqualsSemanticsVerifier.assertEntitySemantics(event1, event2, Event::getId);
        ComparableVerifier.assertInvariants(event1, event2);// inherited
        ComparableVerifier.assertNaturalOrderingIsConsistentWithEquals(event1, event2);// inherited

        final var id1 = event1.getId();
        final var id2 = event2.getId();
        IdTest.assertInvariants(id1, id2);

        final int compareTo = event1.compareTo(event2);
        final int idCompareTo = id1.compareTo(id2);
        assertAll("The natural ordering of Event objects is the same as the natural ordering of their IDs.",
                () -> assertThat(compareTo < 0, is(idCompareTo < 0)),
                () -> assertThat(compareTo > 0, is(idCompareTo > 0)));
    }

    private static <STATE> void constructor(
            @Nonnull final Signal<STATE> causingSignal,
            @Nonnull final Duration when,
            @Nullable final STATE state) {
        final var event = new Event<>(causingSignal, when, state);

        assertInvariants(event);
        assertAll("Attributes", () -> assertSame(causingSignal, event.getCausingSignal(), "causingSignal"),
                () -> assertSame(when, event.getWhen(), "when"),
                () -> assertSame(state, event.getState(), "state"),
                () -> assertThat(event.getSignalsEmitted(), empty()),
                () -> assertThat(event.getCreatedActors(), empty()));

    }

    private static <STATE> void constructor(
            @Nonnull final Signal<STATE> causingSignal,
            @Nonnull final Duration when,
            @Nullable final STATE state,
            @Nonnull final Set<Signal<STATE>> signalsEmitted,
            @Nonnull final Set<Actor<STATE>> createdActors) {
        final var event = new Event<>(causingSignal, when, state, signalsEmitted, createdActors);

        assertInvariants(event);
        assertAll("Attributes", () -> assertSame(causingSignal, event.getCausingSignal(), "causingSignal"),
                () -> assertSame(when, event.getWhen(), "when"),
                () -> assertSame(state, event.getState(), "state"),
                () -> assertThat(event.getSignalsEmitted(), containsInAnyOrder(signalsEmitted.toArray())),
                () -> assertThat(event.getCreatedActors(), containsInAnyOrder(createdActors.toArray())));

    }

    @Test
    public void destruction() {
        constructor(SIGNAL_A, WHEN_A, null);
    }

    @Test
    public void noSignalsEmitted_A() {
        constructor(SIGNAL_A, WHEN_A, 0);
    }

    @Test
    public void noSignalsEmitted_B() {
        constructor(SIGNAL_B, WHEN_B, 1);
    }

    @Test
    public void signalEmitted() {
        final var when = WHEN_A;
        final Set<Signal<Integer>> signalsEmitted = Set.of(new SignalTest.SimpleTestSignal(when, ACTOR_B, ACTOR_A, MEDIUM_A));
        constructor(SIGNAL_A, when, 0, signalsEmitted, Set.of());
    }

    @Test
    public void actorsCreated() {
        final var when = WHEN_A;
        final var actorCreated = new Actor<>(when, 2);
        constructor(SIGNAL_B, when, 1, Set.of(), Set.of(actorCreated));
    }

    public static class IdTest {


        public static <STATE> void assertInvariants(@Nonnull final Event.Id<STATE> id) {
            ObjectVerifier.assertInvariants(id);// inherited
            ComparableVerifier.assertInvariants(id);// inherited

            final var causingSignal = id.getCausingSignal();
            final var when = id.getWhen();

            assertAll(() -> assertNotNull(causingSignal, "causingSignal"),
                    () -> assertNotNull(when, "when"));
        }

        public static <STATE> void assertInvariants(@Nonnull final Event.Id<STATE> id1,
                                                    @Nonnull final Event.Id<STATE> id2) {
            ObjectVerifier.assertInvariants(id1, id2);// inherited
            EqualsSemanticsVerifier.assertValueSemantics(id1, id2, "when", Event.Id::getWhen);
            EqualsSemanticsVerifier.assertValueSemantics(id1, id2, "causingSignal", Event.Id::getCausingSignal);
            ComparableVerifier.assertInvariants(id1, id2);// inherited
            ComparableVerifier.assertNaturalOrderingIsConsistentWithEquals(id1, id2);// inherited

            final int compareTo = id1.compareTo(id2);
            final int compareToWhen = id1.getWhen().compareTo(id2.getWhen());
            assertFalse(compareToWhen < 0 && 0 <= compareTo, "natural ordering first sorts by when (<)");
            assertFalse(compareToWhen > 0 && 0 >= compareTo, "natural ordering first sorts by when (>)");
        }

        private static <STATE> void constructor(
                @Nonnull final Signal<STATE> causingSignal,
                @Nonnull final Duration when) {
            final var event = new Event.Id<>(causingSignal, when);

            assertInvariants(event);
            assertAll("Attributes", () -> assertSame(causingSignal, event.getCausingSignal(), "causingSignal"),
                    () -> assertSame(when, event.getWhen(), "when")
            );

        }

        @Test
        public void a() {
            constructor(SIGNAL_A, WHEN_A);
        }

        @Test
        public void b() {
            constructor(SIGNAL_B, WHEN_B);
        }


        @Nested
        public class Two {

            @Test
            public void differentWhen() {
                final var id1 = new Event.Id<>(SIGNAL_A, WHEN_A);
                final var id2 = new Event.Id<>(SIGNAL_A, WHEN_B);
                assertInvariants(id1, id2);
            }

            @Test
            public void differentCausingSignal() {
                final var id1 = new Event.Id<>(SIGNAL_A, WHEN_A);
                final var id2 = new Event.Id<>(SIGNAL_B, WHEN_A);
                assertInvariants(id1, id2);
            }

            @Test
            public void equivalent() {
                final var id1 = new Event.Id<>(SIGNAL_A, WHEN_A);
                final var id2 = new Event.Id<>(SIGNAL_A, WHEN_A);
                assertInvariants(id1, id2);
            }

        }// class

    }

    @Nested
    public class Two {

        @Test
        public void differentWhen() {
            final var event1 = new Event<>(SIGNAL_A, WHEN_A, 0);
            final var event2 = new Event<>(SIGNAL_A, WHEN_B, 0);
            assertInvariants(event1, event2);
        }

        @Test
        public void differentCausingSignal() {
            final var event1 = new Event<>(SIGNAL_A, WHEN_A, 0);
            final var event2 = new Event<>(SIGNAL_B, WHEN_A, 0);
            assertInvariants(event1, event2);
        }

        @Test
        public void equivalent() {
            final var event1 = new Event<>(SIGNAL_A, WHEN_A, 0);
            final var event2 = new Event<>(SIGNAL_A, WHEN_A, 0);
            assertInvariants(event1, event2);
        }

    }// class

}// class