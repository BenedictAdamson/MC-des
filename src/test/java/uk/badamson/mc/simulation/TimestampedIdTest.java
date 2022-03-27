package uk.badamson.mc.simulation;
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.badamson.dbc.assertions.ComparableVerifier;
import uk.badamson.dbc.assertions.EqualsSemanticsVerifier;
import uk.badamson.dbc.assertions.ObjectVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * Auxiliary test code for classes implementing the {@link TimestampedId}
 * interface.
 * </p>
 */
public class TimestampedIdTest {

    static final UUID OBJECT_A = UUID.randomUUID();
    static final UUID OBJECT_B = UUID.randomUUID();
    static final Duration DURATION_A = Duration.ZERO;
    static final Duration DURATION_B = Duration.ofSeconds(15);

    public static void assertInvariants(final TimestampedId id) {
        ObjectVerifier.assertInvariants(id);// inherited
        ComparableVerifier.assertInvariants(id);

        final UUID objectId = id.getObject();
        final Duration when = id.getWhen();

        assertAll(() -> assertNotNull(objectId, "objectId"), () -> assertNotNull(when, "when"));
    }

    public static void assertInvariants(final TimestampedId id1, final TimestampedId id2) {
        ObjectVerifier.assertInvariants(id1, id2);// inherited
        ComparableVerifier.assertInvariants(id1, id2);
        ComparableVerifier.assertNaturalOrderingIsConsistentWithEquals(id1, id2);

        final Duration when1 = id1.getWhen();
        final Duration when2 = id2.getWhen();
        final boolean whenEquals = when1.equals(when2);
        final int compareTo = Integer.signum(id1.compareTo(id2));

        assertAll("Value semantics",
                () -> EqualsSemanticsVerifier.assertValueSemantics(id1, id2, "object", TimestampedId::getObject),
                () -> EqualsSemanticsVerifier.assertValueSemantics(id1, id2, "when", TimestampedId::getWhen));
        assertFalse(!whenEquals && compareTo != Integer.signum(when1.compareTo(when2)),
                "The natural ordering orders by time-stamp.");
        assertFalse(whenEquals && compareTo != Integer.signum(id1.getObject().compareTo(id2.getObject())),
                "The natural ordering orders by object IDs if time-stamps are equivalent.");
    }

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
                final TimestampedId id = new TimestampedId(object, when);

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

            @Test
            public void differentObjectA() {
                final TimestampedId id1 = new TimestampedId(OBJECT_A, DURATION_A);
                final TimestampedId id2 = new TimestampedId(OBJECT_B, DURATION_A);

                assertInvariants(id1, id2);
                assertThat("Not equal.", id1, not(id2));
            }

            @Test
            public void differentObjectB() {
                final TimestampedId id1 = new TimestampedId(OBJECT_B, DURATION_A);
                final TimestampedId id2 = new TimestampedId(OBJECT_A, DURATION_A);

                assertInvariants(id1, id2);
                assertThat("Not equal.", id1, not(id2));
            }

            @Test
            public void differentWhenA() {
                final TimestampedId id1 = new TimestampedId(OBJECT_A, DURATION_A);
                final TimestampedId id2 = new TimestampedId(OBJECT_A, DURATION_B);

                assertInvariants(id1, id2);
                assertThat("Not equal.", id1, not(id2));
            }

            @Test
            public void differentWhenB() {
                final TimestampedId id1 = new TimestampedId(OBJECT_B, DURATION_B);
                final TimestampedId id2 = new TimestampedId(OBJECT_B, DURATION_A);

                assertInvariants(id1, id2);
                assertThat("Not equal.", id1, not(id2));
            }

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

                    final TimestampedId id1 = new TimestampedId(object, when);
                    final TimestampedId id2 = new TimestampedId(object2, when2);

                    assertInvariants(id1, id2);
                    assertThat("Equal.", id1, equalTo(id2));
                }

            }// class
        }// class

    }// class

}// class