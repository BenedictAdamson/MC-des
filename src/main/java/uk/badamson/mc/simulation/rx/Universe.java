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

import static java.util.stream.Collectors.toUnmodifiableList;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A collection of simulated objects and their {@linkplain ObjectHistory
 * histories}.
 * </p>
 * <p>
 * The histories of the objects may be <dfn>asynchronous</dfn>: different
 * objects may have state transitions at different times.</li>
 * <p>
 * This collection is modifiable: the histories of the simulated objects may be
 * appended to. This collection enforces constraints that ensure that the object
 * histories are <dfn>consistent</dfn>. Consistency means that if a universe
 * contains an object state, it also contains all the depended upon states of
 * that state.
 * </p>
 *
 * @param <STATE>
 *            The class of states of the simulated objects. This must be
 *            {@link Immutable immutable}. It ought to have value semantics, but
 *            that is not required.
 */
@ThreadSafe
public final class Universe<STATE> {

    private final Map<UUID, ObjectHistory<STATE>> objectHistories = new ConcurrentHashMap<>();

    /**
     * <p>
     * Construct an empty universe.
     * </p>
     * <ul>
     * <li>The {@linkplain #getObjects() set of objects} {@linkplain Set#isEmpty()
     * is empty}.</li>
     * </ul>
     */
    public Universe() {
        // Do nothing
    }

    /**
     * <p>
     * Add an object with given start information to the collection of objects in
     * this universe.
     * </p>
     * <p>
     * Invariants:
     * </p>
     * <ul>
     * <li>Does not remove any objects from the {@linkplain #getObjects() set of
     * objects}.</li>
     * </ul>
     * <p>
     * Post conditions:
     * </p>
     * <ul>
     * <li>The {@linkplain #getObjects() set of objects}
     * {@linkplain Set#contains(Object) contains} the {@linkplain Event#getObject()
     * object} of the {@code event}.</li>
     * <li>Adds one object to the set of objects.</li>
     * <li>The {@linkplain #observeState(UUID, Duration) observable state} of the
     * object of the {@code event} at the {@linkplain Event#getWhen() time} of the
     * event is an immediately completing sequence holding only the
     * {@linkplain Event#getState() state} of the event.</li>
     * </ul>
     *
     * @param event
     *            The first (known) state transition of the object.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code event} is null</li>
     *             <li>if the {@linkplain Event#getState() state} of {@code event}
     *             is null. That is, if the first event is the destruction or
     *             removal of the simulated object.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If the {@linkplain #getObjects() set of objects} in this universe
     *             already {@linkplain Set#contains(Object) contains} the
     *             {@linkplain Event#getObject() object} of the {@code event}.
     */
    public void addObject(@Nonnull final Event<STATE> event) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(event.getState(), "event.state");

