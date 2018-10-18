package uk.badamson.mc.simulation;

import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link SetHistory} interface.
 * </p>
 */
public class SetHistoryTest {

    private static <VALUE> ValueHistory<Boolean> assertContainsInvariants(SetHistory<VALUE> history, VALUE value) {
        final ValueHistory<Boolean> contains = history.contains(value);

        assertNotNull("Always have a containment history for a value.", contains);// guard
        final var transitionTimes = contains.getTransitionTimes();
        final Set<Boolean> containsValues = transitionTimes.stream().map(t -> contains.get(t))
                .collect(Collectors.toSet());
        assertThat("The containment history for a value has a value for all points in time [at transition times].",
                containsValues, not(hasItem(nullValue())));
        assertThat(
                "The transition times of the containment history of a value is a sub set of the transition times of this history.",
                transitionTimes, everyItem(isIn(history.getTransitionTimes())));
        for (var t : transitionTimes) {
            assertEquals(
                    "The containment history for a value indicates that the value is present for a point in time if, and only if, that value is contained in the set for which this is a history at that point in time.",
                    history.get(t).contains(value), contains.get(t).booleanValue());
        }

        return contains;
    }

    public static <VALUE> void assertInvariants(SetHistory<VALUE> history) {
        ValueHistoryTest.assertInvariants(history);
    }

    public static <VALUE> void assertInvariants(SetHistory<VALUE> history, Duration time) {
        ValueHistoryTest.assertInvariants(history, time);
    }

    public static <VALUE> void assertInvariants(SetHistory<VALUE> history1, SetHistory<VALUE> history2) {
        ValueHistoryTest.assertInvariants(history1, history2);
    }

    public static <VALUE> void assertInvariants(SetHistory<VALUE> history, VALUE value) {
        assertContainsInvariants(history, value);
    }
}
