package uk.badamson.mc.simulation.rx;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.dbc.assertions.EqualsSemanticsTest;
import uk.badamson.dbc.assertions.ObjectTest;
import uk.badamson.mc.JsonTest;
import uk.badamson.mc.simulation.ObjectStateId;
import uk.badamson.mc.simulation.ObjectStateIdTest;

/**
 * <p>
 * Unit tests for the {@link Event} class.
 * </p>
 */
@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class EventTest {

    @Nested
    public class Constructor {

        @Nested
        public class Two {

            @Test
            public void differentNextEventDependencies() {
                // Tough test: all other attributes and aggregates the same
                final var id = new ObjectStateId(OBJECT_A, WHEN_A);
                final Integer state = Integer.valueOf(0);
                final Map<UUID, Duration> nextEventDependenciesA = Map.of();
                final Map<UUID, Duration> nextEventDependenciesB = Map.of(OBJECT_B, Duration.ofMillis(-100));

                final var eventA = new TestEvent(id, state, nextEventDependenciesA);
                final var eventB = new TestEvent(id, state, nextEventDependenciesB);

                assertInvariants(eventA, eventB);
                assertNotEquals(eventA, eventB);
            }

            @Test
            public void differentObjects() {
                // Tough test: all other attributes and aggregates the same
                final var idA = new ObjectStateId(OBJECT_A, WHEN_A);
                final var idB = new ObjectStateId(OBJECT_B, WHEN_A);
                final Integer state = Integer.valueOf(0);
                final Map<UUID, Duration> nextEventDependencies = Map.of();

                final var eventA = new TestEvent(idA, state, nextEventDependencies);
                final var eventB = new TestEvent(idB, state, nextEventDependencies);

                assertInvariants(eventA, eventB);
                assertNotEquals(eventA, eventB);
            }

            @Test
            public void differentState() {
                // Tough test: all other attributes and aggregates the same
                final var id = new ObjectStateId(OBJECT_A, WHEN_A);
                final Integer stateA = Integer.valueOf(0);
                final Integer stateB = Integer.valueOf(1);
                final Map<UUID, Duration> nextEventDependencies = Map.of();

                final var eventA = new TestEvent(id, stateA, nextEventDependencies);
                final var eventB = new TestEvent(id, stateB, nextEventDependencies);

                assertInvariants(eventA, eventB);
                assertNotEquals(eventA, eventB);
            }

            @Test
            public void differentWhen() {
                // Tough test: all other attributes and aggregates the same
                final var idA = new ObjectStateId(OBJECT_A, WHEN_A);
                final var idB = new ObjectStateId(OBJECT_A, WHEN_B);
                final Integer state = Integer.valueOf(0);
                final Map<UUID, Duration> nextEventDependencies = Map.of();

                final var eventA = new TestEvent(idA, state, nextEventDependencies);
                final var eventB = new TestEvent(idB, state, nextEventDependencies);

                assertInvariants(eventA, eventB);
                assertNotEquals(eventA, eventB);
            }

            @Test
            public void equivalent() {
                final Integer stateA = Integer.valueOf(Integer.MAX_VALUE);
                final Integer stateB = Integer.valueOf(Integer.MAX_VALUE);
                final Map<UUID, Duration> nextEventDependenciesA = Map.of(OBJECT_B, Duration.ofMillis(-100));
                final Map<UUID, Duration> nextEventDependenciesB = new HashMap<>(nextEventDependenciesA);
                final ObjectStateId idA = ID_A;
                final ObjectStateId idB = new ObjectStateId(ID_A.getObject(), ID_A.getWhen());
                assert idA.equals(idB);
                assert idA != idB;// tough test
                assert stateA.equals(stateB);
                assert stateA != stateB;// tough test
                assert nextEventDependenciesA.equals(nextEventDependenciesB);
                assert nextEventDependenciesA != nextEventDependenciesB;// tough test

                final var eventA = new TestEvent(idA, stateA, nextEventDependenciesA);
                final var eventB = new TestEvent(idB, stateA, nextEventDependenciesB);

                assertInvariants(eventA, eventB);
                assertEquals(eventA, eventB);
            }

            @Test
            public void equivalent_nullState() {
                final Integer state = null;
                final Map<UUID, Duration> nextEventDependencies = Map.of();
                final var eventA = new TestEvent(ID_A, state, nextEventDependencies);
                final var eventB = new TestEvent(ID_A, state, nextEventDependencies);

                assertInvariants(eventA, eventB);
                assertEquals(eventA, eventB);
            }
        }// class

        @Test
        public void a() {
            test(ID_A, Integer.valueOf(1), Map.of(OBJECT_B, Duration.ofMillis(-1)));
        }

        @Test
        public void b() {
            test(ID_B, Integer.valueOf(2), Map.of(OBJECT_A, Duration.ofMillis(-100)));
        }

        @Test
        public void destruction() {
            test(ID_A, null, Map.of());
        }

        @Test
        public void noDependencies() {
            test(ID_A, Integer.valueOf(0), Map.of());
        }

        private void test(final ObjectStateId id, final Integer state,
                final Map<UUID, Duration> nextEventDependencies) {
            final var event = new TestEvent(id, state, nextEventDependencies);
            assertInvariants(event);
            assertAll("Has the given attributes and aggregates", () -> assertSame(id, event.getId(), "id"),
                    () -> assertSame(state, event.getState(), "state"),
                    () -> assertSame(nextEventDependencies, event.getNextEventDependencies(), "nextEventDependencies"));
        }
    }// class

    @Nested
    public class JSON {

        @Test
        public void a() {
            test(ID_A, Integer.valueOf(1), Map.of(OBJECT_B, Duration.ofMillis(-1)));
        }

        @Test
        public void b() {
            test(ID_B, Integer.valueOf(2), Map.of(OBJECT_A, Duration.ofMillis(-100)));
        }

        private <STATE> void test(@Nonnull final Event<STATE> event) {
            final var deserialized = JsonTest.serializeAndDeserialize(event);

            assertInvariants(event);
            assertInvariants(event, deserialized);
            assertEquals(event, deserialized);
        }

        private void test(final ObjectStateId id, final Integer state,
                final Map<UUID, Duration> nextEventDependencies) {
            final var event = new TestEvent(id, state, nextEventDependencies);
            test(event);
        }
    }// class

    static final class TestEvent extends Event<Integer> {

        @JsonCreator
        public TestEvent(@JsonProperty("id") final ObjectStateId id, @JsonProperty("state") final Integer state,
                @JsonProperty("nextEventDependencies") final Map<UUID, Duration> nextEventDependencies) {
            super(id, state, nextEventDependencies);
        }

        @Override
        public Map<UUID, Event<Integer>> computeNextEvents(@Nonnull final Map<UUID, Integer> dependentStates) {
            Objects.requireNonNull(dependentStates, "dependentStates");
            if (getState() == null) {
                throw new IllegalStateException("Destroyed objects may not be resurrected");
            }

            final var value = getState().intValue();
            if (value == Integer.MAX_VALUE) {
                // Magic number to trigger a destruction event
                final var whenNext = getWhen().plusMillis(250);
                return Map.of(getObject(), new TestEvent(new ObjectStateId(getObject(), whenNext), null, Map.of()));
            } else if (value == Integer.MIN_VALUE) {
                // Magic number to trigger a creation event
                final var whenNext = getWhen().plusMillis(250);
                final var successorEvent = new TestEvent(new ObjectStateId(getObject(), whenNext), Integer.valueOf(0),
                        Map.of());
                final var createdObject = UUID.randomUUID();
                final var creationEvent = new TestEvent(new ObjectStateId(createdObject, whenNext), Integer.valueOf(1),
                        Map.of());
                return Map.of(getObject(), successorEvent, createdObject, creationEvent);
            } else {
                final Map<UUID, Duration> nextEventDependencies = new HashMap<>();
                this.getNextEventDependencies()
                        .forEach((object, when) -> nextEventDependencies.put(object, when.plusMillis(100)));
                final int dependentValuesSum = this.getNextEventDependencies().keySet().stream()
                        .map(dependentId -> dependentStates.get(dependentId))
                        .mapToInt(dependentState -> dependentState == null ? 0 : dependentState.intValue()).sum();
                final var nextValue = value + dependentValuesSum + 1;
                final var delay = 250 * (1 + Math.abs(nextValue));
                return Map.of(getObject(), new TestEvent(new ObjectStateId(getObject(), getWhen().plusMillis(delay)),
                        Integer.valueOf(nextValue), nextEventDependencies));
            }
        }

    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(750);

    private static final ObjectStateId ID_A = new ObjectStateId(OBJECT_A, WHEN_A);
    private static final ObjectStateId ID_B = new ObjectStateId(OBJECT_B, WHEN_B);

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event) {
        ObjectTest.assertInvariants(event);// inherited

        final var id = event.getId();
        final var nextEventDependencies = event.getNextEventDependencies();
        final var object = event.getObject();
        final var state = event.getState();
        final var when = event.getWhen();
        assertAll("Not null", () -> assertNotNull(id, "id"), // guard
                () -> assertNotNull(nextEventDependencies, "nextEventDependencies"),
                () -> assertNotNull(object, "object"), () -> assertNotNull(when, "when"));
        ObjectStateIdTest.assertInvariants(id);
        assertAll(
                () -> assertAll("Delgates", () -> assertSame(id.getObject(), object, "object"),
                        () -> assertSame(id.getWhen(), when, "when")),
                () -> assertAll("nextEventDependencies",
                        createNextEventDependenciesAssertions(nextEventDependencies, object, when)),
                () -> assertFalse(state == null && !nextEventDependencies.isEmpty(),
                        "If this event is a destruction event the collection of next event dependencies is empty."));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event1,
            @Nonnull final Event<STATE> event2) {
        ObjectTest.assertInvariants(event1, event2);// inherited

        assertAll("Value semantics",
                () -> EqualsSemanticsTest.assertValueSemantics(event1, event2, "id", e -> e.getId()),
                () -> EqualsSemanticsTest.assertValueSemantics(event1, event2, "state", e -> e.getState()),
                () -> EqualsSemanticsTest.assertValueSemantics(event1, event2, "nextEventDependencies",
                        e -> e.getNextEventDependencies()),
                () -> assertTrue(
                        event1.equals(event2) == (event1.getId().equals(event2.getId())
                                && Objects.equals(event1.getState(), event2.getState())
                                && event1.getNextEventDependencies().equals(event2.getNextEventDependencies())),
                        "equals"));
    }

    private static Stream<Executable> createNextEventDependenciesAssertions(
            final Map<UUID, Duration> nextEventDependencies, final UUID eventObject, final Duration eventWhen) {
        return nextEventDependencies.entrySet().stream().map(entry -> new Executable() {

            @Override
            public void execute() throws Throwable {
                final UUID object = entry.getKey();
                final Duration when = entry.getValue();
                assertAll("Not null", // guard
                        () -> assertNotNull(object, "object (key)"), () -> assertNotNull(when, "when (value)"));
                assertAll(
                        () -> assertThat("All the time-stamps are before when this event occurs", when,
                                lessThan(eventWhen)),
                        () -> assertNotEquals(eventObject, object,
                                "None of the object IDs are equal to the object ID of this event"));
            }
        });
    }
}
