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
import uk.badamson.mc.simulation.TimestampedId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toUnmodifiableSet;

/**
 * <p>
 * The sequence of state transitions of a simulated object.
 * </p>
 *
 * @param <STATE> The class of states of the simulated object. This must be
 *                {@link Immutable immutable}. It ought to have value semantics, but
 *                that is not required.
 */
@ThreadSafe
public final class ObjectHistory<STATE> {

    @Nonnull
    private final UUID object;
    @Nonnull
    private final Duration start;
    /*
     * Use a UUID object as the lock so all ObjectHistory objects can have a
     * predictable lock ordering when locking two instances.
     */
    private final UUID lock = UUID.randomUUID();
    @Nonnull
    @GuardedBy("lock")
    private final ModifiableValueHistory<STATE> stateHistory;
    /*
     * Maps event ID to event.
     */
    @Nonnull
    @GuardedBy("lock")
    private final NavigableMap<TimestampedId, Event<STATE>> events;
    /*
     * Maps signal ID to signal.
     */
    @Nonnull
    @GuardedBy("lock")
    private final Map<UUID, Signal<STATE>> incomingSignals;
    /*
     * Maps signal ID to signal.
     */
    @Nonnull
    @GuardedBy("lock")
    private final Map<UUID, Signal<STATE>> receivedSignals;

    /**
     * <p>
     * Copy an object history.
     * </p>
     */
    public ObjectHistory(@Nonnull final ObjectHistory<STATE> that) {
        Objects.requireNonNull(that, "that");
        object = that.object;
        start = that.start;
        synchronized (that.object) {// hard to test
            stateHistory = new ModifiableValueHistory<>(that.stateHistory);
            incomingSignals = new HashMap<>(that.incomingSignals);
            receivedSignals = new HashMap<>(that.receivedSignals);
            events = new TreeMap<>(that.events);
        }
    }

    /**
     * <p>
     * Construct an object history with given start information and no signals.
     * </p>
     * <ul>
     * <li>The {@linkplain #getEvents() events} sequence
     * {@linkplain SortedSet#isEmpty() is empty}.</li>
     * </ul>
     *
     * @param object The unique ID of the object for which this is the history.
     * @param start  The point in time that this history starts.
     * @param state  The first (known) state transition of the {@code
     *               object}.
     * @throws NullPointerException If a {@link Nonnull} argument is null
     */
    public ObjectHistory(@Nonnull final UUID object, @Nonnull final Duration start, @Nonnull final STATE state) {
        Objects.requireNonNull(state, "state");
        this.object = Objects.requireNonNull(object, "object");
        this.start = Objects.requireNonNull(start, "start");
        this.stateHistory = new ModifiableValueHistory<>();
        this.stateHistory.appendTransition(start, state);
        this.incomingSignals = new HashMap<>();
        this.receivedSignals = new HashMap<>();
        this.events = new TreeMap<>();
    }

    private static <STATE> Stream<Signal<STATE>> emittedSignalsStream(@Nonnull final Set<Event<STATE>> events) {
        return events.stream().flatMap(e -> e.getSignalsEmitted().stream());
    }

    @GuardedBy("lock")
    @Nonnull
    private SortedSet<Event<STATE>> addEvent(@Nonnull final Event<STATE> event, @Nonnull final Signal<STATE> signal) {
        final var after = events.tailMap(event.getId(), true);
        final var mustInvalidate = !after.isEmpty();
        // Reduce garbage for the !mustInvalidate case
        final SortedSet<Event<STATE>> invalidatedEvents = mustInvalidate ? new TreeSet<>(after.values())
                : Collections.emptySortedSet();
        if (mustInvalidate) {
            final Set<UUID> invalidatedSignalIds = invalidatedEvents.stream().map(Event::getCausingSignal)
                    .collect(toUnmodifiableSet());
            final Set<Signal<STATE>> invalidatedSignals = invalidatedSignalIds.stream()
                    .map(receivedSignals::get).filter(Objects::nonNull).collect(toUnmodifiableSet());

            after.clear();
            receivedSignals.keySet().removeAll(invalidatedSignalIds);
            invalidatedSignals.forEach(s -> incomingSignals.put(s.getId(), s));
        }

        final var state = event.getState();
        stateHistory.setValueFrom(event.getWhenOccurred(), state);
        events.put(event.getId(), event);
        assert events.lastKey() == event.getId();
        receivedSignals.put(signal.getId(), signal);
        incomingSignals.remove(signal.getId());

        return invalidatedEvents;
    }

