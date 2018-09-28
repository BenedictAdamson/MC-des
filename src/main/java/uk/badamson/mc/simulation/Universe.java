package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
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
 * <p>
 * This collection enforces constraints that ensure that the object state
 * histories are <dfn>consistent</dfn>. Consistency means that if a universe
 * contains an {@linkplain #getStateTransition(ObjectStateId) object state}, it
 * also contains all the {@linkplain ObjectState#getDepenendedUponStates()
 * depended upon states} of that state, unless those states are
 * {@linkplain #getEarliestTimeOfCompleteState() too old}.
 * </p>
 */
public class Universe {

    /**
     * <p>
     * An exception class for indicating that an {@link ObjectState} can not be
     * {@linkplain Universe#append(ObjectState) appended} to a {@link Universe}
     * because its time-stamp is not after the current
     * {@linkplain SortedMap#lastKey() last} {@link ObjectState} of the object.
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
         * {@linkplain Universe#append(ObjectState) appended} to a {@link Universe}
         * because its time-stamp is not after the current
         * {@linkplain SortedMap#lastKey() last} {@link ObjectState} of the object,
         * which has been signalled by an underlying cause.
         * </p>
         * 
         * @param cause
         *            The cause of this exception.
         */
        public InvalidEventTimeStampOrderException(Throwable cause) {
            super(MESSAGE, cause);
        }

    }// class

    /**
     * <p>
     * An exception class for indicating that an {@link ObjectState} can not be
     * {@linkplain Universe#append(ObjectState) appended} to a {@link Universe}
     * because it has a {@linkplain ObjectState#getDepenendedUponStates() depended
     * upon state} that is at or after
     * {@linkplain Universe#getEarliestTimeOfCompleteState() earliest time of a
     * complete state} (so the depended upon state should be present) but that
     * depended upon state is not one of the
     * {@linkplain Universe#getStateTransitionIds() known object state}.
     * </p>
     */
    public static final class MissingDependedUponStateException extends IllegalStateException {

        private static final String MESSAGE = "Missing depended upon state";
        private static final long serialVersionUID = 1L;

        public MissingDependedUponStateException() {
            super(MESSAGE);
        }

        /**
         * <p>
         * Construct an exception for indicating that an {@link ObjectState} can not be
         * {@linkplain Universe#append(ObjectState) appended} to a {@link Universe}
         * because its time-stamp is not after the current
         * {@linkplain SortedMap#lastKey() last} {@link ObjectState} of the object,
         * which has been signalled by an underlying cause.
         * </p>
         * 
         * @param cause
         *            The cause of this exception.
         */
        public MissingDependedUponStateException(Throwable cause) {
            super(MESSAGE, cause);
        }

    }// class

    private static final class ObjectStateData {
        final ObjectState state;
        final Set<ObjectStateId> dependentStateTransitions = new HashSet<>();

        private ObjectStateData(ObjectState state) {
            this.state = state;
        }

    }// class

    /**
     * <p>
     * A transaction for changing the state of a {@link Universe}.
     * </p>
     * <p>
     * That is, a record of a set of reads and up to one write to the state
     * histories of the Universe that can be committed as an atomic operation.
     * </p>
     */
    public final class Transaction implements AutoCloseable {

        private final Map<ObjectStateId, ObjectState> objectStatesRead = new HashMap<>();
        private final Map<UUID, ObjectStateId> dependencies = new HashMap<>();

        private Transaction() {
            // Do nothing
        }

        /**
         * <p>
         * Indicate that this transaction has ended.
         * </p>
         */
        @Override
        public final void close() {
            // Do nothing
        }

        /**
         * <p>
         * Get the state of a given object, of the {@linkplain #getUniverse() universe}
         * of this transaction, at a given point in time.
         * </p>
         * <ul>
         * <li>The object state of for an object ID and point in time is either the same
         * object state as can be {@linkplain Universe#getObjectState(UUID, Duration)
         * got} from the {@linkplain #getUniverse() universe} of this transaction, or is
         * the same object state as has already been {@linkplain #getObjectStatesRead()
         * read} by this transaction.</li>
         * <li>The object state of for an object ID and point in time that has not
         * already been {@linkplain #getObjectStatesRead() read} by this transaction is
         * the same object state as can be
         * {@linkplain Universe#getObjectState(UUID, Duration) got} from the
         * {@linkplain #getUniverse() universe} of this transaction.</li>
         * <li>The object state of for an object ID and point in time that has already
         * been {@linkplain #getObjectStatesRead() read} by this transaction is the same
         * object state as was read previously.</li>
         * <li>The method records the returned state as one of the
         * {@linkplain #getObjectStatesRead() read states}. Hence this method is not a
         * simple getter.</li>
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
        public final ObjectState fetchObjectState(UUID object, Duration when) {
            final ObjectStateId id = new ObjectStateId(object, when);
            final ObjectState objectState = Universe.this.getObjectState(object, when);
            objectStatesRead.put(id, objectState);
            dependencies.put(id.getObject(), id);// FIXME
            return objectState;
        }

        /**
         * <p>
         * The {@linkplain #getObjectStatesRead() object states read} by this
         * transaction, expressed as the implied
         * {@linkplain ObjectState#getDependencies() dependencies} of a
         * {@linkplain ObjectState object state} computed from those object states read.
         * </p>
         * <ul>
         * <li>Always has a (non null) dependency map.</li>
         * <li>The dependency map does not have a null key.</li>
         * <li>The dependency map does not have null values.</li>
         * <li>Each object ID {@linkplain Map#keySet() key} of the dependency map maps
         * to a value that has that same object ID as the
         * {@linkplain ObjectStateId#getObject() object} of the object state ID.</li>
         * <li>Each {@linkplain ObjectStateId#getObject() object} of an
         * {@linkplain #getObjectStatesRead() object state read} has an entry in the
         * dependencies map.</li>
         * <li>The {@linkplain Map#values() collection of values} of the dependencies
         * map is a {@linkplain Set#containsAll(Collection) sub set} of the
         * {@linkplain Map#keySet() keys} of the {@linkplain #getObjectStatesRead()
         * object states read}.</li>
         * <li>The {@linkplain ObjectStateId#getWhen() time-stamp} of each
         * {@linkplain Map#keySet() object state ID key} of the
         * {@linkplain #getObjectStatesRead() object states read} map is at or after the
         * time-stamp of the {@linkplain Map#get(Object) value} in the dependencies map
         * for the {@linkplain ObjectStateId#getObject() object ID} of that object state
         * ID.</li>
         * </ul>
         * 
         * @return The dependency information
         */
        public final Map<UUID, ObjectStateId> getDependencies() {
            return dependencies;
        }

        /**
         * <p>
         * The object states that have been read as part of this transaction.
         * </p>
         * <ul>
         * <li>Always have a (non null) map of object states read.</li>
         * <li>The map of object states read may be unmodifiable or a copy of internal
         * state.</li>
         * <li>The map of object states read does not
         * {@linkplain Map#containsKey(Object) have} a null key.</li>
         * <li>The map of object states read maps the object and time of interest
         * (together represented by an {@link ObjectStateId}) to the
         * {@linkplain ObjectState object state} for that object just after the latest
         * state transition at or before that point in time.</li>
         * <li>A key of the object states read map {@linkplain Map#get(Object) mapping}
         * to a null value indicates that the {@linkplain ObjectStateId#getObject()
         * object} of the key did not exist at the {@linkplain ObjectStateId#getWhen()
         * point in time} of the key.</li>
         * <li>Each key of the object states read map {@linkplain Map#get(Object) maps
         * to} a null value or an object state with an
         * {@linkplain ObjectState#getObject() object ID} equal to the
         * {@linkplain ObjectStateId#getObject() object ID} of the key.</li>
         * <li>Each key of the object states read map {@linkplain Map#get(Object) maps
         * to} a null value or an object state with a {@linkplain ObjectState#getWhen()
         * time-stamp} {@linkplain Duration#compareTo(Duration) at or before} the
         * {@linkplain ObjectStateId#getWhen() time-stamp} of the key.</li>
         * </ul>
         * 
         * @return the object states read.
         * @see Universe#getObjectState(UUID, Duration)
         */
        public final Map<ObjectStateId, ObjectState> getObjectStatesRead() {
            return Collections.unmodifiableMap(objectStatesRead);
        }

        /**
         * <p>
         * The {@link Universe} for which this transaction changes the state.
         * </p>
         * 
         * @return the universe; not null.
         */
        public final Universe getUniverse() {
            return Universe.this;
        }

    }// class

    private final Map<ObjectStateId, ObjectStateData> stateTransitions = new HashMap<>();
    private final Map<UUID, SortedMap<Duration, ObjectState>> objectStateHistories = new HashMap<>();

    private Duration earliestTimeOfCompleteState;

    /**
     * <p>
     * Construct an empty universe.
     * </p>
     * <ul>
     * <li>The {@linkplain #getEarliestTimeOfCompleteState() earliest complete
     * state} time-stamp of this universe is the given earliest complete state
     * time-stamp.</li>
     * <li>The {@linkplain #getObjectIds() set of object IDs}
     * {@linkplain Set#isEmpty() is empty}.</li>
     * <li>The {@linkplain #getStateTransitionIds() set of IDs of object states}
     * {@linkplain Set#isEmpty() is empty}.</li>
     * </ul>
     * 
     * @param earliestTimeOfCompleteState
     *            The earliest point in time for which this universe has a known
     *            {@linkplain ObjectState state} for {@linkplain #getObjectIds() all
     *            the objects} in the universe.
     * @throws NullPointerException
     *             If {@code earliestCompleteState} is null
     */
    public Universe(final Duration earliestTimeOfCompleteState) {
        this.earliestTimeOfCompleteState = Objects.requireNonNull(earliestTimeOfCompleteState,
                "earliestTimeOfCompleteState");
    }

    private void addDependentStateTransitions(ObjectStateId objectStateId, final Set<ObjectStateId> result) {
        final SortedMap<Duration, ObjectState> stateTransitionsFrom = getStateHistoryFrom(objectStateId.getObject(),
                objectStateId.getWhen());
        if (stateTransitionsFrom != null) {
            for (ObjectState stateTransitionAfter : stateTransitionsFrom.values()) {
                final ObjectStateId stateTransitionAfterId = stateTransitionAfter.getId();
                final ObjectStateData stateTransitionAfterData = stateTransitions.get(stateTransitionAfterId);
                result.addAll(stateTransitionAfterData.dependentStateTransitions);
                for (ObjectStateId dependentStateTransitionId : stateTransitionAfterData.dependentStateTransitions) {
                    addDependentStateTransitions(dependentStateTransitionId, result);
                }
            }
        }
    }

    /**
     * <p>
     * Add a state transition to this universe.
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
     * @throws MissingDependedUponStateException
     *             If the {@linkplain ObjectStateId#getWhen() time-stamp} of any
     *             {@linkplain ObjectState#getDependencies() dependencies} of the
     *             {@code objectState} are at or after the
     *             {@linkplain #getEarliestTimeOfCompleteState() earliest complete
     *             state} but are for {@linkplain ObjectStateId#getObject() objects}
     *             that are not {@linkplain #getObjectIds() known objects}. In this
     *             case, this {@link Universe} is unchanged.
     */
    public final void append(ObjectState objectState)
            throws InvalidEventTimeStampOrderException, MissingDependedUponStateException {
        Objects.requireNonNull(objectState, "objectState");

        final ObjectStateId id = objectState.getId();
        final UUID object = id.getObject();
        final Duration when = id.getWhen();
        final Collection<ObjectStateId> dependencies = objectState.getDependencies().values();
        SortedMap<Duration, ObjectState> stateHistory = objectStateHistories.get(object);

        for (ObjectStateId dependency : dependencies) {
            if (0 <= dependency.getWhen().compareTo(earliestTimeOfCompleteState)
                    && !objectStateHistories.containsKey(dependency.getObject())) {
                throw new MissingDependedUponStateException();
            }
        }
        if (stateHistory != null && 0 <= stateHistory.lastKey().compareTo(when)) {
            throw new InvalidEventTimeStampOrderException();
        }

        if (stateHistory == null) {
            stateHistory = new TreeMap<>();
            objectStateHistories.put(object, stateHistory);
        }
        stateHistory.put(when, objectState);

        final ObjectStateData stateData = new ObjectStateData(objectState);
        stateTransitions.put(id, stateData);

        for (ObjectStateId dependency : dependencies) {
            final UUID dependencyObject = dependency.getObject();
            final Duration dependencyWhen = dependency.getWhen();
            final ObjectState dependencyPreviousStateTransition = getObjectState(dependencyObject, dependencyWhen);
            if (dependencyPreviousStateTransition != null) {
                final ObjectStateId dependencyPreviousStateTransitionId = dependencyPreviousStateTransition.getId();
                final ObjectStateData dependencyPreviousStateTransitionData = stateTransitions
                        .get(dependencyPreviousStateTransitionId);
                dependencyPreviousStateTransitionData.dependentStateTransitions.add(id);
            }
        }
    }

    /**
     * <p>
     * Begin a new transaction for changing the state of this {@link Universe}.
     * </p>
     * <ul>
     * <li>Always returns a (non null) transaction.</li>
     * <li>The {@linkplain Universe.Transaction#getUniverse() universe} of the
     * returned transaction is this transaction.</li>
     * <li>The returned transaction {@linkplain Map#isEmpty() has not}
     * {@linkplain Universe.Transaction#getObjectStatesRead() read any object
     * states}.</li>
     * </ul>
     * 
     * @return a new transaction object; not null
     */
    public final Transaction beginTransaction() {
        return new Transaction();
    }

    /**
     * <p>
     * The set of {@linkplain #getStateTransitionIds() known state transitions} that
     * depend on a given object state.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of dependent state transitions.</li>
     * <li>The set of dependent state transitions does not have a null element.</li>
     * <li>The set of dependent state transitions is a
     * {@linkplain Set#containsAll(java.util.Collection) subset} of the
     * {@linkplain #getStateTransitionIds() set of all known state
     * transitions}.</li>
     * <li>Dependencies are time ordered: The dependent state transitions of a given
     * object state all have their {@linkplain ObjectStateId#getWhen() time-stamps}
     * {@linkplain Duration#compareTo(Duration) after} the time-stamp of the given
     * object state.</li>
     * <li>The state transitions that depend on a given object state are consistent
     * with the {@linkplain ObjectState#getDependencies() dependency} information of
     * the {@linkplain #getStateTransition(ObjectStateId) state transitions}: if the
     * universe has state transition <var>S</var> that
     * {@linkplain ObjectState#getDepenendedUponStates() depends on} the state with
     * ID <var>D<var>, which is the state {@linkplain ObjectStateId#getObject() of
     * object} <var>X</var> and time <var>T</var>, and the latest
     * {@linkplain #getObjectStateHistory(UUID) state transition} for <var>X</var>
     * at or before time <var>T</var> has ID <var>J</var>, then one of the dependent
     * state transitions of <var>J</var> is <var>I</var>.</li>
     * <li>Dependencies carry forward through time: if object state <var>I</var> has
     * object state <var>J</var> as a dependent state transition, and <var>I</var>
     * id for {@linkplain ObjectStateId#getObject() of object} <var>X</var> at
     * {@linkplain ObjectStateId#getWhen() time} <var>T</var>, the dependent state
     * transitions for object <var>X</var> at all times after <var>T</var> also
     * include <var>J</var>.</li>
     * <li>Dependencies are transitive: if object state <var>I</var> has object
     * state <var>J</var> as a dependent state transition, and object state
     * <var>J</var> has object state <var>K</var> as a dependent state transition,
     * then <var>I</var> also has <var>K</var> as a dependent state transition.</li>
     * </ul>
     * 
     * @param objectStateId
     *            The ID of the object state of interest
     * @return The state transitions that depend on the object state that has the
     *         given ID.
     * @throws NullPointerException
     *             If {@code ObjectStateId} is null.
     */
    public final Set<ObjectStateId> getDependentStateTransitions(ObjectStateId objectStateId) {
        Objects.requireNonNull(objectStateId, "objectStateId");
        final Set<ObjectStateId> result = new HashSet<>();
        addDependentStateTransitions(objectStateId, result);
        return result;
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
    public final Duration getEarliestTimeOfCompleteState() {
        return earliestTimeOfCompleteState;
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
        final SortedMap<Duration, ObjectState> eventsAtOrBefore = getStateHistoryUntil(object, when);
        if (eventsAtOrBefore == null) {// unknown object
            return null;
        } else if (eventsAtOrBefore.isEmpty()) {// before first event
            return null;
        } else {
            return eventsAtOrBefore.get(eventsAtOrBefore.lastKey());
        }
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

    private SortedMap<Duration, ObjectState> getStateHistoryFrom(UUID object, Duration when) {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(when, "when");

        final SortedMap<Duration, ObjectState> objectStateHistory = objectStateHistories.get(object);
        if (objectStateHistory == null) {// unknown object
            return null;
        }
        return objectStateHistory.tailMap(when);
    }

    private SortedMap<Duration, ObjectState> getStateHistoryUntil(UUID object, Duration when) {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(when, "when");

        final SortedMap<Duration, ObjectState> objectStateHistory = objectStateHistories.get(object);
        if (objectStateHistory == null) {// unknown object
            return null;
        }
        return objectStateHistory.headMap(when.plusNanos(1L));
    }

    /**
     * <p>
     * The state of an object within this universe, just after a state transition,
     * given the {@linkplain ObjectState#getId() ID} of that state transition.
     * </p>
     * <ul>
     * <li>Have a (non null) state transition if, and only if, the given object
     * state ID is one of the {@linkplain #getStateTransitionIds() known object
     * state IDs} of this universe.</li>
     * <li>A (non null) state transition accessed using a given object state ID has
     * an {@linkplain ObjectStateId#equals(Object) equivalent} object state ID as
     * its {@linkplain ObjectState#getId() ID}.</li>
     * <li>All the {@linkplain ObjectState#getDependencies() dependencies} of the
     * state transitions either have a {@linkplain ObjectStateId#getWhen()
     * time-stamp} before the {@linkplain #getEarliestTimeOfCompleteState() earliest
     * complete state} time-stamp of the universe, or are for
     * {@linkplain #getObjectIds() known objects}.</li>
     * </ul>
     * 
     * @param objectStateId
     *            The ID of the state transition of interest.
     * @return the state transition
     * @throws NullPointerException
     *             If {@code objectStateId} is null.
     */
    public final ObjectState getStateTransition(ObjectStateId objectStateId) {
        Objects.requireNonNull(objectStateId, "objectStateId");
        final ObjectStateData objectStateData = stateTransitions.get(objectStateId);
        if (objectStateData == null) {
            return null;
        } else {
            return objectStateData.state;
        }
    }

    /**
     * <p>
     * All the known {@linkplain ObjectStateId identifiers} of state transitions of
     * objects within this universe.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of state transitions IDs.</li>
     * <li>The set of IDs of state transitions does not have a null element.</li>
     * <li>The set of IDs of state transitions does not have elements for
     * {@linkplain ObjectStateId#getObject() object IDs} that are not in the
     * {@linkplain #getObjectIds() set of objects in this universe}.</li>
     * <li>The set of IDs of state transitions may be immutable, or it may be a copy
     * of an underlying collection.</li>
     * </ul>
     * 
     * @return the IDs
     */
    public final Set<ObjectStateId> getStateTransitionIds() {
        return new HashSet<>(stateTransitions.keySet());
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
