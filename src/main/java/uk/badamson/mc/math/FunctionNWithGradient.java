package uk.badamson.mc.math;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A functor for a scalar function of a vector that also has a computable
 * gradient.
 * </p>
 */
@Immutable
public interface FunctionNWithGradient {

	/**
	 * <p>
	 * The number of independent variables of the function.
	 * </p>
	 * <p>
	 * This attribute must be <dfn>constant</dfn>: the value for a given object
	 * must always be the same value.
	 * </p>
	 * 
	 * @return the number of dimensions; positive.
	 */
	public int getDimension();

	/**
	 * <p>
	 * The value of the function and its gradient for a given value of the
	 * continuous variable.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) value.</li>
	 * <li>The {@linkplain Function1WithGradientValue#getX() domain value} of
	 * the returned object is the given domain value.</li>
	 * </ul>
	 * 
	 * @param x
	 *            The domain value
	 * @return The value of the function.
	 */
	public FunctionNWithGradientValue value(ImmutableVector x);
}