    /**
     * <p>
     * Add signals to the {@linkplain #getIncomingSignals() set of incoming
     * signals}.
     * </p>
     *
     * @throws NullPointerException     <ul>
     *                                  <li>If {@code signals} is null.</li>
     *                                  <li>If {@code signals} contains null.</li>
     *                                  </ul>
     * @throws IllegalArgumentException If the {@linkplain Signal#getReceiver() receiver} of any
     *                                  {@code signals} is not {@linkplain UUID#equals(Object) equivalent
     *                                  to} the {@linkplain #getObject() object} of this history.
     */
    public void addIncomingSignals(@Nonnull final Collection<Signal<STATE>> signals) {
        Objects.requireNonNull(signals, "signals");
        synchronized (lock) {
            for (final var signal : signals) {
                Objects.requireNonNull(signal, "signal");
                if (!object.equals(signal.getReceiver())) {
                    throw new IllegalArgumentException("signal.receiver not equal to this.object");
                }
                incomingSignals.put(signal.getId(), signal);
            } // for
        } // synchronized
    }

    /**
     * <p>
     * Add an event to the {@linkplain #getEvents() collection of events} of this
     * history, if the previous event is as expected.
     * </p>
     * <p>
     * This compare-and-set operation enables optimistic concurrency, removing the
     * need to (internally) hold locks for long periods. The method atomically
     * compares the actual previous event with the given expected previous event,
     * and if they match, adds the given event. Adding an event invalidates all
     * subsequent events; the method also atomically removes those invalid events.
     * The method returns the collection of invalidated events, so the caller can
     * perform any processing due to invalidation of
     * {@linkplain Event#getSignalsEmitted() signals emitted} by the invalidated
     * events.
     * </p>
     * <ul>
     * <li>If the given {@code signal} is not {@linkplain Set#contains(Object) one
     * of} the {@linkplain #getIncomingSignals() incoming signals},the method has no
     * effect, and returns null to indicate failure.</li>
     * <li>If the actual previous event is not
     * {@linkplain Objects#equals(Object, Object) equivalent or equivalently null}
     * to the {@code expectedPreviousEvent} the method has no effect, and returns
     * null to indicate failure.</li>
     * <li>If the actual previous event is equivalent or equivalently null to the
     * {@code expectedPreviousEvent}, and the given {@code event} is already
     * {@linkplain SortedSet#contains(Object) present} in the collection of events,
     * the method has no effect, and returns an {@linkplain SortedSet#isEmpty()
     * empty} set.</li>
     * <li>If the actual previous event is equivalent or equivalently null to the
     * {@code expectedPreviousEvent}, and the given {@code event} is not already
     * present in the collection of events, the addition is possible. It
     * <ul>
     * <li>removes the invalidated events</li>
     * <li>adds the {@code event} to the collection of events</li>
     * <li>adds the {@code signal} to the collection of
     * {@linkplain #getReceivedSignals() received signals}</li>
     * <li>removes the {@code signal} from the collection of incoming signals</li>
     * <li>removes the (received) signals that caused any invalidated events from
     * the set of received signals</li>
     * <li>adds the (received) signals that caused any invalidated events to the set
     * of incoming signals</li>
     * <li>returns the set of invalidated events.</li>
     * </ul>
     * </li>
     * </ul>
     * <p>
     * If the method returns a (non null) set of invalidated events
     * </p>
     * <ul>
     * <li>All the invalidated events were previously in the collection of
     * events.</li>
     * <li>All the invalidated events are after the given {@code event}.</li>
     * </ul>
     *
     * @param expectedPreviousEvent The event that is expected to be in the collection of events,
     *                              immediately {@linkplain Event#compareTo(Event) before} the
     *                              {@code event} to be added. Or {@code null} if the {@code event} is
     *                              expected to be the first event.
     * @param event                 The event to add to the history.
     * @param signal                The signal that caused the event.
     * @return an indication of failure or information about events that were
     * removed.
     * @throws NullPointerException     If a {@link Nonnull} argument is null.
     * @throws IllegalArgumentException
     * <ul>
     *     <li>If the {@linkplain  Event#getAffectedObject() affected object} of the {@code event} is no the {@linkplain  #getObject() object} of this history.</li>
     *     <li>If the {@linkplain Event#getCausingSignal() causing signal} of the {@code vent} is not {@linkplain  UUID#equals(Object) equivalent to} the {@linkplain  Signal#getId() ID} of the {@code signal}</li>
     * </ul>
     */
    @Nullable
    SortedSet<Event<STATE>> compareAndAddEvent(@Nullable final Event<STATE> expectedPreviousEvent,
                                               @Nonnull final Event<STATE> event, @Nonnull final Signal<STATE> signal) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(signal, "signal");
        if (!event.getAffectedObject().equals(getObject())) {
            throw new IllegalArgumentException("event.object not equals this.object");
        }
        if (!event.getCausingSignal().equals(signal.getId())) {
            throw new IllegalArgumentException("event.causingSignal not equals signal.id");
        }
        if (expectedPreviousEvent != null) {
            if (!expectedPreviousEvent.getAffectedObject().equals(getObject())) {
                throw new IllegalArgumentException("expectedPreviousEvent.object not equals this.object");
            }
            if (event.compareTo(expectedPreviousEvent) <= 0) {
                throw new IllegalArgumentException("event is not after expectedPreviousEvent");
            }
        }

