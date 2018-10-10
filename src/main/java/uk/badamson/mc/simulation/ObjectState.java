package uk.badamson.mc.simulation;

import java.util.Map;

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
public abstract class ObjectState {

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
     * <li>Always has a (non null) state transition.</li>
     * <li>The {@linkplain StateTransition#getStates() states} of the state
     * transition has an entry (key) for the the
     * {@linkplain ObjectStateId#getObject() object} of the given state ID.</li>
     * <li>The {@linkplain StateTransition#getStates() states} of the state
     * transition has no null values for objects other than the
     * {@linkplain ObjectStateId#getObject() object} of the given state ID.</li>
     * <li>The {@linkplain StateTransition#getWhen() time} of the state transition
     * is after the the {@linkplain ObjectStateId#getWhen() time-stamp} of the given
     * state ID.</li>
     * <li>The given state ID is one of the {@linkplain Map#values() values} of the
     * {@linkplain StateTransition#getDependencies() dependencies} of the state
     * transition.</li>
     * <li>The {@linkplain Map#get(Object) value} of the
     * {@linkplain StateTransition#getStates() state} that has the
     * {@linkplain ObjectStateId#getObject() object ID} of the given ID is not
     * {@linkplain #equals(Object) equal to} this state.</li>
     * </ul>
     * 
     * @param idOfThisState
     *            The ID of this state.
     * 
     * @return The next state transition.
     * @throws NullPointerException
     *             If {@code idOfThisState} is null.
     */
    public abstract StateTransition createNextStateTransition(ObjectStateId idOfThisState);
}
