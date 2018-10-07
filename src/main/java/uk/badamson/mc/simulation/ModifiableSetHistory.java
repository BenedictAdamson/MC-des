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
 * 
 * @see ModifiableValueHistory
 */
@NotThreadSafe
public final class ModifiableSetHistory<VALUE> implements SetHistory<VALUE> {

    private static final ValueHistory<Boolean> ABSENT = new ModifiableValueHistory<>(Boolean.FALSE);

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
        return ABSENT;
    }

    @Override
    public final Set<VALUE> get(Duration t) {
        Objects.requireNonNull(t, "t");
        return Collections.emptySet();// TODO
    }

    @Override
    public final Duration getFirstTansitionTime() {
        return null;// TODO
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
        return Collections.emptySet();
    }

    @Override
    public final Duration getLastTansitionTime() {
        return null;// TODO
    }

    @Override
    public final Set<VALUE> getLastValue() {
        return Collections.emptySet();// TODO
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
