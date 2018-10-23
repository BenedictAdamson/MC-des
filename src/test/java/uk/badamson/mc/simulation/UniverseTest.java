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
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AnyOf.anyOf;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.badamson.mc.ObjectTest;

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

            beginTransaction(universe);
        }

        @Test
        public void b() {
            final Universe universe = new Universe(DURATION_2);

            beginTransaction(universe);
        }

        private Universe.Transaction beginTransaction(final Universe universe) {
            final Universe.Transaction transaction = universe.beginTransaction();

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
                    () -> assertFalse(transaction.didBeginCommit(),
                            "The began commit flag of the returned transaction is clear."),
                    () -> assertNull(transaction.getWhen(), "The returned transaction is in in read mode."));

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
            assertUnknownObjectStateInvariants(universe, new ObjectStateId(OBJECT_A, DURATION_1));
            assertUnknownObjectStateInvariants(universe, new ObjectStateId(OBJECT_B, DURATION_2));
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
                    final Universe.Transaction transaction = universe.beginTransaction();
                    transaction.beginWrite(when1);
                    transaction.put(object, objectState2);

                    final AtomicBoolean committed = new AtomicBoolean(false);
                    final AtomicBoolean aborted = new AtomicBoolean(false);

                    beginCommit(transaction, () -> committed.set(true), () -> aborted.set(true));

                    assertAll(() -> assertTrue(committed.get() || aborted.get(), "Ended transaction"),
                            () -> assertTrue(aborted.get(), "Aborted transaction"));
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

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when0, objectState0);
                    final Universe.Transaction transaction1 = universe.beginTransaction();
                    transaction1.getObjectState(object, when0);
                    final Universe.Transaction transaction2 = universe.beginTransaction();
                    transaction2.getObjectState(object, when0);
                    transaction1.beginWrite(when2);
                    transaction1.put(object, objectState1);
                    transaction2.beginWrite(when1);
                    transaction2.put(object, objectState2);

                    final AtomicBoolean committed2 = new AtomicBoolean(false);
                    final AtomicBoolean aborted2 = new AtomicBoolean(false);

                    beginCommit(transaction2, () -> committed2.set(true), () -> aborted2.set(true));

                    assertTrue(committed2.get() || aborted2.get(), "Ended commit");
                    assertTrue(aborted2.get(), "Aborted commit");
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

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when0, objectState0);
                    final Universe.Transaction transaction1 = universe.beginTransaction();
                    transaction1.getObjectState(object, when0);
                    final Universe.Transaction transaction2 = universe.beginTransaction();
                    transaction2.getObjectState(object, when0);
                    transaction1.beginWrite(when1);
                    transaction1.put(object, objectState1);
                    transaction2.beginWrite(when1);
                    transaction2.put(object, objectState2);

                    final AtomicBoolean committed2 = new AtomicBoolean(false);
                    final AtomicBoolean aborted2 = new AtomicBoolean(false);

                    beginCommit(transaction2, () -> committed2.set(true), () -> aborted2.set(true));

                    assertTrue(committed2.get() || aborted2.get(), "Ended transaction");
                    assertTrue(aborted2.get(), "Aborted transaction");
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

                    final Universe universe = new Universe(earliestCompleteState);
                    putAndCommit(universe, object1, when1, objectState1);
                    final Universe.Transaction transaction = universe.beginTransaction();
                    transaction.getObjectState(object1, when1);
                    transaction.beginWrite(when2);
                    transaction.put(object2, objectState2);

                    final AtomicBoolean committed = new AtomicBoolean(false);
                    final AtomicBoolean aborted = new AtomicBoolean(false);

                    beginCommit(transaction, () -> committed.set(true), () -> aborted.set(true));

                    assertAll(() -> assertTrue(committed.get() || aborted.get(), "Ended transaction"),
                            () -> assertTrue(committed.get(), "Committed transaction"));
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

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    final Universe.Transaction transaction = universe.beginTransaction();
                    transaction.getObjectState(object, when2);

                    final AtomicBoolean committed = new AtomicBoolean(false);
                    final AtomicBoolean aborted = new AtomicBoolean(false);

                    beginCommit(transaction, () -> committed.set(true), () -> aborted.set(true));

                    assertAll(() -> assertFalse(committed.get(), "not committed"),
                            () -> assertFalse(aborted.get(), "not aborted"));
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

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final Universe.Transaction writeTransaction = universe.beginTransaction();
                    writeTransaction.beginWrite(when1);
                    writeTransaction.put(object, objectState1);
                    final Universe.Transaction readTransaction = universe.beginTransaction();
                    readTransaction.getObjectState(object, when2);

                    final AtomicBoolean readCommitted = new AtomicBoolean(false);
                    final AtomicBoolean readAborted = new AtomicBoolean(false);

                    beginCommit(readTransaction, () -> readCommitted.set(true), () -> readAborted.set(true));

                    assertAll(() -> assertFalse(readAborted.get(), "Read not aborted."),
                            () -> assertFalse(readCommitted.get(), "Read not committed."));
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

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when1, objectState1);
                    putAndCommit(universe, object, when3, objectState2);

                    final Universe.Transaction transaction = universe.beginTransaction();
                    transaction.getObjectState(object, when1);
                    transaction.getObjectState(object, when2);

                    final AtomicBoolean committed = new AtomicBoolean(false);
                    final AtomicBoolean aborted = new AtomicBoolean(false);

                    beginCommit(transaction, () -> committed.set(true), () -> aborted.set(true));

                    assertTrue(committed.get() || aborted.get(), "Ended transaction");
                    assertTrue(committed.get(), "Committed transaction");
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

                    final Universe universe = new Universe(earliestCompleteState);
                    final Universe.Transaction transaction = universe.beginTransaction();
                    transaction.getObjectState(OBJECT_A, when1);
                    transaction.beginWrite(when2);
                    transaction.put(OBJECT_B, objectState);

                    final AtomicBoolean committed = new AtomicBoolean(false);
                    final AtomicBoolean aborted = new AtomicBoolean(false);

                    beginCommit(transaction, () -> committed.set(true), () -> aborted.set(true));

                    assertAll(() -> assertTrue(committed.get() || aborted.get(), "Ended transaction"),
                            () -> assertTrue(committed.get(), "Committed transaction"));
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

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object, when0, objectState0);
                    putAndCommit(universe, object, when1, objectState1);

                    final Universe.Transaction transaction = universe.beginTransaction();
                    transaction.beginWrite(when2);
                    transaction.put(object, objectState2);

                    final AtomicBoolean committed = new AtomicBoolean(false);
                    final AtomicBoolean aborted = new AtomicBoolean(false);

                    beginCommit(transaction, () -> committed.set(true), () -> aborted.set(true));

                    assertTrue(committed.get() || aborted.get(), "Ended transaction");
                    assertTrue(aborted.get(), "Aborted commit");
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

                    final Universe universe = new Universe(when);
                    putAndCommit(universe, object1, when, objectState1);
                    final ValueHistory<ObjectState> objectStateHistory1 = universe.getObjectStateHistory(object1);
                    final Universe.Transaction transaction2 = universe.beginTransaction();
                    transaction2.beginWrite(when);
                    transaction2.put(object2, objectState2);
                    final AtomicBoolean committed = new AtomicBoolean(false);

                    transaction2.beginCommit(() -> committed.set(true), () -> committed.set(false));

                    assertTrue(committed.get(), "Committed");
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when1) {
                    final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

                    final AtomicBoolean readCommitted = new AtomicBoolean(false);
                    final AtomicBoolean readAborted = new AtomicBoolean(false);
                    final AtomicBoolean writeCommitted = new AtomicBoolean(false);
                    final AtomicBoolean writeAborted = new AtomicBoolean(false);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final Universe.Transaction writeTransaction = universe.beginTransaction();
                    writeTransaction.beginWrite(when1);
                    writeTransaction.put(object, objectState1);
                    final Universe.Transaction readTransaction = universe.beginTransaction();
                    readTransaction.getObjectState(object, when1);
                    readTransaction.beginCommit(() -> readCommitted.set(true), () -> readAborted.set(true));
                    assert !readCommitted.get() && !readAborted.get();

                    beginCommit(writeTransaction, () -> writeCommitted.set(true), () -> writeAborted.set(true));

                    assertAll(() -> assertFalse(writeAborted.get(), "Write not aborted."),
                            () -> assertTrue(writeCommitted.get(), "Write committed."),
                            () -> assertFalse(readAborted.get(), "Read not aborted."),
                            () -> assertTrue(readCommitted.get(), "Read committed (triggered by commit of write)."));
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

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when2, state2);
                    final Universe.Transaction readTransaction = universe.beginTransaction();
                    readTransaction.getObjectState(object1, when1);
                    readTransaction.getObjectState(object2, when4);// reads state2
                    final Universe.Transaction writeTransaction = universe.beginTransaction();
                    writeTransaction.getObjectState(object2, when2);
                    writeTransaction.beginWrite(when3);
                    writeTransaction.put(object2, state3);

                    final AtomicBoolean committedRead = new AtomicBoolean(false);
                    final AtomicBoolean abortedRead = new AtomicBoolean(false);
                    final AtomicBoolean committedWrite = new AtomicBoolean(false);
                    final AtomicBoolean abortedWrite = new AtomicBoolean(false);

                    beginCommit(writeTransaction, () -> committedWrite.set(true), () -> abortedWrite.set(true));
                    beginCommit(readTransaction, () -> committedRead.set(true), () -> abortedRead.set(true));

                    assertAll(() -> assertTrue(committedRead.get() || abortedRead.get(), "Ended read transaction"),
                            () -> assertTrue(committedWrite.get() || abortedWrite.get(), "Ended write transaction"),
                            () -> assertTrue(committedWrite.get(), "Comitted write transaction"),
                            () -> assertTrue(abortedRead.get(), "Aborted (invalidated) read transaction"));
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

                    final AtomicBoolean committedRead = new AtomicBoolean(false);
                    final AtomicBoolean abortedRead = new AtomicBoolean(false);
                    final AtomicBoolean committedWrite = new AtomicBoolean(false);
                    final AtomicBoolean abortedWrite = new AtomicBoolean(false);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when2, state2);

                    final Universe.Transaction readTransaction = universe.beginTransaction();
                    readTransaction.getObjectState(object1, when1);
                    readTransaction.getObjectState(object2, when4);// reads state2
                    readTransaction.beginCommit(() -> committedRead.set(true), () -> abortedRead.set(true));

                    final Universe.Transaction writeTransaction = universe.beginTransaction();
                    writeTransaction.getObjectState(object2, when2);
                    writeTransaction.beginWrite(when3);
                    writeTransaction.put(object2, state3);

                    beginCommit(writeTransaction, () -> committedWrite.set(true), () -> abortedWrite.set(true));

                    assertAll(() -> assertTrue(committedRead.get() || abortedRead.get(), "Ended read transaction"),
                            () -> assertTrue(committedWrite.get() || abortedWrite.get(), "Ended write transaction"),
                            () -> assertTrue(committedWrite.get(), "Comitted write transaction"),
                            () -> assertTrue(abortedRead.get(), "Aborted (invalidated) read transaction"));
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

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when2, state2);
                    final Universe.Transaction readTransaction = universe.beginTransaction();
                    readTransaction.getObjectState(object1, when1);
                    readTransaction.getObjectState(object2, when5);// reads state2
                    final Universe.Transaction writeTransaction1 = universe.beginTransaction();
                    writeTransaction1.getObjectState(object2, when2);
                    writeTransaction1.beginWrite(when3);
                    writeTransaction1.put(object2, state3);
                    final Universe.Transaction writeTransaction2 = universe.beginTransaction();
                    writeTransaction2.getObjectState(object2, when3);// reads state3
                    writeTransaction2.beginWrite(when4);
                    writeTransaction2.put(object2, state4);

                    final AtomicInteger readCommits = new AtomicInteger(0);
                    final AtomicInteger readAborts = new AtomicInteger(0);
                    final AtomicBoolean committedWrite1 = new AtomicBoolean(false);
                    final AtomicBoolean abortedWrite1 = new AtomicBoolean(false);
                    final AtomicBoolean committedWrite2 = new AtomicBoolean(false);
                    final AtomicBoolean abortedWrite2 = new AtomicBoolean(false);

                    /*
                     * Tough test: when the writes commit, there are call-backs for the reader
                     * waiting.
                     */
                    beginCommit(readTransaction, () -> readCommits.incrementAndGet(),
                            () -> readAborts.incrementAndGet());
                    beginCommit(writeTransaction1, () -> committedWrite1.set(true), () -> abortedWrite1.set(true));
                    beginCommit(writeTransaction2, () -> committedWrite2.set(true), () -> abortedWrite2.set(true));

                    assertAll(() -> assertTrue(0 < readCommits.get() || 0 < readAborts.get(), "Ended read transaction"),
                            () -> assertTrue(committedWrite1.get() || abortedWrite1.get(), "Ended write transaction 1"),
                            () -> assertTrue(committedWrite2.get() || abortedWrite2.get(), "Ended write transaction 2"),
                            () -> assertTrue(committedWrite1.get(), "Comitted write transaction 1"),
                            () -> assertTrue(committedWrite2.get(), "Comitted write transaction 2"),
                            () -> assertEquals(0, readCommits.get(), "Did not commit read transaction"),
                            () -> assertEquals(1, readAborts.get(), "Aborted read transaction precisely once"));
                }

            }// class

            @Nested
            public class PutRollBackOtherRead {

                @Test
                public void a() {
                    test(DURATION_1, DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_A, OBJECT_B);
                }

                @Test
                public void b() {
                    test(DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, OBJECT_B, OBJECT_A);
                }

                @Test
                public void near() {
                    final Duration when3 = DURATION_4;
                    test(DURATION_1, DURATION_2, DURATION_3, when3, when3, OBJECT_A, OBJECT_B);
                }

                private void test(final Duration earliestTimeOfCompleteState, Duration when1, Duration when2,
                        Duration when3, Duration when4, UUID object1, UUID object2) {
                    assert when1.compareTo(when2) < 0;
                    assert when2.compareTo(when3) < 0;
                    assert when3.compareTo(when4) <= 0;
                    final ObjectStateTest.TestObjectState state1 = new ObjectStateTest.TestObjectState(1);
                    final ObjectStateTest.TestObjectState state2 = new ObjectStateTest.TestObjectState(2);
                    final ObjectStateTest.TestObjectState state3 = new ObjectStateTest.TestObjectState(3);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when2, state2);
                    final Universe.Transaction transaction1 = universe.beginTransaction();
                    transaction1.getObjectState(object1, when1);
                    transaction1.getObjectState(object2, when4);// reads state2
                    final Universe.Transaction transaction2 = universe.beginTransaction();
                    transaction2.getObjectState(object2, when2);
                    transaction2.beginWrite(when3);
                    transaction2.put(object2, state3);
                    // transaction3.getObjectState(object2, when4); would read state3

                    final AtomicBoolean readCommitted = new AtomicBoolean(false);
                    final AtomicBoolean readAborted = new AtomicBoolean(false);
                    final AtomicBoolean writeCommitted = new AtomicBoolean(false);
                    final AtomicBoolean writeAborted = new AtomicBoolean(false);

                    beginCommit(transaction2, () -> writeCommitted.set(true), () -> writeAborted.set(true));
                    beginCommit(transaction1, () -> readCommitted.set(true), () -> readAborted.set(true));

                    assertTrue(writeCommitted.get(), "Write transaction committed.");
                    assertFalse(writeAborted.get(), "Write transaction did not abort.");
                    assertFalse(readCommitted.get(), "Read transaction did not commit.");
                    assertTrue(readAborted.get(), "Read transaction aborted.");
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

                    final Universe.Transaction transaction = universe.beginTransaction();
                    transaction.getObjectState(object, when1);
                    transaction.beginWrite(when2);
                    transaction.put(object, objectState2);

                    final AtomicBoolean writeCommitted = new AtomicBoolean(false);
                    final AtomicBoolean writeAborted = new AtomicBoolean(false);

                    beginCommit(transaction, () -> writeCommitted.set(true), () -> writeAborted.set(true));

                    assertFalse(writeAborted.get(), "Write did not abort.");
                    assertTrue(writeCommitted.get(), "Write committed.");
                    assertEquals(Collections.singleton(object), universe.getObjectIds(), "Object IDs.");
                    assertEquals(expectedObjectStateHistory, universe.getObjectStateHistory(object),
                            "Object state history.");
                    assertSame(objectState1, universe.getObjectState(object, when2.minusNanos(1L)),
                            "The state of an object at a given point in time is "
                                    + "the state it had at the latest state transition "
                                    + "at or before that point in time (just before second)");
                }

            }// class

            private void beginCommit(final Universe.Transaction transaction, Runnable onCommit, Runnable onAbort) {
                transaction.beginCommit(onCommit, onAbort);

                assertInvariants(transaction);
                assertTrue(transaction.didBeginCommit(), "The began commit flag becomes set.");
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

                final Universe universe = new Universe(earliestTimeOfCompleteState);
                putAndCommit(universe, object, when0, objectState0);
                final Universe.Transaction transaction1 = universe.beginTransaction();
                transaction1.getObjectState(object, when0);
                final Universe.Transaction transaction2 = universe.beginTransaction();
                transaction2.getObjectState(object, when0);
                transaction1.beginWrite(when1);
                transaction1.put(object, objectState1);
                transaction2.beginWrite(when1);
                transaction2.put(object, objectState2);

                final AtomicBoolean committed1 = new AtomicBoolean(false);
                final AtomicBoolean aborted1 = new AtomicBoolean(false);
                final AtomicBoolean committed2 = new AtomicBoolean(false);
                final AtomicBoolean aborted2 = new AtomicBoolean(false);

                beginCommit(transaction1, () -> committed1.set(true), () -> aborted1.set(true));
                beginCommit(transaction2, () -> committed2.set(true), () -> aborted2.set(true));

                assertFalse(committed2.get(), "Did not commit second transaction");
                assertTrue(aborted2.get(), "Aborted second transaction");
            }

            @Test
            public void immediately() {
                final Universe universe = new Universe(DURATION_1);
                final Universe.Transaction transaction = universe.beginTransaction();

                final AtomicBoolean committed = new AtomicBoolean(false);
                final AtomicBoolean aborted = new AtomicBoolean(false);

                beginCommit(transaction, () -> committed.set(true), () -> aborted.set(true));

                assertFalse(aborted.get(), "Did not abort");
                assertTrue(committed.get(), "Committed");
            }

            @Test
            public void putOk() {
                final Duration earliestTimeOfCompleteState = DURATION_1;
                final UUID object = OBJECT_A;
                final Duration when = DURATION_2;

                final Universe universe = new Universe(earliestTimeOfCompleteState);
                final Universe.Transaction transaction = universe.beginTransaction();
                final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
                transaction.beginWrite(when);
                transaction.put(object, objectState);

                final AtomicBoolean committed = new AtomicBoolean(false);
                final AtomicBoolean aborted = new AtomicBoolean(false);

                beginCommit(transaction, () -> committed.set(true), () -> aborted.set(true));

                assertFalse(aborted.get(), "Did not abort");
                assertTrue(committed.get(), "Committed");
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
                    Universe.Transaction transaction = universe.beginTransaction();

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
                    Universe.Transaction transaction = universe.beginTransaction();
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
                    final Universe.Transaction transaction = universe.beginTransaction();

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
                    final Universe.Transaction transaction = universe.beginTransaction();

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

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final Universe.Transaction transaction1 = universe.beginTransaction();
                    transaction1.beginWrite(when1);
                    transaction1.put(object, objectState1);
                    final Universe.Transaction transaction2 = universe.beginTransaction();

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
                    final Universe.Transaction transaction = universe.beginTransaction();
                    transaction.getObjectState(object1, when1);
                    transaction.beginWrite(when2);

                    put(transaction, object2, objectState2);
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

                private void test(final Duration earliestTimeOfCompleteState, UUID object, Duration when) {
                    final Set<ObjectStateId> objectStateId = Collections.singleton(new ObjectStateId(object, when));
                    final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
                    final ModifiableValueHistory<ObjectState> expectedHistory = new ModifiableValueHistory<>();
                    expectedHistory.appendTransition(when, objectState);

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    final Universe.Transaction transaction = universe.beginTransaction();
                    transaction.beginWrite(when);

                    put(transaction, object, objectState);

                    assertAll(() -> assertEquals(Collections.singleton(object), universe.getObjectIds(), "Object IDs"),
                            () -> assertEquals(objectStateId, universe.getStateTransitionIds(), "State transition IDs"),
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
                    final Universe.Transaction transaction = universe.beginTransaction();
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

                    final Universe universe = new Universe(earliestTimeOfCompleteState);
                    putAndCommit(universe, object1, when1, state1);
                    putAndCommit(universe, object2, when2, state2);
                    final Universe.Transaction transaction1 = universe.beginTransaction();
                    transaction1.getObjectState(object1, when1);
                    transaction1.getObjectState(object2, when4);// reads state2
                    final Universe.Transaction transaction2 = universe.beginTransaction();
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
                    final Universe.Transaction transaction = universe.beginTransaction();
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

                    final Universe.Transaction transaction = universe.beginTransaction();
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
                    () -> assertDependenciesInvariants(transaction));
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

        private static void putAndCommit(final Universe universe, UUID object, Duration when, ObjectState state) {
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.beginWrite(when);
            transaction.put(object, state);
            transaction.beginCommit(() -> {
                // Do nothing
            }, () -> {
                throw new AssertionError("Does not abort");
            });
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

    public static void assertInvariants(Universe universe) {
        ObjectTest.assertInvariants(universe);// inherited

        final Duration earliestTimeOfCompleteState = universe.getEarliestTimeOfCompleteState();

        assertNotNull(earliestTimeOfCompleteState, "Always have a earliest complete state time-stamp.");

        assertAll(() -> assertObjectIdsInvariants(universe), () -> assertStateTransitionIdsInvariants(universe));
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
            final ValueHistory<ObjectState> objectStateHistory = universe.getObjectStateHistory(object);
            final Duration whenFirstState = universe.getWhenFirstState(object);

            assertNotNull(objectStateHistory,
                    "A universe has an object state history for a given object if that object is one of the  objects in the universe.");// guard
            assertNotNull(whenFirstState, "An object has a first state time-stamp if it is a known object.");

            assertFalse(objectStateHistory.isEmpty(), "A object state history for a given object is not empty.");// guard
            assertSame(objectStateHistory.getFirstTansitionTime(), whenFirstState,
                    "If an object is known, its first state time-stamp is the first transition time of the state history of that object.");
            assertNull(universe.getObjectState(object, whenFirstState.minusNanos(1L)),
                    "Known objects have an unknown state just before the first known state of the state history of that object.");
        }
    }

    private static void assertStateTransitionIdsInvariants(Universe universe) {
        final Set<UUID> objectIds = universe.getObjectIds();

        final Set<ObjectStateId> stateTransitionIds = universe.getStateTransitionIds();

        assertNotNull(stateTransitionIds, "Always have a (non null) set of state transition IDs.");// guard

        for (ObjectStateId stateTransitionId : stateTransitionIds) {
            assertNotNull(stateTransitionId, "The set of IDs of state transitions does not have a null element.");// guard
            ObjectStateIdTest.assertInvariants(stateTransitionId);
            final ObjectState stateTransition = universe.getStateTransition(stateTransitionId);
            final UUID object = stateTransitionId.getObject();
            assertThat(
                    "The set of IDs of state transitions does not have elements for object IDs that are not in the set of objects in this universe.",
                    object, isIn(objectIds));
            if (stateTransition != null) {
                ObjectStateTest.assertInvariants(stateTransition);
            }
        }
    }

    private static void assertUnknownObjectInvariants(Universe universe, UUID object) {
        assertAll(() -> assertThat("Not a known object ID", object, not(isIn(universe.getObjectIds()))),
                () -> assertNull(universe.getObjectStateHistory(object),
                        "A universe has an object state history for a given object only if "
                                + "that object is one of the objects in the universe."),
                () -> assertNull(universe.getWhenFirstState(object),
                        "An object has a first state time-stamp only if it is a known object."),
                () -> assertNull(universe.getObjectState(object, DURATION_1),
                        "Unknown objects have an unknown state for all points in time."),
                () -> assertNull(universe.getObjectState(object, DURATION_2),
                        "Unknown objects have an unknown state for all points in time."));
    }

    private static void assertUnknownObjectStateInvariants(Universe universe, ObjectStateId state) {
        assertNull(universe.getStateTransition(state),
                "Have a state transition only if the given object state ID is one of the known object state IDs of this universe.");
    }

}
