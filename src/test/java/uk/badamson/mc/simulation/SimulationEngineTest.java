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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.NonNull;
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.history.ValueHistory;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link SimulationEngine} class.
 * </p>
 */
public class SimulationEngineTest {

    @Nested
    public class AdvanceHistory1 {

        @Nested
        public class Empty {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration when,
                    @NonNull final UUID object) {
                assert historyStart.compareTo(when) < 0;
                final Universe universe = new Universe(historyStart);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                advanceHistory(engine, object, when);
            }
        }// class

        @Nested
        public class Exists {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, WHEN_4, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, WHEN_5, OBJECT_B);
            }

            @Test
            public void eternally() {
                test(WHEN_1, ValueHistory.START_OF_TIME.plusNanos(1L), WHEN_3, ValueHistory.END_OF_TIME, OBJECT_A);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final Duration after, @NonNull final UUID object) {
                assert before.compareTo(when) <= 0;
                assert when.compareTo(after) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state1 = new ObjectStateTest.TestObjectState(1);
                final ObjectState state2 = new ObjectStateTest.TestObjectState(2);
                UniverseTest.putAndCommit(universe, object, before, state1);
                UniverseTest.putAndCommit(universe, object, after, state2);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                advanceHistory(engine, object, when);
            }
        }// class

        @Nested
        public class NoDependencies {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final UUID object) {
                assert historyStart.compareTo(when) < 0;
                assert before.compareTo(when) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
                UniverseTest.putAndCommit(universe, object, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                advanceHistory(engine, object, when);

                assertThat("Advanced the state history", universe.getLatestCommit(object), greaterThanOrEqualTo(when));
            }
        }// class

        private void advanceHistory(final SimulationEngine engine, @NonNull final UUID object,
                @NonNull final Duration when) {
            engine.advanceHistory(object, when);

            assertInvariants(engine);
        }

        @RepeatedTest(32)
        public void independentMultiThreaded() {
            final Duration historyStart = WHEN_1;
            final Duration before = WHEN_2;
            final Duration when = WHEN_3;
            assert historyStart.compareTo(when) < 0;
            assert before.compareTo(when) < 0;

            final Universe universe = new Universe(historyStart);
            final Collection<UUID> objects = new ArrayList<>(N_THREADS);
            for (int o = 1; o <= N_THREADS; ++o) {
                final UUID object = UUID.randomUUID();
                final ObjectState state0 = new ObjectStateTest.TestObjectState(o);
                objects.add(object);
                UniverseTest.putAndCommit(universe, object, before, state0);
            }
            final CountDownLatch ready = new CountDownLatch(1);
            final List<Future<Void>> futures = new ArrayList<>(N_THREADS);
            final SimulationEngine engine = new SimulationEngine(universe, threadPoolExecutor,
                    uncaughtExceptionHandlerA);
            for (final UUID object : objects) {
                futures.add(runInOtherThread(ready, () -> {
                    engine.advanceHistory(object, when);
                }));
            } // for

            ready.countDown();
            get(futures);

            for (final var future : futures) {
                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
            }
        }

        @Test
        public void rejectedExecution() {
            final Duration historyStart = WHEN_1;
            final Duration before = WHEN_2;
            final Duration when = WHEN_3;
            final UUID object = OBJECT_A;
            assert historyStart.compareTo(when) < 0;
            assert before.compareTo(when) < 0;

            final Universe universe = new Universe(historyStart);
            final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
            UniverseTest.putAndCommit(universe, object, before, state0);
            final SimulationEngine engine = new SimulationEngine(universe, threadPoolExecutor,
                    uncaughtExceptionHandlerA);
            threadPoolExecutor.shutdown();

            advanceHistory(engine, object, when);
        }

        @RepeatedTest(32)
        public void sameObjectMultiThreaded() {
            final Duration historyStart = WHEN_1;
            final Duration before = WHEN_2;
            final UUID object = OBJECT_A;

            final Universe universe = new Universe(historyStart);
            UniverseTest.putAndCommit(universe, object, before, new ObjectStateTest.TestObjectState(1));
            final CountDownLatch ready = new CountDownLatch(1);
            final List<Future<Void>> futures = new ArrayList<>(N_THREADS);
            final SimulationEngine engine = new SimulationEngine(universe, threadPoolExecutor,
                    uncaughtExceptionHandlerA);
            final Random random = new Random();
            for (int i = 0; i < N_THREADS; ++i) {
                final Duration when = before.plusMillis(random.nextInt(N_THREADS * 2000));
                futures.add(runInOtherThread(ready, () -> {
                    engine.advanceHistory(object, when);
                }));
            } // for

            ready.countDown();
            get(futures);

            for (final var future : futures) {
                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
            }
        }

    }// class

    @Nested
    public class AdvanceHistoryAll {

        @Nested
        public class Already {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, WHEN_4, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, WHEN_5, OBJECT_B);
            }

            @Test
            public void eternally() {
                test(WHEN_1, ValueHistory.START_OF_TIME.plusNanos(1L), WHEN_3, ValueHistory.END_OF_TIME, OBJECT_A);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final Duration after, @NonNull final UUID object) {
                assert before.compareTo(when) <= 0;
                assert when.compareTo(after) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state1 = new ObjectStateTest.TestObjectState(1);
                final ObjectState state2 = new ObjectStateTest.TestObjectState(2);
                UniverseTest.putAndCommit(universe, object, before, state1);
                UniverseTest.putAndCommit(universe, object, after, state2);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                advanceHistory(engine, when);

                assertEquals(after, universe.getHistoryEnd(), "History end (unchanged)");
            }
        }// class

        @Nested
        public class Empty {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration when,
                    @NonNull final UUID object) {
                assert historyStart.compareTo(when) < 0;
                final Universe universe = new Universe(historyStart);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                advanceHistory(engine, when);

                assertEquals(ValueHistory.END_OF_TIME, universe.getHistoryEnd(), "History end (still)");
            }
        }// class

        @Nested
        public class NoDependencies {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final UUID object) {
                assert historyStart.compareTo(when) < 0;
                assert before.compareTo(when) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
                UniverseTest.putAndCommit(universe, object, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                advanceHistory(engine, when);

                assertThat("Advanced the state history of the sole object.", universe.getLatestCommit(object),
                        greaterThanOrEqualTo(when));
                assertThat("Advanced the history end.", universe.getHistoryEnd(), greaterThanOrEqualTo(when));
            }
        }// class

        @Nested
        public class Past {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, WHEN_4, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, WHEN_5, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when1, @NonNull final Duration when2, @NonNull final UUID object) {
                assert historyStart.compareTo(when1) < 0;
                assert before.compareTo(when1) < 0;
                assert when1.compareTo(when2) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
                UniverseTest.putAndCommit(universe, object, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);
                engine.advanceHistory(when2);

                advanceHistory(engine, when1);

                assertThat("Advanced the state history of the sole object.", universe.getLatestCommit(object),
                        greaterThanOrEqualTo(when2));
                assertThat("Advanced the history end.", universe.getHistoryEnd(), greaterThanOrEqualTo(when2));
            }
        }// class

        @Nested
        public class Spawning {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A, OBJECT_B);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, OBJECT_B, OBJECT_A);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final UUID parent, @NonNull final UUID child) {
                assert historyStart.compareTo(before) < 0;
                assert before.compareTo(when) < 0;
                assert !parent.equals(child);
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.SpawningTestObjectState(1, 1000, child);
                UniverseTest.putAndCommit(universe, parent, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                advanceHistory(engine, when);

                assertAll("Advanced",
                        () -> assertThat("the state history of the parent.", universe.getLatestCommit(parent),
                                greaterThanOrEqualTo(when)),
                        () -> assertThat("the state history of the child.", universe.getLatestCommit(child),
                                greaterThanOrEqualTo(when)),
                        () -> assertThat("the history end.", universe.getHistoryEnd(), greaterThanOrEqualTo(when)));
            }
        }// class

        @Nested
        public class Succesive {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, WHEN_4, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, WHEN_5, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when1, @NonNull final Duration when2, @NonNull final UUID object) {
                assert historyStart.compareTo(when1) < 0;
                assert before.compareTo(when1) < 0;
                assert when1.compareTo(when2) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
                UniverseTest.putAndCommit(universe, object, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);
                engine.advanceHistory(when1);

                advanceHistory(engine, when2);

                assertThat("Advanced the state history of the sole object.", universe.getLatestCommit(object),
                        greaterThanOrEqualTo(when2));
                assertThat("Advanced the history end.", universe.getHistoryEnd(), greaterThanOrEqualTo(when2));
            }
        }// class

        private void advanceHistory(final SimulationEngine engine, @NonNull final Duration when) {
            engine.advanceHistory(when);

            assertInvariants(engine);
        }

        @RepeatedTest(8)
        public void multiThreaded() {
            final Duration historyStart = WHEN_1;
            final Duration before = WHEN_2;
            final UUID object = OBJECT_A;
            final Universe universe = new Universe(historyStart);
            final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
            UniverseTest.putAndCommit(universe, object, before, state0);

            final SimulationEngine engine = new SimulationEngine(universe, threadPoolExecutor,
                    uncaughtExceptionHandlerA);
            /*
             * Closely synchronize the crucial method call, to maximise the possibility of
             * race hazards,
             *
             */
            final CountDownLatch ready = new CountDownLatch(1);
            final CountDownLatch started = new CountDownLatch(N_THREADS);
            final Random random = new Random();
            final Duration[] when = new Duration[N_THREADS];
            Duration last = before;
            for (int i = 0; i < N_THREADS; ++i) {
                when[i] = before.plusMillis(random.nextInt(N_THREADS * 2000));
                last = when[i].compareTo(last) < 0 ? when[i] : last;
                assert historyStart.compareTo(when[i]) < 0;
                assert before.compareTo(when[i]) < 0;
            } // for
            for (int i = 0; i < N_THREADS; ++i) {
                final int iThread = i;
                runInOtherThread(ready, () -> {
                    engine.advanceHistory(when[iThread]);
                    started.countDown();
                });
            } // for

            ready.countDown();
            try {
                started.await();
                threadPoolExecutor.shutdown();
                threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (final InterruptedException e) {
                throw new AssertionError(e);
            }
            assertInvariants(engine);
            UniverseTest.assertInvariants(universe, object);
            assertThat("Advanced the history end.", universe.getHistoryEnd(), greaterThanOrEqualTo(last));
        }

    }// class

    @Nested
    public class ComputeObjectState {

        @Nested
        public class AtOrAfterDestruction {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, WHEN_4, OBJECT_A);
            }

            @Test
            public void at() {
                test(WHEN_1, WHEN_2, WHEN_3, WHEN_3, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, WHEN_5, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration whenExist,
                    @NonNull final Duration whenDestroyed, @NonNull final Duration when, @NonNull final UUID object) {
                assert whenExist.compareTo(whenDestroyed) < 0;
                assert whenDestroyed.compareTo(when) <= 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
                UniverseTest.putAndCommit(universe, object, whenExist, state0);
                UniverseTest.putAndCommit(universe, object, whenDestroyed, null);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                final Future<ObjectState> future = computeObjectState(engine, object, when);

                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
                final ObjectState state;
                try {
                    state = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation immediately succeeds", e);
                    return;// never happens
                }
                assertNull(state, "Indicates that object does not exist (anymore).");
            }
        }// class

        @Nested
        public class Empty {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration when,
                    @NonNull final UUID object) {
                assert historyStart.compareTo(when) < 0;
                final Universe universe = new Universe(historyStart);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                final Future<ObjectState> future = computeObjectState(engine, object, when);

                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
                final ObjectState state;
                try {
                    state = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation immediately succeeds", e);
                    return;// never happens
                }
                assertNull(state, "Object does not exist");
            }
        }// class

        @Nested
        public class Exists {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, WHEN_4, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, WHEN_5, OBJECT_B);
            }

            @Test
            public void eternally() {
                test(WHEN_1, ValueHistory.START_OF_TIME.plusNanos(1L), WHEN_3, ValueHistory.END_OF_TIME, OBJECT_A);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final Duration after, @NonNull final UUID object) {
                assert before.compareTo(when) <= 0;
                assert when.compareTo(after) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state1 = new ObjectStateTest.TestObjectState(1);
                final ObjectState state2 = new ObjectStateTest.TestObjectState(2);
                UniverseTest.putAndCommit(universe, object, before, state1);
                UniverseTest.putAndCommit(universe, object, after, state2);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                final Future<ObjectState> future = computeObjectState(engine, object, when);

                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
                final ObjectState state;
                try {
                    state = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation immediately succeeds", e);
                    return;// never happens
                }
                assertSame(state1, state, "Retrieved the existing object state.");
            }
        }// class

        @Nested
        public class Independent2 {

            @Test
            public void successiveA() {
                test(WHEN_1, WHEN_2, WHEN_3, WHEN_4, OBJECT_A);
            }

            @Test
            public void successiveB() {
                test(WHEN_2, WHEN_3, WHEN_4, WHEN_5, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration whenA, @NonNull final Duration whenB, @NonNull final UUID object) {
                assert historyStart.compareTo(whenA) < 0;
                assert historyStart.compareTo(whenB) < 0;
                assert before.compareTo(whenA) < 0;
                assert before.compareTo(whenB) < 0;
                final Duration whenLast = whenA.compareTo(whenB) <= 0 ? whenA : whenB;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
                UniverseTest.putAndCommit(universe, object, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);
                engine.computeObjectState(object, whenA);

                final Future<ObjectState> future2 = computeObjectState(engine, object, whenB);

                assertAll("future", () -> assertFalse(future2.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future2.isDone(), "Done"));
                final ObjectState state2;
                try {
                    state2 = future2.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation succeeds", e);
                    return;// never happens
                }
                assertNotNull(state2, "Computed a state");// guard
                ObjectStateTest.assertInvariants(state2);
                assertThat("Advanced the state history", universe.getLatestCommit(object),
                        greaterThanOrEqualTo(whenLast));
            }

            @Test
            public void unordered() {
                test(WHEN_1, WHEN_2, WHEN_4, WHEN_3, OBJECT_A);
            }
        }// class

        @Nested
        public class Independent2Enqueued {

            @Test
            public void successiveA() {
                test(WHEN_1, WHEN_2, WHEN_3, WHEN_4, OBJECT_A);
            }

            @Test
            public void successiveB() {
                test(WHEN_2, WHEN_3, WHEN_4, WHEN_5, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration whenA, @NonNull final Duration whenB, @NonNull final UUID object) {
                assert historyStart.compareTo(whenA) < 0;
                assert historyStart.compareTo(whenB) < 0;
                assert before.compareTo(whenA) < 0;
                assert before.compareTo(whenB) < 0;
                final Duration whenLast = whenA.compareTo(whenB) <= 0 ? whenA : whenB;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
                UniverseTest.putAndCommit(universe, object, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, enqueingExector,
                        uncaughtExceptionHandlerA);
                engine.computeObjectState(object, whenA);

                final Future<ObjectState> future2 = computeObjectState(engine, object, whenB);

                enqueingExector.runAll();
                assertAll("future", () -> assertFalse(future2.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future2.isDone(), "Done"));
                final ObjectState state2;
                try {
                    state2 = future2.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation succeeds", e);
                    return;// never happens
                }
                assertNotNull(state2, "Computed a state");// guard
                ObjectStateTest.assertInvariants(state2);
                assertThat("Advanced the state history", universe.getLatestCommit(object),
                        greaterThanOrEqualTo(whenLast));
            }

            @Test
            public void unordered() {
                test(WHEN_1, WHEN_2, WHEN_4, WHEN_3, OBJECT_A);
            }
        }// class

        @Nested
        public class NoDependencies {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final UUID object) {
                assert historyStart.compareTo(when) < 0;
                assert before.compareTo(when) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
                UniverseTest.putAndCommit(universe, object, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                final Future<ObjectState> future = computeObjectState(engine, object, when);

                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
                final ObjectState state;
                try {
                    state = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation succeeds", e);
                    return;// never happens
                }
                assertNotNull(state, "Computed a state");// guard
                ObjectStateTest.assertInvariants(state);
                assertThat("Advanced the state history", universe.getLatestCommit(object), greaterThanOrEqualTo(when));
            }
        }// class

        @Nested
        public class PrehistoricRead {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, OBJECT_B);
            }

            @Test
            public void close() {
                test(WHEN_1, WHEN_2, WHEN_2.plusNanos(1), OBJECT_A);
            }

            private void test(@NonNull final Duration before, @NonNull final Duration when,
                    @NonNull final Duration historyStart, @NonNull final UUID object) {
                assert when.compareTo(historyStart) < 0;
                assert before.compareTo(when) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
                UniverseTest.putAndCommit(universe, object, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                final Future<ObjectState> future = computeObjectState(engine, object, when);

                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
                try {
                    future.get();
                } catch (final InterruptedException e) {
                    fail("get() not interrupted", e);
                    return;// never happens
                } catch (final ExecutionException e) {
                    assertThat("Threw PrehistoryException exception", e.getCause(),
                            instanceOf(PrehistoryException.class));
                    return;
                }
                fail("Did not throw exception");
            }
        }// class

        @Nested
        public class SelfDestructing {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, OBJECT_B);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final UUID object) {
                assert historyStart.compareTo(when) < 0;
                assert before.compareTo(when) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.SelfDestructingObjectState(1);
                UniverseTest.putAndCommit(universe, object, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                final Future<ObjectState> future = computeObjectState(engine, object, when);

                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
                final ObjectState state;
                try {
                    state = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation succeeds", e);
                    return;// never happens
                }
                assertNull(state, "Computed destroyed state");
                assertThat("Advanced the state history", universe.getLatestCommit(object), greaterThanOrEqualTo(when));
            }
        }// class

        @Nested
        public class WithDependency {
            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A, OBJECT_B, Duration.ofMillis(7));
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, OBJECT_B, OBJECT_A, Duration.ofMillis(13));
            }

            @Test
            public void close() {
                final Duration historyStart = WHEN_1;
                final Duration before = historyStart.plusNanos(1L);
                final Duration when = before.plusNanos(2L);
                final Duration dependencyDelay = Duration.ofNanos(1);
                test(historyStart, before, when, OBJECT_A, OBJECT_B, dependencyDelay);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final UUID objectA, @NonNull final UUID objectB,
                    @NonNull final Duration dependencyDelay) {
                assert historyStart.compareTo(when) < 0;
                assert before.compareTo(when) < 0;
                assert historyStart.compareTo(before) < 0;
                assert historyStart.compareTo(before.minus(dependencyDelay)) <= 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState stateA0 = new ObjectStateTest.TestObjectState(11);
                final ObjectState stateB0 = new ObjectStateTest.TestObjectState(12);
                UniverseTest.putAndCommit(universe, objectA, historyStart, stateA0);
                UniverseTest.putAndCommit(universe, objectB, historyStart, stateB0);
                final ObjectState stateB1 = new ObjectStateTest.DependentTestObjectState(22, objectA, dependencyDelay);
                UniverseTest.putAndCommit(universe, objectB, before, stateB1);
                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                final Future<ObjectState> future = computeObjectState(engine, objectB, when);

                final Duration latestCommitB = universe.getLatestCommit(objectB);
                assertAll(
                        () -> assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                                () -> assertTrue(future.isDone(), "Done")),
                        () -> assertAll("Advanced the state history",
                                () -> assertThat("at all", latestCommitB, greaterThan(before)),
                                () -> assertThat("to at least the required time", latestCommitB,
                                        greaterThanOrEqualTo(when))));// guard
                final ObjectState state;
                try {
                    state = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation succeeds", e);
                    return;// never happens
                }
                assertNotNull(state, "Computed a state");// guard
                ObjectStateTest.assertInvariants(state);
            }
        }// class

        @Nested
        public class WithDependencyProvidedInOrder {
            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A, OBJECT_B, Duration.ofMillis(7));
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, OBJECT_B, OBJECT_A, Duration.ofMillis(13));
            }

            @Test
            public void close() {
                final Duration historyStart = WHEN_1;
                final Duration before = historyStart.plusNanos(1L);
                final Duration when = before.plusNanos(2L);
                final Duration dependencyDelay = Duration.ofNanos(1);
                test(historyStart, before, when, OBJECT_A, OBJECT_B, dependencyDelay);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final UUID objectA, @NonNull final UUID objectB,
                    @NonNull final Duration dependencyDelay) {
                assert historyStart.compareTo(when) < 0;
                assert before.compareTo(when) < 0;
                assert historyStart.compareTo(before) < 0;
                assert historyStart.compareTo(before.minus(dependencyDelay)) <= 0;

                final Universe universe = new Universe(historyStart);
                final ObjectState stateA0 = new ObjectStateTest.TestObjectState(11);
                final ObjectState stateB0 = new ObjectStateTest.TestObjectState(12);
                UniverseTest.putAndCommit(universe, objectA, historyStart, stateA0);
                UniverseTest.putAndCommit(universe, objectB, historyStart, stateB0);
                final ObjectState stateB1 = new ObjectStateTest.DependentTestObjectState(22, objectA, dependencyDelay);
                UniverseTest.putAndCommit(universe, objectB, before, stateB1);

                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);
                computeObjectState(engine, objectA, when);

                final Future<ObjectState> future = computeObjectState(engine, objectB, when);

                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
                final ObjectState state;
                try {
                    state = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation succeeds", e);
                    return;// never happens
                }
                assertNotNull(state, "Computed a state");// guard
                ObjectStateTest.assertInvariants(state);
                final Duration latestCommitB = universe.getLatestCommit(objectB);
                assertAll("Advanced the state history", () -> assertThat("at all", latestCommitB, greaterThan(before)),
                        () -> assertThat("to at least the required time", latestCommitB, greaterThanOrEqualTo(when)));
            }
        }// class

        @Nested
        public class WithMutualDependency {

            @Test
            public void a() {
                final Duration dependencyDelay = Duration.ofMillis(13);
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A, OBJECT_B, dependencyDelay);
            }

            @Test
            public void b() {
                final Duration dependencyDelay = Duration.ofMillis(17);
                test(WHEN_2, WHEN_3, WHEN_4, OBJECT_B, OBJECT_A, dependencyDelay);
            }

            @Test
            public void c() {
                final Duration dependencyDelay = Duration.ofSeconds(2);
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A, OBJECT_B, dependencyDelay);
            }

            private void test(@NonNull final Duration historyStart, @NonNull final Duration before,
                    @NonNull final Duration when, @NonNull final UUID objectA, @NonNull final UUID objectB,
                    @NonNull final Duration dependencyDelay) {
                assert historyStart.compareTo(when) < 0;
                assert before.compareTo(when) < 0;
                assert historyStart.compareTo(before) < 0;
                assert historyStart.compareTo(before.minus(dependencyDelay)) <= 0;

                final Universe universe = new Universe(historyStart);
                final ObjectState stateA0 = new ObjectStateTest.TestObjectState(11);
                final ObjectState stateB0 = new ObjectStateTest.TestObjectState(12);
                UniverseTest.putAndCommit(universe, objectA, historyStart, stateA0);
                UniverseTest.putAndCommit(universe, objectB, historyStart, stateB0);
                final ObjectState stateA1 = new ObjectStateTest.DependentTestObjectState(12, objectB, dependencyDelay);
                final ObjectState stateB1 = new ObjectStateTest.DependentTestObjectState(22, objectA, dependencyDelay);
                UniverseTest.putAndCommit(universe, objectA, before, stateA1);
                UniverseTest.putAndCommit(universe, objectB, before, stateB1);

                final SimulationEngine engine = new SimulationEngine(universe, directExecutor,
                        uncaughtExceptionHandlerA);

                final Future<ObjectState> future = computeObjectState(engine, objectB, when);

                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
                final ObjectState state;
                try {
                    state = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation succeeds", e);
                    return;// never happens
                }
                assertNotNull(state, "Computed a state");// guard
                ObjectStateTest.assertInvariants(state);
                final Duration latestCommitB = universe.getLatestCommit(objectB);
                assertAll("Advanced the state history", () -> assertThat("at all", latestCommitB, greaterThan(before)),
                        () -> assertThat("to at least the required time", latestCommitB, greaterThanOrEqualTo(when)));
            }
        }// class

        private Future<ObjectState> computeObjectState(final SimulationEngine engine, @NonNull final UUID object,
                @NonNull final Duration when) {
            final Future<ObjectState> future = engine.computeObjectState(object, when);

            assertInvariants(engine);
            assertNotNull(future, "Always returns a (non null) asynchronous computation.");
            return future;
        }

        @RepeatedTest(32)
        public void independentMultiThreaded() {
            final Duration historyStart = WHEN_1;
            final Duration before = WHEN_2;
            final Duration when = WHEN_3;
            assert historyStart.compareTo(when) < 0;
            assert before.compareTo(when) < 0;

            final Universe universe = new Universe(historyStart);
            final Collection<UUID> objects = new ArrayList<>(N_THREADS);
            for (int o = 1; o <= N_THREADS; ++o) {
                final UUID object = UUID.randomUUID();
                final ObjectState state0 = new ObjectStateTest.TestObjectState(o);
                objects.add(object);
                UniverseTest.putAndCommit(universe, object, before, state0);
            }
            final List<Future<ObjectState>> futures = new ArrayList<>(N_THREADS);
            final SimulationEngine engine = new SimulationEngine(universe, threadPoolExecutor,
                    uncaughtExceptionHandlerA);
            for (final UUID object : objects) {
                futures.add(engine.computeObjectState(object, when));
            }

            get(futures);

            assertInvariants(engine);
            for (final var future : futures) {
                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
            }
            for (final var future : futures) {
                final ObjectState state2;
                try {
                    state2 = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation succeeds", e);
                    return;// never happens
                }
                assertNotNull(state2, "Computed a state");// guard
                ObjectStateTest.assertInvariants(state2);
            }
            for (final UUID object : objects) {
                assertThat("Advanced the state history", universe.getLatestCommit(object), greaterThanOrEqualTo(when));
            }
        }

        @RepeatedTest(32)
        public void sameObjectMultiThreaded() {
            final Duration historyStart = WHEN_1;
            final Duration before = WHEN_2;
            final UUID object = OBJECT_A;

            final Universe universe = new Universe(historyStart);
            UniverseTest.putAndCommit(universe, object, before, new ObjectStateTest.TestObjectState(1));
            final List<Future<ObjectState>> futures = new ArrayList<>(N_THREADS);
            final SimulationEngine engine = new SimulationEngine(universe, threadPoolExecutor,
                    uncaughtExceptionHandlerA);
            final Random random = new Random();
            for (int i = 0; i < N_THREADS; ++i) {
                final Duration when = before.plusMillis(1 + random.nextInt(N_THREADS * 2000));
                assert historyStart.compareTo(when) < 0;
                assert before.compareTo(when) < 0;
                futures.add(engine.computeObjectState(object, when));
            }

            get(futures);

            assertInvariants(engine);
            UniverseTest.assertInvariants(universe, object);
            for (final var future : futures) {
                assertAll("future", () -> assertFalse(future.isCancelled(), "Not cancelled"),
                        () -> assertTrue(future.isDone(), "Done"));
            }
            for (final var future : futures) {
                final ObjectState state2;
                try {
                    state2 = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Computation succeeds", e);
                    return;// never happens
                }
                assertNotNull(state2, "Computed a state");// guard
                ObjectStateTest.assertInvariants(state2);
            } // for
        }

    }// class

    @Nested
    public class Constructor {
        @Test
        public void a() {
            test(WHEN_1, directExecutor, uncaughtExceptionHandlerA);
        }

        @Test
        public void b() {
            test(WHEN_2, enqueingExector, uncaughtExceptionHandlerB);
        }

        private void test(final Duration historyStart, final Executor executor,
                final UncaughtExceptionHandler uncaughtExceptionHandler) {
            final Universe universe = new Universe(historyStart);
            test(universe, executor, uncaughtExceptionHandler);
        }

        private void test(final Universe universe, final Executor executor,
                final UncaughtExceptionHandler uncaughtExceptionHandler) {
            final SimulationEngine engine = new SimulationEngine(universe, executor, uncaughtExceptionHandler);

            assertInvariants(engine);
            assertSame(universe, engine.getUniverse(), "This engine has the given universe as its universe.");
            assertSame(executor, engine.getExecutor(), "This engine has the given executor as its executor.");
            assertSame(uncaughtExceptionHandler, engine.getUncaughtExceptionHandler(),
                    "This engine has the given uncaught exception handler as its uncaught exception handler.");
        }
    }// class

    private static final class DirectExecutor implements Executor {

        @Override
        public void execute(final Runnable runnable) {
            Objects.requireNonNull(runnable, "runnable");
            runnable.run();
        }

    }// class

    private static final class EnqueingExector implements Executor {
        private final Queue<Runnable> queue = new ArrayDeque<>();

        @Override
        public void execute(final Runnable runnable) {
            Objects.requireNonNull(runnable, "runnable");
            queue.add(runnable);
        }

        void runAll() {
            while (!queue.isEmpty()) {
                queue.remove().run();
            }
        }

    }// class

    private static final Duration WHEN_1 = UniverseTest.DURATION_1;
    private static final Duration WHEN_2 = UniverseTest.DURATION_2;
    private static final Duration WHEN_3 = UniverseTest.DURATION_3;
    private static final Duration WHEN_4 = UniverseTest.DURATION_4;
    private static final Duration WHEN_5 = UniverseTest.DURATION_5;

    private static final UUID OBJECT_A = UniverseTest.OBJECT_A;
    private static final UUID OBJECT_B = UniverseTest.OBJECT_B;

    private static final int N_THREADS = Runtime.getRuntime().availableProcessors() * 4;

    public static void assertInvariants(final SimulationEngine engine) {
        ObjectTest.assertInvariants(engine);// inherited

        final Universe universe = engine.getUniverse();
        assertNotNull(universe, "Always have a (non null) associated universe.");// guard

        UniverseTest.assertInvariants(universe);
    }

    public static void assertInvariants(final SimulationEngine engine1, final SimulationEngine engine2) {
        ObjectTest.assertInvariants(engine1, engine2);// inherited
    }

    private static <T> void get(final Future<T> future) {
        try {
            future.get();
        } catch (final InterruptedException e) {
            throw new AssertionError(e);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof AssertionError)
                throw (AssertionError) cause;
            else if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new AssertionError(e);
        }
    }

    private static <T> void get(final List<Future<T>> futures) {
        final List<Throwable> exceptions = new ArrayList<>(futures.size());
        for (final var future : futures) {
            try {
                get(future);
            } catch (Exception | AssertionError e) {
                exceptions.add(e);
            }
        }
        final int nExceptions = exceptions.size();
        if (0 < nExceptions) {
            final Throwable e = exceptions.get(0);
            for (int i = 1; i < nExceptions; ++i) {
                final Throwable exception = exceptions.get(i);
                if (!e.equals(exception)) {
                    e.addSuppressed(exception);
                }
            }
            if (e instanceof AssertionError)
                throw (AssertionError) e;
            else if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new AssertionError(e);
        }
    }

    private static Future<Void> runInOtherThread(final CountDownLatch ready, final Runnable operation) {
        final CompletableFuture<Void> future = new CompletableFuture<Void>();
        final Thread thread = new Thread(() -> {
            try {
                ready.await();
                operation.run();
            } catch (final Throwable e) {
                future.completeExceptionally(e);
                return;
            }
            future.complete(null);
        });
        thread.start();
        return future;
    }

    private DirectExecutor directExecutor;

    private EnqueingExector enqueingExector;

    private ExecutorService threadPoolExecutor;

    private UncaughtExceptionHandlerTest.RecordingUncaughtExceptionHandler uncaughtExceptionHandlerA;

    private UncaughtExceptionHandlerTest.RecordingUncaughtExceptionHandler uncaughtExceptionHandlerB;

    @BeforeEach
    public void setUpExectors() {
        directExecutor = new DirectExecutor();
        enqueingExector = new EnqueingExector();
        threadPoolExecutor = Executors.newFixedThreadPool(N_THREADS);
    }

    @BeforeEach
    public void setUpUncaughtExceptionHandlers() {
        uncaughtExceptionHandlerA = new UncaughtExceptionHandlerTest.RecordingUncaughtExceptionHandler();
        uncaughtExceptionHandlerB = new UncaughtExceptionHandlerTest.RecordingUncaughtExceptionHandler();
    }
}
