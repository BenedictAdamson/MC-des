package uk.badamson.mc.math;

import java.util.Objects;

/**
 * <p>
 * Functions and auxiliary classes for minimization of a {@linkplain FunctionN
 * multidimensional function}.
 * </p>
 */
public final class MinN {

	/**
	 * <p>
	 * Create a {@linkplain Function1 functor for a one-dimensional function of
	 * a continuous variable} that is the evaluation of a {@linkplain FunctionN
	 * functor for a multi-dimensional function of continuous variables} along a
	 * given line.
	 * </p>
	 * <p>
	 * The created functor retains references to the given objects. Those
	 * objects should therefore not be changed while the created function is in
	 * use.
	 * </p>
	 * 
	 * @param f
	 *            The multi-dimensional function
	 * @param x0
	 *            The origin point; the position in the space of the
	 *            multidimensional function corresponding to the origin point of
	 *            the created function.
	 * @param dx
	 *            The direction vector of the line in the space of the
	 *            multidimensional function; (x + dx) corresponds to the value
	 *            for 1.0 of the created function.
	 * @return the created functor; not null.
	 * 
	 * @throws NullPointerException
	 *             <ul>
	 *             <li>If {@code f} is null.</li>
	 *             <li>If {@code x0} is null.</li>
	 *             <li>If {@code dx} is null.</li>
	 *             </ul>
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>If the length of {code x0} is 0.</li>
	 *             <li>If the length of {code x0} is different from the length
	 *             of {@code dx}.</li></li>
	 *             <li>If the length of {code x0} is different from the
	 *             {@linkplain FunctionN#getDimensions() number of dimensions}
	 *             of {@code f}.</li></li>
	 *             </ul>
	 */
	static Function1 createLineFunction(final FunctionN f, final double[] x0, final double[] dx) {
		Objects.requireNonNull(f, "f");
		Objects.requireNonNull(x0, "x0");
		Objects.requireNonNull(dx, "dx");
		final int n = x0.length;
		if (n == 0) {
			throw new IllegalArgumentException("x0.length == 0");
		}
		if (n != dx.length || n != f.getDimensions()) {
			throw new IllegalArgumentException(
					"Inconsistent lengths, x0 " + n + ", dx " + dx.length + ", f.dimensions " + f.getDimensions());
		}

		return new Function1() {

			@Override
			public double value(double w) {
				final double[] x = new double[n];
				for (int i = 0; i < n; i++) {
					x[i] = x0[i] + w * dx[i];
				}
				return f.value(x);
			}
		};
	}

	/**
	 * <p>
	 * Perform <i>line minimisation</i> of a {@linkplain FunctionN
	 * multidimensional function}.
	 * </p>
	 * <p>
	 * That is, find the minimum value of the function along a straight line.
	 * </p>
	 * 
	 * <section>
	 * <h1>Post Conditions</h1>
	 * <ul>
	 * <li>The point on the line ({@code x}) has been moved to the position of
	 * the minimum found.</li>
	 * <li>The direction vector has been set to the amount the point of the line
	 * was moved to move from the original position to the position of the
	 * minimum.</li>
	 * </ul>
	 * </section>
	 * 
	 * @param f
	 *            The multi-dimensional function
	 * @param x
	 *            A point on the line.
	 * @param dx
	 *            The direction vector of the line.
	 * @return the minimum value along the line.
	 * 
	 * @throws NullPointerException
	 *             <ul>
	 *             <li>If {@code f} is null.</li>
	 *             <li>If {@code x} is null.</li>
	 *             <li>If {@code dx} is null.</li>
	 *             </ul>
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>If the length of {code x} is 0.</li>
	 *             <li>If the length of {code x} is different from the length of
	 *             {@code dx}.</li></li>
	 *             <li>If the length of {code x} is different from the
	 *             {@linkplain FunctionN#getDimensions() number of dimensions}
	 *             of {@code f}.</li></li>
	 *             </ul>
	 */
	static double minimiseAlongLine(final FunctionN f, final double[] x, final double[] dx) {
		final Function1 fLine = createLineFunction(f, x, dx);
		final Min1.Bracket bracket = Min1.findBracket(fLine, 0.0, 1.0);
		final Point2 p = Min1.findBrent(fLine, bracket, Min1.TOLERANCE);
		final double w = p.getX();
		for (int i = 0, n = x.length; i < n; i++) {
			final double dxi = dx[i] * w;
			dx[i] = dxi;
			x[i] += dxi;
		}
		return p.getY();
	}

	private MinN() {
		throw new AssertionError("Class should not be instantiated");
	}
}
