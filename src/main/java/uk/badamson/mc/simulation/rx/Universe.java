package uk.badamson.mc.simulation.rx;
/*
 * © Copyright Benedict Adamson 2018m2021.
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

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * <p>
 * A collection of simulated objects and their {@linkplain ObjectHistory
 * histories}.
 * </p>
 * <p>
 * The histories of the objects may be <dfn>asynchronous</dfn>: different
 * objects may have state transitions at different times.</li>
 * <p>
 * This collection is modifiable: the histories of the simulated objects may be
 * appended to. This collection enforces constraints that ensure that the object
 * histories are <dfn>consistent</dfn>. Consistency means that if a universe
 * contains an object state, it also contains all the depended upon states of
 * that state.
 * </p>
 *
 * @param <STATE>
 *            The class of states of the simulated objects. This must be
 *            {@link Immutable immutable}. It ought to have value semantics, but
 *            that is not required.
 */
@ThreadSafe
public final class Universe<STATE> {

    private final Map<UUID, ObjectHistory<STATE>> objectHistories = new ConcurrentHashMap<>();

    /**
     * <p>
     * Construct an empty universe.
     * </p>
     * <ul>
     * <li>The {@linkplain #getObjects() set of objects} {@linkplain Set#isEmpty()
     * is empty}.</li>
     * </ul>
     */
    public Universe() {
        // Do nothing
    }

    /**
     * <p>
     * Add an object with given start information to the collection of objects in
     * this universe.
     * </p>
     * <p>
     * Invariants:
     * </p>
     * <ul>
     * <li>Does not remove any objects from the {@linkplain #getObjects() set of
     * objects}.</li>
     * </ul>
     * <p>
     * Post conditions:
     * </p>
     * <ul>
     * <li>The {@linkplain #getObjects() set of objects}
     * {@linkplain Set#contains(Object) contains} the {@linkplain Event#getObject()
     * object} of the {@code event}.</li>
     * <li>Adds one object to the set of objects.</li>
     * </ul>
     *
     * @param event
     *            The first (known) state transition of the object.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code event} is null</li>
     *             <li>if the {@linkplain Event#getState() state} of {@code event}
     *             is null. That is, if the first event is the destruction or
     *             removal of the simulated object.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If the {@linkplain #getObjects() set of objects} in this universe
     *             already {@linkplain Set#contains(Object) contains} the
     *             {@linkplain Event#getObject() object} of the {@code event}.
     */
    public void addObject(@Nonnull final Event<STATE> event) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(event.getState(), "event.state");

        objectHistories.compute(event.getObject(), (k, v) -> {
            if (v == null) {
                return new ObjectHistory<>(event);
            } else {
                throw new IllegalArgumentException("Already present");
            }
        });
    }

    /**
     * <p>
     * The unique IDs of the simulated objects in this universe.
     * </p>
     * <ul>
     * <li>The set of object IDs is not null.</li>
     * <li>The set of object IDs does not contain a null.</li>
     * <li>The set of object IDs is an unmodifiable copy (snapshot) of the set of
     * object IDs; the returned set is constant, and will not update because of
     * subsequent additions of objects.</li>
     * </ul>
     */
    @Nonnull
    public Set<UUID> getObjects() {
        return Set.copyOf(objectHistories.keySet());
    }

}
