package uk.badamson.mc.simulation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;
import java.util.UUID;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Auxiliary test code for classes implementing the {@link ObjectStateId}
 * interface.
 * </p>
 */
public class ObjectStateIdTest {

    public static void assertInvariants(ObjectStateId id) {
        ObjectTest.assertInvariants(id);// inherited

        final UUID objectId = id.getObject();
        final Duration when = id.getWhen();

        assertNotNull("objectId", objectId);
        assertNotNull("when", when);
    }

    public static void assertInvariants(ObjectStateId id1, ObjectStateId id2) {
        ObjectTest.assertInvariants(id1, id2);// inherited

        final boolean equals = id1.equals(id2);
        assertFalse("ObjectState.Id objects are equivalent only if they have equals object IDs",
                equals && !id1.getObject().equals(id2.getObject()));
        assertFalse("ObjectState.Id objects are equivalent only if they have equals timestamps",
                equals && !id1.getWhen().equals(id2.getWhen()));
    }

}// class