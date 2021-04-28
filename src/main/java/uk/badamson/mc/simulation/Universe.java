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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
 * The state histories of the objects may be <dfn>asynchronous</dfn>: different
 * objects may have state transitions at different times.</li>
 * <p>
 * This collection is modifiable: the state histories of the simulated objects
 * may be appended to. This collection enforces constraints that ensure that the
 * object state histories are <dfn>consistent</dfn>. Consistency means that if a
 * universe contains an object state, it also contains all the depended upon
 * states of that state, unless those states are {@linkplain #getHistoryStart()
 * too old}.
 * </p>
 * <p>
 * This collection enables safe multi-threaded modification by using
 * {@linkplain Universe.Transaction transactions}. Each transaction corresponds
 * to one event of the simulated system.
 * </p>
 */
@ThreadSafe
public class Universe {

    abstract class Lockable implements Comparable<Lockable> {

        /*
         * If locks are to be held on multiple Lockable objects, the locks must be
         * acquired in the natural ordering of the objects. That is, in ascending order
         * of their ids.
         */
        @NonNull
        final Long id;

        final Object lock = new Object();

        protected Lockable(@NonNull final Long id) {
            assert id != null;
            this.id = id;
        }

        abstract void addRequiredForLockedChain(final Set<Lockable> required, Set<Lockable> chain);

        @Override
        public final int compareTo(final Lockable that) {
            return id.compareTo(that.id);
        }

