package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * An identifier (unique key) for an {@link ObjectState}.
 * </p>
 */
@Immutable
public final class ObjectStateId {

    private final UUID object;
    private final Duration when;
    private final UUID version;

    /**
     * <p>
     * Construct an object with given attribute values.
     * </p>
     * <ul>
     * <li>The {@linkplain #getObject() object ID} of this ID is the given object
     * ID.</li>
     * <li>The {@linkplain #getWhen() time-stamp} of this ID is the given
     * time-stamp.</li>
     * <li>The {@linkplain #getVersion() version ID} of this ID is the given version
     * ID.</li>
     * </ul>
     * 
     * @param object
     *            The unique ID of the object for which the {@link ObjectState} this
     *            identifies is a state.
     * @param when
     *            The point in time that the {@linkplain #getObject() object} has
     *            the state identified by this ID.
     * @param version
     *            The version identifier of the object state that this identifies.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             <li>If {@code version} is null.</li>
     *             </ul>
     */
    public ObjectStateId(UUID object, Duration when, UUID version) {
        this.object = Objects.requireNonNull(object, "object");
        this.when = Objects.requireNonNull(when, "when");
        this.version = Objects.requireNonNull(version, "version");
    }

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
    public final boolean equals(Object that) {
        if (this == that)
            return true;
        if (that == null)
            return false;
        if (getClass() != that.getClass())
            return false;
        ObjectStateId other = (ObjectStateId) that;
        return when.equals(other.when) && object.equals(other.object) && version.equals(other.version);
    }

    /**
     * <p>
     * The unique ID of the object for which the {@link ObjectState} this identifies
     * is a state.
     * </p>
     * 
     * @return The object ID; not null.
     */
    public final UUID getObject() {
        return object;
    }

    /**
     * <p>
     * The version identifier of the object state that this identifies.
     * </p>
     * <p>
     * Computation of object states can be provisional, with later versions revising
     * the computed state as more information becomes available. This attribute
     * enables different versions of the computation for an {@linkplain #getObject()
     * object} to be distinguished, even if they are for equal
     * {@linkplain #getWhen() points in time}.
     * </p>
     * <p>
     * Although {@link UUID} are {@linkplain UUID#compareTo(UUID) comparable}, the
     * ordering of version ID values is not significant: the {@link UUID} order need
     * not be the same as the computation order.
     * </p>
     * 
     * @return the version; not null.
     */
    public final UUID getVersion() {
        return version;
    }

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
    public final Duration getWhen() {
        return when;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + object.hashCode();
        result = prime * result + when.hashCode();
        result = prime * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return object + "@" + when + "/" + version;
    }

}