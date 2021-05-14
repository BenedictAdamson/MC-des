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
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * <p>
 * The state of a simulated object, with a time-stamp indicating one of the
 * point in time when the simulated object was in that state.
 * </p>
 *
 * @param <STATE>
 *            The class of states of the simulated object. This must be
 *            {@link Immutable immutable}. It ought to have value semantics, but
 *            that is not required.
 */
@Immutable
public final class TimestampedState<STATE> {

    @Nonnull
    private final Duration when;
    @Nullable
    private final STATE state;

    /**
     * Constructs a value with given attribute values.
     */
    public TimestampedState(@Nonnull final Duration when, @Nullable final STATE state) {
        this.when = Objects.requireNonNull(when, "when");
        this.state = state;
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to a given other object.
     * </p>
     * <p>
     * The TimestampedState class has <i>value semantics</i>.
     * </p>
     */
    @Override
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof TimestampedState)) {
            return false;
        }
        final TimestampedState<?> other = (TimestampedState<?>) that;
        return when.equals(other.when) && Objects.equals(state, other.state);
    }

    /**
     * <p>
     * A state of the simulated object at the {@linkplain #getWhen() time}
     * </p>
     * <ul>
     * <li>Null if the object does not exist at that time.</li>
     * </ul>
     */
    @Nullable
    public STATE getState() {
        return state;
    }

    /**
     * <p>
     * The point in time that the simulated object is in the {@linkplain #getState()
     * state}
     * </p>
     * <p>
     * Expressed as the duration since an (implied) epoch. All objects in a
     * simulation should use the same epoch.
     * </p>
     */
    @Nonnull
    public Duration getWhen() {
        return when;
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, when);
    }

    @Nonnull
    @Override
    public String toString() {
        return "@" + when + "=" + state + "]";
    }

}