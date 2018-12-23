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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.simulation.Universe.PrehistoryException;

/**
 * <p>
 * Drives a simulation {@linkplain Universe universe} forward to a desired end
 * state.
 * </p>
 */
@ThreadSafe
public final class SimulationEngine {

    /*
     * Drives the simulation forward for one simulation object.
     */
    private final class Engine1 implements Universe.TransactionListener, Runnable {
        @GuardedBy("this")
        private boolean running = false;

        @NonNull
        private final UUID object;

        @GuardedBy("this")
        @NonNull
        private Duration latestCommit = ValueHistory.START_OF_TIME;

        // steps has no null values
        @GuardedBy("this")
        private final NavigableMap<Duration, FutureObjectState> steps = new TreeMap<>();

        @NonNull
        @GuardedBy("this")
        private Duration advanceTo = ValueHistory.START_OF_TIME;

        /*
         * Objects that can not have their state advanced until the object that this
         * advances has advanced. dependentObjects does not contain null
         */
        @GuardedBy("this")
        private final Set<UUID> dependentObjects = new HashSet<>();

        /*
         * Objects that ought to have their states advanced before we try to advance the
         * state of the object that this advances.
         * 
         * The dependencies are weak "ought to" rather than a strong "must" because it
         * is impossible to compute reliable dependency information.
         */
        @GuardedBy("this")
        private final Set<UUID> objectDependencies = new HashSet<>();

        @GuardedBy("this")
        private final Set<UUID> creating = new HashSet<>();

        private Engine1(@NonNull UUID object) {
            assert object != null;
            this.object = object;
            synchronized (this) {
                latestCommit = universe.getLatestCommit(object);
            }
        }

        /*
         * By having the dependencies sorted, we will schedule the dependencies in
         * ascending time order, which will tend to result in fewer aborted
         * transactions. The number of dependencies should be small and not proportional
         * to the number of objects, so the performance to advance the universe to a
         * given point in time should remain O(N).
         */
        private void addDependencies(@NonNull final SortedSet<ObjectStateId> dependencyIds) {
            for (ObjectStateId dependencyId : dependencyIds) {
                final UUID objectDependency = dependencyId.getObject();
                final Duration dependencyWhen = dependencyId.getWhen();
                final Duration dependencyLastestCommit = universe.getLatestCommit(objectDependency);
                if (dependencyLastestCommit.compareTo(dependencyWhen) < 0) {
                    /*
                     * To commit we will need this dependency, but it is not yet committed, so
                     * schedule production of the dependency so we will eventually advance.
                     */
                    addDependency(objectDependency);
                    getEngine1(objectDependency).advanceHistory(dependencyWhen, object);
                }
            }
        }

        private void addDependency(final UUID objectDependency) {
            objectDependencies.add(objectDependency);
        }

        private void advance1() {
            try (final Universe.Transaction transaction = universe.beginTransaction(this)) {
                try {
                    final Duration when = universe.getLatestCommit(object);
                    assert when != null;
                    assert when.compareTo(ValueHistory.END_OF_TIME) < 0;
                    synchronized (this) {
                        latestCommit = when;
                        creating.clear();
                    }
                    final ObjectState state0 = transaction.getObjectState(object, when);
                    assert state0 != null;
                    putNextStateTransition(state0, transaction, when);
                    /*
                     * Before this transaction can be committed, any object states it depends on
                     * must be committed. Ensure those object states will be (eventually) committed.
                     * If this transaction aborts, because it depends on states that have be
                     * overwritten, the dependency information is unreliable. However, in that case
                     * the dependency information is probably partially or approximately correct, so
                     * using it is a good heuristic.
                     */
                    addDependencies(new TreeSet<>(transaction.getObjectStatesRead().keySet()));
                    transaction.beginCommit();
                } catch (Exception | AssertionError e) {
                    // Hard to test: race hazard.
                    completeExceptionally(e);
                    transaction.beginAbort();
                }
            }
        }

        private void advanceHistory(@NonNull final Duration when, @Nullable UUID dependent) {
            assert when != null;
            assert !object.equals(dependent);

            final boolean work;
            synchronized (this) {
                final boolean advanceFurther = advanceTo.compareTo(when) < 0;
                work = !running && latestCommit != null && ValueHistory.START_OF_TIME.compareTo(latestCommit) < 0
                        && latestCommit.compareTo(when) < 0 && advanceFurther && universe.containsObject(object);
                if (advanceFurther) {
                    advanceTo = when;
                }
                if (work) {
                    running = true;
                }
            }
            if (dependent != null) {
                dependentObjects.add(dependent);
            }
            if (work) {
                tryToScheduleAdvance1();
            }
        }

