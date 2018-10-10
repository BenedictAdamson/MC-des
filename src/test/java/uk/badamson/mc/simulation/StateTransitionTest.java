package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@linkplain StateTransition}
 * class.
 * </p>
 */
public class StateTransitionTest {

    private static final UUID OBJECT_A = ObjectStateTest.OBJECT_A;
    private static final UUID OBJECT_B = ObjectStateTest.OBJECT_B;
    private static final UUID OBJECT_C = UUID.randomUUID();
    private static final Duration WHEN_1 = ObjectStateTest.WHEN_1;
    private static final Duration WHEN_2 = ObjectStateTest.WHEN_2;
    private static final Map<UUID, ObjectStateId> DEPENDENCIES_A = Collections.emptyMap();
    private static final Map<UUID, ObjectStateId> DEPENDENCIES_B = Collections.singletonMap(OBJECT_A,
            new ObjectStateId(OBJECT_A, WHEN_1));
    private static final ObjectState STATE_A = new ObjectStateTest.TestObjectState(1, OBJECT_A, WHEN_1, DEPENDENCIES_A);
    private static final ObjectState STATE_B = new ObjectStateTest.TestObjectState(2, OBJECT_B, WHEN_2, DEPENDENCIES_B);
    private static final ObjectState STATE_C = new ObjectStateTest.TestObjectState(3, OBJECT_C, WHEN_2, DEPENDENCIES_B);
    private static final Map<UUID, ObjectState> STATES_A = Collections.singletonMap(OBJECT_A, STATE_A);
    private static final Map<UUID, ObjectState> STATES_B = Collections.singletonMap(OBJECT_B, STATE_B);
    private static final Map<UUID, ObjectState> STATES_C = Collections
            .unmodifiableMap(Map.of(OBJECT_B, STATE_B, OBJECT_C, STATE_C));

    public static void assertInvariants(StateTransition stateTransition) {
        ObjectTest.assertInvariants(stateTransition);// inherited

        for (Map.Entry<UUID, ObjectState> entry : stateTransition.getStates().entrySet()) {
            final UUID nextObject = entry.getKey();
            final ObjectState nextState = entry.getValue();
            assertNotNull("The map of object states does not have a null key.", nextObject);
            if (nextState != null) {
                ObjectStateTest.assertInvariants(nextState);
            }
        }
    }

    public static void assertInvariants(StateTransition stateTransition1, StateTransition stateTransition2) {
        ObjectTest.assertInvariants(stateTransition1, stateTransition2);// inherited
    }

    private static StateTransition constructor(Duration when, Map<UUID, ObjectState> states,
            Map<UUID, ObjectStateId> dependencies) {
        final StateTransition stateTransition = new StateTransition(when, states, dependencies);

        assertInvariants(stateTransition);
        assertSame("The time-stamp of this state transition is the same as the given time-stamp.", when,
                stateTransition.getWhen());
        assertEquals("The states map of this state transition is equal to the given states map.", states,
                stateTransition.getStates());
        assertEquals("The dependencies map of this state transition is equal to the given dependencies map.",
                dependencies, stateTransition.getDependencies());

        return stateTransition;
    }

    @Test
    public void constructor_A() {
        constructor(WHEN_1, STATES_A, DEPENDENCIES_A);
    }

    @Test
    public void constructor_B() {
        constructor(WHEN_2, STATES_B, DEPENDENCIES_B);
    }

    @Test
    public void constructor_C() {
        constructor(WHEN_2, STATES_C, DEPENDENCIES_B);
    }
}
