package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2018,2021.
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.badamson.dbc.assertions.CollectionTest;
import uk.badamson.dbc.assertions.EqualsSemanticsTest;
import uk.badamson.dbc.assertions.ObjectTest;

public class UniverseTest {

    @Nested
    public class Constructor {

        @Nested
        public class Copy {

            @Nested
            public class One {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, Integer.valueOf(0));
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, Integer.valueOf(1));
                }

                private void test(@Nonnull final UUID object, @Nonnull final Duration start,
                        @Nonnull final Integer state) {
                    final var objectHistory = new ObjectHistory<>(object, start, state);
                    final Collection<ObjectHistory<Integer>> objectHistories = List.of(objectHistory);
                    final var universe = new Universe<Integer>(objectHistories);

                    constructor(universe);
                }
            }// class

            @Test
            public void empty() {
                final var universe = new Universe<Integer>();

                constructor(universe);

                assertEmpty(universe);
            }
        }// class

        @Nested
        public class FromObjectHistories {

            @Nested
            public class One {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, Integer.valueOf(0));
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, Integer.valueOf(1));
                }

                private void test(@Nonnull final UUID object, @Nonnull final Duration start,
                        @Nonnull final Integer state) {
                    final var objectHistory = new ObjectHistory<>(object, start, state);
                    final Collection<ObjectHistory<Integer>> objectHistories = List.of(objectHistory);

                    constructor(objectHistories);
                }
            }// class

            @Test
            public void empty() {
                constructor(List.of());
            }

            @Test
            public void two() {
                final var objectHistoryA = new ObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(0));
                final var objectHistoryB = new ObjectHistory<>(OBJECT_B, WHEN_B, Integer.valueOf(1));
                final Collection<ObjectHistory<Integer>> objectHistories = List.of(objectHistoryA, objectHistoryB);

                constructor(objectHistories);
            }
        }// class

        @Nested
        public class Two {

            @Test
            public void different_objects() {
                final var state = Integer.valueOf(0);
                final var objectHistoryA = new ObjectHistory<>(OBJECT_A, WHEN_A, state);
                final var objectHistoryB = new ObjectHistory<>(OBJECT_B, WHEN_A, state);
                final Collection<ObjectHistory<Integer>> objectHistoriesA = List.of(objectHistoryA);
                final Collection<ObjectHistory<Integer>> objectHistoriesB = List.of(objectHistoryB);
                final var universeA = new Universe<Integer>(objectHistoriesA);
                final var universeB = new Universe<Integer>(objectHistoriesB);

                assertInvariants(universeA, universeB);
                assertThat(universeA, not(is(universeB)));
            }

            @Test
            public void different_stateHistories() {
                final var objectHistoryA = new ObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(0));
                final var objectHistoryB = new ObjectHistory<>(OBJECT_A, WHEN_B, Integer.valueOf(1));
                final Collection<ObjectHistory<Integer>> objectHistoriesA = List.of(objectHistoryA);
                final Collection<ObjectHistory<Integer>> objectHistoriesB = List.of(objectHistoryB);
                final var universeA = new Universe<Integer>(objectHistoriesA);
                final var universeB = new Universe<Integer>(objectHistoriesB);

                assertInvariants(universeA, universeB);
                assertThat(universeA, not(is(universeB)));
            }

            @Test
            public void equivalent_empty() {
                final var universeA = new Universe<Integer>();
                final var universeB = new Universe<Integer>();

                assertInvariants(universeA, universeB);
                assertThat("equals", universeA, is(universeB));
            }

            @Test
            public void equivalent_nonEmpty() {
                final var object = OBJECT_A;
                final var objectHistoryA = new ObjectHistory<>(object, WHEN_A, Integer.valueOf(0));
                final var objectHistoryB = new ObjectHistory<>(object, WHEN_A, Integer.valueOf(0));
                final Collection<ObjectHistory<Integer>> objectHistoriesA = List.of(objectHistoryA);
                final Collection<ObjectHistory<Integer>> objectHistoriesB = List.of(objectHistoryB);
                final var universeA = new Universe<Integer>(objectHistoriesA);
                final var universeB = new Universe<Integer>(objectHistoriesB);

                assertInvariants(universeA, universeB);
                assertThat("equals", universeA, is(universeB));
            }
        }// class

        @Test
        public void noArgs() {
            final var universe = new Universe<>();

            assertInvariants(universe);
            assertEmpty(universe);
        }

    }// class

    public static class SchedulingMediumTest {

        static <STATE> void addAll(@Nonnull final Universe<STATE>.SchedulingMedium medium,
                @Nonnull final Collection<Signal<STATE>> signals) {
            MediumTest.addAll(medium, signals);// inherited

            assertInvariants(medium);
        }

        static <STATE> void assertInvariants(@Nonnull final Universe<STATE>.SchedulingMedium medium) {
            ObjectTest.assertInvariants(medium);// inherited
            MediumTest.assertInvariants(medium);// inherited
        }

        static <STATE> void assertInvariants(@Nonnull final Universe<STATE>.SchedulingMedium medium1,
                @Nonnull final Universe<STATE>.SchedulingMedium medium2) {
            ObjectTest.assertInvariants(medium1, medium2);// inherited
            MediumTest.assertInvariants(medium1, medium2);// inherited
        }

        static <STATE> void removeAll(@Nonnull final Universe<STATE>.SchedulingMedium medium,
                @Nonnull final Collection<Signal<STATE>> signals) {
            MediumTest.removeAll(medium, signals);// inherited

            assertInvariants(medium);
        }
    }// class

    static final UUID OBJECT_A = ObjectHistoryTest.OBJECT_A;

    static final UUID OBJECT_B = ObjectHistoryTest.OBJECT_B;

    static final UUID OBJECT_C = ObjectHistoryTest.OBJECT_C;

    static final Duration WHEN_A = ObjectHistoryTest.WHEN_A;

    static final Duration WHEN_B = ObjectHistoryTest.WHEN_B;

    static final Duration WHEN_C = ObjectHistoryTest.WHEN_C;

    private static <STATE> void assertEmpty(@Nonnull final Universe<STATE> universe) {
        assertAll("empty", () -> assertThat("objects", universe.getObjects(), empty()),
                () -> assertThat("objectHistories", universe.getObjectHistories(), empty()));
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe) {
        ObjectTest.assertInvariants(universe);// inherited

        final Set<UUID> objects = universe.getObjects();
        final Collection<ObjectHistory<STATE>> objectHistories = universe.getObjectHistories();

        assertAll("Not null", () -> assertNotNull(objects, "objects"), // guard
                () -> assertNotNull(objectHistories, "objectHistories")// guard
        );
        assertFalse(objects.stream().anyMatch(id -> id == null), "The set of object IDs does not contain a null.");
        CollectionTest.assertForAllElements("objectHistories", objectHistories, history -> {
            assertThat(history, notNullValue());// guard
            ObjectHistoryTest.assertInvariants(history);
        });
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe1,
            @Nonnull final Universe<STATE> universe2) {
        ObjectTest.assertInvariants(universe1, universe2);// inherited

        assertAll("Value semantics",
                () -> EqualsSemanticsTest.assertValueSemantics(universe1, universe2, "objectHistories",
                        u -> u.getObjectHistories()),
                () -> assertTrue(universe1.equals(universe2) == universe1.getObjectHistories()
                        .equals(universe2.getObjectHistories()), "equals"));
    }

    private static <STATE> void constructor(@Nonnull final Collection<ObjectHistory<STATE>> objectHistories) {
        final var universe = new Universe<>(objectHistories);

        assertInvariants(universe);
        assertThat("copied objectHistories", universe.getObjectHistories().containsAll(objectHistories));
    }

    private static <STATE> Universe<STATE> constructor(@Nonnull final Universe<STATE> that) {
        final var copy = new Universe<>(that);

        assertInvariants(copy);
        assertInvariants(that);
        assertInvariants(copy, that);
        assertThat("equals", copy, is(that));

        assertAll("copied content", () -> assertThat("objects", copy.getObjects(), is(that.getObjects())),
                () -> assertThat("objectHistories", copy.getObjectHistories(), is(that.getObjectHistories())));

        return copy;
    }
}
