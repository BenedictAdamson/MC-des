package uk.badamson.mc.simulation;

import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Map;

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

        final Map<ObjectStateId, ObjectState> objectStates = universe.getObjectStates();

        assertNotNull("Always have a map of IDs to object states.", objectStates);// guard

        for (Map.Entry<ObjectStateId, ObjectState> entry : objectStates.entrySet()) {
            final ObjectStateId id = entry.getKey();
            final ObjectState objectState = entry.getValue();
            assertNotNull("The map of IDs to object states does not have a null key.", id);// guard
            assertNotNull("The map of IDs to object states does not have null values.", objectState);// guard
            ObjectStateIdTest.assertInvariants(id);
            ObjectStateTest.assertInvariants(objectState);
            assertThat("The map of IDs to object states maps an ID to an object state that has the same ID.",
                    objectState.getId(), sameInstance(id));
        }
    }

    public static void assertInvariants(Universe universe1, Universe universe2) {
        ObjectTest.assertInvariants(universe1, universe2);// inherited
    }

    @Test
    public void constructor() {
        final Universe universe = new Universe();

        assertInvariants(universe);
        assertEquals("The map of IDs to object states is empty.", Collections.emptyMap(), universe.getObjectStates());
    }

}
