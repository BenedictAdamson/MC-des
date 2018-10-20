package uk.badamson.mc.simulation;
/* 
 * © Copyright Benedict Adamson 2018.
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
import java.util.UUID;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A state of a simulated object at one point in time, just after a state
 * change, also encapsulating how to compute the state of the object at future
 * points in time, and encapsulating creation and destruction of objects.
 * </p>
 * <p>
 * The state of the object is assumed to pass through a sequence of discrete
 * states: this class is for discrete event simulations.
 * </p>
 */
@Immutable
public interface ObjectState {

    /**
     * <p>
     * Compute the next state transition of the {@link {@link #getObject()} object}
     * of this object state.
     * </p>
     * <p>
     * This computation may be expensive; recomputing state transition should be
     * avoided.
     * </p>
     * <ul>
     * <li>The points in time for which the method
     * {@linkplain Universe.Transaction#getObjectState(UUID, Duration) fetches}
     * (reads) state information must be before the given (current) point in
     * time.</li>
     * <li>The method puts the given transaction
     * {@linkplain Universe.Transaction#beginWrite(Duration) into write mode}, with
     * a write time-stamp in the future (after the given point in time).</li>
     * <li>The method must {@linkplain Universe.Transaction#put(UUID, ObjectState)
     * put}, using the given transaction, one new state for the given object
     * ID.</li>
     * <li>The method must {@linkplain Universe.Transaction#put(UUID, ObjectState)
     * put} for the given object, using the given transaction, a state that is not
     * {@linkplain ObjectState#equals(Object) equal} to this state.</li>
     * <li>The method may {@linkplain Universe.Transaction#put(UUID, ObjectState)
     * put} no more than one object state for any object ID.</li>
     * </ul>
     * 
     * @param transaction
     *            The transaction that the method must use to
     *            {@linkplain Universe.Transaction#getObjectState(UUID, Duration)
     *            fetch} object states it needs for the computation and to
     *            {@linkplain Universe.Transaction#put(UUID, ObjectState) put} new
     *            object states, including the next state transition of the object.
     * @param object
     *            The ID of the object for which this is a state.
     * @param when
     *            The point in time that the object entered this state.
     * 
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code transaction} is null.</li>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If the {@linkplain Universe.Transaction#getObjectStatesRead()
     *             object states read} for the {@code transaction} consists of other
     *             than an entry for this state with the given object ID and
     *             time-stamp.
     */
    public abstract void putNextStateTransition(Universe.Transaction transaction, UUID object, Duration when);
}
