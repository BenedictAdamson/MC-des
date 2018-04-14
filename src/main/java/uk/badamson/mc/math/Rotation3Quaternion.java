package uk.badamson.mc.math;

import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A rotation in 3D space.
 * </p>
 */
@Immutable
public final class Rotation3Quaternion {

    /**
     * <p>
     * A zero rotation.
     * </p>
     * <ul>
     * <li>Has a (non null) zero rotation.</li>
     * <li>The {@linkplain #getAngle() rotation angle} of the zero rotation is
     * 0.</li>
     * </ul>
     */
    public static final Rotation3Quaternion ZERO = new Rotation3Quaternion(Quaternion.create(1, 0, 0, 0));

    /**
     * <p>
     * Create a rotation that has a given quaternion representation.
     * </p>
     * <ul>
     * <li>Always creates a (non null) rotation.</li>
     * <li>The {@linkplain #getVersor() versor} of the created rotation is derived
     * from the given quaternion, {@linkplain Quaternion#scale(double) scaled} to
     * have unit {@linkplain Quaternion#norm() norm} (magnitude).</li>
     * <li>For the special case of a {@linkplain Quaternion#ZERO zero} (or near
     * zero) quaternion, the method gives the {@linkplain #ZERO zero} rotation.</li>
     * </ul>
     * 
     * @param quaternion
     *            The quaternion of the rotation.
     * @return the rotation
     * @throws NullPointerException
     *             If {@code quaternion} is null.
     */
    public static Rotation3Quaternion valueOf(Quaternion quaternion) {
        Objects.requireNonNull(quaternion, "quaternion");
        final double norm = quaternion.norm();
        if (norm == 1.0) {
            return new Rotation3Quaternion(quaternion);
        } else if (Double.MIN_NORMAL <= norm) {
            return new Rotation3Quaternion(quaternion.scale(1.0 / norm));
        } else {
            return ZERO;
        }
    }

    /**
     * <p>
     * Create a rotation that has a given axis-angle representation.
     * </p>
     * <ul>
     * <li>Always creates a (non null) rotation.</li>
     * <li>The {@linkplain #getAngle() rotation angle} of the created rotation is
     * equal to the given angle (converted to the range -2&pi; to 2&pi;).</li>
     * <li>The {@linkplain #getAxis()) rotation axis} of the created rotation points
     * in the same direction as the given axis.</li>
     * </ul>
     * 
     * @param axis
     *            The angle of rotation of the rotation, in radians.
     * @param angle
     *            The direction vector about which this rotation takes place. This
     *            direction need not have {@linkplain ImmutableVector3#magnitude()
     *            magnitude} of 1.
     * @return the rotation
     * @throws NullPointerException
     *             If {@code axis} is null.
     * @throws IllegalArgumentException
     *             If {@code axis} as zero {@linkplain ImmutableVector3#magnitude()
     *             magnitude} but the rotation amount is not zero.
     */
    public static Rotation3Quaternion valueOfAxisAngle(ImmutableVector3 axis, double angle) {
        Objects.requireNonNull(axis, "axis");
        final double halfAngle = angle * 0.5;
        final double c = Math.cos(halfAngle);
        final double s = Math.sin(halfAngle);
        final double magnitude = axis.magnitude();
        final boolean smallAngle = Double.MIN_NORMAL < Math.abs(s) && (1.0 - c) < Double.MIN_NORMAL;
        if (!smallAngle) {
            if (magnitude < Double.MIN_NORMAL) {
                throw new IllegalArgumentException("Zero axis " + axis);
            }
            final double f = s / magnitude;
            return new Rotation3Quaternion(Quaternion.create(c, f * axis.get(0), f * axis.get(1), f * axis.get(2)));
        } else {
            // Avoid division by zero.
            return ZERO;
        }
    }

    private final Quaternion versor;

    private Rotation3Quaternion(Quaternion versor) {
        this.versor = versor;
    }

    /**
     * <p>
     * Produce the direction vector that results from applying this rotation to a
     * given direction vector.
     * </p>
     * <ul>
     * <li>Always produces a (non null) rotated vector.</li>
     * <li>The rotated vector has the same {@linkplain ImmutableVector3#magnitude()
     * magnitude} as the given vector.</li>
     * <li>Rotation by the {@linkplain #ZERO zero rotation} produces a rotated
     * vector {@linkplain ImmutableVector3#equals(Object) equal} to the given
     * vector.</li>
     * <li>Rotation of a vector that lies along the {@linkplain #getAxis() rotation
     * axis} produces a rotated vector equal to the given vector.</li>
     * </ul>
     * 
     * @param v
     *            The direction vector to be rotated.
     * @return The rotated vector.
     * @throws NullPointerException
     *             If {@code v} is null
     */
    public final ImmutableVector3 apply(ImmutableVector3 v) {
        Objects.requireNonNull(v, "v");
        final Quaternion conj = versor.conjugation(Quaternion.create(0, v.get(0), v.get(1), v.get(2)));
        return ImmutableVector3.create(conj.getB(), conj.getC(), conj.getD());
    }

    /**
     * <p>
     * The angle of rotation of this rotation, in radians.
     * </p>
     * <ul>
     * <li>The angle is in the range -2&pi; to 2&pi;</li>
     * </ul>
     * 
     * @return the angle
     */
    public final double getAngle() {
        final double c = versor.getA();
        final double s = versor.vector().norm();
        return Math.atan2(s, c) * 2.0;
    }

    /**
     * <p>
     * The direction vector about which this rotation takes place.
     * </p>
     * <ul>
     * <li>Always have a (non-null) axis.</li>
     * <li>The axis has a {@linkplain ImmutableVector3#magnitude() magnitude} of 1
     * or 0.</li>
     * <li>The axis has a 0 magnitude for a zero rotation.</li>
     * </ul>
     * 
     * @return
     */
    public final ImmutableVector3 getAxis() {
        final ImmutableVector3 su = ImmutableVector3.create(versor.getB(), versor.getC(), versor.getD());
        final double magnitude = su.magnitude();
        if (Double.MIN_NORMAL < magnitude) {
            return su.scale(1.0 / magnitude);
        } else {
            return ImmutableVector3.ZERO;
        }
    }

    /**
     * <p>
     * The quaternion that represents this rotation.
     * </p>
     * <ul>
     * <li>Always have a (non null) versor.</li>
     * <li>The versor has unit {@linkplain Quaternion#norm() norm} (magnitude).</li>
     * <li>The {@linkplain Quaternion#getA() real component} of the versor is the
     * cosine of half the {@linkplain #getAngle() rotation angle}.</li>
     * <li>The {@linkplain Quaternion#vector() vector part} of the versor is the
     * unit direction vector of the {@linkplain #getAxis() rotation axis} multiplied
     * by the sine of half the rotation angle.</li>
     * </ul>
     * 
     * @return the versor.
     */
    public final Quaternion getVersor() {
        return versor;
    }

    @Override
    public final String toString() {
        return "[" + getAngle() + " radians about " + getAxis() + "]";
    }

}