        final var eventId = event.getId();
        synchronized (lock) {
            if (!incomingSignals.containsKey(signal.getId())) {
                return null;// failure: unrecognized signal
            }
            final var previousEvents = events.headMap(eventId);
            final boolean expectationMet = expectedPreviousEvent == null ? previousEvents.isEmpty()
                    : !previousEvents.isEmpty()
                    && expectedPreviousEvent.equals(previousEvents.get(previousEvents.lastKey()));
            if (expectationMet) {
                if (events.containsKey(eventId)) {// no-op
                    return Collections.emptySortedSet();
                } else {
                    return addEvent(event, signal);
                }
            } else {
                return null;// failure: wrong predecessor
            }
        } // synchronized
    }

    /**
     * <p>
     * Determine the information needed to compute the next {@linkplain #getEvents()
     * event} to {@linkplain #compareAndAddEvent(Event, Event, Signal) add} to this
     * history.
     * </p>
     * <ul>
     * <li>The continuation is null if, and only if, there are no more
     * {@linkplain #getIncomingSignals() incoming signals}.</li>
     * <li>If there is a (non null) continuation, its
     * {@link Continuation#nextSignal} is the same as one of the
     * {@linkplain #getIncomingSignals() incoming signals}.</li>
     * <li>If there is a (non null) continuation, its
     * {@link Continuation#nextSignal} will be the {@linkplain #getIncomingSignals()
     * incoming signal} that has the earliest
     * {@linkplain Signal#getWhenReceived(ValueHistory) reception time}, for the
     * current {@linkplain #getStateHistory() state history}. In the case of ties,
     * the {@linkplain Signal#getId() signal ID} ordering is used as a time breaker.
     * That reception time is recorded in
     * {@link Continuation#whenNextSignalReceived}.</li>
     * <li>The {@link Continuation#previousEvent} will be the last event
     * {@linkplain Duration#compareTo(Duration) before} the
     * {@link Continuation#whenNextSignalReceived}.</li>
     * <li>The {@link Continuation#state} is the state, from the current state
     * history, at {@link Continuation#whenNextSignalReceived}.</li>
     * </ul>
     */
    @Nullable
    Continuation<STATE> computeContinuation() {
        synchronized (lock) {
            Signal<STATE> nextSignal = null;
            TimestampedId nextEventId = null;
            for (final var entry : incomingSignals.entrySet()) {
                final var id = entry.getKey();
                final var signal = entry.getValue();
                final var whenReceived = signal.getWhenReceived(stateHistory);// expensive
                final TimestampedId eventId = new TimestampedId(id, whenReceived);
                if (nextEventId == null || eventId.compareTo(nextEventId) < 0) {
                    nextSignal = signal;
                    nextEventId = eventId;
                }
            } // for
            return createContinuation(nextEventId, nextSignal);
        } // synchronized
    }

    @GuardedBy("lock")
    @Nullable
    private Continuation<STATE> createContinuation(@Nullable final TimestampedId nextEventId,
                                                   @Nullable final Signal<STATE> nextSignal) {
        if (nextEventId == null || nextSignal == null) {
            return null;
        } else {
            final Duration whenNextSignalReceived = nextEventId.getWhen();
            final var previousEvents = events.headMap(nextEventId);
            if (previousEvents.isEmpty()) {
                final STATE state = stateHistory.get(whenNextSignalReceived);
                assert state != null;
                return new Continuation<>(nextSignal, whenNextSignalReceived, state);
            } else {
                final Event<STATE> previousEvent = previousEvents.get(previousEvents.lastKey());
                return new Continuation<>(nextSignal, whenNextSignalReceived, previousEvent);
            }
        }
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to a given object.
     * </p>
     * <p>
     * The {ObjectHistory class has <i>value semantics</i>.
     * </p>
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ObjectHistory)) {
            return false;
        }
        final ObjectHistory<?> other = (ObjectHistory<?>) obj;
        if (!object.equals(other.object) || !start.equals(other.start)) {
            return false;
        }
        if (lockBefore(other)) {
            return equalsGuarded(other);
        } else {
            return other.equalsGuarded(this);
        }
    }

    private boolean equalsGuarded(@Nonnull final ObjectHistory<?> that) {
        // hard to test the thread safety
        synchronized (lock) {
            synchronized (that.lock) {
                return stateHistory.equals(that.stateHistory);
            }
        }
    }

    /**
     * <p>
     * Get a snapshot of the sequence of events that have
     * {@linkplain Event#getAffectedObject() affected} the {@linkplain #getObject()
     * simulated object}.
     * </p>
     * <ul>
     * <li>The events sequence may be {@linkplain SortedSet#isEmpty() empty}.</li>
     * <li>All events {@linkplain Event#getAffectedObject() affect} the
     * {@linkplain #getObject() simulated object} of this history.</li>
     * <li>All events {@linkplain Event#getWhenOccurred() occurred}
     * {@linkplain Duration#compareTo(Duration) after} the {@linkplain #getStart()
     * start} time of this history.</li>
     * <li>The returned event sequence is a snapshot: a copy of data, it is not
     * updated if this object history is subsequently changed.</li>
     * <li>Note that events may be <i>measured as simultaneous</i>: events can have
     * {@linkplain Duration#equals(Object) equivalent}
     * {@linkplain Event#getWhenOccurred() times of occurrence}. Note however, that
     * the collection of events is {@linkplain Event#compareTo(Event) ordered by}
     * the {@linkplain Event#getId() event IDs}, so the
     * {@linkplain Event#getCausingSignal() ID of the causing signal} is used as a
     * tie-breaker to produce a strict ordering. In that case, however, the state
     * transition(s) due to some <i>measured as simultaneous</i> events will not be
     * apparent in the {@linkplain #getStateHistory() state history}; only the
     * <i>measured as simultaneous</i> event with the largest ID of its causing
     * signal will have its state recorded in the state history.
     * </ul>
     *
     * @see #getStateHistory()
     */
    @Nonnull
    public SortedSet<Event<STATE>> getEvents() {
        synchronized (lock) {// hard to test
            return new TreeSet<>(events.values());
        }
    }

    /**
     * <p>
     * Get a snapshot of the signals that are known to have been
     * {@linkplain Signal#getReceiver() sent to} the {@linkplain #getObject()
     * simulated object}.
     * </p>
     * <p>
     * This set of signals includes signals that have not yet had their effect
     * incorporated in the {@linkplain #getStateHistory() state history} through
     * {@linkplain #getEvents() events}; it can include signals in addition to the
     * {@linkplain #getReceivedSignals() received signals}.
     * </p>
     * <ul>
     * <li>The set of incoming signals does not have a null element.</li>
     * <li>The returned set is a snapshot; it will not incorporate subsequent
     * changes.</li>
     * <li>The returned set may be unmodifiable.</li>
     * <li>All incoming signals have the {@linkplain #getObject() object}
     * {@linkplain UUID#equals(Object) as their} {@linkplain Signal#getReceiver()
     * receiver}.</li>
     * </ul>
     */
    @Nonnull
    public Set<Signal<STATE>> getIncomingSignals() {
        synchronized (lock) {
            return Set.copyOf(incomingSignals.values());
        }
    }

    /**
     * <p>
     * The last of the {@linkplain #getEvents() sequence of events} that have
     * {@linkplain Event#getAffectedObject() affected} the {@linkplain #getObject()
     * simulated object}.
     * </p>
     * <ul>
     * <li>The last event is null if, and only if, the sequence of events is
     * empty.</li>
     * <li>The last event is either null or is the {@linkplain SortedSet#last()
     * last} of the sequence of events.</li>
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
            final var lastEntry = events.lastEntry();
            return lastEntry == null ? null : lastEntry.getValue();
        }
    }

    /**
     * <p>
     * The unique ID of the object for which this is the history.
     * </p>
     * <ul>
     * <li>Constant: the history always returns the same object ID.</li>
     * </ul>
     */
    @Nonnull
    public UUID getObject() {
        return object;
    }

    /**
     * <p>
     * The signals that the {@linkplain #getObject() simulated object} has
     * {@linkplain #getReceivedSignals() received}, or are
     * {@linkplain #getIncomingSignals()}.
     * </p>
     * <ul>
     * <li>The returned set may be unmodifiable.</li>
     * <li>The returned set contains all the {@linkplain #getReceivedSignals()
     * received signals} and all the {@linkplain #getIncomingSignals() incoming
     * signals}, and no others.</li>
     * </ul>
     */
    @Nonnull
    Set<Signal<STATE>> getReceivedAndIncomingSignals() {
        synchronized (lock) {// hard to test
            final Set<Signal<STATE>> result = new HashSet<>(receivedSignals.size() + incomingSignals.size());
            result.addAll(incomingSignals.values());
            result.addAll(receivedSignals.values());
            return result;
        }
    }

    /**
     * <p>
     * The signals that the {@linkplain #getObject() simulated object} has received,
     * resulting in {@linkplain #getEvents() events}.
     * </p>
     * <ul>
     * <li>The set of received signals is
     * {@linkplain Collections#disjoint(java.util.Collection, java.util.Collection)
     * disjoint} from the {@linkplain #getIncomingSignals() set of incoming
     * signals}.</li>
     * <li>The returned set may be unmodifiable.</li>
     * </ul>
     */
    @Nonnull
    public Set<Signal<STATE>> getReceivedSignals() {
        synchronized (lock) {// hard to test
            return Set.copyOf(receivedSignals.values());
        }
    }

    /**
     * <p>
     * The point in time that this history starts.
     * </p>
     * <p>
     * The earliest point in time for which the state of the simulated object is
     * known. Expressed as the duration since an (implied) epoch. All objects in a
     * simulation should use the same epoch.
     * </p>
     * <ul>
     * <li>Constant: the history always returns the same start time.</li>
     * </ul>
     */
    @Nonnull
    public Duration getStart() {
        return start;
    }

    /**
     * <p>
     * Get a snapshot of the history of states that the {@linkplain #getObject()
     * simulated object} has passed through.
     * </p>
     * <ul>
     * <li>The state history is never {@linkplain ValueHistory#isEmpty()
     * empty}.</li>
     * <li>The {@linkplain ValueHistory#getFirstTransitionTime() first transition
     * time} of the state history is the same as the {@linkplain #getStart() start}
     * time of this history.</li>
     * <li>The {@linkplain ValueHistory#getFirstValue() state at the start of time}
     * of the state history is null.</li>
     * <li>The {@linkplain Event#getState() state} resulting from an
     * {@linkplain #getEvents() event} is {@linkplain #equals(Object) equivalent to}
     * the {@linkplain ValueHistory#get(Duration) value} of the state history at the
     * {@linkplain Event#getWhenOccurred() time of occurrence} of the event.</li>
     * <li>The returned state history is a snapshot: a copy of data, it is not
     * updated if this object history is subsequently changed.</li>
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
     * The {@linkplain ValueHistory#getTransitions() transitions} in the
     * {@linkplain #getStateHistory() state history} of this object history.
     * </p>
     */
    @Nonnull
    public SortedMap<Duration, STATE> getStateTransitions() {
        synchronized (lock) {// hard to test
            /*
             * No need to make a defensive copy: ModifiableValueHistory.getTransitions()
             * does that for us.
             */
            return stateHistory.getTransitions();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        synchronized (lock) {// hard to test thread safety
            result = prime * result + object.hashCode();
            result = prime * result + stateHistory.hashCode();
        }
        return result;
    }

    private boolean lockBefore(@Nonnull final ObjectHistory<?> that) {
        assert !lock.equals(that.lock);
        return lock.compareTo(that.lock) < 0;
    }

    /**
     * <p>
     * Compute the effect of receiving the next of the
     * {@linkplain #getIncomingSignals() incoming signals}.
     * </p>
     * <p>
     * The <dfn>next incoming signal</dfn> is the same as one of the
     * {@linkplain #getIncomingSignals() incoming signals}. It is the incoming
     * signal that has the earliest {@linkplain Signal#getWhenReceived(ValueHistory)
     * reception time}, for the current {@linkplain #getStateHistory() state
     * history}. In the case of ties, the {@linkplain Signal#getId() signal ID}
     * ordering is used as a tie breaker.
     * </p>
     * <p>
     * Because signals could be {@linkplain #addIncomingSignals(Collection) added to
     * the set of incoming signals} in any order, there is no guarantee that
     * successive calls to this method will process signals in ascending
     * {@linkplain Signal#getWhenReceived(ValueHistory) time of reception}. The
     * <i>next incoming signal</i> could actually be received before some signals
     * that have already been processed as {@linkplain #getReceivedSignals()
     * received signals}: an <dfn>out of order signal</dfn>. But because the
     * {@linkplain Signal#receive(Object) effect of receiving a signal} depends on
     * the state of the receiver, and thus indirectly on which signals it has
     * previously received, an <i>out of order signal</i> means that some computed
     * {@linkplain #getEvents() events} must be invalidated (all the events
     * subsequent to reception of the <i>out of order signal</i>). The method
     * removes those invalid events. The signals that caused those invalidated
     * events will have to be reprocessed. The method therefore moves the signals
     * responsible for those invalidated events from the collection of received
     * signals to the collection of incoming signals. Furthermore,
     * {@linkplain Event#getSignalsEmitted() signals emitted} by those invalidated
     * events are then no longer known to have been emitted, and their effects must
     * also be undone. The method {@linkplain Medium#removeAll(java.util.Collection)
     * removes} those invalidated emitted signals from the {@code medium}. Thus,
     * this method can decrease the number of {@linkplain #getEvents() events} and
     * increase the number of incoming signals.
     * </p>
     * <p>
     * The effect of receiving a signal will be to add an {@linkplain #getEvents()
     * event}, which is the event of receiving the signal. That event may
     * {@linkplain Event#getSignalsEmitted() emit signals}. The method will
     * {@linkplain Medium#addAll(java.util.Collection) add} those emitted signal to
     * the given {@code medium}.
     * </p>
     *
     * @param medium The medium used transmitting signals to and from the
     *               {@linkplain #getObject() simulated object}.
     * @return whether received a signal; that is, whether the method did any work
     * at all.
     */
    boolean receiveNextSignal(@Nonnull final Medium<STATE> medium) {
        Objects.requireNonNull(medium, "medium");
        Event<STATE> event;
        Set<Event<STATE>> invalidatedEvents;
        do {
            final var continuation = computeContinuation();
            if (continuation == null) {
                return false;
            } else {
                assert continuation.state != null;
                event = continuation.nextSignal.receive(continuation.state);// expensive
                invalidatedEvents = compareAndAddEvent(continuation.previousEvent, event, continuation.nextSignal);
                // invalidatedEvents will be null if lose a data race
            }
        } while (invalidatedEvents == null);

        final Set<Signal<STATE>> invalidatedSignals = emittedSignalsStream(invalidatedEvents)
                .collect(toUnmodifiableSet());
        medium.removeAll(invalidatedSignals);
        medium.addAll(event.getSignalsEmitted());
        return true;
    }

    /**
     * <p>
     * Remove a collection of signals from the {@linkplain #getReceivedSignals() set
     * of received signals} and {@linkplain #getIncomingSignals() incoming signals}.
     * </p>
     * <p>
     * Removing received signals requires removing the effect of the events that
     * those signals caused. Doing that rolls back the
     * {@linkplain #getStateHistory() state history} and {@linkplain #getEvents()
     * events sequence} to just before the earliest of the removed received signals.
     * Those rolled back events might themselves have
     * {@linkplain Event#getSignalsEmitted() emitted signals}, which consequently
     * also become invalid.
     * </p>
     * <p>
     * Post conditions:
     * </p>
     * <ul>
     * <li>The {@linkplain #getReceivedSignals() set of received signals} does not
     * include any signals with the given {@code signalIds}.</li>
     * <li>The {@linkplain #getIncomingSignals() set of incoming signals} does not
     * include any signals with the given {@code signalIds}.</li>
     * <li>Any signals removed from the {@linkplain #getReceivedSignals() set of
     * received signals} because of removed events that are not in the given set of
     * {@code signals} are added to the {@linkplain #getIncomingSignals() set of
     * incoming signals}. That is, invalidated events produced by other signals (not
     * in the {@code signals} set) will have their signals rescheduled for
     * reception.</li>
     * <li>The returned collection of removed emitted signals does not include any
     * signals with an ID in the given collection of {@code signalIds}.</li>
     * </ul>
     *
     * @param signalIds The {@linkplain Signal#getId() IDs} of the signals to remove.
     * @return emitted signals that have become invalid.
     * @throws NullPointerException <ul>
     *                                                                                                                                                              <li>If {@code signalIds} is null.</li>
     *                                                                                                                                                              <li>If {@code signalIds} contains a null.</li>
     *                                                                                                                                                              </ul>
     */
    @Nonnull
    Set<Signal<STATE>> removeSignals(@Nonnull final Set<UUID> signalIds) {
        Objects.requireNonNull(signalIds, "signalIds");
        final SortedSet<Event<STATE>> removedEvents;
        synchronized (lock) {
            removedEvents = events.values().stream().filter(event -> signalIds.contains(event.getCausingSignal()))
                    .collect(toCollection(TreeSet::new));
            if (removedEvents.isEmpty()) {
                assert Collections.disjoint(receivedSignals.keySet(), signalIds);
                incomingSignals.keySet().removeAll(signalIds);
            } else {
                final var firstRemovedEventId = removedEvents.first().getId();
                removedEvents.addAll(events.tailMap(firstRemovedEventId).values());
                final Set<Signal<STATE>> unReceivedSignals = removedEvents.stream()
                        .map(Event::getCausingSignal).filter(causingSignal -> !signalIds.contains(causingSignal))
                        .map(receivedSignals::get).collect(toUnmodifiableSet());

                events.tailMap(firstRemovedEventId, true).clear();
                stateHistory.removeTransitionsFrom(firstRemovedEventId.getWhen());
                receivedSignals.keySet().removeAll(signalIds);
                incomingSignals.keySet().removeAll(signalIds);
                unReceivedSignals.forEach(signal -> {
                    final var id = signal.getId();
                    receivedSignals.remove(id);
                    incomingSignals.put(id, signal);
                });
            }
        } // synchronized

        return emittedSignalsStream(removedEvents).filter(s -> !signalIds.contains(s.getId()))
                .collect(toUnmodifiableSet());
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return "ObjectHistory[" + object + " from " + start + ", stateHistory=" + stateHistory + "]";
        }
    }

    @Immutable
    static final class Continuation<STATE> {
        @Nonnull
        final Signal<STATE> nextSignal;
        @Nonnull
        final Duration whenNextSignalReceived;
        @Nullable
        final Event<STATE> previousEvent;
        @Nullable
        final STATE state;

        Continuation(@Nonnull final Signal<STATE> nextSignal, @Nonnull final Duration whenNextSignalReceived,
                     @Nonnull final Event<STATE> previousEvent) {
            this.nextSignal = Objects.requireNonNull(nextSignal, "nextSignal");
            this.whenNextSignalReceived = Objects.requireNonNull(whenNextSignalReceived, "whenNextSignalReceived");
            this.previousEvent = Objects.requireNonNull(previousEvent, "previousEvent");
            this.state = previousEvent.getState();
        }

        Continuation(@Nonnull final Signal<STATE> nextSignal, @Nonnull final Duration whenNextSignalReceived,
                     @Nonnull final STATE state) {
            this.nextSignal = Objects.requireNonNull(nextSignal, "nextSignal");
            this.whenNextSignalReceived = Objects.requireNonNull(whenNextSignalReceived, "whenNextSignalReceived");
            this.previousEvent = null;
            this.state = Objects.requireNonNull(state, "state");
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Continuation)) {
                return false;
            }
            final Continuation<?> other = (Continuation<?>) obj;
            return whenNextSignalReceived.equals(other.whenNextSignalReceived) && nextSignal.equals(other.nextSignal)
                    && Objects.equals(previousEvent, other.previousEvent) && Objects.equals(state, other.state);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + whenNextSignalReceived.hashCode();
            result = prime * result + nextSignal.hashCode();
            result = prime * result + (previousEvent == null ? 0 : previousEvent.hashCode());
            result = prime * result + (state == null ? 0 : state.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "ObjectHistory.Continuation [previousEvent=" + previousEvent + ", state=" + state + ", " + nextSignal
                    + "@" + whenNextSignalReceived + "]";
        }

    }// class

}
