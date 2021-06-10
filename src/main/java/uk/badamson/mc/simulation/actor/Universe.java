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

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
 * appended to.
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
    /*
     * Adding entries to the objectHistories Map is guarded by this lock.
     */
    private final Object objectCreationLock = new Object();

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
     * Construct a universe given the histories of all the objects in it.
     * </p>
     * <ul>
     * <li>The {@linkplain #getObjectHistories() object histories} of this universe
     * are {@linkplain Map#equals(Object) equivalent to} the given
     * {@code objectHistories}.</li>
     * </ul>
     *
     * @param objectHistories
     *            A snapshot of the history information of all the objects in the
     *            universe.
     * @throws NullPointerException
     *             If {@code objectHistories} is null
     */
    @JsonCreator
    public Universe(@Nonnull @JsonProperty("objectHistories") final Collection<ObjectHistory<STATE>> objectHistories) {
        Objects.requireNonNull(objectHistories, "objectHistories");
        objectHistories.forEach(history -> this.objectHistories.put(history.getObject(), new ObjectHistory<>(history)));
    }

    /**
     * <p>
     * Copy a universe.
     * </p>
     */
    public Universe(@Nonnull final Universe<STATE> that) {
        Objects.requireNonNull(that, "that");
        synchronized (that.objectCreationLock) {// hard to test
            that.objectHistories
                    .forEach((object, history) -> objectHistories.put(object, new ObjectHistory<>(history)));
        }
    }

    /**
     * <p>
     * Whether this is <dfn>equivalent</dfn> to a given object.
     * </p>
     * <p>
     * The {@link Universe} class has <i>value semantics</i>. This method may give
     * misleading results if either object is modified during the computation of
     * equality.
     * </p>
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Universe)) {
            return false;
        }
        final Universe<?> other = (Universe<?>) obj;
        /*
         * thread-safe because ConcurrentHashMap.equals and ObjectHistory.equals are
         * thread-safe.
         */
        return objectHistories.equals(other.objectHistories);
    }

    /**
     * <p>
     * Get a snapshot of the history information of all the objects in this
     * universe.
     * </p>
     * <ul>
     * <li>The returned collection will not subsequently change due to events.</li>
     * <li>Has no null elements.</li>
     * <li>Has no duplicate elements.</li>
     * </ul>
     */
    @Nonnull
    @JsonProperty("objectHistories")
    public Collection<ObjectHistory<STATE>> getObjectHistories() {
        synchronized (objectCreationLock) {// hard to test
            return objectHistories.values().stream().map(h -> new ObjectHistory<>(h)).collect(toUnmodifiableList());
        }
    }

    /**
     * <p>
     * The unique IDs of the simulated objects in this universe.
     * </p>
     * <ul>
     * <li>The set of object IDs does not contain a null.</li>
     * <li>The set of object IDs is an unmodifiable copy (snapshot) of the set of
     * object IDs; the returned set is constant, and will not update because of
     * subsequent additions of objects.</li>
     * </ul>
     */
    @Nonnull
    @JsonIgnore
    public Set<UUID> getObjects() {
        return Set.copyOf(objectHistories.keySet());
    }

    @Override
    public int hashCode() {
        return objectHistories.hashCode();
    }

    @Override
    public String toString() {
        return "Universe" + objectHistories.values();
    }

}
