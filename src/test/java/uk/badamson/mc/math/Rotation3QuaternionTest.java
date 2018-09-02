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
import static uk.badamson.mc.math.ImmutableVector3Test.closeToImmutableVector3;
import static uk.badamson.mc.math.Rotation3Test.closeToRotation3;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the class {@link Rotation3Quaternion}
 * </p>
 */
public class Rotation3QuaternionTest {

    private static final double SMALL_ANGLE = Rotation3Test.SMALL_ANGLE;
    private static final double HALF_PI = Rotation3Test.HALF_PI;
    private static final double TOLERANCE = 4.0 * (Math.nextUp(1.0) - 1.0);

    private static ImmutableVector3 apply(Rotation3Quaternion r, ImmutableVector3 v) {
        final ImmutableVector3 rv = Rotation3Test.apply(r, v);// inherited

        assertInvariants(r);// check for side effects

        return rv;
    }

    private static void apply_0(ImmutableVector3 v) {
        final double magnitude0 = v.magnitude();

        final ImmutableVector3 rv = apply(Rotation3Quaternion.ZERO, v);

        assertThat("Rotation by the zero rotation produces a rotated vector equal to the given vector.", rv,
                ImmutableVector3Test.closeTo(v, TOLERANCE * (magnitude0 + 1.0)));
    }

    private static void apply_axis(ImmutableVector3 axis, double angle) {
        final double magnitude0 = axis.magnitude();
        final Rotation3Quaternion r = Rotation3Quaternion.valueOfAxisAngle(axis, angle);

        final ImmutableVector3 rv = apply(r, axis);

        assertTrue(
                "Rotation of a vector that lies along the rotation axis produces a rotated vector equal to the given vector.",
                axis.minus(rv).magnitude() < TOLERANCE * (magnitude0 + 1.0));
        assertThat(
                "Rotation of a vector that lies along the rotation axis produces a rotated vector equal to the given vector.",
                rv, ImmutableVector3Test.closeTo(axis, TOLERANCE * (magnitude0 + 1.0)));
    }

    private static void apply_basisHalfPi(ImmutableVector3 e, ImmutableVector3 eAxis, ImmutableVector3 expected) {
        final Rotation3Quaternion r = Rotation3Quaternion.valueOfAxisAngle(eAxis, Math.PI * 0.5);

        final ImmutableVector3 actual = apply(r, e);

        assertThat(actual, ImmutableVector3Test.closeTo(expected, TOLERANCE));
    }

    public static void assertInvariants(Rotation3Quaternion rotation) {
        ObjectTest.assertInvariants(rotation);// inherited
        Rotation3Test.assertInvariants(rotation);// inherited

        final double angle = rotation.getAngle();
        assertThat("The angle is in the range -2pi to 2pi", angle,
                allOf(greaterThanOrEqualTo(-2.0 * Math.PI), lessThanOrEqualTo(2.0 * Math.PI)));
    }

    public static void assertInvariants(Rotation3Quaternion r1, Rotation3Quaternion r2) {
        ObjectTest.assertInvariants(r1, r2);// inherited
        Rotation3Test.assertInvariants(r1, r2);// inherited
    }

    public static Rotation3Quaternion minus(Rotation3Quaternion r) {
        final Rotation3Quaternion m = (Rotation3Quaternion) Rotation3Test.minus(r);// inherited

        assertInvariants(r);// check for side effects
        assertInvariants(m);
        assertInvariants(m, r);

        return m;
    }

    public static Rotation3 minus(Rotation3Quaternion r, Rotation3 that) {
        final Rotation3 diff = Rotation3Test.minus(r, that);// inherited

        assertInvariants(r);// check for side effects

        return diff;
    }

    private static void minus_0(Rotation3Quaternion r) {
        final Rotation3 diff = minus(r, Rotation3Quaternion.ZERO);

        assertThat("The difference between a rotation and the zero rotation is itself", diff, closeToRotation3(r));
    }

