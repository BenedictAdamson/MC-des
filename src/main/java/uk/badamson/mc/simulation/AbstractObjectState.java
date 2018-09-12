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
 * A base class suitable for an {@link ObjectState}.
 * </p>
 */
@Immutable
public abstract class AbstractObjectState implements ObjectState {

    private final ObjectStateId id;
    private final Map<UUID, ObjectStateDependency> dependencies;

    /**
     * <p>
     * Construct an object with a given ID and dependencies.
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
    protected AbstractObjectState(ObjectStateId id, Map<UUID, ObjectStateDependency> dependencies) {
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

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractObjectState other = (AbstractObjectState) obj;
        return id.equals(other.id);
    }

    @Override
    public final Map<UUID, ObjectStateDependency> getDependencies() {
        return dependencies;
    }

    @Override
    public final ObjectStateId getId() {
        return id;
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

}
