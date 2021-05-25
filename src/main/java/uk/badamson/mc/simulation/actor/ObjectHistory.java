package uk.badamson.mc.simulation.actor;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import org.reactivestreams.Publisher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;
import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.simulation.TimestampedId;

/**
 * <p>
 * The sequence of state transitions of a simulated object.
 * </p>
 *
 * @param <STATE>
 *            The class of states of the simulated object. This must be
 *            {@link Immutable immutable}. It ought to have value semantics, but
 *            that is not required.
 */
@ThreadSafe
public final class ObjectHistory<STATE> {

    /**
     * <p>
     * The state of a simulated object, with a time-range indicating the points in
     * time when the simulated object was in that state, and an indication of
     * whether the state is reliable.
     * </p>
     *
     * @param <STATE>
     *            The class of states of the simulated object. This must be
     *            {@link Immutable immutable}. It ought to have value semantics, but
     *            that is not required.
     */
    @Immutable
    public static final class TimestampedState<STATE> {

        @Nonnull
        private final Duration start;
        @Nonnull
        private final Duration end;
        private final boolean reliable;
        @Nullable
        private final STATE state;

        /**
         * Constructs a value with given attribute values.
         *
         * @throws IllegalArgumentException
         *             If {@code end} {@linkplain Duration#compareTo(Duration) is
         *             before} {@code start}
         */
        public TimestampedState(@Nonnull final Duration start, @Nonnull final Duration end, final boolean reliable,
                @Nullable final STATE state) {
            this.start = Objects.requireNonNull(start, "start");
            this.end = Objects.requireNonNull(end, "end");
            this.reliable = reliable;
            this.state = state;
            if (end.compareTo(start) < 0) {
                throw new IllegalArgumentException("end before start");
            }
        }

        /**
         * <p>
         * Whether this object is <dfn>equivalent</dfn> to a given other object.
         * </p>
         * <p>
         * The TimestampedState class has <i>value semantics</i>.
         * </p>
         */
        @Override
        public boolean equals(final Object that) {
            if (this == that) {
                return true;
            }
            if (!(that instanceof TimestampedState)) {
                return false;
            }
            final TimestampedState<?> other = (TimestampedState<?>) that;
            return reliable == other.reliable && start.equals(other.start) && end.equals(other.end)
                    && Objects.equals(state, other.state);
        }

        /**
         * <p>
         * The last point in time that the simulated object had the
         * {@linkplain #getState() state}
         * </p>
         * <p>
         * This is an <i>inclusive</i> and time. Expressed as the duration since an
         * (implied) epoch.
         * </p>
         * <ul>
         * <li>The end time is {@linkplain Duration#compareTo(Duration) at or after} the
         * {@linkplain #getStart() start} time.</li>
         * </ul>
         */
        @Nonnull
        public Duration getEnd() {
            return end;
        }

        /**
         * <p>
         * The point in time that the simulated object entered the
         * {@linkplain #getState() state}
         * </p>
         * <p>
         * Expressed as the duration since an (implied) epoch. All objects in a
         * simulation should use the same epoch.
         * </p>
         */
        @Nonnull
        public Duration getStart() {
            return start;
        }

        /**
         * <p>
         * The state of the simulated object, in the time-range given by the
         * {@linkplain #getStart() start} and {@linkplain #getEnd() end} times
         * </p>
         * <ul>
         * <li>Null if the object does not exist at that time.</li>
         * </ul>
         */
        @Nullable
        public STATE getState() {
            return state;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + start.hashCode();
            result = prime * result + end.hashCode();
            result = prime * result + (reliable ? 1 : 0);
            result = prime * result + (state == null ? 0 : state.hashCode());
            return result;
        }

        /**
         * <p>
         * Whether this time-stamped state is <i>reliable</i>
         * </p>
         * <p>
         * The opposite of <i>reliable</i> is <i>provisional</i>.
         * </p>
         */
        public boolean isReliable() {
            return reliable;
        }

        @Nonnull
        @Override
        public String toString() {
            return "@(" + start + "," + end + ") " + (reliable ? "reliable" : "provisional") + "=" + state;
        }

    }// class

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
    private Duration end;

    @Nullable
    @GuardedBy("lock")
    private TimestampedId lastSignalApplied;

