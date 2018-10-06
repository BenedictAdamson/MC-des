package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Objects;

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
     * Construct an value history that is null for all points in time.
     * </p>
     */
    public ValueHistory() {
        // FIXME
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
}
