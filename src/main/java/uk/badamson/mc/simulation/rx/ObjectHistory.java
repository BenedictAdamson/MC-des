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
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import org.reactivestreams.Publisher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;

/**
 * <p>
 * The sequence of state transitions of a simulated object.
 * </p>
 * <p>
 * This concrete base class is not <i>modifiable</i>: additional state
 * transitions may not be added after construction. The
 * {@link ModifiableObjectHistory} derived class however is modifiable.
 * </p>
 *
 * @param <STATE>
 *            The class of states of the simulated object. This must be
 *            {@link Immutable immutable}. It ought to have value semantics, but
 *            that is not required.
 */
@ThreadSafe
public class ObjectHistory<STATE> {

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
    protected final Object lock = new Object();
    private final Sinks.Many<Event<STATE>> events = Sinks.many().replay().latest();
    private final Sinks.Many<TimestampedState<STATE>> stateTransitions = Sinks.many().replay().latest();

    @Nonnull
    @GuardedBy("lock")
    private final ModifiableValueHistory<STATE> stateHistory;

    @Nonnull
    @GuardedBy("lock")
    protected Event<STATE> lastEvent;

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
        this.stateHistory = new ModifiableValueHistory<>();
        append1(event);
    }

    /**
     * <p>
     * Copy an object history.
     * </p>
     *
     * @throws NullPointerException
     *             If {@code that} is null
     *
     */
    public ObjectHistory(@Nonnull final ObjectHistory<STATE> that) {
        Objects.requireNonNull(that, "that");
        synchronized (that.object) {// hard to test
            lastEvent = that.lastEvent;
            stateHistory = new ModifiableValueHistory<>(that.stateHistory);
        }
        object = that.object;
        start = that.start;
        emitLastEvent();
    }

    /**
     * <p>
     * Construct an object history with given history information.
     * </p>
     * <ul>
     * <li>The {@linkplain #getLastEvent() last event} of this history is the given
     * {@code event}.</li>
     * <li>The {@linkplain #getPreviousStateTransitions() previous state
     * transitions} of this history is {@linkplain SortedMap#equals(Object)
     * equivalent to} the given {@code previousStateTransitions}
     * </ul>
     *
     * @param previousStateTransitions
     *            The state transitions before the {@code lastEvent}
     * @param lastEvent
     *            The last (known) state transition of the {@linkplain #getObject()
     *            object}.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code stateTransitions} is null</li>
     *             <li>If {@code lastEvent} is null</li>
     *             <li>If {@code stateTransitions} is empty and the
     *             {@linkplain Event#getState() state} of {@code lastEvent} is null.
     *             That is, if the first event is the destruction or removal of the
     *             {@linkplain #getObject() simulated object}.</li>
     *             <li>If {@code stateTransitions} has any null
     *             {@linkplain SortedMap#values() values}. That is, if the object
     *             was destroyed or removed before the {@code lastEvent} .</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the {@linkplain Event#getWhen() time} of the
     *             {@code lastEvent} is not {@linkplain Duration#compareTo(Duration)
     *             after} the {@linkplain SortedMap#lastKey() last} of the given
     *             {@code previousStateTransitions}.</li>
     *             <li>If any {@linkplain SortedMap#values() value} of
     *             {@code previousStateTransitions} is
     *             {@linkplain Objects#equals(Object, Object) equal to (or equally
     *             null as)} its predecessor value.</li>
     *             </ul>
     */
    @JsonCreator
    public ObjectHistory(
            @Nonnull @JsonProperty("previousStateTransitions") final SortedMap<Duration, STATE> previousStateTransitions,
            @JsonProperty("lastEvent") @Nonnull final Event<STATE> lastEvent) {
        this.lastEvent = Objects.requireNonNull(lastEvent, "lastEvent");// redundant; satisfy SpotBugs
        this.object = lastEvent.getObject();
        this.start = previousStateTransitions.isEmpty() ? lastEvent.getWhen() : previousStateTransitions.firstKey();
        this.stateHistory = new ModifiableValueHistory<>(null, previousStateTransitions);
        append1(lastEvent);
    }

    protected final void append1(final Event<STATE> event) {
        lastEvent = event;
        final var state = event.getState();
        if (stateHistory.isEmpty() || !stateHistory.getLastValue().equals(state)) {
            stateHistory.appendTransition(event.getWhen(), state);
        }
        emitLastEvent();
        if (state == null) {// destruction
            final var result3 = events.tryEmitComplete();
            final var result4 = stateTransitions.tryEmitComplete();
            assert result3 == Sinks.EmitResult.OK;
            assert result4 == Sinks.EmitResult.OK;
        }
    }

    private void emitLastEvent() {
        final var result1 = events.tryEmitNext(lastEvent);
        final var result2 = stateTransitions
                .tryEmitNext(new TimestampedState<>(lastEvent.getWhen(), lastEvent.getState()));
        // The sink are reliable, so should always succeed.
        assert result1 == Sinks.EmitResult.OK;
        assert result2 == Sinks.EmitResult.OK;
    }

    /**
     * <p>
     * The last point in time for which this history is reliable.
     * </p>
     * <ul>
     * <li>Expressed as the duration since an (implied) epoch. All objects in a
     * simulation should use the same epoch.</li>
     * <li>The end time is {@linkplain Duration#compareTo(Duration) at or after} the
     * {@linkplain #getStart() start} time.</li>
     * <li>If the {@linkplain Event#getState() state transitioned to} by the
     * {@linkplain #getLastEvent() last event} is null (that is, the last event was
     * destruction or removal of the {@linkplain #getObject() simulated object}),
     * the end time is the {@linkplain ValueHistory#END_OF_TIME end of time}.</li>
     * <li>If the state transitioned to by the last event is not null, the end time
     * is the {@linkplain Event#getWhen() time} of that event.</li>
     * </ul>
     */
    @Nonnull
    @JsonIgnore
    public Duration getEnd() {
        final var event = getLastEvent();
        if (event.getState() == null) {
            return ValueHistory.END_OF_TIME;
        } else {
            return event.getWhen();
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
    @JsonIgnore
    public UUID getObject() {
        return object;
    }

    /**
     * <p>
     * The {@linkplain ValueHistory#getTransitions() transitions} in the
     * {@linkplain #getStateHistory() state history} of this object history
     * {@linkplain SortedMap#headMap(Object) before} the {@linkplain #getLastEvent()
     * last event} of this object history.
     * </p>
     */
    @Nonnull
    public SortedMap<Duration, STATE> getPreviousStateTransitions() {
        synchronized (lock) {// hard to test
            final var transitions = stateHistory.getTransitions();
            return transitions.headMap(transitions.lastKey());
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
    @JsonIgnore
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
     * of the state history is typically the same as the {@linkplain Event#getWhen()
     * time} of the {@linkplain #getLastEvent() last event} of this history. But it
     * will not be if there are events that do not actually change the state.</li>
     * <li>The {@linkplain ValueHistory#getLastValue() state at the end of time} of
     * the state history is the same as the {@linkplain Event#getState() state} of
     * the {@linkplain #getLastEvent() last event} of this history.</li>
     * <li>The state history is never {@linkplain ValueHistory#isEmpty()
     * empty}.</li>
     * <li>The returned state history is a snapshot: a copy of data, it is not
     * updated if events are appended.</li>
     * </ul>
     * <p>
     * Using the sequence provided by {@link #observeState(Duration)} to acquire the
     * state at a known point in time is typically better than using the snapshot of
     * the state history, because the snapshot is not updated, and because creating
     * the snapshot can be expensive.
     * </p>
     *
     * @see #observeState(Duration)
     */
    @Nonnull
    @JsonIgnore
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
     * Provide the state of the {@linkplain #getObject() simulated object} at a
     * given point in time.
     * </p>
     * <ul>
     * <li>Because {@link Publisher} can not provide null values, the sequence uses
     * {@link Optional}, with null states (that is, the state of not existing)
     * indicated by an {@linkplain Optional#isEmpty() empty} Optional.</li>
     * <li>The sequence of states is finite.</li>
     * <li>The last state of the sequence of states is the state at the given point
     * in time.</li>
     * <li>The last state of the sequence of states may be proceeded may
     * <i>provisional</i> values for the state at the given point in time. These
     * provisional values will typically be approximations of the correct value,
     * with successive values being closer to the correct value.</li>
     * <li>The sequence of states does not contain successive duplicates.</li>
     * <li>The time between publication of the last state of the sequence and
     * completion of the sequence can be a large. That is, the process of providing
     * a value and then concluding that it is the correct value rather than a
     * provisional value can be time consuming.</li>
     * <li>If the given point in time is {@linkplain Duration#compareTo(Duration) at
     * or before} the current {@linkplain #getEnd() end} time of this history, the
     * method can return a {@linkplain Mono#just(Object) sequence that immediately
     * provides} the correct value.</li>
     * <li>All states published are the same as the value that could be
     * {@linkplain ValueHistory#get(Duration) obtained} from the
     * {@linkplain #getStateHistory() snapshot of the state history} at the time of
     * publication.</li>
     * </ul>
     *
     * @param when
     *            The point in time of interest, expressed as the duration since an
     *            (implied) epoch. All objects in a simulation should use the same
     *            epoch.
     * @throws NullPointerException
     *             If {@code when} is null.
     * @see #getStateHistory()
     */
    @Nonnull
    public Publisher<Optional<STATE>> observeState(@Nonnull final Duration when) {
        synchronized (lock) {// hard to test
            if (when.compareTo(lastEvent.getWhen()) <= 0) {
                return Mono.just(Optional.ofNullable(stateHistory.get(when)));
            } else {
                return stateTransitions.asFlux().takeWhile(timeStamped -> timeStamped.getWhen().compareTo(when) <= 0)
                        .takeUntil(timeStamped -> when.compareTo(timeStamped.getWhen()) <= 0)
                        .map(timeStamped -> Optional.ofNullable(timeStamped.getState())).distinctUntilChanged();
            }
        }
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
