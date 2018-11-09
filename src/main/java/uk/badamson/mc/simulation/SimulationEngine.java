package uk.badamson.mc.simulation;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import edu.umd.cs.findbugs.annotations.NonNull;
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
        private final UUID object;
        private final Duration when;
        private ObjectState state;
        private Duration latestCommit;

        private FutureObjectState(@NonNull UUID object, @NonNull Duration when) {
            this.object = object;
            this.when = when;
        }

        private void advance1() {
            latestCommit = universe.getLatestCommit(object);
            final Universe.Transaction transaction = universe.beginTransaction(new Universe.TransactionListener() {

                @Override
                public void onAbort() {
                    // TODO retry
                    // TODO handle dependencies
                }

                @Override
                public void onCommit() {
                    latestCommit = universe.getLatestCommit(object);
                    if (latestCommit.compareTo(when) < 0) {
                        // Further work required
                        scheduleAdvance1();
                    } else if (!isDone()) {
                        beginReadTransaction();
                    }
                }
            });

            final ObjectState state0 = transaction.getObjectState(object, latestCommit);
            state0.putNextStateTransition(transaction, object, latestCommit);
            transaction.beginCommit();
        }

        private void beginReadTransaction() {
            final Universe.Transaction transaction = universe.beginTransaction(new Universe.TransactionListener() {

                @Override
                public void onAbort() {
                    // Do nothing; advance1() will retry
                }

                @Override
                public void onCommit() {
                    complete(state);
                }

            });
            try {
                state = transaction.getObjectState(object, when);
                transaction.beginCommit();
            } catch (Universe.PrehistoryException e) {
                completeExceptionally(e);
            }
        }

        private void compute() {
            // Try to fetch an already committed state:
            beginReadTransaction();
            if (!isDone()) {
                scheduleAdvance1();
            }
        }

        private void scheduleAdvance1() {
            executor.execute(() -> advance1());
        }

    }// class

    @NonNull
    private final Universe universe;

    @NonNull
    private final Executor executor;

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
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(when, "when");

        final FutureObjectState future = new FutureObjectState(object, when);
        future.compute();
        return future;
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
