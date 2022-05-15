package uk.badamson.mc.history;
/*
 * © Copyright Benedict Adamson 2018,22.
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

import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsIn.in;
import static org.junit.jupiter.api.Assertions.assertAll;

public class SetHistoryTest {

    private static <VALUE> void assertContainsInvariants(final SetHistory<VALUE> history,
                                                         final VALUE value) {
        final ValueHistory<Boolean> contains = history.contains(value);

        assertThat(contains, notNullValue());
        final var transitionTimes = contains.getTransitionTimes();
        final Set<Boolean> containsValues = transitionTimes.stream().map(contains::get)
                .collect(Collectors.toSet());
        assertThat("The containment history for a value has a value for all points in time [at transition times].",
                containsValues, not(hasItem(nullValue())));
        assertThat(
                "The transition times of the containment history of a value is a sub set of the transition times of this history.",
                transitionTimes, everyItem(in(history.getTransitionTimes())));
        assertUniverseInvariants(history);
        assertAll(
                transitionTimes.stream().map(t -> () ->
                        assertThat("get(t).contains(value) [" + t + "]", history.get(t).contains(value), is(contains.get(t)))
                ));
    }

    public static <VALUE> void assertInvariants(final SetHistory<VALUE> history) {
        ValueHistoryTest.assertInvariants(history);// inherited
    }

    public static <VALUE> void assertInvariants(final SetHistory<VALUE> history1, final SetHistory<VALUE> history2) {
        ValueHistoryTest.assertInvariants(history1, history2);// inherited
    }

    public static <VALUE> void assertInvariants(final SetHistory<VALUE> history, final VALUE value) {
        assertContainsInvariants(history, value);
    }

    private static <VALUE> void assertUniverseInvariants(final SetHistory<VALUE> history) {
        final Set<VALUE> universe = history.getUniverse();
        assertThat("universe", universe, notNullValue());
        assertAll("universe is a super set of",
                () -> assertThat("firstValue", universe.containsAll(history.getFirstValue()), is(true)),
                () -> assertThat("lastValue", universe.containsAll(history.getLastValue()), is(true)));
    }
}
