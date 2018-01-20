package uk.badamson.mc.physics.kinematics;

import java.util.Objects;

import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTerm;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunction;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunctionTerm;;

/**
 * <p>
 * A {@linkplain TimeStepEnergyErrorFunctionTerm term} for a
 * {@linkplain TimeStepEnergyErrorFunction functor that calculates the physical
 * modelling error of a system at a future point in time} that gives the degree
 * of inconsistency of the position and velocity of a body.
 * </p>
 */
public final class PositionError extends AbstractTimeStepEnergyErrorFunctionTerm {

    private final double mass;
    private final int[] positionTerm;
    private final int[] velocityTerm;

    /**
     * <p>
     * Construct a position error term.
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
     * @param positionTerm
     *            Which terms in the solution space vector correspond to the
     *            components of the position vector of the body.
     *            {@code positionTerm[i]} is index of component <var>i</var>,
     * @param velocityTerm
     *            Which terms in the solution space vector correspond to the
     *            components of the velocity vector of the body.
     *            {@code velocityTerm[i]} is index of component <var>i</var>,
     *
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code positionTerm} is null.</li>
     *             <li>If {@code velocityTerm} is null.</li>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code mass} is not a positive and
     *             {@linkplain Double#isFinite(double) finite}.</li>
     *             <li>If the length of {@code positionTerm} does not equal the
     *             length of {@code velocityTerm}.</li>
     *             <li>If {@code positionTerm} has any negative values.</li>
     *             <li>If {@code velocityTerm} has any negative values.</li>
     *             </ul>
     */
    public PositionError(double mass, int[] positionTerm, int[] velocityTerm) {
	this.mass = requireReferenceScale(mass, "mass");
	this.positionTerm = copyTermIndex(positionTerm, "positionTerm");
	this.velocityTerm = copyTermIndex(velocityTerm, "velocityTerm");
	requireConsistentLengths(positionTerm, "positionTerm", velocityTerm, "velocityTerm");
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
     *             {@linkplain ImmutableVector#getDimension() dimension} of
     *             {@code state0}.
     */
    @Override
    public final double evaluate(double[] dedState, ImmutableVector state0, ImmutableVector state, double dt) {
	Objects.requireNonNull(dedState, "dedState");
	Objects.requireNonNull(state0, "state0");
	Objects.requireNonNull(state, "state");
	if (!(0.0 < dt && Double.isFinite(dt))) {
	    throw new IllegalArgumentException("dt " + dt);
	}
	final int nState = state0.getDimension();
	if (state.getDimension() != nState) {
	    throw new IllegalArgumentException(
		    "Inconsistent dimensions x0 " + nState + " and x " + state.getDimension());
	}
	if (dedState.length != nState) {
	    throw new IllegalArgumentException(
		    "Inconsistent length of dedx " + dedState.length + " and dimension of x0 " + nState);
	}

	final double rate = 1.0 / dt;

	final ImmutableVector x0 = extract(state0, positionTerm);
	final ImmutableVector v0 = extract(state0, velocityTerm);
	final ImmutableVector x = extract(state, positionTerm);
	final ImmutableVector v = extract(state, velocityTerm);

	final ImmutableVector dx = x.minus(x0);
	final ImmutableVector vMean = v.mean(v0);
	final ImmutableVector ve = dx.scale(rate).minus(vMean);

	final double e = 0.5 * mass * ve.magnitude2();
	final ImmutableVector dedx = ve.scale(mass * rate);
	final ImmutableVector dedv = ve.scale(-0.5 * mass);

	for (int i = 0, n = positionTerm.length; i < n; ++i) {
	    dedState[positionTerm[i]] += dedx.get(i);
	    dedState[velocityTerm[i]] += dedv.get(i);
	}
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
     * Which terms in the solution space vector correspond to the components of the
     * position vector of the body.
     * </p>
     * 
     * @param i
     *            The component of interest.
     * @return the index of the component of the position vector; not negative
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code i} is negative.</li>
     *             <li>If {@code i} is not less than the
     *             {@linkplain #getSpaceDimension() space dimension}.</li>
     *             </ul>
     */
    public final int getPositionTerm(int i) {
	return positionTerm[i];
    }

    /**
     * <p>
     * The number of space dimensions for which this calculates a position error.
     * </p>
     * 
     * @return
     */
    public final int getSpaceDimension() {
	return positionTerm.length;
    }

    /**
     * <p>
     * Which terms in the solution space vector correspond to the components of the
     * velocity vector of the body.
     * </p>
     * 
     * @param i
     *            The component of interest.
     * @return the index of the component of the velocity vector; not negative
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code i} is negative.</li>
     *             <li>If {@code i} is not less than the
     *             {@linkplain #getSpaceDimension() space dimension}.</li>
     *             </ul>
     */
    public final int getVelocityTerm(int i) {
	return velocityTerm[i];
    }

    /**
     * <p>
     * Whether this term can be calculated for a physical state vector that has a
     * given number of variables.
     * </p>
     * <ul>
     * <li>This is valid for a given dimension if, and only if, the number of
     * variables exceeds the largest {@linkplain #getPositionTerm(int) position term
     * index} and exceeds the largest {@linkplain #getVelocityTerm(int) velocity
     * term index}.</li>
     * </ul>
     * 
     * @return whether valid.
     * @throws IllegalArgumentException
     *             If {@code n} is not positive.
     */
    @Override
    public boolean isValidForDimension(int n) {
	for (int i = 0, pn = positionTerm.length; i < pn; ++i) {
	    if (n < positionTerm[i] + 1 || n < velocityTerm[i] + 1) {
		return false;
	    }
	}
	return true;
    }

}
