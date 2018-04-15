package uk.badamson.mc.math;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.badamson.mc.math.VectorTest.closeToVector;

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

    private static final double TOLERANCE = 4.0 * (Math.nextUp(1.0) - 1.0);

    public static final double SMALL_ANGLE = Math.PI / 180.0;

    public static final double HALF_PI = Math.PI * 0.5;

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

    public static Rotation3 minus(Rotation3 r) {
        final double angle = r.getAngle();
        final ImmutableVector3 axis = r.getAxis();

        final Rotation3 m = r.minus();

        assertNotNull("Not null, result", m);

        assertInvariants(r);// check for side effects
        assertInvariants(m);
        assertInvariants(m, r);

        final double minusAngle = r.getAngle();
        final ImmutableVector3 minusAxis = r.getAxis();

        assertThat(
                "The opposite rotation either has the same axis but the negative of the angle of this rotation, "
                        + "or the same angle but an axis that points in the opposite direction (angle).",
                minusAngle, anyOf(closeTo(angle, TOLERANCE), closeTo(-angle, TOLERANCE)));
        assertThat(
                "The opposite rotation either has the same axis but the negative of the angle of this rotation, "
                        + "or the same angle but an axis that points in the opposite direction (axis).",
                minusAxis, anyOf(closeToVector(axis, TOLERANCE), closeToVector(axis.minus(), TOLERANCE)));

        return m;
    }

    public static Rotation3 minus(Rotation3 r, Rotation3 that) {
        final Rotation3 diff = r.minus(that);

        assertNotNull("Not null, result", diff);

        assertInvariants(r);// check for side effects
        assertInvariants(diff);
        assertInvariants(diff, r);
        assertInvariants(diff, that);

        assertThat(
                "The difference between this rotation and the given rotation is the rotation that, "
                        + "if added to the given rotation would produce this rotation.",
                that.plus(diff), closeToRotation3(r));

        return diff;
    }

    public static double normalizedAngle(double a) {
        return a % (2.0 * Math.PI);
    }

    public static Rotation3 plus(Rotation3 r, Rotation3 that) {
        final Rotation3 sum = r.plus(that);

        assertNotNull("Not null, result", sum);
        assertInvariants(r);// check for side effects
        assertInvariants(that);// check for side effects
        assertInvariants(r, that);// check for side effects
        assertInvariants(sum);
        assertInvariants(sum, r);
        assertInvariants(sum, that);

        return sum;
    }

    public static Rotation3 scale(Rotation3 r, double f) {
        final Rotation3 fr = r.scale(f);

        assertNotNull("Not null, result", fr);
        assertInvariants(r);// check for side effects
        assertInvariants(fr);
        assertInvariants(fr, r);

        assertTrue("The scaled rotation has same axis as this, unless the scaling factor is zero",
                Math.abs(f) < Double.MIN_NORMAL
                        || ImmutableVector3Test.closeTo(r.getAxis(), TOLERANCE).matches(fr.getAxis()));
        assertThat("The scaled rotation has its angle nominally scaled by the scaling factor.",
                normalizedAngle(fr.getAngle()), closeTo(normalizedAngle(r.getAngle() * f), TOLERANCE));

        return fr;
    }
}
