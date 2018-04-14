package uk.badamson.mc.math;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.badamson.mc.math.ImmutableVector3Test.closeToImmutableVector3;
import static uk.badamson.mc.math.Rotation3Test.closeToRotation3;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the class {@link Rotation3AxisAngle}
 * </p>
 */
public class Rotation3AxisAngleTest {

    private static final double TOLERANCE = 4.0 * (Math.nextAfter(1.0, Double.POSITIVE_INFINITY) - 1.0);

    private static final double SMALL_ANGLE = Math.PI * 0.003;

    private static ImmutableVector3 apply(Rotation3AxisAngle r, ImmutableVector3 v) {
        final ImmutableVector3 rv = Rotation3Test.apply(r, v);// inherited

        assertInvariants(r);// check for side effects

        return rv;
    }

    private static void apply_0(ImmutableVector3 v) {
        final double magnitude0 = v.magnitude();

        final ImmutableVector3 rv = apply(Rotation3AxisAngle.ZERO, v);

        assertThat("Rotation by the zero rotation produces a rotated vector equal to the given vector.", rv,
                ImmutableVector3Test.closeTo(v, TOLERANCE * (magnitude0 + 1.0)));
    }

    private static void apply_axis(ImmutableVector3 axis, double angle) {
        final double magnitude0 = axis.magnitude();
        final Rotation3AxisAngle r = Rotation3AxisAngle.valueOfAxisAngle(axis, angle);

        final ImmutableVector3 rv = apply(r, axis);

        assertTrue(
                "Rotation of a vector that lies along the rotation axis produces a rotated vector equal to the given vector.",
                axis.minus(rv).magnitude() < TOLERANCE * (magnitude0 + 1.0));
        assertThat(
                "Rotation of a vector that lies along the rotation axis produces a rotated vector equal to the given vector.",
                rv, ImmutableVector3Test.closeTo(axis, TOLERANCE * (magnitude0 + 1.0)));
    }

    private static void apply_basisHalfPi(ImmutableVector3 e, ImmutableVector3 eAxis, ImmutableVector3 expected) {
        final Rotation3AxisAngle r = Rotation3AxisAngle.valueOfAxisAngle(eAxis, Math.PI * 0.5);

        final ImmutableVector3 actual = apply(r, e);

        assertThat(actual, ImmutableVector3Test.closeTo(expected, TOLERANCE));
    }

    public static void assertInvariants(Rotation3AxisAngle rotation) {
        ObjectTest.assertInvariants(rotation);// inherited
        Rotation3Test.assertInvariants(rotation);// inherited
    }

    public static void assertInvariants(Rotation3AxisAngle r1, Rotation3AxisAngle r2) {
        ObjectTest.assertInvariants(r1, r2);// inherited
        Rotation3Test.assertInvariants(r1, r2);// inherited
    }

    private static double normalizedAngle(double a) {
        return a % (2.0 * Math.PI);
    }

    public static Rotation3 plus(Rotation3AxisAngle r, Rotation3 that) {
        final Rotation3 sum = Rotation3Test.plus(r, that);

        assertInvariants(r);// check for side effects

        return sum;
    }

    private static void plus_0r(Rotation3AxisAngle that) {
        final Rotation3 sum = plus(Rotation3AxisAngle.ZERO, that);
        assertThat("sum", sum, closeToRotation3(that));
    }

    private static void plus_r0(Rotation3AxisAngle r) {
        final Rotation3 sum = plus(r, Rotation3AxisAngle.ZERO);
        assertThat("sum", sum, closeToRotation3(r));
    }

    private static void plus_sameAxis(ImmutableVector3 axis, double angle1, double angle2) {
        final Rotation3AxisAngle r1 = Rotation3AxisAngle.valueOfAxisAngle(axis, angle1);
        final Rotation3AxisAngle r2 = Rotation3AxisAngle.valueOfAxisAngle(axis, angle2);

        final Rotation3 sum = plus(r1, r2);

        assertThat("axis", sum.getAxis(), closeToImmutableVector3(axis, TOLERANCE));
        assertThat("normalized angle", normalizedAngle(sum.getAngle()),
                closeTo(normalizedAngle(angle1 + angle2), TOLERANCE));
    }

