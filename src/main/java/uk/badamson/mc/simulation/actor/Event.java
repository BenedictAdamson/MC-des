package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2021-22.
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * A discrete event in the simulation.
 * </p>
 * <p>
 * All events are the effect that a {@linkplain Signal signal} has upon its
 * {@linkplain Signal#getReceiver() receiver} when it is received. Events are
 * also the only means by which simulated objects can emit signals.
 * </p>
 *
 * @param <STATE> The class of states of a receiver. This must be {@link Immutable
 *                immutable}. It ought to have value semantics, but that is not
 *                required.
 */
@Immutable
public final class Event<STATE> implements Comparable<Event<STATE>>{

    @Nonnull
    private final Signal<STATE> causingSignal;
    @Nonnull
    private final Duration when;
    @Nonnull
    private final Actor<STATE> affectedObject;
    @Nullable
    private final STATE state;
    @Nonnull
    private final Set<Signal<STATE>> signalsEmitted;

    /**
     * <p>
     * Construct an event with given attribute values.
     * </p>
     *
     * @throws NullPointerException     <ul>
     *                                  <li>If any {@link Nonnull} argument is null.</li>
     *                                  <li>If {@code signalsEmitted} contains a null.</li>
     *                                  </ul>
     * @throws IllegalArgumentException If {@code signalsEmitted} contains a signal that was not sent by
     *                                  the event represented by this effect. That is, if the signal was
     *                                  not
     *                                  <ul>
     *                                  <li>{@linkplain Signal#getSender() sent} from the same object as
     *                                  the {@code affectedObject}, or</li>
     *                                  <li>{@linkplain Signal#getWhenSent() sent} at the same time as
     *                                  {@code when}.</li>
     *                                  </ul>
     */
    public Event(@Nonnull final Signal<STATE> causingSignal, @Nonnull final Duration when, @Nonnull final Actor<STATE> affectedObject, @Nullable final STATE state,
                 @Nonnull final Set<Signal<STATE>> signalsEmitted) {
        this.causingSignal = Objects.requireNonNull(causingSignal, "causingSignal");
        this.when = Objects.requireNonNull(when, "when");
        this.affectedObject = Objects.requireNonNull(affectedObject, "affectedObject");
        this.state = state;
        this.signalsEmitted = Set.copyOf(signalsEmitted);
        /* Check after copy to avoid race hazards. */
        this.signalsEmitted.forEach(signal -> {
            if (affectedObject != signal.getSender()) {
                throw new IllegalArgumentException("signalEmitted not sent from sender.");
            }
            if (when != signal.getWhenSent()) {
                throw new IllegalArgumentException("signalEmitted not sent at event time.");
            }
        });
    }

    /**
     * <p>
     * The simulated object changed by this event.
     * </p>
     */
    @Nonnull
    public Actor<STATE> getAffectedObject() {
        return affectedObject;
    }

    /**
     * <p>
     * The signal that caused this event.
     * </p>
     */
    @Nonnull
    public Signal<STATE> getCausingSignal() {
        return causingSignal;
    }

    /**
     * <p>
     * Signals emitted from the {@linkplain #getAffectedObject() affected object} as part of this event.
     * </p>
     * <ul>
     * <li>The returned set of signals emitted is a constant (the method always
     * returns a reference to the same object).</li>
     * <li>The returned set of signals emitted is unmodifiable.</li>
     * <li>The set of signals emitted does not contain a null signal.</li>
     * <li>The returned set of signals emitted may be {@linkplain Set#isEmpty()
     * empty}.</li>
     * </ul>
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification="signalsEmitted is unmodifiable")
    @Nonnull
    public Set<Signal<STATE>> getSignalsEmitted() {
        return signalsEmitted;
    }

    /**
     * <p>
     * The state that the {@linkplain #getAffectedObject() affected object} has as a result of this event.
     * </p>
     * <p>
     * A null state indicates that the affected object is destroyed or removed.
     * </p>
     */
    @Nullable
    public STATE getState() {
        return state;
    }

    /**
     * <p>
     * The point in time that this event occurred.
     * </p>
     */
    @Nonnull
    public Duration getWhen() {
        return when;
    }

    @Override
    public String toString() {
        return "Event [@" + when + ", " + affectedObject + "→" + state + ", ⇝" + signalsEmitted + "]";
    }

    @Override
    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        final Event<?> event = (Event<?>) that;

        return when.equals(event.when) && causingSignal.equals(event.causingSignal);
    }

    @Override
    public int hashCode() {
        int result = when.hashCode();
        result = 31 * result + causingSignal.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     *     The natural ordering of Event objects is by their
     *     {@linkplain  #getWhen() time of occurrence},
     *     then by other criteria.
     *     The natural ordering is consistent with {@link #equals(Object)}
     * </p>
     */
    @Override
    public int compareTo(@Nonnull final Event<STATE> that) {
        int c = when.compareTo(that.when);
        if (c == 0) {
            c = causingSignal.getWhenSent().compareTo(that.causingSignal.getWhenSent());
        }
        if (c == 0) {
            c = causingSignal.getSender().lock.compareTo(that.causingSignal.getSender().lock);
        }
        if (c == 0) {
            c = causingSignal.getMedium().id.compareTo(that.causingSignal.getMedium().id);
        }
        if (c == 0) {
            c = causingSignal.getReceiver().lock.compareTo(that.causingSignal.getReceiver().lock);
        }
        return c;
    }
}