package uk.badamson.mc.math;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A functor for a multi-dimensional function of continuous variables.
 * </p>
 */
@Immutable
public interface FunctionN {

	/**
	 * <p>
	 * The value of the function for given values of the continuous variables.
	 * </p>
	 * 
	 * @param x
	 *            The values of the continuous variables; x[i] is the value of
	 *            variable <var>i</var>.
	 * @return The value of the function.
	 * 
	 * @throws NullPointerException
	 *             If {@code x} is null.
	 * @throws ArrayIndexOutOfBoundsException
	 *             (Optional) If the length of {@code x} is not equal to the
	 *             {@linkplain #getDimensions() number of dimensions} of this
	 *             function. In practice, many implementations will not complain
	 *             is the length of {@code x} exceeds the number of dimensions
	 *             of this function.
	 */
	public double value(double[] x);

	/**
	 * <p>
	 * The number of independent variables of the function.
	 * </p>
	 * 
	 * @return the number of dimensions; positive.
	 */
	public int getDimensions();
}
