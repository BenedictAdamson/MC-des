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
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.collection.IsIn.oneOf;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import uk.badamson.mc.ComparableTest;
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.history.ModifiableValueHistory;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.history.ValueHistoryTest;

/**
 * <p>
 * Unit tests for the {@link Universe} class.
 * </p>
 */
public class UniverseTest {

    @Nested
    public class BeginTransaction {

        @Test
        public void a() {
            final Universe universe = new Universe(DURATION_1);
            final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

            beginTransaction(universe, listener);
        }

        @Test
        public void b() {
            final Universe universe = new Universe(DURATION_2);
            final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

            beginTransaction(universe, listener);
        }

        private Universe.Transaction beginTransaction(final Universe universe, final TransactionListener listener) {
            final Universe.Transaction transaction = universe.beginTransaction(listener);

            assertNotNull(transaction, "Not null, transaction");// guard

            assertAll("Invariants", () -> assertInvariants(universe),
                    () -> TransactionTest.assertInvariants(transaction));

            assertAll(
                    () -> assertSame(universe, transaction.getUniverse(),
                            "The universe of the returned transaction is this transaction."),
                    () -> assertEquals(Collections.EMPTY_MAP, transaction.getObjectStatesRead(),
                            "The returned transaction has not read any object states."),
                    () -> assertEquals(Collections.EMPTY_MAP, transaction.getObjectStatesWritten(),
                            "The returned transaction has not written any object states."),
                    () -> assertEquals(Universe.TransactionOpenness.READING, transaction.getOpenness(),
                            "The transaction is in read mode."));

            return transaction;
        }

    }// class

    @Nested
    public class Constructor {

        @Test
        public void a() {
            test(DURATION_1);
        }

        private void assertPostconditions(final AtomicReference<Universe> universe, final Duration historyStart) {
            assertPostconditions(universe.get(), historyStart);
        }

        private void assertPostconditions(final Universe universe, final Duration historyStart) {
            assertInvariants(universe);

            assertSame(historyStart, universe.getHistoryStart(),
                    "The history start state time-stamp of this universe is "
                            + "the given history start state time-stamp.");
            assertEquals(Collections.emptySet(), universe.getObjectIds(), "The set of object IDs is empty.");
            assertEquals(Collections.emptySet(), universe.getObjectIds(), "The set of IDs of object states is empty.");

            assertUnknownObjectInvariants(universe, OBJECT_A);
            assertUnknownObjectInvariants(universe, OBJECT_B);
        }

        @Test
        public void b() {
            test(DURATION_2);
        }

        @RepeatedTest(64)
        public void multiThreaded() {
            final Duration historyStart = DURATION_1;
            final CountDownLatch ready = new CountDownLatch(1);
            final AtomicReference<Universe> universe = new AtomicReference<>();
            /*
             * Start the other thread while the universe object is not constructed, so the
             * safe publication at Thread.start() does not publish the constructed state.
             */
            final var future = runInOtherThread(ready, () -> assertPostconditions(universe, historyStart));

            universe.set(new Universe(historyStart));

            ready.countDown();
            get(future);
        }

        private void test(final Duration historyStart) {
            final Universe universe = new Universe(historyStart);

            assertPostconditions(universe, historyStart);
        }
    }// class

    @Nested
    public class SetHistoryStart {

        @Nested
        public class NoOpEmpty {

            @Test
            public void a() {
                test(DURATION_1);
            }

            @Test
            public void b() {
                test(DURATION_2);
            }

            private void test(final Duration historyStart) {
                final Universe universe = new Universe(historyStart);

                setHistoryStart(universe, historyStart);
            }
        }

        @Nested
        public class WithStateHistory {

            @Test
            public void a() {
                test(OBJECT_A, DURATION_1, DURATION_2, DURATION_3);
            }

            @Test
            public void b() {
                test(OBJECT_B, DURATION_2, DURATION_3, DURATION_4);
            }

            @Test
            public void end() {
                final Duration end = DURATION_2;
                test(OBJECT_A, DURATION_1, end, end);
            }

            @RepeatedTest(32)
            public void multiThreaded() {
                final UUID object = OBJECT_A;
                final Duration historyStart0 = DURATION_1;
                final Duration historyStart = DURATION_2;
                final Duration whenLastCommit = DURATION_3;

                final CountDownLatch ready = new CountDownLatch(1);
                final AtomicReference<Universe> universeAR = new AtomicReference<>();
                /*
                 * Start the other thread while the universe object is not constructed, so the
                 * safe publication at Thread.start() does not publish the constructed state.
                 */
                final var future = runInOtherThread(ready, () -> assertPostconditions(universeAR, historyStart));

                final Universe universe = new Universe(historyStart0);
                final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
                putAndCommit(universe, object, whenLastCommit, objectState);

                setHistoryStart(universe, historyStart);

                universeAR.set(universe);
                ready.countDown();
                get(future);
            }

            @Test
            public void noOp() {
                final Duration when = DURATION_1;
                test(OBJECT_A, when, when, when);
            }

            private void test(final UUID object, final Duration historyStart0, final Duration historyStart,
                    final Duration whenLastCommit) {
                assert historyStart0.compareTo(historyStart) <= 0;
                assert historyStart.compareTo(whenLastCommit) <= 0;
                final Universe universe = new Universe(historyStart0);
                final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
                putAndCommit(universe, object, whenLastCommit, objectState);

                universe.setHistoryStart(historyStart);

                assertPostconditions(universe, historyStart);
            }

        }// class

        private void assertPostconditions(final AtomicReference<Universe> universe, final Duration historyStart) {
            assertPostconditions(universe.get(), historyStart);
        }

        private void assertPostconditions(final Universe universe, final Duration historyStart) {
            assertInvariants(universe);
            assertEquals(historyStart, universe.getHistoryStart(),
                    "The history start time of this universe is equal to the given history start time.");
        }

        private void setHistoryStart(final Universe universe, @NonNull final Duration historyStart) {
            universe.setHistoryStart(historyStart);

            assertPostconditions(universe, historyStart);
        }

    }// class

    /**
     * <p>
     * Unit tests, and auxiliary test code, for testing the class
     * {@link Universe.Transaction}.
     * </p>
     */
    public static class TransactionTest {

        @Nested
        public class BeginAbort {

            @Nested
            public class AfterReadCommitted {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2, final Duration when3) {
                    assert when1.compareTo(when2) <= 0;
                    assert when2.compareTo(when3) <= 0;
                    assert when1.compareTo(when3) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    UniverseTest.putAndCommit(universe, object, when3, objectState2);

                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when2);

                    beginAbort(transaction);

