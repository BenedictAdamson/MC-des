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
 * The modifiable time-wise variation of a value that changes at discrete points
 * in time.
 * </p>
 *
 * @param <VALUE> The class of values of this value history. This must be {@link Immutable immutable}, or have reference semantics.
 */
@NotThreadSafe
public final class ModifiableValueHistory<VALUE> extends AbstractValueHistory<VALUE> {

    private final NavigableMap<Duration, VALUE> transitions = new TreeMap<>();

    @Nullable
    private VALUE firstValue;

    /**
     * <p>
     * Construct a value history that is initially null for all points in time.
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
     * Construct a value history that initially has the same given value for all
     * points in time.
     * </p>
     * <ul>
     * <li>This {@linkplain #isEmpty() is empty}.</li>
     * <li>The {@linkplain #getFirstValue() value of this history at the start of
     * time} is the given value.</li>
     * </ul>
     */
    public ModifiableValueHistory(@Nullable final VALUE value) {
        firstValue = value;
    }

    /**
     * <p>
     * Construct a value history with a given sequence of state transitions
     * </p>
     * <ul>
     * <li>The {@linkplain #getFirstValue() first value} of this history is the same
     * as the given {@code firstValue}.</li>
     * <li>The {@linkplain #getTransitions() transitions} of this history is
     * {@linkplain SortedMap#equals(Object) equivalent to} the given
     * {@code transitions} map.</li>
     * </ul>
     *
     * @throws IllegalArgumentException If adjacent {@linkplain SortedMap#values() values} of
     *                                  {@code transitions} are
     *                                  {@linkplain Objects#equals(Object, Object) equivalent or
     *                                  equivalently null}.
     */
    public ModifiableValueHistory(@Nullable final VALUE firstValue,
                                  @Nonnull final SortedMap<Duration, VALUE> transitions) {
        Objects.requireNonNull(transitions, "transitions");
        this.firstValue = firstValue;
        appendTransitions(transitions.entrySet().stream());
        // Check after copy to avoid race hazards.
        var previous = firstValue;
        for (final var value : this.transitions.values()) {
            if (Objects.equals(previous, value)) {
                throw new IllegalArgumentException("transitions " + this.transitions);
            }
            previous = value;
        }
    }

