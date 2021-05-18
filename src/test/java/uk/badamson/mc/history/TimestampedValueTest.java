package uk.badamson.mc.history;
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.dbc.assertions.EqualsSemanticsTest;
import uk.badamson.dbc.assertions.ObjectTest;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class TimestampedValueTest {

    @Nested
    public class Constructor {

        @Nested
        public class Two {

            @Test
            public void differentEnd() {
                final Duration start = Duration.ofMillis(1000);
                final Duration endA = Duration.ofMillis(2000);
                final Duration endB = Duration.ofMillis(3000);
                final String value = "State";

                final var timestampedA = new TimestampedValue<>(start, endA, value);
                final var timestampedB = new TimestampedValue<>(start, endB, value);

                assertInvariants(timestampedA, timestampedB);
                assertNotEquals(timestampedA, timestampedB);
            }

            @Test
            public void differentStart() {
                final Duration startA = Duration.ofMillis(1000);
                final Duration startB = Duration.ofMillis(2000);
                final Duration end = Duration.ofMillis(3000);
                final String value = "State";

                final var timestampedA = new TimestampedValue<>(startA, end, value);
                final var timestampedB = new TimestampedValue<>(startB, end, value);

                assertInvariants(timestampedA, timestampedB);
                assertNotEquals(timestampedA, timestampedB);
            }

            @Test
            public void differentValue() {
                final Duration start = WHEN_A;
                final Duration end = start.plusSeconds(5);
                final String valueA = "A";
                final String valueB = "B";

                final var timestampedA = new TimestampedValue<>(start, end, valueA);
                final var timestampedB = new TimestampedValue<>(start, end, valueB);

                assertInvariants(timestampedA, timestampedB);
                assertNotEquals(timestampedA, timestampedB);
            }

            @Test
            public void equivalent() {
                final Duration startA = Duration.ofMillis(1000);
                final Duration startB = Duration.ofMillis(1000);
                final Duration endA = Duration.ofMillis(2000);
                final Duration endB = Duration.ofMillis(2000);
                final String valueA = "Value";
                final String valueB = new String(valueA);
                assert startA.equals(startB);
                assert endA.equals(endB);
                assert valueA.equals(valueB);
                assert startA != startB;// tough test
                assert endA != endB;// tough test
                assert valueA != valueB;// tough test

                final var timestampedA = new TimestampedValue<>(startA, endA, valueA);
                final var timestampedB = new TimestampedValue<>(startB, endB, valueB);

                assertInvariants(timestampedA, timestampedB);
                assertEquals(timestampedA, timestampedB);
            }
        }// class

        @Test
        public void broad() {
            final var start = WHEN_B;
            final var end = start.plusDays(365);
            constructor(start, end, Integer.valueOf(0));
        }

        @Test
        public void narrow() {
            final var start = WHEN_A;
            final var end = start.plusNanos(1);
            constructor(start, end, "Value");
        }

        @Test
        public void nullValue() {
            constructor(WHEN_A, WHEN_A.plusSeconds(5), (Integer) null);
        }

    }// class

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);

    public static <STATE> void assertInvariants(@Nonnull final TimestampedValue<STATE> timestamped) {
        ObjectTest.assertInvariants(timestamped);// inherited

        final var start = timestamped.getStart();
        final var end = timestamped.getEnd();
        assertAll("Not null", // guard
                () -> assertNotNull(start, "start"), () -> assertNotNull(end, "end"));
        assertThat("The end time is at or after the start time.", end, greaterThanOrEqualTo(start));
    }

    public static <STATE> void assertInvariants(@Nonnull final TimestampedValue<STATE> timestamped1,
            @Nonnull final TimestampedValue<STATE> timestamped2) {
        ObjectTest.assertInvariants(timestamped1, timestamped2);// inherited

        final var value1 = timestamped1.getValue();
        final var value2 = timestamped2.getValue();
        if (value1 != null && value2 != null) {
            ObjectTest.assertInvariants(value1, value2);
        }
        assertAll("Value semantics",
                () -> EqualsSemanticsTest.assertValueSemantics(timestamped1, timestamped2, "start",
                        ts -> ts.getStart()),
                () -> EqualsSemanticsTest.assertValueSemantics(timestamped1, timestamped2, "end", ts -> ts.getEnd()),
                () -> assertTrue(
                        timestamped1.equals(timestamped2) == (timestamped1.getStart().equals(timestamped2.getStart())
                                && timestamped1.getEnd().equals(timestamped2.getEnd())
                                && Objects.equals(value1, value2)),
                        "equals"));
    }

    private static <VALUE> void constructor(@Nonnull final Duration start, @Nonnull final Duration end,
            @Nullable final VALUE value) {
        final var timestamped = new TimestampedValue<>(start, end, value);

        assertInvariants(timestamped);
        assertAll(() -> assertSame(start, timestamped.getStart(), "start"),
                () -> assertSame(end, timestamped.getEnd(), "end"),
                () -> assertSame(value, timestamped.getValue(), "value"));
    }
}// class