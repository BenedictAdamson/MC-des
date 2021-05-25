package uk.badamson.mc.simulation.actor;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.dbc.assertions.ObjectTest;
import uk.badamson.mc.simulation.TimestampedId;
import uk.badamson.mc.simulation.TimestampedIdTest;
import uk.badamson.mc.simulation.actor.SignalTest.TestSignal;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class EventTest {

    @Nested
    public class Two {

        @Test
        public void differentId() {
            final TimestampedId idA = ID_A;
            final TimestampedId idB = ID_B;
            final Integer state = Integer.valueOf(Integer.MAX_VALUE);
            final Set<Signal<Integer>> signalsEmitted = Set.of();

            final var eventA = new Event<>(idA, state, signalsEmitted);
            final var eventB = new Event<>(idB, state, signalsEmitted);

            assertInvariants(eventA, eventB);
            assertNotEquals(eventA, eventB);
        }

        @Test
        public void differentSignalsEmitted() {
            final Integer state = Integer.valueOf(0);
            final Set<Signal<Integer>> signalsEmittedA = Set.of();
            final Set<Signal<Integer>> signalsEmittedB = Set.of(new TestSignal(SIGNAL_A, ID_A, OBJECT_B));

            final var eventA = new Event<>(ID_A, state, signalsEmittedA);
            final var eventB = new Event<>(ID_A, state, signalsEmittedB);

            assertInvariants(eventA, eventB);
            assertNotEquals(eventA, eventB);
        }

        @Test
        public void differentState() {
            final Integer stateA = Integer.valueOf(0);
            final Integer stateB = Integer.valueOf(1);
            final Set<Signal<Integer>> signalsEmitted = Set.of();

            final var eventA = new Event<>(ID_A, stateA, signalsEmitted);
            final var eventB = new Event<>(ID_A, stateB, signalsEmitted);

            assertInvariants(eventA, eventB);
            assertNotEquals(eventA, eventB);
        }

        /*
         * A faulty implementation could throw a NullPointerException for this case.
         */
        @Test
        public void equivalentDestruction() {
            final Integer state = null;// critical
            final Set<Signal<Integer>> signalsEmitted = Set.of();

            final var eventA = new Event<>(ID_A, state, signalsEmitted);
            final var eventB = new Event<>(ID_A, state, signalsEmitted);

            assertInvariants(eventA, eventB);
            assertEquivalent(eventA, eventB);
        }

        @Test
        public void equivalentNotDestruction() {
            final TimestampedId idA = ID_A;
            final TimestampedId idB = new TimestampedId(idA.getObject(), idA.getWhen());
            final Integer stateA = Integer.valueOf(Integer.MAX_VALUE);
            final Integer stateB = Integer.valueOf(Integer.MAX_VALUE);
            final Set<Signal<Integer>> signalsEmittedA = Set.of(new TestSignal(SIGNAL_A, idA, OBJECT_B));
            final Set<Signal<Integer>> signalsEmittedB = Set.of(new TestSignal(SIGNAL_B, idB, OBJECT_B));
            assert idA.equals(idB);
            assert stateA.equals(stateB);
            assert idA != idB;// tough test
            assert stateA != stateB;// tough test
            assert signalsEmittedA != signalsEmittedB;// tough test

            final var eventA = new Event<>(idA, stateA, signalsEmittedA);
            final var eventB = new Event<>(idB, stateB, signalsEmittedB);

            assertInvariants(eventA, eventB);
            assertEquivalent(eventA, eventB);
        }
    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();

    private static final UUID OBJECT_B = UUID.randomUUID();

    private static final UUID SIGNAL_A = SignalTest.ID_A;

    private static final UUID SIGNAL_B = SignalTest.ID_B;

    private static final Duration WHEN_A = Duration.ofMillis(0);

    private static final Duration WHEN_B = Duration.ofMillis(5000);

    private static final TimestampedId ID_A = new TimestampedId(OBJECT_A, WHEN_A);

    private static final TimestampedId ID_B = new TimestampedId(OBJECT_B, WHEN_B);

    private static <STATE> void assertEquivalent(@Nonnull final Event<STATE> event1,
            @Nonnull final Event<STATE> event2) {
        assertAll(() -> assertEquals(event1.getId(), event2.getId(), "id"),
                () -> assertEquals(event1.getState(), event2.getState(), "state"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event) {
        ObjectTest.assertInvariants(event);// inherited

        final var affectedObject = event.getAffectedObject();
        final var id = event.getId();
        final var signalsEmitted = event.getSignalsEmitted();
        final var whenOccurred = event.getWhenOccurred();

        assertAll("Not null", () -> assertNotNull(affectedObject, "affectedObject"), () -> assertNotNull(id, "id"), // guard
                () -> assertNotNull(signalsEmitted, "signalsEmitted"), // guard
                () -> assertNotNull(whenOccurred, "whenOccurred"));
        TimestampedIdTest.assertInvariants(id);

        assertAll(() -> assertSame(affectedObject, id.getObject(), "affectedObject"),
                () -> assertAll("signalsEmitted", createSignalsEmittedInvariantAssertions(id, signalsEmitted)),
                () -> assertSame(whenOccurred, id.getWhen(), "whenOccurred"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event1,
            @Nonnull final Event<STATE> event2) {
        ObjectTest.assertInvariants(event1, event2);// inherited

        final boolean equals = event1.equals(event2);
        assertAll("Value semantics", () -> assertFalse(equals && !event1.getId().equals(event2.getId()), "id"),
                () -> assertFalse(equals && !event1.getSignalsEmitted().equals(event2.getSignalsEmitted()),
                        "signalsEmitted"));
    }

    private static <STATE> Event<STATE> constructor(@Nonnull final TimestampedId id, @Nullable final STATE state,
            @Nonnull final Set<Signal<STATE>> signalsEmitted) {
        final var event = new Event<>(id, state, signalsEmitted);

        assertInvariants(event);
        assertAll("Attributes", () -> assertSame(id, event.getId(), "id"),
                () -> assertSame(state, event.getState(), "state"),
                () -> assertEquals(signalsEmitted, event.getSignalsEmitted(), "signalsEmitted"));

        return event;
    }

    private static <STATE> Stream<Executable> createSignalsEmittedInvariantAssertions(final TimestampedId id,
            final Set<Signal<STATE>> signalsEmitted) {
        return signalsEmitted.stream().map(signal -> new Executable() {

            @Override
            public void execute() throws AssertionError {
                assertNotNull(signal, "signal");
                SignalTest.assertInvariants(signal);
                assertSame(id, signal.getSentFrom(), "sentFrom");
            }
        });
    }

    @Test
    public void destruction() {
        constructor(ID_B, (Integer) null, Set.of());
    }

    @Test
    public void noSignalsEmitted() {
        constructor(ID_A, Integer.valueOf(0), Set.of());
    }

    @Test
    public void signalEmitted() {
        final var id = ID_A;
        final Set<Signal<Integer>> signalsEmitted = Set.of(new TestSignal(SIGNAL_A, id, OBJECT_B));
        constructor(id, Integer.valueOf(0), signalsEmitted);
    }

}// class