    /**
     * <p>
     * Construct a value history that is initially a copy of a given value history
     * </p>
     * <ul>
     * <li>This {@linkplain #equals(Object) equals} the given value history.</li>
     * </ul>
     */
    public ModifiableValueHistory(@Nonnull final ValueHistory<VALUE> that) {
        Objects.requireNonNull(that, "that");
        firstValue = that.getFirstValue();
        appendTransitions(that.streamOfTransitions());
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
     * <li>The given point in time becomes the {@linkplain #getLastTransitionTime()
     * last transition time}.</li>
     * <li>The given value becomes the {@linkplain #getLastValue() last value}.</li>
     * </ul>
     *
     * @param when  The point in time when the transition occurs, represented as the
     *              duration since an (implied) epoch.
     * @param value The value at and after the transition.
     * @throws IllegalStateException If
     *                               <ul>
     *                                   <li>{@code when} is at or before the {@linkplain #getLastTransitionTime() last transition time}.</li>
     *                                   <li>{@code when} is {@linkplain Objects#equals(Object, Object) equivalent to} the {@linkplain #getLastValue() last value}.</li>
     *                               </ul>
     *                               This history is unchanged if the method throws IllegalStateException.
     *                                                                                                                                                                                             {@link IllegalStateException}.
     * @see #setValueFrom(Duration, Object)
     */
    public void appendTransition(@Nonnull final Duration when, final VALUE value) throws IllegalStateException {
        Objects.requireNonNull(when, "when");
        final var lastTransition = transitions.lastEntry();
        if (lastTransition != null && when.compareTo(lastTransition.getKey()) <= 0) {
            throw new IllegalStateException("Timestamp out of order");
        } else if (lastTransition != null && Objects.equals(value, lastTransition.getValue())) {
            throw new IllegalStateException("Equal values");
        } else if (lastTransition == null && Objects.equals(firstValue, value)) {
            throw new IllegalStateException("First appended value equals value at start of time");
        }
        transitions.put(when, value);
    }

    private void appendTransitions(@Nonnull final Stream<Map.Entry<Duration, VALUE>> streamOfTransitions) {
        streamOfTransitions.sequential().forEach(entry -> transitions.put(entry.getKey(), entry.getValue()));
    }

    private void clear(final VALUE value) {
        firstValue = value;
        transitions.clear();
    }

    @Override
    public boolean equals(final Object that) {
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }
        if (that instanceof ModifiableValueHistory) {
            // Optimisation
            final ModifiableValueHistory<?> thatValueHistory = (ModifiableValueHistory<?>) that;
            return Objects.equals(firstValue, thatValueHistory.firstValue)
                    && transitions.equals(thatValueHistory.transitions);
        } else {
            return super.equals(that);
        }
    }

    @Override
    public @Nullable
    VALUE get(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        final var previousTransition = transitions.floorEntry(when);
        return previousTransition == null ? firstValue : previousTransition.getValue();
    }

    @Nonnull
    @Override
    public TimestampedValue<VALUE> getTimestampedValue(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        final SortedMap<Duration, VALUE> atOrBefore;
        final SortedMap<Duration, VALUE> after;
        if (END_OF_TIME.equals(when)) {
            atOrBefore = transitions;
            after = Collections.emptyNavigableMap();

        } else {
            atOrBefore = transitions.headMap(when.plusNanos(1));
            after = transitions.tailMap(when.plusNanos(1));
        }
        final var start = atOrBefore.isEmpty() ? START_OF_TIME : atOrBefore.lastKey();
        final Duration end = after.isEmpty() ? END_OF_TIME : after.firstKey().minusNanos(1);
        final var value = atOrBefore.isEmpty() ? firstValue : atOrBefore.get(atOrBefore.lastKey());
        return new TimestampedValue<>(start, end, value);
    }

    @Nullable
    @Override
    public Duration getFirstTransitionTime() {
        return transitions.isEmpty() ? null : transitions.firstKey();
    }

    @Nullable
    @Override
    public VALUE getFirstValue() {
        return firstValue;
    }

    @Nullable
    @Override
    public Duration getLastTransitionTime() {
        return transitions.isEmpty() ? null : transitions.lastKey();
    }

    @Nullable
    @Override
    public VALUE getLastValue() {
        final var lastTransition = transitions.lastEntry();
        return lastTransition == null ? firstValue : lastTransition.getValue();
    }

    @Nullable
    @Override
    public Duration getTransitionTimeAtOrAfter(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        return transitions.ceilingKey(when);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Furthermore, for the ModifiableValueHistory type
     * </p>
     * <ul>
     * <li>The transitions map is a newly constructed object.</li>
     * </ul>
     */
    @Nonnull
    @Override
    public SortedMap<Duration, VALUE> getTransitions() {
        return new TreeMap<>(transitions);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Furthermore, for the ModifiableValueHistory.
     * </p>
     * <ul>
     * <li>The returned set is an
     * {@linkplain Collections#unmodifiableSortedSet(SortedSet) unmodifiable} view
     * of the transition times, which will incorporate any subsequent changes to
     * this history.</li>
     * </ul>
     */
    @Nonnull
    @Override
    public SortedSet<Duration> getTransitionTimes() {
        return Collections.unmodifiableSortedSet(transitions.navigableKeySet());
    }

    @Override
    public int hashCode() {
        return (firstValue == null ? 0 : firstValue.hashCode()) + transitions.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return transitions.isEmpty();
    }

    /**
     * <p>
     * Change this value history so the {@linkplain #getTransitionTimes() set of
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
     * @see #appendTransition(Duration, Object)
     */
    public void removeTransitionsFrom(@Nonnull final Duration when) {
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
     * {@linkplain #getLastTransitionTime() last transition time} is at or before the
     * given time.</li>
     * </ul>
     *
     * @see #appendTransition(Duration, Object)
     * @see #setValueUntil(Duration, Object)
     */
    public void setValueFrom(@Nonnull final Duration when, @Nullable final VALUE value) {
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
     * {@linkplain #getFirstTransitionTime() first transition time} is at or after
     * the given time.</li>
     * </ul>
     *
     * @see #setValueFrom(Duration, Object)
     */
    public void setValueUntil(@Nonnull final Duration when, @Nullable final VALUE value) {
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

    @Nonnull
    @Override
    public Stream<Map.Entry<Duration, VALUE>> streamOfTransitions() {
        return transitions.entrySet().stream();
    }

}
