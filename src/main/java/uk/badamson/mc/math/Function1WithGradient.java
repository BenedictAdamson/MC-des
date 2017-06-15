package uk.badamson.mc.math;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A functor for a one-dimensional function of a continuous variable that also
 * has a computable gradient.
 * </p>
 */
@FunctionalInterface
@Immutable
public interface Function1WithGradient {

	/**
	 * <p>
	 * The value of the function and its gradient for a given value of the
	 * continuous variable.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) value and gradient.</li>
	 * <li>The {@linkplain Function1ValueWithGradient#getX() domain value} of
	 * the returned object is the given domain value.</li>
	 * </ul>
	 * 
	 * @param x
	 *            The domain value
	 * @return The value of the function.
	 */
	public Function1ValueWithGradient value(double x);
}
