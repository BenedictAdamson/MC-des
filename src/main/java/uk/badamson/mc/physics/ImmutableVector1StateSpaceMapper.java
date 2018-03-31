package uk.badamson.mc.physics;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVector1;
import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * A Strategy for mapping from an object representation of a
 * {@linkplain ImmutableVector1 3D vector} to (part of) a state-space
 * representation, and vice versa.
 * </p>
 * <p>
 * The mapper maps one component of the state-space vector to the component of
 * the 1D vector.
 * </p>
 */
@Immutable
public final class ImmutableVector1StateSpaceMapper implements VectorStateSpaceMapper<ImmutableVector1> {

    private final int index;

    /**
     * @param index
     *            The position in the state-space vector of the component that maps
     *            to the component of the 1D vector. If the state-space is
     *            {@linkplain ImmutableVectorN vector} <var>v</var>,
     *            {@linkplain ImmutableVectorN#get(int) component}
     *            <var>v</var><sub>index</sub> is the component of the 1D vector.
     * @throws IllegalArgumentException
     *             If {@code index} is negative
     */
    public ImmutableVector1StateSpaceMapper(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index " + index);
        }
        this.index = index;
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
    public final void fromObject(double[] state, ImmutableVector1 object) {
        Objects.requireNonNull(state, "state");
        if (state.length < index + 1) {
            throw new IndexOutOfBoundsException("state.length " + state.length + " index " + index);
        }
        state[index] = object.get(0);
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
    public final ImmutableVector1 toObject(ImmutableVectorN state) {
        Objects.requireNonNull(state, "state");
        return ImmutableVector1.create(state.get(index));
    }

}
