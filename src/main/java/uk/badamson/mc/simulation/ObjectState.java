package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A state of a simulated object at one point in time, just after a state
 * change, also encapsulating how to compute the state of the object at future
 * points in time, and encapsulating creation and destruction of objects.
 * </p>
 * <p>
 * The state of the object is assumed to pass through a sequence of discrete
 * states: this class is for discrete event simulations.
 * </p>
 */
@Immutable
public abstract class ObjectState {

    private final ObjectStateId id;
    private final Map<UUID, ObjectStateId> dependencies;

    /**
     * <p>
     * Construct an object state with a given ID and dependencies.
     * </p>
     */
    protected ObjectState(UUID object, Duration when, Map<UUID, ObjectStateId> dependencies) {
        id = new ObjectStateId(object, when);
        this.dependencies = Collections.unmodifiableMap(new HashMap<>(dependencies));

        // Check after copy to avoid race hazards
        for (var entry : this.dependencies.entrySet()) {
            final UUID dependencyObject = Objects.requireNonNull(entry.getKey(), "dependencyObject");
            final ObjectStateId dependency = Objects.requireNonNull(entry.getValue(), "dependency");

            if (dependencyObject != dependency.getObject()) {
                throw new IllegalArgumentException(
                        "Object ID key of the dependency map does not map to a value that has that same object ID as its depended upon object.");
            }
            if (when.compareTo(dependency.getWhen()) < 0) {
                throw new IllegalArgumentException(
                        "The time-stamp of a value in the dependency map <" + dependency.getWhen()
                                + "> is not before the time-stamp of the ID of this state<" + when + ">.");
            }
        }
    }

    /**
     * <p>
     * Compute the next state transition of the {@link {@link #getObject()} object}
     * of this object state.
     * </p>
     * <p>
     * This computation may be expensive; recomputing state transition should be
     * avoided.
     * </p>
     * <ul>
     * <li>Always has a (non null) state transition.</li>
     * <li>The {@linkplain StateTransition#getStates() states} of the state
     * transition has an entry (key) for the the
     * {@linkplain ObjectStateId#getObject() object} of the given state ID.</li>
     * <li>The {@linkplain StateTransition#getStates() states} of the state
     * transition has no null values for objects other than the
     * {@linkplain ObjectStateId#getObject() object} of the given state ID.</li>
     * <li>The {@linkplain StateTransition#getWhen() time} of the state transition
     * is after the the {@linkplain ObjectStateId#getWhen() time-stamp} of the given
     * state ID.</li>
     * <li>The given state ID is one of the {@linkplain Map#values() values} of the
     * {@linkplain StateTransition#getDependencies() dependencies} of the state
     * transition.</li>
     * </ul>
     * 
     * @param idOfThisState
     *            The ID of this state.
     * 
     * @return The next state transition.
     * @throws NullPointerException
     *             If {@code idOfThisState} is null.
     */
    public abstract StateTransition createNextStateTransition(ObjectStateId idOfThisState);

    /**
     * <p>
     * The (earlier) object state information directly used to compute this object
     * state.
     * </p>
     * <p>
     * Implicitly, if the object state computations were done differently for the
     * dependent objects, this {@link ObjectState} would be incorrect.
     * </p>
     * <ul>
     * <li>Always has a (non null) dependency map.</li>
     * <li>The dependency map does not have a null key.</li>
     * <li>The dependency map does not have null values.</li>
     * <li>The dependency map maps object IDs of objects that have a depended upon
     * state to an ID of that depended upon state.</li>
     * <li>Each object ID {@linkplain Map#keySet() key} of the dependency map maps
     * to a value that has that same object ID as the
     * {@linkplain ObjectStateId#getObject() object} of the object state ID.</li>
     * <li>The {@linkplain ObjectStateId#getWhen() time-stamp} of every
     * {@linkplain Map#values() value} in the dependency map is before the
     * {@linkplain ObjectStateId#getWhen() time-stamp} of the this state.</li>
     * </ul>
     * <p>
     * The dependencies typically have an entry for the previous state of the
     * {@linkplain ObjectStateId#getObject() object} of this state.
     * </p>
     * 
     * @return The dependency information
     */
    @Deprecated
    public final Map<UUID, ObjectStateId> getDependencies() {
        return dependencies;
    }

    /**
     * <p>
     * The IDs of the (earlier) object state information directly used to compute
     * this object state.
     * </p>
     * <p>
     * Implicitly, if the object state computations were done differently for the
     * dependent objects, this {@link ObjectState} would be incorrect.
     * </p>
     * <ul>
     * <li>Always has a (non null) non null set of depended upon states.</li>
     * <li>The set of depended upon states does not have a null element.</li>
     * <li>The {@linkplain ObjectStateId#getWhen() time-stamp} of every depended
     * upon state is before the {@linkplain #getWhen() time-stamp} of this
     * state.</li>
     * <li>An object state (ID) is one of the depended upon states if, and only if,
     * it is only of the {@linkplain Map#values() values} of the
     * {@linkplain #getDependencies() dependencies} map.</li>
     * <li>The set of depended upon states might or might not be a copy of an
     * unmodifiable internal set.</li>
     * </ul>
     * <p>
     * The depended upon states typically have an entry for the previous state of
     * the {@linkplain ObjectStateId#getObject() object} of this state.
     * </p>
     * 
     * @return The dependency information
     */
    @Deprecated
    public final Collection<ObjectStateId> getDepenendedUponStates() {
        return dependencies.values();
    }

    /**
     * <p>
     * The identifier (unique key) for this object state.
     * </p>
     * <ul>
     * <li>The {@linkplain ObjectStateId#getObject() object} of the ID is the same
     * as the{@linkplain #getObject() object} of this state.</li>
     * <li>The {@linkplain ObjectStateId#getVersion() version} of the ID is the same
     * as the{@linkplain #getVersion() version} of this state.</li>
     * <li>The {@linkplain ObjectStateId#getWhen() time-stamp} of the ID is the same
     * as the {@linkplain #getWhen() time-stamp} of this state.</li>
     * </ul>
     * 
     * @return the ID; not null.
     */
    @Deprecated
    public final ObjectStateId getId() {
        return id;
    }

    /**
     * <p>
     * The unique ID of the object for which this is a state.
     * </p>
     * 
     * @return The object ID; not null.
     */
    @Deprecated
    public final UUID getObject() {
        return id.getObject();
    }

    /**
     * <p>
     * The point in time that the {@linkplain #getObject() object} has this state.
     * </p>
     * <p>
     * Expressed as the duration since an epoch. All objects in a simulation should
     * use the same epoch.
     * </p>
     * 
     * @return the amount of time since the epoch; not null.
     */
    @Deprecated
    public final Duration getWhen() {
        return id.getWhen();
    }
}
