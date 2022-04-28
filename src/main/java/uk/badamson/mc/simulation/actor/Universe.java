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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * <p>
 * A collection of {@linkplain Actor simulated objects and their histories}.
 * </p>
 * <p>
 * The histories of the objects may be <dfn>asynchronous</dfn>: different
 * objects may have state transitions at different times.
 * </p>
 * <ul>
 *     <li>Does not {@linkplain #contains(Object) contain} nulls.</li>
 *     <li>Does not contain} duplicates.</li>
 * </ul>
 *
 * @param <STATE> The class of states of the simulated objects. This must be
 *                {@link Immutable immutable}. It ought to have value semantics, but
 *                that is not required.
 */
@ThreadSafe
public final class Universe<STATE> implements Collection<Actor<STATE>> {

    private final Map<UUID, Actor<STATE>> actors = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public Iterator<Actor<STATE>> iterator() {
        return actors.values().iterator();
    }

    @Override
    public int size() {
        return actors.size();
    }

    @Override
    public boolean add(final Actor<STATE> actor) {
        Objects.requireNonNull(actor);
        return actors.putIfAbsent(actor.lock, actor) == null;
    }

    @Override
    public void clear() {
        actors.clear();
    }

    @Override
    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        final Universe<?> universe = (Universe<?>) that;

        return actors.equals(universe.actors);
    }

    @Override
    public int hashCode() {
        return actors.hashCode();
    }

    @Override
    public boolean contains(final Object o) {
        if (!(o instanceof Actor)) {
            return false;
        }
        final Actor<?> actor = (Actor<?>) o;
        return actors.containsKey(actor.lock);
    }

    @Override
    public boolean isEmpty() {
        return actors.isEmpty();
    }

    @Override
    public Spliterator<Actor<STATE>> spliterator() {
        return actors.values().spliterator();
    }

    @Override
    public boolean remove(final Object o) {
        if (!(o instanceof Actor<?>)) {
            return false;
        }
        return removeActor((Actor<?>) o);
    }

    @Override
    public boolean removeAll(final Collection<?> objects) {
        return objects.stream()
                .filter(o -> o instanceof Actor<?>)
                .map(o -> (Actor<?>) o)
                .map(this::removeActor)
                .reduce((r1, r2) -> r1 || r2)
                .orElse(Boolean.FALSE);
    }

    private boolean removeActor(final Actor<?> actor) {
        return actors.remove(actor.lock) != null;
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return actors.values().toArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull final T[] a) {
        return actors.values().toArray(a);
    }

    @Override
    public boolean containsAll(@Nonnull final Collection<?> c) {
        return actors.values().containsAll(c);
    }

    @Override
    public boolean addAll(@Nonnull final Collection<? extends Actor<STATE>> c) {
        return c.stream()
                .map(this::add)
                .reduce((a1, a2) -> a1 || a2)
                .orElse(false);
    }

    @Override
    public boolean retainAll(@Nonnull final Collection<?> c) {
        return actors.values().retainAll(c);
    }

    /**
     * <p>
     * Have the actors of this Universe {@link Actor#receiveSignal() receive signals}
     * until the {@link  Actor#getWhenReceiveNextSignal time of their next signal}
     * is {@link Duration#compareTo(Duration) at or after} a given time,
     * with the processing done using a given {@link Executor}.
     * </p>
     *
     * @return a Future that {@linkplain Future#isDone() is done} when all the actors have been advanced,
     * or an exception prevents a full computation.
     * @throws NullPointerException if any {@link Nonnull} annotated argument is null.
     */
    @Nonnull
    public Future<Void> advanceTo(
            @Nonnull final Duration when,
            @Nonnull final Executor executor
    ) {
        Objects.requireNonNull(when, "when");
        Objects.requireNonNull(executor, "executor");
        return Actor.advanceSeveralActors(when, this, executor);
    }
}
