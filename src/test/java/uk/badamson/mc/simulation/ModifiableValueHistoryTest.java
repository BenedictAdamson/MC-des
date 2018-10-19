package uk.badamson.mc.simulation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ModifiableValueHistory}
 * class.
 * </p>
 */
public class ModifiableValueHistoryTest {

    private static final Duration WHEN_1 = Duration.ZERO;
    private static final Duration WHEN_2 = Duration.ofSeconds(2);
    private static final Duration WHEN_3 = Duration.ofSeconds(3);
    private static final Duration WHEN_4 = Duration.ofSeconds(5);

    private static <VALUE> void appendTransition(ModifiableValueHistory<VALUE> history, Duration when, VALUE value)
            throws IllegalStateException {
        final SortedSet<Duration> transitionTimes0 = new TreeSet<>(history.getTransitionTimes());
        final Map<Duration, VALUE> transitionValues0 = ValueHistoryTest.getTransitionValues(history);

        try {
            history.appendTransition(when, value);
        } catch (final IllegalStateException e) {
            // Permitted
            assertInvariants(history);
            final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
            final Map<Duration, VALUE> transitionValues = ValueHistoryTest.getTransitionValues(history);
            assertAll("This history is unchanged if it throws IllegalStateException.",
                    () -> assertEquals(transitionTimes0, transitionTimes, "transitionTimes"),
                    () -> assertEquals(transitionValues0, transitionValues, "transitionValues"));
            throw e;
        }

        assertInvariants(history);
        final Collection<Duration> transitionTimes = history.getTransitionTimes();
        final Map<Duration, VALUE> transitionValues = ValueHistoryTest.getTransitionValues(history);
        assertAll("Appending a transition",
                () -> assertTrue(transitionTimes.containsAll(transitionTimes0),
                        "Appending a transition does not remove any times from the set of transition times."),
                () -> assertTrue(transitionValues.entrySet().containsAll(transitionValues0.entrySet()),
                        "Appending a transition does not change the values before the given point in time."),
                () -> assertEquals(transitionTimes0.size() + 1, transitionTimes.size(),
                        "Appending a transition increments the number of transition times."));
        assertAll("The given becomes",
                () -> assertSame(history.getLastTansitionTime(), when,
                        "The given point in time becomes the last transition time."),
                () -> assertSame(history.getLastValue(), value, "The given value becomes the last value."));
    }

    private static <VALUE> void appendTransition_1(Duration when, VALUE value) {
        final ModifiableValueHistory<VALUE> history0 = new ModifiableValueHistory<>();
        final ModifiableValueHistory<VALUE> history1 = new ModifiableValueHistory<>();
        history1.appendTransition(when, value);
        final ModifiableValueHistory<VALUE> history2 = new ModifiableValueHistory<>();

        appendTransition(history2, when, value);

        assertAll("Invariants", () -> assertInvariants(history0, history2), () -> assertInvariants(history1, history2));

        final SortedSet<Duration> transitionTimes = history2.getTransitionTimes();
        final Map<Duration, VALUE> transitionValues = ValueHistoryTest.getTransitionValues(history2);
        assertAll("Transitions", () -> assertEquals(Collections.singleton(when), transitionTimes, "transitionTimes."),
                () -> assertEquals(Collections.singletonMap(when, value), transitionValues, "transitionValues."));

        assertAll("Value semantics", () -> assertNotEquals(history0, history2, "before and after"),
                () -> assertEquals(history1, history2, "same changes"));
    }

    private static <VALUE> void appendTransition_2(Duration when1, VALUE value1, Duration when2, VALUE value2) {
        assert when1.compareTo(when2) < 0;
        final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<>();
        history.appendTransition(when1, value1);

        appendTransition(history, when2, value2);

        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        assertEquals(Set.of(when1, when2), transitionTimes, "transitionTimes.");
    }

