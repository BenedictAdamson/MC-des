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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.simulation.ObjectStateId;
import uk.badamson.mc.simulation.rx.EventTest.TestEvent;

public class UniverseTest {

    @Nested
    public class AddObject {

        @Nested
        public class One {

            @Test
            public void a() {
                test(EVENT_A);
            }

            @Test
            public void b() {
                test(EVENT_B);
            }

            private <STATE> void test(@Nonnull final Event<STATE> event) {
                final Universe<STATE> universe = new Universe<>();

                AddObject.this.test(universe, event);
            }

        }// class

        private <STATE> void test(@Nonnull final Universe<STATE> universe, @Nonnull final Event<STATE> event) {
            final var objects0 = universe.getObjects();

            universe.addObject(event);

            assertInvariants(universe);
            final var objects = universe.getObjects();
            assertAll("objects",
                    () -> assertTrue(objects.containsAll(objects0),
                            "Does not remove any objects from the set of objects."),
                    () -> assertThat("The set of objects contains the object of the event.", objects,
                            hasItem(event.getObject())),
                    () -> assertThat("Adds one object to the set of objects.", objects, hasSize(objects0.size() + 1)));
        }

        @Test
        public void two() {
            final Universe<Integer> universe = new Universe<>();
            universe.addObject(EVENT_A);

            test(universe, EVENT_B);
        }
    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);

    private static final TestEvent EVENT_A = new TestEvent(new ObjectStateId(OBJECT_A, WHEN_A), Integer.valueOf(0),
            Map.of());
    private static final TestEvent EVENT_B = new TestEvent(new ObjectStateId(OBJECT_B, WHEN_B), Integer.valueOf(1),
            Map.of());

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe) {
        ObjectTest.assertInvariants(universe);// inherited

        final Set<UUID> objects = universe.getObjects();
        assertNotNull(objects, "Not null, objects");// guard
        assertFalse(objects.stream().anyMatch(id -> id == null), "The set of object IDs does not contain a null.");
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe1,
            @Nonnull final Universe<STATE> universe2) {
        ObjectTest.assertInvariants(universe1, universe2);// inherited
    }

    @Test
    public void constructor() {
        final var universe = new Universe<>();

        assertInvariants(universe);
        assertThat("The set of objects is empty.", universe.getObjects(), empty());
    }
}
