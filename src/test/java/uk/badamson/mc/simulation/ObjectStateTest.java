package uk.badamson.mc.simulation;

import static org.hamcrest.collection.IsMapContaining.hasValue;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.simulation.ObjectState.Id;

/**
 * <p>
 * Auxiliary test code for classes implementing the {@link ObjectState}
 * interface.
 * </p>
 */
public class ObjectStateTest {

    /**
     * <p>
     * Auxiliary test code for classes implementing the {@link ObjectState.Id}
     * interface.
     * </p>
     */
    public static class IdTest {

        public static void assertInvariants(ObjectState.Id id) {
            ObjectTest.assertInvariants(id);// inherited

            final UUID objectId = id.getObject();
            final Duration when = id.getWhen();

            assertNotNull("objectId", objectId);
            assertNotNull("when", when);
        }

        public static void assertInvariants(ObjectState.Id id1, ObjectState.Id id2) {
            ObjectTest.assertInvariants(id1, id2);// inherited

            final boolean equals = id1.equals(id2);
            assertFalse("ObjectState.Id objects are equivalent only if they have equals object IDs",
                    equals && !id1.getObject().equals(id2.getObject()));
            assertFalse("ObjectState.Id objects are equivalent only if they have equals timestamps",
                    equals && !id1.getWhen().equals(id2.getWhen()));
        }

    }// class

    public static void assertInvariants(ObjectState state) {
        ObjectTest.assertInvariants(state);// inherited

        final ObjectState.Id id = state.getId();

        assertNotNull("id", id);// guard
        ObjectStateTest.IdTest.assertInvariants(id);
    }

    public static void assertInvariants(ObjectState state1, ObjectState state2) {
        ObjectTest.assertInvariants(state1, state2);// inherited

        final Id id1 = state1.getId();
        final Id id2 = state2.getId();

        ObjectStateTest.IdTest.assertInvariants(id1, id2);
        assertEquals("Two ObjectState objects are equivalent if, and only if, they have equals IDs.", id1.equals(id2),
                state1.equals(state2));
    }

    public static Map<UUID, ObjectState> createNextStates(ObjectState state) {
        final ObjectState.Id id = state.getId();

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

                final Id nextId = nextState.getId();
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