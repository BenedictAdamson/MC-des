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
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
import uk.badamson.dbc.assertions.EqualsSemanticsTest;
import uk.badamson.dbc.assertions.ObjectTest;
import uk.badamson.mc.history.ConstantValueHistory;
import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.simulation.ObjectStateId;
import uk.badamson.mc.simulation.ObjectStateIdTest;
import uk.badamson.mc.simulation.actor.Signal.UnreceivableSignalException;

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

        }// class

        @Test
        public void a() {
            final var signal = constructor(ID_A, OBJECT_B);

            assertInvariants(signal, Integer.valueOf(0));
            assertInvariants(signal, Integer.valueOf(Integer.MAX_VALUE));
            assertInvariants(signal, (Integer) null);
        }

        @Test
        public void b() {
            constructor(ID_B, OBJECT_A);
        }

        @Test
        public void endOfTime() {
            final var id = new ObjectStateId(OBJECT_A, Signal.NEVER_RECEIVED);
            final var signal = constructor(id, OBJECT_B);

            assertInvariants(signal, Integer.valueOf(0));
            assertInvariants(signal, Integer.valueOf(Integer.MAX_VALUE));
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
                assertEquivalent(effectA, effectB);
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
                assert eventIdA != eventIdB;// tough test
                assert stateA != stateB;// tough test
                assert signalsEmittedA != signalsEmittedB;// tough test

                final var effectA = new Signal.Effect<>(eventIdA, stateA, signalsEmittedA);
                final var effectB = new Signal.Effect<>(eventIdB, stateB, signalsEmittedB);

                assertInvariants(effectA, effectB);
                assertEquivalent(effectA, effectB);
            }
        }// class

        private static <STATE> void assertEquivalent(@Nonnull final Signal.Effect<STATE> effect1,
                @Nonnull final Signal.Effect<STATE> effect2) {
            assertAll(() -> assertEquals(effect1.getEventId(), effect2.getEventId(), "eventId"),
                    () -> assertEquals(effect1.getState(), effect2.getState(), "state"));
        }

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

    @Nested
    public class Receive {

        @Test
        public void a() {
            test(ID_A, OBJECT_B, Integer.valueOf(0), false);
        }

        @Test
        public void atEndOfTime() {
            final var whenSent = Signal.NEVER_RECEIVED;// critical
            final var sentFrom = new ObjectStateId(OBJECT_A, whenSent);

            test(sentFrom, OBJECT_A, Integer.valueOf(Integer.MAX_VALUE), true);
        }

        @Test
        public void b() {
            test(ID_B, OBJECT_A, Integer.valueOf(1), false);
        }

        private void test(@Nonnull final ObjectStateId sentFrom, @Nonnull final UUID receiver,
                @Nonnull final Integer receiverState, final boolean expectUnreceivableSignalException) {
            final var signal = new TestSignal(sentFrom, receiver);

            try {
                receive(signal, receiverState);
            } catch (final UnreceivableSignalException e) {
                if (expectUnreceivableSignalException) {
                    return;// OK
                } else {
                    throw new AssertionError("Throws UnreceivableSignalException only as specified", e);
                }
            }
        }

    }// class

    static class TestSignal extends Signal<Integer> {

        TestSignal(@Nonnull final ObjectStateId sentFrom, @Nonnull final UUID receiver) {
            super(sentFrom, receiver);
        }

        @Override
        @Nonnull
        @Nonnegative
        protected Duration getPropagationDelay(@Nonnull final Integer receiverState) {
            Objects.requireNonNull(receiverState, "receiverState");
            return Duration.ofSeconds(Integer.max(1, receiverState.intValue()));
        }

        @Override
        protected Signal.Effect<Integer> receive(@Nonnull final Duration when, @Nonnull final Integer receiverState)
                throws UnreceivableSignalException {
            Objects.requireNonNull(when, "when");
            Objects.requireNonNull(receiverState, "receiverState");
            if (when.compareTo(getWhenSent()) <= 0) {
                throw new IllegalArgumentException("when not after whenSent");
            }
            final Integer newState = Integer.valueOf(receiverState.intValue() + 1);
            final Set<Signal<Integer>> signalsEmitted = Set.of();
            return new Signal.Effect<>(new ObjectStateId(getReceiver(), when), newState, signalsEmitted);
        }

    }// class

    @Nested
    public class WhenReceived {

        @Nested
        public class ForHistory {

            @Nested
            public class Constant {
                @Test
                public void a() {
                    test(WHEN_A, Integer.valueOf(1));
                }

                @Test
                public void alwaysDestroyed() {
                    test(WHEN_A, null);
                }

                @Test
                public void b() {
                    test(WHEN_B, Integer.valueOf(2));
                }

                private void test(@Nonnull final Duration whenSet, @Nonnull final Integer receiverState) {
                    final var signal = new TestSignal(new ObjectStateId(OBJECT_A, whenSet), OBJECT_B);
                    final ValueHistory<Integer> receiverStateHistory = new ConstantValueHistory<Integer>(receiverState);

                    final var whenReceived = getWhenReceived(signal, receiverStateHistory);

                    assertEquals(signal.getWhenReceived(receiverState), whenReceived, "when received");
                }

            }// class

            @Nested
            public class OneChange {

                @Test
                public void arriveAtTransition() {
                    final Duration whenSent = WHEN_A;
                    final Duration transitionTime = whenSent.plusSeconds(4);
                    final Integer receiverState0 = Integer.valueOf(16);
                    final Integer receiverState1 = Integer.valueOf(1);

                    test(whenSent, transitionTime, receiverState0, receiverState1);
                }

                @Test
                public void close() {
                    final Duration whenSent = WHEN_A;
                    final Duration transitionTime = whenSent.plusNanos(1);// critical
                    final Integer receiverState0 = Integer.valueOf(1);
                    final Integer receiverState1 = Integer.valueOf(100);

                    test(whenSent, transitionTime, receiverState0, receiverState1);
                }

                @Test
                public void decreasingPropagationDelay() {
                    final Duration whenSent = WHEN_A;
                    final Duration transitionTime = whenSent.plusMillis(400);
                    final Integer receiverState0 = Integer.valueOf(16);
                    final Integer receiverState1 = Integer.valueOf(4);

                    test(whenSent, transitionTime, receiverState0, receiverState1);
                }

                @Test
                public void increasingPropagationDelay() {
                    final Duration whenSent = WHEN_B;
                    final Duration transitionTime = whenSent.plusMillis(100);
                    final Integer receiverState0 = Integer.valueOf(4);
                    final Integer receiverState1 = Integer.valueOf(16);

                    test(whenSent, transitionTime, receiverState0, receiverState1);
                }

                private void test(@Nonnull final Duration whenSet, @Nonnull final Duration transitionTime,
                        @Nonnull final Integer receiverState0, @Nonnull final Integer receiverState1) {
                    assert whenSet.compareTo(transitionTime) < 0;
                    final var signal = new TestSignal(new ObjectStateId(OBJECT_A, whenSet), OBJECT_B);
                    final ModifiableValueHistory<Integer> receiverStateHistory = new ModifiableValueHistory<Integer>(
                            receiverState0);
                    receiverStateHistory.appendTransition(transitionTime, receiverState1);

                    getWhenReceived(signal, receiverStateHistory);
                }

            }// class

        }// class

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

        assertAll("value semantics",
                () -> EqualsSemanticsTest.assertValueSemantics(signal1, signal2, "receiver", s -> s.getReceiver()),
                () -> EqualsSemanticsTest.assertValueSemantics(signal1, signal2, "sentFrom", s -> s.getSentFrom()));
    }

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal,
            @Nullable final STATE receiverState) {
        final var whenSent = signal.getWhenSent();
        final var whenReceived = signal.getWhenReceived(receiverState);

        assertInvariants(signal);
        if (receiverState == null) {
            assertThat("destroyed objects can not receive signals", whenReceived, is(Signal.NEVER_RECEIVED));
        } else {
            final var propagationDelay = signal.getPropagationDelay(receiverState);

            assertAll("Not null", () -> assertNotNull(propagationDelay, "propagationDelay"), // guard
                    () -> assertNotNull(whenReceived, "whenReceived"));// guard
            assertThat("Nonnegative propogationDelay", propagationDelay, greaterThan(Duration.ZERO));
            assertThat(
                    "The reception time is after the sending time, unless the sending time is the maximum possible value.",
                    whenReceived, either(greaterThan(whenSent)).or(is(Signal.NEVER_RECEIVED)));
            if (whenReceived.compareTo(Signal.NEVER_RECEIVED) < 0) {
                assertThat(
                        "If the interval between the sending time and the maximum possible value is less than thepropagation delay, the reception time is the sending time plus the propagation delay.",
                        whenReceived, is(whenSent.plus(propagationDelay)));
            }
        }
    }

    private static Signal<Integer> constructor(@Nonnull final ObjectStateId sentFrom, @Nonnull final UUID receiver) {
        final Signal<Integer> signal = new TestSignal(sentFrom, receiver);

        assertInvariants(signal);
        assertAll("Attributes", () -> assertSame(sentFrom, signal.getSentFrom(), "sentFrom"),
                () -> assertSame(receiver, signal.getReceiver(), "receiver"));
        return signal;
    }

    public static <STATE> Duration getWhenReceived(@Nonnull final Signal<STATE> signal,
            @Nonnull final ValueHistory<STATE> receiverStateHistory) {
        final var whenReceived = signal.getWhenReceived(receiverStateHistory);

        assertNotNull(whenReceived, "Not null, whenReceived");// guard
        assertInvariants(signal);
        final var stateWhenReceived = receiverStateHistory.get(whenReceived);
        assertThat(
                "The reception time is after the sending time, unless the sending time is the maximum possible value.",
                whenReceived, either(greaterThan(signal.getWhenSent())).or(is(Signal.NEVER_RECEIVED)));
        assertFalse(stateWhenReceived == null && !Signal.NEVER_RECEIVED.equals(whenReceived),
                "If the simulated object is destroyed or removed it can not receive a signal.");
        assertThat("The reception time is consistent with the receiver history",
                signal.getWhenReceived(stateWhenReceived), lessThanOrEqualTo(whenReceived));

        return whenReceived;
    }

    public static <STATE> Signal.Effect<STATE> receive(@Nonnull final Signal<STATE> signal,
            @Nonnull final STATE receiverState) throws UnreceivableSignalException {
        final Signal.Effect<STATE> effect;
        try {
            effect = signal.receive(receiverState);
        } catch (final UnreceivableSignalException e) {
            assertInvariants(signal);
            throw e;
        }

        assertNotNull(effect, "Not null, effect");// guard
        assertInvariants(signal);
        EffectTest.assertInvariants(effect);
        final var whenOccurred = effect.getWhenOccurred();
        assertAll("effect", () -> assertEquals(signal.getReceiver(), effect.getAffectedObject(), "affectedObject"),
                () -> assertThat("whenOccurred is before the maximum possible Duration value", whenOccurred,
                        lessThan(Signal.NEVER_RECEIVED)),
                () -> assertEquals(signal.getWhenReceived(receiverState), whenOccurred, "whenOccurred = whenReceived"));

        return effect;
    }

}
