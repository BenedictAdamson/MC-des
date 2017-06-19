package uk.badamson.mc.physics.kinematics;

import java.util.Objects;

import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTerm;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunction;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunctionTerm;;

/**
 * <p>
 * An abstract base class for implementing a
 * {@linkplain TimeStepEnergyErrorFunctionTerm term} for a
 * {@linkplain TimeStepEnergyErrorFunction functor that calculates the
 * kinematics modelling error of a system at a future point in time}.
 * </p>
 * <p>
 * This term calculates the error in only one {@linkplain #getDirection()
 * direction}. The error functor should therefore include a
 * {@linkplain TimeStepEnergyErrorFunction#getTerms() term} of the same class
 * for each dimension of space (usually 3) for each body. The directions for
 * each body must not be co-linear.
 * </p>
 */
public abstract class KinematicsError extends AbstractTimeStepEnergyErrorFunctionTerm {

	private final ImmutableVector direction;
	private final double mass;
	private final int[] positionTerm;
	private final int[] velocityTerm;

	/**
	 * <p>
	 * Construct a kinematics error term.
	 * </p>
	 *
	 * <section>
	 * <h1>Post Conditions</h1>
	 * <ul>
	 * <li>The constructed object has attribute values equal to the given
	 * values.</li>
	 * </ul>
	 * </section>
	 *
	 * @param direction
	 *            The direction in which to calculate the error term.
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
	 *             <li>If {@code direction} is null.</li>
	 *             <li>If {@code positionTerm} is null.</li>
	 *             <li>If {@code velocityTerm} is null.</li>
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>If {@code direction} is not a unit vector.</li>
	 *             <li>If {@code mass} is not a positive and
	 *             {@linkplain Double#isFinite(double) finite}.</li>
	 *             <li>If the length of {@code positionTerm} does not equal the
	 *             {@linkplain ImmutableVector#getDimension() dimension} of
	 *             {@code direction}.</li>
	 *             <li>If the length of {@code velocityTerm} does not equal the
	 *             dimension of {@code direction}.</li>
	 *             <li>If {@code positionTerm} has any negative values.</li>
	 *             <li>If {@code velocityTerm} has any negative values.</li>
	 *             </ul>
	 */
	protected KinematicsError(ImmutableVector direction, double mass, int[] positionTerm, int[] velocityTerm) {
		Objects.requireNonNull(direction, "direction");
		Objects.requireNonNull(positionTerm, "positionTerm");
		Objects.requireNonNull(velocityTerm, "velocityTerm");

		final int n = direction.getDimension();
		if (positionTerm.length != n) {
			throw new IllegalArgumentException("Inconsistent positionTerm.length <" + positionTerm.length
					+ "> and direction.dimension<" + n + ">");
		}
		if (velocityTerm.length != n) {
			throw new IllegalArgumentException("Inconsistent velocityTerm.length <" + velocityTerm.length
					+ "> and direction.dimension<" + n + ">");
		}

		this.direction = direction;
		this.mass = mass;
		this.positionTerm = copyTermIndex(positionTerm);
		this.velocityTerm = copyTermIndex(velocityTerm);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <ol>
	 * <li>The method uses the {@linkplain #getPositionTerm(int) position term
	 * index} information and {@linkplain #getVelocityTerm(int) velocity term
	 * index} information to extract position and velocity vectors from the
	 * given state vectors.</li>
	 * <li>It reduces those vectors to scalars by finding their components along
	 * the {@linkplain #getDirection() direction vector}.</li>
	 * <li>From that it calculates an equivalent velocity error for the velocity
	 * along that direction.</li>
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
	public abstract double evaluate(double[] dedx, ImmutableVector state0, ImmutableVector state, double dt);

	/**
	 * <p>
	 * The direction in which to calculate the error term.
	 * </p>
	 * <ul>
	 * <li>Always have a (non null) direction.</li>
	 * <li>The direction vector is a unit vector (it has
	 * {@linkplain ImmutableVector#magnitude() magnitude} 1.0).</li>
	 * </ul>
	 * 
	 * @return the direction vector.
	 */
	public final ImmutableVector getDirection() {
		return direction;
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
	 * Which terms in the solution space vector correspond to the components of
	 * the position vector of the body.
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
	 * The number of space dimensions for which this calculates a position
	 * error.
	 * </p>
	 * <ul>
	 * <li>The number of space dimensions is equal to the
	 * {@linkplain ImmutableVector#getDimension() dimension} of the
	 * {@linkplain #getDirection() direction vector} along which this calculates
	 * the position error.
	 * </ul>
	 * 
	 * @return
	 */
	public final int getSpaceDimension() {
		return direction.getDimension();
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
	 * variables exceeds the largest {@linkplain #getPositionTerm(int) position
	 * term index} and exceeds the largest {@linkplain #getVelocityTerm(int)
	 * velocity term index}.</li>
	 * </ul>
	 * 
	 * @return whether valid.
	 * @throws IllegalArgumentException
	 *             If {@code n} is not positive.
	 */
	@Override
	public final boolean isValidForDimension(int n) {
		for (int i = 0, pn = positionTerm.length; i < pn; ++i) {
			if (n < positionTerm[i] + 1 || n < velocityTerm[i] + 1) {
				return false;
			}
		}
		return true;
	}

}