    private static Rotation3AxisAngle valueOf(Quaternion quaternion) {
        final Rotation3AxisAngle rotation = Rotation3AxisAngle.valueOf(quaternion);

        assertNotNull("Always creates a rotation", rotation);// guard
        assertInvariants(rotation);

        return rotation;
    }

    private static void valueOf_quaternionForAxisAngle(ImmutableVector3 axis, double angle, double magnitude) {
        final Rotation3AxisAngle rotation0 = Rotation3AxisAngle.valueOfAxisAngle(axis, angle);
        final Quaternion quaternion = rotation0.getVersor().scale(magnitude);

        final Rotation3AxisAngle rotation = valueOf(quaternion);

        assertThat("rotation", rotation, closeToRotation3(rotation0, TOLERANCE * 2));
    }

    private static Rotation3AxisAngle valueOfAxisAngle(ImmutableVector3 axis, double angle) {
        final double sinAngle = Math.sin(angle);
        final double axisMagnitude = Math.abs(sinAngle) < Double.MIN_NORMAL ? 0.0 : axis.magnitude();

        final Rotation3AxisAngle rotation = Rotation3AxisAngle.valueOfAxisAngle(axis, angle);

        assertNotNull("Always creates a rotation", rotation);// guard
        assertInvariants(rotation);
        assertEquals("angle.", angle, rotation.getAngle(), TOLERANCE);
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
    public void plus_00() {
        plus_0r(Rotation3AxisAngle.ZERO);
    }

    @Test
    public void plus_0ISmall() {
        plus_0r(Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void plus_0JSmall() {
        plus_0r(Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE));
    }

    @Test
    public void plus_0KSmall() {
        plus_0r(Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE));
    }

    @Test
    public void plus_ISmall0() {
        plus_r0(Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void plus_JSmall0() {
        plus_r0(Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE));
    }

    @Test
    public void plus_KSmall0() {
        plus_r0(Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE));
    }

    @Test
    public void plus_sameAxisISmallSmall() {
        plus_sameAxis(ImmutableVector3.I, SMALL_ANGLE, SMALL_ANGLE);
    }

    @Test
    public void plus_sameAxisJSmallSmall() {
        plus_sameAxis(ImmutableVector3.J, SMALL_ANGLE, SMALL_ANGLE);
    }

    @Test
    public void plus_sameAxisKSmallSmall() {
        plus_sameAxis(ImmutableVector3.K, SMALL_ANGLE, SMALL_ANGLE);
    }

    @Test
    public void statics() {
        assertNotNull("Has a zero rotation", Rotation3AxisAngle.ZERO);
        assertInvariants(Rotation3AxisAngle.ZERO);
        assertEquals("rotation angle of the zero rotation", 0.0, Rotation3AxisAngle.ZERO.getAngle(), Double.MIN_NORMAL);
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
        valueOfAxisAngle(ImmutableVector3.I, 0);
    }

    @Test
    public void valueOfAxisAngle_0J() {
        valueOfAxisAngle(ImmutableVector3.J, 0);
    }

    @Test
    public void valueOfAxisAngle_0K() {
        valueOfAxisAngle(ImmutableVector3.K, 0);
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
        valueOfAxisAngle(ImmutableVector3.I, Math.PI * 2.0);
    }

    @Test
    public void valueOfAxisAngle_4PiI() {
        valueOfAxisAngle(ImmutableVector3.I, Math.PI * 4.0);
    }

    @Test
    public void valueOfAxisAngle_halfPiI() {
        valueOfAxisAngle(ImmutableVector3.I, Math.PI * 0.5);
    }

    @Test
    public void valueOfAxisAngle_halfPiJ() {
        valueOfAxisAngle(ImmutableVector3.J, Math.PI * 0.5);
    }

    @Test
    public void valueOfAxisAngle_halfPiK() {
        valueOfAxisAngle(ImmutableVector3.K, Math.PI * 0.5);
    }

    @Test
    public void valueOfAxisAngle_piI() {
        valueOfAxisAngle(ImmutableVector3.I, Math.PI);
    }

    @Test
    public void valueOfAxisAngle_smallI() {
        valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE);
    }

    @Test
    public void valueOfAxisAngle_smallJ() {
        valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE);
    }

    @Test
    public void valueOfAxisAngle_smallK() {
        valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE);
    }
}