        @Override
        public final boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Lockable)) {
                return false;
            }
            final Lockable other = (Lockable) obj;
            return getUniverse().equals(other.getUniverse()) && id.equals(other.id);
        }

        final @NonNull Universe getUniverse() {
            return Universe.this;
        }

        @Override
        public final int hashCode() {
            return id.hashCode();
        }
    }// class

    final class ObjectData extends Lockable {

        @GuardedBy("lock")
        private final ModifiableValueHistory<ObjectState> stateHistory;

        @GuardedBy("lock")
        private final ModifiableSetHistory<Transaction> uncommittedReaders;

        @GuardedBy("lock")
        private final ModifiableSetHistory<Transaction> uncommittedWriters;

        @NonNull
        @GuardedBy("lock")
        private Duration latestCommit;

        private ObjectData(final Long id, final Duration whenCreated, final ObjectState createdState,
                final Transaction creator) {
            super(id);
            synchronized (lock) {
                stateHistory = new ModifiableValueHistory<>();
                stateHistory.appendTransition(whenCreated, createdState);
                uncommittedReaders = new ModifiableSetHistory<>();
                uncommittedWriters = new ModifiableSetHistory<>();
                uncommittedWriters.addFrom(whenCreated, creator);
                latestCommit = ValueHistory.START_OF_TIME;
            }
        }

        @Override
        void addRequiredForLockedChain(final Set<Lockable> required, final Set<Lockable> chain) {
            if (!required.contains(this)) {
                required.add(this);
                if (chain.contains(this)) {
                    assert Thread.holdsLock(lock);
                    for (final var r : uncommittedReaders.getUniverse()) {
                        r.addRequiredForLockedChain(required, chain);
                    }
                    for (final var w : uncommittedWriters.getUniverse()) {
                        w.addRequiredForLockedChain(required, chain);
                    }
                }
            }
            // else infinite recursion possible
        }

        private void commit1Writer(final Transaction transaction, @NonNull final Duration when,
                final ObjectState state) {
            synchronized (lock) {
                assert latestCommit.compareTo(when) < 0;
                if (state != null) {
                    latestCommit = when;
                } else {// destruction is forever
                    latestCommit = ValueHistory.END_OF_TIME;
                }
                uncommittedWriters.remove(transaction);
                assert stateHistory.getTransitionTimes().contains(when);
            }
        }

        private Duration getLastCommit() {
            synchronized (lock) {
                return latestCommit;
            }
        }

        private ValueHistory<ObjectState> getStateHistory() {
            synchronized (lock) {
                return new ModifiableValueHistory<>(stateHistory);
            }
        }

        private void prunePrehistory(final Duration when) {
            synchronized (lock) {
                final SortedSet<Duration> prehistoricTransitionTimes = stateHistory.getTransitionTimes().headSet(when);
                if (1 < prehistoricTransitionTimes.size()) {
                    final Duration removalTime = prehistoricTransitionTimes.last().minusNanos(1);
                    stateHistory.setValueUntil(removalTime, null);
                }
            }
        }

        @GuardedBy("lock")
        private void removeUncommittedReader(final Transaction transaction) {
            assert Thread.holdsLock(lock);
            uncommittedReaders.remove(transaction);
        }

        @GuardedBy("lock")
        private boolean rollBackWrite(final Transaction transaction, @NonNull final Duration when) {
            if (latestCommit.compareTo(when) < 0 && uncommittedWriters.contains(transaction).get(when).booleanValue()) {
                stateHistory.removeTransitionsFrom(when);
            }
            // else aborting because of an out-of-order write
            uncommittedWriters.remove(transaction);// optimisation
            return stateHistory.isEmpty();
        }

        @GuardedBy("lock")
        private boolean tryToAppendToHistory(final Transaction transaction, @NonNull final Duration when,
                @Nullable final ObjectState state, @NonNull final Set<Transaction> transactionsToAbort,
                @NonNull final Set<Transaction> pastTheEndReadersToEscalateToSuccessors) throws IllegalStateException {
            assert when != null;
            assert transactionsToAbort != null;
            assert pastTheEndReadersToEscalateToSuccessors != null;
            assert Thread.holdsLock(lock);

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
                @NonNull
                final Duration justAfterPreviousTransition = lastTransition0.plusNanos(1);
                pastTheEndReadersToEscalateToSuccessors.addAll(uncommittedReaders.get(justAfterPreviousTransition));
            }
            return false;
        }

    }// class

    /**
     * <p>
     * A transaction for changing the state of a {@link Universe}.
     * </p>
     * <p>
     * A transaction is a record of a {@linkplain #getObjectStatesRead() collection
     * of reads of} and {@linkplain #getObjectStatesWritten() writes to} the state
     * histories of the Universe that can be committed as an atomic operation. A
     * transaction however may not interleave reads and writes. All its writes must
     * be after any reads, after having {@linkplain #beginWrite(Duration) entered}
     * <i>write mode</i>.
     * </p>
     * <p>
     * Each transaction corresponds to one event of the simulated system. The writes
     * of the transaction are the state changes that occur as a result of the event.
     * The reads of the transaction establish the causality constraints of the
     * event.
     * </p>
     * <p>
     * These transactions are <i>optimistic</i>: a transaction is allowed to perform
     * operations that it may transpire can not be
     * {@linkplain Universe.TransactionOpenness#COMMITTED committed}, in which case
     * it will be {@linkplain Universe.TransactionOpenness#ABORTED aborted}.
     * </p>
     * <p>
     * These transactions are <i>non-blocking</i>: a client indicating that it wants
     * to commit a transaction (by calling {@link #beginCommit()}) does not block in
     * that method call until the transaction has
     * {@linkplain Universe.TransactionOpenness#COMMITTED committed} (or
     * {@linkplain Universe.TransactionOpenness#ABORTED aborted}). Instead a
     * call-back to a {@linkplain TransactionListener listener} is used to indicate
     * the transaction is {@linkplain TransactionListener#onCommit() committed} (or
     * {@linkplain TransactionListener#onAbort() aborted}). However, these
     * transactions are not lock free: callers to the methods of this class may
     * block for short periods while waiting to acquire locks.
     * </p>
     * <p>
     * This class does not have a publicly accessible constructor; use the
     * {@link Universe#beginTransaction(TransactionListener)} method to create new
     * transaction objects.
     * </p>
     * <p>
     * Although transactions may be used directly, in practice it is better to use a
     * {@link SimulationEngine} to create transactions. Transactions with
     * unsatisfied read dependencies will remain uncommitted until some other
     * transaction(s) satisfy them. And aborted transactions need to be restarted.
     * The {@link SimulationEngine} takes care of those complications for you.
     * </p>
     */
    @NotThreadSafe
    public final class Transaction extends Lockable implements AutoCloseable {

        @NonNull
        private final TransactionListener listener;

        @GuardedBy("lock")
        @NonNull
        private final Map<ObjectStateId, ObjectState> objectStatesRead;

        @GuardedBy("lock")
        @NonNull
        private final Map<UUID, ObjectState> objectStatesWritten;

        // Must be appended to and committed before this transaction.
        @GuardedBy("lock")
        @NonNull
        final Map<UUID, ObjectData> pastTheEndReads;

        @Nullable
        @GuardedBy("lock")
        private Duration when;

        /*
         * assert transactionCoordinator.mutualTransactions.contains(this)
         */
        @NonNull
        @GuardedBy("lock")
        TransactionCoordinator transactionCoordinator;

        @NonNull
        @GuardedBy("lock")
        private TransactionOpenness openness;

        private Transaction(@NonNull final Long id, @NonNull final TransactionListener listener) {
            super(id);
            this.listener = Objects.requireNonNull(listener, "listener");
            synchronized (lock) {
                objectStatesRead = new HashMap<>();
                objectStatesWritten = new HashMap<>();
                pastTheEndReads = new HashMap<>();
                when = null;
                transactionCoordinator = createTransactionCoordinator(this);
                openness = TransactionOpenness.READING;
            }
        }

        @Override
        void addRequiredForLockedChain(final Set<Lockable> required, final Set<Lockable> chain) {
            if (!required.contains(this)) {
                required.add(this);
                if (chain.contains(this)) {
                    assert Thread.holdsLock(lock);
                    transactionCoordinator.addRequiredForLockedChain(required, chain);
                    final Universe universe = getUniverse();
                    for (final ObjectStateId id : objectStatesRead.keySet()) {
                        final ObjectData od = universe.objectDataMap.get(id.getObject());
                        if (od != null) {
                            od.addRequiredForLockedChain(required, chain);
                        }
                    } // for
                    for (final UUID object : objectStatesWritten.keySet()) {
                        final ObjectData od = universe.objectDataMap.get(object);
                        if (od != null) {
                            od.addRequiredForLockedChain(required, chain);
                        }
                    }
                }
            }
        }

        /**
         * <p>
         * Begin aborting this transaction, aborting it if possible.
         * </p>
         * <ul>
         * <li>If this transaction {@linkplain #getOpenness() was}
         * {@linkplain Universe.TransactionOpenness#ABORTED aborted}, it remains
         * committed.</li>
         * <li>The transaction {@linkplain #getOpenness() is}
         * {@linkplain Universe.TransactionOpenness#ABORTED aborted},
         * {@linkplain Universe.TransactionOpenness#ABORTING aborting} or
         * {@linkplain Universe.TransactionOpenness#COMMITTED committed}.</li>
         * <li>The method rolls back any {@linkplain #put(UUID, ObjectState) writes} the
         * transaction has performed.</li>
         * <li>This method must be called from the same thread that
         * {@linkplain Universe#beginTransaction(TransactionListener) began} this
         * transaction.</li>
         * </ul>
         *
         * @throws IllegalStateException
         *             If this transaction has already been
         *             {@linkplain Universe.TransactionOpenness#COMMITTED committed}. In
         *             practice, that means this method should not be called after a
         *             request to {@linkplain #beginCommit() begin committing} this
         *             transaction.
         */
        public void beginAbort() {
            withLockedTransactionChain(() -> openness.beginAbort(this));
            executeAwaitingAbortCallbacks();
        }

        /**
         * <p>
         * Begin completion of this transaction, completing it if possible.
         * </p>
         * <ul>
         * <li>The transaction {@linkplain #getOpenness() is} not (anymore)
         * {@linkplain Universe.TransactionOpenness#READING reading} or
         * {@linkplain Universe.TransactionOpenness#WRITING writing}.</li>
         * <li>On return from the method, or subsequently (and perhaps asynchronously),
         * this transaction will become
         * {@linkplain Universe.TransactionOpenness#COMMITTED committed} or
         * {@linkplain Universe.TransactionOpenness#ABORTED aborted}.</li>
         * <li>This method must be called from the same thread that
         * {@linkplain Universe#beginTransaction(TransactionListener) began} this
         * transaction.</li>
         * </ul>
         *
         * @throws IllegalStateException
         *             If committing this transaction has already begun.
         */
        public void beginCommit() {
            withLockedTransactionChain(() -> openness.beginCommit(this));
            executeAwaitingCommitCallbacks();
            executeAwaitingAbortCallbacks();
        }

        /**
         * <p>
         * Change this transaction from read mode to write mode.
         * </p>
         * <ul>
         * <li>The {@linkplain #getWhen() time-stamp of any object states to be written}
         * by this transaction becomes the same as the given time-stamp.</li>
         * <li>This method must be called from the same thread that
         * {@linkplain Universe#beginTransaction(TransactionListener) began} this
         * transaction.</li>
         * </ul>
         *
         * @param when
         *            The time-stamp of all object states to be
         *            {@linkplain #put(UUID, ObjectState) put} (written) by this
         *            transaction, expressed as the duration since an (implied) epoch.
         *
         * @throws NullPointerException
         *             If {@code when} is null.
         * @throws IllegalArgumentException
         *             If {@code when} is the {@linkplain ValueHistory#START_OF_TIME
         *             start of time}.
         * @throws IllegalStateException
         *             <ul>
         *             <li>If the any of the {@linkplain #getObjectStatesRead() reads}
         *             done by this transaction were for
         *             {@linkplain ObjectStateId#getWhen() times} at of after the given
         *             time.</li>
         *             <li>If this transaction is already in write mode. That is, if
         *             this method has already been called for this transaction.</li>
         *             </ul>
         */
        public void beginWrite(@NonNull final Duration when) {
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
         * object, so the transaction object can eventually be garbage collected, if the
         * client no longer has references to it.</li>
         * <li>The method ensures that any transactions dependent on this transaction
         * can also eventually abort or commit.</li>
         * <li>This transaction {@linkplain #getOpenness() is}
         * {@linkplain Universe.TransactionOpenness#ABORTED aborted},
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
        public void close() {
            withLockedTransactionChain(() -> openness.close(this));
            executeAwaitingAbortCallbacks();
        }

        @GuardedBy("lock")
        private void commit() {
            assert Thread.holdsLock(lock);
            assert pastTheEndReads.isEmpty();
            assert openness == TransactionOpenness.COMMITTING;

            openness = TransactionOpenness.COMMITTED;
            for (final var entry : objectStatesWritten.entrySet()) {
                final UUID object = entry.getKey();
                final ObjectState state = entry.getValue();
                assert object != null;
                assert when != null;

                objectDataMap.get(object).commit1Writer(this, when, state);
            }

            noLongerAnUncommittedReader();
            lockables.remove(id);
            awaitingCommitCallbacks.add(listener);
        }

        /**
         * <p>
         * Get (read) the state of a given object, of the universe of this transaction,
         * at a given point in time.
         * </p>
         * <ul>
         * <li>The object state for an object ID and point in time is either the same
         * object state as can be {@linkplain Universe#getObjectState(UUID, Duration)
         * got} from the universe of this transaction, or is the same object state as
         * has already been {@linkplain #getObjectStatesRead() got} by this
         * transaction.</li>
         * <li>The object state of for an object ID and point in time that has not
         * already been {@linkplain #getObjectStatesRead() read} by this transaction is
         * the same object state as can be
         * {@linkplain Universe#getObjectState(UUID, Duration) got} from the universe of
         * this transaction.</li>
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
         *            The point in time of interest, expressed as the duration since an
         *            (implied) epoch.
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
         *             <li>If committing this transaction has started (because its
         *             {@link #beginCommit()} method has been called) and the requested
         *             object is not one of the {@linkplain #getObjectStatesRead()
         *             object states already read}.</li>
         *             </ul>
         */
        public @Nullable ObjectState getObjectState(@NonNull final UUID object, @NonNull final Duration when) {
            final ObjectStateId id = new ObjectStateId(object, when);
            @Nullable
            ObjectState objectState;
            final boolean readUncached;
            synchronized (lock) {
                objectState = objectStatesRead.get(id);
                readUncached = objectState == null && !objectStatesRead.containsKey(id);
            }
            if (readUncached) {
                // Value is not cached
                objectState = getOpenness().readUncachedObjectState(this, id);
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
         * <li>The collection of object states read may include object states that have
         * not been committed by the transaction that
         * {@linkplain #put(UUID, ObjectState) wrote} them. Only if this transaction
         * {@linkplain #getOpenness() has} been
         * {@linkplain Universe.TransactionOpenness#COMMITTED committed} is the
         * collection of object states read guaranteed to contain only committed
         * values.</li>
         * </ul>
         *
         * @return the object states read.
         * @see Universe#getObjectState(UUID, Duration)
         */
        public @NonNull Map<ObjectStateId, ObjectState> getObjectStatesRead() {
            synchronized (lock) {
                return new HashMap<>(objectStatesRead);
            }
        }

        /**
         * <p>
         * The object states that have been written as part of this transaction.
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
        public @NonNull Map<UUID, ObjectState> getObjectStatesWritten() {
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
        public TransactionOpenness getOpenness() {
            synchronized (lock) {
                return openness;
            }
        }

        /**
         * <p>
         * The time-stamp of any object states (to be)
         * {@linkplain #put(UUID, ObjectState) written} by this transaction, expressed
         * as the duration since an (implied) epoch.
         * </p>
         *
         * @return the time-stamp, or null if this transaction has not (yet) entered
         *         write mode.
         */
        public @Nullable Duration getWhen() {
            synchronized (lock) {
                return when;
            }
        }

        @GuardedBy("lock")
        private boolean mayCommit() {
            assert Thread.holdsLock(lock);
            return openness == TransactionOpenness.COMMITTING && pastTheEndReads.isEmpty();
        }

        @GuardedBy("transaction chain of this")
        private void noLongerAnUncommittedReader() {
            for (final ObjectStateId id : objectStatesRead.keySet()) {
                final var od = objectDataMap.get(id.getObject());
                if (od != null) {
                    od.removeUncommittedReader(this);
                }
                // else a non existent object or a prehistoric dependency
            }
        }

        /**
         * <p>
         * Try to add (write) a state transition (or an initial state) for an object to
         * the universe of this transaction.
         * <p>
         * <ul>
         * <li>The method records the given state as one of the
         * {@linkplain #getObjectStatesWritten() states written} of this
         * transaction.</li>
         * <li>This method is <dfn>optimistic</dfn>. It assumes that this transaction
         * will be successfully committed, so any other transactions that have read the
         * old state of the object at the {@linkplain #getWhen() time} of this write (or
         * a later time) are now invalid and must be aborted.</li>
         * <li>This method must be called from the same thread that
         * {@linkplain Universe#beginTransaction(TransactionListener) began} this
         * transaction and which {@linkplain #beginWrite(Duration) changed it to write
         * mode}.</li>
         * </ul>
         *
         * @param object
         *            The ID of the object that has the given state transition at the
         *            given time.
         * @param state
         *            The state of the object just after the state transition, at the
         *            {@linkplain #getWhen() write time} of this transaction. A null
         *            value indicates that the object ceases to exist at the write time
         *            of this transaction.
         * @throws NullPointerException
         *             If {@code object} is null.
         * @throws IllegalStateException
         *             <ul>
         *             <li>If this transaction is not in write mode (because its
         *             {@link #beginWrite(Duration)} method has not been called).</li>
         *             <li>If committing this transaction has been started (because its
         *             {@link #beginCommit()} method has been called).</li>
         *             </ul>
         */
        public void put(@NonNull final UUID object, @Nullable final ObjectState state) {
            Objects.requireNonNull(object, "object");
            if (getOpenness().put(this, object, state)) {
                listener.onCreate(object);
            }
            executeAwaitingAbortCallbacks();
        }

        @GuardedBy("transaction chain")
        private void reallyAbort() {
            assert Thread.holdsLock(lock);
            reallyBeginAbort();

            openness = TransactionOpenness.ABORTED;
            // Help the garbage collector:
            pastTheEndReads.clear();

            lockables.remove(id);
            awaitingAbortCallbacks.add(listener);

        }

        @GuardedBy("transaction chain")
        private void reallyBeginAbort() {
            assert Thread.holdsLock(lock);
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
            assert Thread.holdsLock(lock);
            assert transactionCoordinator.mutualTransactions.contains(this);

            openness = TransactionOpenness.COMMITTING;
            if (!pastTheEndReads.isEmpty()) {
                // Optimization: can not commit
                return;
            }
            transactionCoordinator.commitIfPossible();
        }

        private void reallyBeginWrite(@NonNull final Duration when) {
            synchronized (lock) {
                if (objectStatesRead.keySet().stream().map(id -> id.getWhen()).filter(t -> 0 <= t.compareTo(when))
                        .findAny().orElse(null) != null) {
                    throw new IllegalStateException("Time-stamp of read state at or after the given time.");
                }
                this.when = when;
                openness = TransactionOpenness.WRITING;
            }
        }

        private @Nullable ObjectState reallyReadUncachedObjectStateWhileAborting(@NonNull final ObjectStateId id) {
            final UUID object = id.getObject();
            final Duration when = id.getWhen();

            if (when.compareTo(getHistoryStart()) < 0) {
                throw new PrehistoryException();
            }

            final var od = objectDataMap.get(object);
            @Nullable
            final ObjectState objectState;
            if (od == null) {// unknown object
                objectState = null;
            } else {
                synchronized (od.lock) {
                    objectState = od.stateHistory.get(when);
                }
            }

            synchronized (lock) {
                assert openness == TransactionOpenness.ABORTING;
                objectStatesRead.put(id, objectState);
            }
            return objectState;
        }

        private @Nullable ObjectState reallyReadUncachedObjectStateWhileReading(@NonNull final ObjectStateId id) {
            final UUID object = id.getObject();
            final Duration when = id.getWhen();
            if (when.compareTo(getHistoryStart()) < 0) {
                throw new PrehistoryException();
            }

            final var od = objectDataMap.get(object);
            if (od == null) {// unknown object
                synchronized (lock) {
                    objectStatesRead.put(id, null);
                }
                return null;
            } else {
                final AtomicReference<ObjectState> objectState = new AtomicReference<>();
                Universe.withLockedChain2(this, od, () -> {
                    objectState.set(reallyReadUncachedObjectStateWhileReading1(id, od));
                });
                return objectState.get();
            }
        }

        @GuardedBy("trasaction chain of this")
        private @Nullable ObjectState reallyReadUncachedObjectStateWhileReading1(@NonNull final ObjectStateId id,
                @NonNull final ObjectData od) {
            assert Thread.holdsLock(lock);
            assert Thread.holdsLock(od.lock);
            assert openness == TransactionOpenness.READING;

            final UUID object = id.getObject();
            final Duration when = id.getWhen();
            final Set<Transaction> additionalPredecessors = new HashSet<>();
            boolean isPastTheEndRead = false;
            final ObjectState objectState = od.stateHistory.get(when);
            if (od.latestCommit.compareTo(when) < 0) {
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

            objectStatesRead.put(id, objectState);

            assert openness == TransactionOpenness.READING;
            if (isPastTheEndRead) {
                pastTheEndReads.put(object, od);
            }

            for (final var predecessor : additionalPredecessors) {
                assert Thread.holdsLock(predecessor.lock);
                assert predecessor.transactionCoordinator.mutualTransactions.contains(predecessor);
                assert transactionCoordinator.mutualTransactions.contains(this);

                Universe.addPredecessor(predecessor.transactionCoordinator, transactionCoordinator);
                /*
                 * this is still READING, and has acquired an additional predecessor or mutual
                 * transaction, so committing it should be impossible.
                 */
                assert !mayCommit();
                assert !(predecessor.mayCommit() && predecessor.transactionCoordinator.mayCommit());
            }

            return objectState;
        }

        private void recordObjectStateWritten(final UUID object, final ObjectState state) {
            synchronized (lock) {
                assert when != null;
                objectStatesWritten.put(object, state);
            }
        }

        @GuardedBy("transaction chain")
        private void rollBackWrites() {
            assert Thread.holdsLock(lock);
            for (final UUID object : objectStatesWritten.keySet()) {
                assert object != null;
                assert when != null;
                /*
                 * As we are a writer for the object, the object has at least one recorded state
                 * (the state we wrote), so objectDataMap.get(object) is guaranteed to be not
                 * null.
                 */
                final ObjectData od = objectDataMap.get(object);
                assert Thread.holdsLock(od.lock);
                if (od.rollBackWrite(this, when)) {
                    objectDataMap.remove(object);
                    lockables.remove(od.id);
                }
            }
        }

        @Override
        public String toString() {
            return "Transaction [" + id + "," + getOpenness() + "]";
        }

        private boolean tryToAppendToHistory(final UUID object, final ObjectState state) {
            final ObjectData od;
            synchronized (lock) {
                od = objectDataMap.computeIfAbsent(object, (o) -> createObjectData(when, state, this));
            }
            final AtomicBoolean result = new AtomicBoolean(false);

            final NavigableSet<Lockable> chain = new TreeSet<>();
            addRequiredForLockedChain(chain, Collections.emptySet());
            od.addRequiredForLockedChain(chain, Collections.emptySet());
            while (!withLockedTransactionChain(chain, chain, () -> {
                result.set(tryToAppendToHistory1(object, state));
                return;
            })) {
                // try again
            }
            return result.get();
        }

        private boolean tryToAppendToHistory1(final UUID object, final ObjectState state) {
            assert Thread.holdsLock(lock);
            assert when != null;

            final Set<Transaction> transactionsToAbort = new HashSet<>();
            final Set<Transaction> pastTheEndReadersToEscalateToSuccessors = new HashSet<>();
            final ObjectData od = objectDataMap.get(object);
            assert Thread.holdsLock(od.lock);
            final boolean created;
            try {
                created = od.tryToAppendToHistory(this, when, state, transactionsToAbort,
                        pastTheEndReadersToEscalateToSuccessors);
            } catch (final IllegalStateException e) {
                openness = TransactionOpenness.ABORTING;
                // Not added od.uncommittedWriters.
                return false;
            }

            for (final Transaction transaction : transactionsToAbort) {
                // We have replaced the value written by this other transaction.
                assert Thread.holdsLock(transaction.lock);
                transaction.openness.beginAbort(transaction);
            }
            for (final Transaction pastTheEndReader : pastTheEndReadersToEscalateToSuccessors) {
                assert Thread.holdsLock(lock);
                assert Thread.holdsLock(pastTheEndReader.lock);
                assert transactionCoordinator.mutualTransactions.contains(this);
                assert pastTheEndReader.transactionCoordinator.mutualTransactions.contains(pastTheEndReader);
                assert openness == TransactionOpenness.WRITING;

                Universe.addPredecessor(transactionCoordinator, pastTheEndReader.transactionCoordinator);
                pastTheEndReader.pastTheEndReads.remove(object);

                /*
                 * this transaction is WRITING, so committing it should be impossible. this is
                 * now a predecessor or a mutualTransaction with the pastTheEndReader, so
                 * committing it should be impossible too.
                 */
                assert !mayCommit();
                assert !(pastTheEndReader.mayCommit() && pastTheEndReader.transactionCoordinator.mayCommit());
            }
            return created;

        }

        private boolean withLockedTransactionChain(final NavigableSet<Lockable> unlocked, final Set<Lockable> chain,
                final Runnable runnable) {
            return Universe.withLockedChain(unlocked, chain, () -> {
                final Set<Lockable> required = new HashSet<>();
                addRequiredForLockedChain(required, chain);
                return required;
            }, runnable);
        }

        private void withLockedTransactionChain(final Runnable runnable) {
            final NavigableSet<Lockable> chain = new TreeSet<>();
            addRequiredForLockedChain(chain, Collections.emptySet());
            while (!withLockedTransactionChain(chain, chain, runnable)) {
                // try again
            }
        }
    }// class

    final class TransactionCoordinator extends Lockable {
        /*
         * Must be committed before the mutualTransactions. Includes indirect
         * predecessors.
         *
         * assert !(predecesors.contains(p1) && p1.predecessors.contains(p2) &&
         * !predecessors.contains(p2));
         */
        @NonNull
        @GuardedBy("lock")
        final Set<TransactionCoordinator> predecessors;

        /*
         * assert !(mutualTransactions.contains(t) && t.transactionCoordiantor != this);
         */
        @NonNull
        @GuardedBy("lock")
        final Set<Transaction> mutualTransactions;

        /*
         * Must be committed after the mutualTransactions. Includes indirect successors.
         * successors.isEmpty() for committed TransactionCoordinators.
         *
         * assert !(successors.contains(s1) && s1.successors.contains(sp2) &&
         * !successors.contains(s2));
         */
        @NonNull
        @GuardedBy("lock")
        final Set<TransactionCoordinator> successors;

        private TransactionCoordinator(@NonNull final Long id, @NonNull final Transaction transaction) {
            super(id);
            synchronized (lock) {
                predecessors = new HashSet<>();
                mutualTransactions = new HashSet<>();
                mutualTransactions.add(transaction);
                successors = new HashSet<>();
            }
        }

        @Override
        void addRequiredForLockedChain(final Set<Lockable> required, final Set<Lockable> chain) {
            if (!required.contains(this)) {
                required.add(this);
                if (chain.contains(this)) {
                    assert Thread.holdsLock(lock);
                    for (final var p : predecessors) {
                        p.addRequiredForLockedChain(required, chain);
                    }
                    // mutualTransactions.isEmpty() permitted
                    for (final var t : mutualTransactions) {
                        t.addRequiredForLockedChain(required, chain);
                    }
                    for (final var s : successors) {
                        s.addRequiredForLockedChain(required, chain);
                    }
                }
            }
            // else infinite recursion possible
        }

        @GuardedBy("transaction chain")
        private void beginAbort() {
            assert Thread.holdsLock(lock);
            for (final var predecessor : predecessors) {
                assert Thread.holdsLock(predecessor.lock);
                predecessor.successors.remove(this);
            }
            for (final var transaction : mutualTransactions) {
                assert Thread.holdsLock(transaction.lock);
                assert transaction.transactionCoordinator == this;

                transaction.openness.beginAbort(transaction);
            }
            for (final var successor : successors) {
                successor.beginAbort();
            }
        }

        private void clear() {
            predecessors.clear();
            mutualTransactions.clear();
            successors.clear();
            lockables.remove(id);
        }

        @GuardedBy("transaction chain")
        private void commit() {
            assert predecessors.isEmpty();
            for (final Transaction transaction : mutualTransactions) {
                assert Thread.holdsLock(transaction.lock);
                assert transaction.transactionCoordinator == this;

                transaction.commit();
            }
            for (final var successor : successors) {
                assert Thread.holdsLock(successor.lock);
                // This is no longer blocking the successor:
                successor.predecessors.remove(this);
            }
            for (final var successor : successors) {
                assert successor != this;
                successor.commitIfPossible();
            }
            clear();
        }

        @GuardedBy("transaction chain")
        private void commitIfPossible() {
            assert Thread.holdsLock(lock);
            if (mayCommit()) {
                commit();
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
            assert !mutualTransactions.isEmpty();// else dangling reference

            if (!predecessors.isEmpty()) {
                return false;
            }
            for (final var transaction : mutualTransactions) {
                assert Thread.holdsLock(transaction.lock);
                assert transaction.transactionCoordinator == this;

                if (!transaction.mayCommit()) {
                    return false;
                }
            } // for
            return true;
        }

        @Override
        public String toString() {
            return "TransactionCoordinator [" + id + "]";
        }
    }// class

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
            void beginAbort(final Transaction transaction) {
                transaction.reallyBeginAbort();
            }

            @GuardedBy("transaction chain of transaction")
            @Override
            void beginCommit(final Transaction transaction) {
                transaction.reallyBeginCommit();
            }

            @Override
            void beginWrite(final Transaction transaction, @NonNull final Duration when) {
                transaction.reallyBeginWrite(when);
            }

            @GuardedBy("transaction chain")
            @Override
            void close(final Universe.Transaction transaction) {
                transaction.reallyAbort();
            }

            @Override
            boolean put(final Transaction transaction, @NonNull final UUID object, @Nullable final ObjectState state) {
                throw new IllegalStateException("Not in writing mode");
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(final Universe.Transaction transaction, final ObjectStateId id) {
                return transaction.reallyReadUncachedObjectStateWhileReading(id);
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
            void beginAbort(final Transaction transaction) {
                transaction.reallyBeginAbort();
            }

            @GuardedBy("transaction chain of transaction")
            @Override
            void beginCommit(final Transaction transaction) {
                transaction.reallyBeginCommit();
            }

            @Override
            void beginWrite(final Transaction transaction, @NonNull final Duration when) {
                throw new IllegalStateException("Already writing");
            }

            @GuardedBy("transaction chain")
            @Override
            void close(final Universe.Transaction transaction) {
                transaction.reallyAbort();
            }

            @Override
            boolean put(final Transaction transaction, @NonNull final UUID object, @Nullable final ObjectState state) {
                transaction.recordObjectStateWritten(object, state);
                return transaction.tryToAppendToHistory(object, state);
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(final Universe.Transaction transaction, final ObjectStateId id) {
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
            void beginAbort(final Transaction transaction) {
                transaction.reallyAbort();
            }

            @Override
            void beginCommit(final Transaction transaction) {
                throw new IllegalStateException("Already began");
            }

            @Override
            void beginWrite(final Transaction transaction, @NonNull final Duration when) {
                throw new IllegalStateException("Already committing");
            }

            @Override
            void close(final Universe.Transaction transaction) {
                // Do nothing
            }

            @Override
            boolean put(final Transaction transaction, @NonNull final UUID object, @Nullable final ObjectState state) {
                throw new IllegalStateException("Commiting");
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(final Universe.Transaction transaction, final ObjectStateId id) {
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
            void beginAbort(final Transaction transaction) {
                // Do nothing
            }

            @GuardedBy("transaction chain")
            @Override
            void beginCommit(final Transaction transaction) {
                transaction.reallyAbort();
            }

            @Override
            void beginWrite(final Transaction transaction, @NonNull final Duration when) {
                // Do nothing
            }

            @GuardedBy("transaction chain")
            @Override
            void close(final Universe.Transaction transaction) {
                transaction.reallyAbort();
            }

            @Override
            boolean put(final Transaction transaction, @NonNull final UUID object, @Nullable final ObjectState state) {
                transaction.recordObjectStateWritten(object, state);
                // Do not change the object state history, however.
                return false;
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(final Universe.Transaction transaction, final ObjectStateId id) {
                return transaction.reallyReadUncachedObjectStateWhileAborting(id);
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
            void beginAbort(final Transaction transaction) {
                throw new IllegalStateException("Committed");
            }

            @Override
            void beginCommit(final Transaction transaction) {
                throw new IllegalStateException("Committed");
            }

            @Override
            void beginWrite(final Transaction transaction, @NonNull final Duration when) {
                throw new IllegalStateException("Committed");
            }

            @Override
            void close(final Universe.Transaction transaction) {
                // Do nothing
            }

            @Override
            boolean put(final Transaction transaction, @NonNull final UUID object, @Nullable final ObjectState state) {
                throw new IllegalStateException("Committed");
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(final Universe.Transaction transaction, final ObjectStateId id) {
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
            void beginAbort(final Transaction transaction) {
                // Do nothing
            }

            @Override
            void beginCommit(final Transaction transaction) {
                // Do nothing
            }

            @Override
            void beginWrite(final Transaction transaction, @NonNull final Duration when) {
                throw new IllegalStateException("Aborted");
            }

            @Override
            void close(final Universe.Transaction transaction) {
                // Do nothing
            }

            @Override
            boolean put(final Transaction transaction, @NonNull final UUID object, @Nullable final ObjectState state) {
                throw new IllegalStateException("Aborted");
            }

            @Override
            @Nullable
            ObjectState readUncachedObjectState(final Universe.Transaction transaction, final ObjectStateId id) {
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

        abstract @Nullable ObjectState readUncachedObjectState(Universe.Transaction transaction, ObjectStateId id);
    }// enum

    private static final ValueHistory<ObjectState> EMPTY_STATE_HISTORY = new ConstantValueHistory<ObjectState>(
            (ObjectState) null);

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
            final Set<TransactionCoordinator> sources = new HashSet<>();
            sources.add(source);

            merge(destination, sources);
        } else {
            successor.predecessors.add(predecessor);
            successor.predecessors.addAll(predecessor.predecessors);
            for (final var s : successor.successors) {
                s.predecessors.add(predecessor);
                assert Collections.disjoint(s.predecessors, s.successors);
            }
            predecessor.successors.add(successor);
            predecessor.successors.addAll(successor.successors);
            for (final var p : predecessor.predecessors) {
                p.successors.add(successor);
                assert Collections.disjoint(p.predecessors, p.successors);
            }
        }
    }

    @GuardedBy("destination transaction chain, sources transaction chains")
    private static void merge(final TransactionCoordinator destination, Set<TransactionCoordinator> sources) {
        assert Thread.holdsLock(destination.lock);
        assert !destination.mutualTransactions.isEmpty();// else dangling reference

        while (!sources.isEmpty()) {
            assert !sources.contains(destination);
            for (final var source : sources) {
                assert Thread.holdsLock(source.lock);
                assert !source.mutualTransactions.isEmpty();// else dangling reference
                destination.predecessors.addAll(source.predecessors);
                destination.successors.addAll(source.successors);
                destination.mutualTransactions.addAll(source.mutualTransactions);
            }
            destination.predecessors.removeAll(sources);
            destination.successors.removeAll(sources);
            destination.successors.remove(destination);
            destination.predecessors.remove(destination);
            for (final TransactionCoordinator p : destination.predecessors) {
                assert Thread.holdsLock(p.lock);
                if (!Collections.disjoint(sources, p.predecessors)) {
                    p.predecessors.removeAll(sources);
                    p.predecessors.add(destination);
                    // Will later remove p as a cycle.
                }
                p.successors.removeAll(sources);
                p.successors.add(destination);
                // p.predecessors.isEmpty() is possible
                assert !p.mayCommit();
            }
            for (final TransactionCoordinator s : destination.successors) {
                assert Thread.holdsLock(s.lock);
                if (!Collections.disjoint(sources, s.successors)) {
                    s.successors.removeAll(sources);
                    s.successors.add(destination);
                    // Will later remove s as a cycle.
                }
                s.predecessors.removeAll(sources);
                s.predecessors.add(destination);
                assert !s.predecessors.isEmpty();// hence can not commit
            }
            destination.successors.remove(destination);
            destination.predecessors.remove(destination);
            for (final var source : sources) {
                source.clear();
            }
            /*
             * destination.predecessors.isEmpty() is possible, but in that case committing
             * it will still not be possible
             */

            final Set<TransactionCoordinator> cycles = new HashSet<>(destination.getCycles());
            for (final TransactionCoordinator p : destination.predecessors) {
                assert Thread.holdsLock(p.lock);
                cycles.addAll(p.getCycles());
                if (p.predecessors.contains(destination) && p.successors.contains(destination)) {
                    cycles.add(p);
                }
            }
            for (final TransactionCoordinator s : destination.successors) {
                assert Thread.holdsLock(s.lock);
                cycles.addAll(s.getCycles());
                if (s.predecessors.contains(destination) && s.successors.contains(destination)) {
                    cycles.add(s);
                }
            }
            cycles.remove(destination);
            sources = cycles;
        }
        for (final Transaction transaction : destination.mutualTransactions) {
            assert Thread.holdsLock(transaction.lock);
            transaction.transactionCoordinator = destination;
        }
        assert Collections.disjoint(destination.predecessors, destination.successors);
        assert !destination.mayCommit();
    }

    private static boolean withLockedChain(final NavigableSet<Lockable> unlocked, final Set<Lockable> chain,
            final Callable<Set<Lockable>> requiredComputor, final Runnable runnable) {
        assert chain.containsAll(unlocked);
        if (unlocked.isEmpty()) {
            final Set<Lockable> more;
            try {
                more = requiredComputor.call();
            } catch (final Exception e) {
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

    private static boolean withLockedChain2(final Lockable lockable1, final Lockable lockable2,
            final NavigableSet<Lockable> unlocked, final Set<Lockable> chain, final Runnable runnable) {
        return withLockedChain(unlocked, chain, () -> {
            final Set<Lockable> required = new HashSet<>();
            lockable1.addRequiredForLockedChain(required, chain);
            lockable2.addRequiredForLockedChain(required, chain);
            return required;
        }, runnable);
    }

    private static void withLockedChain2(final Lockable lockable1, final Lockable lockable2, final Runnable runnable) {
        final NavigableSet<Lockable> chain = new TreeSet<>();
        lockable1.addRequiredForLockedChain(chain, Collections.emptySet());
        lockable2.addRequiredForLockedChain(chain, Collections.emptySet());
        while (!withLockedChain2(lockable1, lockable2, chain, chain, runnable)) {
            // try again
        }
    }

    private final Object historyLock = new Object();

    @NonNull
    @GuardedBy("historyLock")
    private Duration historyStart;

    private final Map<UUID, ObjectData> objectDataMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Lockable> lockables = new ConcurrentHashMap<>();
    private final AtomicLong nextLockableId = new AtomicLong(Long.MIN_VALUE);
    private final Queue<TransactionListener> awaitingCommitCallbacks = new ConcurrentLinkedQueue<>();
    private final Queue<TransactionListener> awaitingAbortCallbacks = new ConcurrentLinkedQueue<>();

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
     * <li>The returned transaction {@linkplain Map#isEmpty() has not}
     * {@linkplain Universe.Transaction#getObjectStatesRead() read any object
     * states}.</li>
     * <li>The returned transaction {@linkplain Map#isEmpty() has not}
     * {@linkplain Universe.Transaction#getObjectStatesWritten() written any object
     * states}.</li>
     * <li>The returned transaction {@linkplain Universe.Transaction#getOpenness()
     * is in} {@linkplain Universe.TransactionOpenness#READING read mode}.</li>
     * </ul>
     *
     * @param listener
     *            The transaction listener to use for this transaction. This
     *            universe will inform the listener of the
     *            {@linkplain TransactionListener#onAbort() abort} or
     *            {@linkplain TransactionListener#onCommit() commit} of the
     *            transaction, and of {@linkplain TransactionListener#onCreate(UUID)
     *            creation} of new objects by the transaction. However, those
     *            call-backs may be made by a thread other than the thread that
     *            began the transaction, and there may be a (short) delay between an
     *            abort and commit and the listener being informed.
     * @return a new transaction object; not null
     * @throws NullPointerException
     *             If {@code listener} is null
     */
    public final @NonNull Transaction beginTransaction(@NonNull final TransactionListener listener) {
        Objects.requireNonNull(listener, "listener");
        return (Transaction) createLockable((id) -> new Transaction(id, listener));
    }

    /**
     * <p>
     * Whether this universe contains an object with a given ID.
     * </p>
     * <ul>
     * <li>This universe contains an object with a given ID if, and only if, the
     * {@linkplain #getObjectIds() set of object IDs that this universe contains}
     * {@linkplain Set#contains(Object) contains} that object ID.</li>
     * <li>This method is likely to be more efficient than using the result of the
     * {@link #getObjectIds()} method.</li>
     * <li>This method may indicate that this universe contains an object before the
     * {@linkplain Universe.Transaction transaction} adding that object to this
     * universe has {@linkplain Universe.TransactionOpenness#COMMITTED committed}
     * the addition. That is, this method may read uncommitted information.</li>
     * </ul>
     *
     * @param object
     *            The ID of the object of interest.
     * @return whether contained.
     * @throws NullPointerException
     *             If {@code object} is null.
     */
    public final boolean containsObject(final UUID object) {
        Objects.requireNonNull(object, "object");
        return objectDataMap.containsKey(object);
    }

    private @NonNull Lockable createLockable(final Function<Long, Lockable> factory) {
        Long id;
        do {
            id = Long.valueOf(nextLockableId.getAndIncrement());
        } while (lockables.putIfAbsent(id, factory.apply(id)) != null);
        return lockables.get(id);
    }

    private @NonNull ObjectData createObjectData(@NonNull final Duration whenCreated,
            @NonNull final ObjectState createdState, @NonNull final Transaction creator) {
        assert whenCreated != null;
        assert createdState != null;
        assert creator != null;
        return (ObjectData) createLockable((id) -> new ObjectData(id, whenCreated, createdState, creator));
    }

    private @NonNull TransactionCoordinator createTransactionCoordinator(@NonNull final Transaction transaction) {
        assert transaction != null;
        return (TransactionCoordinator) createLockable((id) -> new TransactionCoordinator(id, transaction));
    }

    private void executeAwaitingAbortCallbacks() {
        TransactionListener listener = awaitingAbortCallbacks.poll();
        while (listener != null) {
            listener.onAbort();
            listener = awaitingAbortCallbacks.poll();
        }
    }

    private void executeAwaitingCommitCallbacks() {
        TransactionListener listener = awaitingCommitCallbacks.poll();
        while (listener != null) {
            listener.onCommit();
            listener = awaitingCommitCallbacks.poll();
        }
    }

    /**
     * <p>
     * The last point in time for which this universe has a known, correct and
     * {@linkplain TransactionOpenness#COMMITTED committed} {@linkplain ObjectState
     * state} for {@linkplain #getObjectIds() all the objects} in the universe.
     * </p>
     * <p>
     * This corresponds to the <dfn>Global Virtual Time</dfn> value of the <i>Time
     * Warp</i> Parallel Discrete Event Simulation algorithm.
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
     * @return the point in time, expressed as the duration since an (implied)
     *         epoch; not null.
     */
    public final @NonNull Duration getHistoryEnd() {
        Duration historyEnd = ValueHistory.END_OF_TIME;
        for (final var od : objectDataMap.values()) {
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
     * @return the point in time, expressed as the duration since an (implied)
     *         epoch; not null.
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
     * @throws NullPointerException
     *             If {@code object} is null
     */
    public final @Nullable Duration getLatestCommit(@NonNull final UUID object) {
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
     * <li>The set of object IDs might or might not be a copy of an internal set,
     * and so might or might not reflect subsequent changes to this universe.</li>
     * <li>The set of object IDs may include the IDs of objects before the
     * {@linkplain Universe.Transaction transactions} adding those object to this
     * universe have {@linkplain Universe.TransactionOpenness#COMMITTED committed}
     * the additions. That is, this method may read uncommitted information.</li>
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
     * <li>This method may return an object state before the
     * {@linkplain Universe.Transaction transaction} adding that object state has
     * {@linkplain Universe.TransactionOpenness#COMMITTED committed} the addition.
     * That is, this method may read uncommitted information.</li>
     * </ul>
     * <p>
     * To read only committed object states use a
     * {@linkplain Universe.Transaction#getObjectState(UUID, Duration) read}
     * operation in a {@linkplain Universe.Transaction transaction} and wait until
     * that transaction has {@linkplain Universe.TransactionOpenness#COMMITTED
     * committed}. In practice, this is easier using a {@link SimulationEngine} to
     * do an {@linkplain SimulationEngine#computeObjectState(UUID, Duration)
     * asynchronous computation} of the wanted object state.
     * </p>
     *
     * @param object
     *            The ID of the object of interest.
     * @param when
     *            The point in time of interest, expressed as the duration since an
     *            (implied) epoch.
     * @return The state of the given object at the given point in time.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             </ul>
     */
    public final @Nullable ObjectState getObjectState(@NonNull final UUID object, @NonNull final Duration when) {
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
     * {@linkplain #getObjectIds() known objects} in this universe.</li>
     * <li>An object state history may record null values
     * {@linkplain ValueHistory#get(Duration) for} points in time, which indicates
     * that the object does not exist (or is not known to exist, for points in time
     * before the {@linkplain #getHistoryStart() start of history}) at that point in
     * time.</li>
     * <li>The {@linkplain ValueHistory#getLastValue() last value} in a (non null)
     * object state history may be a null state (indicating that the object ceased
     * to exist at that time).</li>
     * <li>An object state history may record values before the
     * {@linkplain #getHistoryStart() start of history}, but those records may be
     * incomplete. In particular, the object state history may indicate that the
     * object did not exist (has a null state) for points in time at which it
     * actually existed.</li>
     * <li>The {@linkplain ValueHistory#getTransitions() transitions} in a (non
     * null) object state history may include at most one transition to a null state
     * (indicating that the object ceased to exist at that time).</li>
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
     * committed}, and so could be rolled-back. That is, this method may read
     * uncommitted information.</li>
     * <li>An object state history indicates that the object does not exist (has a
     * null state) {@linkplain ValueHistory#getFirstValue() at the start of
     * time}.</li>
     * <li>The method may a return a copy of the object state history, rather than a
     * reference to the true object state history.</li>
     * </ul>
     *
     * @param object
     *            The object of interest
     * @return The state history of the given object.
     * @throws NullPointerException
     *             If {@code object} is null
     * @see #getLatestCommit(UUID)
     * @see #getObjectState(UUID, Duration)
     */
    public final @NonNull ValueHistory<ObjectState> getObjectStateHistory(@NonNull final UUID object) {
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
     * @throws NullPointerException
     *             If {@code object} is null
     */
    public final @Nullable Duration getWhenFirstState(@NonNull final UUID object) {
        return getObjectStateHistory(object).getFirstTansitionTime();
    }

    /**
     * <p>
     * Remove unnecessary {@linkplain ObjectState object states} from the
     * {@linkplain #getObjectStateHistory(UUID) state history} of a given object for
     * times before the {@linkplain #getHistoryStart() history start time}.
     * </p>
     * <p>
     * The universe needs to record only one object state before the history start
     * time. If the given object has more than one such state, it removes all except
     * the most recent
     * </p>
     *
     * @param object
     *            The ID of the object of interest.
     * @throws NullPointerException
     *             If {@code object} is null
     */
    public final void prunePrehistory(@NonNull final UUID object) {
        Objects.requireNonNull(object, "object");
        final var od = objectDataMap.get(object);
        if (od != null) {
            od.prunePrehistory(getHistoryStart());
        }
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
     * <p>
     * A {@link Universe} records the full {@linkplain #getObjectStateHistory(UUID)
     * state history} of all its {@linkplain #getObjectIds() objects} between the
     * history start time and the {@linkplain #getHistoryEnd() history end time}. To
     * reduce memory use it is therefore tempting to set the history start time to a
     * time close to the history end time and then
     * {@linkplain #prunePrehistory(UUID) prune the prehistory}. However,
     * transactions will fail (by throwing a {@link PrehistoryException}) if they
     * attempt to read state information before the history start time. Therefore in
     * practice the interval between the history start time and history end time
     * must be kept larger than the furthest back in time that a transaction will
     * read. That is, larger than the largest message transmission delay.
     * </p>
     *
     * @param historyStart
     *            the point in time, expressed as the duration since an (implied)
     *            epoch.
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
    public final void setHistoryStart(@NonNull final Duration historyStart) {
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
