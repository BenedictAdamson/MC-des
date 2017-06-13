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

	private MinN() {
		throw new AssertionError("Class should not be instantiated");
	}
}
