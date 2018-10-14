package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Set;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

/**
 * <p>
 * The time-wise variation of a {@linkplain Set set} of value that changes at
 * discrete points in time.
 * </p>
 * 
 * @param VALUE
 *            The class of values of this set history. This must be
 *            {@link Immutable immutable}, or have reference semantics.
 * 
 * @see ValueHistory
 */
@NotThreadSafe
public interface SetHistory<VALUE> extends ValueHistory<Set<VALUE>> {

    /**
     * <p>
     * The history of whether the {@linkplain #get(Duration) value} of this set
     * history {@linkplain Set#contains(Object) contains} a given value.
     * </p>
     * <ul>
     * <li>Always have a (non null) containment history for a value.</li>
     * <li>The containment history for a value has a (non null)
     * {@linkplain ValueHistory#get(Duration) value} for all points in time.</li>
     * <li>The containment history for a value indicates that the value is
     * {@linkplain Boolean#FALSE not} contained at the
     * {@linkplain ValueHistory#getFirstValue() start of time}.</li>
     * <li>The containment history for a value indicates that the value
     * {@linkplain Boolean#TRUE is} present {@linkplain ValueHistory#get(Duration)
     * for a point in time} if, and only if, that value is
     * {@linkplain Set#contains(Object) contained in} the set for which this is a
     * history {@linkplain #get(Duration) at that point in time}.</li>
     * <li>The {@linkplain ValueHistory#getTransitionTimes() transition times} of
     * the containment history of a value is a
     * {@linkplain Set#containsAll(java.util.Collection) sub set} of the
     * {@linkplain #getTransitionTimes() transition times} of this history.</li>
     * </ul>
     * 
     * @param value
     *            The value of interest.
     * @return The containment history of the given value.
     * 
     * @see Set#contains(Object)
     */
    public ValueHistory<Boolean> contains(VALUE value);
}