        private void completeCommitableReads() {
            final Collection<FutureObjectState> transactionsToStart;
            synchronized (this) {
                latestCommit = universe.getLatestCommit(object);
                final boolean unknownObject = latestCommit == null;
                final SortedMap<Duration, FutureObjectState> commitableReads = unknownObject ? steps
                        : new TreeMap<>(steps.headMap(latestCommit, true));
                transactionsToStart = new ArrayList<>(commitableReads.size());
                for (var future : commitableReads.values()) {
                    if (!future.isDone()) {
                        transactionsToStart.add(future);
                    }
                }
                steps.keySet().removeAll(commitableReads.keySet());
                assert !(unknownObject && !steps.isEmpty());
            }

            for (FutureObjectState future : transactionsToStart) {
                future.startReadTransaction();
                assert future.isDone();
            }
        }

        private synchronized void completeExceptionally(Throwable e) {
            advanceTo = latestCommit;// cease further work

            for (var step : steps.values()) {
                step.completeExceptionally(e);
            }
            steps.clear();
        }

        private synchronized boolean hasWorkToDo() {
            return latestCommit != null && latestCommit.compareTo(advanceTo) < 0;
        }

        @Override
        public void onAbort() {
            assert latestCommit != null;
            if (hasWorkToDo()) {
                // Try again
                tryToScheduleAdvance1();
            }
            // else abandoning
        }

        @Override
        public void onCommit() {
            final Set<UUID> dependents;
            final Set<UUID> created;
            synchronized (this) {
                assert latestCommit != null;
                /*
                 * As we have committed, we evidently are not awaiting the advance of any other
                 * objects before committing.
                 */
                objectDependencies.clear();
                dependents = new HashSet<>(dependentObjects);
                dependentObjects.clear();
                created = new HashSet<>(creating);
                creating.clear();
            }
            /*
             * Now that we have advanced the simulation, some waiting reads might be able to
             * complete.
             */
            completeCommitableReads();

            if (hasWorkToDo()) {
                // Further work required for the object this engine moves forward.
                tryToScheduleAdvance1();
            } else {
                synchronized (this) {
                    running = false;
                }
            }

            // Some created objects might now need their state to be advanced.
            for (UUID c : created) {
                final Engine1 engine = getEngine1(c);
                final Duration when = universalAdvanceTo;
                engine.advanceHistory(when, null);
            }

            // And some aborted writes could be restarted
            for (UUID dependent : dependents) {
                final Engine1 engine1 = getEngine1(dependent);
                engine1.removeDependency1(object);
            }
        }

        @Override
        public void onCreate(@NonNull UUID createdObject) {
            synchronized (this) {
                creating.add(createdObject);
            }
        }

        private void putNextStateTransition(@NonNull final ObjectState state0,
                @NonNull final Universe.Transaction transaction, @NonNull final Duration when) {
            try {
                state0.putNextStateTransition(transaction, object, when);

                final var objectStatesReadAtOrAfter = transaction.getObjectStatesRead().keySet().stream()
                        .filter(id -> (when.compareTo(id.getWhen()) < 0
                                || (when.equals(id.getWhen()) && !object.equals(id.getObject()))))
                        .collect(Collectors.toSet());
                final Duration whenWritten = transaction.getWhen();
                final Map<UUID, ObjectState> objectStatesWritten = transaction.getObjectStatesWritten();
                if (!objectStatesReadAtOrAfter.isEmpty()) {
                    throw new IllegalStateException(
                            "Read object states at or after the given time " + objectStatesReadAtOrAfter);
                }
                if (whenWritten == null) {
                    throw new IllegalStateException("Did not put the transaction into write mode.");
                } else if (whenWritten.compareTo(when) <= 0) {
                    throw new IllegalStateException("when is not after the given point in time");
                }
                final ObjectState nextState = objectStatesWritten.get(object);
                if (nextState == null && !objectStatesWritten.containsKey(object)) {
                    throw new IllegalStateException("Did not put() a state for the given object");
                } else if (Objects.equals(state0, nextState)) {
                    throw new IllegalStateException("put() a state equal to itself");
                }
            } catch (RuntimeException e) {
                throw new RuntimeException(createPutNextStateTransitionFailureMessage(state0, object, when), e);
            } catch (AssertionError e) {
                throw new AssertionError(createPutNextStateTransitionFailureMessage(state0, object, when), e);
            }
        }

