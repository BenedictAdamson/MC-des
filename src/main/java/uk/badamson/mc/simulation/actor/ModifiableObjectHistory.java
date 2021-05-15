package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2021.
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
import java.util.Objects;
import java.util.SortedMap;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.badamson.mc.history.ValueHistory;

/**
 * <p>
 * The sequence of state transitions of a simulated object that may be appended
 * to.
 * </p>
 *
 * @param <STATE>
 *            The class of states of the simulated object. This must be
 *            {@link Immutable immutable}. It ought to have value semantics, but
 *            that is not required.
 */
@ThreadSafe
public final class ModifiableObjectHistory<STATE> extends ObjectHistory<STATE> {

    /**
     * <p>
     * Copy an object history.
     * </p>
     */
    public ModifiableObjectHistory(@Nonnull final ObjectHistory<STATE> that) {
        super(that);
    }

    /**
     * <p>
     * Construct an object history with given history information.
     * </p>
     * <ul>
     *
     * @param object
     *            The unique ID of the object for which this is the history.
     * @param end
     *            The last point in time for which this history is reliable.
     * @param stateTransitions
     *            The state transitions
     * @throws NullPointerException
     *             If any {@link Nonnull} argument is null.
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code stateTransitions} is empty.</li>
     *             <li>If {@code end} is {@linkplain Duration#compareTo(Duration)
     *             before} the first time in {@code stateTransitions}.</li>
     *             <li>If adjacent {@linkplain SortedMap#values() values} of
     *             {@code stateTransitions} are
     *             {@linkplain Objects#equals(Object, Object) equivalent or
     *             equivalently null}.</li>
     *             <li>If {@code stateTransitions} contains more than one null
     *             value.</li>
     *             <li>If {@code stateTransitions} has any null
     *             {@linkplain SortedMap#values() values} other than the last. That
     *             is, if the object was destroyed or removed before the last
     *             transition.</li>
     *             <li>The the final value in {@code stateTransitions} is null and
     *             the final state transition is at or before the {@code end} time,
     *             but the {@code end} time is not the
     *             {@linkplain ValueHistory#END_OF_TIME end of time}. That is, if
     *             the arguments indicate that there is reliable information
     *             indicating destruction of the simulated object, but the
     *             reliability information suggests it might be resurrected at a
     *             later time.</li>
     *             </ul>
     */
    @JsonCreator
    public ModifiableObjectHistory(@Nonnull @JsonProperty("object") final UUID object,
            @Nonnull @JsonProperty("end") final Duration end,
            @Nonnull @JsonProperty("stateTransitions") final SortedMap<Duration, STATE> stateTransitions) {
        super(object, end, stateTransitions);
    }

    /**
     * <p>
     * Construct an object history with given start information.
     * </p>
     *
     * @param object
     *            The unique ID of the object for which this is the history.
     * @param start
     *            The point in time that this history starts.
     * @param state
     *            The first (known) state transition of the {@code
     *            object}.
     * @throws NullPointerException
     *             If a {@link Nonnull} argument is null
     */
    public ModifiableObjectHistory(@Nonnull final UUID object, @Nonnull final Duration start,
            @Nonnull final STATE state) {
        super(object, start, state);
    }

}
