package uk.badamson.mc.math;

import java.util.Arrays;
import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A point in multi-dimensional space.
 * </p>
 */
@Immutable
public final class PointN {

	/**
	 * <p>
	 * Create a multi dimensional value with given coordinate values.
	 * </p>
	 * 
	 * <section>
	 * <h1>Post Conditions</h1>
	 * <ul>
	 * <li>Returned a (non null) reference to a point in multi-dimensional
	 * space.</li></li>
	 * <li>The {@linkplain #getDimensions() number of dimensions} of the created
	 * point is equal to the length of the given array of coordinates.</li>
	 * <li>The {@linkplain #getX(int) coordinates} of the point are equal to the
	 * corresponding values of the given array.</li>
	 * </ul>
	 * </section>
	 * 
	 * @param x
	 *            The coordinates of this point.
	 * @throws NullPointerException
	 *             If {@code x} is null.
	 * @throws IllegalArgumentException
	 *             If the length of {@code x} is 0.
	 */
	public static final PointN create(double[] x) {
		Objects.requireNonNull(x, "x");
		if (x.length == 0) {
			throw new IllegalArgumentException("x.length == 0");
		}
		return new PointN(Arrays.copyOf(x, x.length));
	}

	private final double[] x;

	private PointN(double[] x) {
		this.x = x;
	}

	/**
	 * <p>
	 * Whether this object is <dfn>equivalent</dfn> another object.
	 * </p>
	 * <p>
	 * The {@link PointN} class has <i>value semantics</i>: this object is
	 * equivalent to another object if, and only if, the other object is also a
	 * {@link Point2} object, and the two objects have equivalent attributes.
	 * </p>
	 */
	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PointN other = (PointN) obj;
		return Arrays.equals(x, other.x);
	}

	/**
	 * <p>
	 * The number of dimensions of the space containing this function.
	 * </p>
	 * 
	 * @return the number of dimensions; positive.
	 */
	public int getDimensions() {
		return x.length;
	}

	/**
	 * <p>
	 * A coordinate of this point.
	 * </p>
	 * 
	 * @param i
	 *            The dimension for which the coordinate is wanted.
	 * @throws IndexOutOfBoundsException
	 *             <ul>
	 *             <li>If {@code i} is negative.</li>
	 *             <li>If {@code i} is not less than the
	 *             {@linkplain #getDimensions() number of dimensions} of this
	 *             point.</li>
	 *             </ul>
	 */
	public final double getX(int i) {
		return x[i];
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(x);
	}

	@Override
	public final String toString() {
		return Arrays.toString(x);
	}

}
