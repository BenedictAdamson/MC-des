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
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    final class SchedulingMedium implements Medium<STATE> {

        @Nonnull
        private final Executor executor;

        @Nonnull
        private final Duration advanceTo;

        SchedulingMedium(@Nonnull final Executor executor, @Nonnull final Duration advanceTo) {
            this.executor = Objects.requireNonNull(executor, "executor");
            this.advanceTo = Objects.requireNonNull(advanceTo, "advanceTo");
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * Furthermore, for this type, adds the signals to the
         * {@linkplain Universe#getObjectHistories() object histories} of the
         * {@linkplain #getUniverse() universe}, as
         * {@linkplain ObjectHistory#getIncomingSignals() incoming signals} of their
         * {@linkplain Signal#getReceiver() receivers}
         * </p>
         */
        @Override
        public void addAll(@Nonnull final Collection<Signal<STATE>> signals) {
            Objects.requireNonNull(signals, "signals");
            // TODO optimize by grouping signals by receiver
            for (final var signal : signals) {
                Objects.requireNonNull(signal, "signal");
                final var receiver = signal.getReceiver();
                final var history = objectHistories.get(receiver);
                if (history == null) {
                    throw new IllegalStateException("unknown receiver for " + signal);
                }
                history.addIncomingSignal(signal);
                scheduleAdvanceObject(receiver);
            } // for
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * All the {@linkplain ObjectHistory#getReceivedAndIncomingSignals() received
         * and incoming signals} in the {@linkplain Universe#getObjectHistories() object
         * histories} of the {@linkplain #getUniverse() universe} of this medium.
         * </p>
         */
        @Nonnull
        @Override
        public Set<Signal<STATE>> getSignals() {
            return getUniverse().getObjectHistories().stream()
                    .flatMap(history -> history.getReceivedAndIncomingSignals().stream()).collect(toUnmodifiableSet());
        }

        @Nonnull
        Universe<STATE> getUniverse() {
            return Universe.this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * Furthermore, for this type, {@linkplain ObjectHistory#removeSignals(Set)
         * removes} the signals from the {@linkplain Universe#getObjectHistories()
         * object histories} of the {@linkplain #getUniverse() universe}.
         * </p>
         */
        @Override
        public void removeAll(@Nonnull final Collection<Signal<STATE>> signals) {
            Objects.requireNonNull(signals, "signals");
            for (final var signal : signals) {
                Objects.requireNonNull(signal, "signal");
                final var receiver = signal.getReceiver();
                final var history = objectHistories.get(receiver);
                if (history == null) {
                    throw new IllegalStateException("unknown receiver for " + signal);
                }
                history.removeSignals(Set.of(signal.getId()));
                scheduleAdvanceObject(receiver);
                // TODO optimize by grouping signals by receiver
            } // for
        }

        void scheduleAdvanceObject(@Nonnull final UUID object) {
            executor.execute(() -> {
                final var history = objectHistories.get(object);
                final var lastEvent = history.getLastEvent();
                if (history.getEnd().compareTo(advanceTo) < 0
                        && (lastEvent == null || lastEvent.getWhenOccurred().compareTo(advanceTo) < 0)) {
                    if (history.receiveNextSignal(this)) {
                        scheduleAdvanceObject(object);
                    }
                }
            });
        }

    }// class

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

    @Nonnull
    SchedulingMedium createMedium(@Nonnull final Executor executor, @Nonnull final Duration advanceTo) {
        return new SchedulingMedium(executor, advanceTo);
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
     * <li>May be unmodifiable.</li>
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
     * Get a snapshot of the history information of one object in this universe.
     * </p>
     * <ul>
     * <li>Returns null if, and only if, {@code object} is not the
     * {@linkplain #getObjects() ID} of an object in this universe.</li>
     * <li>If returns a (non null) object history, the
     * {@linkplain ObjectHistory#getObject() object ID} of that object history will
     * be {@linkplain UUID#equals(Object) equivalent to} the given {@code object}
     * ID.</li>
     * <li>The returned history will not subsequently change due to events.</li>
     * </ul>
     */
    @Nullable
    public ObjectHistory<STATE> getObjectHistory(@Nonnull final UUID object) {
        Objects.requireNonNull(object, "object");
        final var history = objectHistories.get(object);
        return history == null ? null : new ObjectHistory<>(history);
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
