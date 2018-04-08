package uk.badamson.mc.physics;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Quaternion;

/**
 * <p>
 * A Strategy for mapping from an object representation of a
 * {@linkplain Quaternion quaternion} to (part of) a state-space representation,
 * and vice versa.
 * </p>
 * <p>
 * The mapper maps 4 contiguous components of the state-space vector to the
 * components of the quaternion.
 * </p>
 */
@Immutable
public final class QuaternionStateSpaceMapper implements ObjectStateSpaceMapper<Quaternion> {

    private final int index0;

    /**
     * @param index0
     *            The position in the state-space vector of the components that map
     *            to the components of the quaternion. If the state-space is
     *            {@linkplain ImmutableVectorN vector} <var>v</var>,
     *            {@linkplain ImmutableVectorN#get(int) component}
     *            <var>v</var><sub>index0</sub> is the real component of the
     *            quaternion, <var>v</var><sub>index0+1</sub> is the <b>i</b>
     *            component, <var>v</var><sub>index0+2</sub> is the <b>j</b>
     *            component, and <var>v</var><sub>index0+3</sub> is the <b>k</b>
     *            component.
     * @throws IllegalArgumentException
     *             If {@code index0} is negative
     */
    public QuaternionStateSpaceMapper(int index0) {
        if (index0 < 0) {
            throw new IllegalArgumentException("index0 " + index0);
        }
        this.index0 = index0;
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
    public final void fromObject(double[] state, Quaternion object) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(object, "object");
        state[index0] += object.getA();
        state[index0 + 1] += object.getB();
        state[index0 + 2] += object.getC();
        state[index0 + 3] += object.getD();
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
        return index0 + 3 <= n;
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
    public final Quaternion toObject(ImmutableVectorN state) {
        Objects.requireNonNull(state, "state");
        return Quaternion.create(state.get(index0), state.get(index0 + 1), state.get(index0 + 2),
                state.get(index0 + 3));
    }

}
