package uk.badamson.mc.simulation;
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

/**
 * <p>
 * An identifier (unique key) for the state of a simulated object.
 * </p>
 */
@Immutable
public final class ObjectStateId implements Comparable<ObjectStateId> {

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
     *            The unique ID of the object for which this identifies a state.
     * @param when
     *            The point in time that the {@linkplain #getObject() object} has
     *            the state identified by this ID, expressed as a duration since an
     *            (implied) epoch. All objects in a simulation should use the same
     *            epoch.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             </ul>
     */
    public ObjectStateId(@Nonnull final UUID object, @Nonnull final Duration when) {
        this.object = Objects.requireNonNull(object, "object");
        this.when = Objects.requireNonNull(when, "when");
    }

    /**
     * <p>
     * The <i>natural ordering</i> relation of this ID with a given ID.
     * </p>
     * <ul>
     * <li>The <i>natural ordering</i> of {@link ObjectStateId} is consistent with
     * {@linkplain #equals(Object) equals}.</li>
     * <li>The <i>natural ordering</i> orders by {@linkplain #getWhen() time-stamp};
     * if two IDs have different time-stamps, their ordering is equivalent to the
     * ordering of their time-stamps.</li>
     * <li>The <i>natural ordering</i> orders by {@linkplain #getObject() object
     * IDs} if {@linkplain #getWhen() time-stamps} are equivalent; if two IDs have
     * {@linkplain Duration#equals(Object) equal} time-stamps, their ordering is
     * equivalent to the ordering of their object IDs.</li>
     * </ul>
     *
     * @param that
     *            The other ID to compare with this ID.
     * @return a value that has a sign or zeroness that indicates the order of this
     *         ID with respect to the given ID.
     * @throws NullPointerException
     *             If {@code that} is null.
     */
    @Override
    public int compareTo(final ObjectStateId that) {
        Objects.requireNonNull(that, "that");
        int c = when.compareTo(that.when);
        if (c == 0) {
            c = object.compareTo(that.object);
        }
        return c;
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
     */
    @Override
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        final ObjectStateId other = (ObjectStateId) that;
        return when.equals(other.when) && object.equals(other.object);
    }

    /**
     * <p>
     * The unique ID of the object for which this identifies a state.
     * </p>
     *
     * @return The object ID; not null.
     */
    @Nonnull
    public UUID getObject() {
        return object;
    }

    /**
     * <p>
     * The point in time that the {@linkplain #getObject() object} has the state
     * identified by this ID.
     * </p>
     * <p>
     * Expressed as the duration since an (implied) epoch. All objects in a
     * simulation should use the same epoch.
     * </p>
     *
     * @return the amount of time since the epoch; not null.
     */
    @Nonnull
    public Duration getWhen() {
        return when;
    }

    @Override
    public int hashCode() {
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