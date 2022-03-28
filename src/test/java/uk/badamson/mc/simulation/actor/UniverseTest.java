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

    static final Duration WHEN_A = ObjectHistoryTest.WHEN_A;
    static final Duration WHEN_B = ObjectHistoryTest.WHEN_B;
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private static <STATE> void assertEmpty(@Nonnull final Universe<STATE> universe) {
        assertThat("objectHistories", universe.getObjectHistories(), empty());
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe) {
        ObjectVerifier.assertInvariants(universe);// inherited

        final Collection<ObjectHistory<STATE>> objectHistories = universe.getObjectHistories();

        assertNotNull(objectHistories, "objectHistories");// guard
        CollectionVerifier.assertForAllElements("objectHistories", objectHistories, history -> {
            assertThat(history, notNullValue());// guard
            ObjectHistoryTest.assertInvariants(history);
        });
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe1,
                                                @Nonnull final Universe<STATE> universe2) {
        ObjectVerifier.assertInvariants(universe1, universe2);// inherited

        assertAll("Value semantics",
                () -> EqualsSemanticsVerifier.assertValueSemantics(universe1, universe2, "objectHistories",
                        Universe::getObjectHistories),
                () -> assertEquals(universe1.equals(universe2), universe1.getObjectHistories()
                        .equals(universe2.getObjectHistories()), "equals"));
    }

    private static <STATE> void constructor(@Nonnull final Collection<ObjectHistory<STATE>> objectHistories) {
        final var universe = new Universe<>(objectHistories);

        assertInvariants(universe);
        assertThat("copied objectHistories", new HashSet<>(universe.getObjectHistories()),
                is(new HashSet<>(objectHistories)));
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

            CollectionVerifier.assertForAllElements(medium.getUniverse().getObjectHistories(), history -> {
                assertThat("history", history, notNullValue());// guard
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
                final var objectHistoryA = new ObjectHistory<>(WHEN_A, 0);
                final var objectHistoryB = new ObjectHistory<>(WHEN_B, 1);
                final Collection<ObjectHistory<Integer>> objectHistories = List.of(objectHistoryA, objectHistoryB);

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
                    final var objectHistory = new ObjectHistory<>(start, state);
                    final Collection<ObjectHistory<Integer>> objectHistories = List.of(objectHistory);

                    constructor(objectHistories);
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
