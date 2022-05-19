package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2018,2021-22.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * A simulated object that (potentially) has a time-wise varying state,
 * which varies only as a result of receiving {@linkplain Signal signals}.
 * </p>
 *
 * @param <STATE> The class of states of the simulated object. This must be
 *                {@link Immutable immutable}. It ought to have value semantics, but
 *                that is not required.
 */
@ThreadSafe
public final class Actor<STATE> {

    /**
     * Comparable so can predictably order locks to avoid deadlock.
     */
    final UUID lock = UUID.randomUUID();

    @GuardedBy("lock")
    private final ModifiableValueHistory<STATE> stateHistory = new ModifiableValueHistory<>();

    @GuardedBy("lock")
    private final NavigableSet<Event<STATE>> events = new TreeSet<>();

    @GuardedBy("lock")
    private final Set<Signal<STATE>> unscheduledSignalsToReceive = new HashSet<>();

    @GuardedBy("lock")
    private final Set<Signal<STATE>> signalsToReceive = new HashSet<>();

    @GuardedBy("lock")
    private final Map<Signal<STATE>, Event<STATE>> eventsForSignals = new HashMap<>();

    @GuardedBy("lock")
    Signal<STATE> nextSignalToReceive = null;

    @GuardedBy("lock")
    Duration whenReceiveNextSignal = Signal.NEVER_RECEIVED;

    @GuardedBy("lock")
    @Nonnull
    private Duration start;

    @GuardedBy("lock")
    private long version;

    /**
     * <p>
     * Construct an actor with given start information and no events.
     * </p>
     * <ul>
     * <li>The {@linkplain #getEvents() events} sequence
     * {@linkplain List#isEmpty() is empty}.</li>
     * </ul>
     *
     * @param start The first point in time for which the actor has a known state.
     * @param state The first (known) state of the actor.
     */
    public Actor(@Nonnull final Duration start, @Nonnull final STATE state) {
        Objects.requireNonNull(state, "state");
        this.start = Objects.requireNonNull(start, "start");
        this.stateHistory.appendTransition(start, state);
    }

    private static <STATE> int compareTo(
            @Nonnull final Signal<STATE> signal1,
            @Nonnull final Duration whenReceived1,
            @Nullable final Signal<STATE> signal2,
            @Nonnull final Duration whenReceived2
    ) {
        int compare;
        if (signal2 == null) {
            compare = -1;
        } else {
            compare = whenReceived1.compareTo(whenReceived2);
            if (compare == 0) {
                compare = signal1.tieBreakCompareTo(signal2);
            }
        }
        return compare;
    }

    @Nullable
    private static <STATE, RESULT> RESULT doWithAllActorsLocked(
            @Nonnull final NavigableMap<Actor<STATE>, Long> actorVersions,
            @Nonnull final Supplier<RESULT> operation) {
        if (actorVersions.isEmpty()) {
            return operation.get();
        } else {
            final var firstEntry = actorVersions.firstEntry();
            final Actor<STATE> firstActor = firstEntry.getKey();
            final Long firstVersion = firstEntry.getValue();
            final NavigableMap<Actor<STATE>, Long> remainingActorVersions = actorVersions.tailMap(firstActor, false);
            synchronized (firstActor.lock) {
                if (firstActor.version == firstVersion) {
                    return doWithAllActorsLocked(remainingActorVersions, operation);
                } else {
                    return null;
                }
            }
        }
    }

    @Nonnull
    private static <STATE> NavigableMap<Actor<STATE>, Long> createActorToVersionMapInLockOrder() {
        return new TreeMap<>(Comparator.comparing(a -> a.lock));
    }

    static <STATE> CompletableFuture<AffectedActors<STATE>> advanceSeveralActors(
            @Nonnull final Duration when,
            @Nonnull final Collection<Actor<STATE>> actors,
            @Nonnull final Executor executor
    ) {
        if (actors.isEmpty()) {
            return CompletableFuture.completedFuture(AffectedActors.emptyInstance());
        } else {
            final Iterator<Actor<STATE>> a = actors.iterator();
            final Actor<STATE> actor1 = a.next();
            CompletableFuture<AffectedActors<STATE>> result = actor1.advanceTo(when, executor);
            while (a.hasNext()) {
                final Actor<STATE> nextActor = a.next();
                final CompletableFuture<AffectedActors<STATE>> nextFuture = nextActor.advanceTo(when, executor);
                result = result.thenCombine(nextFuture, AffectedActors::plus);
            }
            return result;
        }
    }

