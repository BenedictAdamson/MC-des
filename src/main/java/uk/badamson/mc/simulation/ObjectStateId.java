package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.UUID;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * An identifier (unique key) for an {@link ObjectState}.
 * </p>
 */
@Immutable
public interface ObjectStateId {

    /**
     * <p>
     * Whether this {@link ObjectStateId} is equivalent to another object.
     * </p>
     * <p>
     * {@link ObjectStateId} objects have value semantics: two IDs are equivalent
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

}