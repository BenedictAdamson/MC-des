package uk.badamson.mc.simulation;

import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collection;
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

    private static final UUID OBJECT_A = ObjectStateIdTest.OBJECT_A;
    private static final UUID OBJECT_B = ObjectStateIdTest.OBJECT_B;
    private static final Duration DURATION_1 = Duration.ofSeconds(13);
    private static final Duration DURATION_2 = Duration.ofSeconds(17);
    private static final Duration DURATION_3 = Duration.ofSeconds(23);
    private static final UUID VERSION_A = ObjectStateIdTest.VERSION_A;

    public static void append(final Universe universe, ObjectState objectState)
            throws Universe.InvalidEventTimeStampOrderException {
        final ObjectStateId id = objectState.getId();
        final UUID object = id.getObject();
        final Duration when = id.getWhen();
        final Set<UUID> objectIds0 = universe.getObjectIds();
        final SortedMap<Duration, ObjectState> objectStateHistory0 = universe.getObjectStateHistory(object);

        try {
            universe.append(objectState);
        } catch (final Universe.InvalidEventTimeStampOrderException e) {// Permitted
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
        final ObjectStateId id = new ObjectStateId(object, when, VERSION_A);
        final Set<ObjectStateId> dependencies = Collections.emptySet();
        final ObjectState objectState = new AbstractObjectStateTest.TestObjectState(id, dependencies);

        final Universe universe = new Universe();

        append(universe, objectState);

        assertThat("The object ID of the ID of the given object state is one of the object IDs of this universe.",
                universe.getObjectIds(), equalTo(Collections.singleton(object)));
        assertThat(
                "The given object state is the last value in the object state history of the object of the ID of the given object state (value).",
                universe.getObjectStateHistory(object), equalTo(Collections.singletonMap(when, objectState)));
    }

    private static void append_2DifferentObjects(final UUID object1, final UUID object2) {
        assert !object1.equals(object2);
        final ObjectStateId id1 = new ObjectStateId(object1, DURATION_1, VERSION_A);
        final ObjectStateId id2 = new ObjectStateId(object2, DURATION_1, VERSION_A);
        final Set<ObjectStateId> dependencies = Collections.emptySet();
        final ObjectState objectState1 = new AbstractObjectStateTest.TestObjectState(id1, dependencies);
        final ObjectState objectState2 = new AbstractObjectStateTest.TestObjectState(id2, dependencies);

        final Universe universe = new Universe();
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
        final ObjectStateId id1 = new ObjectStateId(object, when1, VERSION_A);
        final ObjectStateId id2 = new ObjectStateId(object, when2, VERSION_A);
        final Set<ObjectStateId> dependencies = Collections.emptySet();
        final ObjectState objectState1 = new AbstractObjectStateTest.TestObjectState(id1, dependencies);
        final ObjectState objectState2 = new AbstractObjectStateTest.TestObjectState(id2, dependencies);
        final SortedMap<Duration, ObjectState> expectedObjectStateHistory = new TreeMap<>();
        expectedObjectStateHistory.put(when1, objectState1);

        final Universe universe = new Universe();
        universe.append(objectState1);

        append(universe, objectState2);// throws
    }

    private static void append_2SuccessiveStates(final Duration when1, final Duration when2) {
        assert when1.compareTo(when2) < 0;
        final UUID object = OBJECT_A;
        final ObjectStateId id1 = new ObjectStateId(object, when1, VERSION_A);
        final ObjectStateId id2 = new ObjectStateId(object, when2, VERSION_A);
        final Set<ObjectStateId> dependencies1 = Collections.emptySet();
        final Set<ObjectStateId> dependencies2 = Collections.singleton(id1);
        final ObjectState objectState1 = new AbstractObjectStateTest.TestObjectState(id1, dependencies1);
        final ObjectState objectState2 = new AbstractObjectStateTest.TestObjectState(id2, dependencies2);
        final SortedMap<Duration, ObjectState> expectedObjectStateHistory = new TreeMap<>();
        expectedObjectStateHistory.put(when1, objectState1);
        expectedObjectStateHistory.put(when2, objectState2);

        final Universe universe = new Universe();
        universe.append(objectState1);

        append(universe, objectState2);

        assertEquals("Object IDs.", Collections.singleton(object), universe.getObjectIds());
        assertEquals("Object state history.", expectedObjectStateHistory, universe.getObjectStateHistory(object));
    }

    public static void assertInvariants(Universe universe) {
        ObjectTest.assertInvariants(universe);// inherited

        final Set<UUID> objectIds = universe.getObjectIds();
        final Map<ObjectStateId, ObjectState> objectStates = universe.getObjectStates();

        assertNotNull("Always have a (non null) set of object IDs.", objectIds);// guard
        assertNotNull("Always have a map of IDs to object states.", objectStates);// guard

        for (Map.Entry<ObjectStateId, ObjectState> entry : objectStates.entrySet()) {
            final ObjectStateId id = entry.getKey();
            final ObjectState objectState = entry.getValue();
            assertNotNull("The map of IDs to object states does not have a null key.", id);// guard
            assertThat(
                    "The map of IDs to object states does not have IDs for object IDs that are in the set of objects in this universe.",
                    id.getObject(), isIn(objectIds));
            assertNotNull("The map of IDs to object states does not have null values.", objectState);// guard
            ObjectStateIdTest.assertInvariants(id);
            ObjectStateTest.assertInvariants(objectState);
            assertThat("The map of IDs to object states maps an ID to an object state that has the same ID.",
                    objectState.getId(), sameInstance(id));
        }

        final Collection<ObjectState> allObjectStates = objectStates.values();
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

            Map.Entry<Duration, ObjectState> previous = null;
            int nNull = 0;
            for (Map.Entry<Duration, ObjectState> entry : objectStateHistory.entrySet()) {
                final Duration when = entry.getKey();
                final ObjectState objectState = entry.getValue();
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
                            "All non null object states in the state history of a given object belong to the values collection of the object states map.",
                            objectState, isIn(allObjectStates));
                    if (previous != null) {
                        final ObjectState previousState = previous.getValue();
                        ObjectStateTest.assertInvariants(objectState, previousState);
                        assertThat(
                                "All non null object states in the state history of a given object, except for the first, have as a dependency on the previous object state in the state history.",
                                objectState.getDependencies(), hasItem(previousState.getId()));
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
    public void constructor() {
        final Universe universe = new Universe();

        assertInvariants(universe);

        assertEquals("The set of object IDs is empty.", Collections.emptySet(), universe.getObjectIds());
        assertEquals("The map of IDs to object states is empty.", Collections.emptyMap(), universe.getObjectStates());

        assertUnknownObjectInvariants(universe, OBJECT_A);
        assertUnknownObjectInvariants(universe, OBJECT_B);
    }

}
