package uk.badamson.mc.physics.kinematics;

import java.util.Objects;

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
 * of inconsistency of the position and velocity of a body.
 * </p>
 */
public final class PositionError<VECTOR extends Vector> extends AbstractTimeStepEnergyErrorFunctionTerm {

    private final double mass;
    private final VectorStateSpaceMapper<VECTOR> positionVectorMapper;
    private final VectorStateSpaceMapper<VECTOR> velocityVectorMapper;

    /**
     * <p>
     * Construct a position error term.
     * </p>
     * <ul>
     * <li>The constructed object has attribute values equal to the given
     * values.</li>
     * </ul>
     * 
     * @param mass
     *            A reference mass scale.
     * @param positionVectorMapper
     *            The Strategy for mapping from an object representation of the
     *            position {@linkplain Vector vector} to (part of) a state-space
     *            representation, and vice versa.
     * @param velocityVectorMapper
     *            The Strategy for mapping from an object representation of the
     *            velocity {@linkplain Vector vector} to (part of) a state-space
     *            representation, and vice versa.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code positionVectorMapper} is null.</li>
     *             <li>If {@code velocityVectorMapper} is null.</li>
     * @throws IllegalArgumentException
     *             If {@code mass} is not a positive and
     *             {@linkplain Double#isFinite(double) finite}.
     */
    public PositionError(double mass, VectorStateSpaceMapper<VECTOR> positionVectorMapper,
            VectorStateSpaceMapper<VECTOR> velocityVectorMapper) {
        this.mass = requireReferenceScale(mass, "mass");
        this.positionVectorMapper = Objects.requireNonNull(positionVectorMapper, "positionVectorMapper");
        this.velocityVectorMapper = Objects.requireNonNull(velocityVectorMapper, "velocityVectorMapper");
    }

    /**
     * {@inheritDoc}
     * 
     * <ol>
     * <li>The method uses the {@linkplain #getPositionTerm(int) position term
     * index} information and {@linkplain #getVelocityTerm(int) velocity term index}
     * information to extract position and velocity vectors from the given state
     * vectors.</li>
     * <li>It calculates a mean acceleration from the old and new velocity values,
     * and the time-step size.</li>
     * <li>It calculates the extrapolated position from the old position, the old
     * velocity, and the mean acceleration.</li>
     * <li>It calculates a position error by comparing the new position with the
     * extrapolated position.</li>
     * <li>From that it calculates an equivalent velocity error, by dividing the
     * position error by the time-step size.</li>
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
     *             {@code state0}.
     */
    @Override
    public final double evaluate(double[] dedState, ImmutableVectorN state0, ImmutableVectorN state, double dt) {
        super.evaluate(dedState, state0, state, dt);

        final double rate = 1.0 / dt;

        final Vector x0 = positionVectorMapper.toObject(state0);
        final Vector v0 = velocityVectorMapper.toObject(state0);
        final Vector x = positionVectorMapper.toObject(state);
        final Vector v = velocityVectorMapper.toObject(state);

        final Vector dx = x.minus(x0);
        final Vector vMean = v.mean(v0);
        final Vector ve = dx.scale(rate).minus(vMean);

        final double e = 0.5 * mass * ve.magnitude2();
        final Vector dedx = ve.scale(mass * rate);
        final Vector dedv = ve.scale(-0.5 * mass);

        positionVectorMapper.fromVector(dedState, dedx);
        velocityVectorMapper.fromVector(dedState, dedv);

        return e;
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
     * The Strategy for mapping from an object representation of the position
     * {@linkplain Vector vector} to (part of) a state-space representation, and
     * vice versa.
     * </p>
     * 
     * @return the strategy; not null
     */
    public final VectorStateSpaceMapper<VECTOR> getPositionVectorMapper() {
        return positionVectorMapper;
    }

    /**
     * <p>
     * The number of space dimensions for which this calculates a position error.
     * </p>
     * 
     * @return the number of dimensions; equal to the
     *         {@linkplain VectorStateSpaceMapper#getDimension() number of
     *         dimensions} of the {@linkplain #getPositionVectorMapper() position
     *         vector mapper}.
     */
    public final int getSpaceDimension() {
        return positionVectorMapper.getDimension();
    }

    /**
     * <p>
     * The Strategy for mapping from an object representation of the velocity
     * {@linkplain Vector vector} to (part of) a state-space representation, and
     * vice versa.
     * </p>
     * <ul>
     * <li>Always have a (non null) velocity vector mapper.</li>
     * <li>The {@linkplain VectorStateSpaceMapper#getDimension() number of
     * dimensions} of the velocity vector mapper is equal to the dimension of the
     * {@linkplain #getPositionVectorMapper() position vector mapper}.</li>
     * </ul>
     * 
     * @return the strategy; not null
     */
    public final VectorStateSpaceMapper<VECTOR> getVelocityVectorMapper() {
        return velocityVectorMapper;
    }

    /**
     * {@inheritDoc}
     * <ul>
     * <li>This is valid if, and only if, the {@linkplain #getPositionVectorMapper()
     * position vector mapper} is valid for the given number of variables and the
     * {@linkplain #getVelocityVectorMapper() velocity vector mapper} is valid for
     * the given number of variables.</li>
     * </ul>
     * 
     * @return {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     */
    @Override
    public boolean isValidForDimension(int n) {
        return positionVectorMapper.isValidForDimension(n) && velocityVectorMapper.isValidForDimension(n);
    }

}