    private static <VALUE> void appendTransition_2InvalidState(Duration when1, VALUE value1, Duration when2,
            VALUE value2) {
        assert when2.compareTo(when1) <= 0 || Objects.equals(value1, value2);
        final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<>();
        history.appendTransition(when1, value1);

        assertThrows(IllegalStateException.class, () -> appendTransition(history, when2, value2));
    }

    public static <VALUE> void assertInvariants(ModifiableValueHistory<VALUE> history) {
        ObjectTest.assertInvariants(history);// inherited
        ValueHistoryTest.assertInvariants(history);// inherited
    }

    public static <VALUE> void assertInvariants(ModifiableValueHistory<VALUE> history1,
            ModifiableValueHistory<VALUE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited
        ValueHistoryTest.assertInvariants(history1, history2);// inherited
    }

    private static <VALUE> ModifiableValueHistory<VALUE> constructor(ValueHistory<VALUE> that) {
        final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<>(that);

        assertInvariants(history);
        ValueHistoryTest.assertInvariants(history, that);
        assertEquals(that, history, "This equals the given value history.");

        return history;
    }

    private static <VALUE> void constructor_1(VALUE value) {
        final var history1 = new ModifiableValueHistory<>(value);
        final var history2 = new ModifiableValueHistory<>(value);

        assertAll("Invariants", () -> assertInvariants(history1), () -> assertInvariants(history1, history2));

        assertSame(value, history1.getFirstValue(),
                "The value of this history at the start of time is the given value.");
        assertTrue(history1.isEmpty(), "This is empty.");
        assertEquals(history1, history2, "Value semantics");

        ValueHistoryTest.assertInvariants(history1, WHEN_1);
        ValueHistoryTest.assertInvariants(history1, WHEN_2);
    }

    private static <VALUE> void removeStateTransitionsFrom(ModifiableValueHistory<VALUE> history, Duration when) {
        final VALUE firstValue0 = history.getFirstValue();
        final SortedMap<Duration, VALUE> transitions0 = new TreeMap<>(history.getTransitions());

        history.removeStateTransitionsFrom(when);

        assertInvariants(history);
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        final SortedMap<Duration, VALUE> transitions = history.getTransitions();
        final Set<Entry<Duration, VALUE>> transitionsEntries = transitions.entrySet();

        assertSame(firstValue0, history.getFirstValue(), "The first value of the history is unchanged.");
        assertTrue(transitionTimes.isEmpty() || transitionTimes.last().compareTo(when) < 0,
                "The set of state transitions contains no times at or after the given time.");
        for (var entry0 : transitions0.entrySet()) {
            final Duration t = entry0.getKey();
            assertTrue(when.compareTo(t) <= 0 || transitionsEntries.contains(entry0),
                    "Removing state transitions from a given point  in time does not change the transitions before the point in time.");
        }
    }

    private static void removeStateTransitionsFrom_1AfterLast(Duration t1, Duration t2) {
        assert t1.compareTo(t2) < 0;
        final ModifiableValueHistory<Boolean> history = new ModifiableValueHistory<>(Boolean.FALSE);
        history.appendTransition(t1, Boolean.TRUE);
        final ModifiableValueHistory<Boolean> history0 = new ModifiableValueHistory<>(history);

        removeStateTransitionsFrom(history, t2);

        assertEquals(history0, history, "Unchanged");
    }

    private static void removeStateTransitionsFrom_1BeforeOrAtLast(Duration t1, Duration t2) {
        assert t1.compareTo(t2) <= 0;
        final ModifiableValueHistory<Boolean> history = new ModifiableValueHistory<>(Boolean.FALSE);
        history.appendTransition(t2, Boolean.TRUE);
        final ModifiableValueHistory<Boolean> expected = new ModifiableValueHistory<>(Boolean.FALSE);

        removeStateTransitionsFrom(history, t1);

        assertEquals(expected, history, "Trancated");
    }

