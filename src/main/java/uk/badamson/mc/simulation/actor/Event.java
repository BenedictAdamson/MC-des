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
 * A discrete event in the simulation.
 * </p>
 * <p>
 * All events are the effect that a {@linkplain Signal signal} has upon its
 * {@linkplain Signal#getReceiver() receiver} when it is received. Events are
 * also the only means by wgucg simulated objects can emit signals.
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
    private final TimestampedId id;
    @Nullable
    private final STATE state;
    @Nonnull
    private final Set<Signal<STATE>> signalsEmitted;

    /**
     * <p>
     * Construct an event with given attribute values.
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
    public Event(@Nonnull final TimestampedId id, @Nullable final STATE state,
            @Nonnull final Set<Signal<STATE>> signalsEmitted) {
        this.id = Objects.requireNonNull(id, "id");
        this.state = state;
        this.signalsEmitted = Set.copyOf(signalsEmitted);
        /* Check after copy to avoid race hazards. */
        this.signalsEmitted.forEach(signal -> {
            if (id != signal.getSentFrom()) {
                throw new IllegalArgumentException("signalEmitted not sent from event.");
            }
        });
    }

    /**
     * <p>
     * The simulated object changed by this event.
     * </p>
     */
    @Nonnull
    public UUID getAffectedObject() {
        return id.getObject();
    }

    /**
     * <p>
     * The unique ID for this event.
     * </p>
     * <p>
     * The ID combines the ID of the simulated object that this event changed, and
     * the point in time that the change occurred
     * </p>
     * <ul>
     * <li>The {@linkplain TimestampedId#getObject() object} of the ID is the same
     * as the {@linkplain #getAffectedObject() affected object} of this event.</li>
     * <li>The {@linkplain TimestampedId#getWhen() time-stamp} of the ID is the same
     * as the {@linkplain #getWhenOccurred() time of occurrence} of this event.</li>
     * </ul>
     */
    @Nonnull
    public TimestampedId getId() {
        return id;
    }

    /**
     * <p>
     * Signals emitted from the affected simulated object as part of this event.
     * </p>
     * <ul>
     * <li>The returned set of signals emitted is a constant (the method always
     * returns a reference to the same object).</li>
     * <li>The returned set of signals emitted is unmodifiable.</li>
     * <li>The set of signals emitted does not contain a null signal.</li>
     * <li>The signals emitted are all identified as
     * {@linkplain Signal#getSentFrom() sent from} the {@linkplain #getId() ID} of
     * this event.</li>
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
     * The state that the simulated object has as a result of this event.
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
     * The point in time that this event occurred.
     * </p>
     */
    @Nonnull
    public Duration getWhenOccurred() {
        return id.getWhen();
    }

    @Override
    public String toString() {
        return "Event [" + id + ", →" + state + ", ⇝" + signalsEmitted + "]";
    }

}