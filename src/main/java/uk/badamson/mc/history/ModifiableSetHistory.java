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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * The modifiable time-wise variation of a set of values that changes at discrete
 * points in time.
 * </p>
 *
 * @param <VALUE> The class of values of this set history. This must be {@link Immutable immutable}, or have reference semantics.
 * @see ModifiableValueHistory
 */
@NotThreadSafe
public final class ModifiableSetHistory<VALUE> extends AbstractValueHistory<Set<VALUE>> implements SetHistory<VALUE> {

    private static final ValueHistory<Boolean> ABSENT = new ConstantValueHistory<>(Boolean.FALSE);

    private final Set<VALUE> firstValue = new HashSet<>();

    private final Map<VALUE, ModifiableValueHistory<Boolean>> containsMap = new HashMap<>();

    /**
     * <p>
     * Construct a set value history that is initially {@linkplain Set#isEmpty()
     * empty} for all points in time.
     */
    public ModifiableSetHistory() {
        // Do nothing
    }

    /**
     * <p>
     * Change this set history so the set {@linkplain #get(Duration) at} all points
     * in time {@linkplain Duration#compareTo(Duration) at or after} a given point
     * in time {@linkplain Set#contains(Object) contains} a given value.
     * </p>
     * <ul>
     * <li>Setting the value from a given time does not change the
     * {@linkplain #get(Duration) set} before the given point in time.</li>
     * <li>The {@linkplain ValueHistory#getLastValue() last value} of the
     * {@linkplain #contains(Object) contains history} for the given value is
     * {@link Boolean#TRUE}.</li>
     * <li>The {@linkplain ValueHistory#get(Duration) value at} the given time of
     * the {@linkplain #contains(Object) contains history} for the given value is
     * {@link Boolean#TRUE}.</li>
     * <li>The {@linkplain #contains(Object) contains history} for the given value
     * has its {@linkplain ValueHistory#getLastTransitionTime() last transition time}
     * is at or before the given time.</li>
     * </ul>
     *
     * @see ModifiableValueHistory#setValueFrom(Duration, Object)
     * @see Set#add(Object)
     */
    public void addFrom(@Nonnull final Duration when, @Nullable final VALUE value) {
        Objects.requireNonNull(when, "when");
        var c = containsMap.get(value);
        if (c == null) {
            c = new ModifiableValueHistory<>(Boolean.FALSE);
            containsMap.put(value, c);
        }
        c.setValueFrom(when, Boolean.TRUE);
    }

    /**
     * <p>
     * Change this set history so the set {@linkplain #get(Duration) at} all points
     * in time {@linkplain Duration#compareTo(Duration) at or before} a given point
     * in time {@linkplain Set#contains(Object) contains} a given value.
     * </p>
     * <ul>
     * <li>Setting presence until a given time does not change the
     * {@linkplain #get(Duration) set} after the given point in time.</li>
     * <li>The {@linkplain ValueHistory#getFirstValue() first value} of the
     * {@linkplain #contains(Object) contains history} for the given value is
     * {@link Boolean#TRUE}.</li>
     * <li>The {@linkplain ValueHistory#get(Duration) value at} the given time of
     * the {@linkplain #contains(Object) contains history} for the given value is
     * {@link Boolean#TRUE}.</li>
     * <li>The {@linkplain #contains(Object) contains history} for the given value
     * has its {@linkplain ValueHistory#getFirstTransitionTime() first transition
     * time} after the given time.</li>
     * </ul>
     *
     * @see ModifiableValueHistory#setValueUntil(Duration, Object)
     * @see Set#add(Object)
     */
    public void addUntil(@Nonnull final Duration when, @Nullable final VALUE value) {
        Objects.requireNonNull(when, "when");
        var c = containsMap.get(value);
        if (c == null) {
            c = new ModifiableValueHistory<>(Boolean.FALSE);
            containsMap.put(value, c);
        }
        c.setValueUntil(when, Boolean.TRUE);
        firstValue.add(value);
    }

    @Nonnull
    @Override
    public ValueHistory<Boolean> contains(@Nullable final VALUE value) {
        final var c = containsMap.get(value);
        return c == null ? ABSENT : c;
    }

    @Override
    public boolean equals(final Object that) {
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }
        if (that instanceof ModifiableSetHistory) {
            final ModifiableSetHistory<?> thatValueHistory = (ModifiableSetHistory<?>) that;
            return firstValue.equals(thatValueHistory.firstValue) && containsMap.equals(thatValueHistory.containsMap);
        } else {
            return super.equals(that);
        }
    }

    @Nonnull
    @Override
    public Set<VALUE> get(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        return containsMap.entrySet().stream().filter(e -> e.getValue().get(when))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    @Nonnull
    @Override
    public Set<VALUE> getLastValue() {
        final var lastValue = super.getLastValue();
        assert lastValue != null;
        return lastValue;
    }

    @Nullable
    @Override
    public Duration getFirstTransitionTime() {
        return containsMap.values().stream().map(ModifiableValueHistory::getFirstTransitionTime)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);
    }

    @Nonnull
    @Override
    public Set<VALUE> getFirstValue() {
        return Collections.unmodifiableSet(firstValue);
    }

    @Nullable
    @Override
    public Duration getLastTransitionTime() {
        return containsMap.values().stream().map(ModifiableValueHistory::getLastTransitionTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
    }

    @Override
    public Duration getTransitionTimeAtOrAfter(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        return streamOfTransitionTimes().filter(t -> when.compareTo(t) <= 0).min(Comparator.naturalOrder())
                .orElse(null);
    }

    @Nonnull
    @Override
    public SortedMap<Duration, Set<VALUE>> getTransitions() {
        final SortedMap<Duration, Set<VALUE>> transitions = new TreeMap<>();
        for (final var t : getTransitionTimes()) {
            transitions.put(t, get(t));
        }
        return transitions;
    }

    @Nonnull
    @Override
    public SortedSet<Duration> getTransitionTimes() {
        return streamOfTransitionTimes().collect(Collectors.toCollection(TreeSet::new));
    }

    @Nonnull
    @Override
    public Set<VALUE> getUniverse() {
        return new HashSet<>(containsMap.keySet());
    }

    @Override
    public int hashCode() {
        return getFirstValue().hashCode() + streamOfTransitions().mapToInt(Map.Entry::hashCode).sum();
    }

    @Override
    public boolean isEmpty() {
        return containsMap.isEmpty();
    }

    /**
     * <p>
     * Remove a given object from the set for which this is the history, for all
     * points in time.
     * </p>
     * <ul>
     * <li>This history does not {@linkplain #contains(Object) contain} the given
     * value at any points in time.</li>
     * <li>Whether this history {@linkplain #contains(Object) contains} other values
     * is unchanged.</li>
     * <li>The {@linkplain #getUniverse() universe} of this time varying set does
     * not {@linkplain Set#contains(Object) contain} the given value.</li>
     * </ul>
     *
     * @see Set#remove(Object)
     */
    public void remove(@Nullable final VALUE value) {
        containsMap.remove(value);
        firstValue.remove(value);
    }

    @Nonnull
    @Override
    public Stream<Map.Entry<Duration, Set<VALUE>>> streamOfTransitions() {
        return streamOfTransitionTimes().distinct().map(t -> new AbstractMap.SimpleImmutableEntry<>(t, get(t)));
    }

    private Stream<Duration> streamOfTransitionTimes() {
        return containsMap.values().stream().flatMap(ModifiableValueHistory::streamOfTransitions)
                .map(Map.Entry::getKey);
    }

}
