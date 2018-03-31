package uk.badamson.mc.physics;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVector3;
import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * A Strategy for mapping from an object representation of a
 * {@linkplain ImmutableVector3 3D vector} to (part of) a state-space
 * representation, and vice versa.
 * </p>
 */
@Immutable
public final class ImmutableVector3StateSpaceMapper implements VectorStateSpaceMapper<ImmutableVector3> {

    /**
     * {@inheritDoc}
     * 
     * @param state
     *            {@inheritDoc}
     * @param object
     *            {@inheritDoc}
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IndexOutOfBoundsException
     *             {@inheritDoc}
     */
    @Override
    public final void fromObject(double[] state, ImmutableVector3 object) {
        // TODO Auto-generated method stub

    }

    /**
     * @param state
     *            {@inheritDoc}
     * @return {@inheritDoc}
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IndexOutOfBoundsException
     *             {@inheritDoc}
     */
    @Override
    public final ImmutableVector3 toObject(ImmutableVectorN state) {
        // TODO Auto-generated method stub
        return null;
    }

}
