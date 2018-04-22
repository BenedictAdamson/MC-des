package uk.badamson.mc.physics;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Rotation3AxisAngle;

/**
 * <p>
 * A Strategy for mapping from an object representation of a
 * {@linkplain Rotation3AxisAngle 3D rotation} to (part of) a state-space
 * representation, and vice versa.
 * </p>
 * <p>
 * The mapper maps components of the state-space vector to the rotation axis and
 * angle of the rotation
 * </p>
 */
@Immutable
public final class Rotation3AxisAngleStateSpaceMapper implements ObjectStateSpaceMapper<Rotation3AxisAngle> {

    private final int rotationIndex;
    private final ImmutableVector3StateSpaceMapper axisMapper;

    /**
     * @param rotationIndex
     *            The position in the state-space vector that is the rotation angle.
     * @throws NullPointerException
     *             If {@code axisMapper} is null
     * @throws IllegalArgumentException
     *             if {@code rotationIndex} is negative.
     */
    public Rotation3AxisAngleStateSpaceMapper(int rotationIndex, ImmutableVector3StateSpaceMapper axisMapper) {
        if (rotationIndex < 0) {
            throw new IllegalArgumentException("rotationIndex " + rotationIndex);
        }
        this.rotationIndex = rotationIndex;
        this.axisMapper = Objects.requireNonNull(axisMapper, "quaternionMapper");
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws RuntimeException
     *             {@inheritDoc}
     */
    @Override
    public final void fromObject(double[] state, Rotation3AxisAngle object) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(object, "object");
        state[rotationIndex] += object.getAngle();
        axisMapper.fromObject(state, object.getAxis());
    }

    @Override
    public final boolean isValidForDimension(int n) {
        return axisMapper.isValidForDimension(n) && rotationIndex < n;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws RuntimeException
     *             {@inheritDoc}
     */
    @Override
    public final Rotation3AxisAngle toObject(ImmutableVectorN state) {
        return Rotation3AxisAngle.valueOfAxisAngle(axisMapper.toObject(state), state.get(rotationIndex));
    }

}
