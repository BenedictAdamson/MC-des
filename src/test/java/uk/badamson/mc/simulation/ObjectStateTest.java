package uk.badamson.mc.simulation;

import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link ObjectState} class.
 * </p>
 */
public class ObjectStateTest {

    static final class TestObjectState extends ObjectState {

        public TestObjectState(UUID object, Duration when, Map<UUID, ObjectStateId> dependencies) {
            super(object, when, dependencies);
        }

        @Override
        public StateTransition createNextStateTransition(ObjectStateId idOfThisState) {
            Objects.requireNonNull(idOfThisState, "idOfThisState");
            final UUID object = idOfThisState.getObject();
            final Map<UUID, ObjectState> states = Collections.singletonMap(object, (ObjectState) null);
            final Map<UUID, ObjectStateId> dependencies = Collections.singletonMap(object, idOfThisState);
            return new StateTransition(idOfThisState.getWhen().plusSeconds(7), states, dependencies);
        }

    }// class

    static final UUID OBJECT_A = UUID.randomUUID();
    static final UUID OBJECT_B = UUID.randomUUID();
    static final Duration WHEN_1 = Duration.ofSeconds(13);
    static final Duration WHEN_2 = Duration.ofSeconds(23);

    public static void assertInvariants(ObjectState state) {
        ObjectTest.assertInvariants(state);// inherited
    }

    public static void assertInvariants(ObjectState state1, ObjectState state2) {
        ObjectTest.assertInvariants(state1, state2);// inherited
    }

    private static void constructor(UUID object, Duration when, Map<UUID, ObjectStateId> dependencies) {
        final ObjectState state = new TestObjectState(object, when, dependencies);

        assertInvariants(state);
    }

    public static StateTransition createNextStates(ObjectState state, ObjectStateId idOfThisState) {
        final StateTransition stateTransition = state.createNextStateTransition(idOfThisState);

        assertInvariants(state);
        assertNotNull("Always return a state transition", stateTransition);// guard
        StateTransitionTest.assertInvariants(stateTransition);
        assertThat("The states of the state transition has an entry for the the object of the given state ID.",
                idOfThisState.getObject(), isIn(stateTransition.getStates().keySet()));
        for (var entry : stateTransition.getStates().entrySet()) {
            final UUID object = entry.getKey();
            final ObjectState stateAfterTransition = entry.getValue();
            assertFalse(
                    "The states of the state transition has no null values for objects other than the object of the given state ID.",
                    !object.equals(idOfThisState.getObject()) && stateAfterTransition == null);
        }
        assertThat("The time of the state transition is after the the time-stamp of the given state ID.",
                stateTransition.getWhen(), greaterThan(idOfThisState.getWhen()));
        assertThat("The given state ID is one of the values of the dependencies of the state transition.",
                idOfThisState, isIn(stateTransition.getDependencies().values()));

        return stateTransition;
    }

    @Test
    public void constructor_A() {
        final Map<UUID, ObjectStateId> dependencies = Collections.emptyMap();
        constructor(OBJECT_A, WHEN_1, dependencies);
    }

    @Test
    public void constructor_B() {
        final Map<UUID, ObjectStateId> dependencies = Collections.singletonMap(OBJECT_A,
                new ObjectStateId(OBJECT_A, WHEN_1));
        constructor(OBJECT_B, WHEN_2, dependencies);
    }
}