    private static <STATE> CompletableFuture<AffectedActors<STATE>> advanceToWithCompletableFuture(
            @Nonnull final Duration when,
            @Nonnull final Set<Actor<STATE>> actors,
            @Nonnull final Executor executor
    ) {
        final int nActors = actors.size();
        if (nActors == 0) {
            return CompletableFuture.completedFuture(null);
        } else if (nActors == 1) {
            final var actor = actors.iterator().next();
            return actor.advanceTo(when, executor);
        } else {
            return advanceSeveralActors(when, actors, executor);
        }
    }

    private static <STATE> void addActorVersionsToLockToRemoveEvents(
            @Nonnull final NavigableMap<Actor<STATE>, Long> versions,
            @Nonnull final Set<Event<STATE>> events) {
        while (!events.isEmpty()) {
            final Event<STATE> event = events.iterator().next();
            events.remove(event);
            for (final var signal : event.getSignalsEmitted()) {
                final var receiver = signal.getReceiver();
                synchronized (receiver.lock) {
                    versions.putIfAbsent(receiver, receiver.version);
                    final var causedEvent = receiver.eventsForSignals.get(signal);
                    if (causedEvent != null) {
                        events.addAll(receiver.events.tailSet(causedEvent, false));
                    }
                }
            }
            for (final var createdActor : event.getCreatedActors()) {
                synchronized (createdActor.lock) {
                    versions.putIfAbsent(createdActor, createdActor.version);
                    events.addAll(createdActor.events);
                }
            }
        }
    }

    @Nonnull
    private static <STATE> AffectedActors<STATE> removeEventsWhileLocked(@Nonnull final Collection<Event<STATE>> invalidatedEvents) {
        //noinspection FieldAccessNotGuarded
        return invalidatedEvents.stream()
                .flatMap(invalidatedEvent -> invalidatedEvent.getSignalsEmitted().stream())
                .map(signal -> signal.getReceiver().removeSignalWhileLocked(signal))
                .reduce(AffectedActors::plus)
                .orElse(AffectedActors.emptyInstance());
    }

    @Nonnull
    private static <STATE> Set<Actor<STATE>> plus(
            @Nonnull final Set<Actor<STATE>> actorsA,
            @Nonnull final Set<Actor<STATE>> actorsB
    ) {
        if (actorsB.isEmpty()) {
            return actorsA;
        } else if (actorsA.isEmpty()) {
            return actorsB;
        } else {
            final Set<Actor<STATE>> result = new HashSet<>(actorsA);
            result.addAll(actorsB);
            return result;
        }
    }

    private CompletableFuture<AffectedActors<STATE>> advanceTo(
            @Nonnull final Duration when,
            @Nonnull final Executor executor) {
        final CompletableFuture<AffectedActors<STATE>> future = new CompletableFuture<>();
        executor.execute(() -> {
            final AffectedActors<STATE> affectedActors;
            try {
                if (getWhenReceiveNextSignal().compareTo(when) < 0) {
                    affectedActors = receiveSignal();
                } else {
                    affectedActors = AffectedActors.emptyInstance();
                }
            } catch (final SignalException e) {
                future.completeExceptionally(e);
                return;
            }
            if (affectedActors.isEmpty()) {
                future.complete(affectedActors);
            } else {
                final Set<Actor<STATE>> furtherActorsToAdvance = plus(affectedActors.getChanged(), affectedActors.getAdded());
                advanceToWithCompletableFuture(when, furtherActorsToAdvance, executor)
                        .handle((indirectlyAffectedActors, exception) -> {
                            if (exception == null) {
                                future.complete(affectedActors.plus(indirectlyAffectedActors));
                            } else {
                                future.completeExceptionally(exception);
                            }
                            return null;
                        });
            }
        });
        return future;
    }

