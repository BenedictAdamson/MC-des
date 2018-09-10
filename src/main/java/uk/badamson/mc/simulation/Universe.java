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

    private final Map<ObjectStateId, ObjectState> objectStates = new HashMap<>();
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
     * <li>The {@linkplain #getObjectStates() map of IDs to object states}
     * {@linkplain Map#isEmpty() is empty}.</li>
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
     */
    public final void append(ObjectState objectState) throws InvalidEventTimeStampOrderException {
        Objects.requireNonNull(objectState, "objectState");

        final ObjectStateId id = objectState.getId();
        final UUID object = id.getObject();
        final Duration when = id.getWhen();

        SortedMap<Duration, ObjectState> stateHistory = objectStateHistories.get(object);
        if (stateHistory == null) {
            stateHistory = new TreeMap<>();
            objectStateHistories.put(object, stateHistory);
        } else if (0 <= stateHistory.lastKey().compareTo(when)) {
            throw new InvalidEventTimeStampOrderException();
        }
        stateHistory.put(when, objectState);
        objectStates.put(id, objectState);
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
    public synchronized final Duration getEarliestCompleteState() {
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
     * <li>The set of object IDs might or might not be a copy of an internal map,and
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
     * <li>All non null object states in the state history of a given object belong
     * to the {@linkplain Map#values() values collection} of the
     * {@linkplain #getObjectStates() object states map}.</li>
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
     * All the states of objects within this universe, indexed by their
     * {@linkplain ObjectState#getId() IDs}.
     * </p>
     * <ul>
     * <li>Always have a (non null) map of IDs to object states.</li>
     * <li>The map of IDs to object states does not have a null key.</li>
     * <li>The map of IDs to object states does not have IDs (keys) for
     * {@linkplain ObjectStateId#getObject() object IDs} that are in the
     * {@linkplain #getObjectIds() set of objects in this universe}.</li>
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
        return new HashMap<>(objectStates);
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
