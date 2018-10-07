package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Collections;
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
public interface ValueHistory<VALUE> {
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
    public static final Duration END_OF_TIME = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);

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
    public VALUE get(Duration t);

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
    public Duration getFirstTansitionTime();

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
    public VALUE getFirstValue();

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
    public Duration getLastTansitionTime();

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
    public VALUE getLastValue();

    /**
     * <p>
     * The points in time when the value of this history changes.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of transition times.</li>
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
     * <li>The returned set is an
     * {@linkplain Collections#unmodifiableSortedSet(SortedSet) unmodifiable} view
     * of the transition times, which will incorporate any subsequent changes to
     * this history.</li>
     * </ul>
     * 
     * @return the transition times
     */
    public SortedSet<Duration> getTransitionTimes();

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
    public boolean isEmpty();
}
