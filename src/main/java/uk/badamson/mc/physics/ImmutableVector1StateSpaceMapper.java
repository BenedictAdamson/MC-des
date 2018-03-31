package uk.badamson.mc.physics;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVector1;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Vector;

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

    @Override
    public final void fromObject(double[] state, ImmutableVector1 object) {
        fromVector(state, object);
    }

    @Override
    public final void fromVector(double[] state, Vector vector) {
        Objects.requireNonNull(state, "state");
        if (!isValidForDimension(state.length)) {
            throw new IllegalArgumentException("state.length " + state.length + " index " + index);
        }
        if (vector.getDimension() != 1) {
            throw new IllegalArgumentException("vector dimension " + vector.getDimension());
        }
        state[index] += vector.get(0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The dimension is 1.
     * </p>
     */
    @Override
    public final int getDimension() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     */
    @Override
    public final boolean isValidForDimension(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n " + n);
        }
        return index + 1 <= n;
    }

    @Override
    public final ImmutableVector1 toObject(ImmutableVectorN state) {
        Objects.requireNonNull(state, "state");
        if (!isValidForDimension(state.getDimension())) {
            throw new IllegalArgumentException("state.dimension " + state.getDimension() + " index " + index);
        }
        return ImmutableVector1.create(state.get(index));
    }

}
