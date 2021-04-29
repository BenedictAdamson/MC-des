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
    private final Event<STATE> firstEvent;

    /**
     * <p>
     * Construct an object history with given start information.
     * </p>
     * <ul>
     * <li>The {@linkplain #getFirstEvent() first event} of this history is the
     * given {@code firstEvent};
     * </ul>
     *
     * @param firstEvent
     *            The first (known) state transition of the {@linkplain #getObject()
     *            object}.
     * @throws NullPointerException
     *             If {@code firstEvent} is null
     */
    public ObjectHistory(@Nonnull final Event<STATE> firstEvent) {
        this.firstEvent = Objects.requireNonNull(firstEvent, "firstEvent");
    }

    /**
     * <p>
     * The first (known) state transition of the {@linkplain #getObject() object}.
     * </p>
     * <ul>
     * <li>Always have a (non null) first event.</li>
     * <li>Constant: the history always returns the same first event object.</li>
     * </ul>
     */
    @Nonnull
    public Event<STATE> getFirstEvent() {
        return firstEvent;
    }

    /**
     * <p>
     * The unique ID of the object for which this is the history.
     * </p>
     * <ul>
     * <li>Constant: the history always returns the same object ID.</li>
     * <li>The object ID is the same as the {@linkplain Event#getObject() object} of
     * the {@linkplain #getFirstEvent() first event}.</li>
     * </ul>
     */
    @Nonnull
    public UUID getObject() {
        return firstEvent.getObject();
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
     * <li>The start is the same as the {@linkplain Event#getWhen() time} of the
     * {@linkplain #getFirstEvent() first event}.</li>
     * </ul>
     */
    @Nonnull
    public final Duration getStart() {
        return firstEvent.getWhen();
    }

}
