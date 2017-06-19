package uk.badamson.mc.physics;

import java.util.Arrays;
import java.util.Objects;

import uk.badamson.mc.math.ImmutableVector;;

/**
 * <p>
 * An abstract base class for implementing a
 * {@linkplain TimeStepEnergyErrorFunctionTerm term} for a
 * {@linkplain TimeStepEnergyErrorFunction functor that calculates the physical
 * modelling error of a system at a future point in time}
 * </p>
 */
public abstract class AbstractTimeStepEnergyErrorFunctionTerm implements TimeStepEnergyErrorFunctionTerm {

	protected static int[] copyTermIndex(int[] index) {
		final int[] copy = Arrays.copyOf(index, index.length);
		/* Check precondition after copy to avoid race hazards. */
		for (int i : copy) {
			if (i < 0) {
				throw new IllegalArgumentException("Negative index term " + i);
			}
		}
		return copy;
	}

	protected static ImmutableVector extract(ImmutableVector x, int term[]) {
		final int n = term.length;
		final double[] extract = new double[n];
		for (int i = 0; i < n; i++) {
			extract[i] = x.get(term[i]);
		}
		return ImmutableVector.create(extract);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * The provided implementation checks its arguments and returns 0.
	 * </p>
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
	public double evaluate(double[] dedx, ImmutableVector state0, ImmutableVector state, double dt) {
		Objects.requireNonNull(dedx, "dedx");
		Objects.requireNonNull(state0, "x0");
		Objects.requireNonNull(state, "x");
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

		return 0.0;
	}
}
