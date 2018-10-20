package uk.badamson.mc.simulation;

import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
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

import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.simulation.Universe.Transaction;

/**
 * <p>
 * Unit tests for the {@link Universe} class.
 * </p>
 */
public class UniverseTest {

    /**
     * <p>
     * Unit tests for the {@link Universe.AbortedTransactionException} class.
     * </p>
     */
    public static class AbortedTransactionExceptionTest {

        public static void assertInvariants(Universe.AbortedTransactionException exception) {
            ObjectTest.assertInvariants(exception);// inherited
        }

        public static void assertInvariants(Universe.AbortedTransactionException exception1,
                Universe.AbortedTransactionException exception2) {
            ObjectTest.assertInvariants(exception1, exception2);// inherited
        }

        private static void constructor(Throwable cause) {
            final Universe.AbortedTransactionException exception = new Universe.AbortedTransactionException(cause);

            assertInvariants(exception);
            assertSame(cause, exception.getCause(), "cause");
        }

        @Test
        public void constructor_0() {
            final Universe.AbortedTransactionException exception = new Universe.AbortedTransactionException();

            assertInvariants(exception);
        }

        @Test
        public void constructor_1A() {
            constructor(new NullPointerException("Test exception"));
        }

        @Test
        public void constructor_1B() {
            constructor(new IllegalArgumentException("Test exception"));
        }
    }// class

    /**
     * <p>
     * Unit tests, and auxiliary test code, for testing the class
     * {@link Universe.Transaction}.
     * </p>
     */
    public static class TransactionTest {

