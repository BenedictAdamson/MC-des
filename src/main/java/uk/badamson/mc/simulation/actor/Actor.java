package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2018,2021-22.
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

import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.*;

/**
 * <p>
 * A simulated object.
 * </p>
 *
 * @param <STATE> The class of states of the simulated object. This must be
 *                {@link Immutable immutable}. It ought to have value semantics, but
 *                that is not required.
 */
@ThreadSafe
public final class Actor<STATE> {

    @Nonnull
    private final Duration start;

    private final Object lock = new Object();

    @GuardedBy("lock")
    private final ModifiableValueHistory<STATE> stateHistory = new ModifiableValueHistory<>();

    @GuardedBy("lock")
    private final SortedSet<Event<STATE>> events = new TreeSet<>();

    @GuardedBy("lock")
    private final Set<Signal<STATE>> signalsToReceive = new HashSet<>();

    @GuardedBy("lock")
    private Signal<STATE> nextSignalToReceive;

    // null indicates unknown
    @GuardedBy("lock")
    private Duration whenReceiveNextSignal;

    @GuardedBy("lock")
    private long version;

    /**
     * <p>
     *     Indicates that a method of the {@link Signal} class threw a {@link RuntimeException},
     *     which is likely to be due to a bug in an implementation of that class.
     * </p>
     */
    public static final class SignalException extends RuntimeException {
        <STATE> SignalException(@Nonnull final Signal<STATE> signal, @Nonnull final RuntimeException cause) {
            super("Signal "+signal+" threw exception " + cause, cause);
        }
    }

    /**
     * <p>
     * Construct an actor with given start information and no events.
     * </p>
     * <ul>
     * <li>The {@linkplain #getEvents() events} sequence
     * {@linkplain List#isEmpty() is empty}.</li>
     * </ul>
     *
     * @param start  The first point in time for which the actor has a known state.
     * @param state  The first (known) state of the actor.
     * @throws NullPointerException If any argument is null
     */
    public Actor(@Nonnull final Duration start, @Nonnull final STATE state) {
        Objects.requireNonNull(state, "state");
        this.start = Objects.requireNonNull(start, "start");
        this.stateHistory.appendTransition(start, state);
    }

    /**
     * <p>
     * Get a snapshot of the sequence of events that have
     * {@linkplain Event#getAffectedObject() affected} this actor.
     * </p>
     * <ul>
     * <li>The events sequence may be {@linkplain List#isEmpty() empty}.</li>
     * <li>All events {@linkplain Event#getAffectedObject() affect} this actor.</li>
     * <li>All events {@linkplain Event#getWhen() occurred}
     * {@linkplain Duration#compareTo(Duration) after} the {@linkplain #getStart()
     * start} time of this history.</li>
     * <li>The returned event sequence is a snapshot: a copy of data, it is not
     * updated if this actor is subsequently changed.</li>
     * <li>Note that events may be <i>measured as simultaneous</i>: events can have
     * {@linkplain Duration#equals(Object) equivalent}
     * {@linkplain Event#getWhen() times of occurrence}. However, the state
     * transition(s) due to some <i>measured as simultaneous</i> events will not be
     * apparent in the {@linkplain #getStateHistory() state history}; only the
     * <i>measured as simultaneous</i> event with the largest ID of its causing
     * signal will have its state recorded in the state history.</li>
     * </ul>
     *
     * @see #getStateHistory()
     */
    @Nonnull
    public SortedSet<Event<STATE>> getEvents() {
        synchronized (lock) {// hard to test
            return new TreeSet<>(events);
        }
    }

    /**
     * <p>
     * The last of the {@linkplain #getEvents() events} of this actor.
     * </p>
     * <ul>
     * <li>The last event is null if, and only if, the sequence of events is
     * {@linkplain  List#isEmpty() empty}.</li>
     * <li>This method is likely to be more efficient than using
     * {@link #getEvents()} and then extracting the last event from the
     * sequence.</li>
     * </ul>
     *
     * @see #getEvents()
     */
    @Nullable
    public Event<STATE> getLastEvent() {
        synchronized (lock) {// hard to test
            if (events.isEmpty()) {
                return null;
            } else {
                return events.last();
            }
        }
    }

