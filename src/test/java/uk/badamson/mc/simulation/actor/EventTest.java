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

import java.util.Set;
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
        public void differentEventId() {
            final TimestampedId eventIdA = SignalTest.OBJECT_STATE_ID_A;
            final TimestampedId eventIdB = SignalTest.OBJECT_STATE_ID_B;
            final Integer state = Integer.valueOf(Integer.MAX_VALUE);
            final Set<Signal<Integer>> signalsEmitted = Set.of();

            final var effectA = new Event<>(eventIdA, state, signalsEmitted);
            final var effectB = new Event<>(eventIdB, state, signalsEmitted);

            assertInvariants(effectA, effectB);
            assertNotEquals(effectA, effectB);
        }

        @Test
        public void differentSignalsEmitted() {
            final TimestampedId eventId = SignalTest.OBJECT_STATE_ID_A;
            final Integer state = Integer.valueOf(0);
            final Set<Signal<Integer>> signalsEmittedA = Set.of();
            final Set<Signal<Integer>> signalsEmittedB = Set
                    .of(new TestSignal(SignalTest.ID_A, eventId, SignalTest.OBJECT_B));

            final var effectA = new Event<>(eventId, state, signalsEmittedA);
            final var effectB = new Event<>(eventId, state, signalsEmittedB);

            assertInvariants(effectA, effectB);
            assertNotEquals(effectA, effectB);
        }

        @Test
        public void differentState() {
            final Integer stateA = Integer.valueOf(0);
            final Integer stateB = Integer.valueOf(1);
            final Set<Signal<Integer>> signalsEmitted = Set.of();

            final var effectA = new Event<>(SignalTest.OBJECT_STATE_ID_A, stateA, signalsEmitted);
            final var effectB = new Event<>(SignalTest.OBJECT_STATE_ID_A, stateB, signalsEmitted);

            assertInvariants(effectA, effectB);
            assertNotEquals(effectA, effectB);
        }

        /*
         * A faulty implementation could throw a NullPointerException for this case.
         */
        @Test
        public void equivalentDestruction() {
            final Integer state = null;
            final Set<Signal<Integer>> signalsEmitted = Set.of();

            final var effectA = new Event<>(SignalTest.OBJECT_STATE_ID_A, state, signalsEmitted);
            final var effectB = new Event<>(SignalTest.OBJECT_STATE_ID_A, state, signalsEmitted);

            assertInvariants(effectA, effectB);
            assertEquivalent(effectA, effectB);
        }

        @Test
        public void equivalentNotDestruction() {
            final TimestampedId eventIdA = SignalTest.OBJECT_STATE_ID_A;
            final TimestampedId eventIdB = new TimestampedId(eventIdA.getObject(), eventIdA.getWhen());
            final Integer stateA = Integer.valueOf(Integer.MAX_VALUE);
            final Integer stateB = Integer.valueOf(Integer.MAX_VALUE);
            final Set<Signal<Integer>> signalsEmittedA = Set
                    .of(new TestSignal(SignalTest.ID_A, eventIdA, SignalTest.OBJECT_B));
            final Set<Signal<Integer>> signalsEmittedB = Set
                    .of(new TestSignal(SignalTest.ID_B, eventIdB, SignalTest.OBJECT_B));
            assert eventIdA.equals(eventIdB);
            assert stateA.equals(stateB);
            assert eventIdA != eventIdB;// tough test
            assert stateA != stateB;// tough test
            assert signalsEmittedA != signalsEmittedB;// tough test

            final var effectA = new Event<>(eventIdA, stateA, signalsEmittedA);
            final var effectB = new Event<>(eventIdB, stateB, signalsEmittedB);

            assertInvariants(effectA, effectB);
            assertEquivalent(effectA, effectB);
        }
    }// class

    private static <STATE> void assertEquivalent(@Nonnull final Event<STATE> effect1,
            @Nonnull final Event<STATE> effect2) {
        assertAll(() -> assertEquals(effect1.getEventId(), effect2.getEventId(), "eventId"),
                () -> assertEquals(effect1.getState(), effect2.getState(), "state"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> effect) {
        ObjectTest.assertInvariants(effect);// inherited

        final var affectedObject = effect.getAffectedObject();
        final var eventId = effect.getEventId();
        final var signalsEmitted = effect.getSignalsEmitted();
        final var whenOccurred = effect.getWhenOccurred();

        assertAll("Not null", () -> assertNotNull(affectedObject, "affectedObject"),
                () -> assertNotNull(eventId, "eventId"), // guard
                () -> assertNotNull(signalsEmitted, "signalsEmitted"), // guard
                () -> assertNotNull(whenOccurred, "whenOccurred"));
        TimestampedIdTest.assertInvariants(eventId);

        assertAll(() -> assertSame(affectedObject, eventId.getObject(), "affectedObject"),
                () -> assertAll("signalsEmitted", createSignalsEmittedInvariantAssertions(eventId, signalsEmitted)),
                () -> assertSame(whenOccurred, eventId.getWhen(), "whenOccurred"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> effect1,
            @Nonnull final Event<STATE> effect2) {
        ObjectTest.assertInvariants(effect1, effect2);// inherited

        final boolean equals = effect1.equals(effect2);
        assertAll("Value semantics",
                () -> assertFalse(equals && !effect1.getEventId().equals(effect2.getEventId()), "eventId"),
                () -> assertFalse(equals && !effect1.getSignalsEmitted().equals(effect2.getSignalsEmitted()),
                        "signalsEmitted"));
    }

    private static <STATE> Event<STATE> constructor(@Nonnull final TimestampedId eventId, @Nullable final STATE state,
            @Nonnull final Set<Signal<STATE>> signalsEmitted) {
        final var effect = new Event<>(eventId, state, signalsEmitted);

        assertInvariants(effect);
        assertAll("Attributes", () -> assertSame(eventId, effect.getEventId(), "eventId"),
                () -> assertSame(state, effect.getState(), "state"),
                () -> assertEquals(signalsEmitted, effect.getSignalsEmitted(), "signalsEmitted"));

        return effect;
    }

    private static <STATE> Stream<Executable> createSignalsEmittedInvariantAssertions(final TimestampedId eventId,
            final Set<Signal<STATE>> signalsEmitted) {
        return signalsEmitted.stream().map(signal -> new Executable() {

            @Override
            public void execute() throws AssertionError {
                assertNotNull(signal, "signal");
                SignalTest.assertInvariants(signal);
                assertSame(eventId, signal.getSentFrom(), "sent from the same event that caused this effect");
            }
        });
    }

    @Test
    public void destruction() {
        constructor(SignalTest.OBJECT_STATE_ID_B, (Integer) null, Set.of());
    }

    @Test
    public void noSignalsEmitted() {
        constructor(SignalTest.OBJECT_STATE_ID_A, Integer.valueOf(0), Set.of());
    }

    @Test
    public void signalEmitted() {
        final var eventId = SignalTest.OBJECT_STATE_ID_A;
        final Set<Signal<Integer>> signalsEmitted = Set
                .of(new TestSignal(SignalTest.ID_A, eventId, SignalTest.OBJECT_B));
        constructor(eventId, Integer.valueOf(0), signalsEmitted);
    }

}// class