    /**
     * <p>
     * Get a snapshot of the sequence of events that have
     * {@linkplain Event#getAffectedObject() affected} this actor.
     * </p>
     * <ul>
     * <li>The events sequence may be {@linkplain SortedSet#isEmpty() empty}.</li>
     * <li>All events {@linkplain Event#getAffectedObject() affect} this actor.</li>
     * <li>All events {@linkplain Event#getWhen() occurred}
     * {@linkplain Duration#compareTo(Duration) after} the {@linkplain #getStart()
     * start} time of this history.</li>
     * <li>The returned event sequence is a snapshot: it is not
     * updated if this actor is subsequently changed.</li>
     * <li>Note that events may be <i>measured as simultaneous</i>: events can have
     * {@linkplain Duration#equals(Object) equivalent}
     * {@linkplain Event#getWhen() times of occurrence}. However, the state
     * transition(s) due to some <i>measured as simultaneous</i> events will not be
     * apparent in the {@linkplain #getStateHistory() state history}; only the
     * <i>measured as simultaneous</i> event with the largest ID of its causing
     * signal will have its state recorded in the state history.</li>
     * </ul>
     *
     * @see #getStateHistory()
     */
    @Nonnull
    public SortedSet<Event<STATE>> getEvents() {
        synchronized (lock) {// hard to test
            return new TreeSet<>(events);
        }
    }

    /**
     * <p>
     * The last of the {@linkplain #getEvents() events} of this actor.
     * </p>
     * <ul>
     * <li>The last event is null if, and only if, the sequence of events is
     * {@linkplain  List#isEmpty() empty}.</li>
     * <li>This method is likely to be more efficient than using
     * {@link #getEvents()} and then extracting the last event from the
     * sequence.</li>
     * </ul>
     *
     * @see #getEvents()
     */
    @Nullable
    public Event<STATE> getLastEvent() {
        synchronized (lock) {// hard to test
            if (events.isEmpty()) {
                return null;
            } else {
                return events.last();
            }
        }
    }

    /**
     * <p>
     * The earliest point in time for which the state of this actor is known.
     * </p>
     * <p>
     * Expressed as the duration since an (implied) epoch. All objects in a
     * simulation should use the same epoch.
     * </p>
     * <ul>
     * <li>Constant: this always returns the same start time.</li>
     * </ul>
     */
    @Nonnull
    public Duration getStart() {
        synchronized (lock) {
            return start;
        }
    }

    /**
     * <p>
     * Get a snapshot of the history of states that the
     * actor has passed through.
     * </p>
     * <ul>
     * <li>The state history is never {@linkplain ValueHistory#isEmpty()
     * empty}.</li>
     * <li>The {@linkplain ValueHistory#getFirstTransitionTime() first transition
     * time} of the state history is the same as the {@linkplain #getStart() start}
     * time of this actor.</li>
     * <li>The {@linkplain ValueHistory#getFirstValue() state at the start of time}
     * of the state history is null.</li>
     * <li>The {@linkplain Event#getState() state} resulting from an
     * {@linkplain #getEvents() event} is {@linkplain #equals(Object) equivalent to}
     * the {@linkplain ValueHistory#get(Duration) value} of the state history at the
     * {@linkplain Event#getWhen() time of occurrence} of the event.</li>
     * <li>The returned state history is a snapshot: it is not
     * updated if this actor is subsequently changed.</li>
     * </ul>
     */
    @Nonnull
    public ValueHistory<STATE> getStateHistory() {
        synchronized (lock) {// hard to test
            return new ModifiableValueHistory<>(stateHistory);
        }
    }

    /**
     * <p>
     * The signals that, when received, will add to the {@linkplain #getEvents() sequence of events}
     * of this actor, but which have not yet been {@linkplain #receiveSignal() received}.
     * </p>
     * <ul>
     *     <li>Does not contain a null element.</li>
     *     <li>A snapshot: not updated when signals are subsequently {@linkplain  #addSignalToReceive(Signal) added} or
     *     {@linkplain #receiveSignal() received}.</li>
     *     <li>The set may be unmodifiable.</li>
     *     <li>The {@linkplain Signal#getReceiver() receiver} of every signal in the set is this actor.</li>
     *     <li>The {@linkplain Signal#getWhenSent()} sending time} of every signal in the set is
     *     {@linkplain Duration#compareTo(Duration) before} the {@linkplain #getStart() start time} of this actor.</li>
     * </ul>
     */
    @Nonnull
    public Set<Signal<STATE>> getSignalsToReceive() {
        synchronized (lock) {
            if (unscheduledSignalsToReceive.isEmpty()) {
                return Set.copyOf(signalsToReceive);
            } else {
                final Set<Signal<STATE>> result = new HashSet<>(signalsToReceive);
                result.addAll(unscheduledSignalsToReceive);
                return result;
            }
        }
    }

