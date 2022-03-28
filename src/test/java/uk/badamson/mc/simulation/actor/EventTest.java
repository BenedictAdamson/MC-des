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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uk.badamson.dbc.assertions.ObjectVerifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class EventTest {

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);
    private static final Actor<Integer> ACTOR_A = new Actor<>(WHEN_A, 0);
    private static final Actor<Integer> ACTOR_B = new Actor<>(WHEN_B, 1);
    private static final Signal<Integer> SIGNAL_A = new SignalTest.TestSignal(ACTOR_A, WHEN_A, ACTOR_B);
    private static final Signal<Integer> SIGNAL_B = new SignalTest.TestSignal(ACTOR_B, WHEN_B, ACTOR_A);

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event) {
        ObjectVerifier.assertInvariants(event);// inherited

        final var affectedObject = event.getAffectedObject();
        final var causingSignal = event.getCausingSignal();
        final var signalsEmitted = event.getSignalsEmitted();
        final var when = event.getWhen();

        assertAll("Not null", () -> assertNotNull(affectedObject, "affectedObject"),
                () -> assertNotNull(causingSignal, "causingSignal"),
                () -> assertNotNull(signalsEmitted, "signalsEmitted"), // guard
                () -> assertNotNull(when, "when"));

        assertAll("signalsEmitted",
                createSignalsEmittedInvariantAssertions(when, affectedObject, signalsEmitted));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event1,
                                                @Nonnull final Event<STATE> event2) {
        ObjectVerifier.assertInvariants(event1, event2);// inherited
    }

    private static <STATE> void constructor(
            @Nonnull final Signal<STATE> causingSignal,
            @Nonnull final Duration when,
            @Nonnull final Actor<STATE> affectedObject,
                                            @Nullable final STATE state,
            @Nonnull final Set<Signal<STATE>> signalsEmitted) {
        final var event = new Event<>(causingSignal, when, affectedObject, state, signalsEmitted);

        assertInvariants(event);
        assertAll("Attributes", () -> assertSame(causingSignal, event.getCausingSignal(), "causingSignal"),
                () -> assertSame(when, event.getWhen(), "when"),
                () -> assertSame(affectedObject, event.getAffectedObject(), "affectedObject"),
                () -> assertSame(state, event.getState(), "state"),
                () -> assertEquals(signalsEmitted, event.getSignalsEmitted(), "signalsEmitted"));

    }

    private static <STATE> Stream<Executable> createSignalsEmittedInvariantAssertions(
            final Duration when,
            final Actor<STATE> affectedObject, final Set<Signal<STATE>> signalsEmitted) {
        return signalsEmitted.stream().map(signal -> () -> {
            assertNotNull(signal, "signal");
            SignalTest.assertInvariants(signal);
            assertSame(affectedObject, signal.getSender(), "sender");
            assertSame(when, signal.getWhenSent(), "whenSent");
        });
    }

    @Test
    public void destruction() {
        constructor(SIGNAL_A, WHEN_A, ACTOR_A, null, Set.of());
    }

    @Test
    public void noSignalsEmitted_A() {
        constructor(SIGNAL_A, WHEN_A, ACTOR_A, 0, Set.of());
    }

    @Test
    public void noSignalsEmitted_B() {
        constructor(SIGNAL_B, WHEN_B, ACTOR_B, 1, Set.of());
    }

    @Test
    public void signalEmitted() {
        final var when = WHEN_A;
        final var receiver = ACTOR_A;
        final Set<Signal<Integer>> signalsEmitted = Set.of(new SignalTest.TestSignal(receiver, when, ACTOR_B));
        constructor(SIGNAL_A, when, receiver, 0, signalsEmitted);
    }

}// class