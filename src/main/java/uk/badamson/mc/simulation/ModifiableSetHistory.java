package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

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
 */
@NotThreadSafe
public final class ModifiableSetHistory<VALUE> implements SetHistory<VALUE> {

    /**
     * <p>
     * Construct an set value history that is unknown (null) for all points in time.
     * </p>
     * <ul>
     * <li>The {@linkplain #getFirstValue() value of this history at the start of
     * time} is null.</li>
     * <li>This {@linkplain #isEmpty() is empty}.</li>
     * </ul>
     */
    public ModifiableSetHistory() {
        // Do nothinj
    }

    @Override
    public final Set<VALUE> get(Duration t) {
        Objects.requireNonNull(t, "t");
        return null;// TODO
    }

    @Override
    public final Duration getFirstTansitionTime() {
        return null;// TODO
    }

    @Override
    public final Set<VALUE> getFirstValue() {
        return null;// TODO
    }

    @Override
    public final Duration getLastTansitionTime() {
        return null;// TODO
    }

    @Override
    public final Set<VALUE> getLastValue() {
        return null;// TODO
    }

    @Override
    public final SortedSet<Duration> getTransitionTimes() {
        return Collections.emptySortedSet();// TODO
    }

    @Override
    public final boolean isEmpty() {
        return true;// TODO
    }
}
