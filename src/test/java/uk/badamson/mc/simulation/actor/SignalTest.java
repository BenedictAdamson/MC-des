package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2021.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.simulation.ObjectStateId;
import uk.badamson.mc.simulation.ObjectStateIdTest;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class SignalTest {

    @Nested
    public class Constructor {

        @Nested
        public class Two {

            @Test
            public void differentReceiver() {
                final Signal<Integer> signalA = new TestSignal(ID_A, OBJECT_A);
                final Signal<Integer> signalB = new TestSignal(ID_A, OBJECT_B);

                assertInvariants(signalA, signalB);
                assertNotEquals(signalA, signalB);
            }

            @Test
            public void differentSentFrom() {
                final Signal<Integer> signalA = new TestSignal(ID_A, OBJECT_A);
                final Signal<Integer> signalB = new TestSignal(ID_B, OBJECT_A);

                assertInvariants(signalA, signalB);
                assertNotEquals(signalA, signalB);
            }

            @Test
            public void equivalent() {
                final ObjectStateId sentFromA = ID_A;
                final ObjectStateId sentFromB = new ObjectStateId(ID_A.getObject(), ID_A.getWhen());
                final UUID receiverA = OBJECT_B;
                final UUID receiverB = new UUID(receiverA.getMostSignificantBits(),
                        receiverA.getLeastSignificantBits());
                assert sentFromA.equals(sentFromB);
                assert receiverA.equals(receiverB);
                assert sentFromA != sentFromB; // tough test
                assert receiverA != receiverB; // tough test

                final Signal<Integer> signalA = new TestSignal(sentFromA, receiverA);
                final Signal<Integer> signalB = new TestSignal(sentFromB, receiverB);

                assertInvariants(signalA, signalB);
                assertEquals(signalA, signalB);
            }
        }// class

        @Test
        public void a() {
            constructor(ID_A, OBJECT_B);
        }

        @Test
        public void b() {
            constructor(ID_B, OBJECT_A);
        }

        @Test
        public void reflexive() {
            constructor(ID_A, OBJECT_A);
        }

    }// class

    static class TestSignal extends Signal<Integer> {

        TestSignal(@Nonnull final ObjectStateId sentFrom, @Nonnull final UUID receiver) {
            super(sentFrom, receiver);
        }

    }// class

    private static final UUID OBJECT_A = UUID.randomUUID();
    private static final UUID OBJECT_B = UUID.randomUUID();

    private static final Duration WHEN_A = Duration.ofMillis(0);
    private static final Duration WHEN_B = Duration.ofMillis(5000);

    private static final ObjectStateId ID_A = new ObjectStateId(OBJECT_A, WHEN_A);
    private static final ObjectStateId ID_B = new ObjectStateId(OBJECT_B, WHEN_B);

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal) {
        ObjectTest.assertInvariants(signal);// inherited

        final var receiver = signal.getReceiver();
        final var sender = signal.getSender();
        final var sentFrom = signal.getSentFrom();
        final var whenSent = signal.getWhenSent();
        assertAll("Not null", () -> assertNotNull(receiver, "receiver"), () -> assertNotNull(sender, "sender"),
                () -> assertNotNull(sentFrom, "sentFrom"), // guard
                () -> assertNotNull(whenSent, "whenSent"));
        ObjectStateIdTest.assertInvariants(sentFrom);
        assertAll("consistent attributes", () -> assertSame(sender, sentFrom.getObject(), "sender with SentFrom"),
                () -> assertSame(whenSent, sentFrom.getWhen(), "whenSent with SentFrom"));
    }

    public static <STATE> void assertInvariants(@Nonnull final Signal<STATE> signal1,
            @Nonnull final Signal<STATE> signal2) {
        ObjectTest.assertInvariants(signal1, signal2);// inherited

        final var equals = signal1.equals(signal2);
        assertAll("value semantics",
                () -> assertFalse(equals && !signal1.getReceiver().equals(signal2.getReceiver()), "receiver"),
                () -> assertFalse(equals && !signal1.getSentFrom().equals(signal2.getSentFrom()), "sentFrom"));
    }

    private static Signal<Integer> constructor(@Nonnull final ObjectStateId sentFrom, @Nonnull final UUID receiver) {
        final Signal<Integer> signal = new TestSignal(sentFrom, receiver);

        assertInvariants(signal);
        assertAll("Attributes", () -> assertSame(sentFrom, signal.getSentFrom(), "sentFrom"),
                () -> assertSame(receiver, signal.getReceiver(), "receiver"));
        return signal;
    }

}
