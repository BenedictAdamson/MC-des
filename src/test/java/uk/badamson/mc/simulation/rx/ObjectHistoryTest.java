package uk.badamson.mc.simulation.rx;
/*
 * © Copyright Benedict Adamson 2021.
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.simulation.ObjectStateId;
import uk.badamson.mc.simulation.rx.EventTest.TestEvent;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ObjectHistoryTest {

    @Nested
    public class Constructor {

        @Test
        public void a() {
            test(OBJECT_A, START_A, Integer.valueOf(0), Map.of());
        }

        @Test
        public void B() {
            test(OBJECT_B, START_B, Integer.valueOf(1), Map.of(OBJECT_A, START_B.minusMillis(10)));
        }

        private <STATE> void test(@Nonnull final Event<STATE> event) {
            final var history = new ObjectHistory<>(event);

            assertInvariants(history);
            assertSame(event, history.getLastEvent(), "lastEvent");
        }

        private void test(final UUID object, final Duration start, final Integer state,
                final Map<UUID, Duration> nextEventDependencies) {
            final var event = new TestEvent(new ObjectStateId(object, start), state, nextEventDependencies);
            test(event);
        }

    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();
    private static final Duration START_A = Duration.ofMillis(0);
    private static final Duration START_B = Duration.ofMillis(5000);

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history) {
        ObjectTest.assertInvariants(history);// inherited

        final var object = history.getObject();
        final var start = history.getStart();
        final var lastEvent = history.getLastEvent();

        assertAll("Not null", () -> assertNotNull(object, "object"), () -> assertNotNull(start, "start"), // guard
                () -> assertNotNull(lastEvent, "lastEvent")// guard
        );
        EventTest.assertInvariants(lastEvent);
        assertAll(
                () -> assertSame(object, lastEvent.getObject(),
                        "The object of the last event is the same has the object of this history."),
                () -> assertThat("The time of the last event is at or after the start of this history.",
                        lastEvent.getWhen(), greaterThanOrEqualTo(start)));
    }

    public static <STATE> void assertInvariants(@Nonnull final ObjectHistory<STATE> history1,
            @Nonnull final ObjectHistory<STATE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited
    }

}
