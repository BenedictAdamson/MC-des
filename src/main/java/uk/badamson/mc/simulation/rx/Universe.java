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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uk.badamson.mc.simulation.ObjectStateId;

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

    private static final int MAX_SORT = 32;

    @Nonnull
    private static Collection<ObjectStateId> sortIfSmall(@Nonnull final Collection<ObjectStateId> ids) {
        final var size = ids.size();
        if (1 < size && size <= MAX_SORT) {
            return new TreeSet<>(ids);
        } else {
            return ids;
        }
    }

    private final Map<UUID, ModifiableObjectHistory<STATE>> objectHistories = new ConcurrentHashMap<>();
    /*
     * Adding entries to the objectHistories Map is guarded by this lock.
     */
    private final Object objectCreationLock = new Object();

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
     * Construct a universe given the histories of all the objects in it.
     * </p>
     * <ul>
     * <li>The {@linkplain #getObjectHistories() object histories} of this universe
     * is {@linkplain Map#equals(Object) equivalent to} the given
     * {@code objectHistories}.</li>
     * </ul>
     *
     * @param objectHistories
     *            A snapshot of the history information of all the objects in the
     *            universe.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code objectHistories} is null.</li>
     *             <li>If {@code objectHistories} has a null
     *             {@linkplain Map#keySet() key}.</li>
     *             <li>If {@code objectHistories} has a null
     *             {@linkplain Map#values() value}.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If {@code objectHistories} has an {@linkplain Map#entrySet()
     *             entry} for which the {@linkplain ObjectHistory#getObject()
     *             object} of the value of the entry is not
     *             {@linkplain UUID#equals(Object) equivalent to} the key of the
     *             entry.
     */
    public Universe(@Nonnull final Map<UUID, ObjectHistory<STATE>> objectHistories) {
        Objects.requireNonNull(objectHistories, "objectHistories");
        if (objectHistories.entrySet().stream()
                .anyMatch(entry -> !entry.getKey().equals(entry.getValue().getObject()))) {
            throw new IllegalArgumentException("objectHistories");
        }
        objectHistories.forEach((object, history) -> this.objectHistories.put(history.getObject(),
                new ModifiableObjectHistory<>(history)));
    }

    /**
     * <p>
     * Copy a universe.
     * </p>
     *
     * @throws NullPointerException
     *             If {@code that} is null.
     */
    public Universe(@Nonnull final Universe<STATE> that) {
        Objects.requireNonNull(that, "that");
        synchronized (that.objectCreationLock) {// hard to test
            that.objectHistories
                    .forEach((object, history) -> objectHistories.put(object, new ModifiableObjectHistory<>(history)));
        }
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

        synchronized (objectCreationLock) {
            objectHistories.compute(event.getObject(), (k, v) -> {
                if (v == null) {
                    return new ModifiableObjectHistory<>(event);
                } else {
                    throw new IllegalArgumentException("Already present");
                }
            });
        }
    }

    /**
     * <p>
     * Advance the simulation of this universe so the
     * {@linkplain #observeState(UUID, Duration) observable states} of all the
     * {@linkplain #getObjects() objects} of this universe for all times
     * {@linkplain Duration#compareTo(Duration) at or before} a given time are
     * reliable states.
     * </p>
     * <p>
     * That is, {@link #observeState(UUID, Duration)} sequences for all times
     * <var>t</var> &le; {@code when} will immediately complete. Some (typically
     * most) objects of this universe will also have reliable state information for
     * a short time after the given time.
     * </p>
     * <p>
     * The computation may make use of multiple threads.
     * </p>
     *
     * @param when
     *            the latest point in time for which all objects must have reliable
     *            state information.
     * @param nThreads
     *            the number of threads to use for the computation; the degree of
     *            parallelism. This value should not normally greatly exceed the
     *            number of CPU cores.
     * @return a sequence that completes when all the states have been advanced.
     * @throws NullPointerException
     *             If {@code when} is null.
     * @throws IllegalArgumentException
     *             If {@code nThreads} is not positive.
     */
    @Nonnull
    public Mono<Void> advanceStatesTo(@Nonnull final Duration when, final int nThreads) {
        Objects.requireNonNull(when, "when");
        if (nThreads < 1) {
            throw new IllegalArgumentException("nThreads not positive");
        }

        return Flux.fromStream(getInitialAdvanceObjectives(when))
                .expand(step -> Flux.fromIterable(expandAdvanceStep(step))).parallel(nThreads)
                .runOn(Schedulers.parallel()).then();
    }

    private void applyEvent(@Nonnull final Event<STATE> expectedLastEvent, @Nonnull final Event<STATE> event) {
        objectHistories.compute(event.getObject(), (k, history) -> {
            if (history == null) {
                return new ModifiableObjectHistory<>(event);
            } else {
                history.compareAndAppend(expectedLastEvent, event);
                /*
                 * Ignore failure to append: indicates that another thread has appended the
                 * event for us.
                 */
                return history;
            }
        });
    }

    private void applyNextEvents(@Nonnull final Event<STATE> lastEvent,
            @Nonnull final Map<UUID, Event<STATE>> nextEvents) {
        assert 1 <= nextEvents.size();
        if (nextEvents.size() == 1) {
            nextEvents.values().forEach(event -> applyEvent(lastEvent, event));
        } else {
            // creation events too
            synchronized (objectCreationLock) {
                nextEvents.values().forEach(event -> applyEvent(lastEvent, event));
            }
        }
    }

    /**
     * <p>
     * Whether this is <dfn>equivalent</dfn> to a given object.
     * </p>
     *
     * <p>
     * The {@link Universe} class has <i>value semantics</i>: this is equivalent to
     * a given object if, and only if, the other is also a {@link Universe}, and
     * they have {@linkplain Map#equals(Object) equivalent}
     * {@linkplain #getObjectHistories() object histories}.
     * </p>
     * <p>
     * This method may give misleading results if either object is modified during
     * the computation of equality.
     * </p>
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Universe)) {
            return false;
        }
        final Universe<?> other = (Universe<?>) obj;
        /*
         * thread-safe because ConcurrentHashMap.equals and
         * ModifiableObjectHistory.equals is threads-safe.
         */
        return objectHistories.equals(other.objectHistories);
    }

    private Collection<ObjectStateId> expandAdvanceStep(@Nonnull final ObjectStateId step) {
        final var history = objectHistories.get(step.getObject());
        final var when = step.getWhen();
        while (history.getEnd().compareTo(when) < 0) {// (advancing is necessary)
            final var lastEvent = history.getLastEvent();
            final var dependencies = lastEvent.getNextEventDependencies();
            final var subSteps = new ArrayList<ObjectStateId>(dependencies.size());
            dependencies.entrySet().stream().filter(entry -> {
                final var dependent = entry.getKey();
                final var dependentTime = entry.getValue();
                /*
                 * If we do not yet know the state of the dependent, computing that state is a
                 * sub step.
                 */
                return objectHistories.get(dependent).getEnd().compareTo(dependentTime) < 0;
            }).forEach(entry -> subSteps.add(new ObjectStateId(entry.getKey(), entry.getValue())));
            if (subSteps.isEmpty()) {
                // Can advance right now
                observeNextEvents(lastEvent).doOnNext(events -> applyNextEvents(lastEvent, events)).then().block();
                // But have we advanced far enough? May iterate again.
            } else {
                // Defer the step until its predecessors are done
                subSteps.add(step);
                /*
                 * Ideally, do the sub steps in ascending time-stamp order, so interdependencies
                 * are automatically satisfied. However, do not get bogged down doing an
                 * expensive sort if there are many dependencies.
                 */
                return sortIfSmall(subSteps);
            }
        } // while
        return List.of();
    }

    private Stream<ObjectStateId> getInitialAdvanceObjectives(@Nonnull final Duration when) {
        return List.copyOf(objectHistories.values()).stream().filter(history -> history.getEnd().compareTo(when) < 0)
                .map(history -> new ObjectStateId(history.getObject(), when));
    }

    /**
     * <p>
     * Get a snapshot of the history information of all the objects in this
     * universe.
     * </p>
     * <ul>
     * <li>A map of an object ID to the history of the object that has that ID.</li>
     * <li>The map will not subsequently change due to events.</li>
     * <li>Has no null {@linkplain Map#keySet() keys}.</li>
     * <li>Has no null {@linkplain Map#values() values}.</li>
     * <li>Has only {@linkplain Map#entrySet() entries} for which the
     * {@linkplain ObjectHistory#getObject() object} of the value of the entry is
     * the same as the key of the entry.</li>
     * <li>Has a {@linkplain Map#keySet() set of keys}
     * {@linkplain Set#equals(Object) equivalent to} the {@linkplain #getObjects()
     * set of object IDs}.</li>
     * </ul>
     */
    @Nonnull
    public Map<UUID, ObjectHistory<STATE>> getObjectHistories() {
        final Map<UUID, ObjectHistory<STATE>> copy = new HashMap<>(objectHistories.size());
        synchronized (objectCreationLock) {// hard to test
            objectHistories.forEach((object, history) -> copy.put(object, new ObjectHistory<>(history)));
        }
        return copy;
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

    @Override
    public int hashCode() {
        return objectHistories.hashCode();
    }

    /**
     * <p>
     * Provide the next event after a given event, and any associated <i>object
     * creation events</i>, for an {@linkplain #getObjects() object} in this
     * universe.
     * </p>
     *
     * <p>
     * The event in the sequence of next events is as
     * {@linkplain Event#computeNextEvents(Map) computed as the next event} by the
     * given event, using the correct (non provisional) states of the dependencies.
     * </p>
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
    public Mono<Map<UUID, Event<STATE>>> observeNextEvents(@Nonnull final Event<STATE> event) {
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

            return Mono.zip(dependencyObservers, (states) -> {
                final Map<UUID, STATE> dependentStates = new HashMap<>();
                for (int d = 0; d < nDependencies; ++d) {
                    final UUID id = dependencyIds.get(d);
                    @SuppressWarnings("unchecked")
                    final Optional<STATE> dependencyState = (Optional<STATE>) states[d];
                    if (dependencyState.isPresent()) {
                        dependentStates.put(id, dependencyState.get());
                    }
                }
                return event.computeNextEvents(dependentStates);
            });
        } else {// special case
            return Mono.just(event.computeNextEvents(Map.of()));
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

    @Override
    public String toString() {
        return "Universe" + objectHistories.values();
    }

}
