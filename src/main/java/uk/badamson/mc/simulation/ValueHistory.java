package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

/**
 * <p>
 * The time-wise variation of a value that changes at discrete points in time.
 * </p>
 * 
 * @param VALUE
 *            The class of values of this value history. This must be an
 *            {@link Immutable immutable} type.
 */
@NotThreadSafe
public final class ValueHistory<VALUE> {
    /**
     * <p>
     * The smallest (most negative) {@link Duration} value.
     * </p>
     */
    public static final Duration START_OF_TIME = Duration.ofSeconds(Long.MIN_VALUE);

    /**
     * <p>
     * The largest (most positive) {@link Duration} value.
     * </p>
     */
    public static final Duration END_OF_TIME = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);;

    /**
     * <p>
     * Construct an value history that is null for all points in time.
     * </p>
     * <ul>
     * <li>The {@linkplain #get(Duration) value} of this history at the
     * {@linkplain #START_OF_TIME start of time} is null.</li>
     * <li>This {@linkplain SortedSet#isEmpty() has no}
     * {@linkplain #getTransitionTimes() transition times}.</li>
     * </ul>
     */
    public ValueHistory() {
        // TODO
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
    public VALUE get(Duration t) {
        Objects.requireNonNull(t, "t");
        return null;// TODO
    }

    /**
     * <p>
     * The points in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of transition times.</li>
     * <li>There is not a transition at the {@linkplain ValueHistory#START_OF_TIME
     * start of time}.</li>
     * <li>For all points in time in the set of transition times, the
     * {@linkplain #get(Duration) value} just before the transition is not equal to
     * the value at the transition.</li>
     * <li>For all points in time not in the set of transition times (except the
     * {@linkplain ValueHistory#START_OF_TIME start of time}), the
     * {@linkplain #get(Duration) value} just before the point in time is equal to
     * the value at the point in time.</li>
     * </ul>
     * 
     * @return the transition times
     */
    public SortedSet<Duration> getTransitionTimes() {
        return Collections.emptySortedSet();// TODO
    }
}
