package uk.badamson.mc.history;
/*
 * © Copyright Benedict Adamson 2018,2021-22.
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Stream;

/**
 * <p>
 * The nominally time-wise variation of a value that does not actually vary
 * through time.
 * </p>
 *
 * @param <VALUE> The class of values of this value history. This must be {@link Immutable
 *                immutable}, or have reference semantics.
 */
@Immutable
public final class ConstantValueHistory<VALUE> extends AbstractValueHistory<VALUE> {

    @Nullable
    private final VALUE value;

    /**
     * <p>
     * Construct a history with a given constant value.
     * </p>
     * <ul>
     * <li>The {@linkplain #getFirstValue() first value} is the given value.</li>
     * <li>The {@linkplain #getLastValue() last value} is the given value.</li>
     * </ul>
     *
     * @param value The value {@linkplain #get(Duration) at} all points in time.
     */
    public ConstantValueHistory(@Nullable final VALUE value) {
        this.value = value;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ConstantValueHistory)) {
            return false;
        }
        final ConstantValueHistory<?> other = (ConstantValueHistory<?>) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public VALUE get(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        return value;
    }

    @Override
    @Nonnull
    public TimestampedValue<VALUE> getTimestampedValue(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        return new TimestampedValue<>(START_OF_TIME, END_OF_TIME, value);
    }

    /**
     * <p>
     * The first point in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>The first transition time is always null, to indicate that this history has no
     * transitions. That is, the value is constant for all time.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @return the first transition time.
     */
    @Nullable
    @Override
    public Duration getFirstTransitionTime() {
        return null;
    }

    @Nullable
    @Override
    public VALUE getFirstValue() {
        return value;
    }

    /**
     * <p>
     * The last point in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>The last transition time is always  null, to indicate that this history has no
     * transitions. That is, the value is constant for all time.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @return the last transition time.
     */
    @Nullable
    @Override
    public Duration getLastTransitionTime() {
        return null;
    }

    @Nullable
    @Override
    public VALUE getLastValue() {
        return value;
    }

    /**
     * <p>
     * The point in time when the value of this history changes that is at or after
     * a given point in time.
     * </p>
     * <ul>
     * <li>This value is always null, to indicate that
     * this history has no transitions.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @param when The point in time of interest, expressed as a duration since an
     *             epoch.
     * @return the transition time at or after the given time.
     * @throws NullPointerException if {@code when} is null.
     */
    @Nullable
    @Override
    public Duration getTransitionTimeAtOrAfter(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        return null;
    }

    /**
     * <p>
     * The transitions in the value of this history.
     * </p>
     * <ul>
     * <li>Always has a (non null) transitions map.</li>
     * <li>The transitions map {@linkplain SortedMap#isEmpty() is empty}.</li>
     * <li>The transitions map is
     * {@linkplain Collections#unmodifiableSortedMap(SortedMap) unmodifiable}.</li>
     * </ul>
     *
     * @return a map of the transitions.
     */
    @Nonnull
    @Override
    public SortedMap<Duration, VALUE> getTransitions() {
        return Collections.emptySortedMap();
    }

    /**
     * <p>
     * The points in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of transition times.</li>
     * <li>The transition times are represented as the duration since an (implied)
     * epoch.</li>
     * <li>The set of transition times {@linkplain SortedSet#isEmpty() is
     * empty}.</li>
     * <li>The returned set is
     * {@linkplain Collections#unmodifiableSortedSet(SortedSet) unmodifiable}.</li>
     * </ul>
     *
     * @return the transition times
     */
    @Nonnull
    @Override
    public SortedSet<Duration> getTransitionTimes() {
        return Collections.emptySortedSet();
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.hashCode();
    }

    /**
     * <p>
     * Whether this history is empty.
     * </p>
     * <ul>
     * <li>Always returns true.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * <p>
     * Create a stream of the {@linkplain #getTransitions() transitions} in the
     * value of this history.
     * </p>
     * <ul>
     * <li>Always creates a (non null) steam.</li>
     * <li>The stream of transitions {@linkplain Stream#count() is empty}.</li>
     * <li>Using the stream of transitions is more efficient than getting the
     * {@linkplain #getTransitions() transitions map} and then creating a stream
     * from its entries.</li>
     * </ul>
     *
     * @return a stream of the transitions.
     */
    @Nonnull
    @Override
    public Stream<Map.Entry<Duration, VALUE>> streamOfTransitions() {
        return Stream.empty();
    }

    @Override
    public String toString() {
        return "ConstantValueHistory [" + value + "]";
    }

}
