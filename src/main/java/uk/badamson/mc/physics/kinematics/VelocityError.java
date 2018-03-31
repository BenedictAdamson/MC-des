package uk.badamson.mc.physics.kinematics;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Vector;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTerm;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunction;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunctionTerm;
import uk.badamson.mc.physics.VectorStateSpaceMapper;;

/**
 * <p>
 * A {@linkplain TimeStepEnergyErrorFunctionTerm term} for a
 * {@linkplain TimeStepEnergyErrorFunction functor that calculates the physical
 * modelling error of a system at a future point in time} that gives the degree
 * of inconsistency of the velocity and acceleration of a body.
 * </p>
 */
@Immutable
public final class VelocityError<VECTOR extends Vector> extends AbstractTimeStepEnergyErrorFunctionTerm {

    private final double mass;
    private final VectorStateSpaceMapper<VECTOR> velocityVectorMapper;
    private final VectorStateSpaceMapper<VECTOR> accelerationVectorMapper;

    /**
     * <p>
     * Construct a velocity error term.
     * </p>
     *
     * <section>
     * <h1>Post Conditions</h1>
     * <ul>
     * <li>The constructed object has attribute values equal to the given
     * values.</li>
     * <li>The {@linkplain #getSpaceDimension() space dimension} of the constructed
     * object is equal to the length of the arrays of position terms.</li>
     * </ul>
     * </section>
     * 
     * @param mass
     *            A reference mass scale.
     * @param velocityVectorMapper
     *            The Strategy for mapping from an object representation of the
     *            velocity {@linkplain Vector vector} to (part of) a state-space
     *            representation, and vice versa.
     * @param accelerationVectorMapper
     *            The Strategy for mapping from an object representation of the
     *            acceleration {@linkplain Vector vector} to (part of) a state-space
     *            representation, and vice versa.
     *
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code velocityVectorMapper} is null.</li>
     *             <li>If {@code accelerationVectorMapper} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If {@code mass} is not a positive and
     *             {@linkplain Double#isFinite(double) finite}.
     */
    public VelocityError(double mass, VectorStateSpaceMapper<VECTOR> velocityVectorMapper,
            VectorStateSpaceMapper<VECTOR> accelerationVectorMapper) {
        this.mass = requireReferenceScale(mass, "mass");
        this.velocityVectorMapper = Objects.requireNonNull(velocityVectorMapper, "velocityVectorMapper");
        this.accelerationVectorMapper = Objects.requireNonNull(accelerationVectorMapper, "accelerationVectorMapper");
    }

    /**
     * {@inheritDoc}
     * 
     * <ol>
     * <li>The method uses the {@linkplain #getVelocityTerm(int) velocity term
     * index} information and {@linkplain #getAccelerationTerm(int) acceleration
     * term index} information to extract velocity and acceleration vectors from the
     * given state vectors.</li>
     * <li>It calculates a mean acceleration from the old and new acceleration
     * values.</li>
     * <li>It calculates the extrapolated velocity from the old velocity and the
     * mean acceleration.</li>
     * <li>It calculates a velocity error by comparing the new velocity with the
     * extrapolated velocity.</li>
     * <li>From that it calculates an equivalent kinetic energy error, using the
     * {@linkplain #getMass() characteristic mass value}. That is the error term it
     * returns.</li>
     * </ol>
     * 
     * @param dedx
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
     *             {@code state0}.
     */
    @Override
    public final double evaluate(double[] dedx, ImmutableVectorN state0, ImmutableVectorN state, double dt) {
        super.evaluate(dedx, state0, state, dt);

        final Vector v0 = velocityVectorMapper.toObject(state0);
        final Vector a0 = accelerationVectorMapper.toObject(state0);
        final Vector v = velocityVectorMapper.toObject(state);
        final Vector a = accelerationVectorMapper.toObject(state);

        final Vector dv = v.minus(v0);
        final Vector aMean = a.mean(a0);
        final Vector ve = dv.minus(aMean.scale(dt));

        final double e = 0.5 * mass * ve.magnitude2();
        final Vector dedv = ve.scale(mass);
        final Vector deda = ve.scale(-0.5 * mass * dt);

        velocityVectorMapper.fromVector(dedx, dedv);
        accelerationVectorMapper.fromVector(dedx, deda);

        return e;
    }

    /**
     * <p>
     * The Strategy for mapping from an object representation of the acceleration
     * {@linkplain Vector vector} to (part of) a state-space representation, and
     * vice versa.
     * </p>
     * <ul>
     * <li>Always have a (non null) acceleration vector mapper.</li>
     * <li>The {@linkplain VectorStateSpaceMapper#getDimension() number of
     * dimensions} of the acceleration vector mapper is equal to the dimension of
     * the {@linkplain #getVelocityVectorMapper() velocity vector mapper}.</li>
     * </ul>
     * 
     * @return the strategy
     */
    public final VectorStateSpaceMapper<VECTOR> getAccelerationVectorMapper() {
        return accelerationVectorMapper;
    }

    /**
     * <p>
     * A reference mass scale.
     * </p>
     * <p>
     * The functor uses this value to convert a position error into an energy error.
     * It is tempting to use the mass of the solid body for which this functor
     * calculates the position error, but that will produce bad results if there are
     * multiple bodies and they have very different masses; it is better to use the
     * same value for all bodies, with that value equal to the mass of a typical
     * body.
     * </p>
     * 
     * @return the mass; positive and {@linkplain Double#isFinite(double) finite}
     */
    public final double getMass() {
        return mass;
    }

    /**
     * <p>
     * The number of space dimensions for which this calculates a position error.
     * </p>
     * 
     * @return the number of dimensions; equal to the
     *         {@linkplain VectorStateSpaceMapper#getDimension() number of
     *         dimensions} of the {@linkplain #getVelocityVectorMapper() velocity
     *         vector mapper}.
     */
    public final int getSpaceDimension() {
        return velocityVectorMapper.getDimension();
    }

    /**
     * <p>
     * The Strategy for mapping from an object representation of the velocity
     * {@linkplain Vector vector} to (part of) a state-space representation, and
     * vice versa.
     * </p>
     * 
     * @return the strategy; not null
     */
    public final VectorStateSpaceMapper<VECTOR> getVelocityVectorMapper() {
        return velocityVectorMapper;
    }

    /**
     * {@inheritDoc}
     * <ul>
     * <li>This is valid if, and only if, the {@linkplain #getVelocityVectorMapper()
     * velocity vector mapper} is valid for the given number of variables and the
     * {@linkplain #getAccelerationVectorMapper() acceleration vector mapper} is
     * valid for the given number of variables.</li>
     * </ul>
     * 
     * @return {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     */
    @Override
    public boolean isValidForDimension(int n) {
        return velocityVectorMapper.isValidForDimension(n) && accelerationVectorMapper.isValidForDimension(n);
    }

}
