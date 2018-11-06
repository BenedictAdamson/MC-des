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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link SimulationEngine} class.
 * </p>
 */
public class SimulationEngineTest {

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