    /**
     * <p>
     * The earliest point in time for which the state of this actor is known.
     * </p>
     * <p>
     * Expressed as the duration since an (implied) epoch. All objects in a
     * simulation should use the same epoch.
     * </p>
     * <ul>
     * <li>Constant: this always returns the same start time.</li>
     * </ul>
     */
    @Nonnull
    public Duration getStart() {
        return start;
    }

    /**
     * <p>
     * Get a snapshot of the history of states that the
     * actor has passed through.
     * </p>
     * <ul>
     * <li>The state history is never {@linkplain ValueHistory#isEmpty()
     * empty}.</li>
     * <li>The {@linkplain ValueHistory#getFirstTransitionTime() first transition
     * time} of the state history is the same as the {@linkplain #getStart() start}
     * time of this actor.</li>
     * <li>The {@linkplain ValueHistory#getFirstValue() state at the start of time}
     * of the state history is null.</li>
     * <li>The {@linkplain Event#getState() state} resulting from an
     * {@linkplain #getEvents() event} is {@linkplain #equals(Object) equivalent to}
     * the {@linkplain ValueHistory#get(Duration) value} of the state history at the
     * {@linkplain Event#getWhen() time of occurrence} of the event.</li>
     * <li>The returned state history is a snapshot: a copy of data, it is not
     * updated if this actor is subsequently changed.</li>
     * </ul>
     */
    @Nonnull
    public ValueHistory<STATE> getStateHistory() {
        synchronized (lock) {// hard to test
            return new ModifiableValueHistory<>(stateHistory);
        }
    }

    /**
     * <p>
     * The {@linkplain ValueHistory#getTransitions() transitions} in the
     * {@linkplain #getStateHistory() state history} of this actor.
     * </p>
     */
    @Nonnull
    public SortedMap<Duration, STATE> getStateTransitions() {
        synchronized (lock) {// hard to test
            return stateHistory.getTransitions();
        }
    }

    /**
     * <p>
     * The signals that, when received, will add to the {@linkplain #getEvents() sequence of events}
     * of this actor, but which have not yet been {@linkplain  #receiveSignal() received}.
     * </p>
     * <ul>
     *     <li>Does not contain a null element.</li>
     *     <li>A snapshot: not updated when signals are subsequently {@linkplain  #addSignalToReceive(Signal) added} or {@linkplain #receiveSignal() received}.</li>
     *     <li>The set may be unmodifiable.</li>
     *     <li>The {@linkplain Signal#getReceiver() receiver} of every signal in the set is this actor.</li>
     *     <li>The {@linkplain Signal#getWhenSent()} sending time} of every signal in the set is {@linkplain Duration#compareTo(Duration) before} the {@linkplain #getStart() start time} of this actor.</li>
     * </ul>
     */
    @Nonnull
    public Set<Signal<STATE>> getSignalsToReceive() {
        synchronized (lock) {
            return Set.copyOf(signalsToReceive);
        }
    }

    /**
     * <p>
     * Add a signal to the {@linkplain #getSignalsToReceive() set of signals to receive}.
     * </p>
     * @throws NullPointerException
     * If {@code signal} is null
     * @throws IllegalArgumentException
     * <ul>
     *     <li>If the {@linkplain Signal#getReceiver() receiver} of the {@code signal} is not this actor.</li>
     *     <li>If the {@linkplain Signal#getWhenSent()} sending time of the {@code signal} is {@linkplain Duration#compareTo(Duration) before} the {@linkplain #getStart() start time} of this actor.</li>
     * </ul>
     * @throws SignalException
     * If a {@link Signal} object (not necessarily the given {@code signal}) throws a {@link RuntimeException}.
     */
    public void addSignalToReceive(@Nonnull final Signal<STATE> signal) {
        Objects.requireNonNull(signal, "signal");
        if (signal.getReceiver() != this) {
            throw new IllegalArgumentException("this actor is not the receiver of the signal");
        }
        if (signal.getWhenSent().compareTo(start) < 0) {
            throw new IllegalArgumentException("signal sent before the start time of this actor");
        }
        synchronized (lock) {
            if (whenReceiveNextSignal == null) {
                signalsToReceive.add(signal);
                recomputeNextSignalToReceive();
            } else {
                updateNextSignalToReceiveFor(signal);
                signalsToReceive.add(signal);
            }
            version++;
        }
    }

