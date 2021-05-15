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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.simulation.ObjectStateId;
import uk.badamson.mc.simulation.ObjectStateIdTest;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class SignalTest {

    @Nested
    public class Constructor {

        @Nested
        public class Two {

            @Test
            public void differentReceiver() {
                final Signal<Integer> signalA = new TestSignal(ID_A, OBJECT_A);
                final Signal<Integer> signalB = new TestSignal(ID_A, OBJECT_B);

                assertInvariants(signalA, signalB);
                assertNotEquals(signalA, signalB);
            }

            @Test
            public void differentSentFrom() {
                final Signal<Integer> signalA = new TestSignal(ID_A, OBJECT_A);
                final Signal<Integer> signalB = new TestSignal(ID_B, OBJECT_A);

                assertInvariants(signalA, signalB);
                assertNotEquals(signalA, signalB);
            }

            @Test
            public void equivalent() {
                final ObjectStateId sentFromA = ID_A;
                final ObjectStateId sentFromB = new ObjectStateId(ID_A.getObject(), ID_A.getWhen());
                final UUID receiverA = OBJECT_B;
                final UUID receiverB = new UUID(receiverA.getMostSignificantBits(),
                        receiverA.getLeastSignificantBits());
                assert sentFromA.equals(sentFromB);
                assert receiverA.equals(receiverB);
                assert sentFromA != sentFromB; // tough test
                assert receiverA != receiverB; // tough test

                final Signal<Integer> signalA = new TestSignal(sentFromA, receiverA);
                final Signal<Integer> signalB = new TestSignal(sentFromB, receiverB);

                assertInvariants(signalA, signalB);
                assertEquals(signalA, signalB);
            }
        }// class

        @Test
        public void a() {
            constructor(ID_A, OBJECT_B);
        }

        @Test
        public void b() {
            constructor(ID_B, OBJECT_A);
        }

        @Test
        public void reflexive() {
            constructor(ID_A, OBJECT_A);
        }

    }// class

    public static final class EffectTest {

        @Nested
        public class Two {

            @Test
            public void differentEventId() {
                final ObjectStateId eventIdA = ID_A;
                final ObjectStateId eventIdB = ID_B;
                final Integer state = Integer.valueOf(Integer.MAX_VALUE);
                final Set<Signal<Integer>> signalsEmitted = Set.of();

                final var effectA = new Signal.Effect<>(eventIdA, state, signalsEmitted);
                final var effectB = new Signal.Effect<>(eventIdB, state, signalsEmitted);

                assertInvariants(effectA, effectB);
                assertNotEquals(effectA, effectB);
            }

            @Test
            public void differentSignalsEmitted() {
                final ObjectStateId eventId = ID_A;
                final Integer state = Integer.valueOf(0);
                final Set<Signal<Integer>> signalsEmittedA = Set.of();
                final Set<Signal<Integer>> signalsEmittedB = Set.of(new TestSignal(eventId, OBJECT_B));

                final var effectA = new Signal.Effect<>(eventId, state, signalsEmittedA);
                final var effectB = new Signal.Effect<>(eventId, state, signalsEmittedB);

                assertInvariants(effectA, effectB);
                assertNotEquals(effectA, effectB);
            }

            @Test
            public void differentState() {
                final Integer stateA = Integer.valueOf(0);
                final Integer stateB = Integer.valueOf(1);
                final Set<Signal<Integer>> signalsEmitted = Set.of();

                final var effectA = new Signal.Effect<>(ID_A, stateA, signalsEmitted);
                final var effectB = new Signal.Effect<>(ID_A, stateB, signalsEmitted);

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

                final var effectA = new Signal.Effect<>(ID_A, state, signalsEmitted);
                final var effectB = new Signal.Effect<>(ID_A, state, signalsEmitted);

                assertInvariants(effectA, effectB);
                assertEquals(effectA, effectB);
            }

            @Test
            public void equivalentNotDestruction() {
                final ObjectStateId eventIdA = ID_A;
                final ObjectStateId eventIdB = new ObjectStateId(eventIdA.getObject(), eventIdA.getWhen());
                final Integer stateA = Integer.valueOf(Integer.MAX_VALUE);
                final Integer stateB = Integer.valueOf(Integer.MAX_VALUE);
                final Set<Signal<Integer>> signalsEmittedA = Set.of(new TestSignal(eventIdA, OBJECT_B));
                final Set<Signal<Integer>> signalsEmittedB = Set.of(new TestSignal(eventIdB, OBJECT_B));
                assert eventIdA.equals(eventIdB);
                assert stateA.equals(stateB);
                assert signalsEmittedA.equals(signalsEmittedB);
                assert eventIdA != eventIdB;// tough test
                assert stateA != stateB;// tough test
                assert signalsEmittedA != signalsEmittedB;// tough test

                final var effectA = new Signal.Effect<>(eventIdA, stateA, signalsEmittedA);
                final var effectB = new Signal.Effect<>(eventIdB, stateB, signalsEmittedB);

                assertInvariants(effectA, effectB);
                assertEquals(effectA, effectB);
            }
        }// class

        public static <STATE> void assertInvariants(@Nonnull final Signal.Effect<STATE> effect) {
            ObjectTest.assertInvariants(effect);// inherited

            final var affectedObject = effect.getAffectedObject();
            final var eventId = effect.getEventId();
            final var signalsEmitted = effect.getSignalsEmitted();
            final var whenOccurred = effect.getWhenOccurred();

            assertAll("Not null", () -> assertNotNull(affectedObject, "affectedObject"),
                    () -> assertNotNull(eventId, "eventId"), // guard
                    () -> assertNotNull(signalsEmitted, "signalsEmitted"), // guard
                    () -> assertNotNull(whenOccurred, "whenOccurred"));
            ObjectStateIdTest.assertInvariants(eventId);

            assertAll(() -> assertSame(affectedObject, eventId.getObject(), "affectedObject"),
                    () -> assertAll("signalsEmitted", createSignalsEmittedInvariantAssertions(eventId, signalsEmitted)),
                    () -> assertSame(whenOccurred, eventId.getWhen(), "whenOccurred"));
        }

        public static <STATE> void assertInvariants(@Nonnull final Signal.Effect<STATE> effect1,
                @Nonnull final Signal.Effect<STATE> effect2) {
            ObjectTest.assertInvariants(effect1, effect2);// inherited

            final boolean equals = effect1.equals(effect2);
            assertAll("Value semantics",
                    () -> assertFalse(equals && !effect1.getEventId().equals(effect2.getEventId()), "eventId"),
                    () -> assertFalse(equals && !effect1.getSignalsEmitted().equals(effect2.getSignalsEmitted()),
                            "signalsEmitted"));
        }

        private static <STATE> Signal.Effect<STATE> constructor(@Nonnull final ObjectStateId eventId,
                @Nullable final STATE state, @Nonnull final Set<Signal<STATE>> signalsEmitted) {
            final var effect = new Signal.Effect<>(eventId, state, signalsEmitted);

            assertInvariants(effect);
            assertAll("Attributes", () -> assertSame(eventId, effect.getEventId(), "eventId"),
                    () -> assertSame(state, effect.getState(), "state"),
                    () -> assertEquals(signalsEmitted, effect.getSignalsEmitted(), "signalsEmitted"));

            return effect;
        }

        private static <STATE> Stream<Executable> createSignalsEmittedInvariantAssertions(final ObjectStateId eventId,
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
            constructor(ID_B, (Integer) null, Set.of());
        }

        @Test
        public void noSignalsEmitted() {
            constructor(ID_A, Integer.valueOf(0), Set.of());
        }

        @Test
        public void signalEmitted() {
            final var eventId = ID_A;
            final Set<Signal<Integer>> signalsEmitted = Set.of(new TestSignal(eventId, OBJECT_B));
            constructor(eventId, Integer.valueOf(0), signalsEmitted);
        }

    }// class

    static class TestSignal extends Signal<Integer> {

        TestSignal(@Nonnull final ObjectStateId sentFrom, @Nonnull final UUID receiver) {
            super(sentFrom, receiver);
        }

        @Override
        @Nonnull
        @Nonnegative
        public Duration getPropagationTime(@Nonnull final Integer receiverState) {
            Objects.requireNonNull(receiverState, "receiverState");
            return Duration.ofSeconds(Integer.max(1, receiverState.intValue()));
        }

    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);

    private static final ObjectStateId ID_A = new ObjectStateId(OBJECT_A, WHEN_A);
    private static final ObjectStateId ID_B = new ObjectStateId(OBJECT_B, WHEN_B);

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal) {
        ObjectTest.assertInvariants(signal);// inherited

        final var receiver = signal.getReceiver();
        final var sender = signal.getSender();
        final var sentFrom = signal.getSentFrom();
        final var whenSent = signal.getWhenSent();
        assertAll("Not null", () -> assertNotNull(receiver, "receiver"), () -> assertNotNull(sender, "sender"),
                () -> assertNotNull(sentFrom, "sentFrom"), // guard
                () -> assertNotNull(whenSent, "whenSent"));
        ObjectStateIdTest.assertInvariants(sentFrom);
        assertAll("consistent attributes", () -> assertSame(sender, sentFrom.getObject(), "sender with SentFrom"),
                () -> assertSame(whenSent, sentFrom.getWhen(), "whenSent with SentFrom"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal1,
            @Nonnull final Signal<STATE> signal2) {
        ObjectTest.assertInvariants(signal1, signal2);// inherited

        final var equals = signal1.equals(signal2);
        assertAll("value semantics",
                () -> assertFalse(equals && !signal1.getReceiver().equals(signal2.getReceiver()), "receiver"),
                () -> assertFalse(equals && !signal1.getSentFrom().equals(signal2.getSentFrom()), "sentFrom"));
    }

    private static Signal<Integer> constructor(@Nonnull final ObjectStateId sentFrom, @Nonnull final UUID receiver) {
        final Signal<Integer> signal = new TestSignal(sentFrom, receiver);

        assertInvariants(signal);
        assertAll("Attributes", () -> assertSame(sentFrom, signal.getSentFrom(), "sentFrom"),
                () -> assertSame(receiver, signal.getReceiver(), "receiver"));
        return signal;
    }

    public static <STATE> Duration getPropagationTime(@Nonnull final Signal<STATE> signal,
            @Nonnull final STATE receiverState) {
        final var propagationTime = signal.getPropagationTime(receiverState);

        assertInvariants(signal);
        assertNotNull(propagationTime, "Not null, propagationTime");// guard
        assertThat("Nonnegative", propagationTime, greaterThan(Duration.ZERO));

        return propagationTime;
    }

}
