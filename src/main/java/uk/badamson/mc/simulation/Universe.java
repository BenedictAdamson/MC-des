package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * <p>
 * A collection of simulated objects and their {@linkplain ObjectState state}
 * histories.
 * </p>
 */
public class Universe {

    /**
     * <p>
     * An exception class for indicating that an {@link ObjectState} can not be
     * {@linkplain Universe#append(ObjectState) appended} because its time-stamp is
     * not after the current {@linkplain SortedMap#lastKey() last}
     * {@link ObjectState} of the object.
     * </p>
     */
    public static final class InvalidEventTimeStampOrderException extends IllegalStateException {

        private static final String MESSAGE = "ObjectState time-stamp not after teh current last time-stamp";
        private static final long serialVersionUID = 1L;

        public InvalidEventTimeStampOrderException() {
            super(MESSAGE);
        }

        /**
         * <p>
         * Construct an exception for indicating that an {@link ObjectState} can not be
         * {@linkplain Universe#append(ObjectState) appended} because its time-stamp is
         * not after the current {@linkplain SortedMap#lastKey() last}
         * {@link ObjectState} of the object, which has been signalled by an underlying
         * cause.
         * </p>
         * 
         * @param cause
         *            The cause of this exception.
         */
        public InvalidEventTimeStampOrderException(Throwable cause) {
            super(MESSAGE, cause);
        }

    }// class

    private static final class ObjectStateData {
        ObjectState state;

        private ObjectStateData(ObjectState state) {
            this.state = state;
        }

    }// class

    private final Map<ObjectStateId, ObjectStateData> objectStates = new HashMap<>();
    private final Map<UUID, SortedMap<Duration, ObjectState>> objectStateHistories = new HashMap<>();

    private Duration earliestCompleteState;

    /**
     * <p>
     * Construct an empty universe.
     * </p>
     * <ul>
     * <li>The {@linkplain #getEarliestCompleteState() earliest complete state}
     * time-stamp of this universe is the given earliest complete state
     * time-stamp.</li>
     * <li>The {@linkplain #getObjectIds() set of object IDs}
     * {@linkplain Set#isEmpty() is empty}.</li>
     * <li>The {@linkplain #getObjectStateIds() set of IDs of object states}
     * {@linkplain Set#isEmpty() is empty}.</li>
     * </ul>
     * 
     * @param earliestCompleteState
     *            The earliest point in time for which this universe has a known
     *            {@linkplain ObjectState state} for {@linkplain #getObjectIds() all
     *            the objects} in the universe.
     * @throws NullPointerException
     *             If {@code earliestCompleteState} is null
     */
    public Universe(final Duration earliestCompleteState) {
        this.earliestCompleteState = Objects.requireNonNull(earliestCompleteState, "earliestCompleteState");
    }

    /**
     * <p>
     * Add an object state to this universe.
     * <p>
     * <ul>
     * <li>The {@linkplain ObjectStateId#getObject() object ID} of the
     * {@linkplain ObjectState#getId() ID} of the given object state is one of the
     * {@linkplain #getObjectIds() object IDs} of this universe.</li>
     * <li>The given object state is the {@linkplain SortedMap#lastKey() last} value
     * in the {@linkplain #getObjectStateHistory(UUID) object state history} of the
     * {@linkplain ObjectStateId#getObject() object} of the
     * {@linkplain ObjectState#getId() ID} of the given object state.</li>
     * <li>The {@linkplain #getObjectStateHistory(UUID) object state histories} of
     * other objects are unchanged.</li>
     * </ul>
     * 
     * @param objectState
     *            The state to add.
     * @throws NullPointerException
     *             If {@code objectState} is null
     * @throws InvalidEventTimeStampOrderException
     *             If the {@linkplain ObjectStateId#getObject() object} of the
     *             {@linkplain ObjectState#getId() ID} of {@code objectState} is
     *             already an {@linkplain #getObjectIds() object IDs} of this
     *             universe, but the {@linkplain ObjectStateId#getWhen() time-stamp}
     *             of (the ID of) {@code objectState} is not
     *             {@linkplain Duration#compareTo(Duration) after} the
     *             {@linkplain SortedMap#lastKey() last} state in the
     *             {@linkplain #getObjectStateHistory(UUID) object state history}.
     *             In this case, this {@link Universe} is unchanged.
     * @throws IllegalStateException
     *             If any {@linkplain ObjectState#getDependencies() dependencies} of
     *             the {@code objectState} are at or after the
     *             {@linkplain #getEarliestCompleteState() earliest complete state}
     *             and are not {@linkplain #getObjectStates() known object states}.
     */
    public final void append(ObjectState objectState) throws InvalidEventTimeStampOrderException {
        Objects.requireNonNull(objectState, "objectState");

        final ObjectStateId id = objectState.getId();
        final UUID object = id.getObject();
        final Duration when = id.getWhen();

        for (var dependency : objectState.getDependencies().values()) {
            if (0 <= dependency.getWhen().compareTo(earliestCompleteState)
                    && !objectStates.containsKey(dependency.getPreviousStateTransition())) {
                throw new IllegalStateException("Unknown state for dependency " + dependency);
            }
        }
        SortedMap<Duration, ObjectState> stateHistory = objectStateHistories.get(object);
        if (stateHistory == null) {
            stateHistory = new TreeMap<>();
            objectStateHistories.put(object, stateHistory);
        } else if (0 <= stateHistory.lastKey().compareTo(when)) {
            throw new InvalidEventTimeStampOrderException();
        }
        stateHistory.put(when, objectState);
        final ObjectStateData stateData = new ObjectStateData(objectState);
        objectStates.put(id, stateData);
    }

