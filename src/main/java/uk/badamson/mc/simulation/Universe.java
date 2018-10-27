package uk.badamson.mc.simulation;
/* 
 * © Copyright Benedict Adamson 2018.
 * 
 * This file is part of MC-des.
 *
 * MC-des is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MC-des is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MC-des.  If not, see <https://www.gnu.org/licenses/>.
 */

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.NotThreadSafe;

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

    private static final class ObjectData {
        final ModifiableValueHistory<ObjectState> stateHistory = new ModifiableValueHistory<>();
        final ModifiableSetHistory<Transaction> uncommittedReaders = new ModifiableSetHistory<>();
        final ModifiableSetHistory<Transaction> uncommittedWriters = new ModifiableSetHistory<>();

        @NonNull
        Duration lastCommit = ValueHistory.START_OF_TIME;
    }// class

    /**
     * <p>
     * A transaction for changing the state of a {@link Universe}.
     * </p>
     * <p>
     * That is, a record of a set of reads and up to one write to the state
     * histories of the Universe that can be committed as an atomic operation. A
     * transaction may read (fetch) and write (put) values. A transaction however
     * may not interleave reads and writes. All its writes must be after any reads,
     * after having entered <i>write mode</i>.
     * </p>
     */
    @NotThreadSafe
    public final class Transaction {

        @NonNull
        private final TransactionListener listener;

        private final Map<ObjectStateId, ObjectState> objectStatesRead = new HashMap<>();
        private final Map<UUID, ObjectState> objectStatesWritten = new HashMap<>();
        private final Map<UUID, ObjectStateId> dependencies = new HashMap<>();

        // Must be appended to and committed before this transaction.
        private final Set<UUID> pastTheEndReads = new HashSet<>();

        // Must be committed before this transaction.
        private final Set<Transaction> predecessorTransactions = new HashSet<>();

        // Must be committed simultaneously with this transaction.
        // TODO private final Set<Transaction> mutualTransactions = new HashSet<>();

        // Must be committed after this transaction.
        private final Set<Transaction> successorTransactions = new HashSet<>();

        private Duration when;

        @NonNull
        private TransactionOpenness openness = TransactionOpenness.READING;

        private Transaction(@NonNull TransactionListener listener) {
            this.listener = Objects.requireNonNull(listener, "listener");
        }

        /**
         * <p>
         * End this transaction, if possible, rolling back any
         * {@linkplain #put(UUID, ObjectState) writes} it has performed.
         * </p>
         * <ul>
         * <li>If this transaction {@linkplain #getOpenness() was}
         * {@linkplain Universe.TransactionOpenness#COMMITTED committed}, it remains
         * committed.</li>
         * <li>The transaction {@linkplain #getOpenness() is} either
         * {@linkplain Universe.TransactionOpenness#ABORTED aborted} or
         * {@linkplain Universe.TransactionOpenness#COMMITTED committed}.</li>
         * </ul>
         */
        public final void abort() {
            openness = TransactionOpenness.ABORTING;

            for (UUID object : dependencies.keySet()) {
                var od = objectDataMap.get(object);
                od.uncommittedReaders.remove(this);
            }

            // roll-back changes:
            for (UUID object : objectStatesWritten.keySet()) {
                var od = objectDataMap.get(object);
                od.stateHistory.removeTransitionsFrom(when);
                od.uncommittedWriters.remove(this);// optimisation
                if (od.stateHistory.isEmpty()) {
                    objectDataMap.remove(object);
                }
            }

            openness = TransactionOpenness.ABORTED;
            listener.onAbort();

            // TODO abort mutalTransactions
            for (var successor : successorTransactions) {
                successor.abort();
            }
        }

        /**
         * <p>
         * Begin completion of this transaction, completing it if possible.
         * </p>
         * <ul>
         * <li>The transaction {@linkplain #getOpenness() is} not (anymore)
         * {@linkplain Universe.TransactionOpenness#READING reading} or
         * {@linkplain Universe.TransactionOpenness#WRITING writing}.</li>
         * </ul>
         * 
         * @param[in] onCommit An action to perform when (if) this transaction
         *            successfully completes the commit operation.
         * @param[in] onAbort An action to perform when (if) this transaction aborts the
         *            commit operation.
         * @throws IllegalStateException
         *             If this {@linkplain #didBeginCommit() committing this transaction
         *             has already begun}.
         */
        public final void beginCommit() {
            if (openness == TransactionOpenness.COMMITTING) {
                throw new IllegalStateException("Already began");
            }

            if (openness == TransactionOpenness.ABORTING) {
                abort();
            } else {
                openness = TransactionOpenness.COMMITTING;
                commitIfPossible();
            }
        }

        /**
         * <p>
         * Change this transaction from read mode to write mode.
         * </p>
         * <ul>
         * <li>The {@linkplain #getWhen() time-stamp of any object states to be written}
         * by this transaction becomes the same as the given time-stamp.</li>
         * </ul>
         * 
         * @param when
         *            The time-stamp of all object states to be
         *            {@linkplain #put(UUID, ObjectState) put} (written) by this
         *            transaction, expressed as the duration since an epoch.
         * 
         * @throws NullPointerException
         *             If {@code when} is null.
         * @throws IllegalStateException
         *             <ul>
         *             <li>If the any of the {@linkplain #getObjectStatesRead() reads}
         *             done by this transaction were for for
         *             {@linkplain ObjectStateId#getWhen() times} at of after the given
         *             time.</li>
         *             </ul>
         *             If this transaction is already in write mode. That is, if this
         *             method has already been called for this transaction.</li>
         *             </ul>
         */
        public final void beginWrite(@NonNull Duration when) {
            Objects.requireNonNull(when, "when");
            if (objectStatesRead.keySet().stream().map(id -> id.getWhen()).filter(t -> 0 <= t.compareTo(when)).findAny()
                    .orElse(null) != null) {
                throw new IllegalStateException("Time-stamp of read state at or after the given time.");
            }
            if (this.when != null) {
                throw new IllegalStateException("Already in write mode.");
            }
            this.when = when;
            openness = TransactionOpenness.WRITING;
        }

        private void commit() {
            openness = TransactionOpenness.COMMITTED;
            for (UUID object : objectStatesWritten.keySet()) {
                final var od = objectDataMap.get(object);
                assert od.lastCommit.compareTo(when) < 0;
                od.lastCommit = when;
                od.uncommittedWriters.remove(this);
            }
            for (UUID object : dependencies.keySet()) {
                final var od = objectDataMap.get(object);
                if (od != null) {
                    od.uncommittedReaders.remove(this);
                }
                // else a prehistoric dependency
            }
            listener.onCommit();
            for (var successor : successorTransactions) {
                successor.predecessorTransactions.remove(this);
                if (successor.openness == TransactionOpenness.COMMITTING) {
                    successor.commitIfPossible();
                }
            }
        }

        private void commitIfPossible() {
            if (openness != TransactionOpenness.COMMITTING) {
                return;
            }
            if (!pastTheEndReads.isEmpty()) {
                // Do not commit if awaiting appending of a state.
                return;
            }
            if (!predecessorTransactions.isEmpty()) {
                // Do not commit if have predecessors.
                return;
            }
            // TODO Collaborate with mutual transactions
            commit();
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
        public final @NonNull Map<UUID, ObjectStateId> getDependencies() {
            return new HashMap<>(dependencies);
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
         * got} by this transaction.</li>
         * <li>The object state of for an object ID and point in time that has not
         * already been {@linkplain #getObjectStatesRead() read} by this transaction is
         * the same object state as can be
         * {@linkplain Universe#getObjectState(UUID, Duration) got} from the
         * {@linkplain #getUniverse() universe} of this transaction.</li>
         * <li>The object state for an object ID and point in time that has already been
         * {@linkplain #getObjectStatesRead() read} by this transaction is the same
         * object state as was read previously. That is, the transaction object caches
         * reads.</li>
         * <li>The method records the returned state as one of the
         * {@linkplain #getObjectStatesRead() read states}. Hence this method is not a
         * simple getter.</li>
         * <li>This method is <dfn>optimistic</dfn> and <dfn>non blocking</dfn>. It
         * assumes that {@linkplain #put(UUID, ObjectState) writes} (made by other
         * transactions) will be successfully committed, so the method may return
         * uncommitted writes. It does not wait (block) until the write is
         * committed.</li>
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
         * @throws IllegalStateException
         *             <ul>
         *             <li>If this transaction is in write mode (because its
         *             {@link #beginWrite(Duration)} method has been called) and the
         *             requested object is not one of the
         *             {@linkplain #getObjectStatesRead() object states already
         *             read}.</li>
         *             <li>If {@linkplain #didBeginCommit() committing this transaction
         *             has started} (because its {@link #beginCommit()} method has been
         *             called) and the requested object is not one of the
         *             {@linkplain #getObjectStatesRead() object states already
         *             read}.</li>
         *             </ul>
         */
        public final @Nullable ObjectState getObjectState(@NonNull UUID object, @NonNull Duration when) {
            ObjectStateId id = new ObjectStateId(object, when);
            ObjectState objectState;
            objectState = objectStatesRead.get(id);
            if (objectState == null && !objectStatesRead.containsKey(id)) {
                if (openness != TransactionOpenness.READING) {
                    throw new IllegalStateException("Began commit");
                }
                objectState = readObjectState(object, when, id);
            }
            // else used cached value
            return objectState;
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
         * {@linkplain ObjectState object state} for that object at that point in
         * time.</li>
         * <li>A key of the object states read map {@linkplain Map#get(Object) mapping}
         * to a null value indicates that the {@linkplain ObjectStateId#getObject()
         * object} of the key did not exist at the {@linkplain ObjectStateId#getWhen()
         * point in time} of the key.</li>
         * </ul>
         * 
         * @return the object states read.
         * @see Universe#getObjectState(UUID, Duration)
         */
        public final @NonNull Map<ObjectStateId, ObjectState> getObjectStatesRead() {
            return Collections.unmodifiableMap(objectStatesRead);
        }

        /**
         * <p>
         * The object states that have been read as part of this transaction.
         * </p>
         * <ul>
         * <li>Always have a (non null) map of object states written.</li>
         * <li>The map of object states written may be unmodifiable or a copy of
         * internal state.</li>
         * <li>The map of object states written {@linkplain Map#isEmpty() is empty} if
         * this transaction is in {@linkplain #getWhen() read mode}.</li>
         * <li>The map of object states written does not
         * {@linkplain Map#containsKey(Object) have} a null key.</li>
         * <li>The map of object states written maps the ID of the object of interest to
         * the {@linkplain ObjectState object state} for that object at the
         * {@linkplain #getWhen() write time} of this transaction.</li>
         * <li>A key of the object states written map {@linkplain Map#get(Object)
         * mapping} to a null value indicates that the object of the key ceased to exist
         * at the {@linkplain #getWhen() write time} of this transaction.</li>
         * </ul>
         * 
         * @return the object states written.
         * 
         * @see Universe#getObjectState(UUID, Duration)
         */
        public final @NonNull Map<UUID, ObjectState> getObjectStatesWritten() {
            return Collections.unmodifiableMap(objectStatesWritten);
        }

        /**
         * <p>
         * The degree to which this transaction can be said to be <dfn>open</dfn>.
         * </p>
         * 
         * @return the degree of openness; not null.
         */
        @NonNull
        public final TransactionOpenness getOpenness() {
            return openness;
        }

        /**
         * <p>
         * The {@link Universe} for which this transaction changes the state.
         * </p>
         * 
         * @return the universe; not null.
         */
        public final @NonNull Universe getUniverse() {
            return Universe.this;
        }

        /**
         * <p>
         * The time-stamp of an object states (to be)
         * {@linkplain #put(UUID, ObjectState) written} by this transaction.
         * </p>
         * 
         * @return the time-stamp, or null if this transaction is (still) in read mode.
         */
        public final @Nullable Duration getWhen() {
            return when;
        }

        /**
         * <p>
         * Try to add a state transition (or an initial state) for an object to the
         * {@linkplain #getUniverse() universe} of this transaction.
         * <p>
         * <ul>
         * <li>The method records the given state as one of the
         * {@linkplain #getObjectStatesWritten() states written}.</li>
         * <li>This method is <dfn>optimistic</dfn>. It assumes that this transaction
         * will be successfully committed, so any other transactions that have read the
         * old state of the object at the {@linkplain #getWhen() time} of this write (or
         * a later time) are now invalid and must be aborted.</li>
         * </ul>
         * 
         * @param object
         *            The ID of the object that has the given state transition at the
         *            given time.
         * @param state
         *            The state of the object just after this state transition, at the
         *            given point in time. A null value indicates that the object ceases
         *            to exist at the given time.
         * @throws NullPointerException
         *             If {@code object} is null.
         * @throws IllegalStateException
         *             <ul>
         *             <li>If this transaction is not in write mode (because its
         *             {@link #beginWrite(Duration)} method has not been called).</li>
         *             <li>If {@linkplain #didBeginCommit() committing this transaction
         *             has been started} (because its {@link #beginCommit()} method has
         *             been called).</li>
         *             </ul>
         */
        public final void put(@NonNull UUID object, @Nullable ObjectState state) {
            Objects.requireNonNull(object, "object");
            if (openness != TransactionOpenness.WRITING) {
                throw new IllegalStateException("Began commit");
            }

            objectStatesWritten.put(object, state);

            ObjectData od = objectDataMap.get(object);
            if (od == null) {
                od = new ObjectData();
                objectDataMap.put(object, od);
            }
            final ModifiableValueHistory<ObjectState> stateHistory = od.stateHistory;
            if (state != null && !stateHistory.isEmpty() && stateHistory.getLastValue() == null) {
                // Attempted resurrection of a dead object.
                openness = TransactionOpenness.ABORTING;
                return;
            }
            final Duration lastTransition0 = stateHistory.getLastTansitionTime();
            try {
                stateHistory.appendTransition(when, state);
            } catch (IllegalStateException e) {
                openness = TransactionOpenness.ABORTING;
            }
            od.uncommittedWriters.addFrom(when, this);
            for (Transaction uncommittedReader : od.uncommittedReaders.get(when)) {
                uncommittedReader.abort();
            }
            if (lastTransition0 != null) {
                for (Transaction pastTheEndReader : od.uncommittedReaders.get(lastTransition0.plusNanos(1))) {
                    pastTheEndReader.pastTheEndReads.remove(object);
                    pastTheEndReader.predecessorTransactions.add(this);
                    successorTransactions.add(pastTheEndReader);
                    // TODO handle mutual dependencies
                }
            }
        }

        private ObjectState readObjectState(UUID object, Duration when, ObjectStateId id) {
            final ObjectState objectState;
            final var od = objectDataMap.get(object);
            if (od == null) {// unknown object
                objectState = null;
            } else {
                objectState = od.stateHistory.get(when);
                if (od.stateHistory.getLastTansitionTime().compareTo(when) < 0) {
                    pastTheEndReads.add(object);
                }
                od.uncommittedReaders.addUntil(when, this);
                final Set<Transaction> uncommittedWriters = od.uncommittedWriters.get(when);
                predecessorTransactions.addAll(uncommittedWriters);
                for (var uncommittedWriter : uncommittedWriters) {
                    uncommittedWriter.successorTransactions.add(this);
                }
            }
            objectStatesRead.put(id, objectState);
            final ObjectStateId dependency0 = dependencies.get(object);
            if (dependency0 == null || when.compareTo(dependency0.getWhen()) < 0) {
                dependencies.put(object, id);
            }
            assert dependencies.containsKey(object);
            return objectState;
        }

    }// class

    /**
     * <p>
     * An object that can respond to {@link Universe.Transaction} events.
     * </p>
     */
    public interface TransactionListener {
        /**
         * <p>
         * An action to perform when (if) a transaction successfully completes its
         * commit operation.
         * </p>
         */
        public void onAbort();

        /**
         * <p>
         * An action to perform when (if) a transaction aborts its commit operation.
         * </p>
         */
        public void onCommit();

    }// interface

    /**
     * <p>
     * The degree to which an {@link Universe.Transaction} can be said to be
     * <dfn>open</dfn>.
     * </p>
     */
    public enum TransactionOpenness {
        /**
         * <p>
         * The transaction is <dfn>open</dfn> and may (successfully)
         * {@linkplain Universe.Transaction#getObjectState(UUID, Duration) read object
         * states}.
         * </p>
         */
        READING,
        /**
         * <p>
         * The transaction is <dfn>open</dfn> and may (successfully)
         * {@linkplain Universe.Transaction#put(UUID, ObjectState) write object states}.
         * </p>
         * <p>
         * The transaction may not
         * {@linkplain Universe.Transaction#getObjectState(UUID, Duration) read more
         * object states}.
         * </p>
         */
        WRITING,
        /**
         * <p>
         * The transaction is <dfn>open</dfn> and has started committing.
         * </p>
         */
        COMMITTING,
        /**
         * <p>
         * The transaction is <dfn>open</dfn> and has started aborting.
         * </p>
         */
        ABORTING,
        /**
         * <p>
         * The transaction is <dfn>closed</dfn> (not <dfn>open</dfn>) and has
         * <dfn>committed<dfn>.
         * </p>
         * <p>
         * Object states it has
         * {@linkplain Universe.Transaction#getObjectState(UUID, Duration) read} or
         * {@linkplain Universe.Transaction#put(UUID, ObjectState) written} will not be
         * rolled back. The read and written values can be relied on to be the true
         * values.
         * </p>
         */
        COMMITTED,
        /**
         * <p>
         * The transaction is <dfn>closed</dfn> (not <dfn>open</dfn>) and has aborted.
         * </p>
         * <p>
         * Object states it has
         * {@linkplain Universe.Transaction#getObjectState(UUID, Duration) read} or
         * {@linkplain Universe.Transaction#put(UUID, ObjectState) written} are not
         * reliable; the true values may be different.
         * </p>
         */
        ABORTED
    }// enum

    private Duration earliestTimeOfCompleteState;
    private final Map<UUID, ObjectData> objectDataMap = new HashMap<>();

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
    public Universe(final @NonNull Duration earliestTimeOfCompleteState) {
        this.earliestTimeOfCompleteState = Objects.requireNonNull(earliestTimeOfCompleteState,
                "earliestTimeOfCompleteState");
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
     * <li>The returned transaction {@linkplain Map#isEmpty() has not}
     * {@linkplain Universe.Transaction#getObjectStatesWritten() written any object
     * states}.</li>
     * <li>The transaction {@linkplain Universe.Transaction#getOpenness() is in}
     * {@linkplain Universe.TransactionOpenness#READING read mode}.</li>
     * </ul>
     * 
     * @param listener
     *            The transaction listener to use for this transaction.
     * @return a new transaction object; not null
     * @throws NullPointerException
     *             If {@code listener} is null
     */
    public final @NonNull Transaction beginTransaction(@NonNull TransactionListener listener) {
        return new Transaction(listener);
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
    public final @NonNull Duration getEarliestTimeOfCompleteState() {
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
    public final @NonNull Set<UUID> getObjectIds() {
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
    public final @Nullable ObjectState getObjectState(@NonNull UUID object, @NonNull Duration when) {
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
    public final @Nullable ValueHistory<ObjectState> getObjectStateHistory(@NonNull UUID object) {
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
    public final @Nullable ObjectState getStateTransition(@NonNull ObjectStateId objectStateId) {
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
    public final @NonNull Set<ObjectStateId> getStateTransitionIds() {
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
    public final @Nullable Duration getWhenFirstState(@NonNull UUID object) {
        final var history = getObjectStateHistory(object);
        if (history == null) {
            return null;
        } else {
            return history.getFirstTansitionTime();
        }
    }
}
