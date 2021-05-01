package uk.badamson.mc.simulation.rx;
/*
 * © Copyright Benedict Adamson 2018,2021.
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
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;

/**
 * <p>
 * The sequence of state transitions of a simulated object.
 * </p>
 *
 * @param <STATE>
 *            The class of states of the simulated object. This must be
 *            {@link Immutable immutable}. It ought to have value semantics, but
 *            that is not required.
 */
@ThreadSafe
public final class ObjectHistory<STATE> {

    /**
     * <p>
     * The state of a simulated object, with a time-stamp indicating one of the
     * points in time when the simulated object was in that state.
     * </p>
     *
     * @param <STATE>
     *            The class of states of the simulated object. This must be
     *            {@link Immutable immutable}. It ought to have value semantics, but
     *            that is not required.
     */
    @Immutable
    public static final class TimestampedState<STATE> {

        @Nonnull
        private final Duration when;
        @Nullable
        private final STATE state;

        /**
         * Constructs a value with given attribute values.
         *
         * @throws NullPointerException
         *             If {@code when} is null
         */
        public TimestampedState(@Nonnull final Duration when, @Nullable final STATE state) {
            this.when = Objects.requireNonNull(when, "when");
            this.state = state;
        }

        /**
         * <p>
         * Whether this object is <dfn>equivalent</dfn> to a given other object.
         * </p>
         * <p>
         * The TimestampedState class has <i>value semantics</i>: the object is
         * equivalent to another object if, and only if, the other object is also a
         * TimestampedState and the two have equivalent attribute values.
         * </p>
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TimestampedState)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            final TimestampedState<STATE> other = (TimestampedState<STATE>) obj;
            return when.equals(other.when) && Objects.equals(state, other.state);
        }

        /**
         * <p>
         * A state of the simulated object at the {@linkplain #getWhen() time}
         * </p>
         * <ul>
         * <li>Null if the object does not exist at that time.</li>
         * </ul>
         */
        @Nullable
        public STATE getState() {
            return state;
        }

        /**
         * <p>
         * The point in time that the simulated object is in the {@linkplain #getState()
         * state}
         * </p>
         * <p>
         * Expressed as the duration since an (implied) epoch. All objects in a
         * simulation should use the same epoch.
         * </p>
         */
        @Nonnull
        public Duration getWhen() {
            return when;
        }

        @Override
        public int hashCode() {
            return Objects.hash(state, when);
        }

        @Nonnull
        @Override
        public String toString() {
            return "TimestampedState [@" + when + ", " + state + "]";
        }

    }// class

    @Nonnull
    private final UUID object;
    @Nonnull
    private final Duration start;
    private final Object lock = new Object();
    private final Sinks.Many<Event<STATE>> events = Sinks.many().replay().latest();
    private final Sinks.Many<TimestampedState<STATE>> stateTransitions = Sinks.many().replay().latest();

    @GuardedBy("lock")
    private final ModifiableValueHistory<STATE> stateHistory = new ModifiableValueHistory<>();

    @Nonnull
    @GuardedBy("lock")
    private Event<STATE> lastEvent;

    /**
     * <p>
     * Construct an object history with given start information.
     * </p>
     * <ul>
     * <li>The {@linkplain #getLastEvent() last event} of this history is the given
     * {@code event}.</li>
     * </ul>
     *
     * @param event
     *            The first (known) state transition of the {@linkplain #getObject()
     *            object}.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code event} is null</li>
     *             <li>if the {@linkplain Event#getState() state} of {@code event}
     *             is null. That is, if the first event is the destruction or
     *             removal of the {@linkplain #getObject() simulated object}.</li>
     *             </ul>
     */
    public ObjectHistory(@Nonnull final Event<STATE> event) {
        this.lastEvent = Objects.requireNonNull(event, "event");// redundant; satisfy SpotBugs
        Objects.requireNonNull(event.getState(), "event.state");
        this.object = event.getObject();
        this.start = event.getWhen();
        append1(event);
    }

    /**
     * <p>
     * Append an event to this history.
     * </p>
     *
     * <ul>
     * <li>The {@linkplain #getLastEvent() last event} becomes the same as the given
     * {@code event}.</li>
     * <li>Any subscribers to the {@linkplain #observeStateTransitions() sequence of
     * state transitions} will receive a new state transition corresponding the the
     * {@code event}:
     * <ul>
     * <li>The {@linkplain TimestampedState#getWhen() time} of the state transition
     * will be the same as the {@linkplain Event#getWhen() time} of the
     * {@code event}.</li>
     * <li>The {@linkplain TimestampedState#getState() state} of the state
     * transition will be the same as the {@linkplain Event#getState() state} of the
     * {@code event}.</li>
     * </ul>
     * </ul>
     *
     * @param event
     *            The event to append.
     * @throws NullPointerException
     *             If {@code event} is null
     * @throws IllegalArgumentException
     *             If the {@linkplain Event#getObject() object} of the {@code event}
     *             is not the same as the {@linkplain #getObject() object} of this
     *             history.
     * @throws IllegalStateException
     *             <li>If the {@linkplain Event#getWhen() time} of the {@code event}
     *             is not {@linkplain Duration#compareTo(Duration) after} the time
     *             of the {@linkplain #getLastEvent() last event} of this history.
     *             That can happen if a different thread appended an event.</li>
     *             <li>If the {@linkplain Event#getState() state} of the current
     *             {@linkplain #getLastEvent() last event} is null. That is, if the
     *             current last event was the destruction or removal of the
     *             {@linkplain #getObject() simulated object}: destroyed objects may
     *             not be resurrected.
     */
    public void append(@Nonnull final Event<STATE> event) {
        Objects.requireNonNull(event, "event");
        if (object != event.getObject()) {
            throw new IllegalArgumentException("event.getObject");
        }
        synchronized (lock) {
            if (0 <= lastEvent.getWhen().compareTo(event.getWhen())) {
                throw new IllegalStateException("event.getWhen");
            }
            if (lastEvent.getState() == null) {
                throw new IllegalStateException("resurrection attempted");
            }
            append1(event);
        }
    }

