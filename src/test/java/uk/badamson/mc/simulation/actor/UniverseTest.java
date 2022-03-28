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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.badamson.dbc.assertions.CollectionVerifier;
import uk.badamson.dbc.assertions.EqualsSemanticsVerifier;
import uk.badamson.dbc.assertions.ObjectVerifier;
import uk.badamson.mc.history.ValueHistory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class UniverseTest {

    static final Duration WHEN_A = ActorTest.WHEN_A;
    static final Duration WHEN_B = ActorTest.WHEN_B;
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private static <STATE> void assertEmpty(@Nonnull final Universe<STATE> universe) {
        assertThat("actors", universe.getActors(), empty());
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe) {
        ObjectVerifier.assertInvariants(universe);// inherited

        final Collection<Actor<STATE>> actors = universe.getActors();

        assertNotNull(actors, "actors");// guard
        CollectionVerifier.assertForAllElements("actors", actors, actor -> {
            assertThat(actor, notNullValue());// guard
            ActorTest.assertInvariants(actor);
        });
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe1,
                                                @Nonnull final Universe<STATE> universe2) {
        ObjectVerifier.assertInvariants(universe1, universe2);// inherited
    }

    private static <STATE> void constructor(@Nonnull final Collection<Actor<STATE>> actors) {
        final var universe = new Universe<>(actors);

        assertInvariants(universe);
        assertThat("copied actors", new HashSet<>(universe.getActors()),
                is(new HashSet<>(actors)));
    }

    @Nonnull
    private static <STATE> Universe<STATE>.SchedulingMedium createMedium(@Nonnull final Universe<STATE> universe,
                                                                         @Nonnull final Executor executor, @Nonnull final Duration advanceTo) {
        final var medium = universe.createMedium(executor, advanceTo);

        assertInvariants(universe);
        assertThat("result", medium, notNullValue());// guard
        SchedulingMediumTest.assertInvariants(medium);

        return medium;
    }

    private static class DeferringExecutor implements Executor {

        @Override
        public void execute(@Nonnull final Runnable command) {
            // Do nothing
        }

    }// class

    public static class SchedulingMediumTest {

        static <STATE> void assertInvariants(@Nonnull final Universe<STATE>.SchedulingMedium medium) {
            ObjectVerifier.assertInvariants(medium);// inherited
            MediumTest.assertInvariants(medium);// inherited

            CollectionVerifier.assertForAllElements(medium.getUniverse().getActors(), actor -> {
                assertThat("actor", actor, notNullValue());// guard
            });
        }

        static <STATE> void removeAll(@Nonnull final Universe<STATE>.SchedulingMedium medium,
                                      @Nonnull final Collection<Signal<STATE>> signals) {
            MediumTest.removeAll(medium, signals);// inherited

            assertInvariants(medium);
        }
    }// class

    @Nested
    public class Constructor {

        @Test
        public void noArgs() {
            final var universe = new Universe<>();

            assertInvariants(universe);
            assertEmpty(universe);
        }

        @Nested
        public class FromObjectHistories {

            @Test
            public void empty() {
                constructor(List.of());
            }

            @Test
            public void two() {
                final var actorA = new Actor<>(WHEN_A, 0);
                final var actorB = new Actor<>(WHEN_B, 1);
                final Collection<Actor<Integer>> objectHistories = List.of(actorA, actorB);

                constructor(objectHistories);
            }

            @Nested
            public class One {

                @Test
                public void a() {
                    test(WHEN_A, 0);
                }

                @Test
                public void b() {
                    test(WHEN_B, 1);
                }

                private void test(@Nonnull final Duration start,
                                  @Nonnull final Integer state) {
                    final var actor = new Actor<>(start, state);
                    final Collection<Actor<Integer>> actors = List.of(actor);

                    constructor(actors);
                }
            }// class
        }// class

    }// class

    @Nested
    public class CreateMedium {

        @Nested
        public class NoObjects {

            @Test
            public void toEndOfTime() {
                test(DIRECT_EXECUTOR, ValueHistory.END_OF_TIME);
            }

            @Test
            public void a() {
                test(new DeferringExecutor(), WHEN_A);
            }

            private void test(final Executor executor, final Duration advanceTo) {
                final var universe = new Universe<Integer>();

                final var medium = createMedium(universe, executor, advanceTo);

                assertThat("signals", medium.getSignals(), empty());
            }

        }// class


    }// class
}
