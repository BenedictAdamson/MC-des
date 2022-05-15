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

import javax.annotation.concurrent.Immutable;
import java.util.UUID;

/**
 * <p>
 * A means for transmitting {@linkplain Signal signals} from their
 * {@linkplain Signal#getSender() senders} to their
 * {@linkplain Signal#getReceiver() receivers}.
 * </p>
 * <p>
 * Used as part of the {@linkplain Signal.Id unique ID} of a signal,
 * so signals witrh otherwise equivalent {@linkplain Signal#getSender() senders},
 * {@linkplain Signal#getReceiver() receivers} and
 * {@linkplain Signal#getWhenSent() sending times}
 * can be distinguished.
 * </p>
 *
 * @see Signal
 */
@Immutable
public class Medium {

    final UUID id = UUID.randomUUID();

    @Override
    public String toString() {
        return "Medium@" + id;
    }

    /**
     * Whether this object is <i>equivalent</i> to another.
     * <p>
     * This class has reference semantics.
     */
    @Override
    public final boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Medium medium = (Medium) o;

        return id.equals(medium.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }
}