    /**
     * <p>
     * If this actor has {@linkplain #getSignalsToReceive() signals to receive},
     * receive the first such signal.
     * </p>
     * <p>
     * Although this method removes a signal from the set of signals to receive,
     * it may also add signals for events that have been invalidated by the signal received,
     * </p>
     * @throws SignalException
     * If a {@link Signal} object throws a {@link RuntimeException}.
     */
    public void receiveSignal() {
        final Signal<STATE> signal;
        final long previousVersion;
        synchronized (lock) {
            if (whenReceiveNextSignal == null) {
                recomputeNextSignalToReceive();
            }
            signal = nextSignalToReceive;
            previousVersion = version;
        }
        if (signal != null) {
            final Event<STATE> event;
            synchronized (lock) {
                if (isOutOfDate(previousVersion, signal)) {
                    return;
                }
                event = createNextEvent();
            }
            appendEvent(previousVersion, event);
        }
    }

    @GuardedBy("lock")
    private boolean isOutOfDate(final long expectedVersion, final Signal<STATE> expectedNextSignalToReceive) {
        /* This assumes we will never have more than Long.MAX_VALUE threads,
         * which is safe enough
         */
        return expectedVersion != version || expectedNextSignalToReceive != nextSignalToReceive;
    }

    @GuardedBy("lock")
    private Event<STATE> createNextEvent() throws SignalException {
        assert nextSignalToReceive != null;
        assert whenReceiveNextSignal != null;
        final STATE state = stateHistory.get(whenReceiveNextSignal);
        assert state != null;
        try {
            return nextSignalToReceive.receive(whenReceiveNextSignal, state);
        } catch (final RuntimeException e) {
            throw new SignalException(nextSignalToReceive, e);
        }
    }

    private void appendEvent(final long previousVersion, final Event<STATE> event) {
        final Signal<STATE> causingSignal = event.getCausingSignal();
        synchronized (lock) {
            if (isOutOfDate(previousVersion, causingSignal)) {
                return;
            }
            //TODO optimise if already present
            invalidateEvents(List.copyOf(events.tailSet(event)));
            version++;
            events.add(event);
            stateHistory.setValueFrom(event.getWhen(), event.getState());
            signalsToReceive.remove(causingSignal);
            // TODO send emitted signals
            invalidateNextSignalToReceive();
        }
    }

    @GuardedBy("lock")
    private void recomputeNextSignalToReceive() throws SignalException {
        nextSignalToReceive = null;
        whenReceiveNextSignal = Signal.NEVER_RECEIVED;
        try {
            for (final var signal : signalsToReceive) {
                updateNextSignalToReceiveFor(signal);
            }
        } catch (final SignalException e) {
            invalidateNextSignalToReceive();
            throw e;
        }
    }

    @GuardedBy("lock")
    private void updateNextSignalToReceiveFor(@Nonnull final Signal<STATE> signal) throws SignalException {
        final Duration whenReceived;
        try {
            whenReceived = signal.getWhenReceived(stateHistory);
        } catch (final RuntimeException e) {
            throw new SignalException(signal, e);
        }
        final int compareWhen = whenReceived.compareTo(whenReceiveNextSignal);
        final int compare;
        try {
            compare = compareWhen == 0 ? signal.compareTo(nextSignalToReceive) : compareWhen;
        } catch (final RuntimeException e) {
            throw new SignalException(signal, e);
        }
        if (compare < 0) {
            nextSignalToReceive = signal;
            whenReceiveNextSignal = whenReceived;
        }
        assert nextSignalToReceive != null;
    }

    @GuardedBy("lock")
    private void invalidateEvents(final List<Event<STATE>> invalidatedEvents) {
        for (final var invalidatedEvent: invalidatedEvents) {
            signalsToReceive.add(invalidatedEvent.getCausingSignal());
            events.remove(invalidatedEvent);
            // TODO invalidate emitted signal
        }
    }

    @GuardedBy("lock")
    private void invalidateNextSignalToReceive() {
        nextSignalToReceive = null;
        whenReceiveNextSignal = null;
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return super.toString() + "[from " + start + ", stateHistory=" + stateHistory + "]";
        }
    }

}
