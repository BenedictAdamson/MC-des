package uk.badamson.mc.simulation;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests and auxiliary test code for testing the
 * {@link ObjectStateDependency} class.
 * </p>
 */
public class ObjectStateDependencyTest {

    public static void assertInvariants(final ObjectStateDependency dependency) {
        ObjectTest.assertInvariants(dependency);// inherited
    }

    public static void assertInvariants(final ObjectStateDependency dependency1,
            final ObjectStateDependency dependency2) {
        ObjectTest.assertInvariants(dependency1, dependency2);// inherited
    }

    private static void constructor() {
        final ObjectStateDependency dependency = new ObjectStateDependency();

        assertInvariants(dependency);
    }

    @Test
    public void constructor_0() {
        constructor();
    }
}