    /**
     * When this actor will next receive a signal,
     * and thus change its {@linkplain #getStateHistory() state}.
     *
     * <ul>
     *     <li>After the {@linkplain #getStart() start} time.</li>
     *     <li>{@link Signal#NEVER_RECEIVED} if {@linkplain Set#isEmpty() no}
     *     {@linkplain #getSignalsToReceive() signals to receive}.</li>
     * </ul>
     *
     * @throws SignalException If a {@link Signal} object throws a {@link RuntimeException}.
     *                         The method is safe if this exception is thrown:
     *                         the state of this Actor will not have changed.
     */
    @Nonnull
    public Duration getWhenReceiveNextSignal() {
        synchronized (lock) {
            return computeNextSignalToReceive();
        }
    }

    /**
     * <p>
     * Add a given event to the {@linkplain #getEvents() sequence of events} of this actor.
     * </p>
     *
     * @throws IllegalArgumentException If this is not the {@linkplain Event#getAffectedObject() affected object} of the event
     * @throws IllegalStateException    <ul>
     *                                  <li>If this has a {@linkplain #getLastEvent() last event}  and the given event is not {@linkplain Event#compareTo(Event) after} the last event</li>
     *                                  <li>If the current {@linkplain #getLastEvent() last event}  is a destruction event (for which the {@linkplain Event#getState() state} is null)</li>
     *                                  </ul>
     */
    public void addEvent(@Nonnull final Event<STATE> event) {
        Objects.requireNonNull(event, "event");
        if (event.getAffectedObject() != this) {
            throw new IllegalArgumentException("this is not the affectedObject of the event");
        }
        synchronized (lock) {
            if (!events.isEmpty()) {
                final var last = events.last();
                if (last.getState() == null) {
                    throw new IllegalStateException("last event event is a destruction event");
                }
                if (last.compareTo(event) >= 0) {
                    throw new IllegalStateException("not after the last event");
                }
            }
            appendEventWhileLocked(event);
        }
    }

    /**
     * <p>
     * Add a signal to the {@linkplain #getSignalsToReceive() set of signals to receive}.
     * </p>
     *
     * @throws IllegalArgumentException If the {@linkplain Signal#getReceiver() receiver} of the {@code signal} is not this actor.
     * @throws IllegalStateException    If the {@linkplain Signal#getWhenSent()} sending time of the {@code signal}
     *                                  is {@linkplain Duration#compareTo(Duration) before} the
     *                                  {@linkplain #getStart() start time} of this actor.
     * @see #receiveSignal()
     */
    public void addSignalToReceive(@Nonnull final Signal<STATE> signal) {
        Objects.requireNonNull(signal, "signal");
        if (signal.getReceiver() != this) {
            throw new IllegalArgumentException("this actor is not the receiver of the signal");
        }
        synchronized (lock) {
            if (signal.getWhenSent().compareTo(start) < 0) {
                throw new IllegalStateException("signal sent before the start time of this actor");
            }
            addUnscheduledSignalToReceive(signal);
        }
    }

    @GuardedBy("lock")
    private void addUnscheduledSignalToReceive(@Nonnull final Signal<STATE> signal) {
        assert Thread.holdsLock(lock);
        if (!eventsForSignals.containsKey(signal) && unscheduledSignalsToReceive.add(signal)) {
            version++;
        }
    }