    private static <VALUE> void setValueFrom(ModifiableValueHistory<VALUE> history, Duration when, VALUE value) {
        final VALUE firstValue0 = history.getFirstValue();

        history.setValueFrom(when, value);

        assertInvariants(history);
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        assertTrue(when.equals(ValueHistory.START_OF_TIME) || Objects.equals(firstValue0, history.getFirstValue()),
                "Setting the value from a given time does not change the values before the given point in time [first value]");
        assertEquals(value, history.get(when), "The given value is equal to the value at the given time.");
        assertTrue(transitionTimes.isEmpty() || transitionTimes.last().compareTo(when) <= 0,
                "If this has any transitions, the last transition time is at or before the given time.");
    }

    private static <VALUE> ModifiableValueHistory<VALUE> setValueFrom_1(VALUE firstValue, Duration when, VALUE value) {
        final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<VALUE>(firstValue);

        setValueFrom(history, when, value);

        return history;
    }

    private static <VALUE> ModifiableValueHistory<VALUE> setValueFrom_2(VALUE firstValue, Duration when1, VALUE value1,
            Duration when2, VALUE value2) {
        final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<VALUE>(firstValue);
        history.setValueFrom(when1, value1);

        setValueFrom(history, when2, value2);

        return history;
    }

    private static <VALUE> void setValueUntil(ModifiableValueHistory<VALUE> history, Duration when, VALUE value) {
        final VALUE lastValue0 = history.getLastValue();

        history.setValueUntil(when, value);

        assertInvariants(history);
        final SortedSet<Duration> transitionTimes = history.getTransitionTimes();
        assertTrue(when.equals(ValueHistory.END_OF_TIME) || Objects.equals(lastValue0, history.getLastValue()),
                "Setting the value until a given time does not change the values after the given point in time [last value]");
        assertEquals(value, history.get(when), "The given value is equal to the value at the given time.");
        assertTrue(transitionTimes.isEmpty() || when.compareTo(transitionTimes.first()) <= 0,
                "If this has any transitions, the first transition time is at or after the given time.");
    }

    private static <VALUE> ModifiableValueHistory<VALUE> setValueUntil_1(VALUE firstValue, Duration when, VALUE value) {
        final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<VALUE>(firstValue);

        setValueUntil(history, when, value);

        return history;
    }

    private static <VALUE> ModifiableValueHistory<VALUE> setValueUntil_2(VALUE firstValue, Duration when1, VALUE value1,
            Duration when2, VALUE value2) {
        final ModifiableValueHistory<VALUE> history = new ModifiableValueHistory<VALUE>(firstValue);
        history.setValueUntil(when1, value1);

        setValueUntil(history, when2, value2);

        return history;
    }

    @Test
    public void appendTransition_1A() {
        appendTransition_1(WHEN_1, Boolean.TRUE);
    }

    @Test
    public void appendTransition_1B() {
        appendTransition_1(WHEN_2, Integer.MAX_VALUE);
    }

    @Test
    public void appendTransition_1InvalidState_valuesNull() {
        final Boolean value = null;
        final ModifiableValueHistory<Boolean> history = new ModifiableValueHistory<>();

        assertThrows(IllegalStateException.class, () -> appendTransition(history, WHEN_1, value));
    }

    @Test
    public void appendTransition_2A() {
        appendTransition_2(WHEN_1, Boolean.FALSE, WHEN_2, Boolean.TRUE);
    }

    @Test
    public void appendTransition_2B() {
        appendTransition_2(WHEN_2, Integer.MIN_VALUE, WHEN_3, Integer.MAX_VALUE);
    }

    @Test
    public void appendTransition_2InvalidState_timesOrder() {
        appendTransition_2InvalidState(WHEN_2, Boolean.FALSE, WHEN_1, Boolean.TRUE);
    }

    @Test
    public void appendTransition_2InvalidState_timesSame() {
        appendTransition_2InvalidState(WHEN_1, Boolean.FALSE, WHEN_1, Boolean.TRUE);
    }

