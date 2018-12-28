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
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

/**
 * <p>
 * The modifiable time-wise variation of a value that changes at discrete points
 * in time.
 * </p>
 *
 * @param VALUE
 *            The class of values of this value history. This must be an
 *            {@link Immutable immutable} type.
 */
@NotThreadSafe
public final class ModifiableValueHistory<VALUE> extends AbstractValueHistory<VALUE> {

    @Nullable
    private VALUE firstValue;
    private final NavigableMap<Duration, VALUE> transitions = new TreeMap<>();

    /**
     * <p>
     * Construct a value history that is null for all points in time.
     * </p>
     * <ul>
     * <li>This {@linkplain #isEmpty() is empty}.</li>
     * <li>The {@linkplain #getFirstValue() value of this history at the start of
     * time} is null.</li>
     * </ul>
     */
    public ModifiableValueHistory() {
        firstValue = null;
    }

    /**
     * <p>
     * Construct a value history that has the same given value for all points in
     * time.
     * </p>
     * <ul>
     * <li>This {@linkplain #isEmpty() is empty}.</li>
     * <li>The {@linkplain #getFirstValue() value of this history at the start of
     * time} is the given value.</li>
     * </ul>
     *
     * @param value
     *            The value at all points in time
     */
    public ModifiableValueHistory(@Nullable final VALUE value) {
        firstValue = value;
    }

    /**
     * <p>
     * Construct a value history that is a copy of a given value history
     * </p>
     * <ul>
     * <li>This {@linkplain #equals(Object) equals} the given value history.</li>
     * </ul>
     *
     * @param that
     *            The value history to copy.
     * @throws NullPointerException
     *             If {@code that} is null
     */
    public ModifiableValueHistory(@NonNull final ValueHistory<VALUE> that) {
        Objects.requireNonNull(that, "that");
        firstValue = that.getFirstValue();
        that.streamOfTransitions().sequential().forEach(entry -> transitions.put(entry.getKey(), entry.getValue()));
    }

    /**
     * <p>
     * Append a value transition to this history of value transitions.
     * </p>
     * <ul>
     * <li>Appending a transition does not
     * {@linkplain SortedSet#containsAll(java.util.Collection) remove} any times
     * from the {@linkplain #getTransitionTimes() set of transition times}.</li>
     * <li>Appending a transition does not change the {@linkplain #get(Duration)
     * values} before the given point in time.</li>
     * <li>Appending a transition increments the {@linkplain SortedSet#size() number
     * of} {@linkplain #getTransitionTimes() transition times}.</li>
     * <li>The given point in time becomes the {@linkplain #getLastTansitionTime()
     * last transition time}.</li>
     * <li>The given value becomes the {@linkplain #getLastValue() last value}.</li>
     * </ul>
     *
     * @param when
     *            The point in time when the transition occurs, represented as the
     *            duration since an (implied) epoch.
     * @param value
     *            The value at and after the transition.
     *
     * @throws NullPointerException
     *             If {@code when} is null.
     * @throws IllegalStateException
     *             <ul>
     *             <li>If {@code when} is not
     *             {@linkplain Duration#compareTo(Duration) after} the
     *             {@linkplain #getLastTansitionTime() last transition time}.</li>
     *             <li>If {@code value} is
     *             {@linkplain Objects#equals(Object, Object) equal to (or equally
     *             null as)} the {@linkplain #getLastValue() last value}.</li>
     *             </ul>
     *             This history is unchanged if it throws
     *             {@link IllegalStateException}.
     *
     * @see #setValueFrom(Duration, Object)
     */
    public void appendTransition(@NonNull final Duration when, final VALUE value) throws IllegalStateException {
        Objects.requireNonNull(when, "when");
        final var lastTransition = transitions.lastEntry();
        if (lastTransition != null && when.compareTo(lastTransition.getKey()) <= 0)
            throw new IllegalStateException("Timestamp out of order");
        else if (lastTransition != null && Objects.equals(value, lastTransition.getValue()))
            throw new IllegalStateException("Equal values");
        else if (lastTransition == null && Objects.equals(firstValue, value))
            throw new IllegalStateException("First appended value equals value at start of time");
        transitions.put(when, value);
    }

    private void clear(final VALUE value) {
        firstValue = value;
        transitions.clear();
    }