    /**
     * <p>
     * If this actor has {@linkplain #getSignalsToReceive() signals to receive},
     * receive the first such signal.
     * </p>
     * <p>
     * Although this method removes a signal from the set of signals to receive,
     * it may also add signals for events that have been invalidated by the signal received.
     * If the method {@linkplain Signal#receive(Duration, Object) receives} a signal, that creates a new
     * {@linkplain  #getEvents() event} for this actor,
     * which will be the {@linkplain SortedSet#last() last} of the events.
     * </p>
     *
     * @return The actors affected by this change.
     * <ul>
     *     <li>No {@linkplain AffectedActors#getChanged() changed} actors if, and only if, no signal is received.</li>
     * </ul>
     * @throws SignalException If a {@link Signal} object throws a {@link RuntimeException}.
     *                         The method is safe if this exception is thrown:
     *                         the state of this Actor will not have changed.
     */
    @Nonnull
    public AffectedActors<STATE> receiveSignal() {
        boolean done = false;
        AffectedActors<STATE> totalResult = AffectedActors.emptyInstance();
        do {
            final long previousVersion;
            final Event<STATE> eventToAdd;
            final Event<STATE> eventToRemove;
            synchronized (lock) {
                computeNextSignalToReceive();
                previousVersion = version;
                if (nextSignalToReceive == null) {
                    eventToAdd = null;
                    eventToRemove = null;
                } else {
                    eventToAdd = createNextEvent();
                    final var lastEvent = events.isEmpty() ? null : events.last();
                    if (lastEvent != null && eventToAdd.compareTo(lastEvent) <= 0) {
                        eventToRemove = lastEvent;
                    } else {
                        eventToRemove = null;
                    }
                }
            }
            if (eventToRemove != null) {
                final AffectedActors<STATE> intermediateResult = tryToRemoveEvent(previousVersion, eventToRemove);
                if (intermediateResult != null) {
                    totalResult = totalResult.plus(intermediateResult);
                }
            } else if (eventToAdd != null) {
                final AffectedActors<STATE> intermediateResult = tryToAddEvent(previousVersion, eventToAdd);
                if (intermediateResult != null) {
                    totalResult = totalResult.plus(intermediateResult);
                    done = true;
                }
            } else {
                done = true;
            }
        } while (!done);
        return totalResult;
    }

    /**
     * <p>
     * Remove {@linkplain #getEvents() event} information
     * before a given point in time.
     * </p>
     * <p>Post conditions:</p>
     * <ul>
     *     <li>This has no {@linkplain #getEvents() events} with a {@linkplain Event#getWhen() time of occurrence}
     *     {@linkplain Duration#compareTo(Duration) before} the given time.</li>
     *     <li>If this had any events before the given time, the {@linkplain #getStart() start} time becomes the time of occurrence of the last such event.</li>
     * </ul>
     *
     * @throws IllegalStateException Ifr {@code when} is after the {@linkplain #getWhenReceiveNextSignal() the time of the next signal to receive}.
     */
    public void clearEventsBefore(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        synchronized (lock) {
            final var whenNextSignal = computeNextSignalToReceive();
            if (whenNextSignal != null && whenNextSignal.compareTo(when) < 0) {
                throw new IllegalStateException("when before whenReceiveNextSignal");
            }
            final var e = events.iterator();
            Event<STATE> lastEvent = null;
            while (e.hasNext()) {
                final var event = e.next();
                if (event.getWhen().compareTo(when) < 0) {
                    e.remove();
                    lastEvent = event;
                } else {
                    break;
                }
            }
            if (lastEvent != null) {
                start = lastEvent.getWhen();
                stateHistory.setValueUntil(start.minusNanos(1), null);
            }
        }
    }

    @Nullable
    private AffectedActors<STATE> tryToRemoveEvent(
            final long previousVersion,
            @Nonnull final Event<STATE> event
    ) {
        //noinspection FieldAccessNotGuarded
        return doWithAllActorsLocked(actorVersionsToLockToRemoveEvent(previousVersion, event), () -> removeEventWhileLocked(event));
    }

    @Nullable
    private AffectedActors<STATE> tryToAddEvent(
            final long previousVersion,
            @Nonnull final Event<STATE> event
    ) {
        //noinspection FieldAccessNotGuarded
        return doWithAllActorsLocked(actorVersionsToLockToAddEvent(previousVersion, event), () -> appendEventWhileLocked(event));
    }

    @Nonnull
    private NavigableMap<Actor<STATE>, Long> actorVersionsToLockToAddEvent(
            final long previousVersion,
            @Nonnull final Event<STATE> event) {
        assert event.getAffectedObject() == this;
        final NavigableMap<Actor<STATE>, Long> result = createActorToVersionMapInLockOrder();
        result.put(this, previousVersion);
        for (final var signal : event.getSignalsEmitted()) {
            final var receiver = signal.getReceiver();
            result.putIfAbsent(receiver, receiver.getVersion());
        }
        for (final var createdActor : event.getCreatedActors()) {
            result.putIfAbsent(createdActor, createdActor.getVersion());
        }
        return result;
    }

    @Nonnull
    private NavigableMap<Actor<STATE>, Long> actorVersionsToLockToRemoveEvent(
            final long previousVersion,
            @Nonnull final Event<STATE> event) {
        assert event.getAffectedObject() == this;
        final NavigableMap<Actor<STATE>, Long> result = createActorToVersionMapInLockOrder();
        result.put(this, previousVersion);
        final Set<Event<STATE>> events = new HashSet<>();
        events.add(event);
        addActorVersionsToLockToRemoveEvents(result, events);
        return result;
    }