    @Test
    public void appendTransition_2InvalidState_valuesEqual() {
        final String value1 = "Value";
        final String value2 = new String(value1);
        assert value1.equals(value2);
        assert value1 != value2;// tough test

        appendTransition_2InvalidState(WHEN_1, Boolean.FALSE, WHEN_2, Boolean.FALSE);
    }

    @Test
    public void appendTransition_2InvalidState_valuesSame() {
        appendTransition_2InvalidState(WHEN_1, Boolean.FALSE, WHEN_2, Boolean.FALSE);
    }

    @Test
    public void constructor_0() {
        final var history1 = new ModifiableValueHistory<Integer>();
        final var history2 = new ModifiableValueHistory<Integer>();

        assertAll("Invariants", () -> assertInvariants(history1), () -> assertInvariants(history1, history2));

        assertNull(history1.getFirstValue(), "The value of this history at the start of time is null.");
        assertTrue(history1.isEmpty(), "This is empty.");
        assertEquals(history1, history2, "Value semantics");

        ValueHistoryTest.assertInvariants(history1, WHEN_1);
        ValueHistoryTest.assertInvariants(history1, WHEN_2);
    }

    @Test
    public void constructor_1_null() {
        constructor_1((Boolean) null);
    }

    @Test
    public void constructor_1A() {
        constructor_1(Boolean.FALSE);
    }

    @Test
    public void constructor_1B() {
        constructor_1(Integer.MIN_VALUE);
    }

    @Test
    public void constructor_copy_0() {
        final ValueHistory<Boolean> that = new ModifiableValueHistory<Boolean>();

        constructor(that);
    }

    @Test
    public void constructor_copy_1() {
        final ValueHistory<Boolean> that = new ModifiableValueHistory<Boolean>(Boolean.FALSE);

        constructor(that);
    }

    @Test
    public void constructor_copy_2() {
        final ModifiableValueHistory<Integer> that = new ModifiableValueHistory<Integer>(0);
        that.appendTransition(WHEN_1, 1);

        constructor(that);
    }

    @Test
    public void removeStateTransitionsFrom_0NonNull() {
        final ModifiableValueHistory<Boolean> history = new ModifiableValueHistory<>(Boolean.FALSE);

        removeStateTransitionsFrom(history, WHEN_1);
    }

    @Test
    public void removeStateTransitionsFrom_0Null() {
        final ModifiableValueHistory<Boolean> history = new ModifiableValueHistory<>();

        removeStateTransitionsFrom(history, WHEN_1);
    }

    @Test
    public void removeStateTransitionsFrom_1AfterLastA() {
        removeStateTransitionsFrom_1AfterLast(WHEN_1, WHEN_2);
    }

    @Test
    public void removeStateTransitionsFrom_1AfterLastB() {
        removeStateTransitionsFrom_1AfterLast(WHEN_2, WHEN_3);
    }

    @Test
    public void removeStateTransitionsFrom_1AtLastA() {
        final Duration when = WHEN_1;
        removeStateTransitionsFrom_1BeforeOrAtLast(when, when);
    }

    @Test
    public void removeStateTransitionsFrom_1BeforeLastA() {
        removeStateTransitionsFrom_1BeforeOrAtLast(WHEN_1, WHEN_2);
    }

    @Test
    public void removeStateTransitionsFrom_1BeforeLastB() {
        removeStateTransitionsFrom_1BeforeOrAtLast(WHEN_2, WHEN_3);
    }

    @Test
    public void removeStateTransitionsFrom_2BeforeLastA() {
        final ModifiableValueHistory<Integer> history = new ModifiableValueHistory<>(1);
        history.appendTransition(WHEN_2, 2);
        history.appendTransition(WHEN_4, 3);

        removeStateTransitionsFrom(history, WHEN_3);

    }

    @Test
    public void setValueFrom_1_endOfTime() {
        setValueFrom_1(Boolean.FALSE, ValueHistory.END_OF_TIME, Boolean.TRUE);
    }

