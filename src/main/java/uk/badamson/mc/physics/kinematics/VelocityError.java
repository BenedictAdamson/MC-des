package uk.badamson.mc.physics.kinematics;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTerm;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunction;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunctionTerm;;

/**
 * <p>
 * A {@linkplain TimeStepEnergyErrorFunctionTerm term} for a
 * {@linkplain TimeStepEnergyErrorFunction functor that calculates the physical
 * modelling error of a system at a future point in time} that gives the degree
 * of inconsistency of the velocity and acceleration of a body.
 * </p>
 */
@Immutable
public final class VelocityError extends AbstractTimeStepEnergyErrorFunctionTerm {

	private final double mass;
	private final int[] velocityTerm;
	private final int[] accelerationTerm;

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
	 * <li>The {@linkplain #getSpaceDimension() space dimension} of the
	 * constructed object is equal to the length of the arrays of position
	 * terms.</li>
	 * </ul>
	 * </section>
	 * 
	 * @param mass
	 *            A reference mass scale.
	 * @param velocityTerm
	 *            Which terms in the solution space vector correspond to the
	 *            components of the velocity vector of the body.
	 *            {@code velocityTerm[i]} is the index of component
	 *            <var>i</var>.
	 * @param accelerationTerm
	 *            Which terms in the solution space vector correspond to the
	 *            components of the acceleration vector of the body.
	 *            {@code velocityTerm[i]} is the index of component
	 *            <var>i</var>.
	 *
	 * @throws NullPointerException
	 *             <ul>
	 *             <li>If {@code velocityTerm} is null.</li>
	 *             <li>If {@code accelerationTerm} is null.</li>
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>If {@code mass} is not a positive and
	 *             {@linkplain Double#isFinite(double) finite}.</li>
	 *             <li>If the length of {@code velocityTerm} does not equal the
	 *             length of {@code accelerationTerm}.</li>
	 *             <li>If {@code velocityTerm} has any negative values.</li>
	 *             <li>If {@code accelerationTerm} has any negative values.</li>
	 *             </ul>
	 */
	public VelocityError(double mass, int[] velocityTerm, int[] accelerationTerm) {
		this.mass = requireReferenceScale(mass, "mass");
		this.velocityTerm = copyTermIndex(velocityTerm, "velocityTerm");
		this.accelerationTerm = copyTermIndex(accelerationTerm, "accelerationTerm");
		requireConsistentLengths(velocityTerm, "velocityTerm", accelerationTerm, "accelerationTerm");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <ol>
	 * <li>The method uses the {@linkplain #getVelocityTerm(int) velocity term
	 * index} information and {@linkplain #getAccelerationTerm(int) acceleration
	 * term index} information to extract velocity and acceleration vectors from
	 * the given state vectors.</li>
	 * <li>It calculates a mean acceleration from the old and new acceleration
	 * values.</li>
	 * <li>It calculates the extrapolated velocity from the old velocity and the
	 * mean acceleration.</li>
	 * <li>It calculates a velocity error by comparing the new velocity with the
	 * extrapolated velocity.</li>
	 * <li>From that it calculates an equivalent kinetic energy error, using the
	 * {@linkplain #getMass() characteristic mass value}. That is the error term
	 * it returns.</li>
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
	 *             {@linkplain ImmutableVector#getDimension() dimension} of
	 *             {@code state0}.
	 */
	@Override
	public final double evaluate(double[] dedx, ImmutableVector state0, ImmutableVector state, double dt) {
		Objects.requireNonNull(dedx, "dedx");
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
		if (dedx.length != nState) {
			throw new IllegalArgumentException(
					"Inconsistent length of dedx " + dedx.length + " and dimension of x0 " + nState);
		}

		final ImmutableVector v0 = extract(state0, velocityTerm);
		final ImmutableVector a0 = extract(state0, accelerationTerm);
		final ImmutableVector v = extract(state, velocityTerm);
		final ImmutableVector a = extract(state, accelerationTerm);

		final ImmutableVector dv = v.minus(v0);
		final ImmutableVector aMean = a.mean(a0);
		final ImmutableVector ve = dv.minus(aMean.scale(dt));

		final double e = 0.5 * mass * ve.magnitude2();
		final ImmutableVector dedv = ve.scale(mass);
		final ImmutableVector deda = ve.scale(-0.5 * mass * dt);

		for (int i = 0, n = velocityTerm.length; i < n; ++i) {
			dedx[velocityTerm[i]] += dedv.get(i);
			dedx[accelerationTerm[i]] += deda.get(i);
		}
		return e;
	}

	/**
	 * <p>
	 * Which terms in the solution space vector correspond to the components of
	 * the acceleration vector of the body.
	 * </p>
	 * 
	 * @param i
	 *            The component of interest.
	 * @return the index of the component of the acceleration vector; not
	 *         negative
	 * 
	 * @throws IndexOutOfBoundsException
	 *             <ul>
	 *             <li>If {@code i} is negative.</li>
	 *             <li>If {@code i} is not less than the
	 *             {@linkplain #getSpaceDimension() space dimension}.</li>
	 *             </ul>
	 */
	public final int getAccelerationTerm(int i) {
		return accelerationTerm[i];
	}

	/**
	 * <p>
	 * A reference mass scale.
	 * </p>
	 * <p>
	 * The functor uses this value to convert a position error into an energy
	 * error. It is tempting to use the mass of the solid body for which this
	 * functor calculates the position error, but that will produce bad results
	 * if there are multiple bodies and they have very different masses; it is
	 * better to use the same value for all bodies, with that value equal to the
	 * mass of a typical body.
	 * </p>
	 * 
	 * @return the mass; positive and {@linkplain Double#isFinite(double)
	 *         finite}
	 */
	public final double getMass() {
		return mass;
	}

	/**
	 * <p>
	 * The number of space dimensions for which this calculates a velocity
	 * error.
	 * </p>
	 */
	public final int getSpaceDimension() {
		return velocityTerm.length;
	}

	/**
	 * <p>
	 * Which terms in the solution space vector correspond to the components of
	 * the velocity vector of the body.
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
	 * Whether this term can be calculated for a physical state vector that has
	 * a given number of variables.
	 * </p>
	 * <ul>
	 * <li>This is valid for a given dimension if, and only if, the number of
	 * variables exceeds the largest {@linkplain #getVelocityTerm(int) velocity
	 * term index} and exceeds the largest {@linkplain #getAccelerationTerm(int)
	 * acceleration term index}.</li>
	 * </ul>
	 * 
	 * @return whether valid.
	 * @throws IllegalArgumentException
	 *             If {@code n} is not positive.
	 */
	@Override
	public boolean isValidForDimension(int n) {
		for (int i = 0, pn = velocityTerm.length; i < pn; ++i) {
			if (n < velocityTerm[i] + 1 || n < accelerationTerm[i] + 1) {
				return false;
			}
		}
		return true;
	}

}
