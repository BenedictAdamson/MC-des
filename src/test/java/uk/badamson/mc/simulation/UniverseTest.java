package uk.badamson.mc.simulation;

import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

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
            assertSame("cause", cause, exception.getCause());
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
            assertNotNull("Always has a dependency map.", dependencies);
            final Set<ObjectStateId> objectStatesRead = transaction.getObjectStatesRead().keySet();
            for (var entry : dependencies.entrySet()) {
                final UUID object = entry.getKey();
                final ObjectStateId objectStateId = entry.getValue();
                assertNotNull("The dependency map does not have a null key.", object);// guard
                assertNotNull("The dependency map does not have null values.", objectStateId);// guard

                ObjectStateIdTest.assertInvariants(objectStateId);
                assertSame(
                        "Each object ID key of the dependency map maps to a value that has that same object ID as the object of the object state ID.",
                        object, objectStateId.getObject());
                assertThat(
                        "The collection of values of the dependencies map is a sub set of the keys of the object states read.",
                        objectStateId, isIn(objectStatesRead));
                /**
                 * <li>The {@linkplain ObjectStateId#getWhen() time-stamp} of each
                 * {@linkplain Map#keySet() object state ID key} of the
                 * {@linkplain #getObjectStatesRead() object states read} map is at or after the
                 * time-stamp of the {@linkplain Map#get(Object) value} in the dependencies map
                 * for the {@linkplain ObjectStateId#getObject() object ID} of that object state
                 * ID.</li>
                 * </ul>
                 * 
                 * @return The dependency information
                 */
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

            assertNotNull("Not null, universe", universe);// guard

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
            assertNotNull("Always have a (non null) map of object states read.", objectStatesRead);// guard
            for (var entry : objectStatesRead.entrySet()) {
                final ObjectStateId id = entry.getKey();
                final ObjectState state = entry.getValue();
                assertNotNull("The map of object states read does not have a null key.", id);// guard
                ObjectStateIdTest.assertInvariants(id);
                if (state != null) {
                    ObjectStateTest.assertInvariants(state);
                }
            }
            return objectStatesRead;
        }

        private static Map<UUID, ObjectState> assertObjectStatesWrittenInvariants(Universe.Transaction transaction) {
            final Map<UUID, ObjectState> objectStatesWritten = transaction.getObjectStatesWritten();
            assertNotNull("Always have a (non null) map of object states written.", objectStatesWritten);// guard
            assertFalse("The map of object states written is empty if this transaction is in read mode.",
                    transaction.getWhen() == null && !objectStatesWritten.isEmpty());

            for (var entry : objectStatesWritten.entrySet()) {
                final UUID object = entry.getKey();
                final ObjectState state = entry.getValue();
                assertNotNull("The map of object states written does not have a null key.", object);
                if (state != null) {
                    ObjectStateTest.assertInvariants(state);
                }
            }
            return objectStatesWritten;
        }

        public static void close(Universe.Transaction transaction) {
            transaction.close();

            assertInvariants(transaction);
        }

        public static void commit(final Universe.Transaction transaction) throws Universe.AbortedTransactionException {
            try {
                transaction.commit();
            } catch (Universe.AbortedTransactionException e) {
                // Permitted
                assertInvariants(transaction);
                UniverseTest.AbortedTransactionExceptionTest.assertInvariants(e);
                throw e;
            }

            assertInvariants(transaction);
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

            assertEquals("Object IDs", Set.of(object1, object2), universe.getObjectIds());
            assertEquals("The object state histories of other objects are unchanged.", objectStateHistory1,
                    universe.getObjectStateHistory(object1));
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
            transaction.fetchObjectState(object, when1);
            transaction.beginWrite(when2);
            transaction.put(object, objectState2);

            try {
                commit(transaction);
            } catch (Universe.AbortedTransactionException e) {
                throw new AssertionError(e);
            }

            assertEquals("Object IDs.", Collections.singleton(object), universe.getObjectIds());
            assertEquals("Object state history.", expectedObjectStateHistory, universe.getObjectStateHistory(object));
            assertSame(
                    "The state of an object at a given point in time is "
                            + "the state it had at the latest state transition "
                            + "at or before that point in time (just before second)",
                    objectState1, universe.getObjectState(object, when2.minusNanos(1L)));
        }

        public static ObjectState fetchObjectState(final Universe.Transaction transaction, UUID object, Duration when) {
            final ObjectStateId id = new ObjectStateId(object, when);
            final boolean wasPreviouslyRead = transaction.getObjectStatesRead().containsKey(id);
            final ObjectState previouslyReadState = transaction.getObjectStatesRead().get(id);
            final ObjectState universeObjectState = transaction.getUniverse().getObjectState(object, when);

            final ObjectState objectState = transaction.fetchObjectState(object, when);

            assertInvariants(transaction);
            assertThat(
                    "The object state of for an object ID and point in time is either the same object state as can be got from the universe of this transaction, or is the same object state as has already read by this transaction.",
                    objectState, anyOf(sameInstance(previouslyReadState), sameInstance(universeObjectState)));
            assertTrue(
                    "The object state of for an object ID and point in time that has not already been read by this transaction is the same object state as can be  got from the universe of this transaction.",
                    wasPreviouslyRead || objectState == universeObjectState);
            assertTrue(
                    "The object state of for an object ID and point in time that has already been read by this transaction is the same object state as was read previously.",
                    !wasPreviouslyRead || objectState == previouslyReadState);
            assertThat("The method records the returned state as one of the read states (has key).", id,
                    isIn(transaction.getObjectStatesRead().keySet()));
            assertSame("The method records the returned state as one of the read states (state).", objectState,
                    transaction.getObjectStatesRead().get(id));

            return objectState;
        }

        private static void fetchObjectState_1(final Duration earliestTimeOfCompleteState, UUID object, Duration when1,
                Duration when2) {
            final ObjectStateId id2 = new ObjectStateId(object, when2);
            final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);

            final Universe universe = new Universe(earliestTimeOfCompleteState);
            putAndCommit(universe, object, when1, objectState1);
            final Universe.Transaction transaction = universe.beginTransaction();

            final ObjectState objectState2 = fetchObjectState(transaction, object, when2);

            assertSame("objectState", objectState1, objectState2);
            assertEquals("objectStatesRead", Collections.singletonMap(id2, objectState1),
                    transaction.getObjectStatesRead());
        }

        private static void fetchObjectState_1Empty(final Duration earliestTimeOfCompleteState, UUID object,
                Duration when) {
            final Universe universe = new Universe(earliestTimeOfCompleteState);
            final Universe.Transaction transaction = universe.beginTransaction();

            fetchObjectState(transaction, object, when);
        }

        private static void fetchObjectState_1ObjectSuccesiveTimes(final Duration earliestTimeOfCompleteState,
                UUID object, Duration when1, Duration when2, Duration when3) {
            final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
            final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

            final Universe universe = new Universe(earliestTimeOfCompleteState);
            putAndCommit(universe, object, when1, objectState1);
            putAndCommit(universe, object, when3, objectState2);

            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.fetchObjectState(object, when1);

            fetchObjectState(transaction, object, when2);
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

            assertEquals("Object IDs", Collections.singleton(object), universe.getObjectIds());
            assertEquals("State transition IDs", objectStateId, universe.getStateTransitionIds());
            assertEquals("Object state history", expectedHistory, universe.getObjectStateHistory(object));
        }

        private static void put_1PrehistoricDependency(final Duration when1, final Duration earliestCompleteState,
                final Duration when2) {
            final ObjectState objectState = new ObjectStateTest.TestObjectState(1);

            final Universe universe = new Universe(earliestCompleteState);
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.fetchObjectState(OBJECT_A, when1);
            transaction.beginWrite(when2);

            put(transaction, OBJECT_B, objectState);

            assertFalse("Will not abort commit", transaction.willAbortCommit());
        }

        private static void put_2Dependency(final Duration earliestCompleteState, final Duration when1,
                final Duration when2, UUID object1, UUID object2) {
            final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
            final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

            final Universe universe = new Universe(earliestCompleteState);
            putAndCommit(universe, object1, when1, objectState1);
            final Universe.Transaction transaction = universe.beginTransaction();
            transaction.fetchObjectState(object1, when1);
            transaction.beginWrite(when2);

            put(transaction, object2, objectState2);

            assertFalse("Will not abort commit", transaction.willAbortCommit());
        }

        private static void put_2InvalidEventTimeStampOrder(final Duration earliestTimeOfCompleteState, UUID object,
                Duration when0, Duration when1, Duration when2) {
            final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
            final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
            final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

            final Universe universe = new Universe(earliestTimeOfCompleteState);
            putAndCommit(universe, object, when0, objectState0);
            final Universe.Transaction transaction1 = universe.beginTransaction();
            transaction1.fetchObjectState(object, when0);
            final Universe.Transaction transaction2 = universe.beginTransaction();
            transaction2.fetchObjectState(object, when0);
            transaction1.beginWrite(when2);
            transaction1.put(object, objectState1);
            transaction2.beginWrite(when1);

            put(transaction2, object, objectState2);

            assertTrue("Will abort commit", transaction2.willAbortCommit());
        }

        private static void put_2NotSuccessiveForSameObject(final Duration earliestTimeOfCompleteState, UUID object,
                Duration when0, Duration when1, Duration when2) {
            final ObjectState objectState0 = new ObjectStateTest.TestObjectState(0);
            final ObjectState objectState1 = new ObjectStateTest.TestObjectState(1);
            final ObjectState objectState2 = new ObjectStateTest.TestObjectState(2);

            final Universe universe = new Universe(earliestTimeOfCompleteState);
            putAndCommit(universe, object, when0, objectState0);
            final Universe.Transaction transaction1 = universe.beginTransaction();
            transaction1.fetchObjectState(object, when0);
            final Universe.Transaction transaction2 = universe.beginTransaction();
            transaction2.fetchObjectState(object, when0);
            transaction1.beginWrite(when1);
            transaction1.put(object, objectState1);
            transaction2.beginWrite(when1);

            put(transaction2, object, objectState2);

            assertTrue("Will abort commit", transaction2.willAbortCommit());
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

            assertTrue("Will abort commit", transaction.willAbortCommit());
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

            assertTrue("Will abort commit", transaction.willAbortCommit());
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
            transaction.fetchObjectState(object2, when2);
            transaction.beginWrite(when3);

            put(transaction, object3, objectState3);

            assertFalse("Will not abort commit", transaction.willAbortCommit());
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
            assertSame(
                    "The time-stamp of any object states to be written by this transaction becomes the same as the given time-stamp.",
                    when, transaction.getWhen());
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
            transaction.fetchObjectState(object, when1);

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

        @Test
        public void close_immediately() {
            final Universe universe = new Universe(DURATION_1);
            final Universe.Transaction transaction = universe.beginTransaction();

            close(transaction);

            UniverseTest.assertInvariants(universe);
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
            transaction1.fetchObjectState(object, when0);
            final Universe.Transaction transaction2 = universe.beginTransaction();
            transaction2.fetchObjectState(object, when0);
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
        public void commit_ok() {
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
        public void fetchObjectState_1A() {
            fetchObjectState_1(DURATION_1, OBJECT_A, DURATION_2, DURATION_3);
        }

        @Test
        public void fetchObjectState_1B() {
            fetchObjectState_1(DURATION_2, OBJECT_B, DURATION_3, DURATION_4);
        }

        @Test
        public void fetchObjectState_1EmptyA() {
            fetchObjectState_1Empty(DURATION_1, OBJECT_A, DURATION_2);
        }

        @Test
        public void fetchObjectState_1EmptyB() {
            fetchObjectState_1Empty(DURATION_2, OBJECT_B, DURATION_3);
        }

        @Test
        public void fetchObjectState_1ObjectSuccesiveTimesA() {
            fetchObjectState_1ObjectSuccesiveTimes(DURATION_1, OBJECT_A, DURATION_2, DURATION_3, DURATION_4);
        }

        @Test
        public void fetchObjectState_1ObjectSuccesiveTimesB() {
            fetchObjectState_1ObjectSuccesiveTimes(DURATION_2, OBJECT_B, DURATION_3, DURATION_4, DURATION_5);
        }

        @Test
        public void fetchObjectState_1Precise() {
            final Duration when = DURATION_2;
            fetchObjectState_1(DURATION_1, OBJECT_A, when, when);
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

        /**
         * <p>
         * Change this transaction from read mode to write mode.
         * </p>
         * <ul>
         * <li>The {@linkplain #getWhen() time-stamp of any object states to be written}
         * by this transaction is the same as the given time-stamp.
         * </ul>
         * 
         * @param when
         *            The time-stamp of all object states to be
         *            {@linkplain #put(UUID, Duration, ObjectState) put} (written) by
         *            this transaction, expressed as the duration since an epoch.
         * 
         * @throws NullPointerException
         *             If {@code when} is null.
         * @throws IllegalStateException
         *             <ul>
         *             <li>If the any of the {@linkplain #getObjectStatesRead() reads}
         *             done by this transaction were for for
         *             {@linkplain ObjectStateId#getWhen() times} at of after the given
         *             time.</li>
         *             </ul>
         *             If this transaction is already in write mode. That is, if this
         *             method has already been called for this transaction.</li>
         *             </ul>
         */
    }// class

    private static final UUID OBJECT_A = ObjectStateIdTest.OBJECT_A;
    private static final UUID OBJECT_B = ObjectStateIdTest.OBJECT_B;
    private static final UUID OBJECT_C = UUID.randomUUID();
    private static final Duration DURATION_1 = Duration.ofSeconds(13);
    private static final Duration DURATION_2 = Duration.ofSeconds(17);
    private static final Duration DURATION_3 = Duration.ofSeconds(23);
    private static final Duration DURATION_4 = Duration.ofSeconds(29);
    private static final Duration DURATION_5 = Duration.ofSeconds(31);

    public static void assertInvariants(Universe universe) {
        ObjectTest.assertInvariants(universe);// inherited

        final Duration earliestTimeOfCompleteState = universe.getEarliestTimeOfCompleteState();
        final Set<UUID> objectIds = universe.getObjectIds();
        final Set<ObjectStateId> stateTransitionIds = universe.getStateTransitionIds();

        assertNotNull("Always have a earliest complete state time-stamp.", earliestTimeOfCompleteState);
        assertNotNull("Always have a set of object IDs.", objectIds);// guard
        assertNotNull("Always have a (non null) set of state transition IDs.", stateTransitionIds);

        for (ObjectStateId stateTransitionId : stateTransitionIds) {
            assertNotNull("The set of IDs of state transitions does not have a null element.", stateTransitionId);// guard
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

        for (UUID object : objectIds) {
            assertNotNull("The set of object IDs does not have a null element.", object);// guard
            final ValueHistory<ObjectState> objectStateHistory = universe.getObjectStateHistory(object);
            final Duration whenFirstState = universe.getWhenFirstState(object);

            assertNotNull(
                    "A universe has an object state history for a given object if that object is one of the  objects in the universe.",
                    objectStateHistory);// guard
            assertNotNull("An object has a first state time-stamp if it is a known object.", whenFirstState);

            assertFalse("A object state history for a given object is not empty.", objectStateHistory.isEmpty());// guard
            assertSame(
                    "If an object is known, its first state time-stamp is the first transition time of the state history of that object.",
                    objectStateHistory.getFirstTansitionTime(), whenFirstState);
            assertNull(
                    "Known objects have an unknown state just before the first known state of the state history of that object.",
                    universe.getObjectState(object, whenFirstState.minusNanos(1L)));
        }
    }

    public static void assertInvariants(Universe universe1, Universe universe2) {
        ObjectTest.assertInvariants(universe1, universe2);// inherited
    }

    private static void assertUnknownObjectInvariants(Universe universe, UUID object) {
        assertThat("Not a known object ID", object, not(isIn(universe.getObjectIds())));
        assertNull("A universe has an object state history for a given object only if "
                + "that object is one of the objects in the universe.", universe.getObjectStateHistory(object));
        assertNull("An object has a first state time-stamp only if it is a known object.",
                universe.getWhenFirstState(object));
        assertNull("Unknown objects have an unknown state for all points in time.",
                universe.getObjectState(object, DURATION_1));
        assertNull("Unknown objects have an unknown state for all points in time.",
                universe.getObjectState(object, DURATION_2));
    }

    private static void assertUnknownObjectStateInvariants(Universe universe, ObjectStateId state) {
        assertNull(
                "Have a state transition only if the given object state ID is one of the known object state IDs of this universe.",
                universe.getStateTransition(state));
    }

    private static Universe.Transaction beginTransaction(final Universe universe) {
        final Universe.Transaction transaction = universe.beginTransaction();

        assertNotNull("Not null, transaction", transaction);// guard
        assertInvariants(universe);
        TransactionTest.assertInvariants(transaction);

        assertSame("The universe of the returned transaction is this transaction.", universe,
                transaction.getUniverse());
        assertEquals("The returned transaction has not read any object states.", Collections.EMPTY_MAP,
                transaction.getObjectStatesRead());
        assertEquals("The returned transaction has not written any object states.", Collections.EMPTY_MAP,
                transaction.getObjectStatesWritten());
        assertFalse("The commit abort flag of the return transaction is clear", transaction.willAbortCommit());
        assertNull("The returned transaction is in in read mode.", transaction.getWhen());

        return transaction;
    }

    private static void constructor(final Duration earliestTimeOfCompleteState) {
        final Universe universe = new Universe(earliestTimeOfCompleteState);

        assertInvariants(universe);

        assertSame(
                "The earliest complete state time-stamp of this universe is "
                        + "the given earliest complete state time-stamp.",
                earliestTimeOfCompleteState, universe.getEarliestTimeOfCompleteState());
        assertEquals("The set of object IDs is empty.", Collections.emptySet(), universe.getObjectIds());
        assertEquals("The set of IDs of object states is empty.", Collections.emptySet(), universe.getObjectIds());

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
