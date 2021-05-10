package uk.badamson.mc.simulation.rx;
/*
 * © Copyright Benedict Adamson 2018,2021.
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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
     * Construct an object history with given start information.
     * </p>
     * <ul>
     * <li>The {@linkplain #getLastEvent() last event} of this history is the given
     * {@code event}.</li>
     * </ul>
     *
     * @param event
     *            The first (known) state transition of the {@linkplain #getObject()
     *            object}.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code event} is null</li>
     *             <li>if the {@linkplain Event#getState() state} of {@code event}
     *             is null. That is, if the first event is the destruction or
     *             removal of the {@linkplain #getObject() simulated object}.</li>
     *             </ul>
     */
    public ModifiableObjectHistory(@Nonnull final Event<STATE> event) {
        super(event);
    }

    /**
     * <p>
     * Copy an object history.
     * </p>
     *
     * @throws NullPointerException
     *             If {@code that} is null
     *
     */
    public ModifiableObjectHistory(@Nonnull final ObjectHistory<STATE> that) {
        super(that);
    }

    /**
     * <p>
     * Construct an object history with given history information.
     * </p>
     * <ul>
     * <li>The {@linkplain #getLastEvent() last event} of this history is the given
     * {@code event}.</li>
     * <li>The {@linkplain #getPreviousStateTransitions() previous state
     * transitions} of this history is {@linkplain SortedMap#equals(Object)
     * equivalent to} the given {@code previousStateTransitions}
     * </ul>
     *
     * @param previousStateTransitions
     *            The state transitions before the {@code lastEvent}
     * @param lastEvent
     *            The last (known) state transition of the {@linkplain #getObject()
     *            object}.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code stateTransitions} is null</li>
     *             <li>If {@code lastEvent} is null</li>
     *             <li>If {@code stateTransitions} is empty and the
     *             {@linkplain Event#getState() state} of {@code lastEvent} is null.
     *             That is, if the first event is the destruction or removal of the
     *             {@linkplain #getObject() simulated object}.</li>
     *             <li>If {@code stateTransitions} has any null
     *             {@linkplain SortedMap#values() values}. That is, if the object
     *             was destroyed or removed before the {@code lastEvent} .</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the {@linkplain Event#getWhen() time} of the
     *             {@code lastEvent} is not {@linkplain Duration#compareTo(Duration)
     *             after} the {@linkplain SortedMap#lastKey() last} of the given
     *             {@code previousStateTransitions}.</li>
     *             <li>If any {@linkplain SortedMap#values() value} of
     *             {@code previousStateTransitions} is
     *             {@linkplain Objects#equals(Object, Object) equal to (or equally
     *             null as)} its predecessor value.</li>
     *             </ul>
     */
    @JsonCreator
    public ModifiableObjectHistory(
            @Nonnull @JsonProperty("previousStateTransitions") final SortedMap<Duration, STATE> previousStateTransitions,
            @JsonProperty("lastEvent") @Nonnull final Event<STATE> lastEvent) {
        super(previousStateTransitions, lastEvent);
    }

    /**
     * <p>
     * Append an event to this history.
     * </p>
     *
     * <ul>
     * <li>The {@linkplain #getLastEvent() last event} becomes the same as the given
     * {@code event}.</li>
     * <li>Any subscribers to the {@linkplain #observeStateTransitions() sequence of
     * state transitions} will receive a new state transition corresponding the the
     * {@code event}:
     * <ul>
     * <li>The {@linkplain ObjectHistory.TimestampedState#getWhen() time} of the
     * state transition will be the same as the {@linkplain Event#getWhen() time} of
     * the {@code event}.</li>
     * <li>The {@linkplain ObjectHistory.TimestampedState#getState() state} of the
     * state transition will be the same as the {@linkplain Event#getState() state}
     * of the {@code event}.</li>
     * </ul>
     * </ul>
     *
     * @param event
     *            The event to append. The {@linkplain Event#getState() state}
     *            transitioned to by the event may be equal to the state
     *            transitioned to by the current last event, but that should be
     *            avoided for performance reasons.
     * @throws NullPointerException
     *             If {@code event} is null
     * @throws IllegalArgumentException
     *             If the {@linkplain Event#getObject() object} of the {@code event}
     *             is not the same as the {@linkplain #getObject() object} of this
     *             history.
     * @throws IllegalStateException
     *             <li>If the {@linkplain Event#getWhen() time} of the {@code event}
     *             is not {@linkplain Duration#compareTo(Duration) after} the time
     *             of the {@linkplain #getLastEvent() last event} of this history.
     *             That can happen if a different thread appended an event.</li>
     *             <li>If the {@linkplain Event#getState() state} of the current
     *             {@linkplain #getLastEvent() last event} is null. That is, if the
     *             current last event was the destruction or removal of the
     *             {@linkplain #getObject() simulated object}: destroyed objects may
     *             not be resurrected.
     *
     * @see #compareAndAppend(Event, Event)
     */
    public void append(@Nonnull final Event<STATE> event) {
        Objects.requireNonNull(event, "event");
        if (getObject() != event.getObject()) {
            throw new IllegalArgumentException("event.getObject");
        }
        synchronized (lock) {
            if (0 <= lastEvent.getWhen().compareTo(event.getWhen())) {
                throw new IllegalStateException("event.getWhen");
            }
            if (lastEvent.getState() == null) {
                throw new IllegalStateException("resurrection attempted");
            }
            append1(event);
        }
    }

    /**
     * <p>
     * Append an event to this history, if the current {@linkplain #getLastEvent()
     * last event} has a given expected value.
     * </p>
     *
     * <ul>
     * <li>If the method returns {@code true}, it has the same effect as if
     * {@link #append(Event)} had been called with {@code event}.</li>
     * <li>If the method returns {@code false}, it has no effect.</li>
     * </ul>
     * <p>
     * This provides better thread safety than the {@link #append(Event)} method.
     * </p>
     *
     * @param expectedLastEvent
     *            The expected current last event.
     * @param event
     *            The event to append. The {@linkplain Event#getState() state}
     *            transitioned to by {@code event} may be equal to the state
     *            transitioned to by the {@code expectedLastEvent}, but that should
     *            be avoided for performance reasons.
     * @return The method returns whether the {@linkplain #getLastEvent() last
     *         event} was the same as the given {@code expectedLastEvent}, in which
     *         case it successfully appended the {@code event}.
     *
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code expectedLastEvent} is null.</li>
     *             <li>If {@code event} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the {@linkplain Event#getObject() object} of the
     *             {@code event} is not the same as the {@linkplain #getObject()
     *             object} of this history.</li>
     *             <li>If the {@linkplain Event#getWhen() time} of the {@code event}
     *             is not {@linkplain Duration#compareTo(Duration) after} the time
     *             of the {@code expectedLastEvent}</li>
     *             <li>If the {@linkplain Event#getState() state} of the
     *             {@code expectedLastEvent} is null. That is, if the expected last
     *             event was the destruction or removal of the
     *             {@linkplain #getObject() simulated object}: destroyed objects may
     *             not be resurrected.
     *             </ul>
     *
     * @see #append(Event)
     */
    public boolean compareAndAppend(@Nonnull final Event<STATE> expectedLastEvent, @Nonnull final Event<STATE> event) {
        Objects.requireNonNull(expectedLastEvent, "expectedLastEvent");
        Objects.requireNonNull(event, "event");
        if (getObject() != event.getObject()) {
            throw new IllegalArgumentException("event.getObject");
        }
        if (0 <= expectedLastEvent.getWhen().compareTo(event.getWhen())) {
            throw new IllegalStateException("event.getWhen");
        }
        if (expectedLastEvent.getState() == null) {
            throw new IllegalStateException("resurrection attempted");
        }

        synchronized (lock) {
            final boolean success = lastEvent == expectedLastEvent;
            if (success) {
                append1(event);
            }
            return success;
        }
    }
}
