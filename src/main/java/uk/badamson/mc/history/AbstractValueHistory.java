package uk.badamson.mc.history;
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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

/**
 * <p>
 * The modifiable time-wise variation of a value that changes at discrete points
 * in time.
 * </p>
 * 
 * @param VALUE
 *            The class of values of this value history. This must be an
 *            {@link Immutable immutable} type.
 */
@NotThreadSafe
abstract class AbstractValueHistory<VALUE> implements ValueHistory<VALUE> {

    @Override
    public boolean equals(Object that) {
        if (that == null)
            return false;
        if (this == that)
            return true;
        if (that instanceof ValueHistory) {
            @SuppressWarnings("unchecked")
            final ValueHistory<VALUE> thatValueHistory = (ValueHistory<VALUE>) that;
            return Objects.equals(getFirstValue(), thatValueHistory.getFirstValue())
                    && getTransitions().equals(thatValueHistory.getTransitions());
        } else {
            return false;
        }
    }

    @Override
    public @Nullable Duration getFirstTansitionTime() {
        var transitions = getTransitions();
        return transitions.isEmpty() ? null : transitions.firstKey();
    }

    @Override
    public @Nullable VALUE getFirstValue() {
        return get(START_OF_TIME);
    }

    @Override
    public @Nullable Duration getLastTansitionTime() {
        var transitions = getTransitions();
        return transitions.isEmpty() ? null : transitions.lastKey();
    }

    @Override
    public @Nullable VALUE getLastValue() {
        return get(END_OF_TIME);
    }

    @Override
    public @NonNull SortedSet<Duration> getTransitionTimes() {
        return new TreeSet<>(getTransitions().keySet());
    }

    @Override
    public int hashCode() {
        final VALUE firstValue = getFirstValue();
        return (firstValue == null ? 0 : firstValue.hashCode()) + getTransitions().hashCode();
    }

    @Override
    public boolean isEmpty() {
        return getTransitions().isEmpty();
    }

    @Override
    public @NonNull Stream<Entry<Duration, VALUE>> streamOfTransitions() {
        return getTransitions().entrySet().stream();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [");
        builder.append(getFirstValue());
        builder.append(", ");
        builder.append(getTransitions());
        builder.append("]");
        return builder.toString();
    }

}
