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
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

/**
 * <p>
 * The time-wise variation of a value that changes at discrete points in time.
 * </p>
 * <p>
 * The class encapsulates a {@linkplain #get(Duration) mapping from points in
 * time to the value at that point in time}, with that mapping defined for all
 * points in time. Points in time are represented as the {@linkplain Duration
 * duration} since an (implied) epoch.
 * </p>
 *
 * @param <VALUE> The class of values of this value history. This must be
 *                {@link Immutable immutable}, or have reference semantics.
 */
@NotThreadSafe
public interface ValueHistory<VALUE> {
    /**
     * <p>
     * The smallest (most negative) {@link Duration} value.
     * </p>
     */
    Duration START_OF_TIME = Duration.ofSeconds(Long.MIN_VALUE);

    /**
     * <p>
     * The largest (most positive) {@link Duration} value.
     * </p>
     */
    Duration END_OF_TIME = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);

    /**
     * <p>
     * Whether this value history <dfn>equals</dfn> another object.
     * </p>
     * <p>
     * The ValueHistory class has <i>value semantics</i>: for this to equal
     * another object, the other object must also be a ValueHistory, and the
     * two value histories must have equal (or
     * equally null) {@linkplain #get(Duration) values} at all points in time. The
     * latter is equivalent to having their {@linkplain #getFirstValue() first
     * values} equal (or equally null) and having
     * {@linkplain SortedMap#equals(Object) equal} {@linkplain #getTransitions()
     * transitions}.
     * </p>
     *
     * @param that The other object
     * @return Whether equal.
     */
    @Override
    boolean equals(Object that);

    /**
     * <p>
     * Get the value at a given point in time.
     * </p>
     *
     * @param when The point in time of interest, expressed as a duration since an
     *             (implied) epoch.
     * @return The value at the given point in time.
     * @throws NullPointerException If {@code when} is null.
     */
    @Nullable
    VALUE get(@Nonnull Duration when);

    /**
     * <p>
     * The first point in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>A null first transition time indicates that this history has no
     * transitions. That is, the value is constant for all time.</li>
     * <li>The point in time is represented as the duration since an (implied)
     * epoch.</li>
     * <li>The {@linkplain SortedSet#first() first} value of the
     * {@linkplain #getTransitionTimes() set of transition times} (if it is not
     * empty) is the same as the first transition time.</li>
     * <li>This method is typically more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @return the first transition time.
     */
    @Nullable
    Duration getFirstTransitionTime();

    /**
     * <p>
     * The {@linkplain #get(Duration) value} of this history at the
     * {@linkplain #START_OF_TIME start of time}.
     * </p>
     * <ul>
     * <li>The first value {@linkplain Object#equals(Object) equals} the
     * {@linkplain #get(Duration) value at} the {@linkplain #START_OF_TIME start of
     * time}.</li>
     * <li>This method is typically more efficient than using the
     * {@link #get(Duration)} method.</li>
     * </ul>
     *
     * @return the last value.
     */
    @Nullable
    VALUE getFirstValue();

    /**
     * <p>
     * The last point in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>A null last transition time indicates that this history has no
     * transitions. That is, the value is constant for all time.</li>
     * <li>The point in time is represented as the duration since an (implied)
     * epoch.</li>
     * <li>The {@linkplain SortedSet#last() last} value of the
     * {@linkplain #getTransitionTimes() set of transition times} (if it is not
     * empty) is the same as the last transition time.</li>
     * <li>This method is typically more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @return the last transition time.
     */
    @Nullable
    Duration getLastTransitionTime();

    /**
     * <p>
     * The {@linkplain #get(Duration) value} of this history at the
     * {@linkplain #END_OF_TIME end of time}.
     * </p>
     * <ul>
     * <li>The last value is equal to the {@linkplain #get(Duration) value at} the
     * {@linkplain #END_OF_TIME end of time}.</li>
     * <li>If this history has no {@linkplain #getTransitionTimes() transitions},
     * the last value is {@linkplain Objects#equals(Object, Object) equal to (or
     * equally null as)} the {@linkplain #getFirstValue() first value}.</li>
     * <li>If this history has {@linkplain #getTransitionTimes() transitions}, the
     * last value is {@linkplain Objects#equals(Object, Object) equal to (or equally
     * null as)} the value at the {@linkplain #getLastTransitionTime() last
     * transition}.</li>
     * <li>This method is typically more efficient than using the
     * {@link #getTransitionTimes()} and {@link #get(Duration)} methods.</li>
     * </ul>
     *
     * @return the last value.
     */
    @Nullable
    VALUE getLastValue();

    /**
     * <p>
     * The point in time when the value of this history changes that is at or after
     * a given point in time.
     * </p>
     * <ul>
     * <li>A null transition time at or after the given time indicates that this
     * history has no transitions at or after that time.</li>
     * <li>The point in time is represented as the duration since an (implied)
     * epoch.</li>
     * <li>A (non null) transition time at or after the given time is at or after
     * the given time.</li>
     * <li>A (non null) transition time at or after the given time
     * {@linkplain SortedSet#contains(Object) is one of} the
     * {@linkplain #getTransitionTimes() transition times}.</li>
     * <li>This method is typically more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @param when The point in time of interest, expressed as a duration since an
     *             (implied) epoch.
     * @return the transition time at or after the given time.
     * @throws NullPointerException if {@code when} is null.
     */
    @Nullable
    Duration getTransitionTimeAtOrAfter(@Nonnull Duration when);

    /**
     * <p>
     * Get a time-stamped value that includes a given point in time.
     * </p>
     * <ul>
     * <li>The time-stamped value {@linkplain TimestampedValue#getStart() start
     * time} is {@linkplain Duration#compareTo(Duration) at or before}
     * {@code when}.</li>
     * <li>The time-stamped value {@linkplain TimestampedValue#getEnd() end time} is
     * {@linkplain Duration#compareTo(Duration) at or after} {@code when}.</li>
     * <li>The time-stamped value {@linkplain TimestampedValue#getValue() value} is
     * equivalent to the {@linkplain #get(Duration) value} at {@code when}.</li>
     * <li>The time-stamped value {@linkplain TimestampedValue#getStart() start
     * time} is <em>either</em> the last {@linkplain #getTransitionTimes()
     * transition time} {@linkplain Duration#compareTo(Duration) at or before}
     * {@code when}, <em>or</em> the {@linkplain #START_OF_TIME start of time}, if
     * there are no transition times at or before {@code when}.</li>
     * <li>The time-stamped value {@linkplain TimestampedValue#getEnd() end time} is
     * <em>either</em> the first {@linkplain #getTransitionTimes() transition time}
     * {@linkplain Duration#compareTo(Duration) at or after} {@code when},
     * <em>or</em> the {@linkplain #END_OF_TIME end of time}, if there are no
     * transition times at or after {@code when}.</li>
     * </ul>
     *
     * @param when The point in time of interest, expressed as a duration since an
     *             (implied) epoch.
     * @return The time-stamped value
     * @throws NullPointerException If {@code when} is null.
     */
    @Nonnull
    TimestampedValue<VALUE> getTimestampedValue(@Nonnull Duration when);

    /**
     * <p>
     * The transitions in the value of this history.
     * </p>
     * <ul>
     * <li>Always has a (non null) transitions map.</li>
     * <li>The {@linkplain SortedMap#keySet() keys} of the transitions map are
     * {@linkplain SortedSet#equals(Object) equal} to the
     * {@linkplain #getTransitionTimes() transition times}.</li>
     * <li>The {@linkplain SortedMap#get(Object) values} of the transition map are
     * {@linkplain Objects#equals(Object, Object) equal to (or equally null as)} the
     * {@linkplain #get(Duration) value} of this history at the time of their
     * corresponding key.</li>
     * <li>There is not a transition at the
     * {@linkplain #START_OF_TIME start of time}.</li>
     * <li>The transitions map may be
     * {@linkplain Collections#unmodifiableSortedMap(SortedMap) unmodifiable}.</li>
     * <li>If the transitions map is
     * {@linkplain Collections#unmodifiableSortedMap(SortedMap) modifiable},
     * modifying it will not change this value history (it might be a newly
     * constructed object).</li>
     * </ul>
     *
     * @return a map of the transitions.
     */
    @Nonnull
    SortedMap<Duration, VALUE> getTransitions();

    /**
     * <p>
     * The points in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>The transition times are represented as the duration since an (implied)
     * epoch.</li>
     * <li>There is not a transition at the {@linkplain ValueHistory#START_OF_TIME
     * start of time}.</li>
     * <li>For all points in time in the set of transition times, the
     * {@linkplain #get(Duration) value} just before the transition is not equal to
     * the value at the transition.</li>
     * <li>For all points in time not in the set of transition times (except the
     * {@linkplain ValueHistory#START_OF_TIME start of time}), the
     * {@linkplain #get(Duration) value} just before the point in time is equal to
     * the value at the point in time.</li>
     * <li>The {@linkplain #getTransitionTimeAtOrAfter(Duration) transition time at
     * or after a time} that {@linkplain Duration#equals(Object) equals} one of the
     * transition times equals that transition time.</li>
     * <li>The returned set might be an
     * {@linkplain Collections#unmodifiableSortedSet(SortedSet) unmodifiable} view
     * of the transition times, which will incorporate any subsequent changes to
     * this history. It might be a newly constructed object that does not
     * incorporate subsequent changes.</li>
     * <li>This method is typically more efficient that using the
     * {@linkplain #getTransitions() transitions map}.</li>
     * </ul>
     *
     * @return the transition times
     */
    @Nonnull
    SortedSet<Duration> getTransitionTimes();

    /**
     * <p>
     * A hash code for this value history.
     * </p>
     * <p>
     * The hash code of a value history is the hash
     * code of the {@linkplain #getFirstValue() first value} (or 0 if the first
     * value is null) plus the {@linkplain Map#hashCode() hash code} of the
     * {@linkplain #getTransitions() transitions}.
     * </p>
     *
     * @return the hash code
     */
    @Override
    int hashCode();

    /**
     * <p>
     * Whether this history is empty.
     * </p>
     * <ul>
     * <li>A value history is empty if, and only if, it
     * {@linkplain SortedSet#isEmpty() has no} {@linkplain #getTransitionTimes()
     * transitions}.</li>
     * <li>This method is typically more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     */
    boolean isEmpty();

    /**
     * <p>
     * Create a stream of the {@linkplain #getTransitions() transitions} in the
     * value of this history.
     * </p>
     * <ul>
     * <li>Always creates a (non null) steam.</li>
     * <li>The stream of transitions contains elements equal to the
     * {@linkplain Set#stream() stream} of {@linkplain Map#entrySet() entries} of
     * the {@linkplain #getTransitions() transitions map}.</li>
     * <li>Using the stream of transitions is typically more efficient than getting
     * the {@linkplain #getTransitions() transitions map} and then creating a stream
     * from its entries.</li>
     * </ul>
     *
     * @return a stream of the transitions.
     */
    @Nonnull
    Stream<Map.Entry<Duration, VALUE>> streamOfTransitions();

}
