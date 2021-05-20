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
import java.util.Collection;
import java.util.Objects;
import java.util.SortedMap;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.simulation.TimestampedId;

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
     * Construct an object history with given start information and no signals.
     * </p>
     * <ul>
     * <li>The {@linkplain #getEnd() end} time is the same as the given
     * {@code start} time.</li>
     * <li>The {@linkplain #getLastSignalApplied() time that the last signal was
     * applied} is the same as the given {@code start} time.</li>
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
    public ModifiableObjectHistory(@Nonnull final UUID object, @Nonnull final Duration start,
            @Nonnull final STATE state) {
        super(object, start, state);
    }

    /**
     * <p>
     * Construct an object history with given history and signals information.
     * </p>
     *
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
    public ModifiableObjectHistory(@Nonnull @JsonProperty("object") final UUID object,
            @Nonnull @JsonProperty("end") final Duration end,
            @Nullable @JsonProperty("lastSignalApplied") final TimestampedId lastSignalApplied,
            @Nonnull @JsonProperty("stateTransitions") final SortedMap<Duration, STATE> stateTransitions,
            @Nonnull @JsonProperty("signals") final Collection<Signal<STATE>> signals) {
        super(object, end, lastSignalApplied, stateTransitions, signals);
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
            addSignalUnguarded(signal, true);
        }
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
     * {@linkplain Signal.Effect#getState() state resulting } from the effect, for
     * times at and after the {@linkplain Signal#getWhenReceived(ValueHistory)
     * reception time} of the the signal.</li>
     * <li>If this returns a null effect, the state history is unchanged.</li>
     * <li>If this returns a null effect, the {@linkplain #getLastSignalApplied()
     * last signal applied} is unchanged.</li>
     * <li>If this returns a (non null) effect, the
     * {@linkplain Signal.Effect#getAffectedObject() affected object} of the effect
     * is the same as the {@linkplain #getObject() object} of this history.</li>
     * <li>If this returns a (non null) effect, the
     * {@linkplain Signal.Effect#getWhenOccurred() time that the effect occurred} is
     * {@linkplain Duration#equals(Object) equal to} the reception time of the
     * signal.</li>
     * <li>If this returns a (non null) effect, the
     * {@linkplain ValueHistory#getLastValue() last state} in the the state history
     * is {@linkplain Objects#equals(Object, Object) equivalent to} the
     * {@linkplain Signal.Effect#getState() state resulting} from the effect.</li>
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
    public Signal.Effect<STATE> applyNextSignal() {
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
                final var entry = getNextSignalToApplyUnguarded();
                if (entry == null) {// no more signals
                    return null;
                }
                signalApplied = entry.getKey();
                signal = entry.getValue();
                oldState = stateHistory.get(signalApplied.getWhen());
                lastSignalApplied0 = this.lastSignalApplied;
            }

            /*
             * signal.receive is an expensive alien method, so should not hold a lock while
             * delegating to it.
             */
            final var effect = signal.receive(signalApplied.getWhen(), oldState);
            if (effect == null) {
                /* Provide good diagnostics for abstract class delegated to. */
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
            commitToGuarded(when);
        }
    }

    private boolean compareAndSetState(@Nullable final TimestampedId lastSignalApplied,
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
                    timestampedStates.tryEmitNext(
                            new TimestampedState<>(whenReceived, ValueHistory.END_OF_TIME, false, newState));
                }
            }
            return maySet;
        }
    }
}
