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
import uk.badamson.dbc.assertions.ComparableVerifier;
import uk.badamson.dbc.assertions.EqualsSemanticsVerifier;
import uk.badamson.dbc.assertions.ObjectVerifier;
import uk.badamson.mc.simulation.TimestampedId;
import uk.badamson.mc.simulation.TimestampedIdTest;
import uk.badamson.mc.simulation.actor.SignalTest.TestSignal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class EventTest {

    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();
    private static final UUID SIGNAL_A = SignalTest.ID_A;
    private static final UUID SIGNAL_B = SignalTest.ID_B;
    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);
    private static final TimestampedId ID_A = new TimestampedId(SIGNAL_A, WHEN_A);
    private static final TimestampedId ID_B = new TimestampedId(SIGNAL_B, WHEN_B);

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event) {
        ObjectVerifier.assertInvariants(event);// inherited
        ComparableVerifier.assertInvariants(event);// inherited

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
                () -> assertAll("signalsEmitted",
                        createSignalsEmittedInvariantAssertions(id, affectedObject, signalsEmitted)),
                () -> assertSame(whenOccurred, id.getWhen(), "whenOccurred"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event1,
                                                @Nonnull final Event<STATE> event2) {
        ObjectVerifier.assertInvariants(event1, event2);// inherited
        ComparableVerifier.assertInvariants(event1, event2);// inherited

        EqualsSemanticsVerifier.assertEntitySemantics(event1, event2, Event::getId);
        ComparableVerifier.assertNaturalOrderingIsConsistentWithEquals(event1, event2);
    }

    private static <STATE> void constructor(@Nonnull final TimestampedId id, @Nonnull final UUID affectedObject,
                                            @Nullable final STATE state, @Nonnull final Set<Signal<STATE>> signalsEmitted) {
        final var event = new Event<>(id, affectedObject, state, signalsEmitted);

        assertInvariants(event);
        assertAll("Attributes", () -> assertSame(id, event.getId(), "id"),
                () -> assertSame(affectedObject, event.getAffectedObject(), "affectedObject"),
                () -> assertSame(state, event.getState(), "state"),
                () -> assertEquals(signalsEmitted, event.getSignalsEmitted(), "signalsEmitted"));

    }

    private static <STATE> Stream<Executable> createSignalsEmittedInvariantAssertions(final TimestampedId id,
                                                                                      final UUID affectedObject, final Set<Signal<STATE>> signalsEmitted) {
        return signalsEmitted.stream().map(signal -> () -> {
            assertNotNull(signal, "signal");
            SignalTest.assertInvariants(signal);
            assertSame(affectedObject, signal.getSender(), "sender");
            assertSame(id.getWhen(), signal.getWhenSent(), "whenSent");
        });
    }

    @Test
    public void destruction() {
        constructor(ID_B, OBJECT_B, (Integer) null, Set.of());
    }

    @Test
    public void noSignalsEmitted() {
        constructor(ID_A, OBJECT_A, 0, Set.of());
    }

    @Test
    public void signalEmitted() {
        final var id = ID_A;
        final var when = id.getWhen();
        final var receiver = OBJECT_A;
        final TimestampedId emittedFrom = new TimestampedId(receiver, when);
        final Set<Signal<Integer>> signalsEmitted = Set.of(new TestSignal(SIGNAL_A, emittedFrom, OBJECT_B));
        constructor(id, receiver, 0, signalsEmitted);
    }

    @Nested
    public class Two {

        @Test
        public void different() {
            final Integer state = 0;
            final Set<Signal<Integer>> signalsEmitted = Set.of();

            final var eventA = new Event<>(ID_A, OBJECT_A, state, signalsEmitted);
            final var eventB = new Event<>(ID_B, OBJECT_B, state, signalsEmitted);

            assertInvariants(eventA, eventB);
            assertNotEquals(eventA, eventB);
        }

        @Test
        public void differentTime() {
            final TimestampedId idA = new TimestampedId(SIGNAL_A, WHEN_A);
            final TimestampedId idB = new TimestampedId(SIGNAL_A, WHEN_A.plusNanos(1));
            final Integer state = 0;
            final Set<Signal<Integer>> signalsEmitted = Set.of();

            final var eventA = new Event<>(idA, OBJECT_A, state, signalsEmitted);
            final var eventB = new Event<>(idB, OBJECT_A, state, signalsEmitted);

            assertInvariants(eventA, eventB);
            assertNotEquals(eventA, eventB);
        }

        @Test
        public void equivalent() {
            final var causingSignal = ID_A.getObject();
            final var when = ID_A.getWhen();
            final TimestampedId idB = new TimestampedId(causingSignal, when);
            final Integer stateA = 0;
            final Integer stateB = 1;

            final Set<Signal<Integer>> signalsEmittedA = Set.of();
            final TimestampedId emittedFrom = new TimestampedId(OBJECT_B, when);
            final Set<Signal<Integer>> signalsEmittedB = Set.of(new TestSignal(SIGNAL_A, emittedFrom, OBJECT_B));

            assert ID_A.equals(idB);
            assert ID_A != idB;// tough test
            // tough test: !stateA.equals(stateB)
            // tough test: assert !signalsEmittedA.equals(signalsEmittedB)

            final var eventA = new Event<>(ID_A, OBJECT_A, stateA, signalsEmittedA);
            final var eventB = new Event<>(idB, OBJECT_B, stateB, signalsEmittedB);

            assertInvariants(eventA, eventB);
            assertEquals(eventA, eventB);
        }
    }// class

}// class