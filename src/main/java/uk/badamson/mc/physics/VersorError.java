package uk.badamson.mc.physics;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Quaternion;

/**
 * <p>
 * A {@linkplain TimeStepEnergyErrorFunctionTerm term} for a
 * {@linkplain TimeStepEnergyErrorFunction functor that calculates the physical
 * modelling error of a system at a future point in time} that gives the degree
 * to which a {@linkplain Quaternion quaternion} is not a versor.
 * </p>
 */
@Immutable
public final class VersorError extends AbstractTimeStepEnergyErrorFunctionTerm {

    private final double length;
    private final double mass;
    private final QuaternionStateSpaceMapper quaternionMapper;

    /**
     * @param length
     *            A reference length scale.
     * @param mass
     *            A reference mass scale.
     * @param quaternionMapper
     *            The Strategy for mapping from an object representation of the
     *            {@linkplain Quaternion quaternion} to (part of) a state-space
     *            representation, and vice versa.
     * @throws NullPointerException
     *             If {@code quaternionMapper} is null.
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code length} is not a positive and
     *             {@linkplain Double#isFinite(double) finite}.</li>
     *             <li>If {@code mass} is not a positive and
     *             {@linkplain Double#isFinite(double) finite}.</li>
     *             <li>
     *             </ul>
     */
    public VersorError(double length, double mass, QuaternionStateSpaceMapper quaternionMapper) {
        this.length = requireReferenceScale(length, "length");
        this.mass = requireReferenceScale(mass, "mass");
        this.quaternionMapper = Objects.requireNonNull(quaternionMapper, "quaternionMapper");
    }

    /**
     * {@inheritDoc}
     * 
     * <ol>
     * <li>The method uses the {@linkplain #getQuaternionMapper() quaternion mapper}
     * to extract from the given state vector the quaternion that ought to be a
     * versor. The method uses only the state at teh future point in time; it
     * ignores the state at the current point in time.</li>
     * <li>It computes the differences between the {@linkplain Quaternion#norm()
     * norm} (magnitude) of the quaternion and the unit norm.</li>
     * <li>Assuming that the quaternion is dimensionless, it converts that to a
     * length error by multiplying by the {@linkplain #getLength() length
     * scale}.</li>
     * <li>From that it calculates an equivalent velocity error, by dividing the
     * length error by the time-step size.</li>
     * <li>From that it calculates an equivalent kinetic energy error, using the
     * {@linkplain #getMass() characteristic mass value}. That is the error term it
     * returns.</li>
     * </ol>
     * 
     * @param dedState
     *            {@inheritDoc}
     * @param state0
     *            {@inheritDoc}
     * @param state
     *            {@inheritDoc}
     * @param dt
     *            {@inheritDoc}
     * @return the value
     * 
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     * @throws IllegalArgumentException
     *             If the length of {@code dedx} does not equal the
     *             {@linkplain ImmutableVectorN#getDimension() dimension} of
     *             {@code state}.
     */
    @Override
    public final double evaluate(double[] dedx, ImmutableVectorN state0, ImmutableVectorN state, double dt) {
        super.evaluate(dedx, state0, state, dt);// check preconditions

        final Quaternion q = quaternionMapper.toObject(state);
        final double n = q.norm();
        final double ne = n - 1.0;// TODO smaller
        final double ve = length * ne / dt;
        final double deda2 = -mass * length * ve / (dt * n);// TODO sign

        final double e = 0.5 * mass * ve * ve;
        final Quaternion dedq = q.scale(deda2);
        quaternionMapper.fromObject(dedx, dedq);

        return e;
    }

    /**
     * <p>
     * A reference length scale.
     * </p>
     * <p>
     * The functor uses this value to convert a quaternion
     * {@linkplain Quaternion#norm() norm} error into a length error. It is tempting
     * to use the size of the solid body with which the quaternion is associated,
     * but that will produce bad results if there are multiple bodies and they have
     * very different sizes; it is better to use the same value for all quaternions,
     * with that value equal to the size of a typical body.
     * </p>
     * 
     * @return the length; positive and {@linkplain Double#isFinite(double) finite}
     */
    public final double getLength() {
        return length;
    }

    /**
     * <p>
     * A reference mass scale.
     * </p>
     * <p>
     * The functor uses this value to convert a velocity error into an energy error.
     * It is tempting to use the mass of the solid body with which the quaternion is
     * associated, but that will produce bad results if there are multiple bodies
     * and they have very different masses; it is better to use the same value for
     * all quaternions, with that value equal to the mass of a typical body.
     * </p>
     * 
     * @return the mass; positive and {@linkplain Double#isFinite(double) finite}
     */
    public final double getMass() {
        return mass;
    }

    /**
     * <p>
     * The Strategy for mapping from an object representation of the
     * {@linkplain Quaternion quaternion} to (part of) a state-space representation,
     * and vice versa.
     * </p>
     * 
     * @return the strategy; not null
     */
    public final QuaternionStateSpaceMapper getQuaternionMapper() {
        return quaternionMapper;
    }

    /**
     * @param n
     * @return
     */
    @Override
    public boolean isValidForDimension(int n) {
        // TODO Auto-generated method stub
        return false;
    }

}
