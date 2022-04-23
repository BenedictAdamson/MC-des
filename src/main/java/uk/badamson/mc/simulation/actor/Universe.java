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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>
 * A collection of {@linkplain Actor simulated objects and their histories}.
 * </p>
 * <p>
 * The histories of the objects may be <dfn>asynchronous</dfn>: different
 * objects may have state transitions at different times.
 * <p>
 * This collection is modifiable: the histories of the simulated objects may be
 * appended to.
 * </p>
 *
 * @param <STATE> The class of states of the simulated objects. This must be
 *                {@link Immutable immutable}. It ought to have value semantics, but
 *                that is not required.
 */
@ThreadSafe
public final class Universe<STATE> {

    private final Collection<Actor<STATE>> actors = new CopyOnWriteArrayList<>();

    private final Object lock = new Object();

    /**
     * <p>
     * Construct an empty universe.
     * </p>
     * <ul>
     * <li>The {@linkplain #getActors() collection of actors} {@linkplain Collection#isEmpty()
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
     * <li>The {@linkplain #getActors() object histories} of this universe
     * are {@linkplain Map#equals(Object) equivalent to} the given
     * {@code objectHistories}.</li>
     * </ul>
     *
     * @param actors A snapshot of the history information of all the objects in the
     *               universe.
     * @throws NullPointerException If {@code objectHistories} is null
     */
    public Universe(@Nonnull final Collection<Actor<STATE>> actors) {
        Objects.requireNonNull(actors, "actors");
        this.actors.addAll(actors);
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
     * <li>May be unmodifiable.</li>
     * </ul>
     */
    @Nonnull
    public Collection<Actor<STATE>> getActors() {
        synchronized (lock) {// hard to test
            return List.copyOf(actors);
        }
    }

    @Override
    public String toString() {
        return "Universe" + actors;
    }

}
