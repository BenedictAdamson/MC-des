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
import uk.badamson.mc.ObjectTest;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class TimestampedValueTest {

    @Nested
    public class Constructor {

        @Nested
        public class Two {

            @Test
            public void differentValue() {
                final String valueA = "A";
                final String valueB = "B";

                final var timestampedA = new TimestampedValue<>(WHEN_A, valueA);
                final var timestampedB = new TimestampedValue<>(WHEN_A, valueB);

                assertInvariants(timestampedA, timestampedB);
                assertNotEquals(timestampedA, timestampedB);
            }

            @Test
            public void differentWhen() {
                final Duration whenA = Duration.ofMillis(1000);
                final Duration whenB = Duration.ofMillis(2000);
                final String value = "State";

                final var timestampedA = new TimestampedValue<>(whenA, value);
                final var timestampedB = new TimestampedValue<>(whenB, value);

                assertInvariants(timestampedA, timestampedB);
                assertNotEquals(timestampedA, timestampedB);
            }

            @Test
            public void equivalent() {
                final Duration whenA = Duration.ofMillis(1000);
                final Duration whenB = Duration.ofMillis(1000);
                final String valueA = "Value";
                final String valueB = new String(valueA);
                assert whenA.equals(whenB);
                assert valueA.equals(valueB);
                assert whenA != whenB;// tough test
                assert valueA != valueB;// tough test

                final var timestampedA = new TimestampedValue<>(whenA, valueA);
                final var timestampedB = new TimestampedValue<>(whenB, valueB);

                assertInvariants(timestampedA, timestampedB);
                assertEquals(timestampedA, timestampedB);
            }
        }// class

        @Test
        public void a() {
            test(WHEN_A, "State");
        }

        @Test
        public void b() {
            test(WHEN_B, Integer.valueOf(0));
        }

        @Test
        public void nullValue() {
            test(WHEN_A, (Integer) null);
        }

        private <VALUE> void test(@Nonnull final Duration when, @Nullable final VALUE value) {
            final var timestamped = new TimestampedValue<>(when, value);

            assertInvariants(timestamped);
            assertAll(() -> assertSame(when, timestamped.getWhen(), "when"),
                    () -> assertSame(value, timestamped.getValue(), "value"));
        }

    }// class

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);

    public static <STATE> void assertInvariants(@Nonnull final TimestampedValue<STATE> timestamped) {
        ObjectTest.assertInvariants(timestamped);// inherited

        assertNotNull(timestamped.getWhen(), "Not null, when");
    }

    public static <STATE> void assertInvariants(@Nonnull final TimestampedValue<STATE> timestamped1,
            @Nonnull final TimestampedValue<STATE> timestamped2) {
        ObjectTest.assertInvariants(timestamped1, timestamped2);// inherited

        final var value1 = timestamped1.getValue();
        final var value2 = timestamped2.getValue();
        if (value1 != null && value2 != null) {
            ObjectTest.assertInvariants(value1, value2);
        }
        assertTrue(timestamped1.equals(timestamped2) == (timestamped1.getWhen().equals(timestamped2.getWhen())
                && Objects.equals(value1, value2)), "equals has value semantics");
    }
}// class