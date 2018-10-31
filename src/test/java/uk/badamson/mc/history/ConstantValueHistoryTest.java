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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.Nullable;
import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit test and auxiliary test code for the {@link ConstantValueHistory} class.
 * </p>
 */
public class ConstantValueHistoryTest {

    public static <VALUE> void assertInvariants(ConstantValueHistory<VALUE> history) {
        ObjectTest.assertInvariants(history);// inherited
        ValueHistoryTest.assertInvariants(history);// inherited

        assertAll(
                () -> assertEquals(Collections.emptySortedSet(), history.getTransitionTimes(),
                        "The set of transition times is empty."),
                () -> assertEquals(Collections.emptyMap(), history.getTransitions(), "The transitions map is empty."),
                () -> assertTrue(history.isEmpty(), "A ConstantValueHistoryis always empty."),
                () -> assertEquals(0, history.streamOfTransitions().count(), "The stream of transitions is empty."));
    }

    public static <VALUE> void assertInvariants(ConstantValueHistory<VALUE> history1,
            ConstantValueHistory<VALUE> history2) {
        ObjectTest.assertInvariants(history1, history2);// inherited
        ValueHistoryTest.assertInvariants(history1, history2);// inherited
    }

    public <VALUE> void constructor(@Nullable VALUE value) {
        final ConstantValueHistory<VALUE> history = new ConstantValueHistory<>(value);

        assertInvariants(history);
    }

    @Test
    public void constructor_null() {
        constructor((Boolean) null);
    }
}
