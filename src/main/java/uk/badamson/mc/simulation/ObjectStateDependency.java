package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * The ID of an {@linkplain ObjectState object state} depended upon by a another
 * object state.
 * </p>
 */
@Immutable
public final class ObjectStateDependency {

    private final ObjectStateId previousStateTransition;
    private final Duration when;

    /**
     * <p>
     * Construct an object with given attributes.
     * </p>
     * <ul>
     * <li>The {@linkplain #getWhen() point in time at which the depended upon
     * object had the depended upon state} is the given time-stamp.</li>
     * <li>The {@linkplain #getPreviousStateTransition() previous state transition}
     * is the given previous state transition.</li>
     * </ul>
     * 
     * @param when
     *            The point in time at which the {@linkplain #getDependedUpObject()
     *            depended upon object} had the depended upon state.
     * @param previousStateTransition
     *            The state transition (at or) before the {@linkplain #getWhen()
     *            point in time} of interest.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code when} is null.</li>
     *             <li>If {@code previousStateTransition} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If {@code when} is before the {@linkplain ObjectStateId#getWhen()
     *             time-stamp} of the {@code previousStateTransition}.
     */
    public ObjectStateDependency(Duration when, ObjectStateId previousStateTransition) {
        this.when = Objects.requireNonNull(when, "when");
        this.previousStateTransition = Objects.requireNonNull(previousStateTransition, "previousStateTransition");
        if (when.compareTo(previousStateTransition.getWhen()) < 0) {
            throw new IllegalArgumentException("when is before time-stamp of teh previous state transition");
        }
    }

    /**
     * <p>
     * The ID of the depended upon object.
     * </p>
     * <ul>
     * <li>Always have a (non null) depended upon object.</li>
     * <li>The depended upon object is the {@linkplain ObjectStateId#getObject()
     * object} of the {@linkplain #getPreviousStateTransition() previous state
     * transition}.</li>
     * </ul>
     * 
     * @return the object ID.
     */
    public final UUID getDependedUpObject() {
        return previousStateTransition.getObject();
    }

    /**
     * <p>
     * The state transition of the {@linkplain #getDependedUpObject() depended upon
     * object} (at or) before the {@linkplain #getWhen() point in time} of interest.
     * </p>
     * <ul>
     * <li>Always have a (non null) previous state transition.</li>
     * </ul>
     * 
     * @return the previous state transition.
     */
    public final ObjectStateId getPreviousStateTransition() {
        return previousStateTransition;
    }

    /**
     * <p>
     * The point in time at which the {@linkplain #getDependedUpObject() depended
     * upon object} had the depended upon state.
     * </p>
     * <ul>
     * <li>Always have a (non null) time at which the depended upon object had the
     * depended upon state.</li>
     * <li>The time at which the depended upon object had the depended upon state is
     * {@linkplain Duration#compareTo(Duration) at or after} the
     * {@linkplain ObjectStateId#getWhen() time} of the
     * {@linkplain #getPreviousStateTransition() previous state transition} of the
     * depended upon object.</li>
     * </ul>
     * 
     * @return when
     */
    public final Duration getWhen() {
        return when;
    }

}
