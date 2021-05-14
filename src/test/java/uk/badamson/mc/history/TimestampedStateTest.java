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
public class TimestampedStateTest {

    @Nested
    public class Constructor {

        @Nested
        public class Two {

            @Test
            public void differentState() {
                final String stateA = "A";
                final String stateB = "B";

                final var timestampedA = new TimestampedState<>(WHEN_A, stateA);
                final var timestampedB = new TimestampedState<>(WHEN_A, stateB);

                assertInvariants(timestampedA, timestampedB);
                assertNotEquals(timestampedA, timestampedB);
            }

            @Test
            public void differentWhen() {
                final Duration whenA = Duration.ofMillis(1000);
                final Duration whenB = Duration.ofMillis(2000);
                final String state = "State";

                final var timestampedA = new TimestampedState<>(whenA, state);
                final var timestampedB = new TimestampedState<>(whenB, state);

                assertInvariants(timestampedA, timestampedB);
                assertNotEquals(timestampedA, timestampedB);
            }

            @Test
            public void equivalent() {
                final Duration whenA = Duration.ofMillis(1000);
                final Duration whenB = Duration.ofMillis(1000);
                final String stateA = "State";
                final String stateB = new String(stateA);
                assert whenA.equals(whenB);
                assert stateA.equals(stateB);
                assert whenA != whenB;// tough test
                assert stateA != stateB;// tough test

                final var timestampedA = new TimestampedState<>(whenA, stateA);
                final var timestampedB = new TimestampedState<>(whenB, stateB);

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
        public void nullState() {
            test(WHEN_A, (Integer) null);
        }

        private <STATE> void test(@Nonnull final Duration when, @Nullable final STATE state) {
            final var timestamped = new TimestampedState<>(when, state);

            assertInvariants(timestamped);
            assertAll(() -> assertSame(when, timestamped.getWhen(), "when"),
                    () -> assertSame(state, timestamped.getState(), "state"));
        }

    }// class

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);

    public static <STATE> void assertInvariants(@Nonnull final TimestampedState<STATE> timestamped) {
        ObjectTest.assertInvariants(timestamped);// inherited

        assertNotNull(timestamped.getWhen(), "Not null, when");
    }

    public static <STATE> void assertInvariants(@Nonnull final TimestampedState<STATE> timestamped1,
            @Nonnull final TimestampedState<STATE> timestamped2) {
        ObjectTest.assertInvariants(timestamped1, timestamped2);// inherited

        final var state1 = timestamped1.getState();
        final var state2 = timestamped2.getState();
        if (state1 != null && state2 != null) {
            ObjectTest.assertInvariants(state1, state2);
        }
        assertTrue(timestamped1.equals(timestamped2) == (timestamped1.getWhen().equals(timestamped2.getWhen())
                && Objects.equals(state1, state2)), "equals has value semantics");
    }
}// class