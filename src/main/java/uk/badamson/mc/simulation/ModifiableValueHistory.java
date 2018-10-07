package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;

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
public final class ModifiableValueHistory<VALUE> implements ValueHistory<VALUE> {

    private final NavigableMap<Duration, VALUE> transitions = new TreeMap<>();

    /**
     * <p>
     * Construct an value history that is null for all points in time.
     * </p>
     * <ul>
     * <li>This {@linkplain #isEmpty() is empty}.</li>
     * <li>The {@linkplain #getFirstValue() value of this history at the start of
     * time} is null.</li>
     * </ul>
     */
    public ModifiableValueHistory() {
        // Do nothing
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
     *             null as)} the {@linkplain #getLastValue() last value}. In this
     *             case this history is unchanged.</li>
     *             </ul>
     *             This history is unchanged if it throws
     *             {@link IllegalStateException}.
     */
    public void appendTransition(Duration when, VALUE value) throws IllegalStateException {
        Objects.requireNonNull(when, "when");
        final var lastTransition = transitions.lastEntry();
        if (lastTransition != null && when.compareTo(lastTransition.getKey()) <= 0) {
            throw new IllegalStateException("Timestamp out of order");
        } else if (lastTransition != null && Objects.equals(value, lastTransition.getValue())) {
            throw new IllegalStateException("Equal values");
        } else if (lastTransition == null && value == null) {
            throw new IllegalStateException("First appended value equals value at start of time");
        }
        transitions.put(when, value);
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
    public final VALUE get(Duration t) {
        Objects.requireNonNull(t, "t");
        final var previousTransition = transitions.floorEntry(t);
        return previousTransition == null ? null : previousTransition.getValue();
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
    public final Duration getFirstTansitionTime() {
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
    public final VALUE getFirstValue() {
        return null;// TODO
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
    public final Duration getLastTansitionTime() {
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
    public final VALUE getLastValue() {
        final var lastTransition = transitions.lastEntry();
        return lastTransition == null ? null : lastTransition.getValue();
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
    public final SortedSet<Duration> getTransitionTimes() {
        return Collections.unmodifiableSortedSet(transitions.navigableKeySet());
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
}
