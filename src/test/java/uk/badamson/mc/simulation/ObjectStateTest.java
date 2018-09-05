package uk.badamson.mc.simulation;

import static org.hamcrest.collection.IsMapContaining.hasValue;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Auxiliary test code for classes implementing the {@link ObjectState}
 * interface.
 * </p>
 */
public class ObjectStateTest {

    public static void assertInvariants(ObjectState state) {
        ObjectTest.assertInvariants(state);// inherited

        final ObjectStateId id = state.getId();
        final Set<ObjectStateId> dependencies = state.getDependencies();

        assertNotNull("id", id);// guard
        assertNotNull("dependencies", dependencies);// guard

        ObjectStateIdTest.assertInvariants(id);

        Set<UUID> dependentObjects = new HashSet<>(dependencies.size());
        for (ObjectStateId dependency : dependencies) {
            assertNotNull("The set of dependencies does not have a null entry.", dependency);// guard
            ObjectStateIdTest.assertInvariants(dependency);
            ObjectStateIdTest.assertInvariants(dependency, id);
            assertFalse("The set of dependencies does not have entries with duplicate object IDs.",
                    dependentObjects.contains(dependency.getObject()));
            assertThat("The set of dependencies has time-stamps after the time-stamp of the ID of this state.",
                    dependency.getWhen(), lessThan(id.getWhen()));
            dependentObjects.add(dependency.getObject());
        }
    }

    public static void assertInvariants(ObjectState state1, ObjectState state2) {
        ObjectTest.assertInvariants(state1, state2);// inherited

        final ObjectStateId id1 = state1.getId();
        final ObjectStateId id2 = state2.getId();

        ObjectStateIdTest.assertInvariants(id1, id2);
        assertEquals("Two ObjectState objects are equivalent if, and only if, they have equals IDs.", id1.equals(id2),
                state1.equals(state2));
    }

    public static Map<UUID, ObjectState> createNextStates(ObjectState state) {
        final ObjectStateId id = state.getId();

        final Map<UUID, ObjectState> nextStates = state.createNextStates();

        assertInvariants(state);
        assertNotNull("Always return a map of object states", nextStates);// guard

        Duration nextWhen = null;
        for (Map.Entry<UUID, ObjectState> entry : nextStates.entrySet()) {
            final UUID nextObject = entry.getKey();
            final ObjectState nextState = entry.getValue();
            assertNotNull("The map of object states does not have a null key.", nextObject);
            assertTrue("The map has no null values for objects other than the object ID of the ID of this state.",
                    nextObject.equals(id.getObject()) || nextState != null);
            if (nextState != null) {
                assertInvariants(nextState);
                assertInvariants(state, nextState);

                final ObjectStateId nextId = nextState.getId();
                assertEquals("Non null next state values have the object ID of their ID equal to their key.",
                        nextObject, nextId.getObject());
                final Duration nextWhen1 = nextId.getWhen();
                if (nextWhen == null) {
                    nextWhen = nextWhen1;
                    assertThat(
                            "All the values in the next states map are for a point in time after the point in time of this state.",
                            nextWhen, greaterThan(id.getWhen()));
                } else {
                    assertEquals("All the values  in the next states map are equal points in time.", nextWhen,
                            nextWhen1);
                }
            }
        }
        assertThat("The map of object states has an entry for the object ID of the ID of this state.", nextStates,
                hasValue(id.getObject()));

        return nextStates;
    }
}