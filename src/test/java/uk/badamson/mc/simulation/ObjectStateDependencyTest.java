package uk.badamson.mc.simulation;

import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests and auxiliary test code for testing the
 * {@link ObjectStateDependency} class.
 * </p>
 */
public class ObjectStateDependencyTest {

    private static final UUID OBJECT_A = ObjectStateIdTest.OBJECT_A;
    private static final UUID OBJECT_B = ObjectStateIdTest.OBJECT_B;
    private static final Duration WHEN_1 = Duration.ZERO;
    private static final Duration WHEN_2 = Duration.ofSeconds(13);
    private static final Duration WHEN_3 = Duration.ofSeconds(15);
    private static final UUID VERSION_A = ObjectStateIdTest.VERSION_A;
    private static final UUID VERSION_B = ObjectStateIdTest.VERSION_B;

    public static void assertInvariants(final ObjectStateDependency dependency) {
        ObjectTest.assertInvariants(dependency);// inherited

        final ObjectStateId previousStateTransition = dependency.getPreviousStateTransition();
        final Duration when = dependency.getWhen();
        final UUID dependedUpObject = dependency.getDependedUpObject();

        assertNotNull("Always have a previous state transition.", previousStateTransition);// guard
        assertNotNull("Always have a time at which the depended upon object had the depended upon state.", when);
        assertNotNull("Always have a depended upon object.", dependedUpObject);

        ObjectStateIdTest.assertInvariants(previousStateTransition);

        assertThat(
                "The time at which the depended upon object had the depended upon state is "
                        + "at or after the time of the previous state transition of the depended upon object.",
                when, greaterThanOrEqualTo(previousStateTransition.getWhen()));
        assertSame("The depended upon object is the  object of the previous state transition.",
                previousStateTransition.getObject(), dependedUpObject);
    }

    public static void assertInvariants(final ObjectStateDependency dependency1,
            final ObjectStateDependency dependency2) {
        ObjectTest.assertInvariants(dependency1, dependency2);// inherited

    }

    private static void constructor(Duration when, ObjectStateId previousStateTransition) {
        final ObjectStateDependency dependency = new ObjectStateDependency(when, previousStateTransition);

        assertInvariants(dependency);
        assertSame(
                "The point in time at which the depended upon object had the depended upon state is the given time-stamp.",
                when, dependency.getWhen());
        assertSame("The previous state transition is the given previous state transition.", previousStateTransition,
                dependency.getPreviousStateTransition());
    }

    private ObjectStateId objectStateId1;

    private ObjectStateId objectStateId2;

    @Test
    public void constructor_A() {
        constructor(WHEN_2, objectStateId1);
    }

    @Test
    public void constructor_B() {
        constructor(WHEN_3, objectStateId2);
    }

    @Test
    public void constructor_C() {
        constructor(objectStateId1.getWhen(), objectStateId1);
    }

    @Before
    public void setUp() {
        objectStateId1 = new ObjectStateId(OBJECT_A, WHEN_1, VERSION_A);
        objectStateId2 = new ObjectStateId(OBJECT_B, WHEN_2, VERSION_B);
    }
}