    /*
     * Keyed by the signal reception time and signal ID
     */
    @GuardedBy("lock")
    private final NavigableMap<TimestampedId, Signal<STATE>> signals = new TreeMap<>();

    @Nonnull
    @GuardedBy("lock")
    private final ModifiableValueHistory<STATE> stateHistory;

    private final Sinks.Many<TimestampedState<STATE>> timestampedStates = Sinks.many().replay().latest();

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
            this.end = that.end;
            lastSignalApplied = that.lastSignalApplied;
            completeTimestampedStatesIfNoMoreHistory();
            stateHistory = new ModifiableValueHistory<>(that.stateHistory);
        }
    }

    /**
     * <p>
     * Construct an object history with given start information and no signals.
     * </p>
     * <ul>
     * <li>The {@linkplain #getEnd() end} time is the same as the given
     * {@code start} time.</li>
     * <li>The {@linkplain #getLastSignalApplied() last signal applied} is
     * null.</li>
     * <li>There are no {@linkplain #getSignals() signals}.</li>
     * </ul>
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
    public ObjectHistory(@Nonnull final UUID object, @Nonnull final Duration start, @Nonnull final STATE state) {
        Objects.requireNonNull(state, "state");
        this.object = Objects.requireNonNull(object, "object");
        this.start = Objects.requireNonNull(start, "start");
        this.stateHistory = new ModifiableValueHistory<>();
        this.stateHistory.appendTransition(start, state);
        this.end = start;
        this.lastSignalApplied = null;
        completeTimestampedStatesIfNoMoreHistory();
    }

    /**
     * <p>
     * Construct an object history with given history and signals information.
     * </p>
     *
     * @throws NullPointerException
     *             <ul>
     *             <li>If any {@link Nonnull} argument is null.</li>
     *             <li>If {@code signals} contains a null.</li>
     *             </ul>
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
     *             <li>If {@code signals} contains {@linkplain Signal#equals(Object)
     *             duplicates}.</li>
     *             <li>If any signal in {@code signals} does not have the
     *             {@linkplain #getObject() object} of this history as their
     *             {@linkplain Signal#getReceiver() receiver}.</li>
     *             <li>If any signal in {@code signals} were
     *             {@linkplain Signal#getWhenSent() sent}
     *             {@linkplain Duration#compareTo(Duration) before} the
     *             {@linkplain SortedMap#firstKey() first time-stamp} of
     *             {@code stateTransitions} This ensures it is possible to compute
     *             the {@linkplain Signal#getWhenReceived(ValueHistory) reception
     *             time} of the signal.</li>
     *             </ul>
     */
    @JsonCreator
    public ObjectHistory(@Nonnull @JsonProperty("object") final UUID object,
            @Nonnull @JsonProperty("end") final Duration end,
            @Nullable @JsonProperty("lastSignalApplied") final TimestampedId lastSignalApplied,
            @Nonnull @JsonProperty("stateTransitions") final SortedMap<Duration, STATE> stateTransitions,
            @Nonnull @JsonProperty("signals") final Collection<Signal<STATE>> signals) {
        this.object = Objects.requireNonNull(object, "object");
        Objects.requireNonNull(end, "end");
        this.lastSignalApplied = lastSignalApplied;
        this.stateHistory = new ModifiableValueHistory<>(null, stateTransitions);
        // Check after copy to avoid race hazards
        this.start = this.stateHistory.getFirstTansitionTime();
        if (this.start == null) {
            throw new IllegalArgumentException("empty stateTransitions");
        }
        if (end.compareTo(this.start) < 0) {
            throw new IllegalArgumentException("end before first in stateTransitions");
        }
        final var nDestructionEvents = this.stateHistory.getTransitions().values().stream()
                .filter(state -> state == null).count();
        if (1 < nDestructionEvents) {
            throw new IllegalArgumentException("stateTransitions has multiple destruction events");
        }
        if (0 < nDestructionEvents && this.stateHistory.getLastValue() != null) {
            throw new IllegalArgumentException("stateTransitions destruction event is not the last event");
        }
        if (this.stateHistory.get(end) == null && !ValueHistory.END_OF_TIME.equals(end)) {
            throw new IllegalArgumentException("reliability information suggests destroyed object may be recreated");
        }
        this.end = end;
        completeTimestampedStatesIfNoMoreHistory();
        try {
            signals.forEach(signal -> addSignalGuarded(signal, false));
        } catch (final Signal.UnreceivableSignalException e) {
            throw new IllegalArgumentException("signals", e);
        }
    }

    /**
     * <p>
     * Add a signal to the {@linkplain #getSignals() collection of signals} known to
     * have been sent to the {@linkplain #getObject() object} of this history.
     * </p>
     * <ul>
     * <li>If the collection already {@linkplain Collection#contains(Object)
     * contains} a signal {@linkplain Signal#equals(Object) equal} to the given
     * {@code signal}, this has no effect.</li>
     * </ul>
     *
     * @param signal
     *            The signal to add
     * @throws NullPointerException
     *             If {@code signal} is null.
     * @throws IllegalArgumentException
     *             If the {@linkplain Signal#getReceiver() receiver} of the
     *             {@code signal} is not {@linkplain UUID#equals(Object) equal to}
     *             the {@linkplain #getObject() object} of this history.
     * @throws Signal.UnreceivableSignalException
     *             <ul>
     *             <li>If the signal was {@linkplain Signal#getWhenSent() sent}
     *             {@linkplain Duration#compareTo(Duration) before} the
     *             {@linkplain #getStart() start time} of this history. This ensures
     *             it is possible to compute the
     *             {@linkplain Signal#getWhenReceived(ValueHistory) reception time}
     *             of the signal.</li>
     *             <li>If the {@code signal}
     *             {@linkplain Signal#getWhenReceived(ValueHistory) reception time}
     *             (for the {@linkplain #getStateHistory() state history} of this
     *             history) is {@linkplain Duration#compareTo(Duration) at or
     *             before} the {@linkplain #getEnd() end} of the period of reliable
     *             {@linkplain #getStateHistory() state history}. That is, if
     *             handling the signal would imply altering the (committed) reliable
     *             part of the state history. This is a non-fatal error: if this
     *             exception is thrown, all invariants have been maintained.</li>
     *             </ul>
     */
    public void addSignal(@Nonnull final Signal<STATE> signal) throws Signal.UnreceivableSignalException {
        synchronized (lock) {
            addSignalGuarded(signal, true);
        }
    }

    @GuardedBy("lock")
    private final void addSignalGuarded(@Nonnull final Signal<STATE> signal, final boolean requireAfterEnd)
            throws Signal.UnreceivableSignalException {
        Objects.requireNonNull(signal, "signal");
        if (!getObject().equals(signal.getReceiver())) {
            throw new IllegalArgumentException("signal.receiver");
        }
        final var whenReceived = signal.getWhenReceived(stateHistory);
        if (requireAfterEnd && whenReceived.compareTo(getEnd()) <= 0) {
            throw new Signal.UnreceivableSignalException("signal.whenReceived at or before this.end");
        }
        signals.put(new TimestampedId(signal.getId(), whenReceived), signal);
    }

    /**
     * <p>
     * {@linkplain Signal#receive(Object) Compute the effect} of the
     * {@linkplain #getNextSignalToApply() next signal to be applied}, and apply
     * that effect to the {@linkplain #getStateHistory() state history}
     * </p>
     * <ul>
     * <li>If there is a next signal to apply, the method
     * {@linkplain ModifiableValueHistory#setValueFrom(Duration, Object) sets the
     * state} of the {@linkplain #getStateHistory() state history} to the
     * {@linkplain Event#getState() state resulting } from the effect, for times at
     * and after the {@linkplain Signal#getWhenReceived(ValueHistory) reception
     * time} of the the signal.</li>
     * <li>If this returns a null effect, the state history is unchanged.</li>
     * <li>If this returns a null effect, the {@linkplain #getLastSignalApplied()
     * last signal applied} is unchanged.</li>
     * <li>If this returns a (non null) effect, the
     * {@linkplain Event#getAffectedObject() affected object} of the effect is the
     * same as the {@linkplain #getObject() object} of this history.</li>
     * <li>If this returns a (non null) effect, the
     * {@linkplain Event#getWhenOccurred() time that the effect occurred} is
     * {@linkplain Duration#equals(Object) equal to} the reception time of the
     * signal.</li>
     * <li>If this returns a (non null) effect, the
     * {@linkplain ValueHistory#getLastValue() last state} in the the state history
     * is {@linkplain Objects#equals(Object, Object) equivalent to} the
     * {@linkplain Event#getState() state resulting} from the effect.</li>
     * <li>If this returns a (non null) effect, the state history will either have
     * no {@linkplain ValueHistory#getLastTansitionTime() last transition time}, or
     * the last transition time will be {@linkplain Duration#compareTo(Duration) at
     * or before} the reception time of the signal.</li>
     * <li>If this returns a (non null) effect, {@linkplain #getLastSignalApplied()
     * last signal applied} is updated to indicate the signal and time of the
     * effect.</li>
     * <li>If this returns a (non null) effect, and the effect causes a state
     * change, this history emits a new value to the
     * {@linkplain #observeTimestampedStates() time-stamped states observable},
     * indicating the new state
     * {@linkplain ObjectHistory.TimestampedState#getStart() from} the state
     * transition {@linkplain ObjectHistory.TimestampedState#getEnd() until}
     * {@linkplain ValueHistory#END_OF_TIME the end of time} as
     * {@linkplain ObjectHistory.TimestampedState#isReliable() unreliable}
     * information.</li>
     * <li>If this returns a (non null) effect, and the effect causes a state
     * change, this history emits a new value to every
     * {@linkplain #observeState(Duration) state observable} for a time-stamp at or
     * after the reception time of the signal, with the emitted value being the same
     * as the state resulting from the effect.</li>
     * <li>If this returns a (non null) effect, typically the state history will
     * have a new {@linkplain ValueHistory#getLastTansitionTime() final transition}
     * at the time of the reception of the signal. However, that will not be the
     * case if the signal effect does not cause a state change.</li>
     * </ul>
     *
     * @return the effect of the next signal to apply; or null if there is no such
     *         signal.
     */
    @Nullable
    @SuppressFBWarnings(justification = "Provide good diagnostics for abstract class delegated to", value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public Event<STATE> applyNextSignal() {
        while (true) {
            final TimestampedId signalApplied;
            final Signal<STATE> signal;
            final STATE oldState;
            final TimestampedId lastSignalApplied0;

            /*
             * Determine which signal to apply next, and the state information it applies
             * to.
             */
            synchronized (lock) {
                final var entry = getNextSignalToApplyGuarded();
                if (entry == null) {// no more signals
                    return null;
                }
                signalApplied = entry.getKey();
                signal = entry.getValue();
                oldState = stateHistory.get(signalApplied.getWhen());
                lastSignalApplied0 = lastSignalApplied;
            }

            /*
             * signal.receive is an expensive alien method, so should not hold a lock while
             * delegating to it.
             */
            final var effect = signal.receive(signalApplied.getWhen(), oldState);
            if (effect == null) {
                throw new NullPointerException("Signal.receive(Duration,STATE) from " + signal);
            }
            final var newState = effect.getState();

            if (compareAndSetState(lastSignalApplied0, signalApplied, oldState, newState)) {
                return effect;// success
            }
            /* else lost a data race: try again (hard to test) */
        } // while
    }

    /**
     * <p>
     * Advance the {@linkplain #getEnd() end time of reliable state information} to
     * at least a given time.
     * </p>
     * <ul>
     * <li>Changes the end time to the given time if, and only if, it is after the
     * current end time.</li>
     * </ul>
     */
    public void commitTo(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        synchronized (lock) {
            if (when.compareTo(this.end) <= 0) {
                return;// no-op
            }
            this.end = when;
            completeTimestampedStatesIfNoMoreHistory();
        }
    }

    private final boolean compareAndSetState(@Nullable final TimestampedId lastSignalApplied,
            @Nonnull final TimestampedId signalApplied, @Nonnull final STATE oldState, @Nullable final STATE newState) {
        Objects.requireNonNull(signalApplied, "signalApplied");
        Objects.requireNonNull(oldState, "oldState");
        final var whenReceived = signalApplied.getWhen();

        synchronized (lock) {
            final STATE currentState = stateHistory.get(whenReceived);
            final boolean maySet = Objects.equals(lastSignalApplied, this.lastSignalApplied)
                    && Objects.equals(oldState, currentState);
            if (maySet) {
                this.lastSignalApplied = signalApplied;
                if (!Objects.equals(currentState, newState)) {
                    stateHistory.setValueFrom(whenReceived, newState);
                    final var result = timestampedStates.tryEmitNext(
                            new TimestampedState<>(whenReceived, ValueHistory.END_OF_TIME, false, newState));
                    // The sink is reliable; it should always successfully complete.
                    assert result == EmitResult.OK;
                }
            }
            return maySet;
        }
    }

    private void completeTimestampedStatesIfNoMoreHistory() {
        if (this.end.equals(ValueHistory.END_OF_TIME)) {
            // No further states are possible.
            final var result = timestampedStates.tryEmitComplete();
            // The sink is reliable; it should always successfully complete.
            assert result == EmitResult.OK;
        }
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to a given object.
     * </p>
     * <p>
     * The {@link ObjectHistory} class has <i>value semantics</i>.
     * </p>
     */
    @Override
    public final boolean equals(final Object obj) {
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
                return end.equals(that.end) && Objects.equals(lastSignalApplied, that.lastSignalApplied)
                        && signals.equals(that.signals) && stateHistory.equals(that.stateHistory);
            }
        }
    }

    /**
     * <p>
     * The last point in time for which this history is reliable.
     * </p>
     * <ul>
     * <li>Expressed as the duration since an (implied) epoch. All objects in a
     * simulation should use the same epoch.</li>
     * <li>The end time is {@linkplain Duration#compareTo(Duration) at or after} the
     * {@linkplain #getStart() start} time.</li>
     * </ul>
     */
    @Nonnull
    @JsonProperty("end")
    public final Duration getEnd() {
        return end;
    }

    /**
     * <p>
     * The {@linkplain Signal#getId() ID} of the last signal applied to the
     * {@linkplain #getStateHistory() state history}. and the point in time that the
     * signal was {@linkplain Signal#getWhenReceived(ValueHistory) received}.
     * </p>
     * <ul>
     * <li>The time of the last applied signal is typically
     * {@linkplain Duration#equals(Object) equivalent} to one of the
     * {@linkplain #getStateTransitions() state transition}
     * {@linkplain SortedMap#keySet() times}, because the effect of a signal
     * typically includes a state transition. However, that need not be the case:
     * the effect might not have caused a state transition.</li>
     * <li>The time of the last applied signal is <em>either</em>
     * {@linkplain Duration#compareTo(Duration) at or before} the
     * {@linkplain #getEnd() end} of the reliable state period <em>or</em> is
     * {@linkplain Duration#equals(Object) equal to} to the
     * {@linkplain Signal#getWhenReceived(ValueHistory) reception time} of one of
     * the {@linkplain #getSignals() signals}. However, time of the last applied
     * signal need not be the the reception time of a signal, if it is at or before
     * the end of the reliable state period because the collection of signals need
     * not include all the signals that were received before the end of the reliable
     * state period.</li>
     * </ul>
     */
    @Nonnull
    @JsonProperty("lastSignalApplied")
    public final TimestampedId getLastSignalApplied() {
        synchronized (lock) {// hard to test
            return lastSignalApplied;
        }
    }

    /**
     * <p>
     * The next of the {@linkplain #getSignals() signals} that have not had their
     * {@linkplain Event effect} applied to the {@linkplain #getStateHistory() state
     * history}
     * </p>
     * <ul>
     * <li>Returns null if, and only iff, all the signals have been applied. That
     * includes the case of there being no signals to apply.</li>
     * <li>If there is a (non null) next signal to apply, it is one of the
     * {@linkplain #getSignals() signals}.</li>
     * <li>If and there is no {@linkplain #getLastSignalApplied() last signal
     * applied}, and the collection of signals is not
     * {@linkplain Collection#isEmpty() empty}, the next signal to apply will be
     * first of the signals.</li>
     * <li>If there is a (non null) next signal to apply, and there was a (non null)
     * {@linkplain #getLastSignalApplied() last signal applied}, the next signal to
     * apply will be received {@linkplain Duration#compareTo(Duration) at or after}
     * the last signal applied. That is, its
     * {@linkplain Signal#getWhenReceived(ValueHistory) reception time} (for the
     * current {@linkplain #getStateHistory() state history}) will be at or after
     * the {@linkplain TimestampedId#getWhen() time} that the last signal was
     * applied.</li>
     * </ul>
     */
    @Nullable
    @JsonIgnore
    public final Signal<STATE> getNextSignalToApply() {
        // TODO thread-safety
        final var nextEntry = getNextSignalToApplyGuarded();
        if (nextEntry == null) {
            return null;
        } else {
            return nextEntry.getValue();
        }
    }

    @GuardedBy("lock")
    private final Map.Entry<TimestampedId, Signal<STATE>> getNextSignalToApplyGuarded() {
        final NavigableMap<TimestampedId, Signal<STATE>> remainingSignals = lastSignalApplied == null ? signals
                : signals.tailMap(lastSignalApplied, false);
        return remainingSignals.firstEntry();
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
    @JsonProperty("object")
    public final UUID getObject() {
        return object;
    }

    /**
     * <p>
     * The signals that are known to have been {@linkplain Signal#getReceiver() sent
     * to} the {@linkplain #getObject() object} of this history.
     * </p>
     * <ul>
     * <li>The returned collection of signals may be unmodifiable.</li>
     * <li>The returned collection of signals is a snapshot; it is not updated due
     * to subsequent changes.</li>
     * <li>The collection of signals does not contain any null elements.</li>
     * <li>The collection of signals contains no {@linkplain Signal#equals(Object)
     * duplicates}.</li>
     * <li>All the signals have the {@linkplain #getObject() object} of this history
     * as their {@linkplain Signal#getReceiver() receiver}.</li>
     * <li>All the signals were {@linkplain Signal#getWhenSent() sent}
     * {@linkplain Duration#compareTo(Duration) at or after} the
     * {@linkplain #getStart() start time} of this history. This ensures it is
     * possible to compute the {@linkplain Signal#getWhenReceived(ValueHistory)
     * reception time} of the signal.</li>
     * <li>The collection of signals is sorted in
     * {@linkplain Duration#compareTo(Duration) ascending order} of their
     * {@linkplain Signal#getWhenReceived(ValueHistory) reception time} (given the
     * current {@linkplain #getStateHistory() state history}).</li>
     * <li>The collection of signals need not contain all signals
     * {@linkplain Signal#getWhenReceived(ValueHistory) received} at or before the
     * {@linkplain #getEnd() end} of the reliable history period.</li>
     * <li>The collection of signals contains all known signals received after the
     * end of the reliable history period.</li></li>
     * </ul>
     */
    @Nonnull
    @JsonProperty("signals")
    public final Collection<Signal<STATE>> getSignals() {
        synchronized (lock) {
            return List.copyOf(signals.values());
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
    @JsonIgnore
    public final Duration getStart() {
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
     * <li>The {@linkplain ValueHistory#getFirstTansitionTime() first transition
     * time} of the state history is the same as the {@linkplain #getStart() start}
     * time of this history.</li>
     * <li>The {@linkplain ValueHistory#getFirstValue() state at the start of time}
     * of the state history is null.</li>
     * <li>If the state at the {@linkplain #getEnd() end} time is null (destruction
     * of the {@linkplain #getObject() object}), the end time is the
     * {@linkplain ValueHistory#END_OF_TIME end of time}. That is, if reliable state
     * information indicates that the simulated object was destroyed, it is
     * guaranteed that the simulated object will never be recreated.</li>
     * <li>The returned state history is a snapshot: a copy of data, it is not
     * updated if this object history is subsequently changed.</li>
     * </ul>
     * <p>
     * Using the sequence provided by {@link #observeState(Duration)} to acquire the
     * state at a given point in time is typically better than using the snapshot of
     * the state history, because the snapshot is not updated, and because creating
     * the snapshot can be expensive.
     * </p>
     *
     * @see #observeState(Duration)
     */
    @Nonnull
    @JsonIgnore
    public final ValueHistory<STATE> getStateHistory() {
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
    @JsonProperty("stateTransitions")
    public final SortedMap<Duration, STATE> getStateTransitions() {
        synchronized (lock) {// hard to test
            /*
             * No need to make a defensive copy: ModifiableValueHistory.getTransitions()
             * does that for us.
             */
            return stateHistory.getTransitions();
        }
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        synchronized (lock) {// hard to test thread safety
            result = prime * result + object.hashCode();
            result = prime * result + end.hashCode();
            result = prime * result + (lastSignalApplied == null ? 0 : lastSignalApplied.hashCode());
            result = prime * result + stateHistory.hashCode();
            result = prime * result + signals.hashCode();
        }
        return result;
    }

    private boolean lockBefore(@Nonnull final ObjectHistory<?> that) {
        assert !lock.equals(that.lock);
        return lock.compareTo(that.lock) < 0;
    }

    /**
     * <p>
     * Provide the state of the {@linkplain #getObject() simulated object} at a
     * given point in time.
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
     * with successive values typically being closer to the correct value.</li>
     * <li>The sequence of states does not contain successive duplicates.</li>
     * <li>The sequence rapidly publishes the first state of the sequence.</li>
     * <li>The time between publication of the last state of the sequence and
     * completion of the sequence can be a large. That is, the process of providing
     * a value and then concluding that it is the correct value rather than a
     * provisional value can be time consuming.</li>
     * <li>If the given point in time is {@linkplain Duration#compareTo(Duration) at
     * or before} the current {@linkplain #getEnd() end} time of this history, the
     * method can return a {@linkplain Mono#just(Object) sequence that immediately
     * provides} the correct value.</li>
     * <li>All states published are the same as the value that could be
     * {@linkplain ValueHistory#get(Duration) obtained} from the
     * {@linkplain #getStateHistory() snapshot of the state history} at the time of
     * publication.</li>
     * </ul>
     *
     * @param when
     *            The point in time of interest, expressed as the duration since an
     *            (implied) epoch. All objects in a simulation should use the same
     *            epoch.
     * @throws NullPointerException
     *             If {@code when} is null.
     * @see #getStateHistory()
     */
    @Nonnull
    public final Publisher<Optional<STATE>> observeState(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        synchronized (lock) {// hard to test
            final var observeStateFromStateHistory = Mono.just(Optional.ofNullable(stateHistory.get(when)));
            if (when.compareTo(end) <= 0) {
                return observeStateFromStateHistory;
            } else {
                return Flux.concat(observeStateFromStateHistory, observeStateFromStateTransitions(when))
                        .distinctUntilChanged();
            }
        }
    }

    private Flux<Optional<STATE>> observeStateFromStateTransitions(@Nonnull final Duration when) {
        Objects.requireNonNull(when, "when");
        return timestampedStates.asFlux()
                .filter(timeStamped -> timeStamped.getStart().compareTo(when) <= 0
                        && when.compareTo(timeStamped.getEnd()) <= 0)
                .takeUntil(timeStamped -> timeStamped.isReliable())
                .map(timeStamped -> Optional.ofNullable(timeStamped.getState()));
    }

    /**
     * <p>
     * Provide a sequence of time-stamped states for the {@linkplain #getObject()
     * simulated object}.
     * </p>
     * <ul>
     * <li>Each time-stamped provides <i>complete</i> information for a state: the
     * {@linkplain TimestampedState#getStart() start} time is the time that the
     * simulated object, and the {@linkplain #getEnd() end} time is the last point
     * in time before the next next state transition (or the
     * {@linkplain ValueHistory#END_OF_TIME end of time}, if there is no following
     * state transition).</li>
     * <li>The sequence of time-stamped states can be infinite.</li>
     * <li>The sequence of time-stamped states can override
     * {@linkplain TimestampedState#isReliable() provisional} values: if there is a
     * provisional value in the sequence, a later value may have a time range that
     * overlaps the time range of the provisional value.</li>
     * <li>The sequence never overrides {@linkplain TimestampedState#isReliable()
     * reliable} values: if a value is reliable, no subsequent value will overlap
     * the time range of the reliable value.</li>
     * <li>The sequence of time-stamped states can be finite, if it is certain that
     * there are no more states. That is the case if, and only if, the
     * {@linkplain #getEnd() reliable states end time} is the
     * {@linkplain ValueHistory#END_OF_TIME end of time}.</li>
     * <li>The sequence of time-stamped states are not in any time order, but will
     * tend to be in roughly ascending time order.</li>
     * <li>The sequence of time-stamped states does not include old values; it is a
     * <i>hot</i> observable. The publisher emits values only when there is a change
     * to the {@linkplain #getStateHistory() state history}. In particular, it does
     * not emit an values for a newly constructed object history.</li>
     * </ul>
     */
    @Nonnull
    public final Flux<TimestampedState<STATE>> observeTimestampedStates() {
        return timestampedStates.asFlux();
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return "ObjectHistory[" + object + "from " + start + " to " + end + ", lastSignalApplied="
                    + lastSignalApplied + ", stateHistory=" + stateHistory + ", signals=" + signals + "]";
        }
    }

}
