package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2018.
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
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import uk.badamson.mc.simulation.TimestampedId;

/**
 * <p>
 * The effect that a {@linkplain Signal signal} has upon its
 * {@linkplain Signal#getReceiver() receiver} when it is received.
 * </p>
 *
 * @param <STATE>
 *            The class of states of a receiver. This must be {@link Immutable
 *            immutable}. It ought to have value semantics, but that is not
 *            required.
 */
@Immutable
public final class Event<STATE> {

    @Nonnull
    private final TimestampedId eventId;
    @Nullable
    private final STATE state;
    @Nonnull
    private final Set<Signal<STATE>> signalsEmitted;

    /**
     * <p>
     * Construct an effect with given attribute values.
     * </p>
     *
     * @throws NullPointerException
     *             <ul>
     *             <li>If any {@link Nonnull} argument is null.</li>
     *             <li>If {@code signalsEmitted} contains a null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If {@code signalsEmitted} contains a signal that was not sent by
     *             the event represented by this effect. That is, if the signal was
     *             not {@linkplain Signal#getSentFrom() sent from} the same object
     *             as the {@code eventId}.
     */
    public Event(@Nonnull final TimestampedId eventId, @Nullable final STATE state,
            @Nonnull final Set<Signal<STATE>> signalsEmitted) {
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.state = state;
        this.signalsEmitted = Set.copyOf(signalsEmitted);
        /* Check after copy to avoid race hazards. */
        this.signalsEmitted.forEach(signal -> {
            if (eventId != signal.getSentFrom()) {
                throw new IllegalArgumentException("signalEmitted not sent from event.");
            }
        });
    }

    /**
     * <p>
     * The simulated object changed by this effect.
     * </p>
     */
    @Nonnull
    public UUID getAffectedObject() {
        return eventId.getObject();
    }

    /**
     * <p>
     * The ID of the simulated object that this effect changed, and the point in
     * time that the change occurred, combined to form a unique ID for the event
     * that caused this effect.
     * </p>
     * <ul>
     * <li>The {@linkplain TimestampedId#getObject() object} of the event ID is the
     * same as the {@linkplain #getAffectedObject() affected object} of this
     * effect.</li>
     * <li>The {@linkplain TimestampedId#getWhen() time-stamp} of the event ID is
     * the same as the {@linkplain #getWhenOccurred() time of occurrance} of this
     * effect.</li>
     * </ul>
     */
    @Nonnull
    public TimestampedId getEventId() {
        return eventId;
    }

    /**
     * <p>
     * Signals emitted from the affected simulated object as part of this effect.
     * </p>
     * <ul>
     * <li>The returned set of signals emitted is a constant (the method always
     * returns a reference to the same object).</li>
     * <li>The returned set of signals emitted is unmodifiable.</li>
     * <li>The set of signals emitted does not contain a null signal.</li>
     * <li>The signals emitted are all {@linkplain Signal#getSentFrom() sent from}
     * the same {@linkplain #getEventId() event} that caused this effect.</li>
     * <li>The returned set of signals emitted may be {@linkplain Set#isEmpty()
     * empty}.</li>
     * </ul>
     */
    @Nonnull
    public Set<Signal<STATE>> getSignalsEmitted() {
        return signalsEmitted;
    }

    /**
     * <p>
     * The state that the simulated object has as a result of this effect.
     * </p>
     * <p>
     * A null state indicates that the simulated object is destroyed or removed.
     * </p>
     */
    @Nullable
    public STATE getState() {
        return state;
    }

    /**
     * <p>
     * The point in time that this effect occurred.
     * </p>
     */
    @Nonnull
    public Duration getWhenOccurred() {
        return eventId.getWhen();
    }

    @Override
    public String toString() {
        return "Signal.Effect [" + eventId + ", →" + state + ", ⇝" + signalsEmitted + "]";
    }

}// class