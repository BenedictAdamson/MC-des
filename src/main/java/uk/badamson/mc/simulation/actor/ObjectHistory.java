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
    private final Duration start;

    private final Object lock = new Object();

    @GuardedBy("lock")
    private final ModifiableValueHistory<STATE> stateHistory = new ModifiableValueHistory<>();

    /**
     * In order of occurrence
     */
    @GuardedBy("lock")
    private final List<Event<STATE>> events = new ArrayList<>();

    /**
     * <p>
     * Construct an object history with given start information and no signals.
     * </p>
     * <ul>
     * <li>The {@linkplain #getEvents() events} sequence
     * {@linkplain SortedSet#isEmpty() is empty}.</li>
     * </ul>
     *
     * @param start  The point in time that this history starts.
     * @param state  The first (known) state transition of the {@code
     *               object}.
     * @throws NullPointerException If a {@link Nonnull} argument is null
     */
    public ObjectHistory(@Nonnull final Duration start, @Nonnull final STATE state) {
        Objects.requireNonNull(state, "state");
        this.start = Objects.requireNonNull(start, "start");
        this.stateHistory.appendTransition(start, state);
    }

    /**
     * <p>
     * Get a snapshot of the sequence of events that have
     * {@linkplain Event#getAffectedObject() affected} the simulated object.
     * </p>
     * <ul>
     * <li>The events sequence may be {@linkplain List#isEmpty() empty}.</li>
     * <li>All events {@linkplain Event#getAffectedObject() affect} this object.</li>
     * <li>All events {@linkplain Event#getWhen() occurred}
     * {@linkplain Duration#compareTo(Duration) after} the {@linkplain #getStart()
     * start} time of this history.</li>
     * <li>The returned event sequence is a snapshot: a copy of data, it is not
     * updated if this object history is subsequently changed.</li>
     * <li>Note that events may be <i>measured as simultaneous</i>: events can have
     * {@linkplain Duration#equals(Object) equivalent}
     * {@linkplain Event#getWhen() times of occurrence}. However, the state
     * transition(s) due to some <i>measured as simultaneous</i> events will not be
     * apparent in the {@linkplain #getStateHistory() state history}; only the
     * <i>measured as simultaneous</i> event with the largest ID of its causing
     * signal will have its state recorded in the state history.</li>
     * <li>The events in the list are in order of occurrence</li>
     * </ul>
     *
     * @see #getStateHistory()
     */
    @Nonnull
    public List<Event<STATE>> getEvents() {
        synchronized (lock) {// hard to test
            return List.copyOf(events);
        }
    }

    /**
     * <p>
     * The last of the {@linkplain #getEvents() sequence of events} that have
     * {@linkplain Event#getAffectedObject() affected} this simulated object.
     * </p>
     * <ul>
     * <li>The last event is null if, and only if, the sequence of events is
     * empty.</li>
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
            final int size = events.size();
            if (0 < size) {
                return events.get(size - 1);
            } else {
                return null;
            }
        }
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
     * Get a snapshot of the history of states that the
     * simulated object has passed through.
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
     * {@linkplain Event#getWhen() time of occurrence} of the event.</li>
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
    public String toString() {
        synchronized (lock) {
            return super.toString() + "[from " + start + ", stateHistory=" + stateHistory + "]";
        }
    }

}
