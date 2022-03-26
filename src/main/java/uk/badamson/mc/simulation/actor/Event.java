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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.simulation.TimestampedId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
public final class Event<STATE> implements Comparable<Event<STATE>> {

    @Nonnull
    private final TimestampedId id;
    @Nonnull
    private final UUID affectedObject;
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
     *                                  {@linkplain TimestampedId#getWhen() time-stamp} of the
     *                                  {@code id}.</li>
     *                                  </ul>
     */
    public Event(@Nonnull final TimestampedId id, @Nonnull final UUID affectedObject, @Nullable final STATE state,
                 @Nonnull final Set<Signal<STATE>> signalsEmitted) {
        this.id = Objects.requireNonNull(id, "id");
        this.affectedObject = Objects.requireNonNull(affectedObject, "affectedObject");
        this.state = state;
        this.signalsEmitted = Set.copyOf(signalsEmitted);
        /* Check after copy to avoid race hazards. */
        this.signalsEmitted.forEach(signal -> {
            if (affectedObject != signal.getSender()) {
                throw new IllegalArgumentException("signalEmitted not sent from sender.");
            }
            if (id.getWhen() != signal.getWhenSent()) {
                throw new IllegalArgumentException("signalEmitted not sent at event time.");
            }
        });
    }

    /**
     * <p>
     * The <i>natural ordering</i> of {@link Event} objects.
     * </p>
     * <p>
     * The <i>natural ordering</i> is equivalent to the
     * {@linkplain TimestampedId#compareTo(TimestampedId) natural ordering} of the
     * {@linkplain #getId() IDs}. Hence it is <i>consistent with equals</i> and in
     * {@linkplain Duration#compareTo(Duration) ascending order} of
     * {@linkplain #getWhenOccurred() when they occurred}.
     * </p>
     */
    @Override
    public int compareTo(@Nonnull final Event<STATE> that) {
        Objects.requireNonNull(that, "that");
        return id.compareTo(that.id);
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to a given object.
     * </p>
     * The {@link Event} class has <i>entity semantics</i> with the
     * {@linkplain #getId() event ID} serving as the unique ID.
     * </p>
     */
    @Override
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Event)) {
            return false;
        }
        final Event<?> other = (Event<?>) that;
        return id.equals(other.id);
    }

    /**
     * <p>
     * The simulated object changed by this event.
     * </p>
     */
    @Nonnull
    public UUID getAffectedObject() {
        return affectedObject;
    }

    /**
     * <p>
     * The {@linkplain Signal#getId() unique ID} of the signal that caused this
     * event.
     * </p>
     */
    @Nonnull
    public UUID getCausingSignal() {
        return id.getObject();
    }

    /**
     * <p>
     * The unique ID for this event.
     * </p>
     * <p>
     * The ID combines the ID of the signal that caused this event, and the point in
     * time that the change occurred
     * </p>
     * <ul>
     * <li>The {@linkplain TimestampedId#getObject() object} of the ID is the same
     * as the {@linkplain #getCausingSignal() causing signal} of this event.</li>
     * <li>The {@linkplain TimestampedId#getWhen() time-stamp} of the ID is the same
     * as the {@linkplain #getWhenOccurred() time of occurrence} of this event.</li>
     * </ul>
     * <p>
     * Note that, as signal IDs should be unique, inclusion of the time-stamp is
     * redundant, but enables the {@linkplain #compareTo(Event) natural ordering} to
     * be consistent with {@linkplain #equals(Object) equals} while providing entity
     * semantics.
     * </p>
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
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification="signalsEmitted is unmodifiable")
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
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Event [" + id + ", " + affectedObject + "→" + state + ", ⇝" + signalsEmitted + "]";
    }

}