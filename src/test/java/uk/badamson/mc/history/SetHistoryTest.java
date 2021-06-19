package uk.badamson.mc.history;
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

import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link SetHistory} interface.
 * </p>
 */
public class SetHistoryTest {

    private static <VALUE> void assertContainsInvariants(final SetHistory<VALUE> history,
                                                         final VALUE value) {
        final ValueHistory<Boolean> contains = history.contains(value);

        assertNotNull(contains, "Always have a containment history for a value.");// guard
        final var transitionTimes = contains.getTransitionTimes();
        final Set<Boolean> containsValues = transitionTimes.stream().map(contains::get)
                .collect(Collectors.toSet());
        assertThat("The containment history for a value has a value for all points in time [at transition times].",
                containsValues, not(hasItem(nullValue())));
        assertThat(
                "The transition times of the containment history of a value is a sub set of the transition times of this history.",
                transitionTimes, everyItem(in(history.getTransitionTimes())));
        assertUniverseInvariants(history);
        for (final var t : transitionTimes) {
            assertEquals(history.get(t).contains(value), contains.get(t), "The containment history for a value indicates that the value is present for a point in time if, and only if, that value is contained in the set for which this is a history at that point in time.");
        }

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
        assertNotNull(universe, "Always have a (non null) universe."); // guard
        assertTrue(universe.containsAll(history.getFirstValue()),
                "The value of this time varying set at the start of time is a non-strict sub set of the universe.");
        assertTrue(universe.containsAll(history.getLastValue()),
                "The value of this time varying set at the end of time is a non-strict sub set of the universe.");
    }
}