    @GuardedBy("lock")
    private Duration computeNextSignalToReceive() throws SignalException {
        if (whenReceiveNextSignal == null) {
            nextSignalToReceive = null;
            whenReceiveNextSignal = Signal.NEVER_RECEIVED;
            signalsToReceive.addAll(unscheduledSignalsToReceive);
            unscheduledSignalsToReceive.clear();
            for (final var signal : signalsToReceive) {
                considerAsNextSignalToReceive(signal);
            }
        } else {
            for (final var signal : List.copyOf(unscheduledSignalsToReceive)) {
                considerAsNextSignalToReceive(signal);
            }
        }
        assert unscheduledSignalsToReceive.isEmpty();
        return whenReceiveNextSignal;
    }

    @GuardedBy("lock")
    @Nonnull
    private Event<STATE> createNextEvent() throws SignalException {
        final STATE state = stateHistory.get(whenReceiveNextSignal);
        assert state != null;
        assert nextSignalToReceive != null;
        try {
            return nextSignalToReceive.receive(whenReceiveNextSignal, state);
        } catch (final RuntimeException e) {
            throw new SignalException(nextSignalToReceive, e);
        }
    }

    @GuardedBy("lock")
    private void considerAsNextSignalToReceive(@Nonnull final Signal<STATE> signal) throws SignalException {
        try {
            final Duration whenReceived = computeWhenReceived(signal);
            if (compareTo(signal, whenReceived, nextSignalToReceive, whenReceiveNextSignal) < 0) {
                nextSignalToReceive = signal;
                whenReceiveNextSignal = whenReceived;
            }
            signalsToReceive.add(signal);
            unscheduledSignalsToReceive.remove(signal);
        } catch (final SignalException e) {
            invalidateNextSignalToReceive();
            throw e;
        }
    }

    @Nonnull
    @GuardedBy("lock")
    private Duration computeWhenReceived(@Nonnull final Signal<STATE> signal) throws SignalException {
        try {
            return signal.getWhenReceived(stateHistory);
        } catch (final RuntimeException e) {
            throw new SignalException(signal, e);
        }
    }

    private long getVersion() {
        synchronized (lock) {
            return version;
        }
    }

    @Nonnull
    @GuardedBy("lock")
    private AffectedActors<STATE> appendEventWhileLocked(@Nonnull final Event<STATE> event)
            throws SignalException {
        assert Thread.holdsLock(lock);
        final Signal<STATE> causingSignal = event.getCausingSignal();
        assert !eventsForSignals.containsKey(causingSignal);
        assert this == event.getAffectedObject();
        invalidateNextSignalToReceive();
        version++;
        events.add(event);
        assert events.last() == event;
        eventsForSignals.put(causingSignal, event);
        stateHistory.setValueFrom(event.getWhen(), event.getState());
        signalsToReceive.remove(causingSignal);
        final Collection<Signal<STATE>> signalsEmitted = event.getSignalsEmitted();
        final Set<Actor<STATE>> createdActors = event.getCreatedActors();
        final Set<Actor<STATE>> changedActors = new HashSet<>();
        for (final var emittedSignal : signalsEmitted) {
            final Actor<STATE> receiver = emittedSignal.getReceiver();
            receiver.addUnscheduledSignalToReceive(emittedSignal);
            changedActors.add(receiver);
        }
        changedActors.removeAll(createdActors);
        changedActors.add(this);
        return new AffectedActors<>(changedActors, createdActors, Set.of());
    }

    @Nonnull
    @GuardedBy("lock")
    private AffectedActors<STATE> removeEventWhileLocked(@Nonnull final Event<STATE> event) {
        assert this == event.getAffectedObject();
        assert Thread.holdsLock(lock);
        final List<Event<STATE>> invalidatedEvents = new ArrayList<>(events.tailSet(event, true));
        Collections.reverse(invalidatedEvents);
        final var invalidatedCausingSignals = invalidatedEvents.stream().sequential()
                .map(Event::getCausingSignal)
                .collect(Collectors.toUnmodifiableList());
        invalidatedEvents.forEach(events::remove);
        invalidatedCausingSignals.forEach(eventsForSignals.keySet()::remove);
        signalsToReceive.addAll(invalidatedCausingSignals);
        invalidateNextSignalToReceive();
        version++;
        var result = new AffectedActors<>(Set.of(this), Set.of(), Set.of());
        result = result.plus(removeEventsWhileLocked(invalidatedEvents));
        result = result.plus(invalidatedEvents.stream().sequential()
                .flatMap(invalidatedEvent -> invalidatedEvent.getCreatedActors().stream())
                .map(Actor::removeWhileLocked)
                .reduce(AffectedActors::plus)
                .orElse(AffectedActors.emptyInstance()));
        return result;
    }

