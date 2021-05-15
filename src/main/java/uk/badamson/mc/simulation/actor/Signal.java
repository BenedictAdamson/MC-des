package uk.badamson.mc.simulation.actor;

import java.time.Duration;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import uk.badamson.mc.simulation.ObjectStateId;

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

/**
 * <p>
 * A signal (or message) sent from one simulated object to another.
 * </p>
 *
 * @param <STATE>
 *            The class of states of a receiver. This must be {@link Immutable
 *            immutable}. It ought to have value semantics, but that is not
 *            required.
 */
@Immutable
public abstract class Signal<STATE> {

    @Nonnull
    private final ObjectStateId sentFrom;
    @Nonnull
    private final UUID receiver;

    /**
     * <p>
     * Construct a signal with given attribute values.
     * </p>
     *
     * @param sentFrom
     *            The ID of the simulated object that sent this signal, and the
     *            point in time that it sent (emitted) the signal.
     * @param receiver
     *            The ID of the simulated object that this signal was sent to; the
     *            object that will receive it.
     */
    protected Signal(@Nonnull final ObjectStateId sentFrom, @Nonnull final UUID receiver) {
        this.sentFrom = sentFrom;
        this.receiver = receiver;
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to a given object.
     * </p>
     * <p>
     * The {@link Signal} class has <i>value semantics</i>.
     * </p>
     */
    @Override
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Signal)) {
            return false;
        }
        final Signal<?> other = (Signal<?>) that;
        /*
         * Checking sentFrom first is more efficient, because a sender may send many
         * signals to the same receiver, which would differ only in their sending times.
         */
        return sentFrom.equals(other.sentFrom) && receiver.equals(other.receiver);
    }

    /**
     * <p>
     * The ID of the simulated object that this signal was sent to; the object that
     * will receive it.
     * </p>
     *
     * @see #getSender()
     */
    @Nonnull
    public final UUID getReceiver() {
        return receiver;
    }

    /**
     * <p>
     * The ID of the simulated object that sent this signal.
     * </p>
     * <p>
     * An object may send signals to itself. That is; the sender and
     * {@linkplain #getReceiver() receiver} may be {@linkplain UUID#equals(Object)
     * equivalent}. That can be done to implement internal events, such as timers.
     * </p>
     *
     * @see #getReceiver()
     */
    @Nonnull
    public final UUID getSender() {
        return sentFrom.getObject();
    }

    /**
     * <p>
     * The ID of the simulated object that sent this signal, and the point in time
     * that it sent (emitted) the signal.
     * </p>
     * <ul>
     * <li>The {@linkplain ObjectStateId#getObject() object} of the sent-from
     * information is the same as the {@linkplain #getSender() sender} of this
     * signal.</li>
     * <li>The {@linkplain ObjectStateId#getWhen() time-stamp} of the sent-from
     * information is the same as the {@linkplain #getWhenSent() sending time} of
     * this signal.</li>
     * </ul>
     */
    @Nonnull
    public final ObjectStateId getSentFrom() {
        return sentFrom;
    }

    /**
     * <p>
     * The point in time when this signal was sent (empitted).
     * </p>
     */
    @Nonnull
    public final Duration getWhenSent() {
        return sentFrom.getWhen();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + sentFrom.hashCode();
        result = prime * result + receiver.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + sentFrom + "⇝" + receiver + "]";
    }

}
