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
import uk.badamson.mc.simulation.TimestampedId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.*;

/**
 * <p>
 * The sequence of state transitions of a simulated object.
 * </p>
 *
 * @param <STATE> The class of states of the simulated object. This must be
 *                {@link Immutable immutable}. It ought to have value semantics, but
 *                that is not required.
 */
@ThreadSafe
public final class ObjectHistory<STATE> {

    @Nonnull
    private final UUID object;
    @Nonnull
    private final Duration start;
    /*
     * Use a UUID object as the lock so all ObjectHistory objects can have a
     * predictable lock ordering when locking two instances.
     */
    private final UUID lock = UUID.randomUUID();
    @Nonnull
    @GuardedBy("lock")
    private final ModifiableValueHistory<STATE> stateHistory;
    /*
     * Maps event ID to event.
     */
    @Nonnull
    @GuardedBy("lock")
    private final NavigableMap<TimestampedId, Event<STATE>> events;

    /**
     * <p>
     * Copy an object history.
     * </p>
     */
    public ObjectHistory(@Nonnull final ObjectHistory<STATE> that) {
        Objects.requireNonNull(that, "that");
        object = that.object;
        start = that.start;
        synchronized (that.object) {// hard to test
            stateHistory = new ModifiableValueHistory<>(that.stateHistory);
            events = new TreeMap<>(that.events);
        }
    }

