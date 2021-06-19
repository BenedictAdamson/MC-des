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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>
 * An identifier (unique key) of an object with a time-stamp.
 * </p>
 */
@Immutable
public final class TimestampedId implements Comparable<TimestampedId> {

    private final UUID object;
    private final Duration when;

    /**
     * <p>
     * Construct an object with given attribute values.
     * </p>
     *
     * @throws NullPointerException If any argument is null.
     */
    @JsonCreator
    public TimestampedId(@Nonnull @JsonProperty("object") final UUID object,
                         @Nonnull @JsonProperty("when") final Duration when) {
        this.object = Objects.requireNonNull(object, "object");
        this.when = Objects.requireNonNull(when, "when");
    }

    /**
     * <p>
     * The <i>natural ordering</i> relation of this ID.
     * </p>
     * <ul>
     * <li>The <i>natural ordering</i> of {@link TimestampedId} is consistent with
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
     * @param that The other ID to compare with this ID.
     * @return a value that has a sign or is zero to indicates the order of this
     * ID with respect to the given ID.
     * @throws NullPointerException If {@code that} is null.
     */
    @Override
    public int compareTo(@Nonnull final TimestampedId that) {
        Objects.requireNonNull(that, "that");
        int c = when.compareTo(that.when);
        if (c == 0) {
            c = object.compareTo(that.object);
        }
        return c;
    }

    /**
     * <p>
     * Whether this {@link TimestampedId} is equivalent to another object.
     * </p>
     * <p>
     * {@link TimestampedId} objects have value semantics: two IDs are equivalent
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
        final TimestampedId other = (TimestampedId) that;
        return when.equals(other.when) && object.equals(other.object);
    }

    /**
     * <p>
     * The unique ID of the object.
     * </p>
     */
    @Nonnull
    public UUID getObject() {
        return object;
    }

    /**
     * <p>
     * The time-stamp.
     * </p>
     * <p>
     * Expressed as the duration since an (implied) epoch. All objects in a
     * simulation should use the same epoch.
     * </p>
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