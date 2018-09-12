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
    private final Map<UUID, ObjectStateDependency> dependencies;

    /**
     * <p>
     * Construct an object state with a given ID and dependencies.
     * </p>
     * <ul>
     * <li>The {@linkplain #getId() ID} of this state is the given ID.</li>
     * <li>The {@linkplain #getDependencies() dependencies} of this state are equal
     * to the given dependencies.</li>
     * </ul>
     * 
     * @param id
     *            The identifier (unique key) for this object state.
     * @param dependencies
     *            The (earlier) object states directly used to compute this object
     *            state.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code id} is null.</li>
     *             <li>If {@code dependencies} is null.</li>
     *             <li>If {@code dependencies} has a null key.</li>
     *             <li>If {@code dependencies} has a null value.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If any object ID {@linkplain Map#keySet() key }of the
     *             {@code dependencies} map maps to a {@linkplain Map#values()
     *             value} that does not have that same object ID as its
     *             {@linkplain ObjectStateDependency#getDependedUpObject() depended
     *             upon object}.</li>
     *             <li>If the time-stamp of a value in the dependency map is not
     *             before the time-stamp of the ID of this state.</li>
     *             </ul>
     */
    protected ObjectState(ObjectStateId id, Map<UUID, ObjectStateDependency> dependencies) {
        this.id = Objects.requireNonNull(id, "id");
        this.dependencies = Collections.unmodifiableMap(new HashMap<>(dependencies));

        final Duration when = id.getWhen();
        // Check after copy to avoid race hazards
        for (var entry : this.dependencies.entrySet()) {
            final UUID dependencyObject = Objects.requireNonNull(entry.getKey(), "dependencyObject");
            final ObjectStateDependency dependency = Objects.requireNonNull(entry.getValue(), "dependency");

            if (dependencyObject != dependency.getDependedUpObject()) {
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
     * Compute the state of the {@link {@link #getObject()} object}, and any objects
     * it creates, just after the {@link #getDurationUntilNextEvent next state
     * change of the object}.
     * </p>
     * <p>
     * This computation may be expensive; recomputing future states should be
     * avoided.
     * </p>
     * <ul>
     * <li>Always has a (non null) map of object states.</li>
     * <li>The map of object states may be unmodifiable.</li>
     * <li>The map of object states does not have a null key.</li>
     * <li>The map of object states has an entry (key) for the
     * {@link ObjectStateId#getObject() object ID} of the {@linkplain #getId() ID}
     * of this state.</li>
     * <li>The map has no null values for objects other than the
     * {@link ObjectStateId.Id#getObject() object ID} of the of the
     * {@linkplain #getId() ID} of this state.</li>
     * <li>Non null next state values have the {@link ObjectStateId#getObject()
     * object ID} of their {@linkplain #getId() ID} equal to their key.</li>
     * <li>All the values (states) in the next states map are
     * {@link Duration#equals(Object) equal} {@link #getWhen() points in time}.</li>
     * <li>All the values (states) in the next states map are for a
     * {@link #getWhen() point in time} after the point in time of this state.</li>
     * <li>All the values (states) in the next states map have the
     * {@linkplain #getId() ID} of this state as one of their
     * {@linkplain #getDependencies() dependencies}.</li>
     * </ul>
     * 
     * @return A mapping of object IDs to the states of those objects at the future
     *         point in time. This will usually be a singleton map, containing an
     *         entry for the @link {@link #getObject()} object} for which this is a
     *         state. A null value for the object for which this is a state
     *         indicates that the object no longer exists at the future point in
     *         time. A map with more than one entry indicates that the object has
     *         caused the creation of additional objects.
     */
    public abstract Map<UUID, ObjectState> createNextStates();

    /**
     * <p>
     * Whether this {@link ObjectState} is equivalent to another object.
     * </p>
     * <p>
     * The semantics of an {@link ObjectState} is that the its {@linkplain #getId()
     * ID} attribute is a unique ID. Therefore two {@link ObjectState} objects are
     * equivalent if, and only if, they have equals IDs.
     * </p>
     * 
     * @param that
     *            The object to compare with this object.
     * @return whether this object is equivalent to {@code that} object.
     */
    @Override
    public final boolean equals(Object that) {
        if (this == that)
            return true;
        if (that == null)
            return false;
        if (getClass() != that.getClass())
            return false;
        ObjectState other = (ObjectState) that;
        return id.equals(other.id);
    }

    /**
     * <p>
     * The (earlier) object states directly used to compute this object state.
     * </p>
     * <p>
     * Implicitly, if the object state computations were done differently for the
     * dependent objects, this {@link ObjectState} would be incorrect.
     * </p>
     * <ul>
     * <li>Always has a (non null) dependency map.</li>
     * <li>The dependency map does not have a null key.</li>
     * <li>The dependency map does not have null values.</li>
     * <li>The dependency map maps object IDs if objects that have a dependened upon
     * state to an ID of that depended upon state.</li>
     * <li>Each object ID {@linkplain Map#keySet() key} of the dependency map maps
     * to a value that has that same object ID as its
     * {@linkplain ObjectStateDependency#getDependedUpObject() depended upon
     * object}.</li>
     * <li>The {@linkplain ObjectStateDependency#getWhen() time-stamp} of every
     * {@linkplain Map#values() value} in the dependency map is before the
     * {@linkplain ObjectStateId#getWhen() time-stamp} of the {@linkplain #getId()
     * ID} of this state.</li>
     * </ul>
     * <p>
     * The dependencies typically have an entry for the previous state of the
     * {@linkplain ObjectStateId#getObject() object} of the {@linkplain #getId() ID}
     * of this state.
     * </p>
     * 
     * @return The set of the IDs of the {@link ObjectState} states that this state
     *         directly depends on.
     */
    public final Map<UUID, ObjectStateDependency> getDependencies() {
        return dependencies;
    }

    /**
     * <p>
     * The identifier (unique key) for this object state.
     * </p>
     * 
     * @return the ID; not null.
     */
    public final ObjectStateId getId() {
        return id;
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }
}
