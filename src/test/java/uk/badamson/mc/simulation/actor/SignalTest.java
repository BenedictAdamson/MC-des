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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.badamson.dbc.assertions.EqualsSemanticsTest;
import uk.badamson.dbc.assertions.ObjectTest;
import uk.badamson.mc.JsonTest;
import uk.badamson.mc.history.ConstantValueHistory;
import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.simulation.TimestampedId;
import uk.badamson.mc.simulation.TimestampedIdTest;
import uk.badamson.mc.simulation.actor.Signal.UnreceivableSignalException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class SignalTest {

    static final UUID ID_A = UUID.randomUUID();
    static final UUID ID_B = UUID.randomUUID();
    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();
    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);
    private static final TimestampedId OBJECT_STATE_ID_A = new TimestampedId(OBJECT_A, WHEN_A);
    private static final TimestampedId OBJECT_STATE_ID_B = new TimestampedId(OBJECT_B, WHEN_B);

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal) {
        ObjectTest.assertInvariants(signal);// inherited

        final var id = signal.getId();
        final var receiver = signal.getReceiver();
        final var sender = signal.getSender();
        final var sentFrom = signal.getSentFrom();
        final var whenSent = signal.getWhenSent();
        assertAll("Not null", () -> assertNotNull(id, "id"), () -> assertNotNull(receiver, "receiver"),
                () -> assertNotNull(sender, "sender"), () -> assertNotNull(sentFrom, "sentFrom"), // guard
                () -> assertNotNull(whenSent, "whenSent"));
        TimestampedIdTest.assertInvariants(sentFrom);
        assertAll("consistent attributes", () -> assertSame(sender, sentFrom.getObject(), "sender with SentFrom"),
                () -> assertSame(whenSent, sentFrom.getWhen(), "whenSent with SentFrom"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal1,
                                                @Nonnull final Signal<STATE> signal2) {
        ObjectTest.assertInvariants(signal1, signal2);// inherited

        EqualsSemanticsTest.assertEntitySemantics(signal1, signal2, Signal::getId);
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

    private static Signal<Integer> constructor(@Nonnull final UUID id, @Nonnull final TimestampedId sentFrom,
                                               @Nonnull final UUID receiver) {
        final Signal<Integer> signal = new TestSignal(id, sentFrom, receiver);

        assertInvariants(signal);
        assertAll("Attributes", () -> assertSame(id, signal.getId(), "id"),
                () -> assertSame(sentFrom, signal.getSentFrom(), "sentFrom"),
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

    public static <STATE> Event<STATE> receive(@Nonnull final Signal<STATE> signal, @Nonnull final STATE receiverState)
            throws UnreceivableSignalException {
        final Event<STATE> effect;
        try {
            effect = signal.receive(receiverState);
        } catch (final UnreceivableSignalException e) {
            assertInvariants(signal);
            throw e;
        }

        assertNotNull(effect, "Not null, effect");// guard
        assertInvariants(signal);
        EventTest.assertInvariants(effect);
        final var whenOccurred = effect.getWhenOccurred();
        assertAll("event", () -> assertSame(signal.getId(), effect.getCausingSignal(), "causingSignal"),
                () -> assertEquals(signal.getReceiver(), effect.getAffectedObject(), "affectedObject"),
                () -> assertThat("whenOccurred is before the maximum possible Duration value", whenOccurred,
                        lessThan(Signal.NEVER_RECEIVED)),
                () -> assertEquals(signal.getWhenReceived(receiverState), whenOccurred, "whenOccurred = whenReceived"));

        return effect;
    }

    public static class UnreceivableSignalExceptionTest {

        public static void assertInvariants(@Nonnull final Signal.UnreceivableSignalException exception) {
            ObjectTest.assertInvariants(exception);// inherited
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

        @JsonCreator
        TestSignal(@Nonnull @JsonProperty("id") final UUID id,
                   @Nonnull @JsonProperty("sentFrom") final TimestampedId sentFrom,
                   @Nonnull @JsonProperty("receiver") final UUID receiver) {
            super(id, sentFrom, receiver);
            strobe = false;
        }

        TestSignal(@Nonnull final UUID id, @Nonnull final TimestampedId sentFrom, @Nonnull final UUID receiver,
                   final boolean strobe) {
            super(id, sentFrom, receiver);
            this.strobe = strobe;
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
                throws UnreceivableSignalException {
            Objects.requireNonNull(when, "when");
            Objects.requireNonNull(receiverState, "receiverState");

            if (when.compareTo(getWhenSent()) <= 0) {
                throw new IllegalArgumentException("when not after whenSent");
            }
            final Integer newState = receiverState + 1;
            final Set<Signal<Integer>> signalsEmitted;
            final var eventId = new TimestampedId(getId(), when);
            final var receiver = getReceiver();
            if (strobe) {
                final UUID emittedSignalId = UUID.randomUUID();
                final TimestampedId sentFrom = new TimestampedId(receiver, when);
                final Signal<Integer> signalEmitted = new TestSignal(emittedSignalId, sentFrom, receiver, true);
                signalsEmitted = Set.of(signalEmitted);
            } else {
                signalsEmitted = Set.of();
            }
            return new Event<>(eventId, receiver, newState, signalsEmitted);
        }

    }// class

    @Nested
    public class Constructor {

        @Test
        public void a() {
            final var signal = constructor(ID_A, OBJECT_STATE_ID_A, OBJECT_B);

            assertInvariants(signal, 0);
            assertInvariants(signal, Integer.MAX_VALUE);
            assertInvariants(signal, (Integer) null);
        }

        @Test
        public void b() {
            constructor(ID_B, OBJECT_STATE_ID_B, OBJECT_A);
        }

        @Test
        public void endOfTime() {
            final var sentFrom = new TimestampedId(OBJECT_A, Signal.NEVER_RECEIVED);
            final var signal = constructor(ID_A, sentFrom, OBJECT_B);

            assertInvariants(signal, 0);
            assertInvariants(signal, Integer.MAX_VALUE);
        }

        @Test
        public void reflexive() {
            constructor(ID_A, OBJECT_STATE_ID_A, OBJECT_A);
        }

        @Nested
        public class Two {

            @Test
            public void different() {
                // Tough test: non ID attributes are the same
                final Signal<Integer> signalA = new TestSignal(ID_A, OBJECT_STATE_ID_A, OBJECT_A);
                final Signal<Integer> signalB = new TestSignal(ID_B, OBJECT_STATE_ID_A, OBJECT_A);

                assertInvariants(signalA, signalB);
                assertNotEquals(signalA, signalB);
            }

            @Test
            public void equivalent() {
                // Tough test: non ID attributes are the different
                final Signal<Integer> signalA = new TestSignal(ID_A, OBJECT_STATE_ID_A, OBJECT_A);
                final Signal<Integer> signalB = new TestSignal(ID_A, OBJECT_STATE_ID_B, OBJECT_B);

                assertInvariants(signalA, signalB);
                assertEquals(signalA, signalB);
            }

        }// class

    }// class

    @Nested
    public class JSON {

        @Test
        public void a() {
            test(ID_A, OBJECT_STATE_ID_A, OBJECT_A);
        }

        @Test
        public void b() {
            test(ID_B, OBJECT_STATE_ID_B, OBJECT_B);
        }

        private void test(@Nonnull final UUID id, @Nonnull final TimestampedId sentFrom,
                          @Nonnull final UUID receiver) {
            final var signal = new TestSignal(id, sentFrom, receiver);
            final var deserialized = JsonTest.serializeAndDeserialize(signal);

            assertInvariants(signal);
            assertInvariants(signal, deserialized);
            assertAll(() -> assertThat("equals", deserialized, is(signal)),
                    () -> assertEquals(signal.getId(), deserialized.getId(), "id"),
                    () -> assertEquals(signal.getSentFrom(), deserialized.getSentFrom(), "sentFrom"),
                    () -> assertEquals(signal.getReceiver(), deserialized.getReceiver(), "receiver"));
        }
    }// class

    @Nested
    public class Receive {

        @Test
        public void a() {
            test(ID_A, OBJECT_STATE_ID_A, OBJECT_B, 0, false);
        }

        @Test
        public void atEndOfTime() {
            final var whenSent = Signal.NEVER_RECEIVED;// critical
            final var sentFrom = new TimestampedId(OBJECT_A, whenSent);

            test(ID_A, sentFrom, OBJECT_A, Integer.MAX_VALUE, true);
        }

        @Test
        public void b() {
            test(ID_B, OBJECT_STATE_ID_B, OBJECT_A, 1, false);
        }

        private void test(@Nonnull final UUID id, @Nonnull final TimestampedId sentFrom, @Nonnull final UUID receiver,
                          @Nonnull final Integer receiverState, final boolean expectUnreceivableSignalException) {
            final var signal = new TestSignal(id, sentFrom, receiver);

            try {
                receive(signal, receiverState);
            } catch (final UnreceivableSignalException e) {
                if (!expectUnreceivableSignalException) {
                    throw new AssertionError("Throws UnreceivableSignalException only as specified", e);
                }
                // else OK
            }
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
                    final var signal = new TestSignal(ID_A, new TimestampedId(OBJECT_A, whenSet), OBJECT_B);
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
                    final var signal = new TestSignal(ID_A, new TimestampedId(OBJECT_A, whenSet), OBJECT_B);
                    final ModifiableValueHistory<Integer> receiverStateHistory = new ModifiableValueHistory<>(
                            receiverState0);
                    receiverStateHistory.appendTransition(transitionTime, receiverState1);

                    getWhenReceived(signal, receiverStateHistory);
                }

            }// class

        }// class

    }// class

}
