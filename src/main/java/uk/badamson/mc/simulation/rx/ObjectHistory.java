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
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

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

    @Nonnull
    private final UUID object;
    @Nonnull
    private final Duration start;
    private final Object lock = new Object();

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
     *             If {@code firstEvent} is null
     */
    public ObjectHistory(@Nonnull final Event<STATE> event) {
        this.lastEvent = Objects.requireNonNull(event, "event");
        this.object = lastEvent.getObject();
        this.start = lastEvent.getWhen();
    }

    /**
     * <p>
     * Append an event to this history.
     * </p>
     *
     * <ul>
     * <li>The {@linkplain #getLastEvent() last event} becomes the same as the given
     * {@code event}.</li>
     * </ul>
     *
     * @param event
     *            The event to append.
     * @throws NullPointerException
     *             If {@code event} is null
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the {@linkplain Event#getObject() object} of the
     *             {@code event} is not the same as the {@linkplain #getObject()
     *             object} of this history.</li>
     *             <li>If the {@linkplain Event#getWhen() time} of the {@code event}
     *             is not {@linkplain Duration#compareTo(Duration) after} the time
     *             of the {@linkplain #getLastEvent() last event} of this
     *             history.</li>
     *             </ul>
     */
    public void append(@Nonnull final Event<STATE> event) {
        Objects.requireNonNull(event, "event");
        if (object != event.getObject()) {
            throw new IllegalArgumentException("event.getObject");
        }
        final var eventWhen = event.getWhen();
        synchronized (lock) {
            if (0 <= lastEvent.getWhen().compareTo(eventWhen)) {
                throw new IllegalArgumentException("event.getWhen");
            }
            lastEvent = event;
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

}
