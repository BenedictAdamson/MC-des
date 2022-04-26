package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2018,2021-22.
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

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.badamson.dbc.assertions.CollectionVerifier;
import uk.badamson.dbc.assertions.ObjectVerifier;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UniverseTest {

    static final Duration WHEN_A = ActorTest.WHEN_A;

    static final Duration WHEN_B = ActorTest.WHEN_B;

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe) {
        ObjectVerifier.assertInvariants(universe);// inherited

        CollectionVerifier.assertForAllElements(universe, actor -> {
            assertThat(actor, notNullValue());// guard
            ActorTest.assertInvariants(actor);
        });
        assertThat(universe.toArray().length, is(universe.size()));
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe1,
                                                @Nonnull final Universe<STATE> universe2) {
        ObjectVerifier.assertInvariants(universe1, universe2);// inherited
    }

    private static <STATE> boolean add(@Nonnull final Universe<STATE> universe, @Nonnull final Actor<STATE> actor) {
        final int size0 = universe.size();

        final boolean added = universe.add(actor);

        assertInvariants(universe);
        ActorTest.assertInvariants(actor);
        assertThat(universe, hasItem(actor));
        assertTrue(universe.contains(actor));
        assertThat(universe, not(empty()));
        assertTrue(universe.iterator().hasNext());
        assertThat("parallelStream contains", universe.parallelStream().collect(Collectors.toUnmodifiableList()), hasItem(actor));
        assertThat(universe, either(hasSize(size0 + 1)).or(hasSize(size0)));
        assertThat("stream contains", universe.stream().collect(Collectors.toUnmodifiableList()), hasItem(actor));

        return added;
    }

    private static <STATE> boolean addAll(@Nonnull final Universe<STATE> universe, @Nonnull final Collection<Actor<STATE>> actors) {
        final boolean changed = universe.addAll(actors);

        assertInvariants(universe);
        assertTrue(universe.containsAll(actors));

        return changed;
    }

    private static <STATE> void clear(@Nonnull final Universe<STATE> universe) {
        universe.clear();

        assertInvariants(universe);
        assertThat(universe, empty());
        assertThat(universe, hasSize(0));
    }

    private static <STATE> boolean remove(@Nonnull final Universe<STATE> universe, final Object o) {
        final int size0 = universe.size();

        final boolean removed = universe.remove(o);

        assertInvariants(universe);
        assertFalse(universe.contains(o));
        assertThat(universe, hasSize(removed ? size0 - 1 : size0));

        return removed;
    }

    private static <STATE> boolean removeAll(@Nonnull final Universe<STATE> universe, final Collection<?> objects) {
        final int size0 = universe.size();

        final boolean removed = universe.removeAll(objects);

        assertInvariants(universe);
        assertThat(universe.size(), lessThanOrEqualTo(size0));
        assertFalse(removed && universe.size() == size0);

        return removed;
    }

    private static <STATE> boolean retainAll(@Nonnull final Universe<STATE> universe, @Nonnull final Collection<?> c) {
        final boolean changed = universe.retainAll(c);

        assertInvariants(universe);

        return changed;
    }

    @Test
    public void constructor() {
        final var universe = new Universe<>();

        assertInvariants(universe);
        assertThat(universe, empty());
        assertThat(universe.size(), is(0));
        assertThat("iterator.next", !universe.iterator().hasNext());
        assertThat(universe.toArray(), emptyArray());
    }

    @Nested
    public class Add {
        @Nested
        public class Once {

            @Test
            public void a() {
                test(WHEN_A, 0);
            }

            @Test
            public void b() {
                test(WHEN_B, 1);
            }

            private void test(@Nonnull final Duration start, @Nonnull final Integer state0) {
                final Actor<Integer> actor = new Actor<>(start, state0);
                final Universe<Integer> universe = new Universe<>();

                final boolean added = add(universe, actor);

                assertThat("added", added);
                assertThat(universe, hasSize(1));
                assertThat(universe.iterator().next(), sameInstance(actor));
            }
        }

        @Nested
        public class Twice {
            @Test
            public void same() {
                final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actor);

                final boolean added = add(universe, actor);

                assertThat("not added", !added);
                assertThat(universe, hasSize(1));
                assertThat(universe.iterator().next(), sameInstance(actor));
            }

            @Test
            public void distinct() {
                final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
                final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
                final Universe<Integer> universe = new Universe<>();
                universe.add(actorA);

                final boolean added = add(universe, actorB);

                assertThat("added", added);
                assertThat(universe, hasSize(2));
                assertThat(universe, containsInAnyOrder(actorA, actorB));
                assertTrue(universe.containsAll(List.of(actorA, actorB)));
            }
        }
    }

    @Nested
    public class AddAll {
        @Test
        public void empty() {
            final Universe<Integer> universe = new Universe<>();
            final Collection<Actor<Integer>> actors = List.of();

            final boolean changed = addAll(universe, actors);

            assertFalse(changed);
            assertThat(universe, Matchers.empty());
        }

        @Test
        public void oneToEmpty() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            final Collection<Actor<Integer>> actors = List.of(actor);

            final boolean changed = addAll(universe, actors);

            assertTrue(changed);
            assertThat(universe, contains(actor));
        }

        @Test
        public void oneToNotEmpty() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            universe.add(actorA);
            final Collection<Actor<Integer>> actors = List.of(actorB);

            final boolean changed = addAll(universe, actors);

            assertTrue(changed);
            assertThat(universe, containsInAnyOrder(actorA, actorB));
        }

        @Test
        public void oneRedundant() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            final Collection<Actor<Integer>> actors = List.of(actor);
            universe.add(actor);

            final boolean changed = addAll(universe, actors);

            assertFalse(changed);
            assertThat(universe, contains(actor));
        }

        @Test
        public void twoToEmpty() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            final Collection<Actor<Integer>> actors = List.of(actorA, actorB);

            final boolean changed = addAll(universe, actors);

            assertTrue(changed);
            assertThat(universe, containsInAnyOrder(actorA, actorB));
        }
    }

    @Nested
    public class Clear {

        @Test
        public void empty() {
            final Universe<Integer> universe = new Universe<>();

            clear(universe);
        }

        @Test
        public void notEmpty() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            universe.add(actorA);
            universe.add(actorB);

            clear(universe);
        }
    }

    @Nested
    public class Remove {

        @Test
        public void nonActor() {
            final Universe<Integer> universe = new Universe<>();

            final boolean removed = remove(universe, new Object());

            assertFalse(removed);
        }

        @Test
        public void absent() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);

            final boolean removed = remove(universe, actor);

            assertFalse(removed);
        }

        @Test
        public void solePresent() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            universe.add(actor);

            final boolean removed = remove(universe, actor);

            assertTrue(removed);
        }

        @Test
        public void present() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            universe.add(actorA);
            universe.add(actorB);

            final boolean removed = remove(universe, actorA);

            assertTrue(removed);
            assertThat(universe, contains(actorB));
        }
    }

    @Nested
    public class RemoveAll {

        @Test
        public void nonActor() {
            final Universe<Integer> universe = new Universe<>();

            final boolean removed = removeAll(universe, List.of(new Object()));

            assertFalse(removed);
        }

        @Test
        public void absent() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);

            final boolean removed = removeAll(universe, List.of(actor));

            assertFalse(removed);
        }

        @Test
        public void solePresent() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            universe.add(actor);

            final boolean removed = removeAll(universe, List.of(actor));

            assertTrue(removed);
        }

        @Test
        public void present() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            universe.add(actorA);
            universe.add(actorB);

            final boolean removed = removeAll(universe, List.of(actorA));

            assertTrue(removed);
            assertThat(universe, contains(actorB));
        }

        @Test
        public void all() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            universe.add(actorA);
            universe.add(actorB);

            final boolean removed = removeAll(universe, List.of(actorA, actorB));

            assertTrue(removed);
            assertThat(universe, empty());
        }
    }

    @Nested
    public class RetainAll {
        @Test
        public void bothEmpty() {
            final Universe<Integer> universe = new Universe<>();

            final boolean changed = retainAll(universe, List.of());

            assertFalse(changed);
            assertThat(universe, empty());
        }

        @Test
        public void notPresent() {
            final Universe<Integer> universe = new Universe<>();
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);

            final boolean changed = retainAll(universe, List.of(actor));

            assertFalse(changed);
            assertThat(universe, empty());
        }

        @Test
        public void none() {
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            final Universe<Integer> universe = new Universe<>();
            universe.add(actor);

            final boolean changed = retainAll(universe, List.of());

            assertTrue(changed);
            assertThat(universe, empty());
        }

        @Test
        public void all() {
            final Actor<Integer> actor = new Actor<>(WHEN_A, 0);
            final Universe<Integer> universe = new Universe<>();
            universe.add(actor);

            final boolean changed = retainAll(universe, List.of(actor));

            assertFalse(changed);
            assertThat(universe, contains(actor));
        }

        @Test
        public void some() {
            final Actor<Integer> actorA = new Actor<>(WHEN_A, 0);
            final Actor<Integer> actorB = new Actor<>(WHEN_B, 1);
            final Universe<Integer> universe = new Universe<>();
            universe.add(actorA);
            universe.add(actorB);

            final boolean changed = retainAll(universe, List.of(actorA));

            assertTrue(changed);
            assertThat(universe, contains(actorA));
        }
    }

}
