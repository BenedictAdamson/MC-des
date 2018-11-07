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
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.NonNull;
import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link SimulationEngine} class.
 * </p>
 */
public class SimulationEngineTest {

    @Nested
    public class ComputeObjectState {

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

            private void test(@NonNull Duration historyStart, @NonNull Duration when, @NonNull UUID object) {
                assert historyStart.compareTo(when) < 0;
                final Universe universe = new Universe(historyStart);
                final SimulationEngine engine = new SimulationEngine(universe, executorA);

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

            private void test(@NonNull Duration historyStart, @NonNull Duration before, @NonNull Duration when,
                    @NonNull Duration after, @NonNull UUID object) {
                assert before.compareTo(when) <= 0;
                assert when.compareTo(after) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state1 = new ObjectStateTest.TestObjectState(1);
                final ObjectState state2 = new ObjectStateTest.TestObjectState(2);
                UniverseTest.putAndCommit(universe, object, before, state1);
                UniverseTest.putAndCommit(universe, object, after, state2);
                final SimulationEngine engine = new SimulationEngine(universe, executorA);

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
        public class NoDependencies {

            @Test
            public void a() {
                test(WHEN_1, WHEN_2, WHEN_3, OBJECT_A);
            }

            @Test
            public void b() {
                test(WHEN_2, WHEN_3, WHEN_4, OBJECT_B);
            }

            private void test(@NonNull Duration historyStart, @NonNull Duration before, @NonNull Duration when,
                    @NonNull UUID object) {
                assert historyStart.compareTo(when) < 0;
                assert before.compareTo(when) < 0;
                final Universe universe = new Universe(historyStart);
                final ObjectState state0 = new ObjectStateTest.TestObjectState(1);
                UniverseTest.putAndCommit(universe, object, before, state0);
                final SimulationEngine engine = new SimulationEngine(universe, executorA);

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

        private Future<ObjectState> computeObjectState(SimulationEngine engine, @NonNull UUID object,
                @NonNull Duration when) {
            final Future<ObjectState> future = engine.computeObjectState(object, when);

            assertInvariants(engine);
            assertNotNull(future, "Always returns a (non null) asynchronous computation.");
            return future;
        }

    }// class

    @Nested
    public class Constructor {
        @Test
        public void a() {
            test(WHEN_1, executorA);
        }

        @Test
        public void b() {
            test(WHEN_2, executorB);
        }

        private void test(final Duration historyStart, final Executor executor) {
            final Universe universe = new Universe(historyStart);
            test(universe, executor);
        }

        private void test(final Universe universe, final Executor executor) {
            final SimulationEngine engine = new SimulationEngine(universe, executor);

            assertInvariants(engine);
            assertSame(universe, engine.getUniverse(), "This engine has the given universe as its universe.");
            assertSame(executor, engine.getExecutor(), "This engine has the given executor as its executor.");
        }
    }// class

    private static final class DirectExector implements Executor {

        @Override
        public void execute(Runnable runnable) {
            Objects.requireNonNull(runnable, "runnable");
            runnable.run();
        }

    }// class

    private static final Duration WHEN_1 = UniverseTest.DURATION_1;
    private static final Duration WHEN_2 = UniverseTest.DURATION_2;
    private static final Duration WHEN_3 = UniverseTest.DURATION_3;
    private static final Duration WHEN_4 = UniverseTest.DURATION_4;
    private static final Duration WHEN_5 = UniverseTest.DURATION_5;

    private static final UUID OBJECT_A = UniverseTest.OBJECT_A;
    private static final UUID OBJECT_B = UniverseTest.OBJECT_B;

    public static void assertInvariants(SimulationEngine engine) {
        ObjectTest.assertInvariants(engine);// inherited

        final Universe universe = engine.getUniverse();
        assertNotNull(universe, "Always have a (non null) associated universe.");// guard

        UniverseTest.assertInvariants(universe);
    }

    public static void assertInvariants(SimulationEngine engine1, SimulationEngine engine2) {
        ObjectTest.assertInvariants(engine1, engine2);// inherited
    }

    private Executor executorA;
    private Executor executorB;

    @BeforeEach
    public void setUpExectors() {
        executorA = new DirectExector();
        executorB = new DirectExector();
    }
}