        private synchronized void removeDependency1(final UUID dependedOnObject) {
            assert latestCommit != null;
            objectDependencies.remove(dependedOnObject);
            final boolean readyToAdvance = objectDependencies.isEmpty();
            if (readyToAdvance) {
                // Removed the last remaining dependency, so can (re)try advancing the state
                tryToScheduleAdvance1();
            }
        }

        @Override
        public void run() {
            advance1();
        }

        private FutureObjectState schedule(final ObjectStateId id, UUID dependent) {
            final Duration when = id.getWhen();
            assert object.equals(id.getObject());
            assert !object.equals(dependent);

            final FutureObjectState future;
            synchronized (this) {
                future = steps.computeIfAbsent(when, t -> new FutureObjectState(id));
            }
            advanceHistory(when, dependent);
            completeCommitableReads();
            return future;
        }

        private void tryToScheduleAdvance1() {
            assert universe.containsObject(object);
            try {
                executor.execute(this);
            } catch (RejectedExecutionException e) {
                // Do nothing. Ok to fail because only need to try.
            }
        }

    }

    private class EngineFuture<T> extends CompletableFuture<T> {

        @Override
        public final Executor defaultExecutor() {
            return executor;
        }

        @Override
        public final <U> CompletableFuture<U> newIncompleteFuture() {
            return new EngineFuture<U>();
        }

    }// class

    private final class FutureObjectState extends EngineFuture<ObjectState> {
        private final Object lock = new Object();

        @NonNull
        private final ObjectStateId id;

        @Nullable
        @GuardedBy("lock")
        private ObjectState state;

        private FutureObjectState(@NonNull ObjectStateId id) {
            assert id != null;
            this.id = id;
        }

        private Universe.Transaction createReadTransaction() {
            return universe.beginTransaction(new Universe.TransactionListener() {

                @Override
                public void onAbort() {
                    // Do nothing; the engine will retry
                }

                @Override
                public void onCommit() {
                    complete(getState());
                }

                @Override
                public void onCreate(UUID object) {
                    throw new AssertionError("Read transactions do not create objects");
                }

            });
        }

        @Nullable
        private ObjectState getState() {
            synchronized (lock) {
                return state;
            }
        }

        private void setState(@Nullable ObjectState state) {
            synchronized (lock) {
                this.state = state;
            }
        }

        private void startReadTransaction() {
            try (final Universe.Transaction transaction = createReadTransaction();) {
                try {
                    setState(transaction.getObjectState(id.getObject(), id.getWhen()));
                    transaction.beginCommit();
                } catch (Universe.PrehistoryException e) {
                    completeExceptionally(e);
                } catch (Exception e) {// never happens
                    completeExceptionally(new AssertionError("Unexpected exception from Universe.Transaction", e));
                } catch (AssertionError e) {// never happens
                    completeExceptionally(e);
                }
            }
        }

    }// class

    private static String createPutNextStateTransitionFailureMessage(final ObjectState state0, UUID object,
            final Duration when) {
        return "Failure for " + state0 + " putNextStateTransition, object=" + object + ", when=" + when;
    }

    private final Object lock = new Object();

    @NonNull
    private final Universe universe;

    @NonNull
    private final Executor executor;

    // Hard to test that a concurrent Map.
    private final Map<UUID, Engine1> engines = new ConcurrentHashMap<>();

    @NonNull
    @GuardedBy("lock")
    private Duration universalAdvanceTo = ValueHistory.START_OF_TIME;