    @Test
    public void setValueFrom_1_noOp() {
        final Boolean value = Boolean.FALSE;
        setValueFrom_1(value, WHEN_1, value);
    }

    @Test
    public void setValueFrom_1_noOpNull() {
        final Boolean value = null;
        setValueFrom_1(value, WHEN_1, value);
    }

    @Test
    public void setValueFrom_1_null() {
        setValueFrom_1(Boolean.FALSE, WHEN_1, (Boolean) null);
    }

    @Test
    public void setValueFrom_1_startOfTime() {
        setValueFrom_1(Boolean.FALSE, ValueHistory.START_OF_TIME, Boolean.TRUE);
    }

    @Test
    public void setValueFrom_1A() {
        setValueFrom_1(Boolean.FALSE, WHEN_1, Boolean.TRUE);
    }

    @Test
    public void setValueFrom_1B() {
        setValueFrom_1(Integer.MIN_VALUE, WHEN_2, Integer.MAX_VALUE);
    }

    @Test
    public void setValueFrom_2_append_A() {
        setValueFrom_2(Integer.valueOf(1), WHEN_1, Integer.valueOf(2), WHEN_2, Integer.valueOf(3));
    }

    @Test
    public void setValueFrom_2_append_B() {
        setValueFrom_2(Integer.valueOf(5), WHEN_2, Integer.valueOf(7), WHEN_3, Integer.valueOf(11));
    }

    @Test
    public void setValueFrom_2_before() {
        setValueFrom_2(Integer.valueOf(1), WHEN_2, Integer.valueOf(2), WHEN_1, Integer.valueOf(3));
    }

    @Test
    public void setValueFrom_2_replace() {
        final Duration when = WHEN_1;
        setValueFrom_2(Integer.valueOf(1), when, Integer.valueOf(2), when, Integer.valueOf(3));
    }

    @Test
    public void setValueUntil_1_endOfTime() {
        setValueUntil_1(Boolean.FALSE, ValueHistory.END_OF_TIME, Boolean.TRUE);
    }

    @Test
    public void setValueUntil_1_noOp() {
        final Boolean value = Boolean.FALSE;
        setValueUntil_1(value, WHEN_1, value);
    }

    @Test
    public void setValueUntil_1_noOpNull() {
        final Boolean value = null;
        setValueUntil_1(value, WHEN_1, value);
    }

    @Test
    public void setValueUntil_1_null() {
        setValueUntil_1(Boolean.FALSE, WHEN_1, (Boolean) null);
    }

    @Test
    public void setValueUntil_1_startOfTime() {
        setValueUntil_1(Boolean.FALSE, ValueHistory.START_OF_TIME, Boolean.TRUE);
    }

    @Test
    public void setValueUntil_1A() {
        setValueUntil_1(Boolean.FALSE, WHEN_1, Boolean.TRUE);
    }

    @Test
    public void setValueUntil_1B() {
        setValueUntil_1(Integer.MIN_VALUE, WHEN_2, Integer.MAX_VALUE);
    }

    @Test
    public void setValueUntil_2_after() {
        setValueUntil_2(Integer.valueOf(1), WHEN_1, Integer.valueOf(2), WHEN_2, Integer.valueOf(3));
    }

    @Test
    public void setValueUntil_2_prepend_A() {
        setValueUntil_2(Integer.valueOf(1), WHEN_2, Integer.valueOf(2), WHEN_1, Integer.valueOf(3));
    }

    @Test
    public void setValueUntil_2_prepend_B() {
        setValueUntil_2(Integer.valueOf(5), WHEN_3, Integer.valueOf(7), WHEN_2, Integer.valueOf(11));
    }

    @Test
    public void setValueUntil_2_replace() {
        final Duration when = WHEN_1;
        setValueUntil_2(Integer.valueOf(1), when, Integer.valueOf(2), when, Integer.valueOf(3));
    }

}
