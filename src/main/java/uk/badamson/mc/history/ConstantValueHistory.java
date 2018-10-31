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
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Stream;

import javax.annotation.concurrent.ThreadSafe;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * The nominally time-wise variation of a value that does not actually vary
 * through time.
 * </p>
 * 
 * @param VALUE
 *            The class of values of this value history. This must be
 *            {@link Immutable immutable}, or have reference semantics.
 */
@ThreadSafe
public final class ConstantValueHistory<VALUE> implements ValueHistory<VALUE> {

    public ConstantValueHistory() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public final VALUE get(Duration t) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final Duration getFirstTansitionTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final VALUE getFirstValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final Duration getLastTansitionTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final VALUE getLastValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final Duration getTansitionTimeAtOrAfter(Duration when) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final SortedMap<Duration, VALUE> getTransitions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final SortedSet<Duration> getTransitionTimes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public final Stream<Entry<Duration, VALUE>> streamOfTransitions() {
        // TODO Auto-generated method stub
        return null;
    }

}
