package uk.badamson.mc.simulation.rx;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import uk.badamson.mc.simulation.ObjectStateId;

/**
 * <p>
 * A state transition of a simulated object.
 * </p>
 *
 * @param <STATE>
 *            The class of object states of this event class. This must be
 *            {@link Immutable immutable}. It ought to have value semantics, but
 *            that is not required.
 */
@Immutable
public abstract class Event<STATE> implements Comparable<Event<STATE>> {
    private static Map<UUID, Duration> requireValidNextEventDependencies(
            final Map<UUID, Duration> nextEventDependencies, @Nonnull final ObjectStateId id) {
        Objects.requireNonNull(nextEventDependencies, "nextEventDependencies");
        final var object = id.getObject();
        final var when = id.getWhen();
        if (nextEventDependencies.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
            throw new NullPointerException("nextEventDependencies key or value " + nextEventDependencies);
        }
        if (nextEventDependencies.entrySet().stream()
                .anyMatch(entry -> object.equals(entry.getKey()) || when.compareTo(entry.getValue()) <= 0)) {
            throw new IllegalArgumentException("nextEventDependencies " + nextEventDependencies + " for " + id);
        }

        return nextEventDependencies;
    }

    private final ObjectStateId id;
    private final STATE state;
    private final Map<UUID, Duration> nextEventDependencies;

    /**
     * <p>
     * Construct an event with given attributes and aggregates.
     * </p>
     *
     * @param id
     *            The unique ID of {@linkplain #getState() state} that the
     *            {@linkplain #getObject() simulated object} has as a result of this
     *            event.
     * @param state
     *            The state that the simulated object has as a result of this event.
     * @param nextEventDependencies
     *            The identities of other object states that influence what the next
     *            event (after this event) of the simulated object will be. The
     *            given map must be constant (unmodifiable and never subsequently
     *            changing).
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code id} is null.</li>
     *             <li>If {@code state} is null.</li>
     *             <li>If {@code nextEventDependencies} is null.</li>
     *             <li>If {@code nextEventDependencies} has a null
     *             {@linkplain Map#keySet() key}.</li>
     *             <li>If {@code nextEventDependencies} has a null
     *             {@linkplain Map#values() value}.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If {@code nextEventDependencies} has a {@linkplain Map#values()
     *             value} {@linkplain Duration#compareTo(Duration) at or after}
     *             {@code when}.
     */
    protected Event(@Nonnull final ObjectStateId id, @Nonnull final STATE state,
            @Nonnull final Map<UUID, Duration> nextEventDependencies) {
        this.id = Objects.requireNonNull(id, "id");
        this.state = Objects.requireNonNull(state, "state");
        this.nextEventDependencies = requireValidNextEventDependencies(nextEventDependencies, id);
    }

    /**
     * <p>
     * The natural ordering of {@link Event}s.
     * </p>
     * <ul>
     * <li>The <i>natural ordering</i> of {@link Event} is the
     * {@linkplain ObjectStateId#compareTo(ObjectStateId) natural ordering} of the
     * {@linkplain #getId() IDs} of the events.</li>
     * <li>Hence the <i>natural ordering</i> of {@link Event} is consistent with
     * {@linkplain #equals(Object) equals}.</li>
     * </ul>
     */
    @Override
    public final int compareTo(@Nullable final Event<STATE> that) {
        Objects.requireNonNull(that, "that");
        return id.compareTo(that.getId());
    }

    /**
     * <p>
     * Compute the next event (after this event) of the {@linkplain #getObject()
     * object}, given state information about the
     * {@linkplain #getNextEventDependencies() dependencies of the next event}.
     * </p>
     * <ul>
     * <li>Not null.</li>
     * <li>For the same object as the {@linkplain #getObject() object} of this
     * event.</li>
     * <li>For a time after the {@linkplain #getWhen() time} of this event.</li>
     * <li>The computation must be deterministic in the dependent state information
     * and the attributes of this event.</li>
     * <li>The computation typically also makes use of the {@linkplain #getState()
     * state} of the simulated object just before the next event.</li>
     * </ul>
     */
    @Nonnull
    public abstract Event<STATE> computeNextEvent(Map<UUID, STATE> dependentStates);

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to a given object.
     * </p>
     * <p>
     * The Event class has <i>entity semantics</i> with the {@linkplain #getId() ID}
     * acting as the unique ID: this object is equivalent to another object if, and
     * only if, the other object is also an Event and the two have
     * {@linkplain ObjectStateId#equals(Object) equivalent} IDs.
     * </p>
     */
    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Event)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        final Event<STATE> other = (Event<STATE>) obj;
        return id.equals(other.id);
    }

    /**
     * <p>
     * The unique ID of {@linkplain #getState() state} that the simulated object has
     * as a result of this event.
     * </p>
     * <ul>
     * <li>The {@linkplain ObjectStateId#getWhen() time-stamp} of the ID is point in
     * time that the this event occurs.</li>
     * <li>The {@linkplain ObjectStateId#getObject() object} of the ID is the ID of
     * the object for which this event occurs.</li>
     * </ul>
     */
    @Nonnull
    public final ObjectStateId getId() {
        return id;
    }

    /**
     * <p>
     * The identities of other object states that influence what the next event
     * (after this event) of the simulated object will be.
     * </p>
     * <p>
     * Maps an object ID to the time-stamp of the state of that object that
     * {@linkplain #computeNextEvent(Map) computation of the next event} depends on.
     * </p>
     * <ul>
     * <li>Not null.</li>
     * <li>Not containing any null {@linkplain Map#keySet() keys}.</li>
     * <li>Not containing any null {@linkplain Map#values() values}.</li>
     * <li>Constant: the object always returns the same next event dependencies.
     * This ensures that the simulation is deterministic.</li>
     * <li>All the time-stamps (values) are {@linkplain Duration#compareTo(Duration)
     * before} {@linkplain #getWhen() when} this event occurs. This ensures
     * causality.</li>
     * <li>None of the object IDs (keys) are equal to the {@linkplain #getObject()
     * object ID} of this event. Computation of the next event is always implicitly
     * dependent on the current state of that object.</li>
     * <li>The map may be empty.</li>
     * </ul>
     */
    @Nonnull
    public final Map<UUID, Duration> getNextEventDependencies() {
        return nextEventDependencies;
    }

    /**
     * <p>
     * The unique ID of the object for which this is an event
     * </p>
     * <ul>
     * <li>The same as the {@linkplain ObjectStateId#getObject() object ID} of the
     * {@linkplain #getId() ID} of this event.</li>
     * </ul>
     */
    @Nonnull
    public final UUID getObject() {
        return id.getObject();
    }

    /**
     * <p>
     * The state that the {@linkplain #getObject() simulated object} has as a result
     * of this event.
     * </p>
     * <ul>
     * <li>Returns a null state if the object ceases to exist as a result of this
     * event. That is, if this event is the destruction of the object.</li>
     * <li>This is for a discrete event simulation: the object implicitly has the
     * same state until the {@linkplain #computeNextEvent(Map) next event}.</li>
     * </ul>
     */
    @Nullable
    public final STATE getState() {
        return state;
    }

    /**
     * <p>
     * The point in time that this event occurs.
     * </p>
     * <p>
     * Expressed as the duration since an (implied) epoch. All objects in a
     * simulation should use the same epoch.
     * </p>
     * <ul>
     * <li>The same as the {@linkplain ObjectStateId#getWhen() time-stamp} of the
     * {@linkplain #getId() ID} of this event.</li>
     * </ul>
     */
    @Nonnull
    public final Duration getWhen() {
        return id.getWhen();
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

}
