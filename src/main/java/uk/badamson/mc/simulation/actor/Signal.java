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

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import uk.badamson.mc.history.TimestampedValue;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.simulation.ObjectStateId;

/**
 * <p>
 * A signal (or message) sent from one simulated object to another.
 * </p>
 *
 * @param <STATE>
 *            The class of states of a receiver. This must be {@link Immutable
 *            immutable}. It ought to have value semantics, but that is not
 *            required.
 */
@Immutable
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public abstract class Signal<STATE> {

    /**
     * <p>
     * The effect that a {@linkplain Signal signal} has upon its
     * {@linkplain Signal#getReceiver() receiver} when it is received.
     * </p>
     *
     * @param <STATE>
     *            The class of states of a receiver. This must be {@link Immutable
     *            immutable}. It ought to have value semantics, but that is not
     *            required.
     */
    @Immutable
    public static final class Effect<STATE> {

        @Nonnull
        private final ObjectStateId eventId;
        @Nullable
        private final STATE state;
        @Nonnull
        private final Set<Signal<STATE>> signalsEmitted;

        /**
         * <p>
         * Construct an effect with given attribute values.
         * </p>
         *
         * @throws NullPointerException
         *             <ul>
         *             <li>If any {@link Nonnull} argument is null.</li>
         *             <li>If {@code signalsEmitted} contains a null.</li>
         *             </ul>
         * @throws IllegalArgumentException
         *             If {@code signalsEmitted} contains a signal that was not sent by
         *             the event represented by this effect. That is, if the signal was
         *             not {@linkplain Signal#getSentFrom() sent from} the same object
         *             as the {@code eventId}.
         */
        public Effect(@Nonnull final ObjectStateId eventId, @Nullable final STATE state,
                @Nonnull final Set<Signal<STATE>> signalsEmitted) {
            this.eventId = Objects.requireNonNull(eventId, "eventId");
            this.state = state;
            this.signalsEmitted = Set.copyOf(signalsEmitted);
            /* Check after copy to avoid race hazards. */
            this.signalsEmitted.forEach(signal -> {
                if (eventId != signal.getSentFrom()) {
                    throw new IllegalArgumentException("signalEmitted not sent from event.");
                }
            });
        }

        /**
         * <p>
         * The simulated object changed by this effect.
         * </p>
         */
        @Nonnull
        public UUID getAffectedObject() {
            return eventId.getObject();
        }

        /**
         * <p>
         * The ID of the simulated object that this effect changed, and the point in
         * time that the change occurred, combined to form a unique ID for the event
         * that caused this effect.
         * </p>
         * <ul>
         * <li>The {@linkplain ObjectStateId#getObject() object} of the event ID is the
         * same as the {@linkplain #getAffectedObject() affected object} of this
         * effect.</li>
         * <li>The {@linkplain ObjectStateId#getWhen() time-stamp} of the event ID is
         * the same as the {@linkplain #getWhenOccurred() time of occurrance} of this
         * effect.</li>
         * </ul>
         */
        @Nonnull
        public ObjectStateId getEventId() {
            return eventId;
        }

        /**
         * <p>
         * Signals emitted from the affected simulated object as part of this effect.
         * </p>
         * <ul>
         * <li>The returned set of signals emitted is a constant (the method always
         * returns a reference to the same object).</li>
         * <li>The returned set of signals emitted is unmodifiable.</li>
         * <li>The set of signals emitted does not contain a null signal.</li>
         * <li>The signals emitted are all {@linkplain Signal#getSentFrom() sent from}
         * the same {@linkplain #getEventId() event} that caused this effect.</li>
         * <li>The returned set of signals emitted may be {@linkplain Set#isEmpty()
         * empty}.</li>
         * </ul>
         */
        @Nonnull
        public Set<Signal<STATE>> getSignalsEmitted() {
            return signalsEmitted;
        }

        /**
         * <p>
         * The state that the simulated object has as a result of this effect.
         * </p>
         * <p>
         * A null state indicates that the simulated object is destroyed or removed.
         * </p>
         */
        @Nullable
        public STATE getState() {
            return state;
        }

        /**
         * <p>
         * The point in time that this effect occurred.
         * </p>
         */
        @Nonnull
        public Duration getWhenOccurred() {
            return eventId.getWhen();
        }

        @Override
        public String toString() {
            return "Signal.Effect [" + eventId + ", →" + state + ", ⇝" + signalsEmitted + "]";
        }

    }// class

    /**
     * <p>
     * An exception class for indicating that processing reception of a
     * {@link Signal} is impossible because the state of the
     * {@linkplain Signal#getReceiver() receiver} indicates that reception of the
     * signal would be impossible.
     * </p>
     */
    @SuppressWarnings("serial")
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
    private final UUID id;
    @Nonnull
    private final ObjectStateId sentFrom;
    @Nonnull
    private final UUID receiver;

    /**
     * <p>
     * Construct a signal with given attribute values.
     * </p>
     *
     * @param id
     *            The unique ID for this signal.
     * @param sentFrom
     *            The ID of the simulated object that sent this signal, and the
     *            point in time that it sent (emitted) the signal.
     * @param receiver
     *            The ID of the simulated object that this signal was sent to; the
     *            object that will receive it.
     * @throws NullPointerException
     *             If an {@linkplain Nonnull} argument is null
     */
    protected Signal(@Nonnull final UUID id, @Nonnull final ObjectStateId sentFrom, @Nonnull final UUID receiver) {
        this.id = Objects.requireNonNull(id, "id");
        this.sentFrom = Objects.requireNonNull(sentFrom, "sentFrom");
        this.receiver = Objects.requireNonNull(receiver, "receiver");
    }

    /**
     * <p>
     * Whether this signal is <dfn>equivalent to</dfn> a given other object.
     * </p>
     * <p>
     * The {@link Signal} class has <i>entity semantics</i> with the
     * {@linkplain #getId() ID} attribute acting as the unique ID.
     * </p>
     */
    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Signal)) {
            return false;
        }
        final Signal<?> other = (Signal<?>) obj;
        return id.equals(other.id);
    }

    /**
     * The unique ID for this signal.
     */
    @Nonnull
    @JsonProperty("id")
    public final UUID getId() {
        return id;
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
     * signal propagation speed.
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
     * The ID of the simulated object that this signal was sent to; the object that
     * will receive it.
     * </p>
     *
     * @see #getSender()
     */
    @Nonnull
    @JsonProperty("receiver")
    public final UUID getReceiver() {
        return receiver;
    }

    /**
     * <p>
     * The ID of the simulated object that sent this signal.
     * </p>
     * <p>
     * An object may send signals to itself. That is; the sender and
     * {@linkplain #getReceiver() receiver} may be {@linkplain UUID#equals(Object)
     * equivalent}. That can be done to implement internal events, such as timers.
     * </p>
     *
     * @see #getReceiver()
     */
    @Nonnull
    @JsonIgnore
    public final UUID getSender() {
        return sentFrom.getObject();
    }

    /**
     * <p>
     * The ID of the simulated object that sent this signal, and the point in time
     * that it sent (emitted) the signal.
     * </p>
     * <ul>
     * <li>The {@linkplain ObjectStateId#getObject() object} of the sent-from
     * information is the same as the {@linkplain #getSender() sender} of this
     * signal.</li>
     * <li>The {@linkplain ObjectStateId#getWhen() time-stamp} of the sent-from
     * information is the same as the {@linkplain #getWhenSent() sending time} of
     * this signal.</li>
     * </ul>
     */
    @Nonnull
    @JsonProperty("sentFrom")
    public final ObjectStateId getSentFrom() {
        return sentFrom;
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
     * <li>If the simulated object is destroyed or removed ({@code receiverState} is
     * null), the method returns {@link #NEVER_RECEIVED}: destroyed objects can not
     * receive signals.</li>
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
     * @param receiverState
     *            The state that the simulated object has as a result of this
     *            effect. A null state indicates that the simulated object is
     *            destroyed or removed.
     *
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
                throw new AssertionError("propagationDelay nonnegative (was negative)", e);
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
     * @param receiverStateHistory
     *            The time-wise variation of the state of the receiver. A null
     *            {@linkplain ValueHistory#get(Duration) value at a point in time}
     *            indicates that the simulated object was destroyed or removed at or
     *            before that point in time.
     * @throws NullPointerException
     *             If {@code receiverStateHistory} is null.
     * @throws IllegalArgumentException
     *             If the {@code receiverStateHistory} has a
     *             {@linkplain ValueHistory#getTransitions() transition} to a null
     *             state at a time before the last transition. That is, if
     *             {@code receiverStateHistory} indicates resurrection of a
     *             destroyed object.
     *
     * @see #getWhenReceived(Object)
     */
    @Nonnull
    public final Duration getWhenReceived(@Nonnull final ValueHistory<STATE> receiverStateHistory) {
        Objects.requireNonNull(receiverStateHistory, "receiverStateHistory");

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
            assert timestampedValue.getEnd().compareTo(NEVER_RECEIVED) < 0;
            tProbe = receiverStateHistory.getTansitionTimeAtOrAfter(tProbe);
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
    @JsonIgnore
    public final Duration getWhenSent() {
        return sentFrom.getWhen();
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    /**
     * <p>
     * The effect that his signal has if received by the {@linkplain #getReceiver()
     * receiver} at a given point in time, with the reception having a given event
     * ID.
     * </p>
     * <ul>
     * <li>The {@linkplain Effect#getAffectedObject() affected object} of the
     * returned effect is {@linkplain UUID#equals(Object) equal to} the
     * {@linkplain #getReceiver() receiver} of this signal.</li>
     * <li>The {@linkplain Effect#getWhenOccurred() time of occurrence} of the
     * returned effect is the same as {@code when}.</li>
     * </ul>
     *
     * @param when
     *            The point in time that reception of the signal occurred
     * @throws NullPointerException
     *             If a {@link Nonnull} argument is null.
     * @throws IllegalArgumentException
     *             If {@code when} is not {@linkplain Duration#compareTo(Duration)
     *             after} {@linkplain #getWhenSent() when this signal was sent}.
     * @throws UnreceivableSignalException
     *             If it is impossible for the receiver to receive this signal at
     *             the {@code when} time while its state is {@code receiverState}.
     *             In particular, the method <em>may</em> throw this if {@code when}
     *             is not {@linkplain Duration#equals(Object) equal to} the
     *             {@linkplain #getWhenSent() sending time} plus the
     *             {@linkplain #getPropagationDelay(Object) propagation delay}. This
     *             is a non-fatal error: if this exception is thrown, all invariants
     *             have been maintained.
     * @throws IllegalStateException
     *             If the {@code when} or this signal is inconsistent with
     *             {@code receiverState} in some way.
     *
     * @see #receive(Object)
     */
    @Nonnull
    protected abstract Effect<STATE> receive(@Nonnull Duration when, @Nonnull STATE receiverState)
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
     * <li>The {@linkplain Effect#getAffectedObject() affected object} of the
     * returned effect is {@linkplain UUID#equals(Object) equal to} the
     * {@linkplain #getReceiver() receiver} of this signal.</li>
     * <li>The {@linkplain Effect#getWhenOccurred() occurrence time} of the returned
     * effect is {@linkplain Duration#compareTo(Duration) before} the maximum
     * possible {@link Duration} value.</li>
     * <li>The {@linkplain Effect#getWhenOccurred() time of occurrence} of the
     * returned effect is {@linkplain Duration#equals(Object) equal to} the
     * {@linkplain #getWhenReceived(Object) reception time} of this signal, for the
     * receiver in the given {@code receiverState}.</li>
     * </ul>
     *
     * @param receiverState
     *            The state of the {@linkplain #getReceiver() receiver} for which to
     *            compute the {@linkplain Effect effect} of this signal.
     * @throws NullPointerException
     *             If {@code receiverState} is null.
     * @throws UnreceivableSignalException
     *             If it is impossible for the receiver to receive this signal while
     *             its state is {@code receiverState}. This is a non-fatal error: if
     *             this exception is thrown, all invariants have been maintained.
     * @throws IllegalStateException
     *             If this signal is inconsistent with {@code receiverState} in some
     *             way.
     */
    @Nonnull
    public final Effect<STATE> receive(@Nonnull final STATE receiverState) throws UnreceivableSignalException {
        Objects.requireNonNull(receiverState, "receiverState");
        final var whenReceived = getWhenReceived(receiverState);
        if (whenReceived.compareTo(NEVER_RECEIVED) < 0) {
            return receive(whenReceived, receiverState);
        } else {
            throw new UnreceivableSignalException();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + sentFrom + "⇝" + receiver + "]";
    }

}
