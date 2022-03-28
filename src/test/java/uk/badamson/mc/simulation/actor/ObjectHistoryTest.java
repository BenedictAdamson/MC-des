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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uk.badamson.dbc.assertions.ObjectVerifier;
import uk.badamson.mc.history.ValueHistoryTest;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ObjectHistoryTest {

    static final Duration WHEN_A = Duration.ofMillis(0);
    static final Duration WHEN_B = Duration.ofMillis(5000);
    static final Duration WHEN_C = Duration.ofMillis(7000);

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history) {
        ObjectVerifier.assertInvariants(history);// inherited

        final var events = history.getEvents();
        final var lastEvent = history.getLastEvent();
        final var start = history.getStart();
        final var stateHistory = history.getStateHistory();
        final var stateTransitions = history.getStateTransitions();

        assertAll("Not null", () -> assertNotNull(events, "events"), // guard
                () -> assertNotNull(start, "start"), // guard
                () -> assertNotNull(stateHistory, "stateHistory"), // guard
                () -> assertNotNull(stateTransitions, "stateTransitions") // guard
        );
        ValueHistoryTest.assertInvariants(stateHistory);

        assertAll(() -> assertAll("events", createEventsAssertions(events, start)),
                () -> assertAll("lastEvent",
                        () -> assertThat("is null if, and only if, the sequence of events is empty.",
                                lastEvent == null == events.isEmpty()),
                        () -> assertThat("is either null or is the  last of the sequence of events.",
                                lastEvent == null || lastEvent == events.last())),
                () -> assertAll("stateHistory", () -> assertSame(start, stateHistory.getFirstTransitionTime(),

                        "The first transition time of the state history is the same as the start time of this history."),
                        () -> assertNull(stateHistory.getFirstValue(),
                                "The state at the start of time of the state history is null."),
                        () -> assertFalse(stateHistory.isEmpty(), "The state history is never empty.")),
                () -> assertEquals(stateTransitions, stateHistory.getTransitions(), "stateTransitions"));
    }

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history1,
                                                @Nonnull final ObjectHistory<STATE> history2) {
        ObjectVerifier.assertInvariants(history1, history2);// inherited
    }

    private static <STATE> void constructor(@Nonnull final Duration start,
                                            @Nonnull final STATE state) {
        final var history = new ObjectHistory<>(start, state);

        assertInvariants(history);
        final var stateTransitions = history.getStateTransitions();
        assertAll(
                () -> assertSame(start, history.getStart(), "start"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, Map.of(start, state), "stateTransitions"),
                () -> assertThat("events", history.getEvents(), empty()));

    }

    private static <STATE> Stream<Executable> createEventsAssertions(@Nonnull final SortedSet<Event<STATE>> events,
                                                                     @Nonnull final Duration start) {
        return events.stream().map(event -> () -> {
            final Duration whenOccurred = event.getWhen();
            EventTest.assertInvariants(event);
            assertThat("event [" + event + "].whenOccurred", whenOccurred, greaterThan(start));
        });
    }

    @Nested
    public class Constructor {

        @Nested
        public class InitialState {

            @Test
            public void a() {
                constructor(WHEN_A, 0);
            }

            @Test
            public void b() {
                constructor(WHEN_B, 1);
            }

        }// class

    }// class

}// class
