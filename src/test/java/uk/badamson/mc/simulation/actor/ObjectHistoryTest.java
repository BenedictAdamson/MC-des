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
import uk.badamson.dbc.assertions.EqualsSemanticsVerifier;
import uk.badamson.dbc.assertions.ObjectVerifier;
import uk.badamson.mc.history.ValueHistoryTest;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Map;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ObjectHistoryTest {

    static final UUID OBJECT_A = UUID.randomUUID();
    static final UUID OBJECT_B = UUID.randomUUID();
    static final UUID OBJECT_C = UUID.randomUUID();
    static final Duration WHEN_A = Duration.ofMillis(0);
    static final Duration WHEN_B = Duration.ofMillis(5000);
    static final Duration WHEN_C = Duration.ofMillis(7000);

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history) {
        ObjectVerifier.assertInvariants(history);// inherited

        final var events = history.getEvents();
        final var lastEvent = history.getLastEvent();
        final var object = history.getObject();
        final var start = history.getStart();
        final var stateHistory = history.getStateHistory();
        final var stateTransitions = history.getStateTransitions();

        assertAll("Not null", () -> assertNotNull(events, "events"), // guard
                () -> assertNotNull(object, "object"), () -> assertNotNull(start, "start"), // guard
                () -> assertNotNull(stateHistory, "stateHistory"), // guard
                () -> assertNotNull(stateTransitions, "stateTransitions") // guard
        );
        ValueHistoryTest.assertInvariants(stateHistory);

        assertAll(() -> assertAll("events", createEventsAssertions(events, object, start)),
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

        assertAll("Value semantics",
                () -> EqualsSemanticsVerifier.assertValueSemantics(history1, history2, "stateTransitions",
                        ObjectHistory::getStateTransitions),
                () -> EqualsSemanticsVerifier.assertValueSemantics(history1, history2, "object", ObjectHistory::getObject),
                () -> assertEquals(history1
                        .equals(history2), (history1.getStateTransitions().equals(history2.getStateTransitions())
                        && history1.getObject().equals(history2.getObject())), "equals"));
    }

    private static <STATE> void constructor(@Nonnull final ObjectHistory<STATE> that) {
        final var copy = new ObjectHistory<>(that);

        assertInvariants(copy);
        assertInvariants(copy, that);
        assertEquals(copy, that);
        assertAll("Copied",
                () -> assertSame(that.getObject(), copy.getObject(), "object"),
                () -> assertSame(that.getStart(), copy.getStart(), "start"),
                () -> assertEquals(that.getStateHistory(), copy.getStateHistory(), "stateHistory"),
                () -> assertEquals(that.getEvents(), copy.getEvents(), "events"));

    }

    private static <STATE> void constructor(@Nonnull final UUID object, @Nonnull final Duration start,
                                            @Nonnull final STATE state) {
        final var history = new ObjectHistory<>(object, start, state);

        assertInvariants(history);
        final var stateTransitions = history.getStateTransitions();
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(start, history.getStart(), "start"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, Map.of(start, state), "stateTransitions"),
                () -> assertThat("events", history.getEvents(), empty()));

    }

    private static <STATE> Stream<Executable> createEventsAssertions(@Nonnull final SortedSet<Event<STATE>> events,
                                                                     @Nonnull final UUID object, @Nonnull final Duration start) {
        return events.stream().map(event -> () -> {
            final Duration whenOccurred = event.getWhenOccurred();
            EventTest.assertInvariants(event);
            assertAll("event [" + event + "]",
                    () -> assertThat("event.affectedObject", event.getAffectedObject(), is(object)),
                    () -> assertThat("event.whenOccurred", whenOccurred, greaterThan(start)));
        });
    }

    @Nested
    public class Constructor {

        @Nested
        public class Copy {

            @Nested
            public class NoSignalsNoEvents {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, 0);
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, 1);
                }

                private <STATE> void test(@Nonnull final UUID object, @Nonnull final Duration start,
                                          @Nonnull final STATE state) {
                    final var history = new ObjectHistory<>(object, start, state);

                    constructor(history);
                }

            }// class

        }// class

        @Nested
        public class InitialState {

            @Test
            public void a() {
                constructor(OBJECT_A, WHEN_A, 0);
            }

            @Test
            public void b() {
                constructor(OBJECT_B, WHEN_B, 1);
            }

            @Nested
            public class Two {

                @Test
                public void differentObject() {
                    final var state = 0;
                    final var historyA = new ObjectHistory<>(OBJECT_A, WHEN_A, state);
                    final var historyB = new ObjectHistory<>(OBJECT_B, WHEN_A, state);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentStart() {
                    final var state = 0;
                    final var historyA = new ObjectHistory<>(OBJECT_A, WHEN_A, state);
                    final var historyB = new ObjectHistory<>(OBJECT_A, WHEN_B, state);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentState() {
                    final var historyA = new ObjectHistory<>(OBJECT_A, WHEN_A, 0);
                    final var historyB = new ObjectHistory<>(OBJECT_A, WHEN_A, 1);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void equivalent() {
                    final var objectA = OBJECT_A;
                    final var objectB = new UUID(objectA.getMostSignificantBits(), objectA.getLeastSignificantBits());
                    final long startMillis = 6000;
                    final var startA = Duration.ofMillis(startMillis);
                    final var startB = Duration.ofMillis(startMillis);
                    final var stateA = Integer.valueOf(Integer.MAX_VALUE);
                    final var stateB = Integer.valueOf(Integer.MAX_VALUE);

                    assert objectA.equals(objectB);
                    assert startA.equals(startB);
                    assert objectA != objectB;// tough test
                    assert startA != startB;// tough test
                    // tough test: Integer.valueOf(Integer.MAX_VALUE) are probably not cached

                    final var historyA = new ObjectHistory<>(objectA, startA, stateA);
                    final var historyB = new ObjectHistory<>(objectB, startB, stateB);

                    assertInvariants(historyA, historyB);
                    assertThat("equals", historyA, is(historyB));
                }
            }// class

        }// class

    }// class

}// class
