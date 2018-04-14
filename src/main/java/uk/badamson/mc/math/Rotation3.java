package uk.badamson.mc.math;

/**
 * <p>
 * A rotation in 3D space.
 * </p>
 */
public interface Rotation3 {

    /**
     * <p>
     * Produce the direction vector that results from applying this rotation to a
     * given direction vector.
     * </p>
     * <ul>
     * <li>Always produces a (non null) rotated vector.</li>
     * <li>The rotated vector has the same {@linkplain ImmutableVector3#magnitude()
     * magnitude} as the given vector.</li>
     * <li>Rotation by the zero rotation produces a rotated vector
     * {@linkplain ImmutableVector3#equals(Object) equal} to the given vector.</li>
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
    public ImmutableVector3 apply(ImmutableVector3 v);

    /**
     * <p>
     * The angle of rotation of this rotation, in radians.
     * </p>
     * <ul>
     * <li>Rotation by a complete circle has no effect. The angle might therefore be
     * forced to be in the range -2&pi; to 2&pi;</li>
     * </ul>
     * 
     * @return the angle
     */
    public double getAngle();

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
     * @return the axis
     */
    public ImmutableVector3 getAxis();

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
    public Quaternion getVersor();

}
