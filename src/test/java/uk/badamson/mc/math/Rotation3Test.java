package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the class {@link Rotation3}
 * </p>
 */
public class Rotation3Test {

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
        assertEquals("The rotated vector has the same magnitude as the given vector.", magnitude0, rv.magnitude(),
                TOLERANCE * (magnitude0 + 1.0));

        return rv;
    }

    private static void apply_0(ImmutableVector3 v) {
        final double magnitude0 = v.magnitude();

        final ImmutableVector3 rv = apply(Rotation3.ZERO, v);

        assertTrue("Rotation by the zero rotation produces a rotated vector equal to the given vector.",
                v.minus(rv).magnitude() < TOLERANCE * (magnitude0 + 1.0));
    }

    private static void apply_axis(ImmutableVector3 axis, double angle) {
        final double magnitude0 = axis.magnitude();
        final Rotation3 r = Rotation3.createAxisAngle(axis, angle);

        final ImmutableVector3 rv = apply(r, axis);

        assertTrue(
                "Rotation of a vector that lies along the rotation axis produces a rotated vector equal to the given vector.",
                axis.minus(rv).magnitude() < TOLERANCE * (magnitude0 + 1.0));
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

        assertEquals("The versor has unit norm.", 1.0, versor.norm(), TOLERANCE);
        assertTrue("The angle is in the range -2pi to 2pi", -2.0 * Math.PI <= angle && angle <= 2.0 * Math.PI);
        assertTrue("The axis has a magnitude of 1 or 0.",
                axisMagnitude < TOLERANCE || Math.abs(axisMagnitude - 1.0) < TOLERANCE);
    }

    public static void assertInvariants(Rotation3 r1, Rotation3 r2) {
        ObjectTest.assertInvariants(r1, r2);// inherited
    }

    private static Rotation3 createAxisAngle(ImmutableVector3 axis, double angle) {
        final double sinAngle = Math.sin(angle);
        final double axisMagnitude = Math.abs(sinAngle) < Double.MIN_NORMAL ? 0.0 : axis.magnitude();

        final Rotation3 rotation = Rotation3.createAxisAngle(axis, angle);

        assertNotNull("Always creates a rotation", rotation);// guard
        assertEquals("rotation cosine", Math.cos(angle), Math.cos(rotation.getAngle()), TOLERANCE);
        assertEquals("rotation sine", sinAngle, Math.sin(rotation.getAngle()), TOLERANCE);
        assertEquals("The rotation axis of the created rotation points in the same direction as the given axis",
                axisMagnitude, axis.dot(rotation.getAxis()), axisMagnitude * TOLERANCE);

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
    public void createAxisAngle_0I() {
        createAxisAngle(ImmutableVector3.create(1, 0, 0), 0);
    }

    @Test
    public void createAxisAngle_0J() {
        createAxisAngle(ImmutableVector3.create(0, 1, 0), 0);
    }

    @Test
    public void createAxisAngle_0K() {
        createAxisAngle(ImmutableVector3.create(0, 0, 1), 0);
    }

    @Test
    public void createAxisAngle_2HalfPiI() {
        createAxisAngle(ImmutableVector3.create(2, 0, 0), Math.PI * 0.5);
    }

    @Test
    public void createAxisAngle_2HalfPiJ() {
        createAxisAngle(ImmutableVector3.create(0, 2, 0), Math.PI * 0.5);
    }

    @Test
    public void createAxisAngle_2HalfPiK() {
        createAxisAngle(ImmutableVector3.create(0, 0, 2), Math.PI * 0.5);
    }

    @Test
    public void createAxisAngle_2PiI() {
        createAxisAngle(ImmutableVector3.create(1, 0, 0), Math.PI * 2.0);
    }

    @Test
    public void createAxisAngle_halfPiI() {
        createAxisAngle(ImmutableVector3.create(1, 0, 0), Math.PI * 0.5);
    }

    @Test
    public void createAxisAngle_halfPiJ() {
        createAxisAngle(ImmutableVector3.create(0, 1, 0), Math.PI * 0.5);
    }

    @Test
    public void createAxisAngle_halfPiK() {
        createAxisAngle(ImmutableVector3.create(0, 0, 1), Math.PI * 0.5);
    }

    @Test
    public void createAxisAngle_piI() {
        createAxisAngle(ImmutableVector3.create(1, 0, 0), Math.PI);
    }

    @Test
    public void createAxisAngle_smallI() {
        createAxisAngle(ImmutableVector3.create(1, 0, 0), SMALL_ANGLE);
    }

    @Test
    public void createAxisAngle_smallJ() {
        createAxisAngle(ImmutableVector3.create(0, 1, 0), SMALL_ANGLE);
    }

    @Test
    public void createAxisAngle_smallK() {
        createAxisAngle(ImmutableVector3.create(0, 0, 1), SMALL_ANGLE);
    }

    @Test
    public void statics() {
        assertNotNull("Has a zero rotation", Rotation3.ZERO);
        assertInvariants(Rotation3.ZERO);
        assertEquals("rotation angle of the zero rotation", 0.0, Rotation3.ZERO.getAngle(), Double.MIN_NORMAL);
    }
}