    /**
     * <p>
     * Construct a {@link SimulationEngine} with given associations.
     * </p>
     * <ul>
     * <li>This engine has the given universe as its {@linkplain #getUniverse()
     * universe}.</li>
     * <li>This engine has the given executor as its {@linkplain #getExecutor()
     * executor}.</li>
     * </ul>
     * 
     * @param universe
     *            The collection of simulated objects and their
     *            {@linkplain ObjectState state} histories that this drives
     *            forwards.
     * @param executor
     *            The object that this uses to execute {@link Runnable} tasks.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code universe} is null.</li>
     *             <li>If {@code executor} is null.</li>
     *             </ul>
     */
    public SimulationEngine(@NonNull final Universe universe, @NonNull final Executor executor) {
        this.universe = Objects.requireNonNull(universe, "universe");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * <p>
     * If necessary, schedule computations, so the
     * {@linkplain Universe#getHistoryEnd() history end time} of the
     * {@linkplain #getUniverse() universe} of this engine extends at least to a
     * given point in time.
     * </p>
     * <ul>
     * <li>After scheduling the computation, the method returns immediately, rather
     * than awaiting completion of the computation.</li>
     * </ul>
     * 
     * @param when
     *            The point in time of interest.
     * @throws NullPointerException
     *             If {@code when} is null.
     */
    public final void advanceHistory(@NonNull Duration when) {
        Objects.requireNonNull(when, "when");
        final Set<UUID> objectsToAdvance;
        if (universalAdvanceTo.compareTo(when) < 0) {
            universalAdvanceTo = when;
            objectsToAdvance = universe.getObjectIds();
        } else {
            // Optimization
            objectsToAdvance = Collections.emptySet();
        }
        for (UUID object : objectsToAdvance) {
            getEngine1(object).advanceHistory(when, null);
        }
    }

    /**
     * <p>
     * If necessary, schedule computation of the state history of a given object, of
     * the {@linkplain #getUniverse() universe} of this engine, so the
     * {@linkplain Universe#getLatestCommit(UUID) committed history} of the given
     * object extends at least to a given point in time.
     * </p>
     * <ul>
     * <li>This method is cheaper than {@link #computeObjectState(UUID, Duration)},
     * as it does not need to create and maintain a {@link Future} for recording the
     * computed state.</li>
     * <li>After scheduling the computation, the method returns immediately, rather
     * than awaiting completion of the computation.</li>
     * </ul>
     * 
     * @param object
     *            The ID of the object of interest.
     * @param when
     *            The point in time of interest.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             </ul>
     */
    public final @NonNull void advanceHistory(@NonNull UUID object, @NonNull Duration when) {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(when, "when");
        getEngine1(object).advanceHistory(when, null);
    }

    /**
     * <p>
     * Retrieve or, if necessary, compute the state of a given object, of the
     * {@linkplain #getUniverse() universe} of this engine, at a given point in
     * time.
     * </p>
     * <ul>
     * <li>Always returns a (non null) {@linkplain Future asynchronous
     * computation}.</li>
     * <li>{@linkplain Future#get() retrieving} the result of the returned
     * {@linkplain Future asynchronous computation} may throw an
     * {@link ExecutionException} {@linkplain ExecutionException#getCause() caused}
     * by a {@link PrehistoryException} if the point of time of interest is
     * {@linkplain Duration#compareTo(Duration) before} the
     * {@linkplain Universe#getHistoryStart() start of history} of the
     * {@linkplain #getUniverse() universe} of this engine.</li>
     * </ul>
     * 
     * @param object
     *            The ID of the object of interest.
     * @param when
     *            The point in time of interest.
     * @return The result of the asynchronous computation of the state of the given
     *         object at the given point in time, which can be
     *         {@linkplain Future#get() retrieved} once the computation is complete.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             </ul>
     */
    public final @NonNull Future<ObjectState> computeObjectState(@NonNull UUID object, @NonNull Duration when) {
        final ObjectStateId id = new ObjectStateId(object, when);
        return getEngine1(object).schedule(id, null);
    }

    private Engine1 getEngine1(UUID object) {
        assert object != null;
        return engines.computeIfAbsent(object, Engine1::new);
    }

    /**
     * <p>
     * The object that this uses to execute {@link Runnable} tasks.
     * </p>
     * <ul>
     * <li>Always have a (non null) executor.</li>
     * </ul>
     * 
     * @return the executor
     */
    @NonNull
    public final Executor getExecutor() {
        return executor;
    }

    /**
     * <p>
     * The collection of simulated objects and their {@linkplain ObjectState state}
     * histories that this drives forwards.
     * </p>
     * <ul>
     * <li>Always have a (non null) associated universe.</li>
     * </ul>
     * 
     * @return the universe;
     */
    @NonNull
    public final Universe getUniverse() {
        return universe;
    }

}
