package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.HashMap;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import uk.badamson.mc.simulation.Universe.PrehistoryException;

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

/**
 * <p>
 * Drives a simulation {@linkplain Universe universe} forward to a desired end
 * state.
 * </p>
 */
public final class SimulationEngine {

    private final class Engine1 implements Universe.TransactionListener {
        @NonNull
        private final UUID object;
        @Nullable
        private Duration latestCommit;
        private final NavigableMap<Duration, FutureObjectState> steps = new TreeMap<>();
        private final Set<UUID> dependentObjects = new HashSet<>();
        private final Set<UUID> objectDependencies = new HashSet<>();

        private Engine1(UUID object) {
            this.object = object;
            latestCommit = universe.getLatestCommit(object);
        }

        private void advance1() {
            latestCommit = universe.getLatestCommit(object);
            assert latestCommit != null;
            final Universe.Transaction transaction = universe.beginTransaction(this);

            try {
                final ObjectState state0 = transaction.getObjectState(object, latestCommit);
                state0.putNextStateTransition(transaction, object, latestCommit);
                assert transaction.getWhen() != null;
                assert latestCommit.compareTo(transaction.getWhen()) < 0;
                assert transaction.getObjectStatesWritten().containsKey(object);

                scheduleDependencies(new TreeSet<>(transaction.getDependencies().values()));

                transaction.beginCommit();
            } catch (Universe.PrehistoryException e) {
                // Hard to test: race hazard.
                completeExceptionally(e);
                transaction.beginAbort();
            }
        }

        private void completeCommitableReads() {
            latestCommit = universe.getLatestCommit(object);
            final SortedMap<Duration, FutureObjectState> commitableReads = latestCommit == null ? steps
                    : new TreeMap<>(steps.headMap(latestCommit, true));
            for (var future : commitableReads.values()) {
                if (!future.isDone()) {
                    future.beginReadTransaction();
                }
                assert future.isDone();
            }
            steps.keySet().removeAll(commitableReads.keySet());
            assert !(latestCommit == null && !steps.isEmpty());
        }

        private void completeExceptionally(Throwable e) {
            for (var step : steps.values()) {
                step.completeExceptionally(e);
            }
            steps.clear();
        }

        @Override
        public void onAbort() {
            scheduleAdvance1();
        }

        @Override
        public void onCommit() {
            objectDependencies.clear();
            /*
             * Now that we have advanced the simulation, some waiting reads might be able to
             * complete.
             */
            completeCommitableReads();

            if (!steps.isEmpty()) {
                // Further work required for the object this engine moves forward.
                scheduleAdvance1();
            }

            // And some aborted writes could be restarted
            for (UUID dependent : dependentObjects) {
                removeDependency(object, dependent);
            }
            dependentObjects.clear();
        }

        private void removeDependency1(final UUID dependedOnObject) {
            objectDependencies.remove(dependedOnObject);
            if (objectDependencies.isEmpty()) {
                // Removed the last remaining dependency, so can (re)try advancing the state
                scheduleAdvance1();
            }
        }

        private FutureObjectState schedule(final ObjectStateId id, UUID dependent) {
            final Duration when = id.getWhen();
            assert object.equals(id.getObject());
            assert dependent == null || !object.equals(dependent);
            final boolean wasEmpty = steps.isEmpty();
            final FutureObjectState future = steps.computeIfAbsent(when, t -> new FutureObjectState(id));
            if (dependent != null) {
                dependentObjects.add(dependent);
            }
            completeCommitableReads();
            if (wasEmpty && !steps.isEmpty()) {
                // Have acquired work to do.
                scheduleAdvance1();
            }
            return future;
        }

        private void scheduleAdvance1() {
            executor.execute(() -> advance1());
        }

        /*
         * By having the dependencies sorted, we will schedule the dependencies in
         * ascending time order, which will tend to result in fewer aborted
         * transactions. The number of dependencies should be small and not proportional
         * to the number of objects, so the performance to advance the universe to a
         * given point in time should remain O(N).
         */
        private void scheduleDependencies(final SortedSet<ObjectStateId> dependencyIds) {
            for (var dependencyId : dependencyIds) {
                final UUID objectDependency = dependencyId.getObject();
                final Duration dependencyWhen = dependencyId.getWhen();
                final Duration dependencyLastestCommit = universe.getLatestCommit(objectDependency);
                if (dependencyLastestCommit.compareTo(dependencyWhen) < 0) {
                    /*
                     * To commit we will need this dependency, but it is not yet committed, so
                     * schedule production of the dependency so we will eventually advance.
                     */
                    objectDependencies.add(objectDependency);
                    getEngine1(objectDependency).schedule(dependencyId, object);
                }
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
        private final ObjectStateId id;
        private ObjectState state;

        private FutureObjectState(@NonNull ObjectStateId id) {
            this.id = id;
        }

        private void beginReadTransaction() {
            final Universe.Transaction transaction = universe.beginTransaction(new Universe.TransactionListener() {

                @Override
                public void onAbort() {
                    // Do nothing; the engine will retry
                }

                @Override
                public void onCommit() {
                    complete(state);
                }

            });
            try {
                state = transaction.getObjectState(id.getObject(), id.getWhen());
                transaction.beginCommit();
            } catch (Universe.PrehistoryException e) {
                completeExceptionally(e);
            }
        }

    }// class

    @NonNull
    private final Universe universe;

    @NonNull
    private final Executor executor;

    private final Map<UUID, Engine1> engines = new HashMap<>();

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

    private void removeDependency(UUID dependedOnObject, UUID dependency) {
        final Engine1 engine1 = getEngine1(dependency);
        engine1.removeDependency1(dependedOnObject);
    }

}
