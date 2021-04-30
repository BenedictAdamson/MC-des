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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.ThreadTest;
import uk.badamson.mc.simulation.ObjectStateId;
import uk.badamson.mc.simulation.rx.EventTest.TestEvent;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ObjectHistoryTest {

    @Nested
    public class Append {

        @Nested
        public class One {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0), WHEN_A.plusMillis(10), Integer.valueOf(1));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(3), WHEN_B.plusMillis(1500), Integer.valueOf(2));
            }

            private void test(final UUID object, final Duration when0, final Integer state0, final Duration when1,
                    final Integer state1) {
                final var event0 = new TestEvent(new ObjectStateId(object, when0), state0, Map.of());
                final var event1 = new TestEvent(new ObjectStateId(object, when1), state1, Map.of());
                final var history = new ObjectHistory<>(event0);

                Append.this.test(history, event1);
            }

        }// class

        @RepeatedTest(32)
        public void multiThreaded() {
            final var event0 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A), Integer.valueOf(0), Map.of());
            final var event1 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A.plusMillis(1)), Integer.valueOf(1),
                    Map.of());
            final var event2 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A.plusMillis(2)), Integer.valueOf(2),
                    Map.of());
            final var history = new ObjectHistory<>(event0);

            final CountDownLatch ready = new CountDownLatch(1);
            final var future1 = testInOtherThread(history, event1, ready);
            final var future2 = testInOtherThread(history, event2, ready);
            ready.countDown();
            ThreadTest.get(future1);
            ThreadTest.get(future2);

            assertSame(event2, history.getLastEvent(), "lastEvent");
        }

        private <STATE> void test(@Nonnull final ObjectHistory<STATE> history, @Nonnull final Event<STATE> event) {
            final var object0 = history.getObject();
            final var start0 = history.getStart();

            history.append(event);

            assertInvariants(history);
            assertAll("Does not change constants", () -> assertSame(object0, history.getObject(), "object"),
                    () -> assertSame(start0, history.getStart(), "start"));
            assertSame(event, history.getLastEvent(), "lastEvent");
        }

        private Future<Void> testInOtherThread(final ObjectHistory<Integer> history, final TestEvent event,
                final CountDownLatch ready) {
            return ThreadTest.runInOtherThread(ready, () -> {
                final var object0 = history.getObject();
                final var start0 = history.getStart();

                try {
                    history.append(event);
                } catch (final IllegalStateException e) {
                    // Can happen because of the data race
                }

                assertInvariants(history);
                assertAll("Does not change constants", () -> assertSame(object0, history.getObject(), "object"),
                        () -> assertSame(start0, history.getStart(), "start"));
            });
        }

        @Test
        public void two() {
            final var event0 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A), Integer.valueOf(0), Map.of());
            final var event1 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A.plusMillis(1)), Integer.valueOf(1),
                    Map.of());
            final var event2 = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A.plusMillis(2)), Integer.valueOf(2),
                    Map.of());
            final var history = new ObjectHistory<>(event0);
            history.append(event1);

            test(history, event2);
        }

    }// class

    @Nested
    public class Constructor {

        @Test
        public void a() {
            test(OBJECT_A, WHEN_A, Integer.valueOf(0), Map.of());
        }

        @Test
        public void B() {
            test(OBJECT_B, WHEN_B, Integer.valueOf(1), Map.of(OBJECT_A, WHEN_B.minusMillis(10)));
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
    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);

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
