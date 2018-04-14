package uk.badamson.mc.math;

import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A rotation in 3D space represented by a a rotation angle and a rotation axis.
 * </p>
 */
@Immutable
public final class Rotation3AxisAngle implements Rotation3 {

    /**
     * <p>
     * A rotation with zero rotation angle.
     * </p>
     * <ul>
     * <li>Has a (non null) zero rotation.</li>
     * <li>The {@linkplain #getAngle() rotation angle} of the zero rotation is
     * 0.</li>
     * </ul>
     */
    public static final Rotation3AxisAngle ZERO = null;// FIXME

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
    public static Rotation3AxisAngle valueOf(Quaternion quaternion) {
        return null;// FIXME
    }

    /**
     * <p>
     * Create a rotation quaternion that has a given axis-angle representation.
     * </p>
     * <ul>
     * <li>Always creates a (non null) rotation.</li>
     * <li>The {@linkplain #getAngle() rotation angle} of the created rotation is
     * equal to the given angle.</li>
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
    public static Rotation3AxisAngle valueOfAxisAngle(ImmutableVector3 axis, double angle) {
        Objects.requireNonNull(axis, "axis");
        return null;// FIXME
    }

    @Override
    public final ImmutableVector3 apply(ImmutableVector3 v) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final double getAngle() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public final ImmutableVector3 getAxis() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final Quaternion getVersor() {
        // TODO Auto-generated method stub
        return null;
    }

}
