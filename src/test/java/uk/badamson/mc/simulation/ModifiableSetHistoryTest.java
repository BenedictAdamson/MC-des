package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
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

    private static <VALUE> Set<VALUE> assertFirstValueInvariants(ModifiableSetHistory<VALUE> history) {
        final Set<VALUE> firstValue = history.getFirstValue();

        assertEquals("The first value is an empty set.", Collections.EMPTY_SET, firstValue);

        return firstValue;
    }

    public static <VALUE> void assertInvariants(ModifiableSetHistory<VALUE> history) {
        ObjectTest.assertInvariants(history);// inherited
        SetHistoryTest.assertInvariants(history);// inherited

        assertFirstValueInvariants(history);
    }

    public static <VALUE> void assertInvariants(ModifiableSetHistory<VALUE> history1,
            ModifiableSetHistory<VALUE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited
        SetHistoryTest.assertInvariants(history1, history2);// inherited
    }

    public static <VALUE> void assertInvariants(ModifiableSetHistory<VALUE> history, VALUE value) {
        SetHistoryTest.assertInvariants(history, value);
    }

    private static <VALUE> void setPresentFrom(ModifiableSetHistory<VALUE> history, Duration when, VALUE value) {
        history.setPresentFrom(when, value);

        assertInvariants(history);
        assertInvariants(history, value);
        final ValueHistory<Boolean> contains = history.contains(value);
        assertSame("The last value of the contains history for the given value is TRUE.", Boolean.TRUE,
                contains.getLastValue());
        assertSame("The value at the given time of the contains history for the given value is TRUE.", Boolean.TRUE,
                contains.get(when));
        assertTrue(
                "The contains history for the given value has its last transition time is at or before the given time.",
                contains.getLastTansitionTime().compareTo(when) <= 0);
    }

    private static <VALUE> void setPresentFrom_1(Duration when, VALUE value) {
        final Map<Duration, Set<VALUE>> expectedTransitions = Collections.singletonMap(when,
                Collections.singleton(value));
        final Map<Duration, Boolean> expectedContainsTransitions = Collections.singletonMap(when, Boolean.TRUE);
        final ModifiableSetHistory<VALUE> history0 = new ModifiableSetHistory<>();
        final ModifiableSetHistory<VALUE> history1 = new ModifiableSetHistory<>();
        final ModifiableSetHistory<VALUE> history2 = new ModifiableSetHistory<>();
        history2.setPresentFrom(when, value);

        setPresentFrom(history1, when, value);

        assertInvariants(history0, history1);
        assertInvariants(history1, history2);

        assertEquals("Set at start of time", Collections.EMPTY_SET, history1.getFirstValue());
        assertEquals("Transitions", expectedTransitions, ValueHistoryTest.getTransitionValues(history1));
        assertEquals("contains transisions", expectedContainsTransitions,
                ValueHistoryTest.getTransitionValues(history1.contains(value)));

        assertNotEquals("Value semantics", history0, history1);
        assertEquals("Value semantics", history1, history2);
    }

    private static <VALUE> void setPresentFrom_2_differentValues(Duration when1, VALUE value1, Duration when2,
            VALUE value2) {
        assert when1.compareTo(when2) < 0;
        final Set<Duration> expectedTransitionTimes = Set.of(when1, when2);
        final Map<Duration, Set<VALUE>> expectedTransitions = Map.of(when1, Collections.singleton(value1), when2,
                Set.of(value1, value2));
        final Map<Duration, Boolean> expectedContainsTransitions1 = Collections.singletonMap(when1, Boolean.TRUE);
        final Map<Duration, Boolean> expectedContainsTransitions2 = Collections.singletonMap(when2, Boolean.TRUE);

        final ModifiableSetHistory<VALUE> history = new ModifiableSetHistory<>();
        history.setPresentFrom(when1, value1);

        setPresentFrom(history, when2, value2);

        assertEquals("Set at start of time", Collections.EMPTY_SET, history.getFirstValue());
        assertEquals("transitionTimes", expectedTransitionTimes, history.getTransitionTimes());
        assertEquals("Transitions", expectedTransitions, ValueHistoryTest.getTransitionValues(history));
        assertEquals("contains transisions [1]", expectedContainsTransitions1,
                ValueHistoryTest.getTransitionValues(history.contains(value1)));
        assertEquals("contains transisions [2]", expectedContainsTransitions2,
                ValueHistoryTest.getTransitionValues(history.contains(value2)));
    }

    private static <VALUE> void setPresentFrom_2_sameValue(Duration when1, Duration when2, VALUE value) {
        final Duration whenEarliest = when1.compareTo(when2) <= 0 ? when1 : when2;
        final Map<Duration, Set<VALUE>> expectedTransitions = Collections.singletonMap(whenEarliest,
                Collections.singleton(value));
        final Map<Duration, Boolean> expectedContainsTransitions = Collections.singletonMap(whenEarliest, Boolean.TRUE);

        final ModifiableSetHistory<VALUE> history = new ModifiableSetHistory<>();
        history.setPresentFrom(when1, value);

        setPresentFrom(history, when2, value);

        assertEquals("Set at start of time", Collections.EMPTY_SET, history.getFirstValue());
        assertEquals("Transitions", expectedTransitions, ValueHistoryTest.getTransitionValues(history));
        assertEquals("contains transisions", expectedContainsTransitions,
                ValueHistoryTest.getTransitionValues(history.contains(value)));
    }

    @Test
    public void constructor_0() {
        final var history1 = new ModifiableSetHistory<Integer>();
        final var history2 = new ModifiableSetHistory<Integer>();

        assertInvariants(history1);
        assertInvariants(history1, history2);

        assertEquals("This has no transition times.", Collections.EMPTY_SET, history1.getTransitionTimes());
        assertEquals("Value semantics", history1, history2);

        ValueHistoryTest.assertInvariants(history1, WHEN_1);
        ValueHistoryTest.assertInvariants(history1, WHEN_2);
        assertInvariants(history1, (Integer) null);
        assertInvariants(history1, Integer.MIN_VALUE);
    }

    @Test
    public void setPresentFrom_1A() {
        setPresentFrom_1(WHEN_1, Integer.MIN_VALUE);
    }

    @Test
    public void setPresentFrom_1B() {
        setPresentFrom_1(WHEN_2, "value");
    }

    @Test
    public void setPresentFrom_2_differentValues_A() {
        setPresentFrom_2_differentValues(WHEN_1, Integer.valueOf(1), WHEN_2, Integer.valueOf(2));
    }

    @Test
    public void setPresentFrom_2_differentValues_B() {
        setPresentFrom_2_differentValues(WHEN_1, Integer.valueOf(2), WHEN_2, Integer.valueOf(1));
    }

    @Test
    public void setPresentFrom_2_differentValues_C() {
        setPresentFrom_2_differentValues(WHEN_2, "value 1", WHEN_3, "value 2");
    }

    @Test
    public void setPresentFrom_2_sameValue_A() {
        setPresentFrom_2_sameValue(WHEN_1, WHEN_2, Integer.MIN_VALUE);
    }

    @Test
    public void setPresentFrom_2_sameValue_B() {
        setPresentFrom_2_sameValue(WHEN_2, WHEN_3, Integer.MAX_VALUE);
    }

    @Test
    public void setPresentFrom_2_sameValue_C() {
        setPresentFrom_2_sameValue(WHEN_2, WHEN_1, Integer.MIN_VALUE);
    }

    @Test
    public void setPresentFrom_2_sameValue_sameTime() {
        final Duration when = WHEN_1;
        setPresentFrom_2_sameValue(when, when, Integer.MIN_VALUE);
    }
}
