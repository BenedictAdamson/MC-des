package uk.badamson.mc.history;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * <p>
 * A snapshot of a time-wise varying value, with a time-stamp indicating the
 * point in time of the snapshot.
 * </p>
 *
 * @param <VALUE>
 *            The class of the time-wise varying value. This must be
 *            {@link Immutable immutable}, or have reference semantics. It ought
 *            to have value semantics, but that is not required.
 */
@Immutable
public final class TimestampedValue<VALUE> {

    @Nonnull
    private final Duration start;
    @Nullable
    private final VALUE value;

    /**
     * Constructs a snapshot with given attribute values.
     */
    public TimestampedValue(@Nonnull final Duration start, @Nullable final VALUE state) {
        this.start = Objects.requireNonNull(start, "when");
        this.value = state;
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to a given other object.
     * </p>
     * <p>
     * The {@link TimestampedValue} class has <i>value semantics</i>.
     * </p>
     */
    @Override
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof TimestampedValue)) {
            return false;
        }
        final TimestampedValue<?> other = (TimestampedValue<?>) that;
        return start.equals(other.start) && Objects.equals(value, other.value);
    }

    /**
     * <p>
     * A snapshot of the time-varying value, at the {@linkplain #getStart() time} of
     * the snapshot.
     * </p>
     */
    @Nullable
    public VALUE getValue() {
        return value;
    }

    /**
     * <p>
     * The point in time that the time-varying value started having the
     * {@linkplain #getValue() value}
     * </p>
     * <p>
     * Expressed as the duration since an (implied) epoch.
     * </p>
     */
    @Nonnull
    public Duration getStart() {
        return start;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, start);
    }

    @Nonnull
    @Override
    public String toString() {
        return "@" + start + "=" + value + "]";
    }

}