    private static void minus_axisAngle(double angle, ImmutableVector3 axis) {
        final Rotation3Quaternion r = Rotation3Quaternion.valueOfAxisAngle(axis, angle);

        final Rotation3Quaternion m = minus(r);

        assertThat(
                "The opposite rotation either has the same axis but the negative of the angle of this rotation, "
                        + "or the same angle but an axis that points in the opposite direction.",
                m, anyOf(closeToRotation3(Rotation3Quaternion.valueOfAxisAngle(axis, -angle)),
                        closeToRotation3(Rotation3Quaternion.valueOfAxisAngle(axis.minus(), angle))));
    }

    private static void minus_self(Rotation3Quaternion r) {
        final Rotation3 diff = minus(r, r);

        assertThat("The difference between a rotation and itself is the zero rotation.", diff,
                closeToRotation3(Rotation3Quaternion.ZERO));
    }

    public static Rotation3Quaternion plus(Rotation3Quaternion r, Rotation3 that) {
        final Rotation3Quaternion sum = (Rotation3Quaternion) Rotation3Test.plus(r, that);

        assertInvariants(r);// check for side effects
        assertInvariants(sum);
        assertInvariants(sum, r);

        return sum;
    }

    private static void plus_0r(Rotation3Quaternion that) {
        final Rotation3 sum = plus(Rotation3Quaternion.ZERO, that);
        assertThat("sum", sum, closeToRotation3(that));
    }

    private static void plus_r0(Rotation3Quaternion r) {
        final Rotation3 sum = plus(r, Rotation3Quaternion.ZERO);
        assertThat("sum", sum, closeToRotation3(r));
    }

    private static void plus_sameAxis(ImmutableVector3 axis, double angle1, double angle2) {
        final Rotation3Quaternion r1 = Rotation3Quaternion.valueOfAxisAngle(axis, angle1);
        final Rotation3Quaternion r2 = Rotation3Quaternion.valueOfAxisAngle(axis, angle2);

        final Rotation3 sum = plus(r1, r2);

        assertThat("axis", sum.getAxis(), closeToImmutableVector3(axis, TOLERANCE));
        assertThat("normalized angle", Rotation3Test.normalizedAngle(sum.getAngle()),
                closeTo(Rotation3Test.normalizedAngle(angle1 + angle2), TOLERANCE));
    }

    public static Rotation3Quaternion scale(Rotation3Quaternion r, double f) {
        final Rotation3Quaternion fr = (Rotation3Quaternion) Rotation3Test.scale(r, f);

        assertInvariants(r);// check for side effects
        assertInvariants(fr);
        assertInvariants(fr, r);

        return fr;
    }

    private static Rotation3Quaternion valueOf(Quaternion quaternion) {
        final Rotation3Quaternion rotation = Rotation3Quaternion.valueOf(quaternion);

        assertNotNull("Always creates a rotation", rotation);// guard
        assertInvariants(rotation);

        return rotation;
    }

    private static void valueOf_quaternionForAxisAngle(ImmutableVector3 axis, double angle, double magnitude) {
        final Rotation3Quaternion rotation0 = Rotation3Quaternion.valueOfAxisAngle(axis, angle);
        final Quaternion quaternion = rotation0.getVersor().scale(magnitude);

        final Rotation3Quaternion rotation = valueOf(quaternion);

        assertThat("rotation", rotation, closeToRotation3(rotation0, TOLERANCE * 2));
    }

