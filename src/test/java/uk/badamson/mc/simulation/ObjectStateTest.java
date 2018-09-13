package uk.badamson.mc.simulation;

import static org.hamcrest.collection.IsMapContaining.hasValue;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests and auxiliary test code for the {@link ObjectState} class.
 * </p>
 */
public class ObjectStateTest {

    static final class TestObjectState extends ObjectState {

        public TestObjectState(UUID object, Duration when, UUID version,
                Map<UUID, ObjectStateDependency> dependencies) {
            super(object, when, version, dependencies);
        }

        @Override
        public Map<UUID, ObjectState> createNextStates() {
            return Collections.singletonMap(getId().getObject(), (ObjectState) null);
        }

        @Override
        public String toString() {
            return "TestObjectState[" + getId() + "]";
        }

    }// class

    public static void assertInvariants(ObjectState state) {
        ObjectTest.assertInvariants(state);// inherited

        final ObjectStateId id = state.getId();
        final Map<UUID, ObjectStateDependency> dependencies = state.getDependencies();
        final UUID object = state.getObject();
        final UUID version = state.getVersion();
        final Duration when = state.getWhen();

        assertNotNull("id", id);// guard
        assertNotNull("dependencies", dependencies);// guard
        assertNotNull("object", object);
        assertNotNull("version", version);
        assertNotNull("when", when);// guard

        ObjectStateIdTest.assertInvariants(id);

        assertSame("The object of the ID is the same as the object of this state.", object, id.getObject());
        assertSame("The version of the ID is the same as the version of this state.", version, id.getVersion());
        assertSame("The time-stamp of the ID is the same as the time-stamp of this state.", when, id.getWhen());

        Set<UUID> dependentObjects = new HashSet<>(dependencies.size());
        for (var entry : dependencies.entrySet()) {
            final UUID dependencyObject = entry.getKey();
            final ObjectStateDependency dependency = entry.getValue();
            assertNotNull("The dependency map does not have a null key.", dependencyObject);
            assertNotNull("The dependency map does not have null values.", dependency);// guard
            ObjectStateDependencyTest.assertInvariants(dependency);

            assertSame(
                    "Each object ID key of the dependency map maps to a value that "
                            + "has that same object ID as its depended upon object.",
                    dependencyObject, dependency.getDependedUpObject());
            assertThat(
                    "The time-stamp of every value in the dependency map is before the time-stamp of the ID of this state.",
                    dependency.getWhen(), lessThan(when));
            dependentObjects.add(dependencyObject);
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
        final ObjectStateDependency stateAsDependency = new ObjectStateDependency(id.getWhen(), id);
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
                assertThat(
                        "All the values in the next states map have the ID of this state as one of their dependencies.",
                        nextState.getDependencies(), hasValue(stateAsDependency));
            }
        }
        assertThat("The map of object states has an entry for the object ID of the ID of this state.", nextStates,
                hasValue(id.getObject()));

        return nextStates;
    }
}