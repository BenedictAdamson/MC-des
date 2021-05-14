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
 * A snapshot of a time-wise varying value, with a time-range indicating the
 * points in time when the variable had a particular value.
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
    @Nonnull
    private final Duration end;
    @Nullable
    private final VALUE value;

    /**
     * Constructs a snapshot with given attribute values.
     *
     * @throws IllegalArgumentException
     *             If {@code end} {@linkplain Duration#compareTo(Duration) is
     *             before} {@code start}
     */
    public TimestampedValue(@Nonnull final Duration start, @Nonnull final Duration end, @Nullable final VALUE state) {
        this.start = Objects.requireNonNull(start, "end");
        this.end = Objects.requireNonNull(end, "end");
        this.value = state;
        if (end.compareTo(start) < 0) {
            throw new IllegalArgumentException("end before start");
        }
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
        return start.equals(other.start) && end.equals(other.end) && Objects.equals(value, other.value);// FIXME
    }

    /**
     * <p>
     * The last point in time that the time-varying value had the
     * {@linkplain #getValue() value}
     * </p>
     * <p>
     * This is an <i>inclusive</i> and time. Expressed as the duration since an
     * (implied) epoch.
     * </p>
     * <ul>
     * <li>The end time is {@linkplain Duration#compareTo(Duration) at or after} the
     * {@linkplain #getStart() start} time.</li>
     * </ul>
     */
    @Nonnull
    public Duration getEnd() {
        return end;
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

    /**
     * <p>
     * A snapshot of the time-varying value, in the time-range given by the
     * {@linkplain #getStart() start} and {@linkplain #getEnd() end} times
     * </p>
     */
    @Nullable
    public VALUE getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, start, end);
    }

    @Nonnull
    @Override
    public String toString() {
        return "@(" + start + "," + end + ")=" + value;
    }

}