package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ModifiableSetHistory} class.
 * </p>
 */
public class ModifiableSetHistoryTest {

    private static final Duration WHEN_1 = Duration.ZERO;
    private static final Duration WHEN_2 = Duration.ofSeconds(2);
    private static final Duration WHEN_3 = Duration.ofSeconds(3);

    public static <VALUE> Set<VALUE> assertFirstValueInvariants(ModifiableSetHistory<VALUE> history) {
        final Set<VALUE> firstValue = history.getFirstValue();

        assertEquals("The first value is an empty set.", Collections.EMPTY_SET, firstValue);

        return firstValue;
    }

    public static <VALUE> void assertInvariants(ModifiableSetHistory<VALUE> history) {
        ObjectTest.assertInvariants(history);// inherited
        ValueHistoryTest.assertInvariants(history);// inherited

        assertFirstValueInvariants(history);
    }

    public static <VALUE> void assertInvariants(ModifiableSetHistory<VALUE> history1,
            ModifiableSetHistory<VALUE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited
        ValueHistoryTest.assertInvariants(history1, history2);// inherited
    }

    @Test
    public void constructor_0() {
        final var history = new ModifiableSetHistory<Integer>();

        assertInvariants(history);
        ValueHistoryTest.assertInvariants(history, WHEN_1);
        ValueHistoryTest.assertInvariants(history, WHEN_2);
        assertEquals("This has no transition times.", Collections.EMPTY_SET, history.getTransitionTimes());
    }
}