    @Nonnull
    @GuardedBy("lock")
    private AffectedActors<STATE> removeSignalWhileLocked(@Nonnull final Signal<STATE> signal) {
        assert Thread.holdsLock(lock);
        version++;
        if (unscheduledSignalsToReceive.remove(signal)) {
            invalidateNextSignalToReceive();
            return new AffectedActors<>(Set.of(this), Set.of(), Set.of());
        } else {
            final var invalidatedEvent = eventsForSignals.get(signal);
            if (invalidatedEvent == null) {
                return AffectedActors.emptyInstance();
            } else {
                return removeEventWhileLocked(invalidatedEvent);
            }
        }
    }

    @Nonnull
    @GuardedBy("lock")
    private AffectedActors<STATE> removeWhileLocked() {
        assert Thread.holdsLock(lock);
        final List<Event<STATE>> invalidatedEvents = new ArrayList<>(events);
        Collections.reverse(invalidatedEvents);
        var result = new AffectedActors<>(Set.of(), Set.of(), Set.of(this));
        result = result.plus(removeEventsWhileLocked(invalidatedEvents));
        return result;
    }

    @GuardedBy("lock")
    private void invalidateNextSignalToReceive() {
        nextSignalToReceive = null;
        whenReceiveNextSignal = null;
    }

    @Override
    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        final Actor<?> actor = (Actor<?>) that;