    /**
     * <p>
     * Construct an object history with given start information and no signals.
     * </p>
     * <ul>
     * <li>The {@linkplain #getEvents() events} sequence
     * {@linkplain SortedSet#isEmpty() is empty}.</li>
     * </ul>
     *
     * @param object The unique ID of the object for which this is the history.
     * @param start  The point in time that this history starts.
     * @param state  The first (known) state transition of the {@code
     *               object}.
     * @throws NullPointerException If a {@link Nonnull} argument is null
     */
    public ObjectHistory(@Nonnull final UUID object, @Nonnull final Duration start, @Nonnull final STATE state) {
        Objects.requireNonNull(state, "state");
        this.object = Objects.requireNonNull(object, "object");
        this.start = Objects.requireNonNull(start, "start");
        this.stateHistory = new ModifiableValueHistory<>();
        this.stateHistory.appendTransition(start, state);
        this.events = new TreeMap<>();
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to a given object.
     * </p>
     * <p>
     * The {ObjectHistory class has <i>value semantics</i>.
     * </p>
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ObjectHistory)) {
            return false;
        }
        final ObjectHistory<?> other = (ObjectHistory<?>) obj;
        if (!object.equals(other.object) || !start.equals(other.start)) {
            return false;
        }
        if (lockBefore(other)) {
            return equalsGuarded(other);
        } else {
            return other.equalsGuarded(this);
        }
    }

    private boolean equalsGuarded(@Nonnull final ObjectHistory<?> that) {
        // hard to test the thread safety
        synchronized (lock) {
            synchronized (that.lock) {
                return stateHistory.equals(that.stateHistory);
            }
        }
    }

    /**
     * <p>
     * Get a snapshot of the sequence of events that have
     * {@linkplain Event#getAffectedObject() affected} the {@linkplain #getObject()
     * simulated object}.
     * </p>
     * <ul>
     * <li>The events sequence may be {@linkplain SortedSet#isEmpty() empty}.</li>
     * <li>All events {@linkplain Event#getAffectedObject() affect} the
     * {@linkplain #getObject() simulated object} of this history.</li>
     * <li>All events {@linkplain Event#getWhenOccurred() occurred}
     * {@linkplain Duration#compareTo(Duration) after} the {@linkplain #getStart()
     * start} time of this history.</li>
     * <li>The returned event sequence is a snapshot: a copy of data, it is not
     * updated if this object history is subsequently changed.</li>
     * <li>Note that events may be <i>measured as simultaneous</i>: events can have
     * {@linkplain Duration#equals(Object) equivalent}
     * {@linkplain Event#getWhenOccurred() times of occurrence}. Note however, that
     * the collection of events is {@linkplain Event#compareTo(Event) ordered by}
     * the {@linkplain Event#getId() event IDs}, so the
     * {@linkplain Event#getCausingSignal() ID of the causing signal} is used as a
     * tie-breaker to produce a strict ordering. In that case, however, the state
     * transition(s) due to some <i>measured as simultaneous</i> events will not be
     * apparent in the {@linkplain #getStateHistory() state history}; only the
     * <i>measured as simultaneous</i> event with the largest ID of its causing
     * signal will have its state recorded in the state history.
     * </ul>
     *
     * @see #getStateHistory()
     */
    @Nonnull
    public SortedSet<Event<STATE>> getEvents() {
        synchronized (lock) {// hard to test
            return new TreeSet<>(events.values());
        }
    }

    /**
     * <p>
     * The last of the {@linkplain #getEvents() sequence of events} that have
     * {@linkplain Event#getAffectedObject() affected} the {@linkplain #getObject()
     * simulated object}.
     * </p>
     * <ul>
     * <li>The last event is null if, and only if, the sequence of events is
     * empty.</li>
     * <li>The last event is either null or is the {@linkplain SortedSet#last()
     * last} of the sequence of events.</li>
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
            final var lastEntry = events.lastEntry();
            return lastEntry == null ? null : lastEntry.getValue();
        }
    }

    /**
     * <p>
     * The unique ID of the object for which this is the history.
     * </p>
     * <ul>
     * <li>Constant: the history always returns the same object ID.</li>
     * </ul>
     */
    @Nonnull
    public UUID getObject() {
        return object;
    }

    /**
     * <p>
     * The point in time that this history starts.
     * </p>
     * <p>
     * The earliest point in time for which the state of the simulated object is
     * known. Expressed as the duration since an (implied) epoch. All objects in a
     * simulation should use the same epoch.
     * </p>
     * <ul>
     * <li>Constant: the history always returns the same start time.</li>
     * </ul>
     */
    @Nonnull
    public Duration getStart() {
        return start;
    }

    /**
     * <p>
     * Get a snapshot of the history of states that the {@linkplain #getObject()
     * simulated object} has passed through.
     * </p>
     * <ul>
     * <li>The state history is never {@linkplain ValueHistory#isEmpty()
     * empty}.</li>
     * <li>The {@linkplain ValueHistory#getFirstTransitionTime() first transition
     * time} of the state history is the same as the {@linkplain #getStart() start}
     * time of this history.</li>
     * <li>The {@linkplain ValueHistory#getFirstValue() state at the start of time}
     * of the state history is null.</li>
     * <li>The {@linkplain Event#getState() state} resulting from an
     * {@linkplain #getEvents() event} is {@linkplain #equals(Object) equivalent to}
     * the {@linkplain ValueHistory#get(Duration) value} of the state history at the
     * {@linkplain Event#getWhenOccurred() time of occurrence} of the event.</li>
     * <li>The returned state history is a snapshot: a copy of data, it is not
     * updated if this object history is subsequently changed.</li>
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
     * {@linkplain #getStateHistory() state history} of this object history.
     * </p>
     */
    @Nonnull
    public SortedMap<Duration, STATE> getStateTransitions() {
        synchronized (lock) {// hard to test
            /*
             * No need to make a defensive copy: ModifiableValueHistory.getTransitions()
             * does that for us.
             */
            return stateHistory.getTransitions();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        synchronized (lock) {// hard to test thread safety
            result = prime * result + object.hashCode();
            result = prime * result + stateHistory.hashCode();
        }
        return result;
    }

    private boolean lockBefore(@Nonnull final ObjectHistory<?> that) {
        assert !lock.equals(that.lock);
        return lock.compareTo(that.lock) < 0;
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return "ObjectHistory[" + object + " from " + start + ", stateHistory=" + stateHistory + "]";
        }
    }

}
