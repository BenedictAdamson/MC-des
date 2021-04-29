/**
 * <p>
 * A reactive multi-threaded parallel discrete event simulation (PDES) engine.
 * </p>
 * <ul>
 * <li>This provides a framework for performing simulations of systems on a
 * shared-memory computer using multiple threads. It is therefore suitable only
 * for systems that can record a complete snapshot of their state in the RAM of
 * a practical computer.</li>
 * <li>It is for simulating <dfn>asynchronous</dfn> systems, for which the times
 * of events are widely scattered, so advancement of the simulated state using a
 * global clock is inefficient.</li>
 * <li>In common with most PDES algorithms, this framework partly enables
 * parallel computation by eliminating an explicit global (shared) <i>event
 * list</i>.</li>
 * <li>This framework further enables parallel computation by not attempting to
 * compute and record a single set of <i>state variables</i> at one point in
 * time. It instead computes and records the full time history of all the state
 * variables within a time period of interest.</li>
 * <li>Unlike other PDES algorithms, the framework does not mandate a
 * <i>process-oriented</i> methodology, although it can be used in that manner.
 * Instead, the system is modelled as composed as a set of objects, each of
 * which has a time-varying state.</li>
 * <li>Unlike other PDES algorithms, explicit message passing between
 * continually running <i>logical processes</i> is not used to perform the
 * computation.</li>
 * <li>The algorithm of the framework prevents causality violations by recording
 * (state read) dependency relationships. The framework is
 * <dfn>optimistic</dfn>: work proceeds assuming that it will not violate
 * causality, but is noted as invalid if the dependency information indicates
 * that the assumption is invalid. Invalid work is eventually rescheduling, to
 * ensure progress eventually occurs.</li>
 * <li>Unlike PDES algorithms using <i>logical processes</i>, the simulation
 * events of this framework may access (read) the states of any number of
 * objects.</li>
 * <li>The framework enforces causality and partly avoids deadlock by requiring
 * that the time-stamps of states read by a transaction are strictly before the
 * time-stamp of the state (or states) written by the transaction.</li>
 * <li>Only the (state read) dependencies between transactions constrain the
 * order of their computation. Therefore computation of the state histories for
 * different objects can proceed at different rates, if those objects are
 * uncoupled (or only loosely coupled) by dependencies.</li>
 * <li>The framework reduces the memory required to record all the state
 * histories by recording only the changes in the object states. It assumes that
 * the object states will be recorded as collections of immutable objects, so
 * successive states can reuse (share) references to immutable objects for the
 * parts of the state that have not changed, to further reduce the memory
 * required.</li>
 * </ul>
 */
package uk.badamson.mc.simulation.rx;
/*
 * © Copyright Benedict Adamson 2018,21.
 *
 * This file is part of MC-des.
 *
 * MC-des is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * MC-des is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MC-des. If not, see <https://www.gnu.org/licenses/>.
 */