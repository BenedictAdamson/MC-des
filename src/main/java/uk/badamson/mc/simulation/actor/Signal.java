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

import uk.badamson.mc.history.TimestampedValue;
import uk.badamson.mc.history.ValueHistory;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>
 * A signal (or message) sent from one {@linkplain Actor simulated object} to another.
 * </p>
 *
 * @param <STATE> The class of states of a receiver. This must be {@link Immutable
 *                immutable}. It ought to have value semantics, but that is not
 *                required.
 */
@Immutable
public abstract class Signal<STATE> implements Comparable<Signal<STATE>> {

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
    private final Actor<STATE> sender;

    @Nonnull
    private final Duration whenSent;

    @Nonnull
    private final Actor<STATE> receiver;

    /**
     * <p>
     * Construct a signal with given attribute values.
     * </p>
     * @throws NullPointerException If any {@linkplain Nonnull} argument is null
     */
    protected Signal(@Nonnull final Actor<STATE> sender, @Nonnull final Duration whenSent, @Nonnull final Actor<STATE> receiver) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.whenSent = Objects.requireNonNull(whenSent, "whenSent");
        this.receiver = Objects.requireNonNull(receiver, "receiver");
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

    /**
     * <p>
     * The simulated object that this signal was sent to; the object that
     * will receive it.
     * </p>
     *
     * @see #getSender()
     */
    @Nonnull
    public final Actor<STATE> getReceiver() {
        return receiver;
    }

    /**
     * <p>
     * The simulated object that sent this signal.
     * </p>
     * <p>
     * An object may send signals to itself. That is; the sender and
     * {@linkplain #getReceiver() receiver} may be the same.
     * That can be done to implement internal events, such as timers.
     * </p>
     *
     * @see #getReceiver()
     */
    @Nonnull
    public final Actor<STATE> getSender() {
        return sender;
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
     * @param receiverState The state that the simulated object has as a result of this
     *                      effect. A null state indicates that the simulated object is
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
                throw new AssertionError("propagationDelay non-negative (was negative)", e);
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
     * time-varying state}
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
     * @throws NullPointerException     If {@code receiverStateHistory} is null.
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
     */
    @Nonnull
    public final Duration getWhenSent() {
        return whenSent;
    }

    /**
     * <p>
     * The effect that his signal has if received by the {@linkplain #getReceiver()
     * receiver} at a given point in time, with the receiver having a given state.
     * </p>
     * <ul>
     * <li>The {@linkplain Event#getCausingSignal() signal causing} the
     * returned event is this signal.</li>
     * <li>The {@linkplain Event#getAffectedObject() affected object} of the
     * returned event is {@linkplain UUID#equals(Object) equal to} the
     * {@linkplain #getReceiver() receiver} of this signal.</li>
     * <li>The {@linkplain Event#getWhen() time of occurrence} of the
     * returned event is the same as {@code when}.</li>
     * <li>The implementation must be deterministic: the returned value may depend
     * only on the ({@linkplain Immutable immutable}) state of this signal and the
     * (also immutable) arguments of the method.</li>
     * </ul>
     *
     * @param when The point in time that reception of the signal occurred
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
     *                                     {@linkplain #getPropagationDelay(Object) propagation delay}. This
     *                                     is a non-fatal error: if this exception is thrown, all invariants
     *                                     have been maintained.
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
     * </p>
     * <ul>
     * <li>The {@linkplain Event#getCausingSignal() signal causing} the returned event is this signal.</li>
     * <li>The {@linkplain Event#getAffectedObject() affected object} of the
     * returned event is {@linkplain UUID#equals(Object) equal to} the
     * {@linkplain #getReceiver() receiver} of this signal.</li>
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
     * @throws NullPointerException        If {@code receiverState} is null.
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
     * {@inheritDoc}
     *
     * <p>However, for the Signal class it <em>is</em> required that
     * {@code (x.compareTo(y)==0) == (x.equals(y))}:
     * Signal has a natural ordering that is
     * inconsistent with equals.
     */
    @Override
    public abstract int compareTo(@Nonnull Signal<STATE> that);

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

    }// class

}
