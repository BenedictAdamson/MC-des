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
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;
import uk.badamson.mc.history.ConstantValueHistory;
import uk.badamson.mc.history.ModifiableSetHistory;
import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;

/**
 * <p>
 * A collection of simulated objects and their {@linkplain ObjectState state}
 * histories.
 * </p>
 * <p>
 * This collection enforces constraints that ensure that the object state
 * histories are <dfn>consistent</dfn>. Consistency means that if a universe
 * contains an object state, it also contains all the depended upon states of
 * that state, unless those states are {@linkplain #getHistoryStart() too old}.
 * </p>
 */
@ThreadSafe
public class Universe {

    private abstract class Lockable implements Comparable<Lockable> {

        /*
         * If locks are to be held on multiple Lockable objects, the locks must be
         * acquired in the reverse of the natural ordering of the objects. That is, in
         * descending order of their ids.
         */
        @NonNull
        protected final Long id;

        /*
         * May acquire a lock on this.lock while have a lock on a
         * TransactionCoordinator, but not vice versa.
         */
        protected final Object lock = new Object();

        protected Lockable(@NonNull Long id) {
            assert id != null;
            this.id = id;
        }

        @Override
        public final int compareTo(Lockable that) {
            return id.compareTo(that.id);
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof Lockable))
                return false;
            Lockable other = (Lockable) obj;
            return getUniverse().equals(other.getUniverse()) && id.equals(other.id);
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