    private void append1(final Event<STATE> event) {
        lastEvent = event;
        final var when = event.getWhen();
        final var state = event.getState();
        stateHistory.appendTransition(when, state);
        final var result1 = events.tryEmitNext(event);
        final var result2 = stateTransitions.tryEmitNext(new TimestampedState<>(when, state));
        // The sink are reliable, so should always succeed.
        assert result1 == Sinks.EmitResult.OK;
        assert result2 == Sinks.EmitResult.OK;
        if (state == null) {// destruction
            final var result3 = events.tryEmitComplete();
            final var result4 = stateTransitions.tryEmitComplete();
            assert result3 == Sinks.EmitResult.OK;
            assert result4 == Sinks.EmitResult.OK;
        }
    }

    /**
     * <p>
     * The last (known) state transition of the {@linkplain #getObject() object}.
     * </p>
     * <ul>
     * <li>Always have a (non null) last event.</li>
     * <li>The {@linkplain Event#getObject() object} of the last event is the same
     * as the {@linkplain #getObject() object} of this history.</li>
     * <li>The {@linkplain Event#getWhen() time} of the last event is
     * {@linkplain Duration#compareTo(Duration) at or after} the
     * {@linkplain #getStart() start} of this history.</li>
     * </ul>
     *
     * @see #observeEvents()
     */
    @Nonnull
    public Event<STATE> getLastEvent() {
        synchronized (lock) {
            return lastEvent;
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
     * <li>The {@linkplain ValueHistory#getFirstTansitionTime() first transition
     * time} of the state history is the same as the {@linkplain #getStart() start}
     * time of this history.</li>
     * <li>The {@linkplain ValueHistory#getFirstValue() state at the start of time}
     * of the state history is null.</li>
     * <li>The {@linkplain ValueHistory#getLastTansitionTime() last transition time}
     * of the state history is the same as the {@linkplain Event#getWhen() time} of
     * the {@linkplain #getLastEvent() last event} of this history.</li>
     * <li>The {@linkplain ValueHistory#getLastValue() state at the end of time} of
     * the state history is the same as the {@linkplain Event#getState() state} of
     * the {@linkplain #getLastEvent() last event} of this history.</li>
     * <li>The state history is never {@linkplain ValueHistory#isEmpty()
     * empty}.</li>
     * <li>The returned state history is a snapshot: a copy of data, it is not
     * updated when events are {@linkplain #append(Event) appended}.</li>
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
     * Provide the sequence of {@linkplain Event events} that cause
     * {@linkplain #observeStateTransitions() state transitions} of the
     * {@linkplain #getObject() simulated object}.
     * </p>
     * <ul>
     * <li>The sequence of events has no null events.</li>
     * <li>The sequence of events can be infinite.</li>
     * <li>The sequence of events can be finite. In that case, the last event is the
     * destruction of the object: a transition to a null
     * {@linkplain Event#getState() state}.</li>
     * <li>The sequence of events are in {@linkplain Duration#compareTo(Duration)
     * ascending} {@linkplain Event#getWhen() time-stamp} order.</li>
     * <li>Always have a (non null) last event.</li>
     * <li>The {@linkplain Event#getObject() object} of each event is the same as
     * the {@linkplain #getObject() object} of this history.</li>
     * <li>The {@linkplain Event#getWhen() time} of each event is
     * {@linkplain Duration#compareTo(Duration) at or after} the
     * {@linkplain #getStart() start} of this history.</li>
     * <li>The sequence of events does not include old events; the first event
     * published to a subscriber will be the current {@linkplain #getLastEvent()
     * last event} at the time of subscription.</li>
     * </ul>
     *
     * @see #getLastEvent()
     */
    @Nonnull
    public Flux<Event<STATE>> observeEvents() {
        return events.asFlux();
    }

    /**
     * <p>
     * Provide the sequence of state transitions of the {@linkplain #getObject()
     * simulated object}.
     * </p>
     * <ul>
     * <li>Each state transition is represented by a {@linkplain TimestampedState
     * time-stamped state}: the state that the simulated object transitioned to, and
     * the time that the simulated object entered that state.</li>
     * <li>The sequence of state transitions has no null transitions.</li>
     * <li>The sequence of state transitions can be infinite.</li>
     * <li>The sequence of state transitions can be finite. In that case, the last
     * state transition is the destruction of the object: a transition to a null
     * {@linkplain TimestampedState#getState() state}.</li>
     * <li>The sequence of state transitions are in
     * {@linkplain Duration#compareTo(Duration) ascending}
     * {@linkplain TimestampedState#getWhen() time-stamp} order.</li>
     * <li>The sequence of state transitions does not include old state transitions;
     * the first state transition published to a subscriber will correspond to the
     * current {@linkplain #getLastEvent() last event} at the time of
     * subscription.</li>
     * </ul>
     */
    @Nonnull
    public Flux<TimestampedState<STATE>> observeStateTransitions() {
        return stateTransitions.asFlux();
    }

}
