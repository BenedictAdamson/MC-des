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

    /**
     * <p>
     * Construct an object with given attribute values.
     * </p>
     * <ul>
     * <li>The {@linkplain #getObject() object ID} of this ID is the given object
     * ID.</li>
     * <li>The {@linkplain #getWhen() time-stamp} of this ID is the given
     * time-stamp.</li>
     * </ul>
     * 
     * @param object
     *            The unique ID of the object for which the {@link ObjectState} this
     *            identifies is a state.
     * @param when
     *            The point in time that the {@linkplain #getObject() object} has
     *            the state identified by this ID.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             </ul>
     */
    public ObjectStateId(UUID object, Duration when) {
        this.object = Objects.requireNonNull(object, "object");
        this.when = Objects.requireNonNull(when, "when");
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
        return when.equals(other.when) && object.equals(other.object);
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
        return result;
    }

    @Override
    public String toString() {
        return object + "@" + when;
    }

}