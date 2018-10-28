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
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.collection.IsIn.isOneOf;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.NonNull;
import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.simulation.Universe.TransactionOpenness;

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
            final CountingTransactionListener listener = new CountingTransactionListener();

            beginTransaction(universe, listener);
        }

        @Test
        public void b() {
            final Universe universe = new Universe(DURATION_2);
            final CountingTransactionListener listener = new CountingTransactionListener();

            beginTransaction(universe, listener);
        }

        private Universe.Transaction beginTransaction(final Universe universe,
                final Universe.TransactionListener listener) {
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

        @Test
        public void b() {
            test(DURATION_2);
        }

        private void test(final Duration earliestTimeOfCompleteState) {
            final Universe universe = new Universe(earliestTimeOfCompleteState);

            assertInvariants(universe);

            assertSame(earliestTimeOfCompleteState, universe.getEarliestTimeOfCompleteState(),
                    "The earliest complete state time-stamp of this universe is "
                            + "the given earliest complete state time-stamp.");
            assertEquals(Collections.emptySet(), universe.getObjectIds(), "The set of object IDs is empty.");
            assertEquals(Collections.emptySet(), universe.getObjectIds(), "The set of IDs of object states is empty.");

            assertUnknownObjectInvariants(universe, OBJECT_A);
            assertUnknownObjectInvariants(universe, OBJECT_B);
        }
    }// class

    static class CountingTransactionListener implements Universe.TransactionListener {

        int aborts;
        int commits;

        final int getEnds() {
            return aborts + commits;
        }

        @Override
        public void onAbort() {
            assertEquals(0, aborts, "Aborts at most once");
            ++aborts;
        }

        @Override
        public void onCommit() {
            assertEquals(0, commits, "Commits at most once");
            ++commits;
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
        public class Abort {

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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2, Duration when3) {
                    assert when1.compareTo(when2) <= 0;
                    assert when2.compareTo(when3) <= 0;
                    assert when1.compareTo(when3) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final CountingTransactionListener listener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    putAndCommit(universe, object, when3, objectState2);
                    final Universe.Transaction readTransaction = universe.beginTransaction(listener);
                    readTransaction.getObjectState(object, when2);

                    abort(readTransaction);

                    assertEquals(1, listener.aborts, "Aborted");
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    assert when1.compareTo(when2) <= 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when2);

                    abort(transaction);
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    assert when1.compareTo(when2) <= 0;
                    final ObjectState objectState = new ObjectStateTest.TestObjectState(1);

                    final CountingTransactionListener writeListener = new CountingTransactionListener();
                    final CountingTransactionListener readListener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.beginWrite(when1);
                    writeTransaction.put(object, objectState);
                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object, when2);

                    abort(readTransaction);

                    assertEquals(1, readListener.aborts, "Read aborted.");
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when2);
                    transaction.put(object, objectState2);

                    abort(transaction);

                    assertAll(
                            () -> assertSame(objectState1, universe.getObjectState(object, when2),
                                    "Rolled back aborted write [values]"),
                            () -> assertEquals(Collections.singleton(when1),
                                    universe.getObjectStateHistory(object).getTransitionTimes(),
                                    "Rolled back aborted write [transition times]"));
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

                private void test(final Duration earliestTimeOfCompleteState, Duration when1, Duration when2,
                        Duration when3, UUID object) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);

                    final CountingTransactionListener readListener = new CountingTransactionListener();
                    final CountingTransactionListener writeListener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, state1);
                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.getObjectState(object, when1);
                    writeTransaction.beginWrite(when2);
                    writeTransaction.put(object, state2);
                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object, when3);// reads state2

                    abort(writeTransaction);

                    assertAll(() -> assertEquals(1, readListener.getEnds(), "Ended read transaction"),
                            () -> assertEquals(1, writeListener.getEnds(), "Ended write transaction"),
                            () -> assertEquals(1, writeListener.aborts, "Aborted write transaction"),
                            () -> assertEquals(1, readListener.aborts, "Aborted (invalidated) read transaction"));
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

                private void test(final Duration earliestTimeOfCompleteState, Duration when1, Duration when2,
                        Duration when3, UUID object) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);

                    final CountingTransactionListener listener1 = new CountingTransactionListener();
                    final CountingTransactionListener listener2 = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, state1);
                    final Universe.Transaction transaction1 = universe.beginTransaction(listener1);
                    transaction1.getObjectState(object, when1);
                    transaction1.beginWrite(when2);
                    transaction1.put(object, state2);
                    final Universe.Transaction transaction2 = universe.beginTransaction(listener2);
                    transaction2.getObjectState(object, when2);
                    transaction2.beginWrite(when3);
                    transaction2.put(object, state3);

                    abort(transaction1);

                    assertAll(() -> assertEquals(1, listener1.getEnds(), "Ended transaction 1"),
                            () -> assertEquals(1, listener2.getEnds(), "Ended transaction 2"),
                            () -> assertEquals(1, listener1.aborts, "Aborted transaction 1"),
                            () -> assertEquals(1, listener2.aborts, "Aborted (invalidated) transaction 2"));
                }

            }// class

            private void abort(Universe.Transaction transaction) {
                final boolean committed0 = transaction.getOpenness() == TransactionOpenness.COMMITTED;

                transaction.abort();

                assertInvariants(transaction);

                final Universe.TransactionOpenness openness = transaction.getOpenness();
                assertAll(
                        () -> assertThat("The transaction is either aborted or committed.", openness,
                                isOneOf(TransactionOpenness.COMMITTED, TransactionOpenness.ABORTED)),
                        () -> assertTrue(!committed0 || openness == TransactionOpenness.COMMITTED,
                                "If this transaction was committed, it remains committed."));
            }

            @Test
            public void empty() {
                final Universe universe = new Universe(DURATION_1);
                final CountingTransactionListener listener = new CountingTransactionListener();
                final Universe.Transaction transaction = universe.beginTransaction(listener);

                abort(transaction);

                assertEquals(1, listener.aborts, "Aborted");
            }

        }// class

        @Nested
        public class BeginCommit {

            @Nested
            public class AfterOutOfOrderPut {

                @Test
                public void a() {
                    test(DURATION_2, DURATION_1);
                }

                @Test
                public void b() {
                    test(DURATION_3, DURATION_2);
                }

                @Test
                public void near() {
                    test(DURATION_2, DURATION_2.minusNanos(1L));
                }

                @Test
                public void same() {
                    test(DURATION_2, DURATION_2);
                }

                private void test(final Duration when2, final Duration when1) {
                    assert when1.compareTo(when2) <= 0;
                    final UUID object = UniverseTest.OBJECT_A;
                    final Duration earliestCompleteState = when2;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final SortedMap<Duration, ObjectState> expectedObjectStateHistory = new TreeMap<>();
                    expectedObjectStateHistory.put(when2, objectState1);

                    final Universe universe = new Universe(earliestCompleteState);
                    putAndCommit(universe, object, when2, objectState1);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when1);
                    transaction.put(object, objectState2);

                    beginCommit(transaction);

                    assertAll(() -> assertTrue(0 < listener.getEnds(), "Ended transaction"),
                            () -> assertEquals(0, listener.commits, "Commits"),
                            () -> assertEquals(1, listener.aborts, "Aborts"));
                }

            }// class

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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2, Duration when3) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final CountingTransactionListener readListener = new CountingTransactionListener();
                    final CountingTransactionListener writeListener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);

                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object, when2);
                    readTransaction.beginCommit();

                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.getObjectState(object, when1);
                    writeTransaction.beginWrite(when3);
                    writeTransaction.put(object, objectState2);

                    writeTransaction.beginCommit();

                    assertAll(() -> assertEquals(1, writeListener.commits, "Write committed"),
                            () -> assertEquals(0, writeListener.aborts, "Write not aborted"),
                            () -> assertEquals(1, readListener.commits,
                                    "Read committed (subsequent write enabled commit)"),
                            () -> assertEquals(0, readListener.aborts, "Read not aborted"));
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when0,
                        Duration when1, Duration when2) {
                    final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final CountingTransactionListener listener1 = new CountingTransactionListener();
                    final CountingTransactionListener listener2 = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when0, objectState0);
                    final Universe.Transaction transaction1 = universe.beginTransaction(listener1);
                    transaction1.getObjectState(object, when0);
                    final Universe.Transaction transaction2 = universe.beginTransaction(listener2);
                    transaction2.getObjectState(object, when0);
                    transaction1.beginWrite(when2);
                    transaction1.put(object, objectState1);
                    transaction2.beginWrite(when1);
                    transaction2.put(object, objectState2);

                    beginCommit(transaction2);

                    assertTrue(0 < listener2.getEnds(), "Ended commit");
                    assertEquals(1, listener2.aborts, "Aborted commit");
                }

            }// class

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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when0,
                        Duration when1, Duration when2) {
                    final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final CountingTransactionListener listener1 = new CountingTransactionListener();
                    final CountingTransactionListener listener2 = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when0, objectState0);
                    final Universe.Transaction transaction1 = universe.beginTransaction(listener1);
                    transaction1.getObjectState(object, when0);
                    final Universe.Transaction transaction2 = universe.beginTransaction(listener2);
                    transaction2.getObjectState(object, when0);
                    transaction1.beginWrite(when1);
                    transaction1.put(object, objectState1);
                    transaction2.beginWrite(when1);
                    transaction2.put(object, objectState2);

                    beginCommit(transaction2);

                    assertTrue(0 < listener2.getEnds(), "Ended transaction");
                    assertEquals(1, listener2.aborts, "Aborted transaction");
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
                        UUID object1, UUID object2) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final CountingTransactionListener listener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestCompleteState);
                    putAndCommit(universe, object1, when1, objectState1);
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object1, when1);
                    transaction.beginWrite(when2);
                    transaction.put(object2, objectState2);

                    beginCommit(transaction);

                    assertAll(() -> assertTrue(0 < listener.getEnds(), "Ended transaction"),
                            () -> assertEquals(1, listener.commits, "Committed transaction"));
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final CountingTransactionListener listener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when2);

                    beginCommit(transaction);

                    assertAll(() -> assertEquals(0, listener.commits, "not committed"),
                            () -> assertEquals(0, listener.aborts, "not aborted"));
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    assert when1.compareTo(when2) <= 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final CountingTransactionListener writeListener = new CountingTransactionListener();
                    final CountingTransactionListener readListener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.beginWrite(when1);
                    writeTransaction.put(object, objectState1);
                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object, when2);

                    beginCommit(readTransaction);

                    assertAll(() -> assertEquals(0, readListener.aborts, "Read not aborted."),
                            () -> assertEquals(0, readListener.commits, "Read not committed."));
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2, Duration when3) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final CountingTransactionListener listener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    putAndCommit(universe, object, when3, objectState2);

                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when1);
                    transaction.getObjectState(object, when2);

                    beginCommit(transaction);

                    assertTrue(0 < listener.getEnds(), "Ended transaction");
                    assertEquals(1, listener.commits, "Committed transaction");
                }

            }// class

            @Nested
            public class AfterWriteWithPrehistoricDependency {

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
                    test(DURATION_2.minusNanos(1L), DURATION_2, DURATION_2);
                }

                private void test(final Duration when1, final Duration earliestCompleteState, final Duration when2) {
                    assert when1.compareTo(earliestCompleteState) < 0;
                    assert earliestCompleteState.compareTo(when2) <= 0;
                    final ObjectState objectState = new ObjectStateTest.TestObjectState(1);

                    final CountingTransactionListener listener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestCompleteState);
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(OBJECT_A, when1);
                    transaction.beginWrite(when2);
                    transaction.put(OBJECT_B, objectState);

                    beginCommit(transaction);

                    assertAll(() -> assertTrue(0 < listener.getEnds(), "Ended transaction"),
                            () -> assertEquals(0, listener.aborts, "Did not abort transaction"),
                            () -> assertEquals(1, listener.commits, "Committed transaction"));
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when0,
                        Duration when1, Duration when2) {
                    final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
                    final ObjectState objectState1 = null;// critical
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final CountingTransactionListener listener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when0, objectState0);
                    putAndCommit(universe, object, when1, objectState1);

                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when2);
                    transaction.put(object, objectState2);

                    beginCommit(transaction);

                    assertTrue(0 < listener.getEnds(), "Ended transaction");
                    assertEquals(1, listener.aborts, "Aborted commit");
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

                private void test(final Duration earliestTimeOfCompleteState, Duration when1, Duration when2,
                        Duration when3, Duration when4, UUID object1, UUID object2, UUID object3) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) < 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);
                    final ObjectStateTest.TestObjectState state4 = new ObjectStateTest.TestObjectState(4);
                    final ObjectStateTest.TestObjectState state5 = new ObjectStateTest.TestObjectState(5);

                    final CountingTransactionListener readListener = new CountingTransactionListener();
                    final CountingTransactionListener readWriteListener = new CountingTransactionListener();
                    final CountingTransactionListener writeListener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when1, state2);
                    putAndCommit(universe, object3, when1, state3);

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
                            () -> assertEquals(1, writeListener.commits, "Comitted write transaction"),
                            () -> assertEquals(1, readWriteListener.aborts,
                                    "Aborted (invalidated) read-write transaction"),
                            () -> assertEquals(1, readListener.aborts, "Aborted (invalidated) read transaction"));
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

                    final CountingTransactionListener listener = new CountingTransactionListener();

                    final Universe universe = new Universe(when);
                    putAndCommit(universe, object1, when, objectState1);
                    final ValueHistory<ObjectState> objectStateHistory1 = universe.getObjectStateHistory(object1);
                    final Universe.Transaction transaction2 = universe.beginTransaction(listener);
                    transaction2.beginWrite(when);
                    transaction2.put(object2, objectState2);

                    beginCommit(transaction2);

                    assertEquals(1, listener.commits, "Committed");
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final CountingTransactionListener readListener = new CountingTransactionListener();
                    final CountingTransactionListener writeListener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final Universe.Transaction writeTransaction = universe.beginTransaction(writeListener);
                    writeTransaction.beginWrite(when);
                    writeTransaction.put(object, objectState1);
                    final Universe.Transaction readTransaction = universe.beginTransaction(readListener);
                    readTransaction.getObjectState(object, when);
                    readTransaction.beginCommit();

                    beginCommit(writeTransaction);

                    assertAll(() -> assertEquals(0, writeListener.aborts, "Write not aborted."),
                            () -> assertEquals(1, writeListener.commits, "Write committed."),
                            () -> assertEquals(0, readListener.aborts, "Read not aborted."), () -> assertEquals(1,
                                    readListener.commits, "Read committed (triggered by commit of write)."));
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

                private void test(final Duration earliestTimeOfCompleteState, Duration when1, Duration when2,
                        Duration when3, Duration when4, UUID object1, UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) <= 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);

                    final CountingTransactionListener readListener = new CountingTransactionListener();
                    final CountingTransactionListener writeListener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when2, state2);
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
                            () -> assertEquals(1, writeListener.commits, "Comitted write transaction"),
                            () -> assertEquals(1, readListener.aborts, "Aborted (invalidated) read transaction"));
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

                private void test(final Duration earliestTimeOfCompleteState, Duration when1, Duration when2,
                        Duration when3, Duration when4, UUID object1, UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) <= 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);

                    final CountingTransactionListener readListener = new CountingTransactionListener();
                    final CountingTransactionListener writeListener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when2, state2);

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
                            () -> assertEquals(1, writeListener.commits, "Comitted write transaction"),
                            () -> assertEquals(1, readListener.aborts, "Aborted (invalidated) read transaction"));
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

                private void test(final Duration earliestTimeOfCompleteState, Duration when1, Duration when2,
                        Duration when3, Duration when4, Duration when5, UUID object1, UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) < 0;
                    assert when4.compareTo(when5) < 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);
                    final ObjectStateTest.TestObjectState state4 = new ObjectStateTest.TestObjectState(4);

                    final CountingTransactionListener readListener = new CountingTransactionListener();
                    final CountingTransactionListener writeListener1 = new CountingTransactionListener();
                    final CountingTransactionListener writeListener2 = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when2, state2);
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
                            () -> assertEquals(1, writeListener1.commits, "Comitted write transaction 1"),
                            () -> assertEquals(1, writeListener1.commits, "Comitted write transaction 2"),
                            () -> assertEquals(1, readListener.aborts, "Aborted read transaction"));
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

                private void test(final Duration earliestTimeOfCompleteState, UUID objectA, UUID objectB,
                        Duration whenA1, Duration whenB1, Duration whenB2, Duration whenA2, Duration whenA3,
                        Duration whenB3) {
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

                    final CountingTransactionListener listenerA = new CountingTransactionListener();
                    final CountingTransactionListener listenerB = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, objectA, whenA1, objectStateA1);
                    putAndCommit(universe, objectB, whenB1, objectStateB1);

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

                    assertAll(() -> assertEquals(1, listenerA.commits, "Committed A."),
                            () -> assertEquals(1, listenerB.commits, "Committed B."));
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

                private void test(final Duration earliestTimeOfCompleteState, UUID objectA, UUID objectB,
                        Duration whenA1, Duration whenB1, Duration whenB2, Duration whenA2, Duration whenA3,
                        Duration whenB3) {
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

                    final CountingTransactionListener listenerA = new CountingTransactionListener();
                    final CountingTransactionListener listenerB = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, objectA, whenA1, objectStateA1);
                    putAndCommit(universe, objectB, whenB1, objectStateB1);

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
                            () -> assertEquals(0, listenerA.commits, "Has not committed A."),
                            () -> assertEquals(0, listenerB.commits, "Has not committed B."));
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

                private void test(final Duration earliestTimeOfCompleteState, Duration when1, Duration when2,
                        Duration when3, Duration when4, Duration when5, UUID object1, UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) < 0;
                    assert when4.compareTo(when5) < 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);
                    final ObjectStateTest.TestObjectState state4 = new ObjectStateTest.TestObjectState(4);

                    final CountingTransactionListener listener1 = new CountingTransactionListener();
                    final CountingTransactionListener listener2 = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when2, state2);

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

                    assertAll(() -> assertEquals(1, listener1.getEnds(), "Ended transaction 1"),
                            () -> assertTrue(0 < listener2.getEnds(), "Ended transaction 2"),
                            () -> assertEquals(1, listener2.commits, "Comitted transaction 2"),
                            () -> assertEquals(1, listener1.aborts, "Aborted (invalidated) transaction 1"));
                    assertAll(
                            () -> assertSame(state1, universe.getObjectState(object1, when5),
                                    "Rolled back aborted write [values]"),
                            () -> assertEquals(Collections.singleton(when1),
                                    universe.getObjectStateHistory(object1).getTransitionTimes(),
                                    "Rolled back aborted write [transition times]"));
                }

            }// class

            @Nested
            public class SuccessiveStates2 {
                @Test
                public void a() {
                    test(DURATION_1, DURATION_2);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3);
                }

                @Test
                public void near() {
                    test(DURATION_1, DURATION_1.plusNanos(1));
                }

                private void test(final Duration when1, final Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final UUID object = UniverseTest.OBJECT_A;
                    final Duration earliestCompleteState = when2;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final ModifiableValueHistory<ObjectState> expectedObjectStateHistory = new ModifiableValueHistory<>();
                    expectedObjectStateHistory.appendTransition(when1, objectState1);
                    expectedObjectStateHistory.appendTransition(when2, objectState2);

                    final Universe universe = new Universe(earliestCompleteState);
                    putAndCommit(universe, object, when1, objectState1);

                    final CountingTransactionListener writeListener = new CountingTransactionListener();

                    final Universe.Transaction transaction = universe.beginTransaction(writeListener);
                    transaction.getObjectState(object, when1);
                    transaction.beginWrite(when2);
                    transaction.put(object, objectState2);

                    beginCommit(transaction);

                    assertAll(() -> assertEquals(0, writeListener.aborts, "Write did not abort."),
                            () -> assertEquals(1, writeListener.commits, "Write committed."));
                    assertEquals(Collections.singleton(object), universe.getObjectIds(), "Object IDs.");
                    assertEquals(expectedObjectStateHistory, universe.getObjectStateHistory(object),
                            "Object state history.");
                    assertSame(objectState1, universe.getObjectState(object, when2.minusNanos(1L)),
                            "The state of an object at a given point in time is "
                                    + "the state it had at the latest state transition "
                                    + "at or before that point in time (just before second)");
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
                final Duration earliestTimeOfCompleteState = DURATION_1;
                final UUID object = OBJECT_A;
                final Duration when0 = DURATION_2;
                final Duration when1 = DURATION_3;

                final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
                final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                final CountingTransactionListener listener1 = new CountingTransactionListener();
                final CountingTransactionListener listener2 = new CountingTransactionListener();

                final Universe universe = new Universe(earliestTimeOfCompleteState);
                putAndCommit(universe, object, when0, objectState0);
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

                assertEquals(0, listener2.commits, "Did not commit second transaction");
                assertEquals(1, listener2.aborts, "Aborted second transaction");
            }

            @Test
            public void immediately() {
                final Universe universe = new Universe(DURATION_1);

                final CountingTransactionListener listener = new CountingTransactionListener();
                final Universe.Transaction transaction = universe.beginTransaction(listener);

                beginCommit(transaction);

                assertEquals(0, listener.aborts, "Did not abort");
                assertEquals(1, listener.commits, "Committed");
            }

            @Test
            public void putOk() {
                final Duration earliestTimeOfCompleteState = DURATION_1;
                final UUID object = OBJECT_A;
                final Duration when = DURATION_2;

                final Universe universe = new Universe(earliestTimeOfCompleteState);
                final CountingTransactionListener listener = new CountingTransactionListener();
                final Universe.Transaction transaction = universe.beginTransaction(listener);
                final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
                transaction.beginWrite(when);
                transaction.put(object, objectState);

                beginCommit(transaction);

                assertEquals(0, listener.aborts, "Did not abort");
                assertEquals(1, listener.commits, "Committed");
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

                private final void test(Duration earliestTimeOfCompleteState, Duration when) {
                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    Universe.Transaction transaction = universe.beginTransaction(listener);

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

                private final void test(Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when1);

                    beginWrite(transaction, when2);
                }
            }// class

            private final void beginWrite(Universe.Transaction transaction, Duration when) {
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final CountingTransactionListener listener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when2);
                    transaction.put(object, objectState2);

                    close(transaction);

                    assertAll(() -> assertEquals(1, listener.getEnds(), "Ended the transaction"),
                            () -> assertEquals(1, listener.aborts, "Aborted the transaction"),
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2, Duration when3) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final CountingTransactionListener listener = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    putAndCommit(universe, object, when3, objectState2);

                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when1);
                    transaction.getObjectState(object, when2);
                    transaction.beginCommit();

                    close(transaction);
                }

            }// class

            @Test
            public void afterAbort() {
                final CountingTransactionListener listener = new CountingTransactionListener();

                final Universe universe = new Universe(DURATION_1);
                final Universe.Transaction transaction = universe.beginTransaction(listener);
                transaction.abort();

                close(transaction);

                assertAll(() -> assertEquals(0, listener.commits, "Not committed"),
                        () -> assertEquals(1, listener.aborts, "Aborted"));
            }

            private void close(final Universe.Transaction transaction) {
                final Universe.TransactionOpenness openness0 = transaction.getOpenness();

                transaction.close();

                assertInvariants(transaction);
                final Universe.TransactionOpenness openness = transaction.getOpenness();
                assertThat("This transaction is aborted committing or committed.", openness,
                        isOneOf(Universe.TransactionOpenness.ABORTED, Universe.TransactionOpenness.COMMITTED,
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
                final CountingTransactionListener listener = new CountingTransactionListener();
                final Universe.Transaction transaction = universe.beginTransaction(listener);

                close(transaction);

                assertAll(() -> assertEquals(1, listener.getEnds(), "Ended the transaction"),
                        () -> assertEquals(1, listener.aborts, "Aborted the transaction"));
            }

        }// class

        @Nested
        public class GetObjectState {

            @Nested
            public class AfterPutAndCommit1 {

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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    final ObjectStateId id2 = new ObjectStateId(object, when2);
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);

                    final ObjectState objectState2 = getObjectState(transaction, object, when2);

                    assertSame(objectState1, objectState2, "objectState");
                    assertEquals(Collections.singletonMap(id2, objectState1), transaction.getObjectStatesRead(),
                            "objectStatesRead");
                }

            }// class

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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when) {
                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);

                    getObjectState(transaction, object, when);
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    assert when1.compareTo(when2) <= 0;
                    final ObjectStateId id2 = new ObjectStateId(object, when2);
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final CountingTransactionListener listener1 = new CountingTransactionListener();
                    final CountingTransactionListener listener2 = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
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

            private ObjectState getObjectState(final Universe.Transaction transaction, UUID object, Duration when) {
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
                        () -> assertThat("has key.", id, isIn(transaction.getObjectStatesRead().keySet())),
                        () -> assertSame(objectState, transaction.getObjectStatesRead().get(id), "state"));

                return objectState;
            }

        }// class

        @Nested
        public class Put {

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
                        UUID object1, UUID object2) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final Universe universe = new Universe(earliestCompleteState);
                    putAndCommit(universe, object1, when1, objectState1);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object1, when1);
                    transaction.beginWrite(when2);

                    put(transaction, object2, objectState2);
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                        Duration when2) {
                    assert when1.compareTo(when2) < 0;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);
                    final ModifiableValueHistory<ObjectState> expectedHistory = new ModifiableValueHistory<>();
                    expectedHistory.appendTransition(when1, objectState1);
                    expectedHistory.appendTransition(when2, objectState2);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object, when1);
                    transaction.beginWrite(when2);

                    put(transaction, object, objectState2);

                    assertAll(() -> assertEquals(expectedHistory, universe.getObjectStateHistory(object),
                            "Object state history"));
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);
                    final ModifiableValueHistory<ObjectState> expectedHistory = new ModifiableValueHistory<>();
                    expectedHistory.appendTransition(when, objectState1);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when, objectState1);
                    final CountingTransactionListener listener = new CountingTransactionListener();
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
            public class Call1 {

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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when) {
                    final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
                    final ModifiableValueHistory<ObjectState> expectedHistory = new ModifiableValueHistory<>();
                    expectedHistory.appendTransition(when, objectState);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when);

                    put(transaction, object, objectState);

                    assertAll(() -> assertEquals(Collections.singleton(object), universe.getObjectIds(), "Object IDs"),
                            () -> assertEquals(expectedHistory, universe.getObjectStateHistory(object),
                                    "Object state history"));
                }

            }// class

            @Nested
            public class OutOfOrderStates2 {

                @Test
                public void a() {
                    test(DURATION_2, DURATION_1);
                }

                @Test
                public void b() {
                    test(DURATION_3, DURATION_2);
                }

                @Test
                public void near() {
                    test(DURATION_2, DURATION_2.minusNanos(1L));
                }

                @Test
                public void same() {
                    test(DURATION_2, DURATION_2);
                }

                private void test(final Duration when2, final Duration when1) {
                    assert when1.compareTo(when2) <= 0;
                    final UUID object = UniverseTest.OBJECT_A;
                    final Duration earliestCompleteState = when2;
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

                    final SortedMap<Duration, ObjectState> expectedObjectStateHistory = new TreeMap<>();
                    expectedObjectStateHistory.put(when2, objectState1);

                    final Universe universe = new Universe(earliestCompleteState);
                    putAndCommit(universe, object, when2, objectState1);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.beginWrite(when1);

                    put(transaction, object, objectState2);
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

                private void test(final Duration earliestTimeOfCompleteState, Duration when1, Duration when2,
                        Duration when3, Duration when4, UUID object1, UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) <= 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);

                    final CountingTransactionListener listener1 = new CountingTransactionListener();
                    final CountingTransactionListener listener2 = new CountingTransactionListener();

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when2, state2);
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
            public class PrehistoricDependency {

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
                    test(DURATION_2.minusNanos(1L), DURATION_2, DURATION_2);
                }

                private void test(final Duration when1, final Duration earliestCompleteState, final Duration when2) {
                    final ObjectState objectState = new ObjectStateTest.TestObjectState(1);

                    final Universe universe = new Universe(earliestCompleteState);
                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(OBJECT_A, when1);
                    transaction.beginWrite(when2);

                    put(transaction, OBJECT_B, objectState);
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
                        final Duration when3, UUID object1, UUID object2, UUID object3) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectState objectState3 = new ObjectStateTest.TestObjectState(3);

                    final Universe universe = new Universe(earliestCompleteState);
                    putAndCommit(universe, object1, when1, objectState1);
                    putAndCommit(universe, object2, when2, objectState2);

                    final CountingTransactionListener listener = new CountingTransactionListener();
                    final Universe.Transaction transaction = universe.beginTransaction(listener);
                    transaction.getObjectState(object2, when2);
                    transaction.beginWrite(when3);

                    put(transaction, object3, objectState3);
                }

            }// class

            private void put(final Universe.Transaction transaction, UUID object, ObjectState state) {
                transaction.put(object, state);

                assertInvariants(transaction);
                assertThat("The method records the given state as one of the states written.",
                        transaction.getObjectStatesWritten(), hasEntry(object, state));
            }
        }// class

        private static Map<UUID, ObjectStateId> assertDependenciesInvariants(Universe.Transaction transaction) {
            final Map<UUID, ObjectStateId> dependencies = transaction.getDependencies();
            assertNotNull(dependencies, "Always has a dependency map.");// guard
            final Set<ObjectStateId> objectStatesRead = transaction.getObjectStatesRead().keySet();
            for (var entry : dependencies.entrySet()) {
                final UUID object = entry.getKey();
                final ObjectStateId objectStateId = entry.getValue();
                assertAll(() -> assertNotNull(object, "The dependency map does not have a null key."), // guard
                        () -> assertNotNull(objectStateId, "The dependency map does not have null values."));// guard

                ObjectStateIdTest.assertInvariants(objectStateId);
                assertSame(object, objectStateId.getObject(),
                        "Each object ID key of the dependency map maps to a value that has that same object ID as the object of the object state ID.");
                assertThat(
                        "The collection of values of the dependencies map is a sub set of the keys of the object states read.",
                        objectStateId, isIn(objectStatesRead));
            }
            for (ObjectStateId objectStateRead : objectStatesRead) {
                final UUID object = objectStateRead.getObject();
                final Duration when = objectStateRead.getWhen();
                assertThat("Each object of an object state read has an entry in the dependencies map.", object,
                        isIn(dependencies.keySet()));// guard
                assertThat("The time-stamp of each object state ID key of the object states read map is "
                        + "at or after the time-stamp of the value in the dependencies map for the object ID of that object state ID.",
                        when, greaterThanOrEqualTo(dependencies.get(object).getWhen()));
            }
            return dependencies;
        }

        public static void assertInvariants(Universe.Transaction transaction) {
            ObjectTest.assertInvariants(transaction);// inherited

            final Universe universe = transaction.getUniverse();

            assertNotNull(universe, "universe");// guard

            assertAll(() -> UniverseTest.assertInvariants(universe),
                    () -> assertObjectStatesReadInvariants(transaction),
                    () -> assertObjectStatesWrittenInvariants(transaction),
                    () -> assertDependenciesInvariants(transaction), () -> assertOpennessInvariants(transaction));
        }

        public static void assertInvariants(Universe.Transaction transaction1, Universe.Transaction transaction2) {
            ObjectTest.assertInvariants(transaction1, transaction2);// inherited
        }

        private static Map<ObjectStateId, ObjectState> assertObjectStatesReadInvariants(
                Universe.Transaction transaction) {
            final Map<ObjectStateId, ObjectState> objectStatesRead = transaction.getObjectStatesRead();
            assertNotNull(objectStatesRead, "Always have a map of object states read.");// guard
            for (var entry : objectStatesRead.entrySet()) {
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

        private static Map<UUID, ObjectState> assertObjectStatesWrittenInvariants(Universe.Transaction transaction) {
            final Duration when = transaction.getWhen();

            final Map<UUID, ObjectState> objectStatesWritten = transaction.getObjectStatesWritten();

            assertNotNull(objectStatesWritten, "Always have a map of object states written.");// guard
            assertFalse(when == null && !objectStatesWritten.isEmpty(),
                    "The map of object states written is empty if this transaction is in read mode.");

            for (var entry : objectStatesWritten.entrySet()) {
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

        private static Universe.TransactionOpenness assertOpennessInvariants(Universe.Transaction transaction) {
            final Universe.TransactionOpenness openness = transaction.getOpenness();
            assertNotNull(openness, "openness");
            return openness;
        }

        private static void putAndCommit(final Universe universe, UUID object, Duration when, ObjectState state) {
            final Universe.TransactionListener listener = new Universe.TransactionListener() {

                @Override
                public void onAbort() {
                    throw new AssertionError("Does not abort");
                }

                @Override
                public void onCommit() {
                    // Do nothing
                }

            };
            final Universe.Transaction transaction = universe.beginTransaction(listener);
            transaction.beginWrite(when);
            transaction.put(object, state);
            transaction.beginCommit();
        }
    }// class

    private static final UUID OBJECT_A = ObjectStateIdTest.OBJECT_A;
    private static final UUID OBJECT_B = ObjectStateIdTest.OBJECT_B;
    private static final UUID OBJECT_C = UUID.randomUUID();
    private static final Duration DURATION_1 = Duration.ofSeconds(13);
    private static final Duration DURATION_2 = Duration.ofSeconds(17);
    private static final Duration DURATION_3 = Duration.ofSeconds(23);
    private static final Duration DURATION_4 = Duration.ofSeconds(29);
    private static final Duration DURATION_5 = Duration.ofSeconds(31);
    private static final Duration DURATION_6 = Duration.ofSeconds(37);
    private static final Duration DURATION_7 = Duration.ofSeconds(43);
    private static final Duration DURATION_8 = Duration.ofSeconds(47);

    public static void assertInvariants(Universe universe) {
        ObjectTest.assertInvariants(universe);// inherited

        final Duration earliestTimeOfCompleteState = universe.getEarliestTimeOfCompleteState();

        assertNotNull(earliestTimeOfCompleteState, "Always have a earliest complete state time-stamp.");

        assertObjectIdsInvariants(universe);
    }

    public static void assertInvariants(Universe universe1, Universe universe2) {
        ObjectTest.assertInvariants(universe1, universe2);// inherited
    }

    public static void assertInvariants(Universe universe, UUID object, Duration when) {
        // Do nothing
    }

    private static void assertObjectIdsInvariants(Universe universe) {
        final Set<UUID> objectIds = universe.getObjectIds();

        assertNotNull(objectIds, "Always have a set of object IDs.");// guard

        for (UUID object : objectIds) {
            assertNotNull(object, "The set of object IDs does not have a null element.");// guard
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

    private static @NonNull ValueHistory<ObjectState> assertObjectStateHistoryInvariants(Universe universe,
            @NonNull UUID object) {
        final ValueHistory<ObjectState> history = universe.getObjectStateHistory(object);

        assertNotNull(history, "A universe always has an object state history for a given object.");// guard
        ValueHistoryTest.assertInvariants(history);

        assertFalse(!history.isEmpty() && !universe.getObjectIds().contains(object),
                "The object state history for a given object is not empty only if the object is one of the {@linkplain #getObjectIds() known objects} in this universe.");

        return history;
    }

    private static void assertUnknownObjectInvariants(Universe universe, UUID object) {
        assertAll(() -> assertThat("Not a known object ID", object, not(isIn(universe.getObjectIds()))),
                () -> assertTrue(assertObjectStateHistoryInvariants(universe, object).isEmpty(),
                        "unknown objects have an empty state history"),
                () -> assertNull(universe.getWhenFirstState(object),
                        "An object has a first state time-stamp only if it is a known object."),
                () -> assertNull(universe.getObjectState(object, DURATION_1),
                        "Unknown objects have an unknown state for all points in time."),
                () -> assertNull(universe.getObjectState(object, DURATION_2),
                        "Unknown objects have an unknown state for all points in time."));
    }

}
