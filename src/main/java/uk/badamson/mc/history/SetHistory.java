package uk.badamson.mc.history;
/*
 * © Copyright Benedict Adamson 2018,21-22.
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
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Duration;
import java.util.Set;

/**
 * <p>
 * The time-wise variation of a {@linkplain Set set} of values that changes at
 * discrete points in time.
 * </p>
 *
 * @param <VALUE> The class of values of this set history. This must be {@link Immutable
 *                immutable}, or have reference semantics.
 */
@NotThreadSafe
public interface SetHistory<VALUE> extends ValueHistory<Set<VALUE>> {

    /**
     * <p>
     * The history of whether the {@linkplain #get(Duration) value} of this set
     * history {@linkplain Set#contains(Object) contains} a given value.
     * </p>
     * <ul>
     * <li>The returned containment history has a (non null)
     * {@linkplain ValueHistory#get(Duration) value} for all points in time.</li>
     * <li>The returned containment history indicates that the value
     * {@linkplain Boolean#TRUE is} present {@linkplain ValueHistory#get(Duration)
     * for a point in time} if, and only if, that value is
     * {@linkplain Set#contains(Object) contained in} the set for which this is a
     * history {@linkplain #get(Duration) at that point in time}.</li>
     * <li>The {@linkplain ValueHistory#getTransitionTimes() transition times} of
     * the returned containment history is a
     * {@linkplain Set#containsAll(java.util.Collection) sub set} of the
     * {@linkplain #getTransitionTimes() transition times} of this history.</li>
     * </ul>
     *
     * @see Set#contains(Object)
     */
    @Nonnull
    ValueHistory<Boolean> contains(@Nullable VALUE value);

    /**
     * <p>
     * The set that {@linkplain Set#contains(Object) contains} all the values that
     * can be in this time varying set.
     * </p>
     * <ul>
     * <li>The {@linkplain #get(Duration) value of this time varying set} at any
     * point in time {@linkplain Set#containsAll(java.util.Collection) is a
     * non-strict sub set} of the universe.</li>
     * <li>The {@linkplain #getFirstValue() value of this time varying set at the
     * start of time} {@linkplain Set#containsAll(java.util.Collection) is a
     * non-strict sub set} of the universe.</li>
     * <li>The {@linkplain #getLastValue() value of this time varying set at the end
     * of time} {@linkplain Set#containsAll(java.util.Collection) is a non-strict
     * sub set} of the universe.</li>
     * </ul>
     */
    @Nonnull
    Set<VALUE> getUniverse();


    @Nonnull
    @Override
    Set<VALUE> get(@Nonnull Duration when);


    @Nonnull
    @Override
    Set<VALUE> getFirstValue();


    @Nonnull
    @Override
    Set<VALUE> getLastValue();
}