    private static Rotation3Quaternion valueOfAxisAngle(ImmutableVector3 axis, double angle) {
        final double sinAngle = Math.sin(angle);
        final double axisMagnitude = Math.abs(sinAngle) < Double.MIN_NORMAL ? 0.0 : axis.magnitude();

        final Rotation3Quaternion rotation = Rotation3Quaternion.valueOfAxisAngle(axis, angle);

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
    public void minus_00() {
        minus_0(Rotation3Quaternion.ZERO);
    }

    @Test
    public void minus_i() {
        minus_axisAngle(HALF_PI, ImmutableVector3.I);
    }

    @Test
    public void minus_i0() {
        minus_0(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void minus_iJ() {
        minus(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, HALF_PI),
                Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.J, HALF_PI));
    }

    @Test
    public void minus_ik() {
        minus(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, HALF_PI),
                Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.K, HALF_PI));
    }

    @Test
    public void minus_iSmall() {
        minus_axisAngle(SMALL_ANGLE, ImmutableVector3.I);
    }

    @Test
    public void minus_j() {
        minus_axisAngle(HALF_PI, ImmutableVector3.J);
    }

    @Test
    public void minus_j0() {
        minus_0(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE));
    }

    @Test
    public void minus_jI() {
        minus(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.J, HALF_PI),
                Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, HALF_PI));
    }

    @Test
    public void minus_jK() {
        minus(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.J, HALF_PI),
                Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.K, HALF_PI));
    }

    @Test
    public void minus_k() {
        minus_axisAngle(HALF_PI, ImmutableVector3.K);
    }

    @Test
    public void minus_k0() {
        minus_0(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE));
    }

    @Test
    public void minus_kI() {
        minus(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.K, HALF_PI),
                Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, HALF_PI));
    }

    @Test
    public void minus_kJ() {
        minus(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.K, HALF_PI),
                Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, HALF_PI));
    }

    @Test
    public void minus_selfI() {
        minus_self(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void minus_selfJ() {
        minus_self(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE));
    }

    @Test
    public void minus_selfK() {
        minus_self(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE));
    }

    @Test
    public void plus_00() {
        plus_0r(Rotation3Quaternion.ZERO);
    }

    @Test
    public void plus_0ISmall() {
        plus_0r(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void plus_0JSmall() {
        plus_0r(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE));
    }

    @Test
    public void plus_0KSmall() {
        plus_0r(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE));
    }

    @Test
    public void plus_ISmall0() {
        plus_r0(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void plus_JSmall0() {
        plus_r0(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE));
    }

    @Test
    public void plus_KSmall0() {
        plus_r0(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE));
    }

    @Test
    public void plus_sameAxisI() {
        plus_sameAxis(ImmutableVector3.I, HALF_PI, HALF_PI);
    }

    @Test
    public void plus_sameAxisISmallSmall() {
        plus_sameAxis(ImmutableVector3.I, SMALL_ANGLE, SMALL_ANGLE);
    }

    @Test
    public void plus_sameAxisJ() {
        plus_sameAxis(ImmutableVector3.J, HALF_PI, HALF_PI);
    }

    @Test
    public void plus_sameAxisJSmallSmall() {
        plus_sameAxis(ImmutableVector3.J, SMALL_ANGLE, SMALL_ANGLE);
    }

    @Test
    public void plus_sameAxisK() {
        plus_sameAxis(ImmutableVector3.K, HALF_PI, HALF_PI);
    }

    @Test
    public void plus_sameAxisKSmallSmall() {
        plus_sameAxis(ImmutableVector3.K, SMALL_ANGLE, SMALL_ANGLE);
    }

    @Test
    public void scale_00() {
        scale(Rotation3Quaternion.ZERO, 0);
    }

    @Test
    public void scale_01() {
        scale(Rotation3Quaternion.ZERO, 1);
    }

    @Test
    public void scale_ISmall1() {
        scale(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE), 1);
    }

    @Test
    public void scale_ISmall2() {
        scale(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE), 2);
    }

    @Test
    public void scale_JSmall1() {
        scale(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE), 1);
    }

    @Test
    public void scale_JSmall2() {
        scale(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE), 2);
    }

    @Test
    public void scale_KSmall1() {
        scale(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE), 1);
    }

    @Test
    public void scale_KSmall2() {
        scale(Rotation3Quaternion.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE), 2);
    }

    @Test
    public void statics() {
        assertNotNull("Has a zero rotation", Rotation3Quaternion.ZERO);
        assertInvariants(Rotation3Quaternion.ZERO);
        assertEquals("rotation angle of the zero rotation", 0.0, Rotation3Quaternion.ZERO.getAngle(),
                Double.MIN_NORMAL);
    }

    @Test
    public void valueOf_quaternion_0() {
        final Rotation3Quaternion rotation = valueOf(Quaternion.ZERO);

        assertEquals("rotation", rotation, Rotation3Quaternion.ZERO);
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