        private static Map<UUID, ObjectStateId> assertDependenciesInvariants(Universe.Transaction transaction) {
            final Map<UUID, ObjectStateId> dependencies = transaction.getDependencies();
            assertNotNull(dependencies, "Always has a dependency map.");// guard
            final Set<ObjectStateId> objectStatesRead = transaction.getObjectStatesRead().keySet();
            for (var entry : dependencies.entrySet()) {
                final UUID object = entry.getKey();
                final ObjectStateId objectStateId = entry.getValue();
                assertNotNull(object, "The dependency map does not have a null key.");// guard
                assertNotNull(objectStateId, "The dependency map does not have null values.");// guard

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

            UniverseTest.assertInvariants(universe);
            assertObjectStatesReadInvariants(transaction);
            assertObjectStatesWrittenInvariants(transaction);
            assertDependenciesInvariants(transaction);
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

        private static void close(Universe.Transaction transaction) {
            transaction.close();

            assertInvariants(transaction);
            assertEquals(Collections.EMPTY_MAP, transaction.getDependencies(),
                    "This transaction has no record of its dependencies.");
            assertEquals(Collections.EMPTY_MAP, transaction.getObjectStatesRead(),
                    "This transaction has no record of its object states read.");
            assertEquals(Collections.EMPTY_MAP, transaction.getObjectStatesWritten(),
                    "This transaction has no record of its object states written.");
            assertNull(transaction.getWhen(), "This transaction is in read mode.");
        }

        private static void close_potentiallyInvalidateOtherRead(final Duration earliestTimeOfCompleteState,
                Duration when1, Duration when2, Duration when3, Duration when4, UUID object1, UUID object2) {
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
            assertFalse(transaction1.willAbortCommit(),
                    "Read transaction will not abort (write transaction might be rolled back)");
        }

        private static void close_putAllowOtherRead(final Duration earliestTimeOfCompleteState, Duration when1,
                Duration when2, Duration when3, Duration when4, UUID object1, UUID object2) {
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

            close(transaction2);

            assertSame(state2, universe.getObjectState(object2, when4), "Rolled-back write [after]");
            assertSame(state2, universe.getObjectState(object2, when3), "Rolled-back write [at]");
            assertFalse(transaction1.willAbortCommit(), "Read transaction will not abort");
        }

        private static void commit(final Universe.Transaction transaction) throws Universe.AbortedTransactionException {
            try {
                transaction.commit();
            } catch (Universe.AbortedTransactionException e) {
                // Permitted
                assertInvariants(transaction);
                UniverseTest.AbortedTransactionExceptionTest.assertInvariants(e);
                assertTrue(transaction.willAbortCommit(), "The commit abort flag is set.");
                assertFalse(transaction.isCommitted(), "The committed flag is clear.");
                throw e;
            }

            assertInvariants(transaction);
            assertTrue(transaction.isCommitted(), "The committed flag of this transaction is set.");
        }

        private static void commit_2DifferentObjects(final UUID object1, final UUID object2) {
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

            try {
                transaction2.commit();
            } catch (Universe.AbortedTransactionException e) {
                throw new AssertionError(e);
            }

            assertEquals(Set.of(object1, object2), universe.getObjectIds(), "Object IDs");
            assertEquals(objectStateHistory1, universe.getObjectStateHistory(object1),
                    "The object state histories of other objects are unchanged.");
        }

        private static void commit_2SuccessiveStates(final Duration when1, final Duration when2) {
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

            try {
                commit(transaction);
            } catch (Universe.AbortedTransactionException e) {
                throw new AssertionError(e);
            }

            assertEquals(Collections.singleton(object), universe.getObjectIds(), "Object IDs.");
            assertEquals(expectedObjectStateHistory, universe.getObjectStateHistory(object), "Object state history.");
            assertSame(objectState1, universe.getObjectState(object, when2.minusNanos(1L)),
                    "The state of an object at a given point in time is "
                            + "the state it had at the latest state transition "
                            + "at or before that point in time (just before second)");
        }

        private static void commit_putRollBackOtherRead(final Duration earliestTimeOfCompleteState, Duration when1,
                Duration when2, Duration when3, Duration when4, UUID object1, UUID object2) {
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

            try {
                commit(transaction2);
            } catch (Universe.AbortedTransactionException e) {
                throw new AssertionError(e);
            }

            assertTrue(transaction1.willAbortCommit(), "Read transaction will abort");
        }

        public static ObjectState getObjectState(final Universe.Transaction transaction, UUID object, Duration when) {
            final ObjectStateId id = new ObjectStateId(object, when);
            final boolean wasPreviouslyRead = transaction.getObjectStatesRead().containsKey(id);
            final ObjectState previouslyReadState = transaction.getObjectStatesRead().get(id);
            final ObjectState universeObjectState = transaction.getUniverse().getObjectState(object, when);

            final ObjectState objectState = transaction.getObjectState(object, when);

            assertInvariants(transaction);
            assertThat(
                    "The object state of for an object ID and point in time is either the same object state as can be got from the universe of this transaction, or is the same object state as has already read by this transaction.",
                    objectState, anyOf(sameInstance(previouslyReadState), sameInstance(universeObjectState)));
            assertTrue(wasPreviouslyRead || objectState == universeObjectState,
                    "The object state of for an object ID and point in time that has not already been read by this transaction is the same object state as can be  got from the universe of this transaction.");
            assertTrue(!wasPreviouslyRead || objectState == previouslyReadState,
                    "The object state of for an object ID and point in time that has already been read by this transaction is the same object state as was read previously.");
            assertThat("The method records the returned state as one of the read states (has key).", id,
                    isIn(transaction.getObjectStatesRead().keySet()));
            assertSame(objectState, transaction.getObjectStatesRead().get(id),
                    "The method records the returned state as one of the read states (state).");

            return objectState;
        }

        private static void getObjectState_1(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
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

        private static void getObjectState_1Empty(final Duration earliestTimeOfCompleteState, UUID object,
                Duration when) {
            final Universe universe = new Universe(earliestTimeOfCompleteState);
            final Universe.Transaction transaction = universe.beginTransaction();

            getObjectState(transaction, object, when);
        }

        private static void getObjectState_1ObjectSuccesiveTimes(final Duration earliestTimeOfCompleteState,
                UUID object, Duration when1, Duration when2, Duration when3) {
            final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
            final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

            final Universe universe = new Universe(earliestTimeOfCompleteState);
            putAndCommit(universe, object, when1, objectState1);
            putAndCommit(universe, object, when3, objectState2);

            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.getObjectState(object, when1);

            getObjectState(transaction, object, when2);
        }

        private static void put(final Universe.Transaction transaction, UUID object, ObjectState state) {
            transaction.put(object, state);

            assertInvariants(transaction);
            assertThat("The method records the given state as one of the states written.",
                    transaction.getObjectStatesWritten(), hasEntry(object, state));
        }

        private static void put_1(final Duration earliestTimeOfCompleteState, UUID object, Duration when) {
            final Set<ObjectStateId> objectStateId = Collections.singleton(new ObjectStateId(object, when));
            final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
            final ModifiableValueHistory<ObjectState> expectedHistory = new ModifiableValueHistory<>();
            expectedHistory.appendTransition(when, objectState);

            final Universe universe = new Universe(earliestTimeOfCompleteState);
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.beginWrite(when);

            put(transaction, object, objectState);

            assertEquals(Collections.singleton(object), universe.getObjectIds(), "Object IDs");
            assertEquals(objectStateId, universe.getStateTransitionIds(), "State transition IDs");
            assertEquals(expectedHistory, universe.getObjectStateHistory(object), "Object state history");
        }

        private static void put_1PrehistoricDependency(final Duration when1, final Duration earliestCompleteState,
                final Duration when2) {
            final ObjectState objectState = new ObjectStateTest.TestObjectState(1);

            final Universe universe = new Universe(earliestCompleteState);
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.getObjectState(OBJECT_A, when1);
            transaction.beginWrite(when2);

            put(transaction, OBJECT_B, objectState);

            assertFalse(transaction.willAbortCommit(), "Will not abort commit");
        }

        private static void put_2Dependency(final Duration earliestCompleteState, final Duration when1,
                final Duration when2, UUID object1, UUID object2) {
            final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
            final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

            final Universe universe = new Universe(earliestCompleteState);
            putAndCommit(universe, object1, when1, objectState1);
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.getObjectState(object1, when1);
            transaction.beginWrite(when2);

            put(transaction, object2, objectState2);

            assertFalse(transaction.willAbortCommit(), "Will not abort commit");
        }

        private static void put_2InvalidEventTimeStampOrder(final Duration earliestTimeOfCompleteState, UUID object,
                Duration when0, Duration when1, Duration when2) {
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

            put(transaction2, object, objectState2);

            assertTrue(transaction2.willAbortCommit(), "Will abort commit");
        }

        private static void put_2NotSuccessiveForSameObject(final Duration earliestTimeOfCompleteState, UUID object,
                Duration when0, Duration when1, Duration when2) {
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

            put(transaction2, object, objectState2);

            assertTrue(transaction2.willAbortCommit(), "Will abort commit");
        }

        private static void put_2OutOfOrderStates(final Duration when1, final Duration when2) {
            assert when1.compareTo(when2) >= 0;
            final UUID object = UniverseTest.OBJECT_A;
            final Duration earliestCompleteState = when1;
            final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
            final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

            final SortedMap<Duration, ObjectState> expectedObjectStateHistory = new TreeMap<>();
            expectedObjectStateHistory.put(when1, objectState1);

            final Universe universe = new Universe(earliestCompleteState);
            putAndCommit(universe, object, when1, objectState1);
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.beginWrite(when2);

            put(transaction, object, objectState2);

            assertTrue(transaction.willAbortCommit(), "Will abort commit");
        }

        private static void put_3AttemptedResurection(final Duration earliestTimeOfCompleteState, UUID object,
                Duration when0, Duration when1, Duration when2) {
            final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
            final ObjectState objectState1 = null;// critical
            final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

            final Universe universe = new Universe(earliestTimeOfCompleteState);
            putAndCommit(universe, object, when0, objectState0);
            putAndCommit(universe, object, when1, objectState1);

            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.beginWrite(when2);

            put(transaction, object, objectState2);

            assertTrue(transaction.willAbortCommit(), "Will abort commit");
        }

        private static void put_3TransitiveDependency(final Duration earliestCompleteState, final Duration when1,
                final Duration when2, final Duration when3, UUID object1, UUID object2, UUID object3) {
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

            assertFalse(transaction.willAbortCommit(), "Will not abort commit");
        }

        private static void putAndCommit(final Universe universe, UUID object, Duration when, ObjectState state) {
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.beginWrite(when);
            transaction.put(object, state);
            try {
                transaction.commit();
            } catch (Universe.AbortedTransactionException e) {
                throw new AssertionError(e);
            }
        }

        private final void beginWrite(Universe.Transaction transaction, Duration when) {
            transaction.beginWrite(when);

            assertInvariants(transaction);
            assertSame(when, transaction.getWhen(),
                    "The time-stamp of any object states to be written by this transaction becomes the same as the given time-stamp.");
        }

        private final void beginWrite_0(Duration earliestTimeOfCompleteState, Duration when) {
            final Universe universe = new Universe(earliestTimeOfCompleteState);
            Universe.Transaction transaction = universe.beginTransaction();

            beginWrite(transaction, when);
        }

        @Test
        public void beginWrite_0A() {
            beginWrite_0(DURATION_1, DURATION_2);
        }

        @Test
        public void beginWrite_0B() {
            beginWrite_0(DURATION_2, DURATION_3);
        }

        private final void beginWrite_1(Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                Duration when2) {
            assert when1.compareTo(when2) < 0;
            final Universe universe = new Universe(earliestTimeOfCompleteState);
            Universe.Transaction transaction = universe.beginTransaction();
            transaction.getObjectState(object, when1);

            beginWrite(transaction, when2);
        }

        @Test
        public void beginWrite_1A() {
            beginWrite_1(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
        }

        @Test
        public void beginWrite_1B() {
            beginWrite_1(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
        }

        @Test
        public void beginWrite_1Close() {
            final Duration when1 = DURATION_2;
            final Duration when2 = when1.plusNanos(1L);// critical

            beginWrite_1(DURATION_1, OBJECT_A, when1, when2);
        }

        private void close_afterCommittedWrite(Duration earliestTimeOfCompleteState, Duration when, UUID object) {
            final Universe universe = new Universe(earliestTimeOfCompleteState);
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.beginWrite(when);
            final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
            transaction.put(object, objectState);
            try {
                transaction.commit();
            } catch (Universe.AbortedTransactionException e) {
                throw new AssertionError(e);
            }

            close(transaction);
        }

        @Test
        public void close_afterCommittedWriteA() {
            close_afterCommittedWrite(DURATION_1, DURATION_2, OBJECT_A);
        }

        @Test
        public void close_afterCommittedWriteB() {
            close_afterCommittedWrite(DURATION_2, DURATION_3, OBJECT_B);
        }

        @Test
        public void close_afterRead() {
            final Universe universe = new Universe(DURATION_1);
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.getObjectState(OBJECT_A, DURATION_2);

            close(transaction);

            UniverseTest.assertInvariants(universe);
        }

        private void close_afterWrite(Duration earliestTimeOfCompleteState, Duration when, UUID object) {
            final Universe universe = new Universe(earliestTimeOfCompleteState);
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.beginWrite(when);
            final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
            transaction.put(object, objectState);

            close(transaction);
        }

        @Test
        public void close_afterWriteA() {
            close_afterWrite(DURATION_1, DURATION_2, OBJECT_A);
        }

        @Test
        public void close_afterWriteB() {
            close_afterWrite(DURATION_2, DURATION_3, OBJECT_B);
        }

        private void close_afterWriteSecond(UUID object, Duration earliestTimeOfCompleteState, Duration when1,
                Duration when2) {
            assert when1.compareTo(when2) < 0;
            final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
            final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

            final Universe universe = new Universe(earliestTimeOfCompleteState);
            putAndCommit(universe, object, when1, objectState1);
            final var history0 = new ModifiableValueHistory<>(universe.getObjectStateHistory(object));

            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.beginWrite(when2);
            transaction.put(object, objectState2);

            close(transaction);

            assertEquals(history0, universe.getObjectStateHistory(object), "Rolled back write");
        }

        @Test
        public void close_afterWriteSecondA() {
            close_afterWriteSecond(OBJECT_A, DURATION_1, DURATION_2, DURATION_3);
        }

        @Test
        public void close_afterWriteSecondB() {
            close_afterWriteSecond(OBJECT_B, DURATION_2, DURATION_3, DURATION_4);
        }

        @Test
        public void close_immediately() {
            final Universe universe = new Universe(DURATION_1);
            final Universe.Transaction transaction = universe.beginTransaction();

            close(transaction);

            UniverseTest.assertInvariants(universe);
        }

        @Test
        public void close_potentiallyInvalidateOtherRead_A() {
            close_potentiallyInvalidateOtherRead(DURATION_1, DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_A,
                    OBJECT_B);
        }

        @Test
        public void close_potentiallyInvalidateOtherRead_B() {
            close_potentiallyInvalidateOtherRead(DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, OBJECT_B,
                    OBJECT_A);
        }

        @Test
        public void close_putAllowOtherRead_A() {
            close_putAllowOtherRead(DURATION_1, DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_A, OBJECT_B);
        }

        @Test
        public void close_putAllowOtherRead_B() {
            close_putAllowOtherRead(DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, OBJECT_B, OBJECT_A);
        }

        private void close_rollBackOtherRead(UUID object, Duration earliestTimeOfCompleteState, Duration when1,
                Duration when2, Duration when3) {
            assert when1.compareTo(when2) < 0;
            assert when2.compareTo(when3) <= 0;
            final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
            final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

            final Universe universe = new Universe(earliestTimeOfCompleteState);
            putAndCommit(universe, object, when1, objectState1);
            final var history0 = new ModifiableValueHistory<>(universe.getObjectStateHistory(object));

            final Universe.Transaction transaction1 = universe.beginTransaction();
            transaction1.beginWrite(when2);
            transaction1.put(object, objectState2);
            final Universe.Transaction transaction2 = universe.beginTransaction();
            transaction2.getObjectState(object, when3);
            assert transaction2.getObjectStatesRead().get(new ObjectStateId(object, when3)) == objectState2;

            close(transaction1);

            assertEquals(history0, universe.getObjectStateHistory(object), "Rolled back write");
            assertTrue(transaction2.willAbortCommit(), "Will abort the reader transaction");
        }

        @Test
        public void close_rollBackOtherReadA() {
            close_rollBackOtherRead(OBJECT_A, DURATION_1, DURATION_2, DURATION_3, DURATION_4);
        }

        @Test
        public void close_rollBackOtherReadAt() {
            final Duration when3 = DURATION_3;
            close_rollBackOtherRead(OBJECT_A, DURATION_1, DURATION_2, when3, when3);
        }

        @Test
        public void close_rollBackOtherReadB() {
            close_rollBackOtherRead(OBJECT_B, DURATION_2, DURATION_3, DURATION_4, DURATION_5);
        }

        @Test
        public void commit_2DifferentObjectsA() {
            commit_2DifferentObjects(OBJECT_A, OBJECT_B);
        }

        @Test
        public void commit_2DifferentObjectsB() {
            commit_2DifferentObjects(OBJECT_B, OBJECT_A);
        }

        @Test
        public void commit_2SuccessiveStatesA() {
            TransactionTest.commit_2SuccessiveStates(DURATION_1, DURATION_2);
        }

        @Test
        public void commit_2SuccessiveStatesB() {
            TransactionTest.commit_2SuccessiveStates(DURATION_2, DURATION_3);
        }

        @Test
        public void commit_2SuccessiveStatesClose() {
            TransactionTest.commit_2SuccessiveStates(DURATION_1, DURATION_1.plusNanos(1));
        }

        @Test(expected = Universe.AbortedTransactionException.class)
        public void commit_failure() throws Universe.AbortedTransactionException {
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

            commit(transaction2);
        }

        @Test
        public void commit_immediately() {
            final Universe universe = new Universe(DURATION_1);
            final Universe.Transaction transaction = universe.beginTransaction();

            try {
                commit(transaction);
            } catch (Universe.AbortedTransactionException e) {
                throw new AssertionError(e);
            }
        }

        @Test
        public void commit_putOk() {
            final Duration earliestTimeOfCompleteState = DURATION_1;
            final UUID object = OBJECT_A;
            final Duration when = DURATION_2;

            final Universe universe = new Universe(earliestTimeOfCompleteState);
            final Universe.Transaction transaction = universe.beginTransaction();
            final ObjectState objectState = new ObjectStateTest.TestObjectState(1);
            transaction.beginWrite(when);
            transaction.put(object, objectState);

            try {
                commit(transaction);
            } catch (Universe.AbortedTransactionException e) {
                throw new AssertionError(e);
            }
        }

        @Test
        public void commit_putRollBackOtherReadA() {
            commit_putRollBackOtherRead(DURATION_1, DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_A, OBJECT_B);
        }

        @Test
        public void commit_putRollBackOtherReadB() {
            commit_putRollBackOtherRead(DURATION_2, DURATION_3, DURATION_4, DURATION_5, DURATION_6, OBJECT_B, OBJECT_A);
        }

        @Test
        public void commit_putRollBackOtherReadClose() {
            final Duration when3 = DURATION_4;
            commit_putRollBackOtherRead(DURATION_1, DURATION_2, DURATION_3, when3, when3, OBJECT_A, OBJECT_B);
        }

        @Test
        public void getObjectState_1A() {
            getObjectState_1(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
        }

        @Test
        public void getObjectState_1B() {
            getObjectState_1(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
        }

        @Test
        public void getObjectState_1EmptyA() {
            getObjectState_1Empty(DURATION_1, OBJECT_A, DURATION_2);
        }

        @Test
        public void getObjectState_1EmptyB() {
            getObjectState_1Empty(DURATION_2, OBJECT_B, DURATION_3);
        }

        @Test
        public void getObjectState_1ObjectSuccesiveTimesA() {
            getObjectState_1ObjectSuccesiveTimes(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
        }

        @Test
        public void getObjectState_1ObjectSuccesiveTimesB() {
            getObjectState_1ObjectSuccesiveTimes(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
        }

        @Test
        public void getObjectState_1Precise() {
            final Duration when = DURATION_2;
            getObjectState_1(DURATION_1, OBJECT_A, when, when);
        }

        @Test
        public void put_1A() {
            put_1(DURATION_1, OBJECT_A, DURATION_2);
        }

        @Test
        public void put_1B() {
            put_1(DURATION_2, OBJECT_B, DURATION_3);
        }

        @Test
        public void put_1PrehistoricDependencyA() {
            put_1PrehistoricDependency(DURATION_1, DURATION_2, DURATION_3);
        }

        @Test
        public void put_1PrehistoricDependencyB() {
            put_1PrehistoricDependency(DURATION_2, DURATION_3, DURATION_4);
        }

        @Test
        public void put_1PrehistoricDependencyClose() {
            put_1PrehistoricDependency(DURATION_2.minusNanos(1L), DURATION_2, DURATION_2);
        }

        @Test
        public void put_2DependencyA() {
            put_2Dependency(DURATION_1, DURATION_2, DURATION_3, OBJECT_A, OBJECT_B);
        }

        @Test
        public void put_2DependencyB() {
            put_2Dependency(DURATION_2, DURATION_3, DURATION_4, OBJECT_B, OBJECT_A);
        }

        @Test
        public void put_2InvalidEventTimeStampOrderA() {
            put_2InvalidEventTimeStampOrder(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
        }

        @Test
        public void put_2InvalidEventTimeStampOrderB() {
            put_2InvalidEventTimeStampOrder(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
        }

        @Test
        public void put_2NotSuccessiveForSameObjectA() {
            put_2NotSuccessiveForSameObject(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
        }

        @Test
        public void put_2NotSuccessiveForSameObjectB() {
            put_2NotSuccessiveForSameObject(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
        }

        @Test
        public void put_2OutOfOrderStatesA() {
            put_2OutOfOrderStates(DURATION_2, DURATION_1);
        }

        @Test
        public void put_2OutOfOrderStatesB() {
            put_2OutOfOrderStates(DURATION_3, DURATION_2);
        }

        @Test
        public void put_2OutOfOrderStatesClose() {
            put_2OutOfOrderStates(DURATION_2, DURATION_2.minusNanos(1L));
        }

        @Test
        public void put_2OutOfOrderStatesSame() {
            put_2OutOfOrderStates(DURATION_2, DURATION_2);
        }

        @Test
        public void put_3AttemptedResurectionA() {
            put_3AttemptedResurection(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
        }

        @Test
        public void put_3AttemptedResurectionB() {
            put_3AttemptedResurection(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
        }

        @Test
        public void put_3TransitiveDependencyA() {
            put_3TransitiveDependency(DURATION_1, DURATION_2, DURATION_3, DURATION_4, OBJECT_A, OBJECT_B, OBJECT_C);
        }

        @Test
        public void put_3TransitiveDependencyB() {
            put_3TransitiveDependency(DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_B, OBJECT_C, OBJECT_A);
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

    public static void assertInvariants(Universe universe) {
        ObjectTest.assertInvariants(universe);// inherited

        final Duration earliestTimeOfCompleteState = universe.getEarliestTimeOfCompleteState();

        assertNotNull(earliestTimeOfCompleteState, "Always have a earliest complete state time-stamp.");

        assertObjectIdsInvariants(universe);
        assertStateTransitionIdsInvariants(universe);
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
        assertThat("Not a known object ID", object, not(isIn(universe.getObjectIds())));
        assertNull(universe.getObjectStateHistory(object),
                "A universe has an object state history for a given object only if "
                        + "that object is one of the objects in the universe.");
        assertNull(universe.getWhenFirstState(object),
                "An object has a first state time-stamp only if it is a known object.");
        assertNull(universe.getObjectState(object, DURATION_1),
                "Unknown objects have an unknown state for all points in time.");
        assertNull(universe.getObjectState(object, DURATION_2),
                "Unknown objects have an unknown state for all points in time.");
    }

    private static void assertUnknownObjectStateInvariants(Universe universe, ObjectStateId state) {
        assertNull(universe.getStateTransition(state),
                "Have a state transition only if the given object state ID is one of the known object state IDs of this universe.");
    }

    /**
     * <p>
     * Get the number of pending (not yet {@linkplain Transaction#commit()
     * committed}) {@linkplain Transaction transactions} that state of a given
     * object at a given point in time depend on.
     * </p>
     * <ul>
     * <li>The number of pending transactions for an object and time is non
     * negative.</li>
     * <li>Unknown {@linkplain #getObjectIds() objects} have no pending transactions
     * for all points in time.</li>
     * <li>The number of pending transactions for known {@linkplain #getObjectIds()
     * objects} is zero only at points in time when then object state has been
     * committed by a {@linkplain Transaction transactions}.</li>
     * </ul>
     * 
     * @param object
     *            The ID of the object of interest.
     * @param when
     *            The point in time of interest.
     * @return The number of pending transactions for the given object at the given
     *         point in time.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code object} is null.</li>
     *             <li>If {@code when} is null.</li>
     *             </ul>
     */

    private static Universe.Transaction beginTransaction(final Universe universe) {
        final Universe.Transaction transaction = universe.beginTransaction();

        assertNotNull(transaction, "Not null, transaction");// guard
        assertInvariants(universe);
        TransactionTest.assertInvariants(transaction);

        assertSame(universe, transaction.getUniverse(),
                "The universe of the returned transaction is this transaction.");
        assertEquals(Collections.EMPTY_MAP, transaction.getObjectStatesRead(),
                "The returned transaction has not read any object states.");
        assertEquals(Collections.EMPTY_MAP, transaction.getObjectStatesWritten(),
                "The returned transaction has not written any object states.");
        assertFalse(transaction.isCommitted(), "The committed flag of the returned transaction is clear.");
        assertFalse(transaction.willAbortCommit(), "The commit abort flag of the return transaction is clear");
        assertNull(transaction.getWhen(), "The returned transaction is in in read mode.");

        return transaction;
    }

    private static void constructor(final Duration earliestTimeOfCompleteState) {
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

    @Test
    public void beginTransactionA() {
        final Universe universe = new Universe(DURATION_1);

        beginTransaction(universe);
    }

    @Test
    public void beginTransactionB() {
        final Universe universe = new Universe(DURATION_2);

        beginTransaction(universe);
    }

    @Test
    public void constructor_A() {
        constructor(DURATION_1);
    }

    @Test
    public void constructor_B() {
        constructor(DURATION_2);
    }

}
