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
import uk.badamson.dbc.assertions.ObjectVerifier;
import uk.badamson.mc.history.ConstantValueHistory;
import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class SignalTest {

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);
    private static final Actor<Integer> ACTOR_A = new Actor<>(WHEN_A, 0);
    private static final Actor<Integer> ACTOR_B = new Actor<>(WHEN_B, 1);

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal) {
        ObjectVerifier.assertInvariants(signal);// inherited

        final var receiver = signal.getReceiver();
        final var sender = signal.getSender();
        final var whenSent = signal.getWhenSent();
        assertAll("Not null", () -> assertNotNull(receiver, "receiver"),
                () -> assertNotNull(sender, "sender"),
                () -> assertNotNull(whenSent, "whenSent"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal1,
                                                @Nonnull final Signal<STATE> signal2) {
        ObjectVerifier.assertInvariants(signal1, signal2);// inherited
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
            assertThat("Non-negative propagationDelay", propagationDelay, greaterThan(Duration.ZERO));
            assertThat(
                    "The reception time is after the sending time, unless the sending time is the maximum possible value.",
                    whenReceived, either(greaterThan(whenSent)).or(is(Signal.NEVER_RECEIVED)));
            if (whenReceived.compareTo(Signal.NEVER_RECEIVED) < 0) {
                assertThat(
                        "If the interval between the sending time and the maximum possible value is less than the propagation delay, the reception time is the sending time plus the propagation delay.",
                        whenReceived, is(whenSent.plus(propagationDelay)));
            }
        }
    }

    private static Signal<Integer> constructor(@Nonnull final Actor<Integer> sender, @Nonnull final Duration whenSent, @Nonnull final Actor<Integer> receiver) {
        final Signal<Integer> signal = new TestSignal(sender, whenSent, receiver);

        assertInvariants(signal);
        assertAll("Attributes",
                () -> assertSame(sender, signal.getSender(), "sender"),
                () -> assertSame(whenSent, signal.getWhenSent(), "whenSent"),
        () -> assertSame(receiver, signal.getReceiver(), "receiver")
        );
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

    public static <STATE> Event<STATE> receive(@Nonnull final Signal<STATE> signal, @Nonnull final STATE receiverState)
            throws Signal.UnreceivableSignalException {
        final Event<STATE> effect;
        try {
            effect = signal.receive(receiverState);
        } catch (final Signal.UnreceivableSignalException e) {
            assertInvariants(signal);
            throw e;
        }

        assertNotNull(effect, "Not null, effect");// guard
        assertInvariants(signal);
        EventTest.assertInvariants(effect);
        final var whenOccurred = effect.getWhen();
        assertAll("event", () -> assertSame(signal, effect.getCausingSignal(), "causingSignal"),
                () -> assertEquals(signal.getReceiver(), effect.getAffectedObject(), "affectedObject"),
                () -> assertThat("whenOccurred is before the maximum possible Duration value", whenOccurred,
                        lessThan(Signal.NEVER_RECEIVED)),
                () -> assertEquals(signal.getWhenReceived(receiverState), whenOccurred, "whenOccurred = whenReceived"));

        return effect;
    }

    public static class UnreceivableSignalExceptionTest {

        public static void assertInvariants(@Nonnull final Signal.UnreceivableSignalException exception) {
            ObjectVerifier.assertInvariants(exception);// inherited
        }

        @Test
        public void noArgs() {
            final var exception = new Signal.UnreceivableSignalException();
            assertInvariants(exception);
        }

        @Test
        public void string() {
            final var exception = new Signal.UnreceivableSignalException("message");
            assertInvariants(exception);
        }

        @Test
        public void cause() {
            final var exception = new Signal.UnreceivableSignalException(new IllegalStateException());
            assertInvariants(exception);
        }

        @Test
        public void stringAndCause() {
            final var exception = new Signal.UnreceivableSignalException("message", new IllegalStateException());
            assertInvariants(exception);
        }
    }// class

    static class TestSignal extends Signal<Integer> {

        private final boolean strobe;

        TestSignal(@Nonnull final Actor<Integer> sender, @Nonnull final Duration whenSent, @Nonnull final Actor<Integer> receiver, final boolean strobe) {
            super(sender, whenSent, receiver);
            this.strobe = strobe;
        }

        TestSignal(@Nonnull final Actor<Integer> sender, @Nonnull final Duration whenSent, @Nonnull final Actor<Integer> receiver) {
            this(sender, whenSent, receiver, false);
        }

        @Override
        @Nonnull
        @Nonnegative
        protected Duration getPropagationDelay(@Nonnull final Integer receiverState) {
            Objects.requireNonNull(receiverState, "receiverState");
            return Duration.ofSeconds(Integer.max(1, receiverState));
        }

        @Nonnull
        @Override
        protected Event<Integer> receive(@Nonnull final Duration when, @Nonnull final Integer receiverState)
                throws Signal.UnreceivableSignalException {
            Objects.requireNonNull(when, "when");
            Objects.requireNonNull(receiverState, "receiverState");

            if (when.compareTo(getWhenSent()) <= 0) {
                throw new IllegalArgumentException("when not after whenSent");
            }
            final var receiver = getReceiver();
            final Set<Signal<Integer>> signalsEmitted;
            if (strobe) {
                final Signal<Integer> signalEmitted = new TestSignal(receiver, when, receiver);
                signalsEmitted = Set.of(signalEmitted);
            } else {
                signalsEmitted = Set.of();
            }
            final Integer newState = receiverState + 1;
            return new Event<>(this, when, receiver, newState, signalsEmitted);
        }

    }// class

    @Nested
    public class Constructor {

        @Test
        public void a() {
            final var signal = constructor(ACTOR_A, WHEN_A, ACTOR_B);

            assertInvariants(signal, 0);
            assertInvariants(signal, Integer.MAX_VALUE);
            assertInvariants(signal, (Integer) null);
        }

        @Test
        public void b() {
            constructor(ACTOR_B, WHEN_B, ACTOR_A);
        }

        @Test
        public void endOfTime() {
            final var signal = constructor(ACTOR_A, Signal.NEVER_RECEIVED, ACTOR_B);

            assertInvariants(signal, 0);
            assertInvariants(signal, Integer.MAX_VALUE);
        }

        @Test
        public void reflexive() {
            final Actor<Integer> actor = ACTOR_A;
            constructor(actor, WHEN_A, actor);
        }

        @Test
        public void two() {
            final Signal<Integer> signalA = new TestSignal(ACTOR_A, WHEN_A, ACTOR_B);
            final Signal<Integer> signalB = new TestSignal(ACTOR_B, WHEN_B, ACTOR_A);

            assertInvariants(signalA, signalB);
            assertNotEquals(signalA, signalB);
        }

    }// class

    @Nested
    public class Receive {

        @Test
        public void a() {
            test(ACTOR_A, WHEN_A, ACTOR_B, 0);
        }

        @Test
        public void atEndOfTime() {
            assertThrows(Signal.UnreceivableSignalException.class,
                    () -> test(ACTOR_A, Signal.NEVER_RECEIVED, ACTOR_B, Integer.MAX_VALUE)
            );
        }

        @Test
        public void b() {
            test(ACTOR_B, WHEN_B, ACTOR_A, 1);
        }

        private void test(
                @Nonnull final Actor<Integer> sender,
                @Nonnull final Duration whenSent,
                @Nonnull final Actor<Integer> receiver,
                @Nonnull final Integer receiverState)
                throws Signal.UnreceivableSignalException {
            final var signal = new TestSignal(sender, whenSent, receiver);
            receive(signal, receiverState);
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
                    test(WHEN_A, 1);
                }

                @Test
                public void alwaysDestroyed() {
                    test(WHEN_A, null);
                }

                @Test
                public void b() {
                    test(WHEN_B, 2);
                }

                private void test(@Nonnull final Duration whenSet, @Nullable final Integer receiverState) {
                    final var signal = new TestSignal(ACTOR_A, whenSet, ACTOR_B);
                    final ValueHistory<Integer> receiverStateHistory = new ConstantValueHistory<>(receiverState);

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
                    final Integer receiverState0 = 16;
                    final Integer receiverState1 = 1;

                    test(whenSent, transitionTime, receiverState0, receiverState1);
                }

                @Test
                public void close() {
                    final Duration whenSent = WHEN_A;
                    final Duration transitionTime = whenSent.plusNanos(1);// critical
                    final Integer receiverState0 = 1;
                    final Integer receiverState1 = 100;

                    test(whenSent, transitionTime, receiverState0, receiverState1);
                }

                @Test
                public void decreasingPropagationDelay() {
                    final Duration whenSent = WHEN_A;
                    final Duration transitionTime = whenSent.plusMillis(400);
                    final Integer receiverState0 = 16;
                    final Integer receiverState1 = 4;

                    test(whenSent, transitionTime, receiverState0, receiverState1);
                }

                @Test
                public void increasingPropagationDelay() {
                    final Duration whenSent = WHEN_B;
                    final Duration transitionTime = whenSent.plusMillis(100);
                    final Integer receiverState0 = 4;
                    final Integer receiverState1 = 16;

                    test(whenSent, transitionTime, receiverState0, receiverState1);
                }

                private void test(@Nonnull final Duration whenSet, @Nonnull final Duration transitionTime,
                                  @Nonnull final Integer receiverState0, @Nonnull final Integer receiverState1) {
                    assert whenSet.compareTo(transitionTime) < 0;
                    final var signal = new TestSignal(ACTOR_A, whenSet, ACTOR_B);
                    final ModifiableValueHistory<Integer> receiverStateHistory = new ModifiableValueHistory<>(
                            receiverState0);
                    receiverStateHistory.appendTransition(transitionTime, receiverState1);

                    getWhenReceived(signal, receiverStateHistory);
                }

            }// class

        }// class

    }// class

}
