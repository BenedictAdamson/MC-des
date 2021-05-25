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
import uk.badamson.dbc.assertions.ComparableTest;
import uk.badamson.dbc.assertions.EqualsSemanticsTest;
import uk.badamson.dbc.assertions.ObjectTest;
import uk.badamson.mc.simulation.TimestampedId;
import uk.badamson.mc.simulation.TimestampedIdTest;
import uk.badamson.mc.simulation.actor.SignalTest.TestSignal;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class EventTest {

    @Nested
    public class Two {

        @Test
        public void different() {
            final TimestampedId idA = ID_A;
            final TimestampedId idB = ID_B;
            final Integer state = Integer.valueOf(0);
            final Set<Signal<Integer>> signalsEmitted = Set.of();

            final var eventA = new Event<>(idA, OBJECT_A, state, signalsEmitted);
            final var eventB = new Event<>(idB, OBJECT_B, state, signalsEmitted);

            assertInvariants(eventA, eventB);
            assertNotEquals(eventA, eventB);
        }

        @Test
        public void differentTime() {
            final TimestampedId idA = new TimestampedId(SIGNAL_A, WHEN_A);
            final TimestampedId idB = new TimestampedId(SIGNAL_A, WHEN_A.plusNanos(1));
            final Integer state = Integer.valueOf(0);
            final Set<Signal<Integer>> signalsEmitted = Set.of();

            final var eventA = new Event<>(idA, OBJECT_A, state, signalsEmitted);
            final var eventB = new Event<>(idB, OBJECT_A, state, signalsEmitted);

            assertInvariants(eventA, eventB);
            assertNotEquals(eventA, eventB);
        }

        @Test
        public void equivalent() {
            final TimestampedId idA = ID_A;
            final TimestampedId idB = new TimestampedId(idA.getObject(), idA.getWhen());
            final Integer stateA = Integer.valueOf(0);
            final Integer stateB = Integer.valueOf(1);
            final Set<Signal<Integer>> signalsEmittedA = Set.of();
            final Set<Signal<Integer>> signalsEmittedB = Set.of(new TestSignal(SIGNAL_B, idB, OBJECT_B));
            assert idA.equals(idB);
            assert idA != idB;// tough test
            assert !stateA.equals(stateB);// tough test
            assert !signalsEmittedA.equals(signalsEmittedB);// tough test

            final var eventA = new Event<>(idA, OBJECT_A, stateA, signalsEmittedA);
            final var eventB = new Event<>(idB, OBJECT_B, stateB, signalsEmittedB);

            assertInvariants(eventA, eventB);
            assertEquals(eventA, eventB);
        }
    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();

    private static final UUID OBJECT_B = UUID.randomUUID();

    private static final UUID SIGNAL_A = SignalTest.ID_A;

    private static final UUID SIGNAL_B = SignalTest.ID_B;

    private static final Duration WHEN_A = Duration.ofMillis(0);

    private static final Duration WHEN_B = Duration.ofMillis(5000);

    private static final TimestampedId ID_A = new TimestampedId(SIGNAL_A, WHEN_A);

    private static final TimestampedId ID_B = new TimestampedId(SIGNAL_B, WHEN_B);

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event) {
        ObjectTest.assertInvariants(event);// inherited
        ComparableTest.assertInvariants(event);// inherited

        final var affectedObject = event.getAffectedObject();
        final var causingSignal = event.getCausingSignal();
        final var id = event.getId();
        final var signalsEmitted = event.getSignalsEmitted();
        final var whenOccurred = event.getWhenOccurred();

        assertAll("Not null", () -> assertNotNull(affectedObject, "affectedObject"),
                () -> assertNotNull(causingSignal, "causingSignal"), () -> assertNotNull(id, "id"), // guard
                () -> assertNotNull(signalsEmitted, "signalsEmitted"), // guard
                () -> assertNotNull(whenOccurred, "whenOccurred"));
        TimestampedIdTest.assertInvariants(id);

        assertAll(() -> assertSame(causingSignal, id.getObject(), "causingSignal"),
                () -> assertAll("signalsEmitted", createSignalsEmittedInvariantAssertions(id, signalsEmitted)),
                () -> assertSame(whenOccurred, id.getWhen(), "whenOccurred"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event1,
            @Nonnull final Event<STATE> event2) {
        ObjectTest.assertInvariants(event1, event2);// inherited
        ComparableTest.assertInvariants(event1, event2);// inherited

        EqualsSemanticsTest.assertEntitySemantics(event1, event2, event -> event.getId());
        ComparableTest.assertNaturalOrderingIsConsistentWithEquals(event1, event2);
    }

    private static <STATE> Event<STATE> constructor(@Nonnull final TimestampedId id, @Nonnull final UUID affectedObject,
            @Nullable final STATE state, @Nonnull final Set<Signal<STATE>> signalsEmitted) {
        final var event = new Event<>(id, affectedObject, state, signalsEmitted);

        assertInvariants(event);
        assertAll("Attributes", () -> assertSame(id, event.getId(), "id"),
                () -> assertSame(affectedObject, event.getAffectedObject(), "affectedObject"),
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
        constructor(ID_B, OBJECT_B, (Integer) null, Set.of());
    }

    @Test
    public void noSignalsEmitted() {
        constructor(ID_A, OBJECT_A, Integer.valueOf(0), Set.of());
    }

    @Test
    public void signalEmitted() {
        final var id = ID_A;
        final Set<Signal<Integer>> signalsEmitted = Set.of(new TestSignal(SIGNAL_A, id, OBJECT_B));
        constructor(id, OBJECT_A, Integer.valueOf(0), signalsEmitted);
    }

}// class