    /**
     * <p>
     * The earliest point in time for which this universe has a known
     * {@linkplain ObjectState state} for {@linkplain #getObjectIds() all the
     * objects} in the universe.
     * </p>
     * <ul>
     * <li>Always have a (non null) earliest complete state time-stamp.</li>
     * </ul>
     * 
     * @return the point in time, expressed as the duration since an epoch; not
     *         null.
     */
    public final Duration getEarliestCompleteState() {
        return earliestCompleteState;
    }

    /**
     * <p>
     * The unique IDs of the simulated objects in this universe.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of object IDs.</li>
     * <li>The set of object IDs does not have a null element.</li>
     * <li>The set of object IDs may be immutable.</li>
     * <li>The set of object IDs might or might not be a copy of an internal set,and
     * so might or might not reflect subsequent changes to this universe.</li>
     * </ul>
     * 
     * @return the object IDs.
     */
    public final Set<UUID> getObjectIds() {
        return new HashSet<>(objectStateHistories.keySet());
    }

    /**
     * <p>
     * The state of an object within this universe, given its
     * {@linkplain ObjectState#getId() ID}.
     * </p>
     * <ul>
     * <li>Have a (non null) object state if, and only if, the given object state ID
     * is one of the {@linkplain #getObjectStateIds() known object state IDs} of
     * this universe.</li>
     * <li>A (non null) object state accessed using a given object state ID has an
     * {@linkplain ObjectStateId#equals(Object) equivalent} object state ID as its
     * {@linkplain ObjectState#getId() ID}.</li>
     * <li>All the {@linkplain ObjectState#getDependencies() dependencies} of the
     * object states either have a {@linkplain ObjectStateId#getWhen() time-stamp}
     * before the {@linkplain #getEarliestCompleteState() earliest complete state}
     * time-stamp of the universe, or are themselves {@linkplain #getObjectStates()
     * known object states}.</li>
     * </ul>
     * 
     * @param objectStateId
     *            The ID of the object state of interest.
     * @return the object state
     * @throws NullPointerException
     *             If {@code objectStateId} is null.
     */
    public final ObjectState getObjectState(ObjectStateId objectStateId) {
        Objects.requireNonNull(objectStateId, "objectStateId");
        final ObjectStateData objectStateData = objectStates.get(objectStateId);
        if (objectStateData == null) {
            return null;
        } else {
            return objectStateData.state;
        }
    }

    /**
     * <p>
     * Get the state of a given object at a given point in time.
     * </p>
     * <ul>
     * <li>Unknown {@linkplain #getObjectIds() objects} have an unknown (null) state
     * for all points in time.</li>
     * <li>Known {@linkplain #getObjectIds() objects} have an unknown (null) state
     * for all points before the {@linkplain SortedMap#firstKey() first} known state
     * of the {@linkplain #getObjectStateHistory(UUID) state history} of that
     * object.</li>
     * <li>Returns a (non null) state if the object has a known state at the given
     * point in time.</li>
     * <li>The (non null) state of an object at a given point in time is one of the
     * states ({@linkplain SortedMap#values() values}) in the
     * {@linkplain #getObjectStateHistory(UUID) state history} of the object.</li>
     * <li>The (non null) state of an object at a given point in time is the state
     * it had at the latest state transition at or before that point in time.</li>
     * </ul>
     * 
     * @param object
     *            The ID of the object of interest.
     * @param when
     *            The point in time of interest.
     * @return The state of the given object at the given point in time.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             </ul>
     */
    public final ObjectState getObjectState(UUID object, Duration when) {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(when, "when");

        final SortedMap<Duration, ObjectState> objectStateHistory = objectStateHistories.get(object);
        if (objectStateHistory == null) {// unknown object
            return null;
        }
        final SortedMap<Duration, ObjectState> eventsAtOrBefore = objectStateHistory.headMap(when.plusNanos(1L));
        if (eventsAtOrBefore.isEmpty()) {// before first event
            return null;
        }
        return eventsAtOrBefore.get(eventsAtOrBefore.lastKey());
    }