    @Override
    public final boolean equals(final Object that) {
        if (that == null)
            return false;
        if (this == that)
            return true;
        if (that instanceof ModifiableValueHistory) {
            // Optimisation
            @SuppressWarnings("unchecked")
            final ModifiableValueHistory<VALUE> thatValueHistory = (ModifiableValueHistory<VALUE>) that;
            return Objects.equals(firstValue, thatValueHistory.firstValue)
                    && transitions.equals(thatValueHistory.transitions);
        } else
            return super.equals(that);
    }

    /**
     * <p>
     * Get the value at a given point in time.
     * </p>
     *
     * @param when
     *            The point in time of interest, expressed as a duration since an
     *            epoch.
     * @return The value at the given point in time.
     * @throws NullPointerException
     *             If {@code when} is null.
     */
    @Override
    public final @Nullable VALUE get(@NonNull final Duration when) {
        Objects.requireNonNull(when, "when");
        final var previousTransition = transitions.floorEntry(when);
        return previousTransition == null ? firstValue : previousTransition.getValue();
    }

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
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @return the first transition time.
     */
    @Override
    public final @Nullable Duration getFirstTansitionTime() {
        return transitions.isEmpty() ? null : transitions.firstKey();
    }

    /**
     * <p>
     * The {@linkplain #get(Duration) value} of this history at the
     * {@linkplain #START_OF_TIME start of time}.
     * </p>
     * <ul>
     * <li>The first value is the same as the {@linkplain #get(Duration) value at}
     * the {@linkplain #START_OF_TIME start of time}.</li>
     * <li>This method is more efficient than using the {@link #get(Duration)}
     * method.</li>
     * </ul>
     *
     * @return the last value.
     */
    @Override
    public final @Nullable VALUE getFirstValue() {
        return firstValue;
    }

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
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     * @return the last transition time.
     */
    @Override
    public final @Nullable Duration getLastTansitionTime() {
        return transitions.isEmpty() ? null : transitions.lastKey();
    }

    /**
     * <p>
     * The {@linkplain #get(Duration) value} of this history at the
     * {@linkplain #END_OF_TIME end of time}.
     * </p>
     * <ul>
     * <li>The last value is the same as the {@linkplain #get(Duration) value at}
     * the {@linkplain #END_OF_TIME end of time}.</li>
     * <li>If this history has no {@linkplain #getTransitionTimes() transitions},
     * the last value is the same as the {@linkplain #getFirstValue() first
     * value}.</li>
     * <li>If this history has {@linkplain #getTransitionTimes() transitions}, the
     * last value is the same as the value at the
     * {@linkplain #getLastTansitionTime() last transition}.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} and {@link #get(Duration)} methods.</li>
     * </ul>
     *
     * @return the last value.
     */
    @Override
    public final @Nullable VALUE getLastValue() {
        final var lastTransition = transitions.lastEntry();
        return lastTransition == null ? firstValue : lastTransition.getValue();
    }

    @Override
    public final Duration getTansitionTimeAtOrAfter(@NonNull final Duration when) {
        Objects.requireNonNull(when, "when");
        return transitions.ceilingKey(when);
    }

    @Override
    public final @NonNull SortedMap<Duration, VALUE> getTransitions() {
        return new TreeMap<>(transitions);
    }

    /**
     * <p>
     * The points in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of transition times.</li>
     * <li>The transition times are represented as the duration since an (implied)
     * epoch.</li>
     * <li>There is not a transition at the
     * {@linkplain ModifiableValueHistory#START_OF_TIME start of time}.</li>
     * <li>For all points in time in the set of transition times, the
     * {@linkplain #get(Duration) value} just before the transition is not equal to
     * the value at the transition.</li>
     * <li>For all points in time not in the set of transition times (except the
     * {@linkplain ModifiableValueHistory#START_OF_TIME start of time}), the
     * {@linkplain #get(Duration) value} just before the point in time is equal to
     * the value at the point in time.</li>
     * <li>The returned set is an
     * {@linkplain Collections#unmodifiableSortedSet(SortedSet) unmodifiable} view
     * of the transition times, which will incorporate any subsequent changes to
     * this history.</li>
     * </ul>
     *
     * @return the transition times
     */
    @Override
    public final @NonNull SortedSet<Duration> getTransitionTimes() {
        return Collections.unmodifiableSortedSet(transitions.navigableKeySet());
    }

    @Override
    public final int hashCode() {
        return (firstValue == null ? 0 : firstValue.hashCode()) + transitions.hashCode();
    }

    /**
     * <p>
     * Whether this history is empty.
     * </p>
     * <ul>
     * <li>A value history is empty if, and only if, it
     * {@linkplain SortedSet#isEmpty() has no} {@linkplain #getTransitionTimes()
     * transitions}.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} method.</li>
     * </ul>
     *
     */
    @Override
    public final boolean isEmpty() {
        return transitions.isEmpty();
    }

