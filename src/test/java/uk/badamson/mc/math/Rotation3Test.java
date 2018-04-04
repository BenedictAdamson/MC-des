package uk.badamson.mc.math;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the class {@link Rotation3}
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
    private static final double TOLERANCE = Math.nextAfter(1.0, Double.POSITIVE_INFINITY) - 1.0;

    private static final double SMALL_ANGLE = Math.PI * 0.003;

    private static ImmutableVector3 apply(Rotation3 r, ImmutableVector3 v) {
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

    private static void apply_0(ImmutableVector3 v) {
        final double magnitude0 = v.magnitude();

        final ImmutableVector3 rv = apply(Rotation3.ZERO, v);

        assertThat("Rotation by the zero rotation produces a rotated vector equal to the given vector.", rv,
                ImmutableVector3Test.closeTo(v, TOLERANCE * (magnitude0 + 1.0)));
    }

    private static void apply_axis(ImmutableVector3 axis, double angle) {
        final double magnitude0 = axis.magnitude();
        final Rotation3 r = Rotation3.valueOfAxisAngle(axis, angle);

        final ImmutableVector3 rv = apply(r, axis);

        assertTrue(
                "Rotation of a vector that lies along the rotation axis produces a rotated vector equal to the given vector.",
                axis.minus(rv).magnitude() < TOLERANCE * (magnitude0 + 1.0));
        assertThat(
                "Rotation of a vector that lies along the rotation axis produces a rotated vector equal to the given vector.",
                rv, ImmutableVector3Test.closeTo(axis, TOLERANCE * (magnitude0 + 1.0)));
    }

    private static void apply_basisHalfPi(ImmutableVector3 e, ImmutableVector3 eAxis, ImmutableVector3 expected) {
        final Rotation3 r = Rotation3.valueOfAxisAngle(eAxis, Math.PI * 0.5);

        final ImmutableVector3 actual = apply(r, e);

        assertThat(actual, ImmutableVector3Test.closeTo(expected, TOLERANCE));
    }

    public static void assertInvariants(Rotation3 rotation) {
        ObjectTest.assertInvariants(rotation);// inherited

        final Quaternion versor = rotation.getVersor();
        final double angle = rotation.getAngle();
        final ImmutableVector3 axis = rotation.getAxis();

        assertNotNull("Always have a versor.", versor);// guard
        assertNotNull("Always have an axis.", axis);// guard
        QuaternionTest.assertInvariants(versor);
        ImmutableVector3Test.assertInvariants(axis);

        final double axisMagnitude = axis.magnitude();

        assertThat("The versor has unit norm.", versor.norm(), closeTo(1.0, TOLERANCE));
        assertThat("The angle is in the range -2pi to 2pi", angle,
                allOf(greaterThanOrEqualTo(-2.0 * Math.PI), lessThanOrEqualTo(2.0 * Math.PI)));
        assertThat("The axis has a magnitude of 1 or 0.", axisMagnitude,
                anyOf(closeTo(0.0, TOLERANCE), closeTo(1.0, TOLERANCE)));
    }

    public static void assertInvariants(Rotation3 r1, Rotation3 r2) {
        ObjectTest.assertInvariants(r1, r2);// inherited
    }

    @Factory
    public static Matcher<Rotation3> closeToRotation3(Rotation3 operand) {
        return new IsCloseTo(operand, TOLERANCE);
    }

    @Factory
    public static Matcher<Rotation3> closeToRotation3(Rotation3 operand, double tolerance) {
        return new IsCloseTo(operand, tolerance);
    }

    private static Rotation3 valueOf(Quaternion quaternion) {
        final Rotation3 rotation = Rotation3.valueOf(quaternion);

        assertNotNull("Always creates a rotation", rotation);// guard
        assertInvariants(rotation);

        return rotation;
    }

    private static void valueOf_quaternionForAxisAngle(ImmutableVector3 axis, double angle, double magnitude) {
        final Rotation3 rotation0 = Rotation3.valueOfAxisAngle(axis, angle);
        final Quaternion quaternion = rotation0.getVersor().scale(magnitude);

        final Rotation3 rotation = valueOf(quaternion);

        assertThat("rotation", rotation, closeToRotation3(rotation0, TOLERANCE * 2));
    }

    private static Rotation3 valueOfAxisAngle(ImmutableVector3 axis, double angle) {
        final double sinAngle = Math.sin(angle);
        final double axisMagnitude = Math.abs(sinAngle) < Double.MIN_NORMAL ? 0.0 : axis.magnitude();

        final Rotation3 rotation = Rotation3.valueOfAxisAngle(axis, angle);

        assertNotNull("Always creates a rotation", rotation);// guard
        assertInvariants(rotation);
        assertThat("rotation cosine.", Math.cos(angle), closeTo(Math.cos(rotation.getAngle()), TOLERANCE));
        assertThat("rotation sine.", sinAngle, closeTo(Math.sin(rotation.getAngle()), TOLERANCE));
        assertThat("The rotation axis of the created rotation points in the same direction as the given axis.",
                axisMagnitude, closeTo(axis.dot(rotation.getAxis()), axisMagnitude * TOLERANCE));

        return rotation;
    }

    @Test
    public void apply_02i() {
        apply_0(ImmutableVector3.I.scale(2.0));
    }

    @Test
    public void apply_0i() {
        apply_0(ImmutableVector3.I);
    }

    @Test
    public void apply_0j() {
        apply_0(ImmutableVector3.J);
    }

    @Test
    public void apply_0k() {
        apply_0(ImmutableVector3.K);
    }

    @Test
    public void apply_axis_halfPiI() {
        apply_axis(ImmutableVector3.I, Math.PI * 0.5);
    }

    @Test
    public void apply_axis_halfPiJ() {
        apply_axis(ImmutableVector3.J, Math.PI * 0.5);
    }

    @Test
    public void apply_axis_halfPiK() {
        apply_axis(ImmutableVector3.K, Math.PI * 0.5);
    }

    @Test
    public void apply_basisHalfPiIJ() {
        apply_basisHalfPi(ImmutableVector3.I, ImmutableVector3.J, ImmutableVector3.K.minus());
    }

    @Test
    public void apply_basisHalfPiIK() {
        apply_basisHalfPi(ImmutableVector3.I, ImmutableVector3.K, ImmutableVector3.J);
    }

    @Test
    public void apply_basisHalfPiJI() {
        apply_basisHalfPi(ImmutableVector3.J, ImmutableVector3.I, ImmutableVector3.K);
    }

    @Test
    public void apply_basisHalfPiJK() {
        apply_basisHalfPi(ImmutableVector3.J, ImmutableVector3.K, ImmutableVector3.I.minus());
    }

    @Test
    public void apply_basisHalfPiKI() {
        apply_basisHalfPi(ImmutableVector3.K, ImmutableVector3.I, ImmutableVector3.J.minus());
    }

    @Test
    public void apply_basisHalfPiKJ() {
        apply_basisHalfPi(ImmutableVector3.K, ImmutableVector3.J, ImmutableVector3.I);
    }

    @Test
    public void statics() {
        assertNotNull("Has a zero rotation", Rotation3.ZERO);
        assertInvariants(Rotation3.ZERO);
        assertEquals("rotation angle of the zero rotation", 0.0, Rotation3.ZERO.getAngle(), Double.MIN_NORMAL);
    }

    @Test
    public void valueOf_quaternion_0() {
        final Rotation3 rotation = valueOf(Quaternion.ZERO);

        assertEquals("rotation", rotation, Rotation3.ZERO);
    }

    @Test
    public void valueOf_quaternionForAxisAngle_2iSmall() {
        valueOf_quaternionForAxisAngle(ImmutableVector3.I, SMALL_ANGLE, 2.0);
    }

    @Test
    public void valueOf_quaternionForAxisAngle_iHalfPi() {
        valueOf_quaternionForAxisAngle(ImmutableVector3.I, Math.PI * 0.5, 1.0);
    }

    @Test
    public void valueOf_quaternionForAxisAngle_iSmall() {
        valueOf_quaternionForAxisAngle(ImmutableVector3.I, SMALL_ANGLE, 1.0);
    }

    @Test
    public void valueOf_quaternionForAxisAngle_jSmall() {
        valueOf_quaternionForAxisAngle(ImmutableVector3.J, SMALL_ANGLE, 1.0);
    }

    @Test
    public void valueOf_quaternionForAxisAngle_kSmall() {
        valueOf_quaternionForAxisAngle(ImmutableVector3.K, SMALL_ANGLE, 1.0);
    }

    @Test
    public void valueOfAxisAngle_0I() {
        valueOfAxisAngle(ImmutableVector3.create(1, 0, 0), 0);
    }

    @Test
    public void valueOfAxisAngle_0J() {
        valueOfAxisAngle(ImmutableVector3.create(0, 1, 0), 0);
    }

    @Test
    public void valueOfAxisAngle_0K() {
        valueOfAxisAngle(ImmutableVector3.create(0, 0, 1), 0);
    }

    @Test
    public void valueOfAxisAngle_2HalfPiI() {
        valueOfAxisAngle(ImmutableVector3.create(2, 0, 0), Math.PI * 0.5);
    }

    @Test
    public void valueOfAxisAngle_2HalfPiJ() {
        valueOfAxisAngle(ImmutableVector3.create(0, 2, 0), Math.PI * 0.5);
    }

    @Test
    public void valueOfAxisAngle_2HalfPiK() {
        valueOfAxisAngle(ImmutableVector3.create(0, 0, 2), Math.PI * 0.5);
    }

    @Test
    public void valueOfAxisAngle_2PiI() {
        valueOfAxisAngle(ImmutableVector3.create(1, 0, 0), Math.PI * 2.0);
    }

    @Test
    public void valueOfAxisAngle_halfPiI() {
        valueOfAxisAngle(ImmutableVector3.create(1, 0, 0), Math.PI * 0.5);
    }

    @Test
    public void valueOfAxisAngle_halfPiJ() {
        valueOfAxisAngle(ImmutableVector3.create(0, 1, 0), Math.PI * 0.5);
    }

    @Test
    public void valueOfAxisAngle_halfPiK() {
        valueOfAxisAngle(ImmutableVector3.create(0, 0, 1), Math.PI * 0.5);
    }

    @Test
    public void valueOfAxisAngle_piI() {
        valueOfAxisAngle(ImmutableVector3.create(1, 0, 0), Math.PI);
    }

    @Test
    public void valueOfAxisAngle_smallI() {
        valueOfAxisAngle(ImmutableVector3.create(1, 0, 0), SMALL_ANGLE);
    }

    @Test
    public void valueOfAxisAngle_smallJ() {
        valueOfAxisAngle(ImmutableVector3.create(0, 1, 0), SMALL_ANGLE);
    }

    @Test
    public void valueOfAxisAngle_smallK() {
        valueOfAxisAngle(ImmutableVector3.create(0, 0, 1), SMALL_ANGLE);
    }
}
