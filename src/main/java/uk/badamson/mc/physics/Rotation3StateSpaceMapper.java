package uk.badamson.mc.physics;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Quaternion;
import uk.badamson.mc.math.Rotation3Quaternion;

/**
 * <p>
 * A Strategy for mapping from an object representation of a
 * {@linkplain Rotation3Quaternion 3D rotation} to (part of) a state-space
 * representation, and vice versa.
 * </p>
 * <p>
 * The mapper maps 4 contiguous components of the state-space vector to the
 * components of the quaternion.
 * </p>
 */
@Immutable
public final class Rotation3StateSpaceMapper implements ObjectStateSpaceMapper<Rotation3Quaternion> {

    private final QuaternionStateSpaceMapper quaternionMapper;

    /**
     * @param quaternionMapper
     *            A Strategy for mapping from an object representation of a
     *            {@linkplain Quaternion quaternion} to (part of) a state-space
     *            representation, and vice versa. Used to map to and from the
     *            representation of teh rotation as a quaternion.
     * @throws NullPointerException
     *             If {@code quaternionMapper} is null
     */
    public Rotation3StateSpaceMapper(QuaternionStateSpaceMapper quaternionMapper) {
        this.quaternionMapper = Objects.requireNonNull(quaternionMapper, "quaternionMapper");
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
    public final void fromObject(double[] state, Rotation3Quaternion object) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(object, "object");
        quaternionMapper.fromObject(state, object.getVersor());
    }

    @Override
    public final boolean isValidForDimension(int n) {
        return quaternionMapper.isValidForDimension(n);
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
    public final Rotation3Quaternion toObject(ImmutableVectorN state) {
        return Rotation3Quaternion.valueOf(quaternionMapper.toObject(state));
    }

}
