package uk.badamson.mc.simulation;

import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.collection.IsMapContaining.hasValue;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
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
     * Unit tests for the {@link Universe.InvalidEventTimeStampOrderException}
     * class.
     * </p>
     */
    public static class InvalidEventTimeStampOrderExceptionTest {

        public static void assertInvariants(Universe.InvalidEventTimeStampOrderException exception) {
            ObjectTest.assertInvariants(exception);// inherited
        }

        public static void assertInvariants(Universe.InvalidEventTimeStampOrderException exception1,
                Universe.InvalidEventTimeStampOrderException exception2) {
            ObjectTest.assertInvariants(exception1, exception2);// inherited
        }

        private static void constructor(Throwable cause) {
            final Universe.InvalidEventTimeStampOrderException exception = new Universe.InvalidEventTimeStampOrderException(
                    cause);

            assertInvariants(exception);
            assertSame("cause", cause, exception.getCause());
        }

        @Test
        public void constructor_0() {
            final Universe.InvalidEventTimeStampOrderException exception = new Universe.InvalidEventTimeStampOrderException();

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
     * Unit tests and auxiliary code for the
     * {@link Universe.MissingDependedUponStateException} class.
     */
    public static class MissingDependedUponStateExceptionTest {

        public static void assertInvariants(Universe.MissingDependedUponStateException exception) {
            ObjectTest.assertInvariants(exception);// inherited
        }

        public static void assertInvariants(Universe.MissingDependedUponStateException exception1,
                Universe.MissingDependedUponStateException exception2) {
            ObjectTest.assertInvariants(exception1, exception2);// inherited
        }

        private static void constructor(Throwable cause) {
            final Universe.MissingDependedUponStateException exception = new Universe.MissingDependedUponStateException(
                    cause);

            assertInvariants(exception);
            assertSame("cause", cause, exception.getCause());
        }

        @Test
        public void constructor_0() {
            final Universe.MissingDependedUponStateException exception = new Universe.MissingDependedUponStateException();

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

    private static final UUID OBJECT_A = ObjectStateIdTest.OBJECT_A;
    private static final UUID OBJECT_B = ObjectStateIdTest.OBJECT_B;
    private static final UUID OBJECT_C = UUID.randomUUID();
    private static final Duration DURATION_1 = Duration.ofSeconds(13);
    private static final Duration DURATION_2 = Duration.ofSeconds(17);
    private static final Duration DURATION_3 = Duration.ofSeconds(23);
    private static final Duration DURATION_4 = Duration.ofSeconds(29);
    private static final Duration DURATION_5 = Duration.ofSeconds(31);

    public static void append(final Universe universe, ObjectState objectState)
            throws Universe.InvalidEventTimeStampOrderException {
        final ObjectStateId id = objectState.getId();
        final UUID object = id.getObject();
        final Duration when = id.getWhen();
        final Set<UUID> objectIds0 = universe.getObjectIds();
        final SortedMap<Duration, ObjectState> objectStateHistory0 = universe.getObjectStateHistory(object);

        try {
            universe.append(objectState);
        } catch (final Universe.InvalidEventTimeStampOrderException | Universe.MissingDependedUponStateException e) {// Permitted
            assertInvariants(universe);
            assertEquals("Known object IDs unchanged", objectIds0, universe.getObjectIds());
            assertEquals("Object state history unchanged", objectStateHistory0, universe.getObjectStateHistory(object));
            throw e;
        }

        assertInvariants(universe);
        assertThat("The object ID of the ID of the given object state is one of the object IDs of this universe.",
                universe.getObjectIds(), hasItem(object));
        final SortedMap<Duration, ObjectState> objectStateHistory = universe.getObjectStateHistory(object);
        assertNotNull("Object has a state history", objectStateHistory);// guard
        assertThat(
                "The given object state is the last value in the object state history of the object of the ID of the given object state (time-stamp).",
                objectStateHistory.lastKey(), sameInstance(when));
        assertThat(
                "The given object state is the last value in the object state history of the object of the ID of the given object state (value).",
                objectStateHistory.get(when), sameInstance(objectState));
    }

    private static void append_1(final UUID object, final Duration when) {
        final Duration earliestCompleteState = when;
        final Duration justAfter = when.plusNanos(1L);
        final Map<UUID, ObjectStateId> dependencies = Collections.emptyMap();
        final ObjectState objectState = new ObjectStateTest.TestObjectState(object, when, dependencies);

        final Universe universe = new Universe(earliestCompleteState);

        append(universe, objectState);

        assertThat("The object ID of the ID of the given object state is one of the object IDs of this universe.",
                universe.getObjectIds(), equalTo(Collections.singleton(object)));
        assertThat(
                "The given object state is the last value in the object state history of the object of the ID of the given object state (value).",
                universe.getObjectStateHistory(object), equalTo(Collections.singletonMap(when, objectState)));
        assertSame(
                "The state of an object at a given point in time is "
                        + "the state it had at the latest state transition "
                        + "at or before that point in time (just after transition)",
                objectState, universe.getObjectState(object, justAfter));
        assertUnknownObjectStateInvariants(universe, new ObjectStateId(object, justAfter));
    }

    private static void append_1PrehistoricDependency(final Duration when1, final Duration earliestCompleteState,
            final Duration when2) {
        final ObjectStateId dependentState = new ObjectStateId(OBJECT_A, when1);
        final Map<UUID, ObjectStateId> dependencies = Collections.singletonMap(OBJECT_A, dependentState);
        final ObjectState objectState = new ObjectStateTest.TestObjectState(OBJECT_B, when2, dependencies);

        final Universe universe = new Universe(earliestCompleteState);

        append(universe, objectState);
    }

    private static void append_2Dependency(final Duration earliestCompleteState, final Duration when1,
            final Duration when2, UUID object1, UUID object2) {
        final ObjectStateId id1 = new ObjectStateId(object1, when1);
        final ObjectStateId id2 = new ObjectStateId(object2, when2);
        final Map<UUID, ObjectStateId> dependencies1 = Collections.emptyMap();
        final Map<UUID, ObjectStateId> dependencies2 = Collections.singletonMap(object1, id1);
        final ObjectState objectState1 = new ObjectStateTest.TestObjectState(object1, when1, dependencies1);
        final ObjectState objectState2 = new ObjectStateTest.TestObjectState(object2, when2, dependencies2);

        final Universe universe = new Universe(earliestCompleteState);
        universe.append(objectState1);

        append(universe, objectState2);

        assertEquals("Depended upon object has reverse dependency", Collections.singleton(id2),
                universe.getDependentStateTransitions(id1));
    }

    private static void append_2DifferentObjects(final UUID object1, final UUID object2) {
        assert !object1.equals(object2);
        final Duration when = DURATION_1;
        final Map<UUID, ObjectStateId> dependencies = Collections.emptyMap();
        final ObjectState objectState1 = new ObjectStateTest.TestObjectState(object1, when, dependencies);
        final ObjectState objectState2 = new ObjectStateTest.TestObjectState(object2, when, dependencies);

        final Universe universe = new Universe(when);
        universe.append(objectState1);
        final SortedMap<Duration, ObjectState> objectStateHistory1 = new TreeMap<>(
                universe.getObjectStateHistory(object1));

        append(universe, objectState2);

        assertEquals("Object IDs", Set.of(object1, object2), universe.getObjectIds());
        assertEquals("The object state histories of other objects are unchanged.", objectStateHistory1,
                universe.getObjectStateHistory(object1));
    }

    private static void append_2OutOfOrderStates(final Duration when1, final Duration when2)
            throws Universe.InvalidEventTimeStampOrderException {
        assert when1.compareTo(when2) >= 0;
        final UUID object = OBJECT_A;
        final Duration earliestCompleteState = when1;
        final Map<UUID, ObjectStateId> dependencies = Collections.emptyMap();
        final ObjectState objectState1 = new ObjectStateTest.TestObjectState(object, when1, dependencies);
        final ObjectState objectState2 = new ObjectStateTest.TestObjectState(object, when2, dependencies);
        final SortedMap<Duration, ObjectState> expectedObjectStateHistory = new TreeMap<>();
        expectedObjectStateHistory.put(when1, objectState1);

        final Universe universe = new Universe(earliestCompleteState);
        universe.append(objectState1);

        append(universe, objectState2);// throws
    }

    private static void append_2SuccessiveStates(final Duration when1, final Duration when2) {
        assert when1.compareTo(when2) < 0;
        final UUID object = OBJECT_A;
        final Duration earliestCompleteState = when2;
        final ObjectStateId id1 = new ObjectStateId(object, when1);
        final Map<UUID, ObjectStateId> dependencies1 = Collections.emptyMap();
        final Map<UUID, ObjectStateId> dependencies2 = Collections.singletonMap(object, id1);
        final ObjectState objectState1 = new ObjectStateTest.TestObjectState(object, when1, dependencies1);
        final ObjectState objectState2 = new ObjectStateTest.TestObjectState(object, when2, dependencies2);
        final SortedMap<Duration, ObjectState> expectedObjectStateHistory = new TreeMap<>();
        expectedObjectStateHistory.put(when1, objectState1);
        expectedObjectStateHistory.put(when2, objectState2);

        final Universe universe = new Universe(earliestCompleteState);
        universe.append(objectState1);

        append(universe, objectState2);

        assertEquals("Object IDs.", Collections.singleton(object), universe.getObjectIds());
        assertEquals("Object state history.", expectedObjectStateHistory, universe.getObjectStateHistory(object));
        assertSame(
                "The state of an object at a given point in time is "
                        + "the state it had at the latest state transition "
                        + "at or before that point in time (just before second)",
                objectState1, universe.getObjectState(object, when2.minusNanos(1L)));
    }

    private static void append_3TransitiveDependency(final Duration earliestCompleteState, final Duration when1,
            final Duration when2, final Duration when3, UUID object1, UUID object2, UUID object3) {
        final ObjectStateId id1 = new ObjectStateId(object1, when1);
        final ObjectStateId id2 = new ObjectStateId(object2, when2);
        final ObjectStateId id3 = new ObjectStateId(object3, when3);
        final Map<UUID, ObjectStateId> dependencies1 = Collections.emptyMap();
        final Map<UUID, ObjectStateId> dependencies2 = Collections.singletonMap(object1, id1);
        final Map<UUID, ObjectStateId> dependencies3 = Collections.singletonMap(object2, id2);
        final ObjectState objectState1 = new ObjectStateTest.TestObjectState(object1, when1, dependencies1);
        final ObjectState objectState2 = new ObjectStateTest.TestObjectState(object2, when2, dependencies2);
        final ObjectState objectState3 = new ObjectStateTest.TestObjectState(object3, when3, dependencies3);

        final Universe universe = new Universe(earliestCompleteState);
        universe.append(objectState1);
        universe.append(objectState2);

        append(universe, objectState3);

        assertEquals("Depended upon object has reverse dependency", Collections.singleton(id3),
                universe.getDependentStateTransitions(id2));
        assertEquals("Depended upon object has transitive reverse dependency", Set.of(id3, id2),
                universe.getDependentStateTransitions(id1));
    }

    private static Set<ObjectStateId> assertDependentStateTransitionsInvariants(Universe universe,
            ObjectStateId objectStateId) {
        final Duration when = objectStateId.getWhen();
        final UUID object = objectStateId.getObject();

        final Set<ObjectStateId> dependentStateTransitions = universe.getDependentStateTransitions(objectStateId);

        final Set<ObjectStateId> dependentStateTransitionsForEarlier = universe
                .getDependentStateTransitions(new ObjectStateId(object, when.minusNanos(1L)));
        final Set<ObjectStateId> stateTransitionIds = universe.getStateTransitionIds();
        assertNotNull("Always have a set of dependent state transitions.", dependentStateTransitions);// guard
        for (ObjectStateId dependentStateTransition : dependentStateTransitions) {
            assertNotNull("The set of dependent state transitions does not have a null element.",
                    dependentStateTransition);// guard
            ObjectStateIdTest.assertInvariants(dependentStateTransition);
            ObjectStateIdTest.assertInvariants(objectStateId, dependentStateTransition);
            assertThat("The set of dependent state transitions is a subset of the set of all known state transitions.",
                    dependentStateTransition, isIn(stateTransitionIds));
            assertThat("Dependencies are time ordered", dependentStateTransition.getWhen(), greaterThan(when));
            assertThat("Dependencies carry forward through time", dependentStateTransition,
                    isIn(dependentStateTransitionsForEarlier));
        }
        return dependentStateTransitions;
    }

    public static void assertInvariants(Universe universe) {
        ObjectTest.assertInvariants(universe);// inherited

        final Duration earliestTimeOfCompleteState = universe.getEarliestTimeOfCompleteState();
        final Set<UUID> objectIds = universe.getObjectIds();
        final Set<ObjectStateId> stateTransitionIds = universe.getStateTransitionIds();

        assertNotNull("Always have a earliest complete state time-stamp.", earliestTimeOfCompleteState);
        assertNotNull("Always have a set of object IDs.", objectIds);// guard
        assertNotNull("Always have a (non null) set of state transition IDs.", stateTransitionIds);

        for (ObjectStateId objectStateId : stateTransitionIds) {
            assertNotNull("The set of IDs of state transitions does not have a null element.", objectStateId);// guard
            ObjectStateIdTest.assertInvariants(objectStateId);
            final ObjectState stateTransition = universe.getStateTransition(objectStateId);
            final UUID object = objectStateId.getObject();
            assertThat(
                    "The set of IDs of state transitions does not have elements for object IDs that are not in the set of objects in this universe.",
                    object, isIn(objectIds));
            assertNotNull(
                    "Have a state transition if the given object state ID is one of the known state transition IDs of this universe.",
                    stateTransition);// guard
            ObjectStateTest.assertInvariants(stateTransition);
            assertDependentStateTransitionsInvariants(universe, objectStateId);

            final Map<UUID, ObjectStateId> dependencies = stateTransition.getDependencies();
            assertEquals(
                    "A state transition accessed using a given object state ID has an equivalent object state ID as its ID.",
                    objectStateId, stateTransition.getId());

            for (ObjectStateId dependency : dependencies.values()) {
                ObjectStateIdTest.assertInvariants(objectStateId, dependency);
                final boolean prehistoricDependency = dependency.getWhen().compareTo(earliestTimeOfCompleteState) <= 0;
                assertTrue(
                        "All the dependencies of the state transitions either "
                                + "have a time-stamp before the earliest complete state time-stamp of the universe, "
                                + "or are for known objects.",
                        prehistoricDependency || objectIds.contains(dependency.getObject()));
                final Set<ObjectStateId> dependencyDependentStateTransitions = assertDependentStateTransitionsInvariants(
                        universe, dependency);
                if (!prehistoricDependency) {
                    assertThat(
                            "The state transitions that depend on a given object state are consistent with the dependency information of the state transitions",
                            objectStateId, isIn(dependencyDependentStateTransitions));
                }
            }
        }

        for (UUID object : objectIds) {
            assertNotNull("The set of object IDs does not have a null element.", object);// guard
            final SortedMap<Duration, ObjectState> objectStateHistory = universe.getObjectStateHistory(object);
            final Duration whenFirstState = universe.getWhenFirstState(object);

            assertNotNull(
                    "A universe has an object state history for a given object if that object is one of the  objects in the universe.",
                    objectStateHistory);// guard
            assertNotNull("An object has a first state time-stamp if it is a known object.", whenFirstState);

            assertFalse("A object state history for a given object is not empty.", objectStateHistory.isEmpty());// guard
            assertSame(
                    "If an object is known, its first state time-stamp is the first key of the state history of that object.",
                    objectStateHistory.firstKey(), whenFirstState);
            assertNull(
                    "Known objects have an unknown state just before the first known state of the state history of that object.",
                    universe.getObjectState(object, whenFirstState.minusNanos(1L)));

            Map.Entry<Duration, ObjectState> previous = null;
            int nNull = 0;
            for (Map.Entry<Duration, ObjectState> entry : objectStateHistory.entrySet()) {
                final Duration when = entry.getKey();
                final ObjectState objectState = entry.getValue();

                assertSame(
                        "The state of an object at a given point in time is "
                                + "the state it had at the latest state transition "
                                + "at or before that point in time (at state transition)",
                        objectState, universe.getObjectState(object, when));
                if (objectState != null) {
                    ObjectStateTest.assertInvariants(objectState);
                    final ObjectStateId id = objectState.getId();
                    assertThat(
                            "All non null object states in the state history of a given object have the given object ID as the object ID of the state ID.",
                            id.getObject(), equalTo(object));
                    assertThat(
                            "An object state history for a given object maps to a null value or an object state with a time-stamp equal to the key of the map.",
                            id.getWhen(), equalTo(when));
                    assertThat(
                            "All non null object states in the state history of a given object have an ID that belongs to the set of all known object IDs.",
                            objectState.getId(), isIn(stateTransitionIds));
                    if (previous != null) {
                        final ObjectState previousState = previous.getValue();
                        ObjectStateTest.assertInvariants(objectState, previousState);
                        assertThat(
                                "All non null object states in the state history of a given object, except for the first, have as a dependency on the previous object state in the state history.",
                                objectState.getDependencies(), hasValue(previousState.getId()));
                    }
                }
                previous = entry;
            }
            assertThat("Only the last entry in an object state history may map to a null state (number of nulls).",
                    nNull, lessThanOrEqualTo(1));
            assertTrue("Only the last entry in an object state history may map to a null state (non-last is null).",
                    nNull == 0 || previous.getValue() == null);
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
    public void append_1A() {
        append_1(OBJECT_A, DURATION_1);
    }

    @Test
    public void append_1B() {
        append_1(OBJECT_B, DURATION_2);
    }

    @Test
    public void append_1PrehistoricDependencyA() {
        append_1PrehistoricDependency(DURATION_1, DURATION_2, DURATION_3);
    }

    @Test
    public void append_1PrehistoricDependencyB() {
        append_1PrehistoricDependency(DURATION_2, DURATION_3, DURATION_4);
    }

    @Test
    public void append_1PrehistoricDependencyClose() {
        append_1PrehistoricDependency(DURATION_2.minusNanos(1L), DURATION_2, DURATION_2);
    }

    @Test
    public void append_2DependencyA() {
        append_2Dependency(DURATION_1, DURATION_2, DURATION_3, OBJECT_A, OBJECT_B);
    }

    @Test
    public void append_2DependencyB() {
        append_2Dependency(DURATION_2, DURATION_3, DURATION_4, OBJECT_B, OBJECT_A);
    }

    @Test
    public void append_2DifferentObjectsA() {
        append_2DifferentObjects(OBJECT_A, OBJECT_B);
    }

    @Test
    public void append_2DifferentObjectsB() {
        append_2DifferentObjects(OBJECT_B, OBJECT_A);
    }

    @Test(expected = Universe.InvalidEventTimeStampOrderException.class)
    public void append_2OutOfOrderStatesA() {
        append_2OutOfOrderStates(DURATION_2, DURATION_1);
    }

    @Test(expected = Universe.InvalidEventTimeStampOrderException.class)
    public void append_2OutOfOrderStatesB() {
        append_2OutOfOrderStates(DURATION_3, DURATION_2);
    }

    @Test(expected = Universe.InvalidEventTimeStampOrderException.class)
    public void append_2OutOfOrderStatesClose() {
        append_2OutOfOrderStates(DURATION_2, DURATION_2.minusNanos(1L));
    }

    @Test(expected = Universe.InvalidEventTimeStampOrderException.class)
    public void append_2OutOfOrderStatesSame() {
        append_2OutOfOrderStates(DURATION_2, DURATION_2);
    }

    @Test
    public void append_2SuccessiveStatesA() {
        append_2SuccessiveStates(DURATION_1, DURATION_2);
    }

    @Test
    public void append_2SuccessiveStatesB() {
        append_2SuccessiveStates(DURATION_2, DURATION_3);
    }

    @Test
    public void append_2SuccessiveStatesClose() {
        append_2SuccessiveStates(DURATION_1, DURATION_1.plusNanos(1));
    }

    @Test
    public void append_3TransitiveDependencyA() {
        append_3TransitiveDependency(DURATION_1, DURATION_2, DURATION_3, DURATION_4, OBJECT_A, OBJECT_B, OBJECT_C);
    }

    @Test
    public void append_3TransitiveDependencyB() {
        append_3TransitiveDependency(DURATION_2, DURATION_3, DURATION_4, DURATION_5, OBJECT_B, OBJECT_C, OBJECT_A);
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