                    assertAll(
                            () -> assertEquals(0, listener.getEnds(),
                                    "Did not end transactino (because not begun commit)"),
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, transaction.getOpenness(),
                                    "Aborting"));
                }

            }// class

            @Nested
            public class AfterReadNonExistent {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_4);
                }

                @Test
                public void precise() {
                    final Duration when = DURATION_2;
                    test(DURATION_1, OBJECT_A, when);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when) {
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when);

                    beginAbort(transaction);

                    assertAll(
                            () -> assertEquals(0, listener.getEnds(),
                                    "Did not end transaction (because did not begin commit)"),
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, transaction.getOpenness(),
                                    "Aborting"));
                }

            }// class

            @Nested
            public class AfterReadPastEnd {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                @Test
                public void precise() {
                    final Duration when = DURATION_2;
                    test(DURATION_1, OBJECT_A, when, when);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    assert when1.compareTo(when2) <= 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when2);

                    beginAbort(transaction);
                }

            }// class

            @Nested
            public class AfterReadUncommitted {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                @Test
                public void precise() {
                    final Duration when = DURATION_2;
                    test(DURATION_1, OBJECT_A, when, when);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    assert when1.compareTo(when2) <= 0;
                    final ObjectState objectState = new ObjectStateTest.TestObjectState(1);

                    final TransactionListenerTest.CountingTransactionListener writeListener = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener readListener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.beginWrite(when1);
                    writeTransaction.put(object, objectState);
                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object, when2);

                    beginAbort(readTransaction);

                    assertAll("Did not end transactions (because did not begin commit)",
                            () -> assertEquals(0, readListener.getEnds(), "reader"),
                            () -> assertEquals(0, writeListener.getEnds(), "writer"));
                    assertAll("Openness",
                            () -> assertEquals(Universe.TransactionOpenness.WRITING, writeTransaction.getOpenness(),
                                    "Writer (still) writing"),
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, readTransaction.getOpenness(),
                                    "Reader aborting "));
                }

            }// class

            @Nested
            public class AfterWrite {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when2);
                    transaction.put(object, objectState2);

                    beginAbort(transaction);

                    assertAll(() -> UniverseTest.assertInvariants(universe),
                            () -> assertSame(objectState1, universe.getObjectState(object, when2),
                                    "Rolled back aborted write [values]"),
                            () -> assertEquals(Collections.singleton(when1),
                                    universe.getObjectStateHistory(object).getTransitionTimes(),
                                    "Rolled back aborted write [transition times]"));
                }

            }// class

            @Nested
            public class FirstOfMutualReadPastLastCommit {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, OBJECT_B, DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6,
                            DURATION_7);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, OBJECT_A, DURATION_3, DURATION_4, DURATION_5, DURATION_6, DURATION_7,
                            DURATION_8);
                }

                @Test
                public void symmetric() {
                    final Duration when1 = DURATION_3;
                    final Duration when2 = DURATION_5;
                    final Duration when3 = DURATION_7;

                    test(DURATION_2, OBJECT_B, OBJECT_A, when1, when1, when2, when2, when3, when3);
                }

                private void test(final Duration historyStart, final UUID objectA, final UUID objectB,
                        final Duration whenA1, final Duration whenB1, final Duration whenB2, final Duration whenA2,
                        final Duration whenA3, final Duration whenB3) {
                    assert whenA1.compareTo(whenA2) < 0;
                    assert whenA2.compareTo(whenA3) <= 0;
                    assert whenB1.compareTo(whenB2) < 0;
                    assert whenB2.compareTo(whenB3) <= 0;
                    assert whenB2.compareTo(whenA3) < 0;
                    assert whenA2.compareTo(whenB3) < 0;
                    final ObjectState objectStateA1 = new ObjectStateTest.TestObjectState(11);
                    final ObjectState objectStateA2 = new ObjectStateTest.TestObjectState(12);
                    final ObjectState objectStateB1 = new ObjectStateTest.TestObjectState(21);
                    final ObjectState objectStateB2 = new ObjectStateTest.TestObjectState(22);

                    final TransactionListenerTest.CountingTransactionListener listenerA = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, objectA, whenA1, objectStateA1);
                    UniverseTest.putAndCommit(universe, objectB, whenB1, objectStateB1);

                    final Universe.Transaction transactionA = universe.beginTransaction(listenerA);
                    transactionA.getObjectState(objectA, whenA1);
                    transactionA.getObjectState(objectB, whenB2);
                    transactionA.beginWrite(whenA3);
                    transactionA.put(objectA, objectStateA2);

                    final Universe.Transaction transactionB = universe.beginTransaction(listenerB);
                    transactionB.getObjectState(objectB, whenB1);
                    transactionB.getObjectState(objectA, whenA2);
                    transactionB.beginWrite(whenB3);
                    transactionB.put(objectB, objectStateB2);

                    beginAbort(transactionA);

                    assertAll("Did not end transactions (they have not begun committing).",
                            () -> assertEquals(0, listenerA.getEnds(), "A"),
                            () -> assertEquals(0, listenerB.getEnds(), "B"));
                    assertAll("Openness",
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, transactionA.getOpenness(),
                                    "Aborting A"),
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, transactionB.getOpenness(),
                                    "Aborting B because mutual transaction is aborting"));
                }
            }// class

            @Nested
            public class InvalidateOtherRead {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, DURATION_4, OBJECT_A);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_B);
                }

                private void test(final Duration historyStart, final Duration when1, final Duration when2,
                        final Duration when3, final UUID object) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);

                    final TransactionListenerTest.CountingTransactionListener readListener = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener writeListener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, state1);
                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.getObjectState(object, when1);
                    writeTransaction.beginWrite(when2);
                    writeTransaction.put(object, state2);
                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object, when3);// reads state2

                    beginAbort(writeTransaction);

                    assertAll("Did not end transactions (because commit not begun)",
                            () -> assertEquals(0, readListener.getEnds(), "Read"),
                            () -> assertEquals(0, writeListener.getEnds(), "Write"));
                    assertAll("Transactions are aborting",
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, writeTransaction.getOpenness(),
                                    "Write (as commanded)"),
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, readTransaction.getOpenness(),
                                    "Read (invalidated)"));
                }

            }// class

            @Nested
            public class InvalidateSubsequentWrite {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, DURATION_4, OBJECT_A);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_B);
                }

                private void test(final Duration historyStart, final Duration when1, final Duration when2,
                        final Duration when3, final UUID object) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);

                    final TransactionListenerTest.CountingTransactionListener listener1 = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listener2 = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, state1);
                    final Universe.Transaction transaction1 = universe.beginTransaction(listener1);
                    transaction1.getObjectState(object, when1);
                    transaction1.beginWrite(when2);
                    transaction1.put(object, state2);
                    final Universe.Transaction transaction2 = universe.beginTransaction(listener2);
                    transaction2.getObjectState(object, when2);// becomes a successor of transaction1
                    transaction2.beginWrite(when3);
                    transaction2.put(object, state3);

                    beginAbort(transaction1);

                    assertAll("No transactinos ended (because did not begin commit)",
                            () -> assertEquals(0, listener1.getEnds(), "Transaction 1"),
                            () -> assertEquals(0, listener2.getEnds(), "Transaction 2"));
                    assertAll("Aborting transactions",
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, transaction1.getOpenness(),
                                    "Transaction 1"),
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, transaction2.getOpenness(),
                                    "Transaction 2 (invalidated)"));
                }

            }// class

            @Nested
            public class SecondOfMutualReadPastLastCommit {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, OBJECT_B, DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6,
                            DURATION_7);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, OBJECT_A, DURATION_3, DURATION_4, DURATION_5, DURATION_6, DURATION_7,
                            DURATION_8);
                }

                @Test
                public void symmetric() {
                    final Duration when1 = DURATION_3;
                    final Duration when2 = DURATION_5;
                    final Duration when3 = DURATION_7;

                    test(DURATION_2, OBJECT_B, OBJECT_A, when1, when1, when2, when2, when3, when3);
                }

                private void test(final Duration historyStart, final UUID objectA, final UUID objectB,
                        final Duration whenA1, final Duration whenB1, final Duration whenB2, final Duration whenA2,
                        final Duration whenA3, final Duration whenB3) {
                    assert whenA1.compareTo(whenA2) < 0;
                    assert whenA2.compareTo(whenA3) <= 0;
                    assert whenB1.compareTo(whenB2) < 0;
                    assert whenB2.compareTo(whenB3) <= 0;
                    assert whenB2.compareTo(whenA3) < 0;
                    assert whenA2.compareTo(whenB3) < 0;
                    final ObjectState objectStateA1 = new ObjectStateTest.TestObjectState(11);
                    final ObjectState objectStateA2 = new ObjectStateTest.TestObjectState(12);
                    final ObjectState objectStateB1 = new ObjectStateTest.TestObjectState(21);
                    final ObjectState objectStateB2 = new ObjectStateTest.TestObjectState(22);

                    final TransactionListenerTest.CountingTransactionListener listenerA = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, objectA, whenA1, objectStateA1);
                    UniverseTest.putAndCommit(universe, objectB, whenB1, objectStateB1);

                    final Universe.Transaction transactionA = universe.beginTransaction(listenerA);
                    transactionA.getObjectState(objectA, whenA1);
                    transactionA.getObjectState(objectB, whenB2);// read-past-the-end
                    transactionA.beginWrite(whenA3);
                    transactionA.put(objectA, objectStateA2);

                    final Universe.Transaction transactionB = universe.beginTransaction(listenerB);
                    transactionB.getObjectState(objectB, whenB1);
                    transactionB.getObjectState(objectA, whenA2);
                    transactionB.beginWrite(whenB3);
                    transactionB.put(objectB, objectStateB2);

                    beginAbort(transactionB);

                    assertAll("Did not end transactions (they have not begun committing).",
                            () -> assertEquals(0, listenerA.getEnds(), "A"),
                            () -> assertEquals(0, listenerB.getEnds(), "B"));
                    assertAll("Aborting transactions",
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, transactionA.getOpenness(),
                                    "A (because mutual transaction is aborting)"),
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, transactionB.getOpenness(), "B"));
                }
            }// class

            private void beginAbort(final Universe.Transaction transaction) {
                final boolean aborted0 = transaction.getOpenness() == Universe.TransactionOpenness.ABORTED;
                final boolean committed0 = transaction.getOpenness() == Universe.TransactionOpenness.COMMITTED;

                transaction.beginAbort();

                assertInvariants(transaction);

                final Universe.TransactionOpenness openness = transaction.getOpenness();
                assertAll(
                        () -> assertThat("The transaction is aborting, aborted or committed.", openness,
                                oneOf(Universe.TransactionOpenness.COMMITTED, Universe.TransactionOpenness.ABORTED,
                                        Universe.TransactionOpenness.ABORTING)),
                        () -> assertTrue(!committed0 || openness == Universe.TransactionOpenness.COMMITTED,
                                "If this transaction was committed, it remains committed."),
                        () -> assertTrue(!aborted0 || openness == Universe.TransactionOpenness.ABORTED,
                                "If this transaction was aborted, it remains aborted."));
            }

            @Test
            public void empty() {
                final Universe universe = new Universe(DURATION_1);
                final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                final Universe.Transaction transaction = universe.beginTransaction(listener);

                beginAbort(transaction);

                assertAll(
                        () -> assertEquals(0, listener.getEnds(), "Did not end transaction (because not begun commit)"),
                        () -> assertEquals(Universe.TransactionOpenness.ABORTING, transaction.getOpenness(),
                                "Aborting"));
            }

        }// class

        @Nested
        public class BeginCommit {

            @Nested
            public class AfterOutOfOrderPut {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_3, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_4, DURATION_3);
                }

                @Test
                public void near() {
                    test(DURATION_2, DURATION_2.plusNanos(1L), DURATION_2);
                }

                @Test
                public void same() {
                    test(DURATION_2, DURATION_2, DURATION_2);
                }

                private void test(final Duration historyStart0, final Duration when2, final Duration when1) {
                    assert historyStart0.compareTo(when1) <= 0;
                    assert when1.compareTo(when2) <= 0;
                    final UUID object = UniverseTest.OBJECT_A;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final SortedMap<Duration, ObjectState> expectedObjectStateHistory = new TreeMap<>();
                    expectedObjectStateHistory.put(when2, objectState1);

                    final Universe universe = new Universe(historyStart0);
                    UniverseTest.putAndCommit(universe, object, when2, objectState1);
                    final Duration historyEnd0 = universe.getHistoryEnd();
                    final ValueHistory<ObjectState> objectStateHistory0 = new ModifiableValueHistory<>(
                            universe.getObjectStateHistory(object));

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when1);
                    transaction.put(object, objectState2);

                    beginCommit(transaction);

                    assertAll("Transaction", () -> assertEquals(1, listener.getEnds(), "ended"),
                            () -> assertEquals(1, listener.getAborts(), "aborted"));
                    assertEquals(objectStateHistory0, universe.getObjectStateHistory(object), "Reverted state history");
                    assertAll("History range unchanged",
                            () -> assertEquals(historyStart0, universe.getHistoryStart(), "start"),
                            () -> assertEquals(historyEnd0, universe.getHistoryEnd(), "end"));
                }

            }// class

            @Nested
            public class AfterPut {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2);
                }

                private void assertCommonPostconditions(final Duration historyStart0, final UUID object,
                        final Duration when, final Universe universe,
                        final TransactionListenerTest.CountingTransactionListener listener) {
                    assertAll(() -> UniverseTest.assertInvariants(universe),
                            () -> assertEquals(0, listener.getAborts(), "Did not abort"),
                            () -> assertEquals(1, listener.getCommits(), "Committed"),
                            () -> assertEquals(historyStart0, universe.getHistoryStart(), "History start unchanged"),
                            () -> assertEquals(when, universe.getLatestCommit(object), "Latest commit of object"));
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3);
                }

                @RepeatedTest(8)
                public void multiThreaded() {
                    final Duration historyStart0 = DURATION_1;
                    final CountDownLatch ready = new CountDownLatch(1);
                    final AtomicReference<Universe> universeAR = new AtomicReference<>();
                    final int nThreads = Runtime.getRuntime().availableProcessors() * 4;
                    final Map<Universe.Transaction, TransactionListenerTest.CountingTransactionListener> transactions = new ConcurrentHashMap<>();
                    /*
                     * Start the other threads while the universe object is not constructed, so the
                     * safe publication at Thread.start() does not publish the constructed state.
                     */
                    final List<Future<Void>> futures = new ArrayList<>(nThreads);
                    for (int i = 0; i < nThreads; ++i) {
                        futures.add(runInOtherThread(ready, () -> {
                            final Universe universe = universeAR.get();
                            final ThreadLocalRandom random = ThreadLocalRandom.current();
                            final UUID object = UUID.randomUUID();
                            final Duration when = historyStart0.plusSeconds(random.nextInt(1, nThreads * 7));
                            final ObjectState objectState = new ObjectStateTest.TestObjectState(random.nextInt());

                            final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                            final Universe.Transaction transaction = universe.beginTransaction(listener);
                            transactions.put(transaction, listener);
                            transaction.beginWrite(when);
                            transaction.put(object, objectState);

                            beginCommit(transaction);
                        }));
                    }

                    universeAR.set(new Universe(historyStart0));
                    ready.countDown();

                    get(futures);

                    UniverseTest.assertInvariants(universeAR.get());
                    for (final var entry : transactions.entrySet()) {
                        final Universe.Transaction transaction = entry.getKey();
                        final TransactionListenerTest.CountingTransactionListener listener = entry.getValue();
                        assertInvariants(transaction);
                        assertAll(() -> assertEquals(0, listener.getAborts(), "Did not abort"),
                                () -> assertEquals(1, listener.getCommits(), "Committed"));
                    }
                }

                @Test
                public void prehistoric() {
                    test(DURATION_2, OBJECT_B, DURATION_1);
                }

                private void test(final Duration historyStart0, final UUID object, final Duration when) {
                    final Universe universe = new Universe(historyStart0);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
                    transaction.beginWrite(when);
                    transaction.put(object, objectState);

                    beginCommit(transaction);

                    assertCommonPostconditions(historyStart0, object, when, universe, listener);
                    assertAll(() -> assertThat("History end", universe.getHistoryEnd(), oneOf(historyStart0, when)),
                            () -> assertTrue(
                                    when.compareTo(historyStart0) <= 0 || universe.getHistoryEnd().equals(when),
                                    "History end advanced if not prehistoric"));
                }

            }

            @Nested
            public class AfterPutEnablingCommitOfReadPastLastCommit {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2, final Duration when3) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final TransactionListenerTest.CountingTransactionListener readListener = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener writeListener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);

                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object, when2);
                    readTransaction.beginCommit();

                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.getObjectState(object, when1);
                    writeTransaction.beginWrite(when3);
                    writeTransaction.put(object, objectState2);

                    beginCommit(writeTransaction);

                    assertAll(() -> assertEquals(1, writeListener.getCommits(), "Write committed"),
                            () -> assertEquals(0, writeListener.getAborts(), "Write not aborted"),
                            () -> assertEquals(1, readListener.getCommits(),
                                    "Read committed (subsequent write enabled commit)"),
                            () -> assertEquals(0, readListener.getAborts(), "Read not aborted"),
                            () -> assertEquals(when3, universe.getLatestCommit(object), "Latest commit of object"));
                }

            }// class

            @Nested
            public class AfterPutInvalidEventTimeStampOrder2 {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when0,
                        final Duration when1, final Duration when2) {
                    final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final TransactionListenerTest.CountingTransactionListener listener1 = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listener2 = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when0, objectState0);
                    final Universe.Transaction transaction1 = universe.beginTransaction(listener1);
                    transaction1.getObjectState(object, when0);
                    final Universe.Transaction transaction2 = universe.beginTransaction(listener2);
                    transaction2.getObjectState(object, when0);
                    transaction1.beginWrite(when2);
                    transaction1.put(object, objectState1);
                    transaction2.beginWrite(when1);
                    transaction2.put(object, objectState2);

                    beginCommit(transaction2);

                    assertAll(() -> assertTrue(0 < listener2.getEnds(), "Ended commit"),
                            () -> assertEquals(1, listener2.getAborts(), "Aborted commit"), () -> assertEquals(when0,
                                    universe.getLatestCommit(object), "Latest commit of object (unchanged)"));
                }

            }// class

            @Nested
            public class AfterPutNull {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3);
                }

                private void test(final Duration historyStart0, final UUID object, final Duration when) {
                    assert historyStart0.compareTo(when) < 0;
                    final Universe universe = new Universe(historyStart0);
                    final ObjectState objectState0 = new ObjectStateTest.TestObjectState(1);
                    putAndCommit(universe, object, historyStart0, objectState0);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when);
                    transaction.put(object, null);

                    beginCommit(transaction);

                    assertAll(() -> UniverseTest.assertInvariants(universe),
                            () -> assertEquals(0, listener.getAborts(), "Did not abort"),
                            () -> assertEquals(1, listener.getCommits(), "Committed"),
                            () -> assertEquals(historyStart0, universe.getHistoryStart(), "History start unchanged"));
                    assertAll("Destruction is forever.",
                            () -> assertEquals(ValueHistory.END_OF_TIME, universe.getHistoryEnd(), "History end"),
                            () -> assertEquals(ValueHistory.END_OF_TIME, universe.getLatestCommit(object),
                                    "Latest commit of object"));
                }

            }

            @Nested
            public class AfterPutsNotSuccessiveForSameObject2 {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when0,
                        final Duration when1, final Duration when2) {
                    final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final TransactionListenerTest.CountingTransactionListener listener1 = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listener2 = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when0, objectState0);
                    final Universe.Transaction transaction1 = universe.beginTransaction(listener1);
                    transaction1.getObjectState(object, when0);
                    final Universe.Transaction transaction2 = universe.beginTransaction(listener2);
                    transaction2.getObjectState(object, when0);
                    transaction1.beginWrite(when1);
                    transaction1.put(object, objectState1);
                    transaction2.beginWrite(when1);
                    transaction2.put(object, objectState2);

                    beginCommit(transaction2);

                    assertAll(() -> assertTrue(0 < listener2.getEnds(), "Ended transaction"),
                            () -> assertEquals(1, listener2.getAborts(), "Aborted transaction"),
                            () -> assertEquals(when0, universe.getLatestCommit(object),
                                    "Latest commit of object (unchanged)"));
                }

            }// class

            @Nested
            public class AfterPutWithCommittedDependency {
                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, OBJECT_A, OBJECT_B);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, OBJECT_B, OBJECT_A);
                }

                private void test(final Duration earliestCompleteState, final Duration when1, final Duration when2,
                        final UUID object1, final UUID object2) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(earliestCompleteState);
                    UniverseTest.putAndCommit(universe, object1, when1, objectState1);
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object1, when1);
                    transaction.beginWrite(when2);
                    transaction.put(object2, objectState2);

                    beginCommit(transaction);

                    assertAll(() -> assertTrue(0 < listener.getEnds(), "Ended transaction"),
                            () -> assertEquals(1, listener.getCommits(), "Committed transaction"),
                            () -> assertEquals(when2, universe.getLatestCommit(object2),
                                    "Latest commit of object (advanced)"));
                }

            }// class

            @Nested
            public class AfterReadOfDestruction {

                @Test
                public void a() {
                    test(OBJECT_A, DURATION_1, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void at() {
                    test(OBJECT_A, DURATION_1, DURATION_2, DURATION_3, DURATION_3);
                }

                @Test
                public void b() {
                    test(OBJECT_B, DURATION_2, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final UUID object, final Duration historyStart, final Duration whenExist,
                        final Duration whenDestroy, final Duration whenRead) {
                    assert whenExist.compareTo(whenDestroy) < 0;
                    assert whenDestroy.compareTo(whenRead) <= 0;
                    final ObjectState objectState0 = new ObjectStateTest.TestObjectState(1);

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, whenExist, objectState0);
                    UniverseTest.putAndCommit(universe, object, whenDestroy, null);
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, whenRead);

                    beginCommit(transaction);

                    assertAll("Transaction", () -> assertEquals(1, listener.getCommits(), "committed"),
                            () -> assertEquals(0, listener.getAborts(), "not aborted"));
                }

            }// class

            @Nested
            public class AfterReadPastLastCommit {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                @Test
                public void near() {
                    final Duration when1 = DURATION_3;
                    test(DURATION_2, OBJECT_B, when1, when1.plusNanos(1));
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when2);

                    beginCommit(transaction);

                    assertAll(() -> assertEquals(0, listener.getCommits(), "not committed"),
                            () -> assertEquals(0, listener.getAborts(), "not aborted"));
                }

            }// class

            @Nested
            public class AfterReadUncommitted {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                @Test
                public void precise() {
                    final Duration when = DURATION_2;
                    test(DURATION_1, OBJECT_A, when, when);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    assert when1.compareTo(when2) <= 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final TransactionListenerTest.CountingTransactionListener writeListener = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener readListener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.beginWrite(when1);
                    writeTransaction.put(object, objectState1);
                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object, when2);

                    beginCommit(readTransaction);

                    assertAll(() -> assertEquals(0, readListener.getAborts(), "Read not aborted."),
                            () -> assertEquals(0, readListener.getCommits(), "Read not committed."));
                }

            }// class

            @Nested
            public class AfterReadWithinHistory {
                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2, final Duration when3) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    UniverseTest.putAndCommit(universe, object, when3, objectState2);

                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when1);
                    transaction.getObjectState(object, when2);

                    beginCommit(transaction);

                    assertTrue(0 < listener.getEnds(), "Ended transaction");
                    assertEquals(1, listener.getCommits(), "Committed transaction");
                }

            }// class

            @Nested
            public class AttemptedResurection3 {
                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when0,
                        final Duration when1, final Duration when2) {
                    final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
                    final ObjectState objectState1 = null;// critical
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when0, objectState0);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);

                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when2);
                    transaction.put(object, objectState2);

                    beginCommit(transaction);

                    assertAll(() -> UniverseTest.assertInvariants(universe),
                            () -> assertTrue(0 < listener.getEnds(), "Ended transaction"),
                            () -> assertEquals(1, listener.getAborts(), "Aborted commit"),
                            () -> assertEquals(ValueHistory.END_OF_TIME, universe.getLatestCommit(object),
                                    "Latest commit of object (destruction is forever)"));
                }

            }// class

            @Nested
            public class ChainedDependencies {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_A, OBJECT_B, OBJECT_C);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, OBJECT_B, OBJECT_C, OBJECT_A);
                }

                private void test(final Duration historyStart, final Duration when1, final Duration when2,
                        final Duration when3, final Duration when4, final UUID object1, final UUID object2,
                        final UUID object3) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) < 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);
                    final ObjectStateTest.TestObjectState state4 = new ObjectStateTest.TestObjectState(4);
                    final ObjectStateTest.TestObjectState state5 = new ObjectStateTest.TestObjectState(5);

                    final TransactionListenerTest.CountingTransactionListener readListener = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener readWriteListener = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener writeListener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object1, when1, state1);
                    UniverseTest.putAndCommit(universe, object2, when1, state2);
                    UniverseTest.putAndCommit(universe, object3, when1, state3);

                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object1, when1);
                    readTransaction.getObjectState(object2, when3);// reads state2, which will be invalidated
                    readTransaction.beginCommit();

                    final Universe.Transaction readWriteTransaction = universe.beginTransaction(readWriteListener);
                    readWriteTransaction.getObjectState(object2, when1);
                    readWriteTransaction.getObjectState(object3, when2);// reads state3, which will be invalidated
                    readWriteTransaction.beginWrite(when3);
                    readWriteTransaction.put(object2, state4);
                    readWriteTransaction.beginCommit();

                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.getObjectState(object3, when1);
                    writeTransaction.beginWrite(when2);
                    writeTransaction.put(object3, state5);

                    beginCommit(writeTransaction);

                    assertAll(() -> assertEquals(1, readListener.getEnds(), "Ended read transaction"),
                            () -> assertEquals(1, readWriteListener.getEnds(), "Ended read-write transaction"),
                            () -> assertEquals(1, writeListener.getEnds(), "Ended write transaction"),
                            () -> assertEquals(1, writeListener.getCommits(), "Comitted write transaction"),
                            () -> assertEquals(1, readWriteListener.getAborts(),
                                    "Aborted (invalidated) read-write transaction"),
                            () -> assertEquals(1, readListener.getAborts(), "Aborted (invalidated) read transaction"));
                }

            }// class

            @Nested
            public class DifferentObjects2 {

                @Test
                public void a() {
                    test(OBJECT_A, OBJECT_B);
                }

                @Test
                public void b() {
                    test(OBJECT_B, OBJECT_A);
                }

                private void test(final UUID object1, final UUID object2) {
                    assert !object1.equals(object2);
                    final Duration when = UniverseTest.DURATION_1;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(when);
                    UniverseTest.putAndCommit(universe, object1, when, objectState1);
                    final ValueHistory<ObjectState> objectStateHistory1 = universe.getObjectStateHistory(object1);
                    final Universe.Transaction transaction2 = universe.beginTransaction(listener);
                    transaction2.beginWrite(when);
                    transaction2.put(object2, objectState2);

                    beginCommit(transaction2);

                    UniverseTest.assertInvariants(universe);
                    assertEquals(1, listener.getCommits(), "Committed");
                    assertEquals(Set.of(object1, object2), universe.getObjectIds(), "Object IDs");
                    assertEquals(objectStateHistory1, universe.getObjectStateHistory(object1),
                            "The object state histories of other objects are unchanged.");
                }

            }// class

            @Nested
            public class EnablingCommitOfReadUncommitted {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final TransactionListenerTest.CountingTransactionListener readListener = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener writeListener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.beginWrite(when);
                    writeTransaction.put(object, objectState1);
                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object, when);
                    readTransaction.beginCommit();

                    beginCommit(writeTransaction);

                    assertAll(() -> assertEquals(0, writeListener.getAborts(), "Write not aborted."),
                            () -> assertEquals(1, writeListener.getCommits(), "Write committed."),
                            () -> assertEquals(0, readListener.getAborts(), "Read not aborted."), () -> assertEquals(1,
                                    readListener.getCommits(), "Read committed (triggered by commit of write)."));
                }

            }// class

            @Nested
            public class InvalidateOtherRead {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_A, OBJECT_B);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, OBJECT_B, OBJECT_A);
                }

                private void test(final Duration historyStart, final Duration when1, final Duration when2,
                        final Duration when3, final Duration when4, final UUID object1, final UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) <= 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);

                    final TransactionListenerTest.CountingTransactionListener readListener = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener writeListener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object1, when1, state1);
                    UniverseTest.putAndCommit(universe, object2, when2, state2);
                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object1, when1);
                    readTransaction.getObjectState(object2, when4);// reads state2
                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.getObjectState(object2, when2);
                    writeTransaction.beginWrite(when3);
                    writeTransaction.put(object2, state3);

                    beginCommit(writeTransaction);
                    beginCommit(readTransaction);

                    assertAll(() -> assertEquals(1, readListener.getEnds(), "Ended read transaction"),
                            () -> assertEquals(1, writeListener.getEnds(), "Ended write transaction"),
                            () -> assertEquals(1, writeListener.getCommits(), "Comitted write transaction"),
                            () -> assertEquals(1, readListener.getAborts(), "Aborted (invalidated) read transaction"));
                }

            }// class

            @Nested
            public class InvalidateOtherReadThatBeganCommit {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_A, OBJECT_B);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, OBJECT_B, OBJECT_A);
                }

                private void test(final Duration historyStart, final Duration when1, final Duration when2,
                        final Duration when3, final Duration when4, final UUID object1, final UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) <= 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);

                    final TransactionListenerTest.CountingTransactionListener readListener = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener writeListener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object1, when1, state1);
                    UniverseTest.putAndCommit(universe, object2, when2, state2);

                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object1, when1);
                    readTransaction.getObjectState(object2, when4);// reads state2
                    readTransaction.beginCommit();

                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.getObjectState(object2, when2);
                    writeTransaction.beginWrite(when3);
                    writeTransaction.put(object2, state3);

                    beginCommit(writeTransaction);

                    assertAll(() -> assertEquals(1, readListener.getEnds(), "Ended read transaction"),
                            () -> assertEquals(1, writeListener.getEnds(), "Ended write transaction"),
                            () -> assertEquals(1, writeListener.getCommits(), "Comitted write transaction"),
                            () -> assertEquals(1, readListener.getAborts(), "Aborted (invalidated) read transaction"));
                }

            }// class

            @Nested
            public class MultipleInvalidateOtherRead {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, OBJECT_A, OBJECT_B);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, DURATION_7, OBJECT_B, OBJECT_A);
                }

                private void test(final Duration historyStart, final Duration when1, final Duration when2,
                        final Duration when3, final Duration when4, final Duration when5, final UUID object1,
                        final UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) < 0;
                    assert when4.compareTo(when5) < 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);
                    final ObjectStateTest.TestObjectState state4 = new ObjectStateTest.TestObjectState(4);

                    final TransactionListenerTest.CountingTransactionListener readListener = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener writeListener1 = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener writeListener2 = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object1, when1, state1);
                    UniverseTest.putAndCommit(universe, object2, when2, state2);
                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object1, when1);
                    readTransaction.getObjectState(object2, when5);// reads state2
                    final Universe.Transaction writeTransaction1 = universe.beginTransaction(writeListener1);
                    writeTransaction1.getObjectState(object2, when2);
                    writeTransaction1.beginWrite(when3);
                    writeTransaction1.put(object2, state3);
                    final Universe.Transaction writeTransaction2 = universe.beginTransaction(writeListener2);
                    writeTransaction2.getObjectState(object2, when3);// reads state3
                    writeTransaction2.beginWrite(when4);
                    writeTransaction2.put(object2, state4);

                    beginCommit(readTransaction);
                    beginCommit(writeTransaction1);
                    beginCommit(writeTransaction2);

                    assertAll(() -> assertEquals(1, readListener.getEnds(), "Ended read transaction"),
                            () -> assertEquals(1, writeListener1.getEnds(), "Ended write transaction 1"),
                            () -> assertEquals(1, writeListener2.getEnds(), "Ended write transaction 2"),
                            () -> assertEquals(1, writeListener1.getCommits(), "Comitted write transaction 1"),
                            () -> assertEquals(1, writeListener2.getCommits(), "Comitted write transaction 2"),
                            () -> assertEquals(1, readListener.getAborts(), "Aborted read transaction"));
                }

            }// class

            @Nested
            public class MutualReadPastLast3 {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, OBJECT_B, OBJECT_C, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, OBJECT_C, OBJECT_D, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final Duration historyStart, final UUID objectA, final UUID objectB,
                        final UUID objectC, final Duration when1, final Duration when2, final Duration when3) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) <= 0;
                    final ObjectState objectStateA1 = new ObjectStateTest.TestObjectState(11);
                    final ObjectState objectStateA2 = new ObjectStateTest.TestObjectState(12);
                    final ObjectState objectStateB1 = new ObjectStateTest.TestObjectState(21);
                    final ObjectState objectStateB2 = new ObjectStateTest.TestObjectState(22);
                    final ObjectState objectStateC1 = new ObjectStateTest.TestObjectState(31);
                    final ObjectState objectStateC2 = new ObjectStateTest.TestObjectState(32);

                    final TransactionListenerTest.CountingTransactionListener listenerA = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerC = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, objectA, when1, objectStateA1);
                    UniverseTest.putAndCommit(universe, objectB, when1, objectStateB1);
                    UniverseTest.putAndCommit(universe, objectC, when1, objectStateC1);

                    final Universe.Transaction transactionA = universe.beginTransaction(listenerA);
                    transactionA.getObjectState(objectA, when1);
                    transactionA.getObjectState(objectB, when2);
                    transactionA.getObjectState(objectC, when2);
                    transactionA.beginWrite(when3);
                    transactionA.put(objectA, objectStateA2);

                    final Universe.Transaction transactionB = universe.beginTransaction(listenerB);
                    transactionB.getObjectState(objectB, when1);
                    transactionB.getObjectState(objectA, when2);
                    transactionA.getObjectState(objectC, when2);
                    transactionB.beginWrite(when3);
                    transactionB.put(objectB, objectStateB2);

                    final Universe.Transaction transactionC = universe.beginTransaction(listenerC);
                    transactionC.getObjectState(objectC, when1);
                    transactionC.getObjectState(objectB, when2);
                    transactionC.getObjectState(objectA, when2);
                    transactionC.beginWrite(when3);
                    transactionC.put(objectC, objectStateC2);

                    beginCommit(transactionA);
                    beginCommit(transactionB);
                    beginCommit(transactionC);

                    assertAll(() -> assertEquals(1, listenerA.getCommits(), "Committed A."),
                            () -> assertEquals(1, listenerB.getCommits(), "Committed B."),
                            () -> assertEquals(1, listenerC.getCommits(), "Committed C."));
                }

            }// class

            @Nested
            public class MutualReadPastLast3Cycle {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, OBJECT_B, OBJECT_C, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, OBJECT_C, OBJECT_D, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final Duration historyStart, final UUID objectA, final UUID objectB,
                        final UUID objectC, final Duration when1, final Duration when2, final Duration when3) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) <= 0;
                    final ObjectState objectStateA1 = new ObjectStateTest.TestObjectState(11);
                    final ObjectState objectStateA2 = new ObjectStateTest.TestObjectState(12);
                    final ObjectState objectStateB1 = new ObjectStateTest.TestObjectState(21);
                    final ObjectState objectStateB2 = new ObjectStateTest.TestObjectState(22);
                    final ObjectState objectStateC1 = new ObjectStateTest.TestObjectState(31);
                    final ObjectState objectStateC2 = new ObjectStateTest.TestObjectState(32);

                    final TransactionListenerTest.CountingTransactionListener listenerA = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerC = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, objectA, when1, objectStateA1);
                    UniverseTest.putAndCommit(universe, objectB, when1, objectStateB1);
                    UniverseTest.putAndCommit(universe, objectC, when1, objectStateC1);

                    final Universe.Transaction transactionA = universe.beginTransaction(listenerA);
                    transactionA.getObjectState(objectA, when1);
                    transactionA.getObjectState(objectB, when2);
                    transactionA.beginWrite(when3);
                    transactionA.put(objectA, objectStateA2);

                    final Universe.Transaction transactionB = universe.beginTransaction(listenerB);
                    transactionB.getObjectState(objectB, when1);
                    transactionB.getObjectState(objectC, when2);
                    transactionB.beginWrite(when3);
                    transactionB.put(objectB, objectStateB2);

                    final Universe.Transaction transactionC = universe.beginTransaction(listenerC);
                    transactionC.getObjectState(objectC, when1);
                    transactionC.getObjectState(objectA, when2);
                    transactionC.beginWrite(when3);
                    transactionC.put(objectC, objectStateC2);

                    beginCommit(transactionA);
                    beginCommit(transactionB);
                    beginCommit(transactionC);

                    assertAll(() -> assertEquals(1, listenerA.getCommits(), "Committed A."),
                            () -> assertEquals(1, listenerB.getCommits(), "Committed B."),
                            () -> assertEquals(1, listenerC.getCommits(), "Committed C."));
                }

            }// class

            @Nested
            public class MutualReadPastLast4Merge {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, OBJECT_B, OBJECT_C, OBJECT_D, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, OBJECT_C, OBJECT_D, OBJECT_E, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final Duration historyStart, final UUID objectA, final UUID objectB,
                        final UUID objectC, final UUID objectD, final Duration when1, final Duration when2,
                        final Duration when3) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) <= 0;
                    final ObjectState objectStateA1 = new ObjectStateTest.TestObjectState(11);
                    final ObjectState objectStateA2 = new ObjectStateTest.TestObjectState(12);
                    final ObjectState objectStateB1 = new ObjectStateTest.TestObjectState(21);
                    final ObjectState objectStateB2 = new ObjectStateTest.TestObjectState(22);
                    final ObjectState objectStateC1 = new ObjectStateTest.TestObjectState(31);
                    final ObjectState objectStateC2 = new ObjectStateTest.TestObjectState(32);
                    final ObjectState objectStateD1 = new ObjectStateTest.TestObjectState(41);
                    final ObjectState objectStateD2 = new ObjectStateTest.TestObjectState(42);

                    final TransactionListenerTest.CountingTransactionListener listenerA = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerC = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerD = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, objectA, when1, objectStateA1);
                    UniverseTest.putAndCommit(universe, objectB, when1, objectStateB1);
                    UniverseTest.putAndCommit(universe, objectC, when1, objectStateC1);
                    UniverseTest.putAndCommit(universe, objectD, when1, objectStateD1);

                    final Universe.Transaction transactionA = universe.beginTransaction(listenerA);
                    transactionA.getObjectState(objectA, when1);
                    transactionA.getObjectState(objectB, when2);
                    transactionA.beginWrite(when3);
                    transactionA.put(objectA, objectStateA2);

                    final Universe.Transaction transactionB = universe.beginTransaction(listenerB);
                    transactionB.getObjectState(objectB, when1);
                    transactionB.getObjectState(objectA, when2);
                    transactionB.beginWrite(when3);
                    transactionB.put(objectB, objectStateB2);

                    final Universe.Transaction transactionC = universe.beginTransaction(listenerC);
                    transactionC.getObjectState(objectC, when1);
                    transactionC.getObjectState(objectD, when2);
                    transactionC.beginWrite(when3);
                    transactionC.put(objectC, objectStateC2);

                    final Universe.Transaction transactionD = universe.beginTransaction(listenerD);
                    transactionD.getObjectState(objectD, when1);
                    transactionD.getObjectState(objectC, when2);
                    transactionD.getObjectState(objectB, when2);
                    transactionD.getObjectState(objectA, when2);
                    transactionD.beginWrite(when3);
                    transactionD.put(objectD, objectStateD2);

                    beginCommit(transactionA);
                    beginCommit(transactionB);
                    beginCommit(transactionC);
                    beginCommit(transactionD);

                    assertAll(() -> assertEquals(1, listenerA.getCommits(), "Committed A."),
                            () -> assertEquals(1, listenerB.getCommits(), "Committed B."),
                            () -> assertEquals(1, listenerC.getCommits(), "Committed C."),
                            () -> assertEquals(1, listenerD.getCommits(), "Committed D."));
                }

            }// class

            @Nested
            public class MutualReadPastLast6MergeCycles {

                @Test
                public void abcdef() {
                    test(OBJECT_A, OBJECT_B, OBJECT_C, OBJECT_D, OBJECT_E, OBJECT_F);
                }

                @Test
                public void bcdefa() {
                    test(OBJECT_B, OBJECT_C, OBJECT_D, OBJECT_E, OBJECT_F, OBJECT_A);
                }

                @Test
                public void cdefab() {
                    test(OBJECT_C, OBJECT_D, OBJECT_E, OBJECT_F, OBJECT_A, OBJECT_B);
                }

                @Test
                public void defabc() {
                    test(OBJECT_D, OBJECT_E, OBJECT_F, OBJECT_A, OBJECT_B, OBJECT_C);
                }

                @Test
                public void efabcd() {
                    test(OBJECT_E, OBJECT_F, OBJECT_A, OBJECT_B, OBJECT_C, OBJECT_D);
                }

                @Test
                public void fabcde() {
                    test(OBJECT_F, OBJECT_A, OBJECT_B, OBJECT_C, OBJECT_D, OBJECT_E);
                }

                private void test(final UUID objectA, final UUID objectB, final UUID objectC, final UUID objectD,
                        final UUID objectE, final UUID objectF) {
                    final ObjectState objectStateA1 = new ObjectStateTest.TestObjectState(11);
                    final ObjectState objectStateA2 = new ObjectStateTest.TestObjectState(12);
                    final ObjectState objectStateB1 = new ObjectStateTest.TestObjectState(21);
                    final ObjectState objectStateB2 = new ObjectStateTest.TestObjectState(22);
                    final ObjectState objectStateC1 = new ObjectStateTest.TestObjectState(31);
                    final ObjectState objectStateC2 = new ObjectStateTest.TestObjectState(32);
                    final ObjectState objectStateD1 = new ObjectStateTest.TestObjectState(41);
                    final ObjectState objectStateD2 = new ObjectStateTest.TestObjectState(42);
                    final ObjectState objectStateE1 = new ObjectStateTest.TestObjectState(51);
                    final ObjectState objectStateE2 = new ObjectStateTest.TestObjectState(52);
                    final ObjectState objectStateF1 = new ObjectStateTest.TestObjectState(61);
                    final ObjectState objectStateF2 = new ObjectStateTest.TestObjectState(62);

                    final TransactionListenerTest.CountingTransactionListener listenerA = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerC = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerD = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerE = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerF = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(DURATION_1);
                    UniverseTest.putAndCommit(universe, objectA, DURATION_1, objectStateA1);
                    UniverseTest.putAndCommit(universe, objectB, DURATION_1, objectStateB1);
                    UniverseTest.putAndCommit(universe, objectC, DURATION_1, objectStateC1);
                    UniverseTest.putAndCommit(universe, objectD, DURATION_1, objectStateD1);
                    UniverseTest.putAndCommit(universe, objectE, DURATION_1, objectStateE1);
                    UniverseTest.putAndCommit(universe, objectF, DURATION_1, objectStateF1);

                    final Universe.Transaction transactionA = universe.beginTransaction(listenerA);
                    transactionA.getObjectState(objectA, DURATION_1);
                    transactionA.getObjectState(objectC, DURATION_2);

                    final Universe.Transaction transactionB = universe.beginTransaction(listenerB);
                    transactionB.getObjectState(objectB, DURATION_1);
                    transactionB.getObjectState(objectA, DURATION_2);

                    final Universe.Transaction transactionC = universe.beginTransaction(listenerC);
                    transactionC.getObjectState(objectC, DURATION_1);
                    transactionC.getObjectState(objectB, DURATION_2);

                    // A, B & C form a cycle

                    final Universe.Transaction transactionD = universe.beginTransaction(listenerD);
                    transactionD.getObjectState(objectD, DURATION_1);
                    transactionD.getObjectState(objectF, DURATION_2);

                    final Universe.Transaction transactionE = universe.beginTransaction(listenerE);
                    transactionE.getObjectState(objectE, DURATION_1);
                    transactionE.getObjectState(objectD, DURATION_2);

                    final Universe.Transaction transactionF = universe.beginTransaction(listenerF);
                    transactionF.getObjectState(objectF, DURATION_1);
                    transactionF.getObjectState(objectE, DURATION_2);

                    // D, E & F form a cycle

                    // Join the cycles
                    transactionA.getObjectState(objectF, DURATION_2);
                    transactionF.getObjectState(objectA, DURATION_2);

                    transactionA.beginWrite(DURATION_3);
                    transactionA.put(objectA, objectStateA2);
                    transactionB.beginWrite(DURATION_3);
                    transactionB.put(objectB, objectStateB2);
                    transactionC.beginWrite(DURATION_3);
                    transactionC.put(objectC, objectStateC2);
                    transactionD.beginWrite(DURATION_3);
                    transactionD.put(objectD, objectStateD2);
                    transactionE.beginWrite(DURATION_3);
                    transactionE.put(objectE, objectStateE2);
                    transactionF.beginWrite(DURATION_3);
                    transactionF.put(objectF, objectStateF2);

                    beginCommit(transactionA);
                    beginCommit(transactionB);
                    beginCommit(transactionC);
                    beginCommit(transactionD);
                    beginCommit(transactionE);
                    beginCommit(transactionF);

                    assertAll(() -> assertEquals(1, listenerA.getCommits(), "Committed A."),
                            () -> assertEquals(1, listenerB.getCommits(), "Committed B."),
                            () -> assertEquals(1, listenerC.getCommits(), "Committed C."),
                            () -> assertEquals(1, listenerD.getCommits(), "Committed D."),
                            () -> assertEquals(1, listenerE.getCommits(), "Committed E."),
                            () -> assertEquals(1, listenerF.getCommits(), "Committed F."));
                }

            }// class

            @Nested
            public class MutualReadPastLastCommit {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, OBJECT_B, DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6,
                            DURATION_7);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, OBJECT_A, DURATION_3, DURATION_4, DURATION_5, DURATION_6, DURATION_7,
                            DURATION_8);
                }

                @Test
                public void symmetric() {
                    final Duration when1 = DURATION_3;
                    final Duration when2 = DURATION_5;
                    final Duration when3 = DURATION_7;

                    test(DURATION_2, OBJECT_B, OBJECT_A, when1, when1, when2, when2, when3, when3);
                }

                private void test(final Duration historyStart, final UUID objectA, final UUID objectB,
                        final Duration whenA1, final Duration whenB1, final Duration whenB2, final Duration whenA2,
                        final Duration whenA3, final Duration whenB3) {
                    assert whenA1.compareTo(whenA2) < 0;
                    assert whenA2.compareTo(whenA3) <= 0;
                    assert whenB1.compareTo(whenB2) < 0;
                    assert whenB2.compareTo(whenB3) <= 0;
                    assert whenB2.compareTo(whenA3) < 0;
                    assert whenA2.compareTo(whenB3) < 0;
                    final ObjectState objectStateA1 = new ObjectStateTest.TestObjectState(11);
                    final ObjectState objectStateA2 = new ObjectStateTest.TestObjectState(12);
                    final ObjectState objectStateB1 = new ObjectStateTest.TestObjectState(21);
                    final ObjectState objectStateB2 = new ObjectStateTest.TestObjectState(22);

                    final TransactionListenerTest.CountingTransactionListener listenerA = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, objectA, whenA1, objectStateA1);
                    UniverseTest.putAndCommit(universe, objectB, whenB1, objectStateB1);

                    final Universe.Transaction transactionA = universe.beginTransaction(listenerA);
                    transactionA.getObjectState(objectA, whenA1);
                    transactionA.getObjectState(objectB, whenB2);
                    transactionA.beginWrite(whenA3);
                    transactionA.put(objectA, objectStateA2);

                    final Universe.Transaction transactionB = universe.beginTransaction(listenerB);
                    transactionB.getObjectState(objectB, whenB1);
                    transactionB.getObjectState(objectA, whenA2);
                    transactionB.beginWrite(whenB3);
                    transactionB.put(objectB, objectStateB2);

                    beginCommit(transactionA);
                    beginCommit(transactionB);

                    assertAll(() -> assertEquals(1, listenerA.getCommits(), "Committed A."),
                            () -> assertEquals(1, listenerB.getCommits(), "Committed B."));
                }

            }// class

            @Nested
            public class MutualReadPastLastCommitAwaitingCommit {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, OBJECT_B, DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6,
                            DURATION_7);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, OBJECT_A, DURATION_3, DURATION_4, DURATION_5, DURATION_6, DURATION_7,
                            DURATION_8);
                }

                @Test
                public void symmetric() {
                    final Duration when1 = DURATION_3;
                    final Duration when2 = DURATION_5;
                    final Duration when3 = DURATION_7;

                    test(DURATION_2, OBJECT_B, OBJECT_A, when1, when1, when2, when2, when3, when3);
                }

                private void test(final Duration historyStart, final UUID objectA, final UUID objectB,
                        final Duration whenA1, final Duration whenB1, final Duration whenB2, final Duration whenA2,
                        final Duration whenA3, final Duration whenB3) {
                    assert whenA1.compareTo(whenA2) < 0;
                    assert whenA2.compareTo(whenA3) <= 0;
                    assert whenB1.compareTo(whenB2) < 0;
                    assert whenB2.compareTo(whenB3) <= 0;
                    assert whenB2.compareTo(whenA3) < 0;
                    assert whenA2.compareTo(whenB3) < 0;
                    final ObjectState objectStateA1 = new ObjectStateTest.TestObjectState(11);
                    final ObjectState objectStateA2 = new ObjectStateTest.TestObjectState(12);
                    final ObjectState objectStateB1 = new ObjectStateTest.TestObjectState(21);
                    final ObjectState objectStateB2 = new ObjectStateTest.TestObjectState(22);

                    final TransactionListenerTest.CountingTransactionListener listenerA = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, objectA, whenA1, objectStateA1);
                    UniverseTest.putAndCommit(universe, objectB, whenB1, objectStateB1);

                    final Universe.Transaction transactionA = universe.beginTransaction(listenerA);
                    transactionA.getObjectState(objectA, whenA1);
                    transactionA.getObjectState(objectB, whenB2);
                    transactionA.beginWrite(whenA3);
                    transactionA.put(objectA, objectStateA2);

                    final Universe.Transaction transactionB = universe.beginTransaction(listenerB);
                    transactionB.getObjectState(objectB, whenB1);
                    transactionB.getObjectState(objectA, whenA2);
                    transactionB.beginWrite(whenB3);
                    transactionB.put(objectB, objectStateB2);

                    beginCommit(transactionA);

                    assertAll(() -> assertEquals(0, listenerA.getEnds(), "Has not ended A."),
                            () -> assertEquals(0, listenerB.getEnds(), "Has not ended B."),
                            () -> assertEquals(0, listenerA.getCommits(), "Has not committed A."),
                            () -> assertEquals(0, listenerB.getCommits(), "Has not committed B."));
                }

            }// class

            @Nested
            public class MutualReadPastLastCommitWithAdditionalUncommittedDependency {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, OBJECT_B, OBJECT_C, DURATION_2, DURATION_3, DURATION_4, DURATION_5,
                            DURATION_6, DURATION_7, DURATION_8);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, OBJECT_C, OBJECT_A, DURATION_3, DURATION_4, DURATION_5, DURATION_6,
                            DURATION_7, DURATION_8, DURATION_9);
                }

                @Test
                public void symmetric() {
                    final Duration when1 = DURATION_3;
                    final Duration when2 = DURATION_5;
                    final Duration when3 = DURATION_7;
                    test(DURATION_1, OBJECT_A, OBJECT_B, OBJECT_C, DURATION_2, when1, when1, when2, when2, when3,
                            when3);
                }

                private void test(final Duration historyStart, final UUID objectA, final UUID objectB,
                        final UUID objectC, final Duration whenA, final Duration whenB1, final Duration whenC1,
                        final Duration whenC2, final Duration whenB2, final Duration whenB3, final Duration whenC3) {
                    assert whenA.compareTo(whenB3) < 0;
                    assert whenB1.compareTo(whenB2) < 0;
                    assert whenB2.compareTo(whenB3) <= 0;
                    assert whenC1.compareTo(whenC2) < 0;
                    assert whenC2.compareTo(whenC3) <= 0;
                    assert whenC2.compareTo(whenB3) < 0;
                    assert whenB2.compareTo(whenC3) < 0;
                    final ObjectState objectStateA = new ObjectStateTest.TestObjectState(0);
                    final ObjectState objectStateB1 = new ObjectStateTest.TestObjectState(21);
                    final ObjectState objectStateB2 = new ObjectStateTest.TestObjectState(22);
                    final ObjectState objectStateC1 = new ObjectStateTest.TestObjectState(31);
                    final ObjectState objectStateC2 = new ObjectStateTest.TestObjectState(32);

                    final TransactionListenerTest.CountingTransactionListener listenerA = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerC = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, objectB, whenB1, objectStateB1);
                    UniverseTest.putAndCommit(universe, objectC, whenC1, objectStateC1);

                    final Universe.Transaction transactionA = universe.beginTransaction(listenerA);
                    transactionA.beginWrite(whenA);
                    transactionA.put(objectA, objectStateA);// uncommitted write

                    final Universe.Transaction transactionB = universe.beginTransaction(listenerB);
                    transactionB.getObjectState(objectB, whenB1);
                    transactionB.getObjectState(objectA, whenA);// read uncommitted
                    transactionB.getObjectState(objectC, whenC2);
                    transactionB.beginWrite(whenB3);
                    transactionB.put(objectB, objectStateB2);

                    final Universe.Transaction transactionC = universe.beginTransaction(listenerC);
                    transactionC.getObjectState(objectC, whenC1);
                    transactionC.getObjectState(objectB, whenB2);
                    transactionC.beginWrite(whenC3);
                    transactionC.put(objectC, objectStateC2);

                    beginCommit(transactionB);
                    beginCommit(transactionC);

                    assertAll(() -> assertEquals(0, listenerA.getEnds(), "Did not end A."),
                            () -> assertEquals(0, listenerB.getEnds(), "Did not end B."),
                            () -> assertEquals(0, listenerC.getEnds(), "Did not end C."),
                            () -> assertEquals(0, listenerA.getCommits(), "Did not commit A."),
                            () -> assertEquals(0, listenerB.getCommits(), "Did not commit B."), () -> assertEquals(0,
                                    listenerC.getCommits(), "Did not commit C (awaiting indirect dependency)."));
                }

            }// class

            @Nested
            public class RollbackWrite {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, OBJECT_A, OBJECT_B);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, DURATION_7, OBJECT_B, OBJECT_A);
                }

                private void test(final Duration historyStart, final Duration when1, final Duration when2,
                        final Duration when3, final Duration when4, final Duration when5, final UUID object1,
                        final UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) < 0;
                    assert when4.compareTo(when5) < 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);
                    final ObjectStateTest.TestObjectState state4 = new ObjectStateTest.TestObjectState(4);

                    final TransactionListenerTest.CountingTransactionListener listener1 = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listener2 = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object1, when1, state1);
                    UniverseTest.putAndCommit(universe, object2, when2, state2);

                    final Universe.Transaction transaction1 = universe.beginTransaction(listener1);
                    transaction1.getObjectState(object1, when1);
                    transaction1.getObjectState(object2, when4);// reads state2
                    transaction1.beginWrite(when5);
                    transaction1.put(object1, state4);// was previously state1
                    transaction1.beginCommit();

                    final Universe.Transaction transaction2 = universe.beginTransaction(listener2);
                    transaction2.getObjectState(object2, when2);
                    transaction2.beginWrite(when3);
                    transaction2.put(object2, state3);

                    beginCommit(transaction2);

                    assertAll(() -> UniverseTest.assertInvariants(universe),
                            () -> assertEquals(1, listener1.getEnds(), "Ended transaction 1"),
                            () -> assertTrue(0 < listener2.getEnds(), "Ended transaction 2"),
                            () -> assertEquals(1, listener2.getCommits(), "Comitted transaction 2"),
                            () -> assertEquals(1, listener1.getAborts(), "Aborted (invalidated) transaction 1"));
                    assertAll(
                            () -> assertSame(state1, universe.getObjectState(object1, when5),
                                    "Rolled back aborted write [values]"),
                            () -> assertEquals(Collections.singleton(when1),
                                    universe.getObjectStateHistory(object1).getTransitionTimes(),
                                    "Rolled back aborted write [transition times]"));
                }

            }// class

            @Nested
            public class SatisfyingDependencyOfDuplicateAppends {

                @Test
                public void a() {
                    test(OBJECT_A, OBJECT_B, DURATION_1, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(OBJECT_B, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
                }

                private void test(final UUID objectA, final UUID objectB, final Duration historyStart,
                        final Duration whenA, final Duration whenB) {
                    assert historyStart.compareTo(whenA) < 0;
                    assert whenA.compareTo(whenB) < 0;
                    final ObjectState objectStateA0 = new ObjectStateTest.TestObjectState(11);
                    final ObjectState objectStateA1 = new ObjectStateTest.TestObjectState(12);
                    final ObjectState objectStateB0 = new ObjectStateTest.TestObjectState(21);
                    final ObjectState objectStateB1 = new ObjectStateTest.TestObjectState(22);
                    final ObjectState objectStateB2 = new ObjectStateTest.TestObjectState(22);

                    final TransactionListenerTest.CountingTransactionListener listenerA = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB1 = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listenerB2 = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    putAndCommit(universe, objectA, historyStart, objectStateA0);
                    putAndCommit(universe, objectB, historyStart, objectStateB0);

                    final Universe.Transaction transactionA = universe.beginTransaction(listenerA);
                    transactionA.getObjectState(objectA, historyStart);
                    transactionA.beginWrite(whenA);
                    transactionA.put(objectA, objectStateA1);

                    final Universe.Transaction transactionB1 = universe.beginTransaction(listenerB1);
                    transactionB1.getObjectState(objectB, historyStart);
                    transactionB1.getObjectState(objectA, whenA);
                    transactionB1.beginWrite(whenB);
                    transactionB1.put(objectB, objectStateB1);
                    transactionB1.beginCommit();

                    final Universe.Transaction transactionB2 = universe.beginTransaction(listenerB2);
                    transactionB1.getObjectState(objectB, historyStart);
                    transactionB2.getObjectState(objectA, whenA);
                    transactionB2.beginWrite(whenB);
                    transactionB2.put(objectB, objectStateB2);
                    transactionB2.beginCommit();

                    beginCommit(transactionA);

                    assertAll("Independent write ended", () -> assertEquals(0, listenerA.getAborts(), "not aborted."),
                            () -> assertEquals(1, listenerA.getCommits(), "committed."));
                    assertAll("First of duplicate writes won",
                            () -> assertEquals(0, listenerB1.getAborts(), "not aborted."),
                            () -> assertEquals(1, listenerB1.getCommits(), "committed."));
                    assertAll("Seconds of duplicate writes lost",
                            () -> assertEquals(1, listenerB2.getAborts(), "aborted."),
                            () -> assertEquals(0, listenerB2.getCommits(), "not committed."));
                }

            }// class

            @Nested
            public class SuccessiveStates2 {
                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void near() {
                    test(DURATION_1, DURATION_1, DURATION_1.plusNanos(1));
                }

                private void test(final Duration historyStart, final Duration when1, final Duration when2) {
                    assert historyStart.compareTo(when1) <= 0;
                    assert when1.compareTo(when2) < 0;
                    final UUID object = UniverseTest.OBJECT_A;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final ModifiableValueHistory<ObjectState> expectedObjectStateHistory = new ModifiableValueHistory<>();
                    expectedObjectStateHistory.appendTransition(when1, objectState1);
                    expectedObjectStateHistory.appendTransition(when2, objectState2);

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);

                    final TransactionListenerTest.CountingTransactionListener writeListener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe.Transaction transaction = universe.beginTransaction(writeListener);
                    transaction.getObjectState(object, when1);
                    transaction.beginWrite(when2);
                    transaction.put(object, objectState2);

                    beginCommit(transaction);

                    assertAll(() -> assertEquals(0, writeListener.getAborts(), "Write did not abort."),
                            () -> assertEquals(1, writeListener.getCommits(), "Write committed."));
                    assertEquals(Collections.singleton(object), universe.getObjectIds(), "Object IDs.");
                    assertEquals(expectedObjectStateHistory, universe.getObjectStateHistory(object),
                            "Object state history.");
                    assertSame(objectState1, universe.getObjectState(object, when2.minusNanos(1L)),
                            "The state of an object at a given point in time is "
                                    + "the state it had at the latest state transition "
                                    + "at or before that point in time (just before second)");
                    assertAll("History time range",
                            () -> assertSame(historyStart, universe.getHistoryStart(), "start unchanged"),
                            () -> assertSame(when2, universe.getHistoryEnd(), "end"));
                }

            }// class

            private void beginCommit(final Universe.Transaction transaction) {
                transaction.beginCommit();

                assertInvariants(transaction);
                final Universe.TransactionOpenness openness = transaction.getOpenness();
                assertThat("The transaction is not reading or writing.", openness,
                        not(anyOf(is(Universe.TransactionOpenness.READING), is(Universe.TransactionOpenness.WRITING))));
            }

            @Test
            public void failure() {
                final Duration historyStart = DURATION_1;
                final UUID object = OBJECT_A;
                final Duration when0 = DURATION_2;
                final Duration when1 = DURATION_3;

                final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
                final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                final TransactionListenerTest.CountingTransactionListener listener1 = new TransactionListenerTest.CountingTransactionListener();
                final TransactionListenerTest.CountingTransactionListener listener2 = new TransactionListenerTest.CountingTransactionListener();

                final Universe universe = new Universe(historyStart);
                UniverseTest.putAndCommit(universe, object, when0, objectState0);
                final Universe.Transaction transaction1 = universe.beginTransaction(listener1);
                transaction1.getObjectState(object, when0);
                final Universe.Transaction transaction2 = universe.beginTransaction(listener2);
                transaction2.getObjectState(object, when0);
                transaction1.beginWrite(when1);
                transaction1.put(object, objectState1);
                transaction2.beginWrite(when1);
                transaction2.put(object, objectState2);

                beginCommit(transaction1);
                beginCommit(transaction2);

                assertEquals(0, listener2.getCommits(), "Did not commit second transaction");
                assertEquals(1, listener2.getAborts(), "Aborted second transaction");
            }

            @Test
            public void immediately() {
                final Universe universe = new Universe(DURATION_1);

                final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                final Universe.Transaction transaction = universe.beginTransaction(listener);

                beginCommit(transaction);

                assertEquals(0, listener.getAborts(), "Did not abort");
                assertEquals(1, listener.getCommits(), "Committed");
            }

            @RepeatedTest(8)
            public void mutualReadPastLastMultiThreaded() {
                final Duration historyStart = DURATION_1;
                final Duration when1 = DURATION_2;
                final Duration when2 = DURATION_3;
                final Duration when3 = DURATION_4;

                final int nThreads = Runtime.getRuntime().availableProcessors() * 4;
                /*
                 * Use multiple latches to limit the kinds of interleaving, so we are mostly
                 * testing only beginCommit().
                 */
                final CountDownLatch readyToStart = new CountDownLatch(1);
                final CountDownLatch readsDone = new CountDownLatch(nThreads);
                final CountDownLatch readyToWrite = new CountDownLatch(1);
                final CountDownLatch writesDone = new CountDownLatch(nThreads);
                final CountDownLatch readyToCommit = new CountDownLatch(1);

                final UUID[] objects = new UUID[nThreads];
                final Map<UUID, AtomicReference<Universe.Transaction>> transactions = new ConcurrentHashMap<>(nThreads);
                final Universe universe = new Universe(historyStart);
                for (int i = 0; i < nThreads; i++) {
                    objects[i] = UUID.randomUUID();
                    final ObjectState objectStateI1 = new ObjectStateTest.TestObjectState(i);
                    UniverseTest.putAndCommit(universe, objects[i], when1, objectStateI1);
                }
                final List<Future<Void>> futures = new ArrayList<>(nThreads);
                for (int i = 0; i < nThreads; ++i) {
                    final int iObject = i;
                    futures.add(runInOtherThread(readyToStart, () -> {
                        final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                        try (final Universe.Transaction transaction = universe.beginTransaction(listener);) {
                            transactions.put(objects[iObject], new AtomicReference<Universe.Transaction>(transaction));
                            for (int j = 0; j < nThreads; ++j) {
                                final UUID object = objects[j];
                                if (iObject == j) {
                                    transaction.getObjectState(object, when1);
                                } else {
                                    transaction.getObjectState(object, when2);
                                }
                            }
                            readsDone.countDown();
                            try {
                                readyToWrite.await();
                            } catch (final InterruptedException e) {
                                throw new AssertionError(e);
                            }
                            transaction.beginWrite(when3);
                            transaction.put(objects[iObject], new ObjectStateTest.TestObjectState(1000 + iObject));
                            writesDone.countDown();
                            try {
                                readyToCommit.await();
                            } catch (final InterruptedException e) {
                                throw new AssertionError(e);
                            }

                            beginCommit(transaction);
                        }
                    }));
                }

                readyToStart.countDown();
                try {
                    readsDone.await();
                } catch (final InterruptedException e) {
                    throw new AssertionError(e);
                }
                readyToWrite.countDown();
                try {
                    writesDone.await();
                } catch (final InterruptedException e) {
                    throw new AssertionError(e);
                }
                readyToCommit.countDown();

                get(futures);

                UniverseTest.assertInvariants(universe);
                final Universe.TransactionCoordinator coordinator0 = transactions.get(objects[0])
                        .get().transactionCoordinator;
                UniverseTest.assertInvariants(coordinator0);
                synchronized (coordinator0.lock) {
                    assertTrue(coordinator0.predecessors.isEmpty(), "TransactionCoordinator has no predecessors");
                    assertTrue(coordinator0.successors.isEmpty(), "TransactionCoordinator has no succesors");
                }

                for (int i = 0; i < nThreads; ++i) {
                    final UUID object = objects[i];
                    final Universe.Transaction transaction = transactions.get(object).get();
                    assertInvariants(transaction);
                    final Universe.TransactionCoordinator coordinator;
                    synchronized (transaction.lock) {
                        coordinator = transaction.transactionCoordinator;
                    }
                    assertSame(coordinator0, coordinator, "All the transactions have merged");
                }

                for (int i = 0; i < nThreads; ++i) {
                    final UUID object = objects[i];
                    final Universe.Transaction transaction = transactions.get(object).get();
                    assertInvariants(transaction);
                    final Universe.TransactionCoordinator coordinator;
                    final Map<UUID, Universe.ObjectData> pastTheEndReads;
                    synchronized (transaction.lock) {
                        coordinator = transaction.transactionCoordinator;
                        pastTheEndReads = transaction.pastTheEndReads;
                    }
                    assertSame(coordinator0, coordinator, "All the transactions have merged");
                    assertTrue(pastTheEndReads.isEmpty(), "All past-the-end-reads converted to mutual dependency");
                    assertEquals(Universe.TransactionOpenness.COMMITTED, transaction.getOpenness(),
                            "Committed write for object [" + i + "]");
                    assertEquals(when3, universe.getLatestCommit(object), "Committed write for object [" + i + "]");
                }
            }
        }// class

        @Nested
        public class BeginWrite {

            @Nested
            public class AfterRead0 {
                @Test
                public void a() {
                    test(DURATION_1, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3);
                }

                private final void test(final Duration historyStart, final Duration when) {
                    final Universe universe = new Universe(historyStart);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);

                    beginWrite(transaction, when);
                }
            }// class

            @Nested
            public class AfterRead1 {
                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                @Test
                public void near() {
                    final Duration when1 = DURATION_2;
                    final Duration when2 = when1.plusNanos(1L);// critical

                    test(DURATION_1, OBJECT_A, when1, when2);
                }

                private final void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final Universe universe = new Universe(historyStart);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when1);

                    beginWrite(transaction, when2);
                }
            }// class

            private final void beginWrite(final Universe.Transaction transaction, final Duration when) {
                transaction.beginWrite(when);

                assertInvariants(transaction);
                assertSame(when, transaction.getWhen(),
                        "The time-stamp of any object states to be written by this transaction becomes the same as the given time-stamp.");
            }
        }// class

        @Nested
        public class Close {

            @Nested
            public class AfterBeginCommitReadPastLastCommit {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                @Test
                public void near() {
                    final Duration when1 = DURATION_3;
                    test(DURATION_2, OBJECT_B, when1, when1.plusNanos(1));
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when2);
                    transaction.beginCommit();

                    close(transaction);
                }

            }// class

            @Nested
            public class AfterWrite {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when2);
                    transaction.put(object, objectState2);

                    close(transaction);

                    assertAll(() -> assertEquals(1, listener.getEnds(), "Ended the transaction"),
                            () -> assertEquals(1, listener.getAborts(), "Aborted the transaction"),
                            () -> assertSame(objectState1, universe.getObjectState(object, when2),
                                    "Rolled back aborted write [values]"),
                            () -> assertEquals(Collections.singleton(when1),
                                    universe.getObjectStateHistory(object).getTransitionTimes(),
                                    "Rolled back aborted write [transition times]"));
                }

            }// class

            @Nested
            public class AfterXommitOfReadWithinHistory {
                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2, final Duration when3) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    UniverseTest.putAndCommit(universe, object, when3, objectState2);

                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when1);
                    transaction.getObjectState(object, when2);
                    transaction.beginCommit();

                    close(transaction);
                }

            }// class

            @Test
            public void afterAbort() {
                final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();

                final Universe universe = new Universe(DURATION_1);
                final Universe.Transaction transaction = universe.beginTransaction(listener);
                transaction.beginAbort();

                close(transaction);

                assertAll(() -> assertEquals(0, listener.getCommits(), "Not committed"),
                        () -> assertEquals(1, listener.getAborts(), "Aborted"));
            }

            private void close(final Universe.Transaction transaction) {
                final Universe.TransactionOpenness openness0 = transaction.getOpenness();

                transaction.close();

                assertInvariants(transaction);
                final Universe.TransactionOpenness openness = transaction.getOpenness();
                assertThat("This transaction is aborted committing or committed.", openness,
                        oneOf(Universe.TransactionOpenness.ABORTED, Universe.TransactionOpenness.COMMITTED,
                                Universe.TransactionOpenness.COMMITTING));
                assertFalse(
                        openness0 == Universe.TransactionOpenness.ABORTED
                                && openness != Universe.TransactionOpenness.ABORTED,
                        "If this transaction was aborted it remains aborted.");
                assertFalse(
                        openness0 == Universe.TransactionOpenness.COMMITTED
                                && openness != Universe.TransactionOpenness.COMMITTED,
                        "If this transaction was committed it remains committed.");
                assertFalse(
                        openness0 == Universe.TransactionOpenness.COMMITTING
                                && openness != Universe.TransactionOpenness.COMMITTING,
                        "If this transaction was committing it is still committing.");
                assertFalse(
                        openness0 != Universe.TransactionOpenness.COMMITTED
                                && openness == Universe.TransactionOpenness.COMMITTED,
                        "This transaction is committed only if it was already committed.");
            }

            @Test
            public void immediately() {
                final Universe universe = new Universe(DURATION_1);
                final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                final Universe.Transaction transaction = universe.beginTransaction(listener);

                close(transaction);

                assertAll(() -> assertEquals(1, listener.getEnds(), "Ended the transaction"),
                        () -> assertEquals(1, listener.getAborts(), "Aborted the transaction"));
            }

        }// class

        @Nested
        public class GetObjectState {

            @Nested
            public class Empty {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when) {
                    final Universe universe = new Universe(historyStart);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);

                    getObjectState(transaction, object, when);
                }

            }// class

            @Nested
            public class Prehistoric {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3);
                }

                private void doTransaction(final Duration when1, final Universe universe) {
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);

                    assertThrows(PrehistoryException.class, () -> getObjectState(transaction, OBJECT_A, when1));
                }

                @RepeatedTest(32)
                public void multiThreaded() {
                    final Duration when1 = DURATION_1;
                    final Duration historyStart = DURATION_2;

                    final CountDownLatch ready = new CountDownLatch(1);
                    final AtomicReference<Universe> universeAR = new AtomicReference<>();
                    /*
                     * Start the other thread while the universe object is not constructed, so the
                     * safe publication at Thread.start() does not publish the constructed state.
                     */
                    final var future = runInOtherThread(ready, () -> doTransaction(when1, universeAR.get()));
                    universeAR.set(new Universe(historyStart));
                    ready.countDown();
                    get(future);
                }

                @Test
                public void near() {
                    test(DURATION_2.minusNanos(1L), DURATION_2);
                }

                private void test(final Duration when1, final Duration historyStart) {
                    assert when1.compareTo(historyStart) < 0;

                    final Universe universe = new Universe(historyStart);
                    doTransaction(when1, universe);
                }

            }// class

            @Nested
            public class ReadCommitted {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                @Test
                public void precise() {
                    final Duration when = DURATION_2;
                    test(DURATION_1, OBJECT_A, when, when);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    final ObjectStateId id2 = new ObjectStateId(object, when2);
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);

                    final ObjectState objectState2 = getObjectState(transaction, object, when2);

                    assertSame(objectState1, objectState2, "objectState");
                    assertEquals(Collections.singletonMap(id2, objectState1), transaction.getObjectStatesRead(),
                            "objectStatesRead");
                }

            }// class

            @Nested
            public class ReadCommittedAfterBeginAbort {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                @Test
                public void precise() {
                    final Duration when = DURATION_2;
                    test(DURATION_1, OBJECT_A, when, when);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    final ObjectStateId id2 = new ObjectStateId(object, when2);
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginAbort();

                    final ObjectState objectState2 = getObjectState(transaction, object, when2);

                    assertSame(objectState1, objectState2, "objectState");
                    assertEquals(Collections.singletonMap(id2, objectState1), transaction.getObjectStatesRead(),
                            "objectStatesRead");
                }

            }// class

            @Nested
            public class ReadUncommitted {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                @Test
                public void precise() {
                    final Duration when = DURATION_2;
                    test(DURATION_1, OBJECT_A, when, when);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    assert when1.compareTo(when2) <= 0;
                    final ObjectStateId id2 = new ObjectStateId(object, when2);
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final TransactionListenerTest.CountingTransactionListener listener1 = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listener2 = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    final Universe.Transaction transaction1 = universe.beginTransaction(listener1);
                    transaction1.beginWrite(when1);
                    transaction1.put(object, objectState1);
                    final Universe.Transaction transaction2 = universe.beginTransaction(listener2);

                    final ObjectState objectState2 = getObjectState(transaction2, object, when2);

                    assertSame(objectState1, objectState2, "Read the uncommitted value");
                    assertEquals(Collections.singletonMap(id2, objectState1), transaction2.getObjectStatesRead(),
                            "objectStatesRead");
                }

            }// class

            private ObjectState getObjectState(final Universe.Transaction transaction, final UUID object,
                    final Duration when) throws PrehistoryException {
                final ObjectStateId id = new ObjectStateId(object, when);
                final boolean wasPreviouslyRead = transaction.getObjectStatesRead().containsKey(id);
                final ObjectState previouslyReadState = transaction.getObjectStatesRead().get(id);
                final ObjectState universeObjectState = transaction.getUniverse().getObjectState(object, when);

                final ObjectState objectState = transaction.getObjectState(object, when);

                assertInvariants(transaction);
                assertAll("The object state for an object ID and point in time", () -> assertThat(
                        "is either the same object state as can be got from the universe of this transaction, or is the same object state as has already read by this transaction.",
                        objectState, anyOf(sameInstance(previouslyReadState), sameInstance(universeObjectState))),
                        () -> assertTrue(wasPreviouslyRead || objectState == universeObjectState,
                                "that has not already been read by this transaction is the same object state as can be  got from the universe of this transaction."),
                        () -> assertTrue(!wasPreviouslyRead || objectState == previouslyReadState,
                                "that has already been read by this transaction is the same object state as was read previously."));
                assertAll("The method records the returned state as one of the read states.",
                        () -> assertThat("has key.", id, in(transaction.getObjectStatesRead().keySet())),
                        () -> assertSame(objectState, transaction.getObjectStatesRead().get(id), "state"));

                return objectState;
            }

        }// class

        @Nested
        public class Put {

            @Nested
            public class Aborting {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3);
                }

                @Test
                public void endOfTime() {
                    test(DURATION_1, OBJECT_A, ValueHistory.END_OF_TIME);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when) {
                    final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
                    final ModifiableValueHistory<ObjectState> expectedHistory = new ModifiableValueHistory<>();

                    final Universe universe = new Universe(historyStart);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when);
                    transaction.beginAbort();

                    put(transaction, object, objectState);

                    assertAll(() -> UniverseTest.assertInvariants(universe),
                            () -> assertEquals(expectedHistory, universe.getObjectStateHistory(object),
                                    "Object state history (did not add to history because aborting)"),
                            () -> assertEquals(Collections.emptySet(), listener.getCreated(),
                                    "Did not call creation call-back"));
                }

            }// class

            @Nested
            public class AfterRead {
                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, OBJECT_A, OBJECT_B);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, OBJECT_B, OBJECT_A);
                }

                private void test(final Duration earliestCompleteState, final Duration when1, final Duration when2,
                        final UUID object1, final UUID object2) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final Universe universe = new Universe(earliestCompleteState);
                    UniverseTest.putAndCommit(universe, object1, when1, objectState1);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object1, when1);
                    transaction.beginWrite(when2);

                    put(transaction, object2, objectState2);

                    assertEquals(Collections.singleton(object2), listener.getCreated(), "Called creation call-back");
                }

            }// class

            @Nested
            public class Append {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
                }

                @Test
                public void endOfTime() {
                    test(DURATION_1, OBJECT_A, DURATION_2, ValueHistory.END_OF_TIME);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when1,
                        final Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);
                    final ModifiableValueHistory<ObjectState> expectedHistory = new ModifiableValueHistory<>();
                    expectedHistory.appendTransition(when1, objectState1);
                    expectedHistory.appendTransition(when2, objectState2);

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when1, objectState1);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when1);
                    transaction.beginWrite(when2);

                    put(transaction, object, objectState2);

                    assertAll(
                            () -> assertEquals(expectedHistory, universe.getObjectStateHistory(object),
                                    "Object state history"),
                            () -> assertEquals(Collections.emptySet(), listener.getCreated(),
                                    "Did not call creation call-back"));
                }

            }// class

            @Nested
            public class AttemptToReplaceCommittedValue {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3);
                }

                @Test
                public void endOfTime() {
                    test(DURATION_1, OBJECT_A, ValueHistory.END_OF_TIME);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);
                    final ModifiableValueHistory<ObjectState> expectedHistory = new ModifiableValueHistory<>();
                    expectedHistory.appendTransition(when, objectState1);

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object, when, objectState1);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    // Should transaction.getObjectState(object, when), but not for this test.
                    transaction.beginWrite(when);

                    put(transaction, object, objectState2);

                    assertAll(
                            () -> assertEquals(Universe.TransactionOpenness.ABORTING, transaction.getOpenness(),
                                    "Aborting the transaction"),
                            () -> assertEquals(expectedHistory, universe.getObjectStateHistory(object),
                                    "Did not replace committed history"));
                }

            }// class

            @Nested
            public class CreateWithoutDependencies {

                @Test
                public void a() {
                    test(DURATION_1, OBJECT_A, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, OBJECT_B, DURATION_3);
                }

                @Test
                public void endOfTime() {
                    test(DURATION_1, OBJECT_A, ValueHistory.END_OF_TIME);
                }

                @RepeatedTest(8)
                public void multiThreaded() {
                    final Duration historyStart = DURATION_1;
                    final Duration when = DURATION_2;

                    final CountDownLatch ready = new CountDownLatch(1);
                    final AtomicReference<Universe> universeAR = new AtomicReference<>();
                    final int nThreads = Runtime.getRuntime().availableProcessors() * 4;
                    /*
                     * Start the other threads while the universe object is not constructed, so the
                     * safe publication at Thread.start() does not publish the constructed state.
                     */
                    final List<Future<Void>> futures = new ArrayList<>(nThreads);
                    for (int i = 0; i < nThreads; ++i) {
                        futures.add(runInOtherThread(ready, () -> {
                            final Universe universe = universeAR.get();
                            final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                            final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
                            final UUID object = UUID.randomUUID();
                            final Universe.Transaction transaction = universe.beginTransaction(listener);
                            transaction.beginWrite(when);

                            put(transaction, object, objectState);

                            final ModifiableValueHistory<ObjectState> expectedHistory = new ModifiableValueHistory<>();
                            expectedHistory.appendTransition(when, objectState);
                            assertAll(() -> assertThat("Added the object", object, in(universe.getObjectIds())),
                                    () -> assertEquals(expectedHistory, universe.getObjectStateHistory(object),
                                            "Object state history"),
                                    () -> assertEquals(Collections.singleton(object), listener.getCreated(),
                                            "Called creation call-back"));
                        }));
                    }

                    universeAR.set(new Universe(historyStart));
                    ready.countDown();
                    get(futures);
                }

                private void test(final Duration historyStart, final UUID object, final Duration when) {
                    final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
                    final ModifiableValueHistory<ObjectState> expectedHistory = new ModifiableValueHistory<>();
                    expectedHistory.appendTransition(when, objectState);

                    final Universe universe = new Universe(historyStart);
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when);

                    put(transaction, object, objectState);

                    assertAll(() -> UniverseTest.assertInvariants(universe),
                            () -> assertEquals(Collections.singleton(object), universe.getObjectIds(), "Object IDs"),
                            () -> assertEquals(expectedHistory, universe.getObjectStateHistory(object),
                                    "Object state history"),
                            () -> assertEquals(Collections.singleton(object), listener.getCreated(),
                                    "Called creation call-back"));
                }

            }// class

            @Nested
            public class OutOfOrderStates2 {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_3, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_4, DURATION_3);
                }

                @Test
                public void near() {
                    test(DURATION_1, DURATION_2, DURATION_2.minusNanos(1L));
                }

                @Test
                public void same() {
                    test(DURATION_1, DURATION_2, DURATION_2);
                }

                private void test(final Duration historyStart0, final Duration when2, final Duration when1) {
                    assert when1.compareTo(when2) <= 0;
                    final UUID object = UniverseTest.OBJECT_A;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final Universe universe = new Universe(historyStart0);
                    UniverseTest.putAndCommit(universe, object, when2, objectState1);
                    final ValueHistory<ObjectState> objectStateHistory0 = new ModifiableValueHistory<>(
                            universe.getObjectStateHistory(object));

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when1);

                    put(transaction, object, objectState2);

                    assertEquals(objectStateHistory0, universe.getObjectStateHistory(object),
                            "Object state history unchanged");
                    assertAll("History range unchanged",
                            () -> assertEquals(historyStart0, universe.getHistoryStart(), "start"),
                            () -> assertEquals(when2, universe.getHistoryEnd(), "end"));
                }

            }// class

            @Nested
            public class PotentiallyInvalidateOtherRead {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_A, OBJECT_B);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, OBJECT_B, OBJECT_A);
                }

                private void test(final Duration historyStart, final Duration when1, final Duration when2,
                        final Duration when3, final Duration when4, final UUID object1, final UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) <= 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);

                    final TransactionListenerTest.CountingTransactionListener listener1 = new TransactionListenerTest.CountingTransactionListener();
                    final TransactionListenerTest.CountingTransactionListener listener2 = new TransactionListenerTest.CountingTransactionListener();

                    final Universe universe = new Universe(historyStart);
                    UniverseTest.putAndCommit(universe, object1, when1, state1);
                    UniverseTest.putAndCommit(universe, object2, when2, state2);
                    final Universe.Transaction transaction1 = universe.beginTransaction(listener1);
                    transaction1.getObjectState(object1, when1);
                    transaction1.getObjectState(object2, when4);// reads state2
                    final Universe.Transaction transaction2 = universe.beginTransaction(listener2);
                    transaction2.getObjectState(object2, when2);
                    transaction2.beginWrite(when3);

                    put(transaction2, object2, state3);

                    assertSame(state3, universe.getObjectState(object2, when3), "Provisionally wrote new value");
                }

            }// class

            @Nested
            public class TransitiveDependency3 {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, DURATION_4, OBJECT_A, OBJECT_B, OBJECT_C);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_B, OBJECT_C, OBJECT_A);
                }

                private void test(final Duration earliestCompleteState, final Duration when1, final Duration when2,
                        final Duration when3, final UUID object1, final UUID object2, final UUID object3) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectState objectState3 = new ObjectStateTest.TestObjectState(3);

                    final Universe universe = new Universe(earliestCompleteState);
                    UniverseTest.putAndCommit(universe, object1, when1, objectState1);
                    UniverseTest.putAndCommit(universe, object2, when2, objectState2);

                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object2, when2);
                    transaction.beginWrite(when3);

                    put(transaction, object3, objectState3);
                }

            }// class

            @RepeatedTest(32)
            public void mutualReadPastLastMultiThreaded() {
                final Duration historyStart = DURATION_1;
                final Duration when1 = DURATION_2;
                final Duration when2 = DURATION_3;
                final Duration when3 = DURATION_4;

                final CountDownLatch ready = new CountDownLatch(1);
                final int nThreads = Runtime.getRuntime().availableProcessors() * 4;

                final UUID[] objects = new UUID[nThreads];
                final Map<UUID, AtomicReference<Universe.Transaction>> transactions = new ConcurrentHashMap<>(nThreads);
                final Universe universe = new Universe(historyStart);
                for (int i = 0; i < nThreads; i++) {
                    objects[i] = UUID.randomUUID();
                    final ObjectState objectStateI1 = new ObjectStateTest.TestObjectState(i);
                    UniverseTest.putAndCommit(universe, objects[i], when1, objectStateI1);
                }
                final List<Future<Void>> futures = new ArrayList<>(nThreads);
                for (int i = 0; i < nThreads; ++i) {
                    final int iObject = i;
                    futures.add(runInOtherThread(ready, () -> {
                        final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                        final Universe.Transaction transaction = universe.beginTransaction(listener);
                        transactions.put(objects[iObject], new AtomicReference<Universe.Transaction>(transaction));
                        for (int j = 0; j < nThreads; ++j) {
                            final UUID object = objects[j];
                            if (iObject == j) {
                                transaction.getObjectState(object, when1);
                            } else {
                                transaction.getObjectState(object, when2);
                            }
                            assertInvariants(transaction);
                        }
                        transaction.beginWrite(when3);
                        final ObjectStateTest.TestObjectState state = new ObjectStateTest.TestObjectState(
                                1000 + iObject);
                        put(transaction, objects[iObject], state);
                    }));
                }

                ready.countDown();
                get(futures);
                UniverseTest.assertInvariants(universe);
                final Universe.TransactionCoordinator coordinator0 = transactions.get(objects[0])
                        .get().transactionCoordinator;
                UniverseTest.assertInvariants(coordinator0);
                synchronized (coordinator0.lock) {
                    assertTrue(coordinator0.predecessors.isEmpty(), "TransactionCoordinator has no predecessors");
                    assertTrue(coordinator0.successors.isEmpty(), "TransactionCoordinator has no succesors");
                }

                for (int i = 0; i < nThreads; ++i) {
                    final UUID object = objects[i];
                    final Universe.Transaction transaction = transactions.get(object).get();
                    assertInvariants(transaction);
                    final Universe.TransactionCoordinator coordinator;
                    synchronized (transaction.lock) {
                        coordinator = transaction.transactionCoordinator;
                    }
                    assertSame(coordinator0, coordinator, "All the transactions have merged");
                }

                for (int i = 0; i < nThreads; ++i) {
                    final UUID object = objects[i];
                    final Universe.Transaction transaction = transactions.get(object).get();
                    assertInvariants(transaction);
                    final Universe.TransactionCoordinator coordinator;
                    final Map<UUID, Universe.ObjectData> pastTheEndReads;
                    synchronized (transaction.lock) {
                        coordinator = transaction.transactionCoordinator;
                        pastTheEndReads = transaction.pastTheEndReads;
                    }
                    assertSame(coordinator0, coordinator, "All the transactions have merged");
                    assertTrue(pastTheEndReads.isEmpty(), "All past-the-end-reads converted to mutual dependency");
                }
            }

            private void put(final Universe.Transaction transaction, final UUID object, final ObjectState state) {
                transaction.put(object, state);

                assertInvariants(transaction);
                assertThat("The method records the given state as one of the states written.",
                        transaction.getObjectStatesWritten(), hasEntry(object, state));
            }
        }// class

        public static void assertInvariants(final Universe.Transaction transaction) {
            UniverseTest.assertInvariants(transaction);// inherited

            assertAll(() -> assertObjectStatesReadInvariants(transaction),
                    () -> assertObjectStatesWrittenInvariants(transaction),
                    () -> assertOpennessInvariants(transaction));
        }

        public static void assertInvariants(final Universe.Transaction transaction1,
                final Universe.Transaction transaction2) {
            UniverseTest.assertInvariants(transaction1, transaction2);// inherited
        }

        private static Map<ObjectStateId, ObjectState> assertObjectStatesReadInvariants(
                final Universe.Transaction transaction) {
            final Map<ObjectStateId, ObjectState> objectStatesRead = transaction.getObjectStatesRead();
            assertNotNull(objectStatesRead, "Always have a map of object states read.");// guard
            for (final var entry : objectStatesRead.entrySet()) {
                final ObjectStateId id = entry.getKey();
                final ObjectState state = entry.getValue();
                assertNotNull(id, "The map of object states read does not have a null key.");// guard
                ObjectStateIdTest.assertInvariants(id);
                if (state != null) {
                    ObjectStateTest.assertInvariants(state);
                }
                UniverseTest.assertInvariants(transaction.getUniverse(), id.getObject(), id.getWhen());
            }
            return objectStatesRead;
        }

        private static Map<UUID, ObjectState> assertObjectStatesWrittenInvariants(
                final Universe.Transaction transaction) {
            final Duration when = transaction.getWhen();

            final Map<UUID, ObjectState> objectStatesWritten = transaction.getObjectStatesWritten();

            assertNotNull(objectStatesWritten, "Always have a map of object states written.");// guard
            assertFalse(when == null && !objectStatesWritten.isEmpty(),
                    "The map of object states written is empty if this transaction is in read mode.");

            for (final var entry : objectStatesWritten.entrySet()) {
                final UUID object = entry.getKey();
                final ObjectState state = entry.getValue();
                assertNotNull(object, "The map of object states written does not have a null key.");
                if (state != null) {
                    ObjectStateTest.assertInvariants(state);
                }
                UniverseTest.assertInvariants(transaction.getUniverse(), object, when);
            }
            return objectStatesWritten;
        }

        private static Universe.TransactionOpenness assertOpennessInvariants(final Universe.Transaction transaction) {
            final Universe.TransactionOpenness openness = transaction.getOpenness();
            assertNotNull(openness, "openness");
            return openness;
        }

        @RepeatedTest(32)
        public void mutualReadPastLastMultiThreaded() {
            final Duration historyStart = DURATION_1;
            final Duration when1 = DURATION_2;
            final Duration when2 = DURATION_3;
            final Duration when3 = DURATION_4;

            final CountDownLatch ready = new CountDownLatch(1);
            final int nThreads = Runtime.getRuntime().availableProcessors() * 4;

            final UUID[] objects = new UUID[nThreads];
            final Map<UUID, AtomicReference<Universe.Transaction>> transactions = new ConcurrentHashMap<>(nThreads);
            final Universe universe = new Universe(historyStart);
            for (int i = 0; i < nThreads; i++) {
                objects[i] = UUID.randomUUID();
                final ObjectState objectStateI1 = new ObjectStateTest.TestObjectState(i);
                UniverseTest.putAndCommit(universe, objects[i], when1, objectStateI1);
            }
            final List<Future<Void>> futures = new ArrayList<>(nThreads);
            for (int i = 0; i < nThreads; ++i) {
                final int iObject = i;
                futures.add(runInOtherThread(ready, () -> {
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    try (final Universe.Transaction transaction = universe.beginTransaction(listener);) {
                        transactions.put(objects[iObject], new AtomicReference<Universe.Transaction>(transaction));
                        for (int j = 0; j < nThreads; ++j) {
                            final UUID object = objects[j];
                            if (iObject == j) {
                                transaction.getObjectState(object, when1);
                            } else {
                                transaction.getObjectState(object, when2);
                            }
                            assertInvariants(transaction);
                        }
                        transaction.beginWrite(when3);
                        assertInvariants(transaction);
                        transaction.put(objects[iObject], new ObjectStateTest.TestObjectState(1000 + iObject));
                        assertInvariants(transaction);
                        transaction.beginCommit();
                        assertInvariants(transaction);
                    } // try
                }));
            }

            ready.countDown();
            get(futures);
            UniverseTest.assertInvariants(universe);
            final Universe.TransactionCoordinator coordinator0 = transactions.get(objects[0])
                    .get().transactionCoordinator;
            UniverseTest.assertInvariants(coordinator0);
            synchronized (coordinator0.lock) {
                assertTrue(coordinator0.predecessors.isEmpty(), "TransactionCoordinator has no predecessors");
                assertTrue(coordinator0.successors.isEmpty(), "TransactionCoordinator has no succesors");
            }

            for (int i = 0; i < nThreads; ++i) {
                final UUID object = objects[i];
                final Universe.Transaction transaction = transactions.get(object).get();
                assertInvariants(transaction);
                final Universe.TransactionCoordinator coordinator;
                synchronized (transaction.lock) {
                    coordinator = transaction.transactionCoordinator;
                }
                assertSame(coordinator0, coordinator, "All the transactions have merged");
            }

            for (int i = 0; i < nThreads; ++i) {
                final UUID object = objects[i];
                final Universe.Transaction transaction = transactions.get(object).get();
                assertInvariants(transaction);
                final Universe.TransactionCoordinator coordinator;
                final Map<UUID, Universe.ObjectData> pastTheEndReads;
                synchronized (transaction.lock) {
                    coordinator = transaction.transactionCoordinator;
                    pastTheEndReads = transaction.pastTheEndReads;
                }
                assertSame(coordinator0, coordinator, "All the transactions have merged");
                assertTrue(pastTheEndReads.isEmpty(), "All past-the-end-reads converted to mutual dependency");
                assertEquals(Universe.TransactionOpenness.COMMITTED, transaction.getOpenness(),
                        "Committed write for object [" + i + "]");
                assertEquals(when3, universe.getLatestCommit(object), "Committed write for object [" + i + "]");
            }
        }

        @RepeatedTest(32)
        public void mutualReadPastLastMultiThreadedInterleavedReadsAndWrites() {
            final Duration historyStart = DURATION_1;
            final Duration when1 = DURATION_2;
            final Duration when2 = DURATION_3;
            final Duration when3 = DURATION_4;

            final int nThreads = Runtime.getRuntime().availableProcessors() * 4;
            final CountDownLatch ready = new CountDownLatch(1);
            final CountDownLatch writesDone = new CountDownLatch(nThreads);
            final CountDownLatch beginCommits = new CountDownLatch(1);

            final UUID[] objects = new UUID[nThreads];
            final Map<UUID, AtomicReference<Universe.Transaction>> transactions = new ConcurrentHashMap<>(nThreads);
            final Universe universe = new Universe(historyStart);
            for (int i = 0; i < nThreads; i++) {
                objects[i] = UUID.randomUUID();
                final ObjectState objectStateI1 = new ObjectStateTest.TestObjectState(i);
                UniverseTest.putAndCommit(universe, objects[i], when1, objectStateI1);
            }
            final List<Future<Void>> futures = new ArrayList<>(nThreads);
            for (int i = 0; i < nThreads; ++i) {
                final int iObject = i;
                futures.add(runInOtherThread(ready, () -> {
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    try (final Universe.Transaction transaction = universe.beginTransaction(listener);) {
                        transactions.put(objects[iObject], new AtomicReference<Universe.Transaction>(transaction));
                        for (int j = 0; j < nThreads; ++j) {
                            final UUID object = objects[j];
                            if (iObject == j) {
                                transaction.getObjectState(object, when1);
                            } else {
                                transaction.getObjectState(object, when2);
                            }
                            assertInvariants(transaction);
                        } // for
                        transaction.beginWrite(when3);
                        assertInvariants(transaction);
                        transaction.put(objects[iObject], new ObjectStateTest.TestObjectState(1000 + iObject));
                        assertInvariants(transaction);
                        writesDone.countDown();
                        try {
                            beginCommits.await();
                        } catch (final InterruptedException e) {
                            throw new AssertionError(e);
                        }
                        synchronized (transaction.lock) {
                            assertTrue(transaction.pastTheEndReads.isEmpty(),
                                    "Past-the-end-reads converted to mutual transactions (No past-the-end-reads)");
                        }
                        transaction.beginCommit();
                        assertInvariants(transaction);
                    } // try
                }));
            }

            ready.countDown();
            try {
                writesDone.await();
            } catch (final InterruptedException e) {
                throw new AssertionError(e);
            }
            beginCommits.countDown();

            get(futures);

            UniverseTest.assertInvariants(universe);
            final Universe.TransactionCoordinator coordinator0 = transactions.get(objects[0])
                    .get().transactionCoordinator;
            UniverseTest.assertInvariants(coordinator0);
            synchronized (coordinator0.lock) {
                assertTrue(coordinator0.predecessors.isEmpty(), "TransactionCoordinator has no predecessors");
                assertTrue(coordinator0.successors.isEmpty(), "TransactionCoordinator has no succesors");
            }

            for (int i = 0; i < nThreads; ++i) {
                final UUID object = objects[i];
                final Universe.Transaction transaction = transactions.get(object).get();
                assertInvariants(transaction);
                final Universe.TransactionCoordinator coordinator;
                synchronized (transaction.lock) {
                    coordinator = transaction.transactionCoordinator;
                }
                assertSame(coordinator0, coordinator, "All the transactions have merged");
            }

            for (int i = 0; i < nThreads; ++i) {
                final UUID object = objects[i];
                final Universe.Transaction transaction = transactions.get(object).get();
                assertInvariants(transaction);
                final Universe.TransactionCoordinator coordinator;
                final Map<UUID, Universe.ObjectData> pastTheEndReads;
                synchronized (transaction.lock) {
                    coordinator = transaction.transactionCoordinator;
                    pastTheEndReads = transaction.pastTheEndReads;
                }
                assertSame(coordinator0, coordinator, "All the transactions have merged");
                assertTrue(pastTheEndReads.isEmpty(), "All past-the-end-reads converted to mutual dependency");
                assertEquals(Universe.TransactionOpenness.COMMITTED, transaction.getOpenness(),
                        "Committed write for object [" + i + "]");
                assertEquals(when3, universe.getLatestCommit(object), "Committed write for object [" + i + "]");
            }
        }

        @RepeatedTest(32)
        public void mutualReadPastLastMultiThreadedInterleavedWritesAndCommits() {
            final Duration historyStart = DURATION_1;
            final Duration when1 = DURATION_2;
            final Duration when2 = DURATION_3;
            final Duration when3 = DURATION_4;

            final int nThreads = Runtime.getRuntime().availableProcessors() * 4;
            final CountDownLatch ready = new CountDownLatch(1);
            final CountDownLatch readsDone = new CountDownLatch(nThreads);
            final CountDownLatch beginWrites = new CountDownLatch(1);

            final UUID[] objects = new UUID[nThreads];
            final Map<UUID, AtomicReference<Universe.Transaction>> transactions = new ConcurrentHashMap<>(nThreads);
            final Universe universe = new Universe(historyStart);
            for (int i = 0; i < nThreads; i++) {
                objects[i] = UUID.randomUUID();
                final ObjectState objectStateI1 = new ObjectStateTest.TestObjectState(i);
                UniverseTest.putAndCommit(universe, objects[i], when1, objectStateI1);
            }
            final List<Future<Void>> futures = new ArrayList<>(nThreads);
            for (int i = 0; i < nThreads; ++i) {
                final int iObject = i;
                futures.add(runInOtherThread(ready, () -> {
                    final TransactionListenerTest.CountingTransactionListener listener = new TransactionListenerTest.CountingTransactionListener();
                    try (final Universe.Transaction transaction = universe.beginTransaction(listener);) {
                        transactions.put(objects[iObject], new AtomicReference<Universe.Transaction>(transaction));
                        for (int j = 0; j < nThreads; ++j) {
                            final UUID object = objects[j];
                            if (iObject == j) {
                                transaction.getObjectState(object, when1);
                            } else {
                                transaction.getObjectState(object, when2);
                            }
                            assertInvariants(transaction);
                        } // for
                        readsDone.countDown();
                        try {
                            beginWrites.await();
                        } catch (final InterruptedException e) {
                            throw new AssertionError(e);
                        }
                        transaction.beginWrite(when3);
                        assertInvariants(transaction);
                        transaction.put(objects[iObject], new ObjectStateTest.TestObjectState(1000 + iObject));
                        assertInvariants(transaction);
                        transaction.beginCommit();
                        assertInvariants(transaction);
                    } // try
                }));
            }

            ready.countDown();
            try {
                readsDone.await();
            } catch (final InterruptedException e) {
                throw new AssertionError(e);
            }
            beginWrites.countDown();

            get(futures);

            UniverseTest.assertInvariants(universe);
            final Universe.TransactionCoordinator coordinator0 = transactions.get(objects[0])
                    .get().transactionCoordinator;
            UniverseTest.assertInvariants(coordinator0);
            synchronized (coordinator0.lock) {
                assertTrue(coordinator0.predecessors.isEmpty(), "TransactionCoordinator has no predecessors");
                assertTrue(coordinator0.successors.isEmpty(), "TransactionCoordinator has no succesors");
            }

            for (int i = 0; i < nThreads; ++i) {
                final UUID object = objects[i];
                final Universe.Transaction transaction = transactions.get(object).get();
                assertInvariants(transaction);
                final Universe.TransactionCoordinator coordinator;
                synchronized (transaction.lock) {
                    coordinator = transaction.transactionCoordinator;
                }
                assertSame(coordinator0, coordinator, "All the transactions have merged");
            }

            for (int i = 0; i < nThreads; ++i) {
                final UUID object = objects[i];
                final Universe.Transaction transaction = transactions.get(object).get();
                assertInvariants(transaction);
                final Universe.TransactionCoordinator coordinator;
                final Map<UUID, Universe.ObjectData> pastTheEndReads;
                synchronized (transaction.lock) {
                    coordinator = transaction.transactionCoordinator;
                    pastTheEndReads = transaction.pastTheEndReads;
                }
                assertSame(coordinator0, coordinator, "All the transactions have merged");
                assertTrue(pastTheEndReads.isEmpty(), "All past-the-end-reads converted to mutual dependency");
                assertEquals(Universe.TransactionOpenness.COMMITTED, transaction.getOpenness(),
                        "Committed write for object [" + i + "]");
                assertEquals(when3, universe.getLatestCommit(object), "Committed write for object [" + i + "]");
            }
        }
    }// class

    static final UUID OBJECT_A = ObjectStateIdTest.OBJECT_A;

    static final UUID OBJECT_B = ObjectStateIdTest.OBJECT_B;
    static final UUID OBJECT_C = UUID.randomUUID();
    static final UUID OBJECT_D = UUID.randomUUID();
    static final UUID OBJECT_E = UUID.randomUUID();
    static final UUID OBJECT_F = UUID.randomUUID();
    static final Duration DURATION_1 = Duration.ofSeconds(13);
    static final Duration DURATION_2 = Duration.ofSeconds(17);
    static final Duration DURATION_3 = Duration.ofSeconds(23);
    static final Duration DURATION_4 = Duration.ofSeconds(29);
    static final Duration DURATION_5 = Duration.ofSeconds(31);
    static final Duration DURATION_6 = Duration.ofSeconds(37);
    static final Duration DURATION_7 = Duration.ofSeconds(43);
    static final Duration DURATION_8 = Duration.ofSeconds(47);
    static final Duration DURATION_9 = Duration.ofSeconds(53);

    private static boolean assertContainsObjectInvariants(final Universe universe, @NonNull final UUID object) {
        final boolean containsObject = universe.containsObject(object);
        assertTrue(universe.getObjectIds().contains(object) == containsObject,
                "This universe contains an object with a given ID if, and only if, "
                        + "the set of object IDs that this universe contains that object ID.");
        return containsObject;
    }

    private static Duration assertHistoryEndInvariants(final Universe universe) {
        final Duration historyEnd = universe.getHistoryEnd();

        assertNotNull(historyEnd, "Always have a history end time-stamp.");// guard
        assertThat("The history end is at or after the history start.", historyEnd,
                greaterThanOrEqualTo(universe.getHistoryStart()));
        assertFalse(universe.getObjectIds().isEmpty() && !ValueHistory.END_OF_TIME.equals(historyEnd),
                "The end of the history of an empty universe is the end of time.");

        return historyEnd;
    }

    private static Duration assertHistoryStartInvariants(final Universe universe) {
        final Duration historyStart = universe.getHistoryStart();

        assertNotNull(historyStart, "Always have a history start time-stamp.");
        return historyStart;
    }

    public static void assertInvariants(final Universe universe) {
        ObjectTest.assertInvariants(universe);// inherited

        assertAll(() -> assertHistoryStartInvariants(universe), () -> assertHistoryEndInvariants(universe),
                () -> assertObjectIdsInvariants(universe));
    }

    public static void assertInvariants(final Universe universe1, final Universe universe2) {
        ObjectTest.assertInvariants(universe1, universe2);// inherited
    }

    public static void assertInvariants(final Universe universe, @NonNull final UUID object) {
        assertAll(() -> assertObjectStateHistoryInvariants(universe, object),
                () -> assertLatestCommitInvariants(universe, object),
                () -> assertContainsObjectInvariants(universe, object));
    }

    public static void assertInvariants(final Universe universe, final UUID object, final Duration when) {
        // Do nothing
    }

    private static void assertInvariants(final Universe.Lockable lockable) {
        ObjectTest.assertInvariants(lockable);// inherited
        ComparableTest.assertInvariants(lockable);// inherited

        final Universe universe = lockable.getUniverse();

        assertNotNull(universe, "universe");// guard

        UniverseTest.assertInvariants(universe);
    }

    private static void assertInvariants(final Universe.Lockable lockable1, final Universe.Lockable lockable2) {
        ObjectTest.assertInvariants(lockable1, lockable2);// inherited
        ComparableTest.assertInvariants(lockable1, lockable2);// inherited
        ComparableTest.assertComparableConsistentWithEquals(lockable1, lockable2);
    }

    private static @Nullable Duration assertLatestCommitInvariants(@NonNull final Universe universe,
            @NonNull final UUID object) {
        final boolean containedObject = universe.containsObject(object);
        final SortedSet<Duration> transitionTimes = containedObject
                ? universe.getObjectStateHistory(object).getTransitionTimes()
                : Collections.emptySortedSet();

        final Duration latestCommit = universe.getLatestCommit(object);

        assertTrue(latestCommit != null == containedObject,
                "An object has a last committed state time-stamp if, and only if, it is a known object.");
        assertThat(
                "If an object is known, its last committed state time-stamp is one of the transition times of the state history of that object, or is the start of time, or is the end of time.",
                latestCommit, anyOf(is((Duration) null), in(transitionTimes), is(ValueHistory.START_OF_TIME),
                        is(ValueHistory.END_OF_TIME)));
        return latestCommit;
    }

    private static void assertObjectIdsInvariants(final Universe universe) {
        final Set<UUID> objectIds = universe.getObjectIds();

        assertNotNull(objectIds, "Always have a set of object IDs.");// guard

        for (final UUID object : objectIds) {
            assertNotNull(object, "The set of object IDs does not have a null element.");// guard
            assertInvariants(universe, object);
            final ValueHistory<ObjectState> objectStateHistory = assertObjectStateHistoryInvariants(universe, object);
            final Duration whenFirstState = universe.getWhenFirstState(object);

            assertNotNull(whenFirstState, "An object has a first state time-stamp if it is a known object.");
            assertFalse(objectStateHistory.isEmpty(), "A object state history for a known object is not empty.");// guard
            assertSame(objectStateHistory.getFirstTansitionTime(), whenFirstState,
                    "If an object is known, its first state time-stamp is the first transition time of the state history of that object.");
            assertNull(universe.getObjectState(object, whenFirstState.minusNanos(1L)),
                    "Known objects have an unknown state just before the first known state of the state history of that object.");
        }
    }

    private static @NonNull ValueHistory<ObjectState> assertObjectStateHistoryInvariants(final Universe universe,
            @NonNull final UUID object) {
        final ValueHistory<ObjectState> history = universe.getObjectStateHistory(object);

        assertNotNull(history, "A universe always has an object state history for a given object.");// guard
        ValueHistoryTest.assertInvariants(history);

        assertNull(history.getFirstValue(),
                "An object state history indicates that the object does not exist at the start of time.");
        assertFalse(!history.isEmpty() && !universe.containsObject(object),
                "The object state history for a given object is not empty only if the object is one of the known objects in this universe.");

        return history;
    }

    private static void assertUnknownObjectInvariants(final Universe universe, final UUID object) {
        assertAll(() -> assertThat("Not a known object ID", object, not(in(universe.getObjectIds()))),
                () -> assertTrue(assertObjectStateHistoryInvariants(universe, object).isEmpty(),
                        "unknown objects have an empty state history"),
                () -> assertNull(universe.getWhenFirstState(object),
                        "An object has a first state time-stamp only if it is a known object."),
                () -> assertNull(universe.getObjectState(object, DURATION_1),
                        "Unknown objects have an unknown state for all points in time."),
                () -> assertNull(universe.getObjectState(object, DURATION_2),
                        "Unknown objects have an unknown state for all points in time."));
    }

    private static void get(final Future<Void> future) {
        try {
            future.get();
        } catch (final InterruptedException e) {
            throw new AssertionError(e);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof AssertionError)
                throw (AssertionError) cause;
            else if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new AssertionError(e);
        }
    }

    private static void get(final List<Future<Void>> futures) {
        final List<Throwable> exceptions = new ArrayList<>(futures.size());
        for (final var future : futures) {
            try {
                get(future);
            } catch (Exception | AssertionError e) {
                exceptions.add(e);
            }
        }
        final int nExceptions = exceptions.size();
        if (0 < nExceptions) {
            final Throwable e = exceptions.get(0);
            for (int i = 1; i < nExceptions; ++i) {
                e.addSuppressed(exceptions.get(i));
            }
            if (e instanceof AssertionError)
                throw (AssertionError) e;
            else if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new AssertionError(e);
        }
    }

    static void putAndCommit(final Universe universe, final UUID object, final Duration when, final ObjectState state) {
        final TransactionListener listener = new TransactionListener() {

            @Override
            public void onAbort() {
                throw new AssertionError("Does not abort");
            }

            @Override
            public void onCommit() {
                // Do nothing
            }

            @Override
            public void onCreate(@NonNull final UUID object) {
                // Do nothing
            }

        };
        try (final Universe.Transaction transaction = universe.beginTransaction(listener);) {
            transaction.beginWrite(when);
            transaction.put(object, state);
            transaction.beginCommit();
        }
    }

    private static Future<Void> runInOtherThread(final CountDownLatch ready, final Runnable operation) {
        final CompletableFuture<Void> future = new CompletableFuture<Void>();
        final Thread thread = new Thread(() -> {
            try {
                ready.await();
                operation.run();
            } catch (final Throwable e) {
                future.completeExceptionally(e);
                return;
            }
            future.complete(null);
        });
        thread.start();
        return future;
    }

}
