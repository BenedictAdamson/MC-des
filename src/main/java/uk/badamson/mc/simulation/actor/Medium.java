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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * A means for transmitting {@linkplain Signal signals} from their
 * {@linkplain Signal#getSender() senders} to their
 * {@linkplain Signal#getReceiver() receivers}
 * </p>
 *
 * @see Signal
 */
@ThreadSafe
interface Medium<STATE> {

    /**
     * <p>
     * Add several signals to be transmitted through this medium.
     * </p>
     * <p>
     * {@linkplain Set#addAll(Collection) Adds all} the given {@code signals} to the
     * {@linkplain #getSignals() signals} being transmitted through this medium.
     * </p>
     *
     * @param signals The signals to be transmitted. The method will not modify this
     *                collection.
     * @throws NullPointerException  <ul>
     *                               <li>If {@code signals} is null</li>
     *                               <li>If {@code signals} contains null</li>
     *                               </ul>
     * @throws IllegalStateException If any of the {@code signals} are inconsistent with this medium.
     *                               For example, if the signal {@linkplain Signal#getReceiver()
     *                               receiver} is unknown.
     * @see Set#addAll(Collection)
     */
    void addAll(@Nonnull Collection<Signal<STATE>> signals);

    /**
     * <p>
     * The signals that are being transmitted through this medium.
     * </p>
     * <ul>
     * <li>Does not contain a null signal.</li>
     * <li>May be unmodifiable.</li>
     * <li>A snapshot: the returned set does not reflect subsequent changes.</li>
     * </ul>
     */
    @Nonnull
    Set<Signal<STATE>> getSignals();

    /**
     * <p>
     * Remove several signals, which might previously have been
     * {@linkplain #addAll(Collection) added} for transmission through this medium.
     * </p>
     * <p>
     * {@linkplain Set#removeAll(Collection) Removes all} the given {@code signals}
     * from the {@linkplain #getSignals() set of signals} being transmitted through
     * this medium.
     * </p>
     *
     * @param signals The signals to be removed. The method will not modify this
     *                collection.
     * @throws NullPointerException  <ul>
     *                               <li>If {@code signals} is null</li>
     *                               <li>If {@code signals} contains null</li>
     *                               </ul>
     * @throws IllegalStateException If any of the {@code signals} are inconsistent with this medium.
     *                               For example, if the signal {@linkplain Signal#getReceiver()
     *                               receiver} is unknown.
     * @see Set#removeAll(Collection)
     */
    void removeAll(@Nonnull Collection<Signal<STATE>> signals);

}
