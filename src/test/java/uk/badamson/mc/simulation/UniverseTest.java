package uk.badamson.mc.simulation;

import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the {@link Universe} class.
 * </p>
 */
public class UniverseTest {

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
            SortedMap<Duration, ObjectState> objectStateHistory = universe.getObjectStateHistory(object);

            assertNotNull(
                    "A universe has an object state history for a given object if that object is one of the  objects in the universe.",
                    objectStateHistory);// guard
            assertFalse("A object state history for a given object is not empty.", objectStateHistory.isEmpty());

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

    @Test
    public void constructor() {
        final Universe universe = new Universe();

        assertInvariants(universe);
        assertEquals("The set of object IDs is empty.", Collections.emptySet(), universe.getObjectIds());
        assertEquals("The map of IDs to object states is empty.", Collections.emptyMap(), universe.getObjectStates());
    }

}
