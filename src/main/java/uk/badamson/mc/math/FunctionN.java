package uk.badamson.mc.math;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

/**
 * <p>
 * A functor for a multi-dimensional function of continuous variables.
 * </p>
 */
@Immutable
@NotThreadSafe
public interface FunctionN {

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
	 * The value of the function for given values of the continuous variables.
	 * </p>
	 * <p>
	 * Because this interface is {@linkplain NotThreadSafe not thread safe},
	 * callers should not alter the content of the {@code x} array during the
	 * computation of teh value.
	 * </p>
	 * 
	 * @param x
	 *            The values of the continuous variables; x[i] is the value of
	 *            variable <var>i</var>.
	 * @return The value of the function.
	 * 
	 * @throws NullPointerException
	 *             If {@code x} is null.
	 * @throws IndexOutOfBoundsException
	 *             (Optional) If the length of {@code x} is not equal to the
	 *             {@linkplain #getDimension() number of dimensions} of this
	 *             function. In practice, many implementations will not complain
	 *             is the length of {@code x} exceeds the number of dimensions
	 *             of this function.
	 */
	public double value(double[] x);
}
