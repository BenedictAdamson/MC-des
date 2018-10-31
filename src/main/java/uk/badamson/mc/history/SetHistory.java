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
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

/**
 * <p>
 * The time-wise variation of a {@linkplain Set set} of value that changes at
 * discrete points in time.
 * </p>
 * 
 * @param VALUE
 *            The class of values of this set history. This must be
 *            {@link Immutable immutable}, or have reference semantics.
 * 
 * @see ValueHistory
 */
@NotThreadSafe
public interface SetHistory<VALUE> extends ValueHistory<Set<VALUE>> {

    /**
     * <p>
     * The history of whether the {@linkplain #get(Duration) value} of this set
     * history {@linkplain Set#contains(Object) contains} a given value.
     * </p>
     * <ul>
     * <li>Always have a (non null) containment history for a value.</li>
     * <li>The containment history for a value has a (non null)
     * {@linkplain ValueHistory#get(Duration) value} for all points in time.</li>
     * <li>The containment history for a value indicates that the value
     * {@linkplain Boolean#TRUE is} present {@linkplain ValueHistory#get(Duration)
     * for a point in time} if, and only if, that value is
     * {@linkplain Set#contains(Object) contained in} the set for which this is a
     * history {@linkplain #get(Duration) at that point in time}.</li>
     * <li>The {@linkplain ValueHistory#getTransitionTimes() transition times} of
     * the containment history of a value is a
     * {@linkplain Set#containsAll(java.util.Collection) sub set} of the
     * {@linkplain #getTransitionTimes() transition times} of this history.</li>
     * </ul>
     * 
     * @param value
     *            The value of interest.
     * @return The containment history of the given value.
     * 
     * @see Set#contains(Object)
     */
    public @NonNull ValueHistory<Boolean> contains(@Nullable VALUE value);
}
