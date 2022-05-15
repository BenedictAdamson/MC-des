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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.history.ValueHistory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
 * @param <STATE> The class of states of a {@link Signal}  receiver. This must be {@link Immutable
 *                immutable}. It ought to have value semantics, but that is not
 *                required.
 */
@Immutable
public final class Event<STATE> implements Comparable<Event<STATE>> {

    @Nonnull
    private final Id<STATE> id;

    @Nonnull
    private final Actor<STATE> affectedObject;

    @Nullable
    private final STATE state;

    @Nonnull
    private final Set<Signal<STATE>> signalsEmitted;

    @Nonnull
    private final Set<Actor<STATE>> createdActors;

    @Nonnull
    private final Set<Actor<STATE>> indirectlyAffectedObjects;

    /**
     * <p>
     * Construct a simple event that causes only a state transition.
     * </p>
     */
    public Event(
            @Nonnull final Signal<STATE> causingSignal,
            @Nonnull final Duration when,
            @Nonnull final Actor<STATE> affectedObject,
            @Nullable final STATE state
    ) {
        this.id = new Id<>(causingSignal, when);
        this.affectedObject = Objects.requireNonNull(affectedObject, "affectedObject");
        this.state = state;
        this.signalsEmitted = Set.of();
        this.createdActors = Set.of();
        this.indirectlyAffectedObjects = Set.of(affectedObject);
    }

    /**
     * <p>
     * Construct an event with given attribute values.
     * </p>
     *
     * @throws NullPointerException     <ul>
     *                                  <li>If any {@link Nonnull} argument is null.</li>
     *                                  <li>If {@code signalsEmitted} contains a null.</li>
     *                                  <li>If {@code createdActors} contains a null.</li>
     *                                  </ul>
     * @throws IllegalArgumentException <ul>
     *                                  <li>If {@code signalsEmitted} contains a signal that was not
     *                                  {@linkplain Signal#getSender() sent} from the same object as
     *                                  the {@code affectedObject}</li>
     *                                  <li>If {@code signalsEmitted} contains a signal that was not
     *                                  {@linkplain Signal#getWhenSent() sent} at the same time as
     *                                  {@code when}.</li>
     *                                  <li> If {@code createdActors} {@linkplain Set#contains(Object) contains} {@code affectedObject}.</li>
     *                                  <li>If any of the {@code createdActors} have a
     *                                  {@linkplain Actor#getStart() start time} that is not {@linkplain Duration#equals(Object) equal to} {@code when}.</li>
     *                                  </ul>
     */
    public Event(@Nonnull final Signal<STATE> causingSignal,
                 @Nonnull final Duration when,
                 @Nonnull final Actor<STATE> affectedObject,
                 @Nullable final STATE state,
                 @Nonnull final Set<Signal<STATE>> signalsEmitted,
                 @Nonnull final Set<Actor<STATE>> createdActors) {
        this.id = new Id<>(causingSignal, when);
        this.affectedObject = Objects.requireNonNull(affectedObject, "affectedObject");
        this.state = state;
        this.signalsEmitted = Set.copyOf(signalsEmitted);
        this.createdActors = Set.copyOf(createdActors);
        /* Check after copy to avoid race hazards. */
        this.signalsEmitted.forEach(signal -> {
            if (affectedObject != signal.getSender()) {
                throw new IllegalArgumentException("signalEmitted not sent from sender.");
            }
            if (when != signal.getWhenSent()) {
                throw new IllegalArgumentException("signalEmitted not sent at event time.");
            }
        });
        this.createdActors.forEach(actor -> {
            if (!when.equals(actor.getStart())) {
                throw new IllegalArgumentException("createdActors.start not equal to when.");
            }
            if (this.createdActors.contains(affectedObject)) {
                throw new IllegalArgumentException("affectedObject is a createdActors.");
            }
        });
        this.indirectlyAffectedObjects = createIndirectlyAffectedObjects(
                affectedObject, this.signalsEmitted
        );
    }

    private static <STATE> Set<Actor<STATE>> createIndirectlyAffectedObjects(
            @Nonnull final Actor<STATE> affectedObject,
            @Nonnull final Set<Signal<STATE>> signalsEmitted) {
        final Set<Actor<STATE>> result = new HashSet<>(1 + signalsEmitted.size());
        result.add(affectedObject);
        signalsEmitted.forEach(signal -> result.add(signal.getReceiver()));
        return Set.copyOf(result);
    }

