package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * The state of an object, and any objects it creates, just after a state
 * transition of the object.
 * </p>
 */
@Immutable
public final class StateTransition {
    private final Duration when;
    private final Map<UUID, ObjectState> states;
    private final Map<UUID, ObjectStateId> dependencies;

    /**
     * <p>
     * Construct a state transition with given values.
     * </p>
     * <ul>
     * <li>The {@linkplain #getWhen() time-stamp} of this state transition is the
     * same as the given time-stamp.</li>
     * <li>The {@linkplain #getStates() states map} of this state transition is
     * {@linkplain Map#equals(Object) equal to} the given states map.</li>
     * <li>The {@linkplain #getDependencies() dependencies map} of this state
     * transition is {@linkplain Map#equals(Object) equal to} the given dependencies
     * map.</li>
     * </ul>
     * 
     * @param when
     *            The point in time that this state transition occurs.
     * @param states
     *            The states of an object, and the states of any objects it creates
     *            at the state transition, just after this state transition.
     * @param dependencies
     *            The (earlier) object state information directly used to compute
     *            the object {@linkplain #getStates() states}.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code when} is null.</li>
     *             <li>If {@code states} is null.</li>
     *             <li>If {@code dependencies} is null.</li>
     *             <li>If a {@linkplain Map#keySet() key} of {@code states} is
     *             null.</li>
     *             <li>If a {@linkplain Map#keySet() key} of {@code dependencies} is
     *             null.</li>
     *             <li>If a {@linkplain Map#values() value} of {@code dependencies}
     *             is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If {@code states} has more than one null value.
     */
    public StateTransition(Duration when, Map<UUID, ObjectState> states, Map<UUID, ObjectStateId> dependencies) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(dependencies, "dependencies");

        this.when = Objects.requireNonNull(when, "when");
        this.states = Collections.unmodifiableMap(new HashMap<>(states));
        this.dependencies = Collections.unmodifiableMap(new HashMap<>(dependencies));

        boolean hasNullState = false;
        for (var entry : this.states.entrySet()) {
            final UUID object = entry.getKey();
            final ObjectState state = entry.getValue();
            Objects.requireNonNull(object, "states key");
            if (hasNullState && state == null) {
                throw new IllegalArgumentException("Too many null states " + object);
            }
            hasNullState = hasNullState || (state == null);
        }
        for (var entry : this.dependencies.entrySet()) {
            final UUID object = entry.getKey();
            final ObjectStateId dependency = entry.getValue();
            Objects.requireNonNull(object, "dependencies key");
            Objects.requireNonNull(dependency, "dependencies value");
        }
    }

    /**
     * <p>
     * The (earlier) object state information directly used to compute the object
     * {@linkplain #getStates() states}.
     * </p>
     * <p>
     * Implicitly, if the object state computations were done differently for the
     * dependent objects, this collection of next states would be incorrect.
     * </p>
     * <ul>
     * <li>Always has a (non null) dependencies map.</li>
     * <li>The dependencies map does not have a null key.</li>
     * <li>The dependencies map does not have null values.</li>
     * </ul>
     * 
     * @return The dependency information
     */
    public final Map<UUID, ObjectStateId> getDependencies() {
        return dependencies;
    }

    /**
     * <p>
     * The states of an object, and the states of any objects it creates at the
     * state transition, just after this state transition.
     * </p>
     * <ul>
     * <li>Always has a (non null) map of object states.</li>
     * <li>The map of object states may be unmodifiable.</li>
     * <li>The map of object states does not have a null key.</li>
     * <li>The map has at most one null values.</li>
     * </ul>
     * 
     * @return A mapping of object IDs to the states of those objects just after
     *         this state transition. This will usually be a singleton map. A null
     *         value indicates that the object no longer exists just after the state
     *         transition. A map with more than one entry indicates that the state
     *         transition has caused the creation of additional objects.
     */
    public final Map<UUID, ObjectState> getStates() {
        return states;
    }

    /**
     * <p>
     * The point in time that this state transition occurs.
     * </p>
     * <p>
     * Expressed as the duration since an epoch. All objects in a simulation should
     * use the same epoch.
     * </p>
     * 
     * @return the amount of time since the epoch; not null.
     */
    public final Duration getWhen() {
        return when;
    }

}// class