        return lock.equals(actor.lock);
    }

    @Override
    public int hashCode() {
        return lock.hashCode();
    }

    @Override
    public String toString() {
        return "Actor@" + lock;
    }

    /**
     * <p>
     * Indicates that a method of the {@link Signal} class threw a {@link RuntimeException},
     * which is likely to be due to a bug in an implementation of that class.
     * </p>
     */
    public static final class SignalException extends RuntimeException {
        <STATE> SignalException(@Nonnull final Signal<STATE> signal, @Nonnull final RuntimeException cause) {
            super("Signal " + signal + " threw exception " + cause, cause);
        }
    }

    /**
     * <p>
     * The sets of Actor objects {@linkplain #getChanged() changed}, {@linkplain #getAdded() added} and {@linkplain #getRemoved()}  removed}
     * by a {@linkplain Signal signal} or set of signals.
     * </p>
     *
     * @param <STATE> The class of states of the Actors. This must be
     *                {@link Immutable immutable}. It ought to have value semantics, but
     *                that is not required.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "immutable Sets")
    @Immutable
    public static final class AffectedActors<STATE> {

        private static final AffectedActors<?> EMPTY = new AffectedActors<>(Set.of(), Set.of(), Set.of());

        @Nonnull
        private final Set<Actor<STATE>> changed;

        @Nonnull
        private final Set<Actor<STATE>> added;

        @Nonnull
        private final Set<Actor<STATE>> removed;

        /**
         * <p>Construct an object with given aggregate content.</p>
         *
         * @throws NullPointerException     Ifd any given {@link Set} is null or contains null.
         * @throws IllegalArgumentException <ul>
         *                                  <li>If {@code added} {@linkplain Collection#contains(Object) contains} any elements in {@code changed}</li>
         *                                  <li>If {@code removed} {@linkplain Collection#contains(Object) contains} any elements in {@code changed}</li>
         *                                  <li>If {@code removed} {@linkplain Collection#contains(Object) contains} any elements in {@code added}</li>
         *                                  </ul>
         */
        public AffectedActors(@Nonnull final Set<Actor<STATE>> changed, @Nonnull final Set<Actor<STATE>> added, @Nonnull final Set<Actor<STATE>> removed) {
            this.changed = Set.copyOf(changed);
            this.added = Set.copyOf(added);
            this.removed = Set.copyOf(removed);
            requireNoIntersection("added and changed intersect", this.added, this.changed);
            requireNoIntersection("removed and changed intersect", this.removed, this.changed);
            requireNoIntersection("removed and added intersect", this.removed, this.added);
        }

        private static <E> void requireNoIntersection(
                @Nonnull final String message,
                @Nonnull final Set<E> setA,
                @Nonnull final Set<E> setB) {
            if (setA.stream().anyMatch(setB::contains)) {
                throw new IllegalArgumentException(message);
            }
        }

        @SuppressWarnings("unchecked")
        public static <STATE> AffectedActors<STATE> emptyInstance() {
            return (AffectedActors<STATE>) EMPTY;
        }

        @Nonnull
        private static <STATE> Set<Actor<STATE>> minus(
                @Nonnull final Set<Actor<STATE>> actorsA,
                @Nonnull final Set<Actor<STATE>> actorsB
        ) {
            if (actorsA.isEmpty() || actorsB.isEmpty()) {
                return actorsA;
            } else {
                final Set<Actor<STATE>> result = new HashSet<>(actorsA);
                result.removeAll(actorsB);
                return reduceDuplicateObjects(result, actorsA, actorsB);
            }
        }

        @SafeVarargs
        @Nonnull
        private static <T> T reduceDuplicateObjects(
                @Nonnull final T newObject,
                @Nonnull final T... oldObjects) {
            for (final var oldObject : oldObjects) {
                if (oldObject.equals(newObject)) {
                    return oldObject;
                }
            }
            return newObject;
        }

        /**
         * <p>
         * The Actors that had {@linkplain Actor#getEvents() events} added or removed,
         * or had {@linkplain Actor#getSignalsToReceive() signals to receive} added,
         * as a result of a {@linkplain Signal signal} or set of signals.
         * </p>
         * <p>
         * Exclusive with the sets of {@linkplain #getAdded() added actors} and {@linkplain #getRemoved() removed actors}.
         * </p>
         */
        @Nonnull
        public Set<Actor<STATE>> getChanged() {
            return changed;
        }

        /**
         * The Actors {@linkplain Event#getCreatedActors() created} by {@linkplain Signal#receiveForStateHistory(ValueHistory) reception of a signal}
         * or set of signals.
         * <p>
         * Exclusive with the set of {@linkplain #getRemoved() removed actors}.
         */
        @Nonnull
        public Set<Actor<STATE>> getAdded() {
            return added;
        }

        /**
         * The Actors removed by {@linkplain Actor#receiveSignal()  reception of a signal}
         * or set of signals.
         * <p>
         * Receiving a signal can result in removal of an actor
         * if it invalidates {@linkplain Event events} that
         * {@linkplain Event#getCreatedActors() added} actors.
         */
        @Nonnull
        public Set<Actor<STATE>> getRemoved() {
            return removed;
        }

        public boolean isEmpty() {
            return changed.isEmpty() && added.isEmpty() && removed.isEmpty();
        }

        @Override
        public String toString() {
            return "{" +
                    "changed=" + changed +
                    ", added=" + added +
                    ", removed=" + removed +
                    '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final AffectedActors<?> that = (AffectedActors<?>) o;

            return changed.equals(that.changed) &&
                    added.equals(that.added) &&
                    removed.equals(that.removed);
        }

        @Override
        public int hashCode() {
            int result = changed.hashCode();
            result = 31 * result + added.hashCode();
            result = 31 * result + removed.hashCode();
            return result;
        }

        @Nonnull
        public AffectedActors<STATE> plus(@Nonnull final AffectedActors<STATE> that) {
            if (that.isEmpty()) {
                return this;
            } else if (isEmpty()) {
                return that;
            } else {
                final var changedSum = Actor.plus(changed, that.changed);
                final var addedSum = Actor.plus(added, that.added);
                final var removedSum = Actor.plus(removed, that.removed);
                var changedResult = minus(changedSum, Actor.plus(addedSum, removedSum));
                var addedResult = minus(addedSum, removedSum);
                var removedResult = minus(removedSum, addedSum);
                changedResult = reduceDuplicateObjects(changedResult, Set.of(), changed, that.changed);
                addedResult = reduceDuplicateObjects(addedResult, Set.of(), added, that.added);
                removedResult = reduceDuplicateObjects(removedResult, Set.of(), removed, that.removed);
                return new AffectedActors<>(
                        changedResult,
                        addedResult,
                        removedResult
                );
            }
        }
    }
}
