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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public abstract class Event<STATE> {

    private static <STATE> Map<UUID, Duration> requireValidNextEventDependencies(
            final Map<UUID, Duration> nextEventDependencies, @Nonnull final ObjectStateId id, final STATE state) {
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
        if (state == null && !nextEventDependencies.isEmpty()) {
            throw new IllegalArgumentException("destruction events must have no nextEventDependencies");
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
     *            Null state if the object ceases to exist as a result of this
     *            event. That is, if this event is the destruction of the object.
     * @param nextEventDependencies
     *            The identities of other object states that influence what the next
     *            event (after this event) of the simulated object will be. The
     *            given map must be constant (unmodifiable and never subsequently
     *            changing).
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code id} is null.</li>
     *             <li>If {@code nextEventDependencies} is null.</li>
     *             <li>If {@code nextEventDependencies} has a null
     *             {@linkplain Map#keySet() key}.</li>
     *             <li>If {@code nextEventDependencies} has a null
     *             {@linkplain Map#values() value}.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code nextEventDependencies} has a
     *             {@linkplain Map#values() value}
     *             {@linkplain Duration#compareTo(Duration) at or after}
     *             {@code when}.</li>
     *             <li>If {@code state} is null and {@code nextEventDependencies} is
     *             not {@linkplain Map#isEmpty() empty}.</li>
     *             </ul>
     */
    protected Event(@Nonnull final ObjectStateId id, @Nullable final STATE state,
            @Nonnull final Map<UUID, Duration> nextEventDependencies) {
        this.id = Objects.requireNonNull(id, "id");
        this.state = state;
        this.nextEventDependencies = requireValidNextEventDependencies(nextEventDependencies, id, state);
    }

    /**
     * <p>
     * Compute the next event (after this event) of the {@linkplain #getObject()
     * object}, and any <i>object creation events</i>, given state information about
     * the {@linkplain #getNextEventDependencies() dependencies}.
     * </p>
     *
     * <p>
     * The returned map
     * </p>
     * <ul>
     * <li>Is not null.</li>
     * <li>Is not {@linkplain Map#isEmpty() empty}.</li>
     * <li>Maps the IDs of simulated objects to an event for each of those simulated
     * objects.</li>
     * <li>Has no null {@linkplain Map#keySet() keys} (simulated object IDs).</li>
     * <li>Has no null {@linkplain Map#values() values} (events).</li>
     * <li>Has only entries for which the simulated object ID (entry key) is
     * {@linkplain UUID#equals(Object) equivalent to} the
     * {@linkplain Event#getObject() object} of the event (entry value).</li>
     * <li>Has an entry for the same object as the {@linkplain #getObject() object}
     * of this event.</li>
     * <li>Has only entries for which the {@linkplain Event#getWhen() event times}
     * are the same.</li>
     * <li>Has only entries for which the {@linkplain Event#getWhen() event times}
     * are after the {@linkplain #getWhen() time} of this event.</li>
     * <li>May have a destruction event (an event with a null
     * {@linkplain #getState() state}) only for the same object as the
     * {@linkplain #getObject() object} of this event.</li>
     * <li>The computation must be deterministic in the dependent state information
     * and the attributes of this event.</li>
     * <li>The computation typically also makes use of the {@linkplain #getState()
     * state} of the simulated object.</li>
     * </ul>
     * <p>
     * Entries in the map for different simulated objects than the
     * {@linkplain #getObject() object} of this event are <dfn>object creation
     * events</dfn>. Those events are the first events (in {@linkplain #getWhen()
     * time} for those objects. The method must ensure that the object IDs of the
     * created objects are {@linkplain UUID#equals(Object) different} from the IDs
     * of all other objects, for example by using {@linkplain UUID#randomUUID() new
     * random} IDs.
     * </p>
     * {@linkplain #getNextEventDependencies() Event dependencies} must respect
     * causality with respect to object creation. No event may depend on the state
     * of an object before its creation.
     * </p>
     *
     * @param dependentStates
     *            Information about the states the the
     *            {@linkplain #getNextEventDependencies() depended on}. The map maps
     *            the ID of a depended on object to the state of the depended on
     *            object. The depended on objects, and the time-stamps of the states
     *            of interest, are given by the
     *            {@linkplain #getNextEventDependencies() next event dependencies}
     *            value. The given map may have null values or missing entries for
     *            depended on objects that do not exist (because they have been
     *            destroyed or removed).
     * @throws NullPointerException
     *             If {@code dependentStates} is null
     * @throws IllegalStateException
     *             If the {@linkplain #getState() state} transitioned to due to this
     *             event was null. That is, if this event was the destruction or
     *             removal of the {@linkplain #getObject() simulated object}:
     *             destroyed objects may not be resurrected.
     */
    @Nonnull
    public abstract Map<UUID, Event<STATE>> computeNextEvents(@Nonnull Map<UUID, STATE> dependentStates);

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
     * {@linkplain #computeNextEvents(Map) computation of the next event} depends
     * on.
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
     * <li>The collection of next event dependencies may be
     * {@linkplain Map#isEmpty() empty}.</li>
     * <li>If this event is a destruction event (the {@link #getState() state} after
     * this event is null), the collection of next event dependencies is empty.</li>
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
    @JsonIgnore
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
     * same state until the {@linkplain #computeNextEvents(Map) next event}.</li>
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
    @JsonIgnore
    public final Duration getWhen() {
        return id.getWhen();
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Event [" + id + "]";
    }

}
