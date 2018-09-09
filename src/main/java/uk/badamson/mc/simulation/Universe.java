package uk.badamson.mc.simulation;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * A collection of simulated objects and their {@linkplain ObjectState state}
 * histories.
 * </p>
 */
public class Universe {

    private final Map<ObjectStateId, ObjectState> objectStates = new HashMap<>();

    /**
     * <p>
     * Construct an empty universe.
     * </p>
     * <ul>
     * <li>The map of IDs to object states {@linkplain Map#isEmpty() is empty}.</li>
     * </ul>
     */
    public Universe() {
        // Do nothing
    }

    /**
     * <p>
     * All the states of objects within this universe, indexed by their
     * {@linkplain ObjectState#getId() IDs}.
     * </p>
     * <ul>
     * <li>Always have a (non null) map of IDs to object states.</li>
     * <li>The map of IDs to object states does not have a null key.</li>
     * <li>The map of IDs to object states does not have null values.</li>
     * <li>The map of IDs to object states maps an ID to an object state that has
     * the same {@linkplain ObjectState#getId() ID}.</li>
     * <li>The map of IDs to object states may be immutable, or it may be a copy of
     * an underlying collection.</li>
     * </ul>
     * 
     * @return the objectStates
     */
    public final Map<ObjectStateId, ObjectState> getObjectStates() {
        return objectStates;
    }

}