        @Override
        public final int hashCode() {
            return id.hashCode();
        }
    }// class

    static final class ObjectData {

        @GuardedBy("this")
        private final ModifiableValueHistory<ObjectState> stateHistory;

        @GuardedBy("this")
        private final ModifiableSetHistory<Transaction> uncommittedReaders;

        @GuardedBy("this")
        private final ModifiableSetHistory<Transaction> uncommittedWriters;

        @NonNull
        @GuardedBy("this")
        private Duration latestCommit;

        ObjectData(Duration whenCreated, ObjectState createdState, Transaction creator) {
            synchronized (this) {
                stateHistory = new ModifiableValueHistory<>();
                stateHistory.appendTransition(whenCreated, createdState);
                uncommittedReaders = new ModifiableSetHistory<>();
                uncommittedWriters = new ModifiableSetHistory<>();
                uncommittedWriters.addFrom(whenCreated, creator);
                latestCommit = ValueHistory.START_OF_TIME;
            }
        }

        private synchronized void commit1Writer(Transaction transaction, Duration when, ObjectState state) {
            assert latestCommit.compareTo(when) < 0;
            if (state != null) {
                latestCommit = when;
            } else {// destruction is forever
                latestCommit = ValueHistory.END_OF_TIME;
            }
            uncommittedWriters.remove(transaction);
            assert stateHistory.getTransitionTimes().contains(when);
        }

        private synchronized Duration getLastCommit() {
            return latestCommit;
        }

        private synchronized ValueHistory<ObjectState> getStateHistory() {
            return new ModifiableValueHistory<>(stateHistory);
        }

        private synchronized void removeUncommittedReader(Transaction transaction) {
            uncommittedReaders.remove(transaction);
        }

        private synchronized boolean rollBackWrite(Transaction transaction, Duration when) {
            if (latestCommit.compareTo(when) < 0 && uncommittedWriters.contains(transaction).get(when)) {
                stateHistory.removeTransitionsFrom(when);
            }
            // else aborting because of an out-of-order write
            uncommittedWriters.remove(transaction);// optimisation
            return stateHistory.isEmpty();
        }

        private synchronized boolean tryToAppendToHistory(Transaction transaction, UUID object, Duration when,
                ObjectState state, Set<Transaction> transactionsToAbort,
                Set<Transaction> pastTheEndReadersToEscalateToSuccessors) throws IllegalStateException {
            assert when != null;
            if (when.compareTo(latestCommit) <= 0) {
                // Another transaction has committed a write that invalidates this transaction.
                throw new IllegalStateException("when before last commit");
            }
            if (state != null && !stateHistory.isEmpty() && stateHistory.getLastValue() == null) {
                // Not added to uncommittedWriters.
                throw new IllegalStateException("Attempted resurrection of a dead object");
            }

            final Duration lastTransition0 = stateHistory.getLastTansitionTime();
            assert lastTransition0 == null || latestCommit.compareTo(lastTransition0) <= 0;

            if (state != null && stateHistory.getFirstTansitionTime().equals(when)
                    && Boolean.TRUE.equals(uncommittedWriters.contains(transaction).get(when))
                    && state.equals(stateHistory.get(when))) {
                // The given transaction is the creator of the object
                return true;
            } else {// appending
                assert lastTransition0 != null;
                stateHistory.appendTransition(when, state);// throws IllegalStateException
                assert lastTransition0.compareTo(when) < 0;

                uncommittedWriters.addFrom(when, transaction);
                // We have replaced the value written by these other transactions.
                transactionsToAbort.addAll(uncommittedReaders.get(when));

                /*
                 * Because when must be after lastTransition0, we know that lastTransition0 is
                 * not at the end of time, so we can compute a point in time just after
                 * lastTransition0 without danger of overflow.
                 */
                final Duration justAfterPreviousTransition = lastTransition0.plusNanos(1);
                pastTheEndReadersToEscalateToSuccessors.addAll(uncommittedReaders.get(justAfterPreviousTransition));
            }
            return false;
        }
    }// class

    /**
     * <p>
     * An exception for indicating that the {@linkplain ObjectState state} of an
     * object can not be determined for a given point in time because that point in
     * time is before the {@linkplain Universe#getHistoryStart() start of history}.
     * </p>
     */
    public static final class PrehistoryException extends IllegalStateException {

        private static final long serialVersionUID = 1L;

        private PrehistoryException() {
            super("Point in time is before the start of history");
            // Do nothing
        }

    }

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
    public final class Transaction extends Lockable implements AutoCloseable {

        @NonNull
        private final TransactionListener listener;

        /*
         * May acquire a lock on this.lock while have a lock on a
         * TransactionCoordinator, but not vice versa.
         */

        private final Map<ObjectStateId, ObjectState> objectStatesRead = new HashMap<>();

        @GuardedBy("lock")
        private final Map<UUID, ObjectState> objectStatesWritten = new HashMap<>();
        @GuardedBy("lock")
        private final Map<UUID, ObjectStateId> dependencies = new HashMap<>();

        // Must be appended to and committed before this transaction.
        @GuardedBy("lock")
        private final Set<UUID> pastTheEndReads = new HashSet<>();

        @Nullable
        @GuardedBy("lock")
        private Duration when;

        /*
         * openness == ABORTED || openness == COMMITTED ||
         * transactionCoordinator.mutualTransactions.contains(this)
         */
        @NonNull
        @GuardedBy("lock")
        private TransactionCoordinator transactionCoordinator;

        @NonNull
        @GuardedBy("lock")
        private TransactionOpenness openness = TransactionOpenness.READING;

        private Transaction(@NonNull Long id, @NonNull TransactionListener listener) {
            super(id);
            this.listener = Objects.requireNonNull(listener, "listener");
            synchronized (lock) {
                transactionCoordinator = createTransactionCoordinator(this);
            }
        }

        /**
         * <p>
         * Begin aborting this transaction, aborting it if possible.
         * </p>
         * <ul>
         * <li>If this transaction {@linkplain #getOpenness() was}
         * {@linkplain Universe.TransactionOpenness#COMMITTED committed}, it remains
         * committed.</li>
         * <li>If this transaction {@linkplain #getOpenness() was}
         * {@linkplain Universe.TransactionOpenness#ABORTED aborted}, it remains
         * committed.</li>
         * <li>The transaction {@linkplain #getOpenness() is}
         * {@linkplain Universe.TransactionOpenness#ABORTED aborted},
         * {@linkplain Universe.TransactionOpenness#ABORTING aborting} or
         * {@linkplain Universe.TransactionOpenness#COMMITTED committed}.</li>
         * <li>The method rolls back any {@linkplain #put(UUID, ObjectState) writes} the
         * transaction has performed.</li>
         * </ul>
         */
        public final void beginAbort() {
            withLockedTransactionChain(() -> openness.beginAbort(this));
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
         * @param onCommit
         *            An action to perform when (if) this transaction successfully
         *            completes the commit operation.
         * @param onAbort
         *            An action to perform when (if) this transaction aborts the commit
         *            operation.
         * @throws IllegalStateException
         *             If {@linkplain #didBeginCommit() committing this transaction has
         *             already begun}.
         */
        public final void beginCommit() {
            withLockedTransactionChain(() -> openness.beginCommit(this));
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
         * @throws IllegalArgumentException
         *             If {@code when} is the {@linkplain ValueHistory#START_OF_TIME
         *             start of time}.
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
            if (ValueHistory.START_OF_TIME.equals(when)) {
                throw new IllegalArgumentException("May not write at the start of time");
            }
            getOpenness().beginWrite(this, when);
        }

        /**
         * <p>
         * Ensure that this transaction is either {@linkplain #beginAbort() aborted} or
         * (eventually) committed.
         * </p>
         * <ul>
         * <li>The method removes any unnecessary hidden references to this transaction
         * object, so the transaction object can be garbage collected.</li>
         * <li>The method ensures that any transactions dependent on this transaction
         * can also eventually abort or commit.</li>
         * <li>This transaction {@linkplain #getOpenness() is}
         * {@linkplain Universe.TransactionOpenness#ABORTED aborted}
         * {@linkplain Universe.TransactionOpenness#COMMITTING committing} or
         * {@linkplain Universe.TransactionOpenness#COMMITTED committed}.</li>
         * <li>If this transaction {@linkplain #getOpenness() was}
         * {@linkplain Universe.TransactionOpenness#ABORTED aborted} it remains
         * aborted.</li>
         * <li>If this transaction {@linkplain #getOpenness() was}
         * {@linkplain Universe.TransactionOpenness#COMMITTING committing} it is still
         * committing.</li>
         * <li>If this transaction {@linkplain #getOpenness() was}
         * {@linkplain Universe.TransactionOpenness#COMMITTED committed} it remains
         * committed.</li>
         * <li>This transaction {@linkplain #getOpenness() is} (now)
         * {@linkplain Universe.TransactionOpenness#COMMITTED committed} only if it was
         * already committed.</li>
         * </ul>
         */
        @Override
        public final void close() {
            withLockedTransactionChain(() -> openness.close(this));
        }

        @GuardedBy("lock")
        private void commit() {
            assert pastTheEndReads.isEmpty();
            assert openness == TransactionOpenness.COMMITTING;
            openness = TransactionOpenness.COMMITTED;
            for (var entry : objectStatesWritten.entrySet()) {
                final UUID object = entry.getKey();
                final ObjectState state = entry.getValue();
                assert object != null;
                assert when != null;

                objectDataMap.get(object).commit1Writer(this, when, state);
            }

            noLongerAnUncommittedReader();
            lockables.remove(id);
            listener.onCommit();
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
            synchronized (lock) {
                return new HashMap<>(dependencies);
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
         * @throws PrehistoryException
         *             If this transaction is in read mode, and {@code when} is
         *             {@linkplain Duration#compareTo(Duration) before} the
         *             {@linkplain Universe#getHistoryStart() start of history} of the
         *             {@linkplain #getUniverse() universe} of this transaction.
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
            final ObjectStateId id = new ObjectStateId(object, when);
            @Nullable
            ObjectState objectState;
            final Set<Transaction> additionalPredecessors = new HashSet<>();
            objectState = objectStatesRead.get(id);
            if (objectState == null && !objectStatesRead.containsKey(id)) {
                // Value is not cached
                objectState = getOpenness().readUncachedObjectState(this, id, additionalPredecessors);
            }
            // else used cached value
            for (var transaction : additionalPredecessors) {
                Universe.addPredecessor(transaction, this);
            }
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
            return new HashMap<>(objectStatesRead);
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
            synchronized (lock) {
                return new HashMap<>(objectStatesWritten);
            }
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
            synchronized (lock) {
                return openness;
            }
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
            synchronized (lock) {
                return when;
            }
        }

        @GuardedBy("transaction chain")
        private boolean mayCommit() {
            assert Thread.holdsLock(lock);
            assert transactionCoordinator.mutualTransactions.contains(this);
            return openness == TransactionOpenness.COMMITTING && pastTheEndReads.isEmpty();
        }

        @GuardedBy("lock")
        private void noLongerAnUncommittedReader() {
            final Set<UUID> objects;
            objects = dependencies.keySet();
            for (UUID object : objects) {
                var od = objectDataMap.get(object);
                if (od != null) {
                    od.removeUncommittedReader(this);
                }
                // else a non existent object or a prehistoric dependency
            }
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
         *            to exist at the given time. Destroyed objects may not be
         *            resurrected. Therefore the object state will remain null for all
         *            subsequent points in time.
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
            if (getOpenness().put(this, object, state)) {
                listener.onCreate(object);
            }
        }

        @GuardedBy("transaction chain")
        private void reallyAbort() {
            reallyBeginAbort();

            openness = TransactionOpenness.ABORTED;
            // Help the garbage collector:
            pastTheEndReads.clear();

            listener.onAbort();

        }

        @GuardedBy("transaction chain")
        private void reallyBeginAbort() {
            assert openness != TransactionOpenness.ABORTED;
            openness = TransactionOpenness.ABORTING;
            noLongerAnUncommittedReader();
            rollBackWrites();

            /*
             * Cause our dependents to also begin aborting, rather than awaiting commit or
             * close of this transaction.
             */
            transactionCoordinator.beginAbort();
        }

        @GuardedBy("transaction chain")
        private void reallyBeginCommit() {
            assert transactionCoordinator.mutualTransactions.contains(this);
            openness = TransactionOpenness.COMMITTING;
            transactionCoordinator.commitIfPossible();
        }

        private void reallyBeginWrite(@NonNull Duration when) {
            if (objectStatesRead.keySet().stream().map(id -> id.getWhen()).filter(t -> 0 <= t.compareTo(when)).findAny()
                    .orElse(null) != null) {
                throw new IllegalStateException("Time-stamp of read state at or after the given time.");
            }
            synchronized (lock) {
                this.when = when;
                openness = TransactionOpenness.WRITING;
            }
        }

        private @Nullable ObjectState reallyReadUncachedObjectState(@NonNull ObjectStateId id, boolean addTriggers,
                Set<Transaction> additionalPredecessors) {
            final UUID object = id.getObject();
            final Duration when = id.getWhen();

            if (when.compareTo(getHistoryStart()) < 0) {
                throw new Universe.PrehistoryException();
            }

            @Nullable
            final ObjectState objectState;
            boolean isPastTheEndRead = false;
            final var od = objectDataMap.get(object);
            if (od == null) {// unknown object
                objectState = null;
            } else {
                synchronized (od) {
                    objectState = od.stateHistory.get(when);
                    if (addTriggers && od.latestCommit.compareTo(when) < 0) {
                        // Is reading an uncommitted state.
                        if (od.stateHistory.getLastTansitionTime().compareTo(when) < 0) {
                            isPastTheEndRead = true;
                        } else {
                            @NonNull
                            final Duration nextWrite = od.stateHistory.getTansitionTimeAtOrAfter(when);
                            additionalPredecessors.addAll(od.uncommittedWriters.get(nextWrite));
                        }
                        od.uncommittedReaders.addUntil(when, this);
                        additionalPredecessors.addAll(od.uncommittedWriters.get(when));
                    }
                }
            }

            objectStatesRead.put(id, objectState);

            synchronized (lock) {
                if (isPastTheEndRead) {
                    pastTheEndReads.add(object);
                }

                final ObjectStateId dependency0 = dependencies.get(object);
                if (dependency0 == null || when.compareTo(dependency0.getWhen()) < 0) {
                    dependencies.put(object, id);
                }
                assert dependencies.containsKey(object);
            }
            return objectState;
        }

        private void recordObjectStateWritten(UUID object, ObjectState state) {
            synchronized (lock) {
                assert when != null;
                objectStatesWritten.put(object, state);
            }
        }

        @GuardedBy("lock")
        private void rollBackWrites() {
            for (UUID object : objectStatesWritten.keySet()) {
                assert object != null;
                assert when != null;
                /*
                 * As we are a writer for the object, the object has at least one recorded state
                 * (the state we wrote), so objectDataMap.get(object) is guaranteed to be not
                 * null.
                 */
                if (objectDataMap.get(object).rollBackWrite(this, when)) {
                    objectDataMap.remove(object);
                }
            }
        }

        private boolean tryToAppendToHistory(UUID object, ObjectState state) {
            assert when != null;

            final Set<Transaction> transactionsToAbort = new HashSet<>();
            final Set<Transaction> pastTheEndReadersToEscalateToSuccessors = new HashSet<>();
            final ObjectData od = objectDataMap.computeIfAbsent(object, (o) -> new ObjectData(when, state, this));
            final boolean created;
            try {
                created = od.tryToAppendToHistory(this, object, when, state, transactionsToAbort,
                        pastTheEndReadersToEscalateToSuccessors);
            } catch (IllegalStateException e) {
                synchronized (lock) {
                    openness = TransactionOpenness.ABORTING;
                }
                // Not added od.uncommittedWriters.
                return false;
            }

            for (Transaction transaction : transactionsToAbort) {
                // We have replaced the value written by this other transaction.
                transaction.beginAbort();
            }
            for (Transaction pastTheEndReader : pastTheEndReadersToEscalateToSuccessors) {
                Universe.addPredecessor(this, pastTheEndReader);
                synchronized (pastTheEndReader.lock) {
                    pastTheEndReader.pastTheEndReads.remove(object);
                }
            }
            return created;
        }

        private boolean withLockedTransactionChain(NavigableSet<Lockable> unlocked, Set<Lockable> chain,
                Runnable runnable) {
            return Universe.withLockedChain(unlocked, chain, () -> {
                final Set<Lockable> required = new HashSet<>();
                addRequiredForLockedChain(required, this, chain);
                return required;
            }, runnable);
        }

        private void withLockedTransactionChain(Runnable runnable) {
            final NavigableSet<Lockable> chain = new TreeSet<>(Collections.reverseOrder());
            chain.add(this);
            while (!withLockedTransactionChain(chain, chain, runnable)) {
                // try again
            }
        }

    }// class

    private final class TransactionCoordinator extends Lockable {
        /*
         * Must be committed before the mutualTransactions. Includes indirect
         * predecessors.
         */
        @NonNull
        @GuardedBy("lock")
        private final Set<TransactionCoordinator> predecessors;

        /*
         * mutualTransactions.isEmpty() for committed TransactionCoordinators.
         */
        @NonNull
        @GuardedBy("lock")
        private final Set<Transaction> mutualTransactions;

        /*
         * Must be committed after the mutualTransactions. Includes indirect successors.
         * successors.isEmpty() for committed TransactionCoordinators.
         */
        @NonNull
        @GuardedBy("lock")
        private final Set<TransactionCoordinator> successors;

        TransactionCoordinator(@NonNull Long id, @NonNull Transaction transaction) {
            super(id);
            synchronized (lock) {
                predecessors = new HashSet<>();
                mutualTransactions = new HashSet<>();
                mutualTransactions.add(transaction);
                successors = new HashSet<>();
            }
        }

        @GuardedBy("transaction chain")
        private final void beginAbort() {
            for (var predecessor : predecessors) {
                predecessor.successors.remove(this);
            }
            for (var transaction : mutualTransactions) {
                transaction.beginAbort();
            }
            for (var successor : successors) {
                successor.beginAbort();
            }
        }

        @GuardedBy("transaction chain")
        private void commitIfPossible() {
            assert Thread.holdsLock(lock);
            if (mayCommit()) {
                assert predecessors.isEmpty();
                for (Transaction transaction : mutualTransactions) {
                    assert transaction.transactionCoordinator == this;
                    assert Thread.holdsLock(transaction.lock);
                    transaction.commit();
                }
                mutualTransactions.clear();
                for (var successor : successors) {
                    assert Thread.holdsLock(successor.lock);
                    // This is no longer blocking the successor.
                    successor.predecessors.remove(this);
                }
                for (var successor : successors) {
                    assert successor != this;
                    successor.commitIfPossible();
                }
                successors.clear();
                lockables.remove(id);
            }
        }

        @GuardedBy("lock")
        private Set<TransactionCoordinator> getCycles() {
            assert Thread.holdsLock(lock);
            final Set<TransactionCoordinator> cycles = new HashSet<>(predecessors);
            cycles.retainAll(successors);
            return cycles;
        }

        @GuardedBy("transaction chain")
        private boolean mayCommit() {
            assert Thread.holdsLock(lock);
            assert !successors.contains(this);
            assert !predecessors.contains(this);
            if (!predecessors.isEmpty()) {
                return false;
            }
            for (var transaction : mutualTransactions) {
                if (!transaction.mayCommit()) {
                    return false;
                }
            } // for
            return true;
        }

    }// class

    /**
     * <p>
     * An object that can respond to {@link Universe.Transaction} events.
     * </p>
     */
    @ThreadSafe
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

        /**
         * <p>
         * An action to perform when (if) a transaction creates a new object.
         * </p>
         * <p>
         * The method can be called before the transaction commits.
         * </p>
         * 
         * @param object
         *            The object created by the transaction.
         * @throws NullPointerException
         *             (Optionally) if {@code object} is null.
         * @throws IllegalStateException
         *             (Optionally) if this method of listener has previously been
         *             called for the same transaction and object.
         */
        public void onCreate(@NonNull UUID object);

    }// interface

    /**
     * <p>
     * The degree to which a {@link Universe.Transaction} can be said to be
     * <dfn>open</dfn>.
     * </p>
     */
    public enum TransactionOpenness {
        /*
         * In addition to its public interface, this enum also acts as a Strategy for
         * how to handle most transaction mutations.
         */

        /**
         * <p>
         * The transaction is <dfn>open</dfn> and may (successfully)
         * {@linkplain Universe.Transaction#getObjectState(UUID, Duration) read object
         * states}.
         * </p>
         */
        READING {

            @GuardedBy("transaction chain of transaction")
            @Override
            void beginAbort(Transaction transaction) {
                transaction.reallyBeginAbort();
            }

            @GuardedBy("transaction chain of transaction")
            @Override
            void beginCommit(Transaction transaction) {
                transaction.reallyBeginCommit();
            }

            @Override
            void beginWrite(Transaction transaction, @NonNull Duration when) {
                transaction.reallyBeginWrite(when);
            }

            @GuardedBy("transaction chain")
            @Override
            void close(Universe.Transaction transaction) {
                transaction.reallyAbort();
            }

            @Override
            boolean put(Transaction transaction, @NonNull UUID object, @Nullable ObjectState state) {
                throw new IllegalStateException("Not in writing mode");
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(Universe.Transaction transaction, ObjectStateId id,
                    Set<Transaction> additionalPredecessors) {
                return transaction.reallyReadUncachedObjectState(id, true, additionalPredecessors);
            }
        },

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
        WRITING {

            @GuardedBy("transaction chain of transaction")
            @Override
            void beginAbort(Transaction transaction) {
                transaction.reallyBeginAbort();
            }

            @GuardedBy("transaction chain of transaction")
            @Override
            void beginCommit(Transaction transaction) {
                transaction.reallyBeginCommit();
            }

            @Override
            void beginWrite(Transaction transaction, @NonNull Duration when) {
                throw new IllegalStateException("Already writing");
            }

            @GuardedBy("transaction chain")
            @Override
            void close(Universe.Transaction transaction) {
                transaction.reallyAbort();
            }

            @Override
            boolean put(Transaction transaction, @NonNull UUID object, @Nullable ObjectState state) {
                transaction.recordObjectStateWritten(object, state);
                return transaction.tryToAppendToHistory(object, state);
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(Universe.Transaction transaction, ObjectStateId id,
                    Set<Transaction> additionalPredecessors) {
                throw new IllegalStateException("Already writing");
            }
        },
        /**
         * <p>
         * The transaction is <dfn>open</dfn> and has started committing.
         * </p>
         */
        COMMITTING {

            @GuardedBy("transaction chain of transaction")
            @Override
            void beginAbort(Transaction transaction) {
                transaction.reallyAbort();
            }

            @Override
            void beginCommit(Transaction transaction) {
                throw new IllegalStateException("Already began");
            }

            @Override
            void beginWrite(Transaction transaction, @NonNull Duration when) {
                throw new IllegalStateException("Already committing");
            }

            @Override
            void close(Universe.Transaction transaction) {
                // Do nothing
            }

            @Override
            boolean put(Transaction transaction, @NonNull UUID object, @Nullable ObjectState state) {
                throw new IllegalStateException("Commiting");
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(Universe.Transaction transaction, ObjectStateId id,
                    Set<Transaction> additionalPredecessors) {
                throw new IllegalStateException("Already committing");
            }
        },
        /**
         * <p>
         * The transaction is <dfn>open</dfn> and has started aborting.
         * </p>
         */
        ABORTING {

            @Override
            void beginAbort(Transaction transaction) {
                // Do nothing
            }

            @GuardedBy("transaction chain")
            @Override
            void beginCommit(Transaction transaction) {
                transaction.reallyAbort();
            }

            @Override
            void beginWrite(Transaction transaction, @NonNull Duration when) {
                // Do nothing
            }

            @GuardedBy("transaction chain")
            @Override
            void close(Universe.Transaction transaction) {
                transaction.reallyAbort();
            }

            @Override
            boolean put(Transaction transaction, @NonNull UUID object, @Nullable ObjectState state) {
                transaction.recordObjectStateWritten(object, state);
                // Do not change the object state history, however.
                return false;
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(Universe.Transaction transaction, ObjectStateId id,
                    Set<Transaction> additionalPredecessors) {
                // Refrain from adding additional read dependencies.
                return transaction.reallyReadUncachedObjectState(id, false, additionalPredecessors);
            }
        },

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
        COMMITTED {

            @Override
            void beginAbort(Transaction transaction) {
                throw new IllegalStateException("Committed");
            }

            @Override
            void beginCommit(Transaction transaction) {
                throw new IllegalStateException("Committed");
            }

            @Override
            void beginWrite(Transaction transaction, @NonNull Duration when) {
                throw new IllegalStateException("Committed");
            }

            @Override
            void close(Universe.Transaction transaction) {
                // Do nothing
            }

            @Override
            boolean put(Transaction transaction, @NonNull UUID object, @Nullable ObjectState state) {
                throw new IllegalStateException("Committed");
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(Universe.Transaction transaction, ObjectStateId id,
                    Set<Transaction> additionalPredecessors) {
                throw new IllegalStateException("Committed");
            }
        },
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
        ABORTED {

            @Override
            void beginAbort(Transaction transaction) {
                // Do nothing
            }

            @Override
            void beginCommit(Transaction transaction) {
                // Do nothing
            }

            @Override
            void beginWrite(Transaction transaction, @NonNull Duration when) {
                throw new IllegalStateException("Aborted");
            }

            @Override
            void close(Universe.Transaction transaction) {
                // Do nothing
            }

            @Override
            boolean put(Transaction transaction, @NonNull UUID object, @Nullable ObjectState state) {
                throw new IllegalStateException("Aborted");
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(Universe.Transaction transaction, ObjectStateId id,
                    Set<Transaction> additionalPredecessors) {
                throw new IllegalStateException("Aborted");
            }
        };

        @GuardedBy("transaction chain of transaction")
        abstract void beginAbort(Universe.Transaction transaction);

        @GuardedBy("transaction chain of transaction")
        abstract void beginCommit(Universe.Transaction transaction);

        abstract void beginWrite(Transaction transaction, @NonNull Duration when);

        @GuardedBy("transaction chain of transaction")
        abstract void close(Universe.Transaction transaction);

        /*
         * @returns Whether object created.
         */
        abstract boolean put(Transaction transaction, @NonNull UUID object, @Nullable ObjectState state);

        abstract @Nullable ObjectState readUncachedObjectState(Universe.Transaction transaction, ObjectStateId id,
                Set<Transaction> additionalPredecessors);
    }// enum

    private static final ValueHistory<ObjectState> EMPTY_STATE_HISTORY = new ConstantValueHistory<>((ObjectState) null);

    private static void addPredecessor(@NonNull final Transaction predecessor, @NonNull final Transaction successor) {
        withLockedChain2(predecessor, successor, () -> {
            addPredecessor(predecessor.transactionCoordinator, successor.transactionCoordinator);
        });
    }

    @GuardedBy("predecessor transaction chain, successor transaction chain")
    private static void addPredecessor(@NonNull final TransactionCoordinator predecessor,
            @NonNull final TransactionCoordinator successor) {
        assert Thread.holdsLock(predecessor.lock);
        assert Thread.holdsLock(successor.lock);
        if (predecessor == successor) {
            // May not be both predecessor and successor; already recorded as mutual
        } else if (successor.predecessors.contains(predecessor)) {
            // Already done
        } else if (successor.successors.contains(predecessor) || predecessor.predecessors.contains(successor)) {
            // Must merge
            final TransactionCoordinator source;
            final TransactionCoordinator destination;
            if (predecessor.compareTo(successor) < 0) {
                destination = predecessor;
                source = successor;
            } else {
                destination = successor;
                source = predecessor;
            }
            Set<TransactionCoordinator> sources = new HashSet<>();
            sources.add(source);

            merge(destination, sources);
            /* Merging can remove predecessors, and thus make committing possible. */
            for (var p : destination.predecessors) {
                p.commitIfPossible();
            }
            destination.commitIfPossible();
        } else {
            successor.predecessors.add(predecessor);
            successor.predecessors.addAll(predecessor.predecessors);
            predecessor.successors.add(successor);
            predecessor.successors.addAll(successor.successors);
        }
    }

    private static void addRequiredForLockedChain(final Set<Lockable> required, Transaction transaction,
            Set<Lockable> chain) {
        required.add(transaction);
        required.add(transaction.transactionCoordinator);
        if (chain.contains(transaction.transactionCoordinator)) {
            required.addAll(transaction.transactionCoordinator.predecessors);
            required.addAll(transaction.transactionCoordinator.mutualTransactions);
            required.addAll(transaction.transactionCoordinator.successors);
            for (var p : transaction.transactionCoordinator.predecessors) {
                if (chain.contains(p)) {
                    required.addAll(p.mutualTransactions);
                    /*
                     * The latter is redundant if the successor and predecessor data is consistent,
                     * but can be needed during merging.
                     */
                    required.addAll(p.predecessors);
                    required.addAll(p.successors);
                }
            }
            for (var s : transaction.transactionCoordinator.successors) {
                if (chain.contains(s)) {
                    required.addAll(s.mutualTransactions);
                    /*
                     * The latter is redundant if the successor and predecessor data is consistent,
                     * but can be needed during merging.
                     */
                    required.addAll(s.predecessors);
                    required.addAll(s.successors);
                }
            }
        }
    }

    @GuardedBy("destination.lock, source.lock")
    private static void copyOrdering(final TransactionCoordinator source, final TransactionCoordinator destination) {
        destination.predecessors.addAll(source.predecessors);
        destination.successors.addAll(source.successors);
        destination.mutualTransactions.addAll(source.mutualTransactions);
    }

    /*
     * Merging can remove predecessors, so merging can make committing possible.
     */
    @GuardedBy("destination transaction chain, sources transaction chains")
    private static void merge(final TransactionCoordinator destination, Set<TransactionCoordinator> sources) {
        assert Thread.holdsLock(destination.lock);
        while (!sources.isEmpty()) {
            assert !sources.contains(destination);
            for (var s : sources) {
                assert Thread.holdsLock(s.lock);
                copyOrdering(s, destination);
            }
            destination.predecessors.removeAll(sources);
            destination.successors.removeAll(sources);
            destination.successors.remove(destination);
            destination.predecessors.remove(destination);
            for (TransactionCoordinator p : destination.predecessors) {
                assert Thread.holdsLock(p.lock);
                p.predecessors.removeAll(sources);
                p.successors.removeAll(sources);
                p.successors.add(destination);
            }
            for (TransactionCoordinator s : destination.successors) {
                assert Thread.holdsLock(s.lock);
                s.predecessors.removeAll(sources);
                s.successors.removeAll(sources);
                s.predecessors.add(destination);
            }
            destination.successors.remove(destination);
            destination.predecessors.remove(destination);

            final Set<TransactionCoordinator> cycles = new HashSet<>(destination.getCycles());
            for (TransactionCoordinator p : destination.predecessors) {
                assert Thread.holdsLock(p.lock);
                cycles.addAll(p.getCycles());
                if (p.predecessors.contains(destination) && p.successors.contains(destination)) {
                    cycles.add(p);
                }
            }
            for (TransactionCoordinator s : destination.successors) {
                assert Thread.holdsLock(s.lock);
                cycles.addAll(s.getCycles());
                if (s.predecessors.contains(destination) && s.successors.contains(destination)) {
                    cycles.add(s);
                }
            }
            cycles.remove(destination);
            sources = cycles;
        }
        for (Transaction transaction : destination.mutualTransactions) {
            assert Thread.holdsLock(transaction.lock);
            transaction.transactionCoordinator = destination;
        }
        assert Collections.disjoint(destination.predecessors, destination.successors);
    }

    private static boolean withLockedChain(NavigableSet<Lockable> unlocked, Set<Lockable> chain,
            Callable<Set<Lockable>> requiredComputor, Runnable runnable) {
        assert chain.containsAll(unlocked);
        if (unlocked.isEmpty()) {
            final Set<Lockable> more;
            try {
                more = requiredComputor.call();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            more.removeAll(chain);
            if (more.isEmpty()) {
                runnable.run();
                return true;
            } else {
                chain.addAll(more);
                return false;
            }
        } else {
            final Lockable first = unlocked.first();
            final NavigableSet<Lockable> remaining = unlocked.tailSet(first, false);
            synchronized (first.lock) {
                return withLockedChain(remaining, chain, requiredComputor, runnable);
            }
        }
    }

    private static boolean withLockedChain2(Transaction transaction1, Transaction transaction2,
            NavigableSet<Lockable> unlocked, Set<Lockable> chain, Runnable runnable) {
        return withLockedChain(unlocked, chain, () -> {
            final Set<Lockable> required = new HashSet<>();
            addRequiredForLockedChain(required, transaction1, chain);
            addRequiredForLockedChain(required, transaction2, chain);
            return required;
        }, runnable);
    }

    private static void withLockedChain2(Transaction transaction1, Transaction transaction2, Runnable runnable) {
        final NavigableSet<Lockable> chain = new TreeSet<>(Collections.reverseOrder());
        chain.add(transaction1);
        chain.add(transaction2);
        while (!withLockedChain2(transaction1, transaction2, chain, chain, runnable)) {
            // try again
        }
    }

    private final Object historyLock = new Object();
    @NonNull
    @GuardedBy("historyLock")
    private Duration historyStart;

    private final Map<UUID, ObjectData> objectDataMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Lockable> lockables = new ConcurrentHashMap<>();
    private final AtomicLong nextTransactionId = new AtomicLong(Long.MIN_VALUE);

    /**
     * <p>
     * Construct an empty universe.
     * </p>
     * <ul>
     * <li>The {@linkplain #getHistoryStart() history start} time-stamp of this
     * universe is the given history start time-stamp.</li>
     * <li>The {@linkplain #getHistoryEnd() history end} time-stamp of this universe
     * is the given history start time-stamp.</li>
     * <li>The {@linkplain #getObjectIds() set of object IDs}
     * {@linkplain Set#isEmpty() is empty}.</li>
     * </ul>
     * 
     * @param historyStart
     *            The earliest point in time for which this universe has a known
     *            {@linkplain ObjectState state} for {@linkplain #getObjectIds() all
     *            the objects} in the universe.
     * @throws NullPointerException
     *             If {@code historyStart} is null
     */
    public Universe(final @NonNull Duration historyStart) {
        Objects.requireNonNull(historyStart, "historyStart");
        synchronized (historyLock) {
            this.historyStart = historyStart;
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
        Objects.requireNonNull(listener, "listener");
        Long id;
        do {
            id = nextTransactionId.getAndIncrement();
        } while (lockables.putIfAbsent(id, new Transaction(id, listener)) != null);
        return (Transaction) lockables.get(id);
    }

    private @NonNull TransactionCoordinator createTransactionCoordinator(@NonNull final Transaction transaction) {
        assert transaction != null;
        Long id;
        do {
            id = nextTransactionId.getAndIncrement();
        } while (lockables.putIfAbsent(id, new TransactionCoordinator(id, transaction)) != null);
        return (TransactionCoordinator) lockables.get(id);
    }

    /**
     * <p>
     * The last point in time for which this universe has a known, correct and
     * {@linkplain TransactionOpenness#COMMITTED committed} {@linkplain ObjectState
     * state} for {@linkplain #getObjectIds() all the objects} in the universe.
     * </p>
     * <ul>
     * <li>Always have a (non null) history end.</li>
     * <li>The history end is {@linkplain Duration#compareTo(Duration) at or after}
     * the {@linkplain #getHistoryStart() history start}.</li>
     * <li>The end of the history of an empty universe (which has no
     * {@linkplain #getObjectIds() objects}) is the
     * {@linkplain ValueHistory#END_OF_TIME end of time}.</li>
     * </ul>
     * 
     * @return the point in time, expressed as the duration since an epoch; not
     *         null.
     */
    public final @NonNull Duration getHistoryEnd() {
        Duration historyEnd = ValueHistory.END_OF_TIME;
        for (var od : objectDataMap.values()) {
            final Duration lastCommmit = od.getLastCommit();
            if (lastCommmit.compareTo(historyEnd) < 0) {
                historyEnd = lastCommmit;
            }
        }
        final Duration start = getHistoryStart();
        return historyEnd.compareTo(start) < 0 ? start : historyEnd;
    }

    /**
     * <p>
     * The earliest point in time for which this universe has a known and correct
     * {@linkplain ObjectState state} for {@linkplain #getObjectIds() all the
     * objects} in the universe.
     * </p>
     * <ul>
     * <li>Always have a (non null) history start.</li>
     * </ul>
     * 
     * @return the point in time, expressed as the duration since an epoch; not
     *         null.
     */
    public final @NonNull Duration getHistoryStart() {
        synchronized (historyLock) {
            return historyStart;
        }
    }

    /**
     * <p>
     * The time-stamp of the last committed
     * {@linkplain ValueHistory#getTransitions() state transition}
     * {@linkplain #getObjectStateHistory(UUID) event} of an object.
     * </p>
     * <ul>
     * <li>An object has a (non null) last committed state time-stamp if, and only
     * if, it is a {@linkplain #getObjectIds() known object}.</li>
     * <li>If an object is known, its last committed state time-stamp is one of the
     * {@linkplain ValueHistory#getTransitionTimes() transition times} of the
     * {@linkplain #getObjectStateHistory(UUID) state history} of that object, or is
     * the {@linkplain ValueHistory#START_OF_TIME start of time}, or is the
     * {@linkplain ValueHistory#END_OF_TIME end of time}.</li>
     * </ul>
     * 
     * @param object
     *            The ID of the object of interest.
     * @return The time-stamp of the last committed state time-stamp of the object
     *         with {@code object} as its ID, or null if {@code object} is not a
     *         {@linkplain #getObjectIds() known object ID}.
     */
    public final @Nullable Duration getLatestCommit(@NonNull UUID object) {
        Objects.requireNonNull(object, "object");
        final var od = objectDataMap.get(object);
        if (od == null) {
            return null;
        } else {
            return od.getLastCommit();
        }
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
     * <li>Returns a (non null) state if the object exists and has a known state at
     * the given point in time.</li>
     * <li>The (non null) state of an object at a given point in time is one of the
     * states (values) in the {@linkplain #getObjectStateHistory(UUID) state
     * history} of the object.</li>
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
        return getObjectStateHistory(object).get(when);
    }

    /**
     * <p>
     * The (currently known) history of the state transitions (succession of object
     * states) of a given object in this universe.
     * </p>
     * <ul>
     * <li>A universe always has a (non null) object state history for a given
     * object.</li>
     * <li>The object state history for a given object is not
     * {@linkplain ValueHistory#isEmpty() empty} only if the object is one of the
     * {@linkplain #getObjectIds() known objects} in this universe..</li>
     * <li>Only the {@linkplain ValueHistory#getLastValue() last value} in a (non
     * null) object state history may be a null state (indicating that the object
     * ceased to exist at that time).</li>
     * <li>An object state history may record values before the
     * {@linkplain #getHistoryStart() start of history}, but those records may be
     * incomplete. In particular, the object state history may indicate that the
     * object did not exist (has a null state) for points in time at which it
     * actually existed.</li>
     * <li>An object state history may record
     * {@linkplain ValueHistory#getTransitions() state transitions} after the
     * {@linkplain #getHistoryEnd() end of history}, but those transitions might be
     * transitions {@linkplain Transaction#put(UUID, ObjectState) written} by
     * transactions that have not yet been {@linkplain TransactionOpenness#COMMITTED
     * committed}, and so could be rolled-back.</li>
     * <li>An object state history may record null values
     * {@linkplain ValueHistory#get(Duration) for} points in time, which indicates
     * that the object does not exist (or is not known to exist, for points in time
     * before the {@linkplain #getHistoryStart() start of history}) at that point in
     * time.</li>
     * <li>An object state history indicates that the object does not exist (has a
     * null state) {@linkplain ValueHistory#getFirstValue() at the start of
     * time}.</li>
     * <li>The method may a return a copy of the object state history, rather than a
     * reference to the true object state history.</li>
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
    public final @NonNull ValueHistory<ObjectState> getObjectStateHistory(@NonNull UUID object) {
        Objects.requireNonNull(object, "object");
        final var od = objectDataMap.get(object);
        if (od == null) {
            return EMPTY_STATE_HISTORY;
        } else {
            return od.getStateHistory();
        }
    }

    /**
     * <p>
     * The time-stamp of the {@linkplain ValueHistory#getFirstTansitionTime() first}
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
        return getObjectStateHistory(object).getFirstTansitionTime();
    }

    /**
     * <p>
     * Change the {@linkplain #getHistoryStart() earliest point in time} for which
     * this universe has a known and correct {@linkplain ObjectState state} for
     * {@linkplain #getObjectIds() all the objects} in the universe.
     * </p>
     * <ul>
     * <li>The {@linkplain #getHistoryStart() history start time} of this universe
     * is (becomes) {@linkplain Duration#equals(Object) equal to} the given history
     * start time.</li>
     * </ul>
     * 
     * @param historyStart
     *            the point in time, expressed as the duration since an epoch.
     * 
     * @throws NullPointerException
     *             If {@code historyStart} is null.
     * @throws IllegalArgumentException
     *             If {@code historyStart} is before the
     *             {@linkplain #getHistoryStart() current history start}
     * @throws IllegalStateException
     *             If {@code historyStart} is after {@linkplain #getHistoryEnd() the
     *             history end time}.
     */
    public final void setHistoryStart(@NonNull Duration historyStart) {
        Objects.requireNonNull(historyStart, "historyStart");
        synchronized (historyLock) {
            if (historyStart.compareTo(this.historyStart) < 0) {
                throw new IllegalArgumentException("Before current history start");
            }
            if (getHistoryEnd().compareTo(historyStart) < 0) {
                throw new IllegalStateException("After current history end");
            }
            if (this.historyStart.equals(historyStart)) {
                // Optimisation
                return;
            }
            this.historyStart = historyStart;
        }
    }
}
