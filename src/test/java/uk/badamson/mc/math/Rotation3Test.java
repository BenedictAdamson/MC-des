package uk.badamson.mc.math;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertNotNull;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * <p>
 * Unit tests and auxiliary testing code for classes that implement the
 * {@link Rotation3} interface.
 * </p>
 */
public class Rotation3Test {

    private static class IsCloseTo extends TypeSafeMatcher<Rotation3> {
        private final double tolerance;
        private final Rotation3 value;

        private IsCloseTo(Rotation3 value, double tolerance) {
            this.tolerance = tolerance;
            this.value = value;
        }

        @Override
        public void describeMismatchSafely(Rotation3 item, Description mismatchDescription) {
            mismatchDescription.appendValue(item).appendText(" differed by ").appendValue(distance(item));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a rotation within ").appendValue(tolerance).appendText(" of ").appendValue(value);
        }

        private final double distance(Rotation3 item) {
            return value.getVersor().distance(item.getVersor());
        }

        @Override
        public boolean matchesSafely(Rotation3 item) {
            return distance(item) <= tolerance;
        }
    }// class

    private static final double TOLERANCE = 4.0 * (Math.nextAfter(1.0, Double.POSITIVE_INFINITY) - 1.0);

    public static ImmutableVector3 apply(Rotation3 r, ImmutableVector3 v) {
        final double magnitude0 = v.magnitude();

        final ImmutableVector3 rv = r.apply(v);

        assertNotNull("Always produces a rotated vector.", rv);// guard
        assertInvariants(r);// check for side effects
        ImmutableVector3Test.assertInvariants(v);// check for side effects
        ImmutableVector3Test.assertInvariants(rv);
        ImmutableVector3Test.assertInvariants(rv, v);
        assertThat("The rotated vector has the same magnitude as the given vector.", rv.magnitude(),
                closeTo(magnitude0, TOLERANCE * (magnitude0 + 1.0)));

        return rv;
    }

    public static void assertInvariants(Rotation3 rotation) {
        final Quaternion versor = rotation.getVersor();
        final ImmutableVector3 axis = rotation.getAxis();

        assertNotNull("Always have a versor.", versor);// guard
        assertNotNull("Always have an axis.", axis);// guard
        QuaternionTest.assertInvariants(versor);
        ImmutableVector3Test.assertInvariants(axis);

        final double axisMagnitude = axis.magnitude();

        assertThat("The versor has unit norm.", versor.norm(), closeTo(1.0, TOLERANCE));
        assertThat("The axis has a magnitude of 1 or 0.", axisMagnitude,
                anyOf(closeTo(0.0, TOLERANCE), closeTo(1.0, TOLERANCE)));
    }

    public static void assertInvariants(Rotation3 r1, Rotation3 r2) {
        // Do nothing
    }

    @Factory
    public static Matcher<Rotation3> closeToRotation3(Rotation3 operand) {
        return new IsCloseTo(operand, TOLERANCE);
    }

    @Factory
    public static Matcher<Rotation3> closeToRotation3(Rotation3 operand, double tolerance) {
        return new IsCloseTo(operand, tolerance);
    }
}