    /**
     * <p>
     * The simulated object changed by this event.
     * </p>
     */
    @Nonnull
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "effectively immutable")
    public Actor<STATE> getAffectedObject() {
        return affectedObject;
    }

    /**
     * <p>
     * All simulated objects that will indirectly be affected by this event.
     * </p>
     * <ul>
     *     <li>Does not contain null.</li>
     *     <li>May be unmodifiable</li>
     *     <li>{@linkplain Set#contains(Object) contains} the {@linkplain #getAffectedObject() directly affected object}.</li>
     *     <li>{@linkplain Set#contains(Object) contains} the {@linkplain Signal#getReceiver() receivers} of {@linkplain #getSignalsEmitted() signals emitted}.</li>
     * </ul>
     */
    @Nonnull
    public Set<Actor<STATE>> getIndirectlyAffectedObjects() {
        return indirectlyAffectedObjects;
    }

    @Nonnull
    public Signal<STATE> getCausingSignal() {
        return id.getCausingSignal();
    }

    /**
     * <p>
     * Signals emitted from the {@linkplain #getAffectedObject() affected object} as part of this event.
     * </p>
     * <ul>
     * <li>The returned set of signals emitted is a constant (the method always
     * returns a reference to the same object).</li>
     * <li>The returned set of signals emitted is unmodifiable.</li>
     * <li>The set of signals emitted does not contain a null signal.</li>
     * <li>The returned set of signals emitted may be {@linkplain Set#isEmpty()
     * empty}.</li>
     * </ul>
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "signalsEmitted is unmodifiable")
    @Nonnull
    public Set<Signal<STATE>> getSignalsEmitted() {
        return signalsEmitted;
    }

    /**
     * <p>
     * The state that the {@linkplain #getAffectedObject() affected object} has as a result of this event.
     * </p>
     * <p>
     * A null state indicates that the affected object is destroyed or removed.
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
    public Duration getWhen() {
        return id.getWhen();
    }

    /**
     * The Actors that have their a non-null {@linkplain Actor#getStateHistory() state} as a result of this event.
     *
     * <ul>
     * <li>Does not contains null.</li>
     * <li>May be {@linkplain Set#isEmpty() empty}</li>
     * <li>Does not {@linkplain Set#contains(Object) contain} the
     *  {@linkplain #getAffectedObject() directly affected object}.</li>
     * <li>The {@linkplain Actor#getStart() start time} of the created actors is
     * {@linkplain Duration#equals(Object) equal to} the {@linkplain #getWhen() time that this event occurred}.</li>
     * </ul>
     */
    @Nonnull
    public Set<Actor<STATE>> getCreatedActors() {
        return createdActors;
    }

    /**
     * The unique ID of this event.
     *
     * <ul>
     *     <li>The {@linkplain #getCausingSignal() causing signal} of this event is the same as the {@linkplain Id#getCausingSignal() causing signal} of the ID.</li>
     *     <li>The {@linkplain #getWhen() time of occurrence} of this event is the same as the {@linkplain Id#getWhen() time of occurrence}  of the ID.</li>
     * </ul>
     */
    @Nonnull
    public Id<STATE> getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Event{" + id + "}";
    }

    /**
     * Whether this object is <i>equivalent to</i> a given object.
     * <p>
     * The Event class has <i>reference semantics</i>,
     * with the {@link #getId() providing the unique ID}.
     */
    @Override
    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        final Event<?> event = (Event<?>) that;

        return id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The natural ordering of Event objects is the same as the natural ordering of their {@linkplain #getId() IDs}.
     * </p>
     */
    @Override
    public int compareTo(@Nonnull final Event<STATE> that) {
        return id.compareTo(that.id);
    }

    /**
     * The unique ID of an {@link Event}.
     *
     * @param <STATE> The class of states of a {@link Signal} receiver. This must be {@link Immutable
     *                immutable}. It ought to have value semantics, but that is not
     *                required.
     */
    public static final class Id<STATE> implements Comparable<Id<STATE>> {

        @Nonnull
        private final Signal<STATE> causingSignal;

        @Nonnull
        private final Duration when;

        public Id(
                @Nonnull final Signal<STATE> causingSignal,
                @Nonnull final Duration when
        ) {
            this.causingSignal = Objects.requireNonNull(causingSignal, "causingSignal");
            this.when = Objects.requireNonNull(when, "when");
        }

        /**
         * <p>
         * The point in time that the event occurred.
         * </p>
         */
        @Nonnull
        public Duration getWhen() {
            return when;
        }

        /**
         * <p>
         * The signal, {@linkplain Signal#receiveForStateHistory(ValueHistory) reception} of which
         * caused the event.
         * </p>
         */
        @Nonnull
        public Signal<STATE> getCausingSignal() {
            return causingSignal;
        }


        @Override
        public String toString() {
            return "@" + when + "\u2190" + causingSignal;
        }

        /**
         * Whether this object is equivalent to another.
         * <p>
         * This class has <i>value semantics</i>.
         */
        @Override
        public boolean equals(final Object that) {
            if (this == that) return true;
            if (that == null || getClass() != that.getClass()) return false;

            final Id<?> id = (Id<?>) that;

            return when.equals(id.when) && causingSignal.equals(id.causingSignal);
        }

        @Override
        public int hashCode() {
            int result = when.hashCode();
            result = 31 * result + causingSignal.hashCode();
            return result;
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * The natural ordering of Event.Id objects is by their
         * {@linkplain  #getWhen() time of occurrence},
         * then by a tie-break.
         * The natural ordering is consistent with {@link #equals(Object)}
         * </p>
         */
        @Override
        public int compareTo(@Nonnull final Id<STATE> that) {
            int c = when.compareTo(that.when);
            if (c == 0) {
                c = causingSignal.tieBreakCompareTo(that.causingSignal);
            }
            return c;
        }

    }
}