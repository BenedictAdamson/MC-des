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
import uk.badamson.mc.history.TimestampedValue;
import uk.badamson.mc.history.ValueHistory;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;

/**
 * <p>
 * A signal (or message) sent from one {@link Actor} to another.
 * </p>
 *
 * @param <STATE> The class of states of a receiver. This must be {@link Immutable
 *                immutable}. It ought to have value semantics, but that is not
 *                required.
 */
@Immutable
public abstract class Signal<STATE> {

    /**
     * <p>
     * A sentinel value for the {@linkplain #getPropagationDelay(Object) propagation
     * delay} and to indicate that it is impossible for a signal to be received.
     * </p>
     * <p>
     * The maximum possible {@link Duration}.
     * </p>
     */
    @Nonnull
    @Nonnegative
    public static final Duration NEVER_RECEIVED = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);

    @Nonnull
    private final Id<STATE> id;

    protected Signal(@Nonnull final Id<STATE> id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    protected Signal(
            @Nonnull final Duration whenSent,
            @Nullable final Actor<STATE> sender, @Nonnull final Actor<STATE> receiver,
            @Nonnull final Medium medium
    ) {
        this(new Id<>(whenSent, sender, receiver, medium));
    }

    /**
     * Whether this object is <i>equivalent</i> to another.
     * <p>
     * The Signal class has <i>entity semantics</i>,
     * with the {@link #getId()} method providing the unique ID.
     */
    @Override
    public final boolean equals(final Object that) {
        if (this == that) return true;
        if (!(that instanceof Signal<?>)) return false;

        final Signal<?> signal = (Signal<?>) that;

        return id.equals(signal.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    /**
     * <p>
     * The length of time it takes for this signal to propagate from the
     * {@linkplain #getSender() sender} to the {@linkplain #getReceiver() receiver},
     * for the receiver in a given state.
     * </p>
     * <p>
     * The propagation delay can depend on the receiver state to implement signals
     * sent through a medium while the receiver also moves. The method may return a
     * {@link #NEVER_RECEIVED} value to indicate that reception is impossible. For
     * example, when the receiver is moving away from the sender at faster than the
     * signal propagation speed. The propagation delay must be a deterministic
     * function of the given {@code receiverState} and values of this
     * ({@linkplain Immutable immutable}) signal. This method may be called while a
     * lock is held. Therefore to avoid deadlock this method must not delegate to
     * any alien methods that might acquire a lock. And to ensure good concurrency,
     * the implementation should be quick.
     * </p>
     *
     * @see #receive(Object)
     * @see #getWhenReceived(Object)
     */
    @Nonnull
    @Nonnegative
    protected abstract Duration getPropagationDelay(@Nonnull STATE receiverState);

    @Nonnull
    public Id<STATE> getId() {
        return id;
    }

    /**
     * <p>
     * The simulated object that this signal was sent to; the object that
     * will receive it.
     * </p>
     * <ul>
     *     <li>The same as the {@link Id#getReceiver() receiver} of the {@link #getId() ID} of this Signal.</li>
     * </ul>
     *
     * @see #getSender()
     */
    @Nonnull
    public final Actor<STATE> getReceiver() {
        return id.getReceiver();
    }

    /**
     * <p>
     * The simulated object that sent this signal.
     * </p>
     * <p>
     * An object may send signals to itself. That is; the sender and
     * {@linkplain #getReceiver() receiver} may be the same.
     * That can be done to implement internal events, such as timers.
     * The sender may be null, which indicates that the signal is an input to the simulation
     * rather than having been generated within the simulation.
     * </p>
     * <ul>
     *     <li>The same as the {@link Id#getSender() sender} of the {@link #getId() ID} of this Signal.</li>
     * </ul>
     *
     * @see #getReceiver()
     */
    @Nullable
    public final Actor<STATE> getSender() {
        return id.getSender();
    }

    /**
     * <p>
     * The means of transmitting this signal.
     * </p>
     * <ul>
     *     <li>The same as the {@link Id#getMedium() medium} of the {@link #getId() ID} of this Signal.
     * </ul>
     */
    @Nonnull
    public final Medium getMedium() {
        return id.getMedium();
    }

    /**
     * <p>
     * The point in time when this signal will be received, if the
     * {@linkplain #getReceiver() receiver} has a given state.
     * </p>
     * <p>
     * The reception time can depend on the receiver state to implement signals sent
     * through a medium while the receiver also moves. The method may return a
     * {@link #NEVER_RECEIVED} value to indicate that reception is impossible. For
     * example, when the receiver is moving away from the sender at faster than the
     * signal propagation speed, or if the {@linkplain #getWhenSent() sending time}
     * is too close to the end of time (the maximum {@link Duration} value), so
     * computation of the reception time would overflow.
     * </p>
     * <p>
     * This is a <i>template method</i> that delegates to the
     * {@link #getPropagationDelay(Object)} <i>primitive operation</i>.
     * It may be called while a lock is held.
     * </p>
     * <ul>
     * <li>Destroyed objects can not receive signals:
     * If the simulated object is destroyed or removed ({@code receiverState} is
     * null), the method returns {@link #NEVER_RECEIVED}.</li>
     * <li>The reception time is {@linkplain Duration#compareTo(Duration) after} the
     * {@linkplain #getWhenSent() sending time}, unless the sending time is the
     * maximum possible {@link Duration} value.</li>
     * <li>If the interval between the {@linkplain #getWhenSent() sending time} and
     * the maximum possible {@link Duration} value is less than the
     * {@linkplain #getPropagationDelay(Object) propagation delay}, the reception
     * time is the sending time plus the propagation delay.</li>
     * <li>Otherwise it returns {@link #NEVER_RECEIVED}.</li>
     * </ul>
     *
     * @param receiverState The state that the simulated object has just before it receives this signal.
     *                      A null state indicates that the simulated object is
     *                      destroyed or removed.
     * @see #receive(Object)
     * @see #getWhenReceived(ValueHistory)
     */
    @Nonnull
    public final Duration getWhenReceived(@Nullable final STATE receiverState) {
        if (receiverState == null) {
            return NEVER_RECEIVED;
        } else {
            final var whenSent = getWhenSent();
            final var propagationDelay = getPropagationDelay(receiverState);
            final boolean haveEnoughTime;
            try {
                haveEnoughTime = whenSent.compareTo(NEVER_RECEIVED.minus(propagationDelay)) < 0;
            } catch (final ArithmeticException e) {
                throw new IllegalStateException("propagationDelay non-negative (was negative)", e);
            }
            assert !Duration.ZERO.equals(propagationDelay);
            if (haveEnoughTime) {
                return whenSent.plus(propagationDelay);
            } else {// would overflow
                return NEVER_RECEIVED;
            }
        }
    }

    /**
     * <p>
     * The point in time when this signal will be received, if the
     * {@linkplain #getReceiver() receiver} has a given {@linkplain ValueHistory
     * history of time-varying states}
     * </p>
     * <p>
     * The reception time can depend on the receiver state to implement signals sent
     * through a medium while the receiver also moves. The method may return a
     * {@link #NEVER_RECEIVED} value to indicate that reception is impossible. The
     * computed reception time can be {@linkplain ValueHistory#get(Duration)
     * combined} with the given {@code receiverStateHistory} to compute the state of
     * the receiver when it receives the signal.
     * </p>
     * <p>
     * This is a <i>template method</i> that delegates to the
     * {@link #getPropagationDelay(Object)} <i>primitive operation</i>.
     * </p>
     * <ul>
     * <li>If the simulated object is destroyed or removed it can not receive a
     * signal. Therefore the reception time may indicate a {@code null} receiver
     * state only if the reception time is {@link #NEVER_RECEIVED}.</li>
     * <li>The reception time is {@linkplain Duration#compareTo(Duration) after} the
     * {@linkplain #getWhenSent() sending time}, unless the sending time is the
     * maximum possible {@link Duration} value.</li>
     * <li>The reception time is consistent with the receiver history: the returned
     * reception time indicates the earliest state after the
     * {@linkplain #getWhenSent() sending} of the signal for which the
     * {@linkplain #getWhenReceived(Object) reception time for that state} is
     * {@linkplain Duration#compareTo(Duration) at or before} the returned reception
     * time. The possibility that it is <em>before</em> is to allow for
     * discontinuities in the {@linkplain #getPropagationDelay(Object) propagation
     * delay}.</li>
     * </ul>
     *
     * @param receiverStateHistory The time-wise variation of the state of the receiver. A null
     *                             {@linkplain ValueHistory#get(Duration) value at a point in time}
     *                             indicates that the simulated object was destroyed or removed at or
     *                             before that point in time.
     * @throws IllegalArgumentException If the {@code receiverStateHistory} has a
     *                                  {@linkplain ValueHistory#getTransitions() transition} to a null
     *                                  state at a time before the last transition. That is, if
     *                                  {@code receiverStateHistory} indicates resurrection of a
     *                                  destroyed object.
     * @see #getWhenReceived(Object)
     */
    @Nonnull
    public final Duration getWhenReceived(@Nonnull final ValueHistory<STATE> receiverStateHistory) {
        Objects.requireNonNull(receiverStateHistory, "receiverStateHistory");

        final var transitionTimes = receiverStateHistory.getTransitionTimes();
        Duration tProbe = getWhenSent();
        while (tProbe.compareTo(NEVER_RECEIVED) < 0) {
            final TimestampedValue<STATE> timestampedValue = receiverStateHistory.getTimestampedValue(tProbe);
            final STATE receiverState = timestampedValue.getValue();
            final Duration whenReceived = getWhenReceived(receiverState);
            assert getWhenSent().compareTo(whenReceived) < 0;
            if (whenReceived.compareTo(timestampedValue.getStart()) <= 0) {
                assert getWhenSent().compareTo(timestampedValue.getStart()) < 0;
                return timestampedValue.getStart();
            } else if (whenReceived.compareTo(timestampedValue.getEnd()) <= 0) {
                return whenReceived;
            } // else must iterate
            tProbe = transitionTimes.tailSet(tProbe.plusNanos(1)).first();
            assert tProbe != null;
        } // while
        return NEVER_RECEIVED;
    }

    /**
     * <p>
     * The point in time when this signal was sent (emitted).
     * </p>
     * <ul>
     *     <li>The same as the {@link Id#getWhenSent() when sent} time of the {@link #getId() ID} of this Signal.</li>
     * </ul>
     */
    @Nonnull
    public final Duration getWhenSent() {
        return id.getWhenSent();
    }

    /**
     * <p>
     * The effect that his signal has if received by the {@linkplain #getReceiver()
     * receiver} at a given point in time, with the receiver having a given state.
     * </p>
     * <ul>
     * <li>The {@linkplain Event#getCausingSignal() signal causing} the
     * returned event is this signal.</li>
     * <li>The {@linkplain Event#getWhen() time of occurrence} of the
     * returned event is the same as {@code when}.</li>
     * <li>The implementation must be deterministic: the returned value may depend
     * only on the ({@linkplain Immutable immutable}) state of this signal and the
     * (also immutable) arguments of the method.
     * This method may be called while a lock is held; it must therefore not acquire any locks.
     * As it uses only immutable objects, it has no reason to acquire locks.</li>
     * </ul>
     *
     * @param when          The point in time that reception of the signal occurred
     * @param receiverState The state of the receiver when the signal arrived
     * @return The effect of this signal.
     * @throws NullPointerException        If a {@link Nonnull} argument is null.
     * @throws IllegalArgumentException    If {@code when} is not {@linkplain Duration#compareTo(Duration)
     *                                     after} {@linkplain #getWhenSent() when this signal was sent}.
     * @throws UnreceivableSignalException If it is impossible for the receiver to receive this signal at
     *                                     the {@code when} time while its state is {@code receiverState}.
     *                                     In particular, the method <em>may</em> throw this if {@code when}
     *                                     is not {@linkplain Duration#equals(Object) equal to} the
     *                                     {@linkplain #getWhenSent() sending time} plus the
     *                                     {@linkplain #getPropagationDelay(Object) propagation delay}.
     * @throws IllegalStateException       If the {@code when} or this signal is inconsistent with
     *                                     {@code receiverState} in some way.
     * @see #receive(Object)
     */
    @Nonnull
    protected abstract Event<STATE> receive(@Nonnull Duration when, @Nonnull STATE receiverState)
            throws UnreceivableSignalException;

    /**
     * <p>
     * The effect that his signal has if received by the {@linkplain #getReceiver()
     * receiver} while it is in a given state.
     * </p>
     * <p>
     * This is a <i>template method</i> that delegates to the
     * {@link #getPropagationDelay(Object)} and {@link #receive(Duration, Object)}
     * <i>primitive operations</i>.
     * It does not acquire any locks, and therefore may be called while a lock is held.
     * </p>
     * <ul>
     * <li>The {@linkplain Event#getCausingSignal() signal causing} the returned event is this signal.</li>
     * <li>The {@linkplain Event#getWhen() occurrence time} of the returned
     * event is {@linkplain Duration#compareTo(Duration) before} the maximum
     * possible {@link Duration} value.</li>
     * <li>The {@linkplain Event#getWhen() time of occurrence} of the
     * returned event is {@linkplain Duration#equals(Object) equal to} the
     * {@linkplain #getWhenReceived(Object) reception time} of this signal, for the
     * receiver in the given {@code receiverState}.</li>
     * </ul>
     *
     * @param receiverState The state of the {@linkplain #getReceiver() receiver} for which to
     *                      compute the {@linkplain Event effect} of this signal.
     * @throws UnreceivableSignalException If it is impossible for the receiver to receive this signal while
     *                                     its state is {@code receiverState}. This is a non-fatal error: if
     *                                     this exception is thrown, all invariants have been maintained.
     * @throws IllegalStateException       If this signal is inconsistent with {@code receiverState} in some
     *                                     way.
     */
    @Nonnull
    public final Event<STATE> receive(@Nonnull final STATE receiverState) throws UnreceivableSignalException {
        Objects.requireNonNull(receiverState, "receiverState");
        final var whenReceived = getWhenReceived(receiverState);
        if (whenReceived.compareTo(NEVER_RECEIVED) < 0) {
            return receive(whenReceived, receiverState);
        } else {
            throw new UnreceivableSignalException();
        }
    }

    /**
     * <p>
     * The effect that his signal has if received by the {@linkplain #getReceiver()
     * receiver} if the receiver has a given state history.
     * </p>
     * <p>
     * This is a <i>template method</i> that delegates to the
     * {@link #getPropagationDelay(Object)} and {@link #receive(Duration, Object)}
     * <i>primitive operations</i>.
     * It does not acquire any locks, and therefore may be called while a lock is held.
     * </p>
     * <ul>
     * <li>The {@linkplain Event#getCausingSignal() signal causing} the returned event is this signal.</li>
     * <li>The {@linkplain Event#getWhen() occurrence time} of the returned
     * event is {@linkplain Duration#compareTo(Duration) before} the maximum
     * possible {@link Duration} value.</li>
     * <li>The {@linkplain Event#getWhen() time of occurrence} of the
     * returned event is {@linkplain Duration#equals(Object) equal to} the
     * {@linkplain #getWhenReceived(Object) reception time} of this signal, for the
     * receiver in the given {@code receiverState}.</li>
     * </ul>
     *
     * @param receiverStateHistory The time-wise variation of the state of the receiver. A null
     *                             {@linkplain ValueHistory#get(Duration) value at a point in time}
     *                             indicates that the simulated object was destroyed or removed at or
     *                             before that point in time.
     * @throws UnreceivableSignalException If it is impossible for the receiver to receive this signal.
     * @throws IllegalStateException       If this signal is inconsistent with {@code receiverStateHistory} in some
     *                                     way.
     */
    @Nonnull
    public final Event<STATE> receiveForStateHistory(@Nonnull final ValueHistory<STATE> receiverStateHistory) throws UnreceivableSignalException {
        Objects.requireNonNull(receiverStateHistory, "receiverStateHistory");
        final var whenReceived = getWhenReceived(receiverStateHistory);
        if (whenReceived.compareTo(NEVER_RECEIVED) < 0) {
            final STATE receiverState = receiverStateHistory.get(whenReceived);
            assert receiverState != null;
            return receive(whenReceived, receiverState);
        } else {
            throw new UnreceivableSignalException();
        }
    }

    final int tieBreakCompareTo(@Nonnull final Signal<STATE> that) {
        return id.tieBreakCompareTo(that.id);
    }

    /**
     * <p>
     * An exception class for indicating that processing reception of a
     * {@link Signal} is impossible because the state of the
     * {@linkplain Signal#getReceiver() receiver} indicates that reception of the
     * signal would be impossible.
     * </p>
     */
    public static class UnreceivableSignalException extends IllegalStateException {

        private static final String DEFAULT_MESSAGE = "Reception of the signal would be impossible";

        public UnreceivableSignalException() {
            super(DEFAULT_MESSAGE);
        }

        public UnreceivableSignalException(final String s) {
            super(s);
        }

        public UnreceivableSignalException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public UnreceivableSignalException(final Throwable cause) {
            super(DEFAULT_MESSAGE, cause);
        }

    }

    /**
     * The unique ID of a Signal.
     *
     * @param <STATE> The class of states of a receiver of a Signal. This must be {@link Immutable
     *                immutable}. It ought to have value semantics, but that is not
     *                required.
     */
    @Immutable
    public static final class Id<STATE> {

        @Nonnull
        private final Duration whenSent;

        @Nullable
        private final Actor<STATE> sender;

        @Nonnull
        private final Actor<STATE> receiver;

        @Nonnull
        private final Medium medium;

        @SuppressFBWarnings(value="EI_EXPOSE_REP2", justification = "sender has reference semantics")
        public Id(
                @Nonnull final Duration whenSent,
                @Nullable final Actor<STATE> sender,
                @Nonnull final Actor<STATE> receiver,
                @Nonnull final Medium medium
        ) {
            this.whenSent = Objects.requireNonNull(whenSent, "whenSent");
            this.sender = sender;
            this.receiver = Objects.requireNonNull(receiver, "receiver");
            this.medium = Objects.requireNonNull(medium, "medium");
        }

        @Nonnull
        public Duration getWhenSent() {
            return whenSent;
        }

        /**
         * The {@link Actor} in the simulation that sent this signal,
         * or null if this signal is an input to the simulation.
         */
        @Nullable
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "reference semantics")
        public Actor<STATE> getSender() {
            return sender;
        }

        @Nonnull
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "reference semantics")
        public Actor<STATE> getReceiver() {
            return receiver;
        }

        @Nonnull
        public Medium getMedium() {
            return medium;
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * The Signal.Id class has value semantics.
         * </p>
         */
        @Override
        public boolean equals(final Object that) {
            if (this == that) return true;
            if (that == null || getClass() != that.getClass()) return false;

            final Id<?> id = (Id<?>) that;
            return whenSent.equals(id.whenSent) &&
                    Objects.equals(sender, id.sender) &&
                    receiver.equals(id.receiver) &&
                    medium.equals(id.medium);
        }

        @Override
        public int hashCode() {
            int result = whenSent.hashCode();
            result = 31 * result + (sender == null ? 0 : sender.hashCode());
            result = 31 * result + receiver.hashCode();
            result = 31 * result + medium.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "{" +
                    "@" + whenSent +
                    " " + sender +
                    "\u2192" + receiver +
                    " through " + medium +
                    '}';
        }

        int tieBreakCompareTo(@Nonnull final Id<STATE> that) {
            int c = whenSent.compareTo(that.whenSent);
            if (c == 0) {
                if (sender != null && that.sender != null) {
                    c = sender.lock.compareTo(that.sender.lock);
                } else if (sender == null && that.sender != null) {
                    c = -1;
                } else if (sender != null) {
                    c = 1;
                }
            }
            if (c == 0) {
                c = medium.id.compareTo(that.medium.id);
            }
            if (c == 0) {
                c = receiver.lock.compareTo(that.receiver.lock);
            }
            return c;
        }
    }

}