    /**
     * <p>
     * The (currently known) history of the state transitions (succession of object
     * states) of a given object in this universe.
     * </p>
     * <ul>
     * <li>A universe has a (non null) object state history for a given object if,
     * and only if, that object is one of the {@linkplain #getObjectIds() objects}
     * in the universe.</li>
     * <li>A (non null) object state history for a given object is not
     * {@linkplain Map#isEmpty() empty}.</li>
     * <li>Only the {@linkplain SortedMap#lastKey() last} entry in a (non null)
     * object state history may map to a null state (indicating that the object
     * ceased to exist at that time).</li>
     * <li>A (non null) object state history for a given object maps to a null value
     * or an object state with a {@linkplain ObjectStateId#getWhen() time-stamp}
     * equal to the key of the map.</li>
     * <li>All non null object states in the state history of a given object have
     * the given object ID as the {@linkplain ObjectStateId#getObject() object ID}
     * of the {@linkplain ObjectState#getId() state ID}.</li>
     * <li>All non null object states in the state history of a given object have an
     * ID that belongs to the {@linkplain #getObjectIds() set of all known object
     * IDs}.</li>
     * <li>All non null object states in the state history of a given object, except
     * for the {@linkplain SortedMap#firstKey() first}, have as a
     * {@linkplain ObjectState#getDependencies() dependency} on the previous object
     * state in the state history.</li>
     * </ul>
     * 
     * @param object
     *            The object of interest
     * @return The state history of the given object, as a map of the times of a
     *         state transition to the state just after that transition, or null if
     *         this universe does not {@linkplain #getObjectIds() include} the given
     *         object. A null value indicates that the object ceased to exist at
     *         that time.
     * @throws NullPointerException
     *             If {@code object} is null
     */
    public final SortedMap<Duration, ObjectState> getObjectStateHistory(UUID object) {
        Objects.requireNonNull(object, "object");
        SortedMap<Duration, ObjectState> objectStateHistory = objectStateHistories.get(object);
        if (objectStateHistory == null) {
            return null;
        } else {
            return new TreeMap<>(objectStateHistory);
        }
    }

    /**
     * <p>
     * All the known {@linkplain ObjectStateId identifiers} of states of objects
     * within this universe.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of object state IDs.</li>
     * <li>The set of IDs of object states does not have a null element.</li>
     * <li>The set of IDs of object states does not have elements for
     * {@linkplain ObjectStateId#getObject() object IDs} that are not in the
     * {@linkplain #getObjectIds() set of objects in this universe}.</li>
     * <li>The set of IDs of object states may be immutable, or it may be a copy of
     * an underlying collection.</li>
     * </ul>
     * 
     * @return the IDs
     */
    public final Set<ObjectStateId> getObjectStateIds() {
        return new HashSet<>(objectStates.keySet());
    }

    /**
     * <p>
     * The time-stamp of the {@linkplain SortedMap#firstKey() first}
     * {@linkplain #getObjectStateHistory(UUID) event} of an object.
     * </p>
     * <ul>
     * <li>An object has a (non null) first state time-stamp if, and only if, it is
     * a {@linkplain #getObjectIds() known object}.</li>
     * <li>If an object is known, its first state time-stamp is the
     * {@linkplain SortedMap#firstKey() first key} of the
     * {@linkplain #getObjectStateHistory(UUID) state history} of that object.</li>
     * </ul>
     * 
     * @param object
     *            The ID of the object of interest.
     * @return The time-stamp of the first event of the object with {@code object}
     *         as its ID, or null if {@code object} is not a
     *         {@linkplain #getObjectIds() known object ID}.
     */
    public final Duration getWhenFirstState(UUID object) {
        Objects.requireNonNull(object, "object");
        final SortedMap<Duration, ObjectState> objectStateHistory = objectStateHistories.get(object);
        if (objectStateHistory == null) {
            return null;
        } else {
            return objectStateHistory.firstKey();
        }
    }

}
