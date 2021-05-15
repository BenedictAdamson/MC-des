package uk.badamson.mc.simulation.actor;
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
import reactor.core.publisher.Sinks.EmitResult;
import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.simulation.rx.ModifiableObjectHistory;

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
     * The state of a simulated object, with a time-range indicating the points in
     * time when the simulated object was in that state, and an indication of
     * whether the state is reliable.
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
        private final Duration start;
        @Nonnull
        private final Duration end;
        private final boolean reliable;
        @Nullable
        private final STATE state;

        /**
         * Constructs a value with given attribute values.
         *
         * @throws IllegalArgumentException
         *             If {@code end} {@linkplain Duration#compareTo(Duration) is
         *             before} {@code start}
         */
        public TimestampedState(@Nonnull final Duration start, @Nonnull final Duration end, final boolean reliable,
                @Nullable final STATE state) {
            this.start = Objects.requireNonNull(start, "start");
            this.end = Objects.requireNonNull(end, "end");
            this.reliable = reliable;
            this.state = state;
            if (end.compareTo(start) < 0) {
                throw new IllegalArgumentException("end before start");
            }
        }

        /**
         * <p>
         * Whether this object is <dfn>equivalent</dfn> to a given other object.
         * </p>
         * <p>
         * The TimestampedState class has <i>value semantics</i>.
         * </p>
         */
        @Override
        public boolean equals(final Object that) {
            if (this == that) {
                return true;
            }
            if (!(that instanceof TimestampedState)) {
                return false;
            }
            final TimestampedState<?> other = (TimestampedState<?>) that;
            return reliable == other.reliable && start.equals(other.start) && end.equals(other.end)
                    && Objects.equals(state, other.state);
        }

        /**
         * <p>
         * The last point in time that the simulated object had the
         * {@linkplain #getState() state}
         * </p>
         * <p>
         * This is an <i>inclusive</i> and time. Expressed as the duration since an
         * (implied) epoch.
         * </p>
         * <ul>
         * <li>The end time is {@linkplain Duration#compareTo(Duration) at or after} the
         * {@linkplain #getStart() start} time.</li>
         * </ul>
         */
        @Nonnull
        public Duration getEnd() {
            return end;
        }

        /**
         * <p>
         * The point in time that the simulated object entered the
         * {@linkplain #getState() state}
         * </p>
         * <p>
         * Expressed as the duration since an (implied) epoch. All objects in a
         * simulation should use the same epoch.
         * </p>
         */
        @Nonnull
        public Duration getStart() {
            return start;
        }

        /**
         * <p>
         * The state of the simulated object, in the time-range given by the
         * {@linkplain #getStart() start} and {@linkplain #getEnd() end} times
         * </p>
         * <ul>
         * <li>Null if the object does not exist at that time.</li>
         * </ul>
         */
        @Nullable
        public STATE getState() {
            return state;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + start.hashCode();
            result = prime * result + end.hashCode();
            result = prime * result + (reliable ? 1 : 0);
            result = prime * result + (state == null ? 0 : state.hashCode());
            return result;
        }

        /**
         * <p>
         * Whether this time-stamped state is <i>reliable</i>
         * </p>
         * <p>
         * The opposite of <i>reliable</i> is <i>provisional</i>.
         * </p>
         */
        public boolean isReliable() {
            return reliable;
        }

        @Nonnull
        @Override
        public String toString() {
            return "@(" + start + "," + end + ") " + (reliable ? "reliable" : "provisional") + "=" + state;
        }

    }// class

    @Nonnull
    private final UUID object;
    @Nonnull
    private final Duration start;
    /*
     * Use a UUID object as the lock so all ObjectHistory objects can have a
     * predictable lock ordering when locking two instances.
     */
    protected final UUID lock = UUID.randomUUID();
    private final Sinks.Many<TimestampedState<STATE>> timestampedStates = Sinks.many().replay().latest();

    @Nonnull
    @GuardedBy("lock")
    private final Duration end;
    @Nonnull
    @GuardedBy("lock")
    private final ModifiableValueHistory<STATE> stateHistory;

    /**
     * <p>
     * Copy an object history.
     * </p>
     */
    public ObjectHistory(@Nonnull final ObjectHistory<STATE> that) {
        Objects.requireNonNull(that, "that");
        synchronized (that.object) {// hard to test
            end = that.end;
            stateHistory = new ModifiableValueHistory<>(that.stateHistory);
        }
        object = that.object;
        start = that.start;

        if (end.equals(ValueHistory.END_OF_TIME)) {
            noMoreTimeStampedStates();
        }
    }

    /**
     * <p>
     * Construct an object history with given history information.
     * </p>
     * <ul>
     *
     * @param object
     *            The unique ID of the object for which this is the history.
     * @param end
     *            The last point in time for which this history is reliable.
     * @param stateTransitions
     *            The state transitions
     * @throws NullPointerException
     *             If any {@link Nonnull} argument is null.
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code stateTransitions} is empty.</li>
     *             <li>If {@code end} is {@linkplain Duration#compareTo(Duration)
     *             before} the first time in {@code stateTransitions}.</li>
     *             <li>If adjacent {@linkplain SortedMap#values() values} of
     *             {@code stateTransitions} are
     *             {@linkplain Objects#equals(Object, Object) equivalent or
     *             equivalently null}.</li>
     *             <li>If {@code stateTransitions} contains more than one null
     *             value.</li>
     *             <li>If {@code stateTransitions} has any null
     *             {@linkplain SortedMap#values() values} other than the last. That
     *             is, if the object was destroyed or removed before the last
     *             transition.</li>
     *             <li>The the final value in {@code stateTransitions} is null and
     *             the final state transition is at or before the {@code end} time,
     *             but the {@code end} time is not the
     *             {@linkplain ValueHistory#END_OF_TIME end of time}. That is, if
     *             the arguments indicate that there is reliable information
     *             indicating destruction of the simulated object, but the
     *             reliability information suggests it might be resurrected at a
     *             later time.</li>
     *             </ul>
     */
    @JsonCreator
    public ObjectHistory(@Nonnull @JsonProperty("object") final UUID object,
            @Nonnull @JsonProperty("end") final Duration end,
            @Nonnull @JsonProperty("stateTransitions") final SortedMap<Duration, STATE> stateTransitions) {
        this.object = Objects.requireNonNull(object, "object");
        this.end = Objects.requireNonNull(end, "end");
        this.stateHistory = new ModifiableValueHistory<>(null, stateTransitions);
        // Check after copy to avoid race hazards
        this.start = this.stateHistory.getFirstTansitionTime();
        if (this.start == null) {
            throw new IllegalArgumentException("empty stateTransitions");
        }
        if (end.compareTo(this.start) < 0) {
            throw new IllegalArgumentException("end before first in stateTransitions");
        }
        final var nDestructionEvents = this.stateHistory.getTransitions().values().stream()
                .filter(state -> state == null).count();
        if (1 < nDestructionEvents) {
            throw new IllegalArgumentException("stateTransitions has multiple destruction events");
        }
        if (0 < nDestructionEvents && this.stateHistory.getLastValue() != null) {
            throw new IllegalArgumentException("stateTransitions destruction event is not the last event");
        }
        if (stateHistory.get(end) == null && !ValueHistory.END_OF_TIME.equals(end)) {
            throw new IllegalArgumentException("reliability information suggests destroyed object may be recreated");
        }

        if (end.equals(ValueHistory.END_OF_TIME)) {
            noMoreTimeStampedStates();
        }
    }

    /**
     * <p>
     * Construct an object history with given start information.
     * </p>
     *
     * @param object
     *            The unique ID of the object for which this is the history.
     * @param start
     *            The point in time that this history starts.
     * @param state
     *            The first (known) state transition of the {@code
     *            object}.
     * @throws NullPointerException
     *             If a {@link Nonnull} argument is null
     */
    public ObjectHistory(@Nonnull final UUID object, @Nonnull final Duration start, @Nonnull final STATE state) {
        Objects.requireNonNull(state, "state");
        this.object = Objects.requireNonNull(object, "object");
        this.start = Objects.requireNonNull(start, "start");
        this.end = start;
        this.stateHistory = new ModifiableValueHistory<>();
        this.stateHistory.appendTransition(start, state);
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to a given object.
     * </p>
     * <p>
     * The {@link ObjectHistory} class has <i>value semantics</i>.
     * </p>
     */
    @Override
    public final boolean equals(final Object obj) {
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

    private boolean equalsGuarded(@Nonnull final ObjectHistory<?> other) {
        // hard to test the thread safety
        synchronized (lock) {
            synchronized (other.lock) {
                return end.equals(other.end) && stateHistory.equals(other.stateHistory);
            }
        }
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
     * </ul>
     */
    @Nonnull
    @JsonProperty("end")
    public final Duration getEnd() {
        return end;
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
    @JsonProperty("object")
    public final UUID getObject() {
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
    @JsonIgnore
    public final Duration getStart() {
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
     * <li>The {@linkplain ValueHistory#getFirstTansitionTime() first transition
     * time} of the state history is the same as the {@linkplain #getStart() start}
     * time of this history.</li>
     * <li>The {@linkplain ValueHistory#getFirstValue() state at the start of time}
     * of the state history is null.</li>
     * <li>If the state at the {@linkplain #getEnd() end} time is null (destruction
     * of the {@linkplain #getObject() object}), the end time is the
     * {@linkplain ValueHistory#END_OF_TIME end of time}. That is, if reliable state
     * information indicates that the simulated object was destroyed, it is
     * guaranteed that the simulated object will never be recreated.</li>
     * <li>The returned state history is a snapshot: a copy of data, it is not
     * updated if this object history is subsequently changed.</li>
     * </ul>
     * <p>
     * Using the sequence provided by {@link #observeState(Duration)} to acquire the
     * state at a given point in time is typically better than using the snapshot of
     * the state history, because the snapshot is not updated, and because creating
     * the snapshot can be expensive.
     * </p>
     *
     * @see #observeState(Duration)
     */
    @Nonnull
    @JsonIgnore
    public final ValueHistory<STATE> getStateHistory() {
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
    @JsonProperty("stateTransitions")
    public final SortedMap<Duration, STATE> getStateTransitions() {
        synchronized (lock) {// hard to test
            /*
             * No need to make a defensive copy: ModifiableValueHistory.getTransitions()
             * does that for us.
             */
            return stateHistory.getTransitions();
        }
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        synchronized (lock) {// hard to test thread safety
            result = prime * result + object.hashCode();
            result = prime * result + end.hashCode();
            result = prime * result + stateHistory.hashCode();
        }
        return result;
    }

    private boolean lockBefore(@Nonnull final ObjectHistory<?> that) {
        assert !lock.equals(that.lock);
        return lock.compareTo(that.lock) < 0;
    }

    private void noMoreTimeStampedStates() {
        assert this.end.equals(ValueHistory.END_OF_TIME);
        final var result = timestampedStates.tryEmitComplete();
        // The sink is reliable; it should always successfully complete.
        assert result == EmitResult.OK;
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
     * with successive values typically being closer to the correct value.</li>
     * <li>The sequence of states does not contain successive duplicates.</li>
     * <li>The sequence rapidly publishes the first state of the sequence.</li>
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
    public final Publisher<Optional<STATE>> observeState(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        synchronized (lock) {// hard to test
            final var observeStateFromStateHistory = Mono.just(Optional.ofNullable(stateHistory.get(when)));
            if (when.compareTo(end) <= 0) {
                return observeStateFromStateHistory;
            } else {
                return Flux.concat(observeStateFromStateHistory, observeStateFromStateTransitions(when))
                        .distinctUntilChanged();
            }
        }
    }

    private Flux<Optional<STATE>> observeStateFromStateTransitions(@Nonnull final Duration when) {
        // TODO use reliable
        return timestampedStates.asFlux().takeWhile(timeStamped -> timeStamped.getStart().compareTo(when) <= 0)
                .takeUntil(timeStamped -> when.compareTo(timeStamped.getStart()) <= 0)
                .map(timeStamped -> Optional.ofNullable(timeStamped.getState()));
    }

    /**
     * <p>
     * Provide a sequence of time-stamped states for the {@linkplain #getObject()
     * simulated object}.
     * </p>
     * <ul>
     * <li>Each time-stamped provides <i>complete</i> information for a state: the
     * {@linkplain TimestampedState#getStart() start} time is the time that the
     * simulated object, and the {@linkplain #getEnd() end} time is the last point
     * in time before the next next state transition (or the
     * {@linkplain ValueHistory#END_OF_TIME end of time}, if there is no following
     * state transition).</li>
     * <li>The sequence of time-stamped states can be infinite.</li>
     * <li>The sequence of time-stamped states can override
     * {@linkplain TimestampedState#isReliable() provisional} values: if there is a
     * provisional value in the sequence, a later value may have a time range that
     * overlaps the time range of the provisional value.</li>
     * <li>The sequence never overrides {@linkplain TimestampedState#isReliable()
     * reliable} values: if a value is reliable, no subsequent value will overlap
     * the time range of the reliable value.</li>
     * <li>The sequence of time-stamped states can be finite. In that case, the last
     * state transition is a a {@linkplain TimestampedState#isReliable() reliable}
     * indication of the destruction of the object: a transition to a null
     * {@linkplain TimestampedState#getState() state}.</li>
     * <li>The sequence of time-stamped states are not in any time order, but will
     * tend to be in roughly ascending time order.</li>
     * <li>The sequence of time-stamped states does not include old values; it is a
     * <i>hot</i> observable. The publisher emits values only when there is a change
     * to the {@linkplain #getStateHistory() state history}. In particular, it does
     * not emit an values for a newly constructed object history.</li>
     * </ul>
     */
    @Nonnull
    public final Flux<TimestampedState<STATE>> observeTimestampedStates() {
        return timestampedStates.asFlux();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + object + "from " + start + " to " + end + "stateHistory="
                + stateHistory + "]";
    }

}
