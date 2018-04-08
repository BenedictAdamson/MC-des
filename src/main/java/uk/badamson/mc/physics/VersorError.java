package uk.badamson.mc.physics;

import java.util.Objects;

import net.jcip.annotations.Immutable;
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
