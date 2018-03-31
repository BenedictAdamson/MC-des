package uk.badamson.mc.physics;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVector3;
import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * A Strategy for mapping from an object representation of a
 * {@linkplain ImmutableVector3 3D vector} to (part of) a state-space
 * representation, and vice versa.
 * </p>
 * <p>
 * The mapper maps 3 contiguous components of the state-space vector to the
 * components of the 3D vector.
 * </p>
 */
@Immutable
public final class ImmutableVector3StateSpaceMapper implements VectorStateSpaceMapper<ImmutableVector3> {

    private final int index0;

    /**
     * @param index0
     *            The position in the state-space vector of the components that map
     *            to the components of the 3D vector. If the state-space is
     *            {@linkplain ImmutableVectorN vector} <var>v</var>,
     *            {@linkplain ImmutableVectorN#get(int) component}
     *            <var>v</var><sub>index0</sub> is the x component of the3D vector,
     *            <var>v</var><sub>index0+1</sub> is the y component, and
     *            <var>v</var><sub>index0+2</sub> is the z component.
     * @throws IllegalArgumentException
     *             If {@code index0} is negative
     */
    public ImmutableVector3StateSpaceMapper(int index0) {
        if (index0 < 0) {
            throw new IllegalArgumentException("index0 " + index0);
        }
        this.index0 = index0;
    }

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
     *             {@inheritDoc} If the
     */
    @Override
    public final void fromObject(double[] state, ImmutableVector3 object) {
        Objects.requireNonNull(state, "state");
        if (state.length < index0 + 3) {
            throw new IndexOutOfBoundsException("state.length " + state.length + " index0 " + index0);
        }
        state[index0] = object.get(0);
        state[index0 + 1] = object.get(1);
        state[index0 + 2] = object.get(2);
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
        Objects.requireNonNull(state, "state");
        return ImmutableVector3.create(state.get(index0), state.get(index0 + 1), state.get(index0 + 2));
    }

}
