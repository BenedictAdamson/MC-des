package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A state of a simulated object at one point in time, just after a state
 * change, also encapsulating how to compute the state of the object at future
 * points in time, and creation and destruction of objects.
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
     * An identifier (unique key) for an {@link ObjectState}.
     * </p>
     */
    @Immutable
    public interface Id {

        /**
         * <p>
         * Whether this {@link ObjectState.Id} is equivalent to another object.
         * </p>
         * <p>
         * {@link ObjectState.Id} objects have value semantics: two IDs are equivalent
         * if, and only if, they have equivalent {@linkplain #getObject() object IDs}
         * and {@linkplain #getWhen() time-stamps}.
         * </p>
         * 
         * @param that
         *            The object to compare with this object.
         * @return whether this object is equivalent to {@code that} object.
         */
        @Override
        public boolean equals(Object that);

        /**
         * <p>
         * The unique ID of the object for which the {@link ObjectState} this identifies
         * is a state.
         * </p>
         * 
         * @return The object ID; not null.
         */
        public UUID getObject();

        /**
         * <p>
         * The point in time that the {@linkplain #getObject() object} has the state
         * identified by this ID.
         * </p>
         * <p>
         * Expressed as the duration since an epoch. All {@link ObjectState} objects in
         * a simulation should use the same epoch.
         * </p>
         * 
         * @return the amount of time since the epoch; not null.
         */
        public Duration getWhen();

    }// interface

    /**
     * <p>
     * Compute the state of the {@link {@link #getObject()} object}, and any
     * objects it creates, just after the {@link #getDurationUntilNextEvent next
     * state change of the object}.
     * </p>
     * <p>
     * This computation may be expensive; recomputing future states should be
     * avoided.
     * </p>
     * <ul>
     * <li>Always has a (non null) map of object states.</li>
     * <li>The map of object states may be unmodifiable.</li>
     * <li>The map of object states does not have a null key.</li>
     * <li>The map of object states has an entry (key) for the
     * {@link ObjectState.Id#getObject() object ID} of the {@linkplain #getId()
     * ID} of this state.</li>
     * <li>The map has no null values for objects other than the
     * {@link ObjectId.Id#getObject() object ID} of the of the
     * {@linkplain #getId() ID} of this state.</li>
     * <li>Non null next state values have the {@link ObjectState.Id#getObject()
     * object ID} of their {@linkplain #getId() ID} equal to their key.</li>
     * <li>All the values (states) in the next states map are
     * {@link Duration#equals(Object) equal} {@link #getWhen() points in time}.</li>
     * <li>All the values (states) in the next states map are for a
     * {@link #getWhen() point in time} after the point in time of this state.</li>
     * </ul>
     * 
     * @return A mapping of object IDs to the states of those objects at the future
     *         point in time. This will usually be a singleton map, containing an
     *         entry for the @link {@link #getObject()} object} for which this is
     *         a state. A null value for the object for which this is a state
     *         indicates that the object no longer exists at the future point in
     *         time. A map with more than one entry indicates that the object has
     *         caused the creation of additional objects.
     */
    public Map<UUID, ObjectState> createNextStates();

    /**
     * <p>
     * Whether this {@link ObjectState} is equivalent to another object.
     * </p>
     * <p>
     * The semantics of an {@link ObjectState} is that the its {@linkplain #getId()
     * ID} attribute is a unique ID. Therefore two {@link ObjectState} objects are
     * equivalent if, and only if, they have equals IDs.
     * </p>
     * 
     * @param that
     *            The object to compare with this object.
     * @return whether this object is equivalent to {@code that} object.
     */
    @Override
    public boolean equals(Object that);

    /**
     * <p>
     * The identifier (unique key) for this object state
     * </p>
     * 
     * @return the ID; not null.
     */
    public Id getId();
    
    
    /**
     * <p>
     * The (earlier) object states directly used to compute this object state.
     * </p>
     * <p>
     * Implicitly, if the object state computations were done differently for the dependent objects,
     * this {@link ObjectState} would be incorrect.
     * </p>
     * <ul>
     * <li>Always has a (non null) set of dependencies.</li>
     * <li>The set of dependencies does not have a null entry.</li>
     * <li>The set of dependencies does not have entries with duplicate {@linkplain ObjectState.Id#getObject() object IDs}.</li>
     * <li>The set of dependencies has {@linkplain ObjectState.Id#getWhen() time-stamps} after the time-stamp of the {@linkplain #getId() ID} of this state.</li>
     * </ul>
     * 
     * @return The set of the IDs of the {@link ObjectState} states that this state directly depends on.
     */
    public Set<Id> getDependencies();

}
