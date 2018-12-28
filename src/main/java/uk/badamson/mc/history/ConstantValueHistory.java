package uk.badamson.mc.history;
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
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.Immutable;

/**
 * <p>
 * The nominally time-wise variation of a value that does not actually vary
 * through time.
 * </p>
 *
 * @param VALUE
 *            The class of values of this value history. This must be
 *            {@link Immutable immutable}, or have reference semantics.
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
     * @param value
     *            The value {@linkplain #get(Duration) at} all points in time.
     */
    public ConstantValueHistory(@Nullable final VALUE value) {
        this.value = value;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        // FIXME
        if (!(obj instanceof ConstantValueHistory))
            return false;
        @SuppressWarnings("unchecked")
        final ConstantValueHistory<VALUE> other = (ConstantValueHistory<VALUE>) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public final VALUE get(final Duration t) {
        Objects.requireNonNull(t, "t");
        return value;
    }

    /**
     * <p>
     * The first point in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>The first transition time is null, to indicate that this history has no
     * transitions. That is, the value is constant for all time.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @return the first transition time.
     */
    @Override
    public final Duration getFirstTansitionTime() {
        return null;
    }

    @Override
    public final VALUE getFirstValue() {
        return value;
    }

    /**
     * <p>
     * The last point in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>The last transition time is null, to indicate that this history has no
     * transitions. That is, the value is constant for all time.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @return the last transition time.
     */
    @Override
    public final Duration getLastTansitionTime() {
        return null;
    }

    @Override
    public final VALUE getLastValue() {
        return value;
    }

    /**
     * <p>
     * The point in time when the value of this history changes that is at or after
     * a given point in time.
     * </p>
     * <ul>
     * <li>The transition time at or after all given times is null, to indicate that
     * this history has no transitions.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @param when
     *            The point in time of interest, expressed as a duration since an
     *            epoch.
     * @return the transition time at or after the given time.
     * @throws NullPointerException
     *             if {@code when} is null.
     */
    @Override
    public final Duration getTansitionTimeAtOrAfter(final Duration when) {
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
    @Override
    public final SortedMap<Duration, VALUE> getTransitions() {
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
     * <li>For all points in time (except the {@linkplain ValueHistory#START_OF_TIME
     * start of time}), the {@linkplain #get(Duration) value} just before the point
     * in time is equal to the value at the point in time.</li>
     * <li>The returned set is
     * {@linkplain Collections#unmodifiableSortedSet(SortedSet) unmodifiable}.</li>
     * </ul>
     *
     * @return the transition times
     */
    @Override
    public final SortedSet<Duration> getTransitionTimes() {
        return Collections.emptySortedSet();
    }

    @Override
    public final int hashCode() {
        return value == null ? 0 : value.hashCode();
    }

    /**
     * <p>
     * Whether this history is empty.
     * </p>
     * <ul>
     * <li>A {@link ConstantValueHistory} is always empty.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     */
    @Override
    public final boolean isEmpty() {
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
    @Override
    public final Stream<Entry<Duration, VALUE>> streamOfTransitions() {
        return Stream.empty();
    }

    @Override
    public String toString() {
        return "ConstantValueHistory [" + value + "]";
    }

}
