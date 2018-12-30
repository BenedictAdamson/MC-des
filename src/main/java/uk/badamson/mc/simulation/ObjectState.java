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

import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * <p>
 * A state of a simulated object at one point in time, just after a state
 * change.
 * </p>
 * <p>
 * Objects of this class also encapsulating how to compute the state of the
 * object at future points in time, and encapsulating creation and destruction
 * of objects. The state of the object is assumed to pass through a sequence of
 * discrete states: this interface is for discrete event simulations. Each state
 * change is an <dfn>event</dfn>.
 * </p>
 */
@Immutable
public interface ObjectState {

    /**
     * <p>
     * Compute the effect of the next event for the object for which this is an
     * object state.
     * </p>
     * <p>
     * The method computes the effect of the event using a given
     * {@linkplain Universe.Transaction transaction}. The method
     * {@linkplain Universe.Transaction#put(UUID, ObjectState) writes} the new state
     * for the object using the transaction. If the event creates new objects, the
     * method also writes the initial states of those new objects. The point in time
     * for which the method {@linkplain Universe.Transaction#beginWrite(Duration)
     * writes} those states is taken to be the point in time that the event occurs.
     * </p>
     * <p>
     * Events computed by this method respect causality: the effect of the event
     * always depends on the state of (some of) the simulated system at points in
     * time before the time of the event. In the simplest case, the effect of the
     * event depends on this object state. The event may also depend on the states
     * of other objects. For those states to effect the event computation done by
     * this method, the method must read (access) those states. The method
     * <em>must</em> use the given transaction to
     * {@linkplain Universe.Transaction#getObjectState(UUID, Duration) read} any
     * object states it needs, other than its own state (which it may access
     * directly, of course). This ensures that the given transaction records the
     * causality constraints of the event. Those reads can be interpreted as a
     * message transmitting information from the other objects to the object for
     * which this is computing a state. The reads must be for points in time before
     * the given point in time. The amount by which the reads are before can be
     * interpreted as the message propagation time or the message transmission
     * delay.
     * </p>
     * <p>
     * In most cases the new state of the object computed by this method will be
     * similar to this object state. In most cases this object state will be
     * composed from other objects that are also {@linkplain Immutable immutable}.
     * The method should take reasonable steps to conserve memory by reusing
     * component immutable objects from its own state in the new state.
     * </p>
     * <p>
     * This computation may be expensive; recomputing events should be avoided.
     * </p>
     * <ul>
     * <li>The points in time for which the method
     * {@linkplain Universe.Transaction#getObjectState(UUID, Duration) reads} state
     * information must be before the given (current) point in time.</li>
     * <li>The method must change the given transaction
     * {@linkplain Universe.Transaction#beginWrite(Duration) into write mode}, with
     * a write time-stamp in the future (after the given point in time).</li>
     * <li>The method must must not {@linkplain Universe.Transaction#beginCommit()
     * begin committing} or {@linkplain Universe.Transaction#beginAbort() begin
     * aborting} the given transaction.</li>
     * <li>The method must {@linkplain Universe.Transaction#put(UUID, ObjectState)
     * write (put)}, using the given transaction, one new state for the given object
     * ID.</li>
     * <li>The method must {@linkplain Universe.Transaction#put(UUID, ObjectState)
     * write} for the given object, using the given transaction, a state that is not
     * equal to this state.</li>
     * <li>The method may {@linkplain Universe.Transaction#put(UUID, ObjectState)
     * write} no more than one object state for any object ID.</li>
     * </ul>
     *
     * @param transaction
     *            The transaction that the method must use to
     *            {@linkplain Universe.Transaction#getObjectState(UUID, Duration)
     *            read} object states it needs for the computation and to
     *            {@linkplain Universe.Transaction#put(UUID, ObjectState) write} new
     *            object states, including the next state transition of the object.
     * @param object
     *            The ID of the object for which this is a state.
     * @param when
     *            The point in time that the object entered this state, expressed as
     *            a duration since an (implied) epoch.
     *
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code transaction} is null.</li>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code transaction} is not
     *             {@linkplain Universe.Transaction#getOpenness() in}
     *             {@linkplain Universe.TransactionOpenness#READING read mode}.</li>
     *             <li>If the {@linkplain Universe.Transaction#getObjectStatesRead()
     *             object states read} for the {@code transaction} consists of other
     *             than an entry for this state with the given object ID and
     *             time-stamp.</li>
     *             </ul>
     */
    public abstract void doNextEvent(@NonNull Universe.Transaction transaction, @NonNull UUID object,
            @NonNull Duration when);
}
