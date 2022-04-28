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
 * A simulated object.
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

    @Nonnull
    private final Duration start;

    @GuardedBy("lock")
    private final ModifiableValueHistory<STATE> stateHistory = new ModifiableValueHistory<>();

    @GuardedBy("lock")
    private final SortedSet<Event<STATE>> events = new TreeSet<>();

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
     * @throws NullPointerException If any argument is null
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

    @Nonnull
    private static <STATE> NavigableSet<Actor<STATE>> getReceiversOfSignalsEmittedInLockOrder(@Nonnull final Event<STATE> event) {
        final NavigableSet<Actor<STATE>> actors = createActorSetInLockOrder();
        actors.add(event.getAffectedObject());
        event.getSignalsEmitted().stream().map(Signal::getReceiver).forEach(actors::add);
        return actors;
    }

    private static <STATE, RESULT> RESULT doWithAllActorsLocked(
            @Nonnull final NavigableSet<Actor<STATE>> actors,
            @Nonnull final Supplier<RESULT> operation) {
        if (actors.isEmpty()) {
            return operation.get();
        } else {
            final Actor<STATE> firstActor = actors.first();
            final NavigableSet<Actor<STATE>> remainingActors = actors.tailSet(firstActor, false);
            synchronized (firstActor.lock) {
                return doWithAllActorsLocked(remainingActors, operation);
            }
        }
    }

    private static <STATE> boolean determineConsequencesOfRemovingSignal(
            @Nonnull final Signal<STATE> signalToRemove,
            @Nonnull final NavigableSet<Actor<STATE>> actors,
            @Nonnull final Map<Actor<STATE>, Event<STATE>> lastValidEventForActors,
            @Nonnull final Map<Actor<STATE>, Long> previousVersionForActors,
            @Nonnull final Set<Signal<STATE>> signalsToRemove) {
        final var actor = signalToRemove.getReceiver();
        final Set<Signal<STATE>> emittedSignalsToRemove;
        synchronized (actor.lock) {
            final var eventToRemove = actor.eventsForSignals.get(signalToRemove);
            final boolean isSignalToReceive = actor.unscheduledSignalsToReceive.contains(signalToRemove) ||
                    actor.signalsToReceive.contains(signalToRemove);
            if (eventToRemove == null && !isSignalToReceive) {
                return true;
            }
            final var lastValidEventForActor = lastValidEventForActors.get(actor);
            final var previousVersionForActor = previousVersionForActors.get(actor);
            final var version = actor.version;
            if (previousVersionForActor != null && previousVersionForActor != version) {
                return false;// lost data race
            }
            actors.add(actor);
            if (eventToRemove != null && (lastValidEventForActor == null || eventToRemove.compareTo(lastValidEventForActor) < 0)) {
                lastValidEventForActors.put(actor, eventToRemove);
                previousVersionForActors.put(actor, version);
                emittedSignalsToRemove = actor.events.tailSet(eventToRemove).stream()
                        .flatMap(event -> event.getSignalsEmitted().stream()).collect(Collectors.toSet());
            } else {
                emittedSignalsToRemove = Set.of();
            }
            if (previousVersionForActor == null) {
                previousVersionForActors.put(actor, version);
            }
        }
        signalsToRemove.add(signalToRemove);
        for (final var emittedSignal : emittedSignalsToRemove) {
            if (!signalsToRemove.contains(emittedSignal) && !determineConsequencesOfRemovingSignal(
                    emittedSignal, actors, lastValidEventForActors, previousVersionForActors, signalsToRemove)) {
                return false;
            }
            signalsToRemove.add(emittedSignal);
        }
        return true;
    }

    @Nonnull
    private static <STATE> NavigableSet<Actor<STATE>> createActorSetInLockOrder() {
        return new TreeSet<>(Comparator.comparing(a -> a.lock));
    }

    private static <STATE> boolean invalidateEventsAndRemoveSignals(
            @Nonnull final NavigableSet<Actor<STATE>> actorsInLockOrder,
            @Nonnull final Map<Actor<STATE>, Event<STATE>> firstInvalidEventForActors,
            @Nonnull final Map<Actor<STATE>, Long> previousVersionForActors,
            @Nonnull final Set<Signal<STATE>> signalsToRemove) {
        if (actorsInLockOrder.isEmpty()) {
            invalidateEventsAndRemoveSignalsWhileLocked(firstInvalidEventForActors, signalsToRemove);
            return true;
        } else {
            final var actor = actorsInLockOrder.first();
            final var remainingActorsInLockOrder = actorsInLockOrder.tailSet(actor, false);
            final var previousVersionForActor = previousVersionForActors.get(actor);
            assert previousVersionForActor != null;
            synchronized (actor.lock) {
                return previousVersionForActor == actor.version &&
                        invalidateEventsAndRemoveSignals(remainingActorsInLockOrder, firstInvalidEventForActors, previousVersionForActors, signalsToRemove);
            }
        }
    }

    private static <STATE> void invalidateEventsAndRemoveSignalsWhileLocked(
            @Nonnull final Map<Actor<STATE>, Event<STATE>> firstInvalidEventForActors,
            @Nonnull final Set<Signal<STATE>> signalsToRemove) {
        firstInvalidEventForActors.forEach((actor, firstInvalidEvent) -> {
            assert Thread.holdsLock(actor.lock);
            //noinspection FieldAccessNotGuarded
            actor.invalidateEventsWhileLocked(firstInvalidEvent, signalsToRemove);
        });
        for (final var signal : signalsToRemove) {
            final var actor = signal.getReceiver();
            assert Thread.holdsLock(actor.lock);
            //noinspection FieldAccessNotGuarded
            actor.signalsToReceive.remove(signal);
            //noinspection FieldAccessNotGuarded
            actor.unscheduledSignalsToReceive.remove(signal);
        }
    }

    private static <STATE> void determineConsequencesOfSignalsEmitted(
            @Nonnull final Event<STATE> event,
            @Nonnull final NavigableSet<Actor<STATE>> actors,
            @Nonnull final Map<Actor<STATE>, Long> previousVersionForActors) {
        for (final var signal : event.getSignalsEmitted()) {
            final var actor = signal.getReceiver();
            actors.add(actor);
            synchronized (actor.lock) {
                //noinspection FieldAccessNotGuarded
                previousVersionForActors.computeIfAbsent(actor, a -> a.version);
            }
        }
    }

    private static <STATE> boolean determineConsequencesOfRemovingSignals(
            @Nonnull final NavigableSet<Actor<STATE>> actors,
            @Nonnull final Map<Actor<STATE>, Event<STATE>> firstInvalidEventForActors,
            @Nonnull final Map<Actor<STATE>, Long> previousVersionForActors,
            @Nonnull final Set<Signal<STATE>> signalsToRemove,
            @Nonnull final Set<Signal<STATE>> additionalSignalsToRemove) {
        for (final var additionalSignalToRemove : additionalSignalsToRemove) {
            if (!signalsToRemove.contains(additionalSignalToRemove)) {
                signalsToRemove.add(additionalSignalToRemove);
                if (!determineConsequencesOfRemovingSignal(
                        additionalSignalToRemove, actors, firstInvalidEventForActors, previousVersionForActors, signalsToRemove)) {
                    return false;
                }
            }
        }
        return true;
    }

    static <STATE> CompletableFuture<Void> advanceSeveralActors(
            @Nonnull final Duration when,
            @Nonnull final Collection<Actor<STATE>> actors,
            @Nonnull final Executor executor
    ) {
        final Collection<CompletableFuture<Void>> futures = actors.stream()
                .map(actor -> actor.advanceTo(when, executor))
                .collect(Collectors.toUnmodifiableList());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private static <STATE> CompletableFuture<Void> advanceToWithCompletableFuture(
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

    private CompletableFuture<Void> advanceTo(@Nonnull final Duration when, @Nonnull final Executor executor) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        executor.execute(() -> {
            final Set<Actor<STATE>> affectedActors;
            try {
                if (getWhenReceiveNextSignal().compareTo(when) < 0) {
                    affectedActors = receiveSignal();
                } else {
                    affectedActors = Set.of();
                }
            } catch (final SignalException e) {
                future.completeExceptionally(e);
                return;
            }
            if (affectedActors.isEmpty()) {
                future.complete(null);
            } else {
                advanceToWithCompletableFuture(when, affectedActors, executor)
                        .handle((ignored, exception) -> {
                            if (exception == null) {
                                future.complete(null);
                            } else {
                                future.completeExceptionally(exception);
                            }
                            return null;
                        });
            }
        });
        return future;
    }

    @GuardedBy("lock")
    private void invalidateEventsWhileLocked
            (@Nonnull final Event<STATE> firstInvalidEvent,
             @Nonnull final Set<Signal<STATE>> signalsToRemove) {
        assert Thread.holdsLock(lock);
        invalidateEvents(List.copyOf(events.tailSet(firstInvalidEvent)), signalsToRemove);
        invalidateNextSignalToReceive();
        version++;
    }

    /**
     * <p>
     * Get a snapshot of the sequence of events that have
     * {@linkplain Event#getAffectedObject() affected} this actor.
     * </p>
     * <ul>
     * <li>The events sequence may be {@linkplain List#isEmpty() empty}.</li>
     * <li>All events {@linkplain Event#getAffectedObject() affect} this actor.</li>
     * <li>All events {@linkplain Event#getWhen() occurred}
     * {@linkplain Duration#compareTo(Duration) after} the {@linkplain #getStart()
     * start} time of this history.</li>
     * <li>The returned event sequence is a snapshot: a copy of data, it is not
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
        return start;
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
     * <li>The returned state history is a snapshot: a copy of data, it is not
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
     * of this actor, but which have not yet been {@linkplain  #receiveSignal() received}.
     * </p>
     * <ul>
     *     <li>Does not contain a null element.</li>
     *     <li>A snapshot: not updated when signals are subsequently {@linkplain  #addSignalToReceive(Signal) added} or {@linkplain #receiveSignal() received}.</li>
     *     <li>The set may be unmodifiable.</li>
     *     <li>The {@linkplain Signal#getReceiver() receiver} of every signal in the set is this actor.</li>
     *     <li>The {@linkplain Signal#getWhenSent()} sending time} of every signal in the set is {@linkplain Duration#compareTo(Duration) before} the {@linkplain #getStart() start time} of this actor.</li>
     * </ul>
     */
    @Nonnull
    public Set<Signal<STATE>> getSignalsToReceive() {
        synchronized (lock) {
            if (unscheduledSignalsToReceive.isEmpty()) {
                return Set.copyOf(signalsToReceive);
            } else {
                final Set<Signal<STATE>> result = new HashSet<>((signalsToReceive));
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
     *     <li>{@linkplain Signal#NEVER_RECEIVED} if {@linkplain Set#isEmpty() no} {@linkplain #getSignalsToReceive() signals to receive}.</li>
     * </ul>
     *
     * @throws SignalException If a {@link Signal} object throws a {@link RuntimeException}.
     *                         The method is safe if this exception is thrown: the state of this Actor will not have changed.
     */
    @Nonnull
    public Duration getWhenReceiveNextSignal() {
        synchronized (lock) {
            return computeNextSignalToReceive();
        }
    }

    /**
     * <p>
     * Add a signal to the {@linkplain #getSignalsToReceive() set of signals to receive}.
     * </p>
     *
     * @throws NullPointerException     If {@code signal} is null
     * @throws IllegalArgumentException <ul>
     *                                  <li>If the {@linkplain Signal#getReceiver() receiver} of the {@code signal} is not this actor.</li>
     *                                  <li>If the {@linkplain Signal#getWhenSent()} sending time of the {@code signal} is {@linkplain Duration#compareTo(Duration) before} the {@linkplain #getStart() start time} of this actor.</li>
     *                                  </ul>
     * @see #removeSignal(Signal)
     * @see #receiveSignal()
     */
    public void addSignalToReceive(@Nonnull final Signal<STATE> signal) {
        Objects.requireNonNull(signal, "signal");
        if (signal.getReceiver() != this) {
            throw new IllegalArgumentException("this actor is not the receiver of the signal");
        }
        if (signal.getWhenSent().compareTo(start) < 0) {
            throw new IllegalArgumentException("signal sent before the start time of this actor");
        }
        synchronized (lock) {
            addUnscheduledSignalToReceive(signal);
        }
    }

    /**
     * <p>
     * Remove the potential or actual effect of a signal on this actor.
     * </p>
     * <p>Post conditions:</p>
     * <ul>
     *     <li>The {@code signal} is not {@linkplain Set#contains(Object) one of} of the {@linkplain #getSignalsToReceive() signals to receive}.</li>
     *     <li>None of the {@linkplain #getEvents() events} have the {@code signal} as their {@linkplain Event#getCausingSignal() causing signal}.</li>
     * </ul>
     *
     * @return The set of actors affected by this change; does not contain a null element; may be unmodifiable.
     * @throws NullPointerException     if {@code signal} is null
     * @throws IllegalArgumentException if the {@linkplain Signal#getReceiver() receiver} of the {@code signal} is not {@code  this} Actor.
     */
    @Nonnull
    public Set<Actor<STATE>> removeSignal(@Nonnull final Signal<STATE> signal) {
        Objects.requireNonNull(signal, "signal");
        if (signal.getReceiver() != this) {
            throw new IllegalArgumentException("signal.receiver != this");
        }
        do {
            synchronized (lock) {
                if (unscheduledSignalsToReceive.remove(signal)) {
                    return Set.of(this);
                }
                if (signalsToReceive.remove(signal)) {
                    if (nextSignalToReceive == signal) {
                        invalidateNextSignalToReceive();
                    }
                    version++;// hard to test
                    return Set.of(this);
                }
            }
            final NavigableSet<Actor<STATE>> actors = createActorSetInLockOrder();
            final Map<Actor<STATE>, Event<STATE>> firstInvalidEventForActors = new HashMap<>();
            final Map<Actor<STATE>, Long> previousVersionForActors = new HashMap<>();
            final Set<Signal<STATE>> signalsToRemove = new HashSet<>();
            if (determineConsequencesOfRemovingSignal(signal, actors, firstInvalidEventForActors, previousVersionForActors, signalsToRemove)
                    && invalidateEventsAndRemoveSignals(actors, firstInvalidEventForActors, previousVersionForActors, signalsToRemove)) {
                return actors;
            }
        } while (true);
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
     * </p>
     * <ul>
     *     <li>If the method returns a (non null) event, the {@linkplain Event#getAffectedObject() affected object} of the event is {@code this}.</li>
     * </ul>
     *
     * @return The set of actors affected by this change; does not contain a null element; may be unmodifiable.
     * @throws SignalException If a {@link Signal} object throws a {@link RuntimeException}.
     *                         The method is safe if this exception is thrown: the state of this Actor will not have changed.
     */
    @Nonnull
    public Set<Actor<STATE>> receiveSignal() {
        do {
            final long previousVersion;
            synchronized (lock) {
                computeNextSignalToReceive();
                previousVersion = version;
            }
            final Event<STATE> event;
            synchronized (lock) {
                if (isOutOfDate(previousVersion)) {
                    event = null;
                } else if (nextSignalToReceive == null) {
                    return Set.of();
                } else {
                    event = createNextEvent();
                }
            }
            if (event != null) {
                final NavigableSet<Actor<STATE>> actors = createActorSetInLockOrder();
                final Map<Actor<STATE>, Event<STATE>> firstInvalidEventForActors = new HashMap<>();
                final Map<Actor<STATE>, Long> previousVersionForActors = new HashMap<>();
                final Set<Signal<STATE>> signalsToRemove = new HashSet<>();
                if (determineConsequencesOfAppendingEvent(event, actors, firstInvalidEventForActors, previousVersionForActors, signalsToRemove)
                        && invalidateEventsAndRemoveSignals(actors, firstInvalidEventForActors, previousVersionForActors, signalsToRemove)
                        && appendEvent(previousVersion, event)) {
                    return actors;
                }
            }
        } while (true);
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
        try {
            return nextSignalToReceive.receive(whenReceiveNextSignal, state);
        } catch (final RuntimeException e) {
            throw new SignalException(nextSignalToReceive, e);
        }
    }

    @GuardedBy("lock")
    private void considerAsNextSignalToReceive(final Signal<STATE> signal) throws SignalException {
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

    @GuardedBy("lock")
    private boolean isOutOfDate(final long expectedVersion) {
        /* This assumes we will never have more than Long.MAX_VALUE threads,
         * which is safe enough
         */
        return expectedVersion != version;
    }

    private boolean determineConsequencesOfAppendingEvent(
            @Nonnull final Event<STATE> eventToAppend,
            @Nonnull final NavigableSet<Actor<STATE>> actors,
            @Nonnull final Map<Actor<STATE>, Event<STATE>> firstInvalidEventForActors,
            @Nonnull final Map<Actor<STATE>, Long> previousVersionForActors,
            @Nonnull final Set<Signal<STATE>> signalsToRemove) {
        determineConsequencesOfSignalsEmitted(eventToAppend, actors, previousVersionForActors);
        final Set<Signal<STATE>> additionalSignalsToRemove = new HashSet<>();
        synchronized (lock) {
            if (!determineDirectConsequencesOfAppendingEvent(
                    eventToAppend,
                    actors,
                    firstInvalidEventForActors,
                    previousVersionForActors,
                    additionalSignalsToRemove)) {
                return false;
            }
        }
        return determineConsequencesOfRemovingSignals(
                actors,
                firstInvalidEventForActors,
                previousVersionForActors,
                signalsToRemove,
                additionalSignalsToRemove);
    }

    @GuardedBy("lock")
    private boolean determineDirectConsequencesOfAppendingEvent(
            @Nonnull final Event<STATE> eventToAppend,
            @Nonnull final NavigableSet<Actor<STATE>> actors,
            @Nonnull final Map<Actor<STATE>, Event<STATE>> firstInvalidEventForActors,
            @Nonnull final Map<Actor<STATE>, Long> previousVersionForActors,
            @Nonnull final Set<Signal<STATE>> additionalSignalsToRemove) {
        final SortedSet<Event<STATE>> invalidatedEvents = events.tailSet(eventToAppend);
        final Event<STATE> firstEventToRemove = invalidatedEvents.isEmpty() ? null : invalidatedEvents.first();
        final var firstInvalidEventForActor = firstInvalidEventForActors.get(this);
        final var previousVersionForActor = previousVersionForActors.get(this);
        if (previousVersionForActor != null && previousVersionForActor != version) {
            return false;// lost data race
        }
        if (firstEventToRemove != null && (firstInvalidEventForActor == null || firstEventToRemove.compareTo(firstInvalidEventForActor) < 0)) {
            firstInvalidEventForActors.put(this, firstEventToRemove);
            previousVersionForActors.put(this, version);
            invalidatedEvents.forEach(event -> additionalSignalsToRemove.addAll(event.getSignalsEmitted()));
        } else if (previousVersionForActor == null) {
            previousVersionForActors.put(this, version);
        }
        actors.add(this);
        return true;
    }

    private boolean appendEvent(final long previousVersion, @Nonnull final Event<STATE> event) {
        assert event.getAffectedObject() == this;
        //noinspection FieldAccessNotGuarded
        return doWithAllActorsLocked(getReceiversOfSignalsEmittedInLockOrder(event), () -> appendEventWhileLocked(previousVersion, event));
    }

    @GuardedBy("lock")
    private boolean appendEventWhileLocked(final long previousVersion, @Nonnull final Event<STATE> event)
            throws SignalException {
        assert Thread.holdsLock(lock);
        if (isOutOfDate(previousVersion)) {
            return false;
        }
        final Signal<STATE> causingSignal = event.getCausingSignal();
        assert !eventsForSignals.containsKey(causingSignal);
        invalidateNextSignalToReceive();
        version++;
        events.add(event);
        assert events.last() == event;
        eventsForSignals.put(causingSignal, event);
        stateHistory.setValueFrom(event.getWhen(), event.getState());
        signalsToReceive.remove(causingSignal);
        for (final var emittedSignal : event.getSignalsEmitted()) {
            emittedSignal.getReceiver().addUnscheduledSignalToReceive(emittedSignal);
        }
        return true;
    }

    @GuardedBy("lock")
    private void invalidateEvents(
            @Nonnull final Collection<Event<STATE>> invalidatedEvents,
            @Nonnull final Set<Signal<STATE>> signalsToRemove) {
        for (final var invalidatedEvent : invalidatedEvents) {
            final Signal<STATE> causingSignal = invalidatedEvent.getCausingSignal();
            assert events.contains(invalidatedEvent);
            assert eventsForSignals.containsKey(causingSignal);
            events.remove(invalidatedEvent);
            eventsForSignals.remove(causingSignal);
            if (!signalsToRemove.contains(causingSignal)) {
                // reschedule reception of the signal
                signalsToReceive.add(causingSignal);
            }
        }
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
}
