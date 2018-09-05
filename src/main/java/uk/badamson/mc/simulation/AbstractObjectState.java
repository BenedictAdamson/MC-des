package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
    private final Set<ObjectStateId> dependencies;

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
     *             <li>If {@code dependencies} has a null entry.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code dependencies} has entries with duplicate
     *             {@linkplain ObjectStateId#getObject() object IDs}.</li>
     *             <li>If {@code dependencies} has
     *             {@linkplain ObjectStateId#getWhen() time-stamps} at or after the
     *             time-stamp of {@code id}.</li>
     *             </ul>
     */
    protected AbstractObjectState(ObjectStateId id, Set<ObjectStateId> dependencies) {
        this.id = Objects.requireNonNull(id, "id");
        this.dependencies = Collections.unmodifiableSet(new HashSet<>(dependencies));

        final Duration when = id.getWhen();
        final Set<UUID> dependentObjects = new HashSet<>(dependencies.size());
        // Check after copy to avoid race hazards
        for (ObjectStateId dependency : this.dependencies) {
            Objects.requireNonNull(dependency, "dependency");
            final Duration dependencyWhen = dependency.getWhen();
            final UUID dependencyObject = dependency.getObject();
            if (when.compareTo(dependencyWhen) <= 0) {
                throw new IllegalArgumentException("dependency has time-stamp <" + dependencyWhen
                        + "> at or after the time-stamp of id <" + when + ">");
            }
            if (dependentObjects.contains(dependencyObject)) {
                throw new IllegalArgumentException("dependencies has duplicate object ID <" + dependencyObject + ">");
            }
            dependentObjects.add(dependencyObject);
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
    public final Set<ObjectStateId> getDependencies() {
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