    /**
     * <p>
     * Change this value history so {@linkplain #getTransitionTimes() set of
     * transitions} has no transitions at or after a given point in time.
     * </p>
     * <ul>
     * <li>The {@linkplain #getFirstValue() first value} of the history is
     * unchanged.</li>
     * <li>The {@linkplain #getTransitionTimes() set of transition times}
     * {@linkplain SortedSet#contains(Object) contains} no times at or after the
     * given time.</li>
     * <li>Removing transitions from a given point in time does not change the
     * {@linkplain #getTransitions() transitions} before the point in time.</li>
     * </ul>
     *
     * @param when
     *            The point in time from which transitions must be removed,
     *            represented as the duration since an (implied) epoch.
     *
     * @throws NullPointerException
     *             If {@code when} is null.
     *
     * @see #appendTransition(Duration, Object)
     */
    public final void removeTransitionsFrom(@NonNull final Duration when) {
        Objects.requireNonNull(when, "when");
        transitions.keySet().removeIf(t -> when.compareTo(t) <= 0);
    }

    /**
     * <p>
     * Change this value history so the value {@linkplain #get(Duration) at} all
     * points in time {@linkplain Duration#compareTo(Duration) at or after} a given
     * point in time is equal to a given value.
     * </p>
     * <ul>
     * <li>Setting the value from a given time does not change the
     * {@linkplain #get(Duration) values} before the given point in time.</li>
     * <li>The given value is the {@linkplain #getLastValue() last value}.</li>
     * <li>The given value is {@linkplain Object#equals(Object) equal} to
     * {@linkplain #get(Duration) the value at} the given time.</li>
     * <li>If this {@linkplain #isEmpty() has any transitions}, the
     * {@linkplain #getLastTansitionTime() last transition time} is at or before the
     * given time.</li>
     * </ul>
     *
     * @param when
     *            The point in time from which this history must have the
     *            {@code value}, represented as the duration since an (implied)
     *            epoch.
     * @param value
     *            The value that this history must have at or after teh given point
     *            in time.
     *
     * @throws NullPointerException
     *             If {@code when} is null.
     *
     * @see #appendTransition(Duration, Object)
     * @see #setValueUntil(Duration, Object)
     */
    public final void setValueFrom(@NonNull final Duration when, @Nullable final VALUE value) {
        Objects.requireNonNull(when, "when");
        if (when.equals(START_OF_TIME)) {
            clear(value);
        } else {
            transitions.keySet().removeIf(t -> when.compareTo(t) < 0);
            if (!Objects.equals(getLastValue(), value)) {
                transitions.put(when, value);
            }
        }
    }

    /**
     * <p>
     * Change this value history so the value {@linkplain #get(Duration) at} all
     * points in time {@linkplain Duration#compareTo(Duration) at or before} a given
     * point in time is equal to a given value.
     * </p>
     * <ul>
     * <li>Setting the value until a given time does not change the
     * {@linkplain #get(Duration) values} after the given point in time.</li>
     * <li>The given value is the {@linkplain #getFirstValue() first value}.</li>
     * <li>The given value is {@linkplain Object#equals(Object) equal} to
     * {@linkplain #get(Duration) the value at} the given time.</li>
     * <li>If this {@linkplain #isEmpty() has any transitions}, the
     * {@linkplain #getFirstTansitionTime() first transition time} is at or after
     * the given time.</li>
     * </ul>
     *
     * @param when
     *            The point in time until which this history must have the
     *            {@code value}, represented as the duration since an (implied)
     *            epoch.
     * @param value
     *            The value that this history must have at or before the given point
     *            in time.
     *
     * @throws NullPointerException
     *             If {@code when} is null.
     *
     * @see #setValueFrom(Duration, Object)
     */
    public final void setValueUntil(@NonNull final Duration when, @Nullable final VALUE value) {
        Objects.requireNonNull(when, "when");
        if (when.equals(END_OF_TIME)) {
            clear(value);
        } else {
            final VALUE lastValue0 = getLastValue();
            firstValue = value;
            transitions.keySet().removeIf(t -> t.compareTo(when) <= 0);
            if (!Objects.equals(lastValue0, getLastValue())) {
                transitions.put(when.plusNanos(1L), lastValue0);
            }
        }
    }

    @Override
    public final @NonNull Stream<Entry<Duration, VALUE>> streamOfTransitions() {
        return transitions.entrySet().stream();
    }

}
