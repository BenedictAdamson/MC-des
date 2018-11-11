package uk.badamson.mc.simulation;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import uk.badamson.mc.ComparableTest;
import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Auxiliary test code for classes implementing the {@link ObjectStateId}
 * interface.
 * </p>
 */
public class ObjectStateIdTest {

    static final UUID OBJECT_A = UUID.randomUUID();
    static final UUID OBJECT_B = UUID.randomUUID();
    static final Duration DURATION_A = Duration.ZERO;
    static final Duration DURATION_B = Duration.ofSeconds(15);

    public static void assertInvariants(ObjectStateId id) {
        ObjectTest.assertInvariants(id);// inherited
        ComparableTest.assertInvariants(id);

        final UUID objectId = id.getObject();
        final Duration when = id.getWhen();

        assertAll(() -> assertNotNull(objectId, "objectId"), () -> assertNotNull(when, "when"));
    }

    public static void assertInvariants(ObjectStateId id1, ObjectStateId id2) {
        ObjectTest.assertInvariants(id1, id2);// inherited
        ComparableTest.assertInvariants(id1, id2);
        ComparableTest.assertComparableConsistentWithEquals(id1, id2);

        final Duration when1 = id1.getWhen();
        final Duration when2 = id2.getWhen();
        final boolean whenEquals = when1.equals(when2);

        final boolean equals = id1.equals(id2);
        final int compareTo = Integer.signum(id1.compareTo(id2));

        assertAll("ObjectStateId objects are equivalent only if they have equals",
                () -> assertFalse(equals && !id1.getObject().equals(id2.getObject()), "object IDs"),
                () -> assertFalse(equals && !whenEquals, "timestamps"));
        assertFalse(!whenEquals && compareTo != Integer.signum(when1.compareTo(when2)),
                "The natural ordering orders by time-stamp.");
        assertFalse(whenEquals && compareTo != Integer.signum(id1.getObject().compareTo(id2.getObject())),
                "The natural ordering orders by object IDs if time-stamps are equivalent.");
    }

    private static void constructor(UUID object, Duration when) {
        final ObjectStateId id = new ObjectStateId(object, when);

        assertInvariants(id);
        assertAll(
                () -> assertThat("The object ID of this ID is the given object ID.", id.getObject(),
                        sameInstance(object)),
                () -> assertThat("The time-stamp of this ID is the given time-stamp.", id.getWhen(),
                        sameInstance(when)));
    }

    private static void constructor_2Equal(UUID object, Duration when) {
        // Copy attributes so can test that checks for equality rather than sameness
        final UUID object2 = new UUID(object.getMostSignificantBits(), object.getLeastSignificantBits());
        final Duration when2 = when.plus(Duration.ZERO);

        final ObjectStateId id1 = new ObjectStateId(object, when);
        final ObjectStateId id2 = new ObjectStateId(object2, when2);

        assertInvariants(id1, id2);
        assertThat("Equal.", id1, equalTo(id2));
    }

    @Test
    public void constructor_2DifferentObjectA() {
        final ObjectStateId id1 = new ObjectStateId(OBJECT_A, DURATION_A);
        final ObjectStateId id2 = new ObjectStateId(OBJECT_B, DURATION_A);

        assertInvariants(id1, id2);
        assertThat("Not equal.", id1, not(id2));
    }

    @Test
    public void constructor_2DifferentObjectB() {
        final ObjectStateId id1 = new ObjectStateId(OBJECT_B, DURATION_A);
        final ObjectStateId id2 = new ObjectStateId(OBJECT_A, DURATION_A);

        assertInvariants(id1, id2);
        assertThat("Not equal.", id1, not(id2));
    }

    @Test
    public void constructor_2DifferentWhenA() {
        final ObjectStateId id1 = new ObjectStateId(OBJECT_A, DURATION_A);
        final ObjectStateId id2 = new ObjectStateId(OBJECT_A, DURATION_B);

        assertInvariants(id1, id2);
        assertThat("Not equal.", id1, not(id2));
    }

    @Test
    public void constructor_2DifferentWhenB() {
        final ObjectStateId id1 = new ObjectStateId(OBJECT_B, DURATION_B);
        final ObjectStateId id2 = new ObjectStateId(OBJECT_B, DURATION_A);

        assertInvariants(id1, id2);
        assertThat("Not equal.", id1, not(id2));
    }

    @Test
    public void constructor_2EqualA() {
        constructor_2Equal(OBJECT_A, DURATION_A);
    }

    @Test
    public void constructor_2EqualB() {
        constructor_2Equal(OBJECT_B, DURATION_B);
    }

    @Test
    public void constructor_A() {
        constructor(OBJECT_A, DURATION_A);
    }

    @Test
    public void constructor_B() {
        constructor(OBJECT_B, DURATION_B);
    }

}// class