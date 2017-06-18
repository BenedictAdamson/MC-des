package uk.badamson.mc.math;

import java.util.Arrays;
import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A constant (immutable) mathematical vector or pseudo vector
 * </p>
 */
@Immutable
public final class ImmutableVector {

	/**
	 * <p>
	 * Create a vector from its components.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) vector.</li>
	 * <li>This has the given values for its components.</li>
	 * <li>The {@linkplain #getDimension() number of dimensions} of this vector
	 * is equal to the length of the given array of components.</li>
	 * </ul>
	 *
	 * @param x
	 *            The components of this vector
	 * @return the created vector
	 * 
	 * @throws NullPointerException
	 *             If {@code x} is null.
	 * @throws IllegalArgumentException
	 *             If {@code x} is empty (length 0)
	 */
	public static ImmutableVector create(double... x) {
		Objects.requireNonNull(x, "x");
		final int n = x.length;
		if (n == 0) {
			throw new IllegalArgumentException("x is empty");
		}
		return new ImmutableVector(Arrays.copyOf(x, n));
	}

	/**
	 * <p>
	 * Create a vector that lies along a line given by an origin point and
	 * position vector.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) vector.</li>
	 * <li>The {@linkplain ImmutableVector#getDimension() dimension} of the
	 * returned vector is equal to the dimension of thetwo input vectors.</li>
	 * <li>Returns the vector <code>x0 + w dx</code></li>
	 * </ul>
	 * 
	 * @param x0
	 *            The original point
	 * @param dx
	 *            The direction vector along the line
	 * @param w
	 *            Position parameter giving the position along the line.
	 * @return the indicate point on the line
	 * 
	 * @throws NullPointerException
	 *             <ul>
	 *             <li>If {@code x0} is null.</li>
	 *             <li>If {@code dx} is null.</li>
	 *             </ul>
	 * @throws IllegalArgumentException
	 *             If {@code x0} and {@code dx} have different
	 *             {@linkplain ImmutableVector#getDimension() dimensions}.
	 */
	public static ImmutableVector createOnLine(final ImmutableVector x0, ImmutableVector dx, double w) {
		Objects.requireNonNull(x0, "x0");
		Objects.requireNonNull(dx, "dx");
		requireConsistentDimensions(x0, dx);
		final int n = x0.getDimension();

		final double[] x = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = x0.x[i] + w * dx.x[i];
		}
		return new ImmutableVector(x);
	}

	private static void requireConsistentDimensions(ImmutableVector v1, ImmutableVector v2) {
		if (v1.getDimension() != v2.getDimension()) {
			throw new IllegalArgumentException(
					"Inconsistent dimensions, " + v1.getDimension() + ", " + v2.getDimension());
		}
	}

	private final double[] x;

	private ImmutableVector(double... x) {
		this.x = x;
	}

	/**
	 * <p>
	 * Calculate the dot product of this vector and another vector.
	 * </p>
	 * 
	 * @param that
	 *            The other vector
	 * @return the product
	 * 
	 * @throws NullPointerException
	 *             If {@code that} is null.
	 * @throws IllegalArgumentException
	 *             If the {@linkplain #getDimension() dimension} of {@code that}
	 *             is not equal to the dimension of this.
	 */
	public double dot(ImmutableVector that) {
		Objects.requireNonNull(that, "that");
		requireConsistentDimensions(this, that);

		double d = 0.0;
		for (int i = 0, n = x.length; i < n; ++i) {
			d += x[i] * that.x[i];
		}
		return d;
	}

	/**
	 * <p>
	 * Whether this object is <dfn>equivalent</dfn> to another object.
	 * </p>
	 * <p>
	 * The {@link ImmutableVector} class has <i>value semantics</i>: this object
	 * is equivalent to another if, and only if, the other object is also an
	 * {@link ImmutableVector} and they have equivalent attribtues.
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
		ImmutableVector other = (ImmutableVector) obj;
		return Arrays.equals(x, other.x);
	}

	/**
	 * <p>
	 * One of the components of this vector.
	 * </p>
	 * 
	 * @param i
	 *            The index of the component.
	 * @return the component.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             If {@code i} is less than 0 or greater than or equal to the
	 *             {@linkplain #getDimension() number of dimensions} of thsi
	 *             vector.
	 */
	public final double get(int i) {
		return x[i];
	}

	/**
	 * <p>
	 * The number of dimensions of this vector.
	 * </p>
	 * <ul>
	 * <li>The number of dimensions is positive.</li>
	 * </ul>
	 * 
	 * @return the number of dimensions
	 */
	public final int getDimension() {
		return x.length;
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(x);
	}

	/**
	 * <p>
	 * Create the vector that is opposite in direction of this vector.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) vector.</li>
	 * <li>The opposite vector has the same {@linkplain #getDimension()
	 * dimension} as this vector.</li>
	 * <li>The {@linkplain #get(int) components} of the opposite vector are the
	 * negative of the corresponsing component of this vector.</li>
	 * </ul>
	 * 
	 * @return the opposite vector; not null
	 */
	public final ImmutableVector minus() {
		final int n = x.length;
		final double[] minus = new double[n];
		for (int i = 0; i < n; ++i) {
			minus[i] = -x[i];
		}
		return new ImmutableVector(minus);
	}

	@Override
	public final String toString() {
		return Arrays.toString(x);
	}
}
