package uk.badamson.mc.simulation;
/*
 * © Copyright Benedict Adamson 2018,2021.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.badamson.dbc.assertions.ComparableTest;
import uk.badamson.dbc.assertions.EqualsSemanticsTest;
import uk.badamson.dbc.assertions.ObjectTest;
import uk.badamson.mc.JsonTest;

/**
 * <p>
 * Auxiliary test code for classes implementing the {@link ObjectStateId}
 * interface.
 * </p>
 */
public class ObjectStateIdTest {

    @Nested
    public class Constructor {

        @Nested
        public class One {

            @Test
            public void a() {
                test(OBJECT_A, DURATION_A);
            }

            @Test
            public void b() {
                test(OBJECT_B, DURATION_B);
            }

            private void test(final UUID object, final Duration when) {
                final ObjectStateId id = new ObjectStateId(object, when);

                assertInvariants(id);
                assertAll(
                        () -> assertThat("The object ID of this ID is the given object ID.", id.getObject(),
                                sameInstance(object)),
                        () -> assertThat("The time-stamp of this ID is the given time-stamp.", id.getWhen(),
                                sameInstance(when)));
            }
        }// class

        @Nested
        public class Two {

            @Nested
            public class Equal {

                @Test
                public void a() {
                    test(OBJECT_A, DURATION_A);
                }

                @Test
                public void b() {
                    test(OBJECT_B, DURATION_B);
                }

                private void test(final UUID object, final Duration when) {
                    // Copy attributes so can test that checks for equality rather than sameness
                    final UUID object2 = new UUID(object.getMostSignificantBits(), object.getLeastSignificantBits());
                    final Duration when2 = when.plus(Duration.ZERO);

                    final ObjectStateId id1 = new ObjectStateId(object, when);
                    final ObjectStateId id2 = new ObjectStateId(object2, when2);

                    assertInvariants(id1, id2);
                    assertThat("Equal.", id1, equalTo(id2));
                }

            }// class

            @Test
            public void differentObjectA() {
                final ObjectStateId id1 = new ObjectStateId(OBJECT_A, DURATION_A);
                final ObjectStateId id2 = new ObjectStateId(OBJECT_B, DURATION_A);

                assertInvariants(id1, id2);
                assertThat("Not equal.", id1, not(id2));
            }

            @Test
            public void differentObjectB() {
                final ObjectStateId id1 = new ObjectStateId(OBJECT_B, DURATION_A);
                final ObjectStateId id2 = new ObjectStateId(OBJECT_A, DURATION_A);

                assertInvariants(id1, id2);
                assertThat("Not equal.", id1, not(id2));
            }

            @Test
            public void differentWhenA() {
                final ObjectStateId id1 = new ObjectStateId(OBJECT_A, DURATION_A);
                final ObjectStateId id2 = new ObjectStateId(OBJECT_A, DURATION_B);

                assertInvariants(id1, id2);
                assertThat("Not equal.", id1, not(id2));
            }

            @Test
            public void differentWhenB() {
                final ObjectStateId id1 = new ObjectStateId(OBJECT_B, DURATION_B);
                final ObjectStateId id2 = new ObjectStateId(OBJECT_B, DURATION_A);

                assertInvariants(id1, id2);
                assertThat("Not equal.", id1, not(id2));
            }
        }// class

    }// class

    @Nested
    public class JSON {

        @Test
        public void a() {
            test(OBJECT_A, DURATION_A);
        }

        @Test
        public void b() {
            test(OBJECT_B, DURATION_B);
        }

        private <VALUE> void test(@Nonnull final ObjectStateId id) {
            final var deserialized = JsonTest.serializeAndDeserialize(id);

            assertInvariants(id);
            assertInvariants(id, deserialized);
            assertEquals(id, deserialized);
        }

        private void test(@Nonnull final UUID object, @Nonnull final Duration when) {
            final ObjectStateId id = new ObjectStateId(object, when);
            test(id);
        }

    }// class

    static final UUID OBJECT_A = UUID.randomUUID();
    static final UUID OBJECT_B = UUID.randomUUID();

    static final Duration DURATION_A = Duration.ZERO;
    static final Duration DURATION_B = Duration.ofSeconds(15);

    public static void assertInvariants(final ObjectStateId id) {
        ObjectTest.assertInvariants(id);// inherited
        ComparableTest.assertInvariants(id);

        final UUID objectId = id.getObject();
        final Duration when = id.getWhen();

        assertAll(() -> assertNotNull(objectId, "objectId"), () -> assertNotNull(when, "when"));
    }

    public static void assertInvariants(final ObjectStateId id1, final ObjectStateId id2) {
        ObjectTest.assertInvariants(id1, id2);// inherited
        ComparableTest.assertInvariants(id1, id2);
        ComparableTest.assertNaturalOrderingIsConsistentWithEquals(id1, id2);

        final Duration when1 = id1.getWhen();
        final Duration when2 = id2.getWhen();
        final boolean whenEquals = when1.equals(when2);
        final int compareTo = Integer.signum(id1.compareTo(id2));

        assertAll("Value semantics",
                () -> EqualsSemanticsTest.assertValueSemantics(id1, id2, "object", id -> id.getObject()),
                () -> EqualsSemanticsTest.assertValueSemantics(id1, id2, "when", id -> id.getWhen()));
        assertFalse(!whenEquals && compareTo != Integer.signum(when1.compareTo(when2)),
                "The natural ordering orders by time-stamp.");
        assertFalse(whenEquals && compareTo != Integer.signum(id1.getObject().compareTo(id2.getObject())),
                "The natural ordering orders by object IDs if time-stamps are equivalent.");
    }

}// class