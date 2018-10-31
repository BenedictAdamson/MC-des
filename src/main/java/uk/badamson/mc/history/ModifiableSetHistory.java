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
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

/**
 * <p>
 * The modifiable time-wise variation of a set of value that changes at discrete
 * points in time.
 * </p>
 * 
 * @param VALUE
 *            The class of values of this value history. This must be an
 *            {@link Immutable immutable} type.
 * 
 * @see ModifiableValueHistory
 */
@NotThreadSafe
public final class ModifiableSetHistory<VALUE> implements SetHistory<VALUE> {

    private static final ValueHistory<Boolean> ABSENT = new ModifiableValueHistory<>(Boolean.FALSE);

    private final Set<VALUE> firstValue = new HashSet<>();
    private final Map<VALUE, ModifiableValueHistory<Boolean>> containsMap = new HashMap<>();

    /**
     * <p>
     * Construct an set value history that is unknown (null) for all points in time.
     * </p>
     * <ul>
     * <li>This {@linkplain #isEmpty() is empty}.</li>
     * </ul>
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
     * has its {@linkplain ValueHistory#getLastTansitionTime() last transition time}
     * is at or before the given time.</li>
     * </ul>
     * 
     * @param when
     *            The point in time from which this set history must have the
     *            {@code value} as one of the values of the set, represented as the
     *            duration since an (implied) epoch.
     * @param value
     *            The value that this set history must have as one of the values of
     *            the set at or after the given point in time.
     * 
     * @throws NullPointerException
     *             If {@code when} is null.
     * 
     * @see ModifiableValueHistory#setValueFrom(Duration, Object)
     * @see Set#add(Object)
     */
    public final void addFrom(@NonNull Duration when, @Nullable VALUE value) {
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
     * has its {@linkplain ValueHistory#getFirstTansitionTime() first transition
     * time} after the given time.</li>
     * </ul>
     * 
     * @param when
     *            The point in time until which this set history must have the
     *            {@code value} as one of the values of the set, represented as the
     *            duration since an (implied) epoch.
     * @param value
     *            The value that this set history must have as one of the values of
     *            the set at or before the given point in time.
     * 
     * @throws NullPointerException
     *             If {@code when} is null.
     * 
     * @see ModifiableValueHistory#setValueUntil(Duration, Object)
     * @see Set#add(Object)
     */
    public final void addUntil(@NonNull Duration when, @Nullable VALUE value) {
        Objects.requireNonNull(when, "when");
        var c = containsMap.get(value);
        if (c == null) {
            c = new ModifiableValueHistory<>(Boolean.FALSE);
            containsMap.put(value, c);
        }
        c.setValueUntil(when, Boolean.TRUE);
        firstValue.add(value);
    }

    @Override
    public final @NonNull ValueHistory<Boolean> contains(@Nullable VALUE value) {
        final var c = containsMap.get(value);
        return c == null ? ABSENT : c;
    }

    @Override
    public final boolean equals(Object that) {
        if (that == null)
            return false;
        if (this == that)
            return true;
        if (that instanceof ModifiableSetHistory) {
            @SuppressWarnings("unchecked")
            final ModifiableSetHistory<VALUE> thatValueHistory = (ModifiableSetHistory<VALUE>) that;
            return firstValue.equals(thatValueHistory.firstValue) && containsMap.equals(thatValueHistory.containsMap);
        } else if (that instanceof ValueHistory) {
            @SuppressWarnings("unchecked")
            final ValueHistory<VALUE> thatValueHistory = (ValueHistory<VALUE>) that;
            return Objects.equals(getFirstValue(), thatValueHistory.getFirstValue())
                    && getTransitions().equals(thatValueHistory.getTransitions());
        } else {
            return false;
        }
    }

    @Override
    public final @NonNull Set<VALUE> get(@NonNull Duration t) {
        Objects.requireNonNull(t, "t");
        Set<VALUE> result = containsMap.entrySet().stream().filter(e -> e.getValue().get(t).booleanValue())
                .map(e -> e.getKey()).collect(Collectors.toSet());
        return result;
    }

    @Override
    public final @Nullable Duration getFirstTansitionTime() {
        return containsMap.values().stream().map(contains -> contains.getFirstTansitionTime())
                .min((t1, t2) -> t1.compareTo(t2)).orElse(null);
    }

    @Override
    public final @NonNull Set<VALUE> getFirstValue() {
        return Collections.unmodifiableSet(firstValue);
    }

    @Override
    public final @Nullable Duration getLastTansitionTime() {
        return containsMap.values().stream().map(contains -> contains.getLastTansitionTime())
                .max((t1, t2) -> t1.compareTo(t2)).orElse(null);
    }

    @Override
    public final @NonNull Set<VALUE> getLastValue() {
        return get(END_OF_TIME);
    }

    @Override
    public final Duration getTansitionTimeAtOrAfter(@NonNull final Duration when) {
        Objects.requireNonNull(when, "when");
        return streamOfTransitionTimes().filter(t -> when.compareTo(t) <= 0).min(Comparator.naturalOrder())
                .orElse(null);
    }

    @Override
    public final @NonNull SortedMap<Duration, Set<VALUE>> getTransitions() {
        final SortedMap<Duration, Set<VALUE>> transitions = new TreeMap<>();
        for (var t : getTransitionTimes()) {
            transitions.put(t, get(t));
        }
        return transitions;
    }

    @Override
    public final @NonNull SortedSet<Duration> getTransitionTimes() {
        return new TreeSet<>(streamOfTransitionTimes().collect(Collectors.toSet()));
    }

    @Override
    public final int hashCode() {
        return getFirstValue().hashCode() + streamOfTransitions().mapToInt(entry -> entry.hashCode()).sum();
    }

    @Override
    public final boolean isEmpty() {
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
     * </ul>
     * 
     * @param value
     *            The value to remove
     * 
     * @see Set#remove(Object)
     */
    public final void remove(@Nullable VALUE value) {
        containsMap.remove(value);
        firstValue.remove(value);
    }

    @Override
    public final @NonNull Stream<Map.Entry<Duration, Set<VALUE>>> streamOfTransitions() {
        return streamOfTransitionTimes().distinct().map(t -> new AbstractMap.SimpleImmutableEntry<>(t, get(t)));
    }

    private Stream<Duration> streamOfTransitionTimes() {
        return containsMap.values().stream().flatMap(contains -> contains.streamOfTransitions())
                .map(transition -> transition.getKey());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ModifiableSetHistory [");
        builder.append(getFirstValue());
        builder.append(", ");
        builder.append(getTransitions());
        builder.append("]");
        return builder.toString();
    }

}
