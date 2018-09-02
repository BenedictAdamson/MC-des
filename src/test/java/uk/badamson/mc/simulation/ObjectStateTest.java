package uk.badamson.mc.simulation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import uk.badamson.mc.ObjectTest;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.collection.IsMapContaining.hasValue;

/**
 * <p>
 * Auxiliary test code for classes implementing the {@link ObjectState}
 * interface.
 * </p>
 */
public class ObjectStateTest {

    public static void assertInvariants(ObjectState state) {
        ObjectTest.assertInvariants(state);// inherited

        Duration duration = state.getDurationUntilNextEvent();
        final UUID objectId = state.getObjectId();

        assertNotNull("duration", duration);// guard
        assertNotNull("objectId", objectId);

        assertThat("duration", duration, greaterThan(Duration.ZERO));
    }

    public static void assertInvariants(ObjectState state1, ObjectState state2) {
        ObjectTest.assertInvariants(state1, state2);// inherited
    }

    public static Map<UUID, ObjectState> createNextStates(ObjectState state) {
        final UUID objectId = state.getObjectId();

        final Map<UUID, ObjectState> nextStates = state.createNextStates();

        assertInvariants(state);
        assertNotNull("Always return a map of object states", nextStates);// guard
        for (Map.Entry<UUID, ObjectState> entry : nextStates.entrySet()) {
            final UUID nextObject = entry.getKey();
            final ObjectState nextState = entry.getValue();
            assertNotNull("The map of object states does not have a null key.", nextObject);
            assertTrue(
                    "The map has no null values for objects other than the object ID of the object for which this is a state.",
                    nextObject.equals(objectId) || nextState != null);
            if (nextState != null) {
                assertInvariants(nextState);
                assertInvariants(state, nextState);
            }
        }
        assertThat("The map of object states has an entry for the object ID of the object for which this is a state.",
                nextStates, hasValue(objectId));

        return nextStates;
    }
}