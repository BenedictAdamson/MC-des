package uk.badamson.mc.simulation;

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
 *            The class of values of this set history. This must be an
 *            {@link Immutable immutable} type.
 * 
 * @see ValueHistory
 */
@NotThreadSafe
public interface SetHistory<VALUE> extends ValueHistory<Set<VALUE>> {
}
