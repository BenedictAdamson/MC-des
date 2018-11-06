package uk.badamson.mc.simulation;

import java.util.Objects;
import java.util.concurrent.Executor;

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

    private final Universe universe;
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
    public SimulationEngine(final Universe universe, final Executor executor) {
        this.universe = Objects.requireNonNull(universe, "universe");
        this.executor = Objects.requireNonNull(executor, "executor");
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
    public final Universe getUniverse() {
        return universe;
    }

}
