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
import uk.badamson.dbc.assertions.EqualsSemanticsVerifier;
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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class SignalTest {

    private static final Duration WHEN_A = Duration.ofMillis(0);

    private static final Duration WHEN_B = Duration.ofMillis(5000);

    private static final Duration WHEN_C = Duration.ofMillis(7000);

    private static final Actor<Integer> ACTOR_A = new Actor<>(WHEN_A, 0);

    private static final Actor<Integer> ACTOR_B = new Actor<>(WHEN_B, 1);

    private static final Medium MEDIUM_A = new Medium();

    private static final Medium MEDIUM_B = new Medium();

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal) {
        ObjectVerifier.assertInvariants(signal);// inherited

        final var id = signal.getId();
        final var receiver = signal.getReceiver();
        final var sender = signal.getSender();
        final var whenSent = signal.getWhenSent();
        assertAll(
                () -> assertThat("id", id, notNullValue()),
                () -> assertThat("receiver", receiver, notNullValue()),
                () -> assertThat("whenSent", whenSent, notNullValue()));
        assertAll(
                () -> assertThat("receiver", receiver, sameInstance(id.getReceiver())),
                () -> assertThat("sender", sender, sameInstance(id.getSender())),
                () -> assertThat("whenSent", whenSent, sameInstance(id.getWhenSent()))
        );
    }

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal1,
                                                @Nonnull final Signal<STATE> signal2) {
        ObjectVerifier.assertInvariants(signal1, signal2);// inherited
        EqualsSemanticsVerifier.assertEntitySemantics(signal1, signal2, Signal::getId);
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

            assertAll(() -> assertThat("propagationDelay", propagationDelay, notNullValue()),
                    () -> assertThat("whenReceived", whenReceived, notNullValue()));
            assertAll(
                    () -> assertThat("propagationDelay", propagationDelay, greaterThan(Duration.ZERO)),
                    () -> assertThat(
                            "The reception time is after the sending time, unless the sending time is the maximum possible value.",
                            whenReceived, either(greaterThan(whenSent)).or(is(Signal.NEVER_RECEIVED))));
            if (whenReceived.compareTo(Signal.NEVER_RECEIVED) < 0) {
                assertThat(
                        "If the interval between the sending time and the maximum possible value is less than the propagation delay, the reception time is the sending time plus the propagation delay.",
                        whenReceived, is(whenSent.plus(propagationDelay)));
            }
        }
    }

    private static Signal<Integer> constructor(
            @Nullable final Actor<Integer> sender, @Nonnull final Duration whenSent,
            @Nonnull final Actor<Integer> receiver, @Nonnull final Medium medium) {
        final Signal<Integer> signal = new SimpleTestSignal(whenSent, sender, receiver, medium);

        assertInvariants(signal);
        assertAll(
                () -> assertThat("sender", signal.getSender(), sameInstance(sender)),
                () -> assertThat("whenSent", signal.getWhenSent(), sameInstance(whenSent)),
                () -> assertThat("receiver", signal.getReceiver(), sameInstance(receiver)),
                () -> assertThat("medium", signal.getMedium(), sameInstance(medium))
        );
        return signal;
    }

    public static <STATE> Duration getWhenReceived(@Nonnull final Signal<STATE> signal,
                                                   @Nonnull final ValueHistory<STATE> receiverStateHistory) {
        final var whenReceived = signal.getWhenReceived(receiverStateHistory);

        assertThat(whenReceived, notNullValue());
        assertInvariants(signal);
        final var stateWhenReceived = receiverStateHistory.get(whenReceived);
        assertAll("whenReceived",
                () -> assertThat(
                        "after the sending time, unless the sending time is the maximum possible value.",
                        whenReceived, either(greaterThan(signal.getWhenSent())).or(is(Signal.NEVER_RECEIVED))),
                () -> assertThat("If the simulated object is destroyed or removed it can not receive a signal.",
                        stateWhenReceived == null && !Signal.NEVER_RECEIVED.equals(whenReceived),
                        is(false)),
                () -> assertThat("is consistent with the receiver history",
                        signal.getWhenReceived(stateWhenReceived), lessThanOrEqualTo(whenReceived)));

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

        assertThat(effect, notNullValue());
        assertInvariants(signal);
        EventTest.assertInvariants(effect);
        final var whenOccurred = effect.getWhen();
        assertAll("event", () -> assertThat("causingSignal", effect.getCausingSignal(), sameInstance(signal)),
                () -> assertThat("affectedObject", effect.getAffectedObject(), sameInstance(signal.getReceiver())),
                () -> assertThat("whenOccurred is before the maximum possible Duration value", whenOccurred,
                        lessThan(Signal.NEVER_RECEIVED)),
                () -> assertThat("whenOccurred = whenReceived", signal.getWhenReceived(receiverState), is(whenOccurred)));

        return effect;
    }

    public static <STATE> Event<STATE> receiveForStateHistory(@Nonnull final Signal<STATE> signal, @Nonnull final ValueHistory<STATE> receiverStateHistory)
            throws Signal.UnreceivableSignalException {
        final Event<STATE> effect;
        try {
            effect = signal.receiveForStateHistory(receiverStateHistory);
        } catch (final Signal.UnreceivableSignalException e) {
            assertInvariants(signal);
            throw e;
        }

        assertThat(effect, notNullValue());
        assertInvariants(signal);
        EventTest.assertInvariants(effect);
        final var whenOccurred = effect.getWhen();
        assertAll("event", () -> assertThat("causingSignal", effect.getCausingSignal(), sameInstance(signal)),
                () -> assertThat("affectedObject", effect.getAffectedObject(), sameInstance(signal.getReceiver())),
                () -> assertThat("whenOccurred is before the maximum possible Duration value", whenOccurred,
                        lessThan(Signal.NEVER_RECEIVED)),
                () -> assertThat("whenOccurred = whenReceived", signal.getWhenReceived(receiverStateHistory), is(whenOccurred)));

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
    }

    static final class ThrowingSignal extends Signal<Integer> {

        ThrowingSignal(@Nonnull final Duration whenSent, @Nullable final Actor<Integer> sender, @Nonnull final Actor<Integer> receiver, @Nonnull final Medium medium) {
            super(whenSent, sender, receiver, medium);
        }

        @Nonnull
        @Override
        protected Duration getPropagationDelay(@Nonnull final Integer receiverState) {
            return Duration.ofSeconds(1L);
        }

        @Nonnull
        @Override
        protected Event<Integer> receive(@Nonnull final Duration when, @Nonnull final Integer receiverState) throws InevitableException {
            throw new InevitableException();
        }

        static final class InevitableException extends RuntimeException {
            InevitableException() {
                super("Inevitable thrown exception");
            }
        }
    }

    static abstract class AbstractTestSignal extends Signal<Integer> {

        protected AbstractTestSignal(
                @Nullable final Actor<Integer> sender, @Nonnull final Duration whenSent, @Nonnull final Actor<Integer> receiver, @Nonnull final Medium medium) {
            super(whenSent, sender, receiver, medium);
        }

        @Override
        @Nonnull
        @Nonnegative
        protected final Duration getPropagationDelay(@Nonnull final Integer receiverState) {
            Objects.requireNonNull(receiverState, "receiverState");
            return Duration.ofSeconds(Integer.max(1, receiverState));
        }

        @Nonnull
        @Override
        protected final Event<Integer> receive(@Nonnull final Duration when, @Nonnull final Integer receiverState)
                throws Signal.UnreceivableSignalException {
            Objects.requireNonNull(when, "when");
            Objects.requireNonNull(receiverState, "receiverState");

            if (when.compareTo(getWhenSent()) <= 0) {
                throw new IllegalArgumentException("when not after whenSent");
            }
            final Integer newState = receiverState + 1;
            return new Event<>(
                    this, when, newState,
                    signalsEmitted(when), actorsCreated(when)
            );
        }

        protected abstract Set<Signal<Integer>> signalsEmitted(@Nonnull final Duration when);

        protected Set<Actor<Integer>> actorsCreated(@Nonnull final Duration when) {
            return Set.of();
        }
    }

    static class SimpleTestSignal extends AbstractTestSignal {

        SimpleTestSignal(
                @Nonnull final Duration whenSent,
                @Nullable final Actor<Integer> sender, @Nonnull final Actor<Integer> receiver,
                @Nonnull final Medium medium) {
            super(sender, whenSent, receiver, medium);
        }

        @Override
        protected Set<Signal<Integer>> signalsEmitted(@Nonnull final Duration when) {
            return Set.of();
        }
    }

    static class StrobingTestSignal extends AbstractTestSignal {

        StrobingTestSignal(
                @Nonnull final Duration whenSent,
                @Nonnull final Actor<Integer> sender, @Nonnull final Actor<Integer> receiver,
                @Nonnull final Medium medium) {
            super(sender, whenSent, receiver, medium);
        }

        @Override
        protected Set<Signal<Integer>> signalsEmitted(@Nonnull final Duration when) {
            return Set.of(new StrobingTestSignal(when, getReceiver(), getReceiver(), getMedium()));
        }

    }

    static class EchoingTestSignal extends AbstractTestSignal {

        EchoingTestSignal(
                @Nonnull final Duration whenSent,
                @Nonnull final Actor<Integer> sender, @Nonnull final Actor<Integer> receiver,
                @Nonnull final Medium medium) {
            super(sender, whenSent, receiver, medium);
            Objects.requireNonNull(sender, "sender");
        }

        @Override
        protected Set<Signal<Integer>> signalsEmitted(@Nonnull final Duration when) {
            assert getSender() != null;
            return Set.of(new EchoingTestSignal(when, getReceiver(), getSender(), getMedium()));
        }

    }

    static class ActorCreatingTestSignal extends AbstractTestSignal {

        ActorCreatingTestSignal(
                @Nonnull final Duration whenSent,
                @Nonnull final Actor<Integer> sender, @Nonnull final Actor<Integer> receiver,
                @Nonnull final Medium medium) {
            super(sender, whenSent, receiver, medium);
        }

        @Override
        protected Set<Signal<Integer>> signalsEmitted(@Nonnull final Duration when) {
            return Set.of();
        }

        @Override
        protected Set<Actor<Integer>> actorsCreated(@Nonnull final Duration when) {
            final int state = when.toSecondsPart();
            final Actor<Integer> actor = new Actor<>(when, state);
            return Set.of(actor);
        }
    }

    static class InteractingActorCreatingTestSignal extends Signal<Integer> {

        InteractingActorCreatingTestSignal(
                @Nonnull final Duration whenSent,
                @Nullable final Actor<Integer> sender, @Nonnull final Actor<Integer> receiver,
                @Nonnull final Medium medium) {
            super(whenSent, sender, receiver, medium);
        }

        @Nonnull
        @Override
        protected Duration getPropagationDelay(@Nonnull final Integer receiverState) {
            return Duration.ofSeconds(1 + Math.abs(receiverState));
        }

        @Nonnull
        @Override
        protected Event<Integer> receive(
                @Nonnull final Duration when, @Nonnull final Integer receiverState
        ) throws UnreceivableSignalException {
            final Integer newState = receiverState + 1;
            final Actor<Integer> createdActor = new Actor<>(when, receiverState - 3);
            final Signal<Integer> emittedSignal = new StrobingTestSignal(when, getReceiver(), createdActor, MEDIUM_A);
            return new Event<>(this, when, newState, Set.of(emittedSignal), Set.of(createdActor));
        }
    }

    public static class IdTest {

        public static <STATE> void assertInvariants(@Nonnull final Signal.Id<STATE> id) {
            ObjectVerifier.assertInvariants(id);
            assertAll(
                    () -> assertThat(id.getWhenSent(), notNullValue()),
                    () -> assertThat(id.getReceiver(), notNullValue()),
                    () -> assertThat(id.getMedium(), notNullValue())
            );
        }

        public static <STATE> void assertInvariants(@Nonnull final Signal.Id<STATE> id1, @Nonnull final Signal.Id<STATE> id2) {
            ObjectVerifier.assertInvariants(id1, id2);
            assertAll(
                    () -> EqualsSemanticsVerifier.assertValueSemantics(id1, id2, "whenSent", Signal.Id::getWhenSent),
                    () -> EqualsSemanticsVerifier.assertValueSemantics(id1, id2, "sender", Signal.Id::getSender),
                    () -> EqualsSemanticsVerifier.assertValueSemantics(id1, id2, "receiver", Signal.Id::getReceiver),
                    () -> EqualsSemanticsVerifier.assertValueSemantics(id1, id2, "medium", Signal.Id::getMedium)
            );
        }

        @Nested
        public class One {

            @Test
            public void a() {
                test(WHEN_A, ACTOR_A, ACTOR_B, MEDIUM_A);
            }

            @Test
            public void b() {
                test(WHEN_B, ACTOR_B, ACTOR_A, MEDIUM_B);
            }

            @Test
            public void nullSender() {
                test(WHEN_A, null, ACTOR_B, MEDIUM_A);
            }

            private <STATE> void test(
                    @Nonnull final Duration whenSent,
                    @Nullable final Actor<STATE> sender, @Nonnull final Actor<STATE> receiver,
                    @Nonnull final Medium medium
            ) {
                final var id = new Signal.Id<>(whenSent, sender, receiver, medium);
                assertInvariants(id);
                assertAll(
                        () -> assertThat(id.getWhenSent(), sameInstance(whenSent)),
                        () -> assertThat(id.getSender(), sameInstance(sender)),
                        () -> assertThat(id.getReceiver(), sameInstance(receiver)),
                        () -> assertThat(id.getMedium(), sameInstance(medium))
                );
            }
        }

        @Nested
        public class Two {

            @Test
            public void equal() {
                final var idA = new Signal.Id<>(WHEN_A, ACTOR_A, ACTOR_B, MEDIUM_A);
                final var idB = new Signal.Id<>(WHEN_A, ACTOR_A, ACTOR_B, MEDIUM_A);
                assertInvariants(idA, idB);
                assertThat(idA, is(idB));
            }

            @Test
            public void equalNullSenders() {
                final var idA = new Signal.Id<>(WHEN_A, null, ACTOR_B, MEDIUM_A);
                final var idB = new Signal.Id<>(WHEN_A, null, ACTOR_B, MEDIUM_A);
                assertInvariants(idA, idB);
                assertThat(idA, is(idB));
            }

            @Test
            public void differentWhenSent() {
                testDifferent(
                        WHEN_B, ACTOR_A, ACTOR_B, MEDIUM_A
                );
            }

            @Test
            public void differentSender() {
                testDifferent(
                        WHEN_A, ACTOR_B, ACTOR_B, MEDIUM_A
                );
            }

            @Test
            public void differentThatSenderNull() {
                testDifferent(
                        WHEN_A, null, ACTOR_B, MEDIUM_A
                );
            }

            @Test
            public void differentThisSenderNull() {
                final var idA = new Signal.Id<>(WHEN_A, null, ACTOR_B, MEDIUM_A);
                final var idB = new Signal.Id<>(WHEN_A, ACTOR_A, ACTOR_B, MEDIUM_A);
                assertInvariants(idA, idB);
                assertThat(idA, not(idB));
            }

            @Test
            public void differentReceiver() {
                testDifferent(
                        WHEN_A, ACTOR_A, ACTOR_A, MEDIUM_A
                );
            }

            @Test
            public void differentMedium() {
                testDifferent(
                        WHEN_A, ACTOR_A, ACTOR_B, MEDIUM_B
                );
            }

            private void testDifferent(
                    @Nonnull final Duration whenSentB,
                    @Nullable final Actor<Integer> senderB, @Nonnull final Actor<Integer> receiverB,
                    @Nonnull final Medium mediumB
            ) {
                final var idA = new Signal.Id<>(WHEN_A, ACTOR_A, ACTOR_B, MEDIUM_A);
                final var idB = new Signal.Id<>(whenSentB, senderB, receiverB, mediumB);
                assertInvariants(idA, idB);
                assertThat(idA, not(idB));
            }

        }
    }

    @Nested
    public class Constructor {

        @Test
        public void a() {
            final var signal = constructor(ACTOR_A, WHEN_A, ACTOR_B, MEDIUM_A);

            assertInvariants(signal, 0);
            assertInvariants(signal, Integer.MAX_VALUE);
            assertInvariants(signal, (Integer) null);
        }

        @Test
        public void nullSender() {
            final var signal = constructor(null, WHEN_A, ACTOR_B, MEDIUM_A);

            assertInvariants(signal, 0);
            assertInvariants(signal, Integer.MAX_VALUE);
            assertInvariants(signal, (Integer) null);
        }

        @Test
        public void b() {
            constructor(ACTOR_B, WHEN_B, ACTOR_A, MEDIUM_B);
        }

        @Test
        public void endOfTime() {
            final var signal = constructor(ACTOR_A, Signal.NEVER_RECEIVED, ACTOR_B, MEDIUM_A);

            assertInvariants(signal, 0);
            assertInvariants(signal, Integer.MAX_VALUE);
        }

        @Test
        public void reflexive() {
            final Actor<Integer> actor = ACTOR_A;
            constructor(actor, WHEN_A, actor, MEDIUM_A);
        }

        @Nested
        public class Two {

            @Test
            public void equal() {
                final Signal<Integer> signalA = new SimpleTestSignal(WHEN_A, ACTOR_A, ACTOR_A, MEDIUM_A);
                final Signal<Integer> signalB = new SimpleTestSignal(WHEN_A, ACTOR_A, ACTOR_A, MEDIUM_A);
                assertInvariants(signalA, signalB);
                assertThat(signalA, is(signalB));
            }

            @Test
            public void different() {
                final Signal<Integer> signalA = new SimpleTestSignal(WHEN_A, ACTOR_A, ACTOR_A, MEDIUM_A);
                final Signal<Integer> signalB = new SimpleTestSignal(WHEN_B, ACTOR_B, ACTOR_B, MEDIUM_B);
                assertInvariants(signalA, signalB);
                assertThat(signalA, not(signalB));
            }

        }
    }

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
            final var signal = new SimpleTestSignal(whenSent, sender, receiver, MEDIUM_A);
            receive(signal, receiverState);
        }

    }

    @Nested
    public class ReceiveForStateHistory {

        @Test
        public void sentAtStart() {
            test(WHEN_A, WHEN_A, 0);
        }

        @Test
        public void sentAfterStart() {
            test(WHEN_B, WHEN_C, 1);
        }

        private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent, @Nonnull final Integer state0) {
            final var sender = new Actor<>(start, state0);
            final var receiver = new Actor<>(start, state0);
            final var signal = new SimpleTestSignal(whenSent, sender, receiver, MEDIUM_A);
            final ValueHistory<Integer> receiverStateHistory = new ConstantValueHistory<>(state0);

            final var event = receiveForStateHistory(signal, receiverStateHistory);

            assertThat("event.when", event.getWhen(), is(signal.getWhenReceived(state0)));
            assertThat("event", event, is(signal.receive(event.getWhen(), state0)));
        }
    }

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
                    final var signal = new SimpleTestSignal(whenSet, ACTOR_A, ACTOR_B, MEDIUM_A);
                    final ValueHistory<Integer> receiverStateHistory = new ConstantValueHistory<>(receiverState);

                    final var whenReceived = getWhenReceived(signal, receiverStateHistory);

                    assertThat(whenReceived, is(signal.getWhenReceived(receiverState)));
                }

            }

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
                    final var signal = new SimpleTestSignal(whenSet, ACTOR_A, ACTOR_B, MEDIUM_A);
                    final ModifiableValueHistory<Integer> receiverStateHistory = new ModifiableValueHistory<>(
                            receiverState0);
                    receiverStateHistory.appendTransition(transitionTime, receiverState1);

                    getWhenReceived(signal, receiverStateHistory);
                }

            }

        }

    }

}
