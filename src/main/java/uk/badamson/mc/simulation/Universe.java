package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * A checked exception for indicating that a {@link Universe.Transaction} must
     * be aborted, because the consistency constraints of transaction and of the
     * {@linkplain Universe.Transaction#getUniverse() universe} of the transaction
     * would not then be satisfied.
     * </p>
     */
    public static final class AbortedTransactionException extends Exception {

        private static final long serialVersionUID = 1L;
        private static final String MESSAGE = "Aborted transaction";

        /**
         * <p>
         * Construct a checked exception for indicating that a
         * {@link Universe.Transaction} must be aborted, because the consistency
         * constraints of transaction and of the
         * {@linkplain Universe.Transaction#getUniverse() universe} of the transaction
         * would not then be satisfied.
         * </p>
         */
        public AbortedTransactionException() {
            super(MESSAGE);
        }

        /**
         * <p>
         * Construct a checked exception for indicating that a
         * {@link Universe.Transaction} must be aborted, because the consistency
         * constraints of transaction and of the
         * {@linkplain Universe.Transaction#getUniverse() universe} of the transaction
         * would not then be satisfied, and that has been indicated by an underlying
         * cause
         * </p>
         * 
         * @param cause
         *            The exception thrown to signal the underlying cause.
         */
        public AbortedTransactionException(Throwable cause) {
            super(MESSAGE, cause);
        }

    }// class

    private static final class ObjectData {
        final ModifiableValueHistory<ObjectState> stateHistory = new ModifiableValueHistory<>();
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
        private boolean abortCommit;

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
         * Complete this transaction.
         * </p>
         * <ul>
         * <li>If this transaction included a {@linkplain #put(ObjectState) put} (write)
         * of an object state, that object state has been recorded in the
         * {@linkplain Universe#getObjectStateHistory(UUID) state history} of the
         * {@linkplain ObjectState#getObject() object} of the object state in the
         * {@linkplain #getUniverse() universe} of this transaction.</li>
         * </ul>
         * 
         * @throws Universe.AbortedTransactionException
         *             If the consistency constraints of this transaction and of the
         *             {@linkplain #getUniverse() universe} of this transaction could
         *             not then be satisfied. In this case the
         *             {@linkplain #willAbortCommit() commit abort flag} is set.
         */
        public final void commit() throws Universe.AbortedTransactionException {
            if (abortCommit) {
                throw new Universe.AbortedTransactionException();
            }
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
            ObjectStateId id = new ObjectStateId(object, when);
            final ObjectState objectState = Universe.this.getObjectState(object, when);
            objectStatesRead.put(id, objectState);
            final ObjectStateId dependency0 = dependencies.get(object);
            if (dependency0 == null || when.compareTo(dependency0.getWhen()) < 0) {
                dependencies.put(object, id);
            }
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
            return new HashMap<>(dependencies);
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

        /**
         * <p>
         * Try to add a state transition to the {@linkplain #getUniverse() universe} of
         * this transaction.
         * <p>
         * <ul>
         * <li>Either the {@linkplain #willAbortCommit() commit abort flag } is set or
         * the {@linkplain StateTransition#getStates() state} {@linkplain Map#values()
         * values} of the given state transition have been appended to the
         * {@linkplain Universe#getObjectStateHistory(UUID) object state history} of
         * their corresponding object, with the {@linkplain StateTransition#getWhen()
         * time-stamp} of the state-transition as their {@linkplain SortedMap#keySet()
         * key}.</li>
         * </ul>
         * 
         * @param stateTransition
         *            The state transition to add.
         * @throws NullPointerException
         *             If {@code stateTransition} is null
         * @throws IllegalArgumentException
         *             If the {@linkplain StateTransition#getDependencies()
         *             dependencies} of the state transition are not
         *             {@linkplain Map#equals(Object) equal} to the
         *             {@linkplain #getDependencies() dependencies} of this transaction.
         * 
         */
        public final void put(StateTransition stateTransition) {
            Objects.requireNonNull(stateTransition, "stateTransition");
            if (!dependencies.equals(stateTransition.getDependencies())) {
                throw new IllegalArgumentException(
                        "State transition dependencies not equal to transaction dependencies");
            }
            try {
                appendStateTransition(stateTransition);
            } catch (IllegalStateException e) {
                abortCommit = true;
            }
        }

        /**
         * <p>
         * Whether it is already known that attempting to {@linkplain #commit() commit}
         * this transaction will fail (that is, will throw a
         * {@link Universe.AbortedTransactionException}.
         * </p>
         * 
         * @return whether already known
         */
        public final boolean willAbortCommit() {
            return abortCommit;
        }

    }// class

    private Duration earliestTimeOfCompleteState;
    private Map<UUID, ObjectData> objectDataMap = new HashMap<>();

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

    private void appendStateTransition(Duration when, UUID object, ObjectState state,
            final Map<UUID, ObjectStateId> dependencies) {
        ObjectData od = objectDataMap.get(object);
        if (od == null) {
            od = new ObjectData();
            objectDataMap.put(object, od);
        }
        od.stateHistory.appendTransition(when, state);
    }

    /**
     * <p>
     * Add a state transition to this universe.
     * <p>
     * 
     * @param stateTransition
     *            The state transition to append.
     * @throws NullPointerException
     *             If {@code stateTransition} is null.
     * @throws IllegalStateException
     *             <ul>
     *             <li>If the {@linkplain ObjectState#getObject() object} of the
     *             {@code objectState} is already an {@linkplain #getObjectIds()
     *             object ID} of this universe, but the
     *             {@linkplain ObjectState#getWhen() time-stamp} of
     *             {@code objectState} is not
     *             {@linkplain Duration#compareTo(Duration) after} the
     *             {@linkplain SortedMap#lastKey() last} state in the
     *             {@linkplain #getObjectStateHistory(UUID) object state history} of
     *             that object. In this case, this {@link Universe} is unchanged.
     *             </li>
     *             <li>If the {@linkplain ObjectStateId#getWhen() time-stamp} of any
     *             the {@code dependencies} are at or after the
     *             {@linkplain #getEarliestTimeOfCompleteState() earliest complete
     *             state} but are for {@linkplain ObjectStateId#getObject() objects}
     *             that are not {@linkplain #getObjectIds() known objects}. In this
     *             case, this {@link Universe} is unchanged.</li>
     *             <li>If the {@linkplain ObjectStateId#getWhen() time-stamp} of the
     *             {@code objectState} is after the
     *             {@linkplain #getEarliestTimeOfCompleteState() earliest complete
     *             state} but the {@code dependencies} does not include the
     *             {@linkplain ObjectState#getId() ID} of the
     *             {@linkplain SortedMap#lastKey() last} state transition of the
     *             {@linkplain #getObjectStateHistory(UUID) state history} of the
     *             {@linkplain ObjectState#getObject() object} of the
     *             {@code objectState}. In this case, this {@link Universe} is
     *             unchanged.</li>
     *             <li>If the {@linkplain ObjectState#getObject() object} of the
     *             {@code objectState} is already an {@linkplain #getObjectIds()
     *             object ID} of this universe, and the
     *             {@linkplain SortedMap#lastKey() last} entry in the
     *             {@linkplain Universe#getObjectStateHistory(UUID) state history}
     *             is a null value (indicating that the object ceased to exist at
     *             the time of that entry). In this case, this {@link Universe} is
     *             unchanged.</li>
     *             </ul>
     */
    public final void appendStateTransition(StateTransition stateTransition) throws IllegalStateException {
        Objects.requireNonNull(stateTransition, "stateTransition");

        final Duration when = stateTransition.getWhen();
        final Map<UUID, ObjectStateId> dependencies = stateTransition.getDependencies();
        stateTransition.getStates().entrySet().stream().forEach(e -> {
            final UUID object = e.getKey();
            final ObjectState state = e.getValue();
            appendStateTransition(when, object, state, dependencies);
        });

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
     * <li>The {@linkplain Transaction#willAbortCommit() commit abort flag} of the
     * return transaction is clear ({@code false}.</li>
     * </ul>
     * 
     * @return a new transaction object; not null
     */
    public final Transaction beginTransaction() {
        return new Transaction();
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
        return Collections.unmodifiableSet(objectDataMap.keySet());
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
        Objects.requireNonNull(when, "when");
        final var history = getObjectStateHistory(object);
        if (history == null) {
            return null;
        } else {
            return history.get(when);
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
     * {@linkplain ValueHistory#isEmpty() empty}.</li>
     * <li>Only the {@linkplain ValueHistory#getLastValue() last value} in a (non
     * null) object state history may be a null state (indicating that the object
     * ceased to exist at that time).</li>
     * </ul>
     * 
     * @param object
     *            The object of interest
     * @return The state history of the given object, or null if this universe does
     *         not {@linkplain #getObjectIds() include} the given object. A null
     *         {@linkplain ValueHistory#get(Duration) value} in the history
     *         indicates that the object did not exist at that time.
     * @throws NullPointerException
     *             If {@code object} is null
     */
    public final ValueHistory<ObjectState> getObjectStateHistory(UUID object) {
        Objects.requireNonNull(object, "object");
        final var od = objectDataMap.get(object);
        if (od == null) {
            return null;
        } else {
            return od.stateHistory;
        }
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
        final var history = getObjectStateHistory(objectStateId.getObject());
        if (history == null) {
            return null;
        }
        final Duration when = objectStateId.getWhen();
        if (history.getTransitionTimes().contains(when)) {
            return history.get(when);
        } else {
            return null;
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
        final Stream<ObjectStateId> stream = objectDataMap.entrySet().stream().flatMap(objectDataMapEntry -> {
            final UUID object = objectDataMapEntry.getKey();
            final ValueHistory<ObjectState> history = objectDataMapEntry.getValue().stateHistory;
            return history.streamOfTransitions().map(transition -> {
                final Duration t = transition.getKey();
                return new ObjectStateId(object, t);
            });
        });

        return stream.collect(Collectors.toUnmodifiableSet());
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
     * {@linkplain ValueHistory#getFirstTansitionTime() first transition time} of
     * the {@linkplain #getObjectStateHistory(UUID) state history} of that
     * object.</li>
     * </ul>
     * 
     * @param object
     *            The ID of the object of interest.
     * @return The time-stamp of the first event of the object with {@code object}
     *         as its ID, or null if {@code object} is not a
     *         {@linkplain #getObjectIds() known object ID}.
     */
    public final Duration getWhenFirstState(UUID object) {
        final var history = getObjectStateHistory(object);
        if (history == null) {
            return null;
        } else {
            return history.getFirstTansitionTime();
        }
    }
}