        objectHistories.compute(event.getObject(), (k, v) -> {
            if (v == null) {
                return new ObjectHistory<>(event);
            } else {
                throw new IllegalArgumentException("Already present");
            }
        });
    }

    /**
     * <p>
     * Start computation of the {@linkplain #observeNextEvent(Event) next event} of
     * an {@linkplain #getObjects() object} in this universe, and append that event
     * to the history of events (state transitions) of the object, publishing new
     * state information to {@linkplain #observeState(UUID, Duration) observers of
     * the state information} if necessary.
     * </p>
     * <p>
     * The computation may halt, waiting until
     * {@linkplain #observeState(UUID, Duration) reliable state information} of
     * {@linkplain Event#getNextEventDependencies() dependent objects} becomes
     * available.
     * </p>
     *
     * @param object
     *            The unique ID of the object for which to advance.
     * @throws NullPointerException
     *             If {@code object} is null.
     * @throws IllegalArgumentException
     *             If {@code object} is not the ID of ab {@linkplain #getObjects()
     *             object} in this universe.
     */
    public void advanceState(@Nonnull final UUID object) {
        Objects.requireNonNull(object, "object");
        final var objectHistory = objectHistories.get(object);
        if (objectHistory == null) {
            throw new IllegalArgumentException("Unknown object");
        }
        final var oldLastEvent = objectHistory.getLastEvent();
        Flux.from(observeNextEvent(oldLastEvent))
                .subscribe(newLastEvent -> objectHistory.compareAndAppend(oldLastEvent, newLastEvent));
    }

    /**
     * <p>
     * The unique IDs of the simulated objects in this universe.
     * </p>
     * <ul>
     * <li>The set of object IDs is not null.</li>
     * <li>The set of object IDs does not contain a null.</li>
     * <li>The set of object IDs is an unmodifiable copy (snapshot) of the set of
     * object IDs; the returned set is constant, and will not update because of
     * subsequent additions of objects.</li>
     * </ul>
     */
    @Nonnull
    public Set<UUID> getObjects() {
        return Set.copyOf(objectHistories.keySet());
    }

    /**
     * <p>
     * Provide the next event after a given event for an {@linkplain #getObjects()
     * object} in this universe.
     * </p>
     *
     * <ul>
     * <li>The sequence of next events is finite.</li>
     * <li>Events in the sequence of next events are for the same object as the
     * {@linkplain Event#getObject() object} of the given event.</li>
     * <li>Events in the sequence of next events are for a time after the
     * {@linkplain Event#getWhen() time} of the given event.</li>
     * <li>The sequence of next events is {@linkplain Event#computeNextEvent(Map)
     * computed} using the given event and the
     * {@linkplain #observeState(UUID, Duration) states} of the
     * {@linkplain Event#getNextEventDependencies() dependencies} of the event.</li>
     * <li>The last event of the sequence of next events is the correct next event
     * for the given event, computed using the given event and the correct states of
     * the dependencies.</li>
     * <li>The last event of the sequence of next events may be proceeded may
     * <i>provisional</i> values for the next event. The provisional values will
     * have been computed using provisional values for the states of the
     * dependencies. These provisional values will typically be approximations of
     * the correct value, with successive values being closer to the correct
     * value.</li>
     * <li>The time between publication of the last event of the sequence and
     * completion of the sequence can be a large. That is, the process of providing
     * a value and then concluding that it is the correct value rather than a
     * provisional value can be time consuming.</li>
     * </ul>
     *
     * @param event
     *            The event for which to provide the next event. The
     *            {@linkplain Event#getObject() simulated object} is typically one
     *            of the {@linkplain #getObjects() objects} in this universe, and
     *            the event will typically be the latest event of that object, but
     *            that is not required.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code event} is null</li>
     *             <li>If the {@linkplain Event#getState() state} transitioned to
     *             due to the {@code event} is null. That is, if {@code event} was
     *             the destruction or removal of the {@linkplain Event#getObject()
     *             simulated object}: destroyed objects may not be resurrected.</li>
     *             </ul>
     */
    @Nonnull
    public Mono<Event<STATE>> observeNextEvent(@Nonnull final Event<STATE> event) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(event.getState(), "event.state");

        final var nextEventDependencies = event.getNextEventDependencies();
        final int nDependencies = nextEventDependencies.size();
        if (0 < nDependencies) {
            final var dependencyIds = nextEventDependencies.entrySet().stream().sequential()
                    .map(entry -> entry.getKey()).collect(toUnmodifiableList());
            final var dependencyObservers = nextEventDependencies.entrySet().stream().sequential()
                    .map(entry -> Flux.from(observeState(entry.getKey(), entry.getValue())).last())
                    .collect(toUnmodifiableList());

            return Flux.combineLatest(dependencyObservers, (states) -> {
                final Map<UUID, STATE> dependentStates = new HashMap<>();
                for (int d = 0; d < nDependencies; ++d) {
                    final UUID id = dependencyIds.get(d);
                    @SuppressWarnings("unchecked")
                    final Optional<STATE> dependencyState = (Optional<STATE>) states[d];
                    if (dependencyState.isPresent()) {
                        dependentStates.put(id, dependencyState.get());
                    }
                }
                return event.computeNextEvent(dependentStates);
            }).last();
        } else {// special case
            return Mono.just(event.computeNextEvent(Map.of()));
        }
    }

    /**
     * <p>
     * Provide the state of a simulated object at a given point in time.
     * </p>
     * <ul>
     * <li>Because {@link Publisher} can not provide null values, the sequence uses
     * {@link Optional}, with null states (that is, the state of not existing)
     * indicated by an {@linkplain Optional#isEmpty() empty} Optional.</li>
     * <li>The sequence of states is finite.</li>
     * <li>The last state of the sequence of states is the state at the given point
     * in time.</li>
     * <li>The last state of the sequence of states may be proceeded may
     * <i>provisional</i> values for the state at the given point in time. These
     * provisional values will typically be approximations of the correct value,
     * with successive values being closer to the correct value.</li>
     * <li>The sequence of states does not contain successive duplicates.</li>
     * <li>The time between publication of the last state of the sequence and
     * completion of the sequence can be a large. That is, the process of providing
     * a value and then concluding that it is the correct value rather than a
     * provisional value can be time consuming.</li>
     * </ul>
     *
     * @param object
     *            The unique ID of the object for which the state is wanted.
     * @param when
     *            The point in time of interest, expressed as the duration since an
     *            (implied) epoch. All objects in this universe should use the same
     *            epoch.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If the {@code object} is not one of the {@linkplain #getObjects()
     *             objects} in this universe.
     */
    @Nonnull
    public Publisher<Optional<STATE>> observeState(@Nonnull final UUID object, @Nonnull final Duration when) {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(when, "when");
        final var objectHistory = objectHistories.get(object);
        if (objectHistory == null) {
            throw new IllegalArgumentException("Not an object of this universe");
        }
        return objectHistory.observeState(when);
    }
}
