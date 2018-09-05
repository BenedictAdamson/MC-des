package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * The state of a simulated object at one point in time, just after a state
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
     * Compute the state of the {@link {@link #getObjectId()} object}, and any
     * objects it creates, just after the {@link #getDurationUntilNextEvent next
     * state change of the object}.
     * </p>
     * <p>
     * This computation may be expensive; recomputing future states should be
     * avoided.
     * </p>
     * <ul>
     * <li>Always return a (non null) map of object states.</li>
     * <li>The map of object states does not have a null key.</li>
     * <li>The map of object states has an entry (key) for the {@link #getObjectId()
     * object ID} of the object for which this is a state.</li>
     * <li>The map has no null values for objects other than the
     * {@link #getObjectId() object ID} of the object for which this is a
     * state.</li>
     * <li>All the values (states) in the next states map are
     * {@link Duration#equals(Object) equal} {@link #getWhen() points in time}.</li>
     * <li>All the values (states) in the next states map are for a
     * {@link #getWhen() point in time} after the point in time of this state.</li>
     * </ul>
     * 
     * @return A mapping of object IDs to the states of those objects at the future
     *         point in time. This will usually be a singleton map, containing an
     *         entry for the @link {@link #getObjectId()} object} for which this is
     *         a state. A null value for the object for which this is a state
     *         indicates that the object no longer exists at the future point in
     *         time. A map with more than one entry indicates that the object has
     *         caused the creation of additional objects.
     */
    public Map<UUID, ObjectState> createNextStates();

    /**
     * <p>
     * The unique ID of the object for which this is a state.
     * </p>
     * 
     * @return The object ID; not null.
     */
    public UUID getObjectId();

    /**
     * <p>
     * The point in time that the {@linkplain #getObjectId() object} had this state.
     * </p>
     * <p>
     * Expressed as the duration since an epoch. All {@link ObjectState} objects in
     * a simulation should use the same peoch.
     * </p>
     * 
     * @return the amount of time since the epoch; not null.
     */
    public Duration getWhen();
}
