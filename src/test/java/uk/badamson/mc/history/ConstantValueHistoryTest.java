package uk.badamson.mc.history;
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

import edu.umd.cs.findbugs.annotations.Nullable;
import org.junit.jupiter.api.Test;
import uk.badamson.dbc.assertions.ObjectVerifier;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ConstantValueHistory} class.
 * </p>
 */
public class ConstantValueHistoryTest {

    public static <VALUE> void assertInvariants(final ConstantValueHistory<VALUE> history) {
        ObjectVerifier.assertInvariants(history);// inherited
        ValueHistoryTest.assertInvariants(history);// inherited

        assertAll(
                () -> assertEquals(Collections.emptySortedSet(), history.getTransitionTimes(),
                        "The set of transition times is empty."),
                () -> assertEquals(Collections.emptyMap(), history.getTransitions(), "The transitions map is empty."),
                () -> assertTrue(history.isEmpty(), "A ConstantValueHistory is always empty."),
                () -> assertEquals(0, history.streamOfTransitions().count(), "The stream of transitions is empty."),
                () -> assertNull(history.getFirstTransitionTime(), "The first transition time is null."),
                () -> assertNull(history.getLastTransitionTime(), "The  last transition time is null."));
    }

    public static <VALUE> void assertInvariants(final ConstantValueHistory<VALUE> history1,
                                                final ConstantValueHistory<VALUE> history2) {
        ObjectVerifier.assertInvariants(history1, history2);// inherited
        ValueHistoryTest.assertInvariants(history1, history2);// inherited
    }

    private <VALUE> void constructor(@Nullable final VALUE value) {
        final ConstantValueHistory<VALUE> history = new ConstantValueHistory<>(value);

        assertInvariants(history);
        assertAll(() -> assertSame(value, history.getFirstValue(), "The first value is the given value."),
                () -> assertSame(value, history.getLastValue(), "The last value is the given value."));
    }

    private <VALUE> void constructor_2Equals(@Nullable final VALUE value) {
        final ConstantValueHistory<VALUE> history1 = new ConstantValueHistory<>(value);
        final ConstantValueHistory<VALUE> history2 = new ConstantValueHistory<>(value);

        assertInvariants(history1, history2);
        assertEquals(history1, history2);
    }

    @Test
    public void constructor_2Equals_A() {
        constructor_2Equals(Boolean.TRUE);
    }

    @Test
    public void constructor_2Equals_B() {
        constructor_2Equals(Integer.MAX_VALUE);
    }

    @Test
    public void constructor_2Equals_null() {
        constructor_2Equals((Boolean) null);
    }

    @Test
    public void constructor_A() {
        constructor(Boolean.TRUE);
    }

    @Test
    public void constructor_B() {
        constructor(Integer.MAX_VALUE);
    }

    @Test
    public void constructor_null() {
        constructor((Boolean) null);
    }
}
