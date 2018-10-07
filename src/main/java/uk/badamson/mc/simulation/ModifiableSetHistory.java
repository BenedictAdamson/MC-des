package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final Set<VALUE> emptySet = Collections.emptySet();
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

    @Override
    public final ValueHistory<Boolean> contains(VALUE value) {
        final var c = containsMap.get(value);
        return c == null ? ABSENT : c;
    }

    @Override
    public final Set<VALUE> get(Duration t) {
        Objects.requireNonNull(t, "t");
        Set<VALUE> result = containsMap.entrySet().stream().filter(e -> e.getValue().get(t).booleanValue())
                .map(e -> e.getKey()).collect(Collectors.toSet());
        if (result.isEmpty()) {
            result = emptySet;
        }
        return result;
    }

    @Override
    public final Duration getFirstTansitionTime() {
        return containsMap.values().stream().map(contains -> contains.getFirstTansitionTime())
                .min((t1, t2) -> t1.compareTo(t2)).orElse(null);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * What is more:
     * </p>
     * <ul>
     * <li>The first value is an {@linkplain Set#isEmpty() empty set}.</li>
     * </ul>
     */
    @Override
    public final Set<VALUE> getFirstValue() {
        return emptySet;
    }

    @Override
    public final Duration getLastTansitionTime() {
        return containsMap.values().stream().map(contains -> contains.getLastTansitionTime())
                .max((t1, t2) -> t1.compareTo(t2)).orElse(null);
    }

    @Override
    public final Set<VALUE> getLastValue() {
        return get(END_OF_TIME);
    }

    @Override
    public final SortedSet<Duration> getTransitionTimes() {
        return new TreeSet<>(streamOfTransitionTimes().collect(Collectors.toSet()));
    }

    @Override
    public final boolean isEmpty() {
        return containsMap.isEmpty();
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
     */
    public final void setPresentFrom(Duration when, VALUE value) {
        Objects.requireNonNull(when, "when");
        var c = containsMap.get(value);
        if (c == null) {
            c = new ModifiableValueHistory<>(Boolean.FALSE);
            containsMap.put(value, c);
        }
        c.setValueFrom(when, Boolean.TRUE);
    }

    @Override
    public final Stream<Map.Entry<Duration, Set<VALUE>>> streamOfTransitions() {
        return streamOfTransitionTimes().map(t -> new AbstractMap.SimpleImmutableEntry<>(t, get(t)));
    }

    private Stream<Duration> streamOfTransitionTimes() {
        return containsMap.values().stream().flatMap(contains -> contains.streamOfTransitions())
                .map(transition -> transition.getKey()).distinct();
    }

}
