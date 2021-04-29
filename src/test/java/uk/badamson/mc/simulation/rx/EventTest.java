package uk.badamson.mc.simulation.rx;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.simulation.ObjectStateId;

/**
 * <p>
 * Unit tests for the {@link Event} class.
 * </p>
 */
@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class EventTest {

    @Nested
    public class Constructor {

        @Test
        public void a() {
            test(ID_A, Integer.valueOf(1), Map.of(OBJECT_B, Duration.ofMillis(-1)));
        }

        @Test
        public void b() {
            test(ID_B, Integer.valueOf(2), Map.of(OBJECT_A, Duration.ofMillis(-100)));
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

    private static final class TestEvent extends Event<Integer> {

        public TestEvent(final ObjectStateId id, final Integer state, final Map<UUID, Duration> nextEventDependencies) {
            super(id, state, nextEventDependencies);
        }

        @Override
        public Event<Integer> computeNextEvent(final Map<UUID, Integer> dependentStates) {
            final Map<UUID, Duration> nextEventDependencies = new HashMap<>();
            this.getNextEventDependencies()
                    .forEach((object, when) -> nextEventDependencies.put(object, when.plusMillis(100)));
            return new TestEvent(new ObjectStateId(getObject(), getWhen().plusMillis(250)),
                    Integer.valueOf(getState().intValue() + 1), nextEventDependencies);
        }

    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();

    private static final ObjectStateId ID_A = new ObjectStateId(OBJECT_A, Duration.ofMillis(0));
    private static final ObjectStateId ID_B = new ObjectStateId(OBJECT_B, Duration.ofMillis(750));

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event) {
        ObjectTest.assertInvariants(event);// inherited

        final var id = event.getId();
        final var nextEventDependencies = event.getNextEventDependencies();
        final var object = event.getObject();
        final var state = event.getState();
        final var when = event.getWhen();
        assertAll("Not null", () -> assertNotNull(id, "id"),
                () -> assertNotNull(nextEventDependencies, "nextEventDependencies"),
                () -> assertNotNull(object, "object"), () -> assertNotNull(state, "state"),
                () -> assertNotNull(when, "when"));
        assertAll(
                () -> assertAll("Delgates", () -> assertSame(id.getObject(), object, "object"),
                        () -> assertSame(id.getWhen(), when, "when")),
                () -> assertAll("nextEventDependencies",
                        createNextEventDependenciesAssertions(nextEventDependencies, object, when)));
    }

    public static <STATE> void assertInvariants(@Nonnull final Event<STATE> event1,
            @Nonnull final Event<STATE> event2) {
        ObjectTest.assertInvariants(event1, event2);// inherited
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
