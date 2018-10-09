package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Stream;

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
     * Whether this value history is <dfn>equal</dfn> another object.
     * </p>
     * <p>
     * The {@link ValueHistory} class has <i>value semantics</i>: for this to be
     * equal to another object, the other object must also be a
     * {@link ValueHistory}, and the two value histories must have
     * {@linkplain Objects#equals(Object) equal (or equally null)}
     * {@linkplain #get(Duration) values} at all points in time. The latter is
     * equivalent to having the their {@linkplain #getFirstValue() first values}
     * equal (or equally null) and having {@linkplain SortedMap#equals(Object)
     * equal} {@linkplain #getTransitions() transitions}.
     * </p>
     * 
     * @param that
     *            The other object
     * @return Whether equal.
     */
    @Override
    public boolean equals(Object that);

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
     * <li>The last value is equal to the {@linkplain #get(Duration) value at} the
     * {@linkplain #END_OF_TIME end of time}.</li>
     * <li>If this history has no {@linkplain #getTransitionTimes() transitions},
     * the last value is the same as the {@linkplain #getFirstValue() first
     * value}.</li>
     * <li>If this history has {@linkplain #getTransitionTimes() transitions}, the
     * last value is {@linkplain Objects#equals(Object, Object) equal to (or equally
     * null as)} the value at the {@linkplain #getLastTansitionTime() last
     * transition}.</li>
     * <li>This method is more efficient than using the
     * {@link #getTransitionTimes()} and {@link #get(Duration)} methods.</li>
     * </ul>
     * 
     * @return the last value.
     */
    public VALUE getLastValue();

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
     * {@linkplain Objects#equals(Object, Object) equal (or equally null)} of the
     * {@linkplain #get(Duration) value} of this history at the time of their
     * corresponding {@linkplain Map.Entry#getKey() key}.</li>
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
    public SortedMap<Duration, VALUE> getTransitions();

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
     * <li>The returned set might be an
     * {@linkplain Collections#unmodifiableSortedSet(SortedSet) unmodifiable} view
     * of the transition times, which will incorporate any subsequent changes to
     * this history. It might be a newly constructed object that does not
     * incorporate subsequent changes.</li>
     * </ul>
     * 
     * @return the transition times
     */
    public SortedSet<Duration> getTransitionTimes();

    /**
     * <p>
     * A hash code for this value history.
     * </p>
     * <p>
     * The hash code of a value history is the {@linkplain Object#hashCode() hash
     * code} of the {@linkplain #getFirstValue() first value} (or 0 if the first
     * value is null) plus the {@linkplain Map#hashCode() hash code} of the
     * {@linkplain #getTransitions() transitions}.
     * </p>
     * 
     * @return the hash code
     */
    @Override
    public int hashCode();

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

    /**
     * <p>
     * Create a stream of the transitions in the value of this history.
     * </p>
     * <ul>
     * <li>Always creates a (non null) steam.</li>
     * <li>The stream of transitions contains elements equal to the
     * {@linkplain Set#stream() stream} of {@linkplain Map#entrySet() entries} of
     * the {@linkplain #getTransitions() transitions map}.</li>
     * <li>Using the stream of transitions is more efficient than getting the
     * {@linkplain #getTransitions() transitions map} and then creating a stream
     * from its entries.</li>
     * </ul>
     * 
     * @return a stream of the transitions.
     */
    public Stream<Map.Entry<Duration, VALUE>> streamOfTransitions();

}
