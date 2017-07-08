package uk.badamson.mc.math;

import java.util.Arrays;
import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A constant (immutable) mathematical vector or pseudo vector.
 * </p>
 * <ul>
 * <li>A vector is a {@linkplain ImmutableMatrix matrix} that has only one
 * {@linkplain #getColumns() column}.</li>
 * </ul>
 */
@Immutable
public final class ImmutableVector extends ImmutableMatrix {

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
	 * Create a zero vector having a given dimension.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) vector.</li>
	 * <li>The {@linkplain #getDimension() dimension} of the zero vector is the
	 * given dimension.</li>
	 * <li>The {@linkplain #get(int) elements} of the zero vector are all
	 * zero.</li>
	 * </ul>
	 * 
	 * @param dimension
	 *            The dimension
	 * @return the zero vector.
	 * @throws IllegalArgumentException
	 *             If {@code dimension} is not positive
	 */
	public static ImmutableVector create0(int dimension) {
		if (dimension <= 0) {
			throw new IllegalArgumentException("dimension " + dimension);
		}
		return new ImmutableVector(new double[dimension]);
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
			x[i] = x0.elements[i] + w * dx.elements[i];
		}
		return new ImmutableVector(x);
	}

	private static void requireConsistentDimensions(ImmutableVector v1, ImmutableVector v2) {
		if (v1.getDimension() != v2.getDimension()) {
			throw new IllegalArgumentException(
					"Inconsistent dimensions, " + v1.getDimension() + ", " + v2.getDimension());
		}
	}

	/**
	 * <p>
	 * Calculate the sum of several vectors that have the same
	 * {@linkplain #getDimension() dimension}.
	 * </p>
	 * </ul>
	 * <li>Always returns a (non null) sum vector.</li>
	 * <li>The dimension of the sum equals the dimension of the summed
	 * vectors.</li>
	 * </ul>
	 * 
	 * @param x
	 *            The vectors to sum
	 * @return The sum; not null
	 * 
	 * @throws NullPointerException
	 *             <ul>
	 *             <li>If {@code x} is null.</li>
	 *             <li>If {@code x} has any null elements.</li>
	 *             </ul>
	 * @throws IllegalArgumentException
	 *             If the elements of {@code x} do not have the same
	 *             {@linkplain #getDimension() dimension}.
	 * @see #plus(ImmutableVector)
	 */
	public static ImmutableVector sum(ImmutableVector... x) {
		Objects.requireNonNull(x, "x");
		final int n = x.length;
		if (n == 0) {
			throw new IllegalArgumentException("Number of vector arguments");
		}
		Objects.requireNonNull(x[0], "x[0]");

		final int d = x[0].getDimension();
		final double[] sum = new double[d];
		for (int j = 0; j < n; ++j) {
			final ImmutableVector xj = x[j];
			Objects.requireNonNull(xj, "x[j]");
			if (xj.getDimension() != d) {
				throw new IllegalArgumentException("Inconsistent dimension " + d + ", " + xj.getDimension());
			}

			for (int i = 0; i < d; ++i) {
				sum[i] += xj.get(i);
			}
		}

		return new ImmutableVector(sum);
	}

	/**
	 * <p>
	 * Calculate the weighted sum of several vectors that have the same
	 * {@linkplain #getDimension() dimension}.
	 * </p>
	 * </ul>
	 * <li>Always returns a (non null) sum vector.</li>
	 * <li>The dimension of the sum equals the dimension of the summed
	 * vectors.</li>
	 * </ul>
	 * 
	 * @param weight
	 *            The weights to apply; {@code weight[i]} is the weight for
	 *            vector {@code x[i]}.
	 * @param x
	 *            The vectors to sum
	 * @return The weighted sum; not null
	 * 
	 * @throws NullPointerException
	 *             <ul>
	 *             <li>If {@code weight} is null.</li>
	 *             <li>If {@code x} is null.</li>
	 *             <li>If {@code x} has any null elements.</li>
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>If {@code weight} has a length of 0.</li>
	 *             <li>If {@code weight} and {@code x} have different
	 *             lengths.</li>
	 *             <li>If the elements of {@code x} do not have the same
	 *             {@linkplain #getDimension() dimension}.</li>
	 *             </ul>
	 */
	public static ImmutableVector weightedSum(double[] weight, ImmutableVector[] x) {
		Objects.requireNonNull(weight, "weight");
		Objects.requireNonNull(x, "x");
		final int n = weight.length;
		if (n == 0) {
			throw new IllegalArgumentException("weight.length " + n);
		}
		if (n != x.length) {
			throw new IllegalArgumentException("Inconsistent lengths weight.length " + n + " x.length " + x.length);
		}
		Objects.requireNonNull(x[0], "x[0]");

		final int d = x[0].getDimension();
		final double[] sum = new double[d];
		for (int j = 0; j < n; ++j) {
			final double wj = weight[j];
			final ImmutableVector xj = x[j];
			Objects.requireNonNull(xj, "x[j]");
			if (xj.getDimension() != d) {
				throw new IllegalArgumentException("Inconsistent dimension " + d + ", " + xj.getDimension());
			}

			for (int i = 0; i < d; ++i) {
				sum[i] += wj * xj.get(i);
			}
		}

		return new ImmutableVector(sum);
	}

	ImmutableVector(double... x) {
		super(x.length, 1, x);
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
		for (int i = 0, n = elements.length; i < n; ++i) {
			d += elements[i] * that.elements[i];
		}
		return d;
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
		return elements[i];
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
		return elements.length;
	}

	private double getScale() {
		double scale = 0.0;
		for (double xI : elements) {
			scale = Math.max(scale, Math.abs(xI));
		}
		return scale;
	}

	/**
	 * <p>
	 * The magnitude of this vector.
	 * </p>
	 * 
	 * @return the magnitude
	 */
	public final double magnitude() {
		double scale = getScale();
		if (!Double.isFinite(scale) || scale < Double.MIN_NORMAL) {
			return scale;
		} else {
			final double r = 1.0 / scale;
			double m2 = 0.0;
			for (double xI : elements) {
				final double xIScaled = xI * r;
				m2 += xIScaled * xIScaled;
			}
			return Math.sqrt(m2) * scale;
		}
	}

	/**
	 * <p>
	 * The square of the magnitude of this vector.
	 * </p>
	 * <p>
	 * The method takes care to properly handle vectors with components that are
	 * large, not numbers, or which differ greatly in magnitude. It is otherwise
	 * similar to the {@linkplain #dot(ImmutableVector) dot product} of the
	 * vector with itself.
	 * </p>
	 * 
	 * @return the square of the magnitude.
	 */
	public final double magnitude2() {
		/* Use a scaling value to avoid overflow. */
		double scale = getScale();
		final double scale2 = scale * scale;
		if (!Double.isFinite(scale) || scale < Double.MIN_NORMAL) {
			return scale2;
		} else {
			final double r = 1.0 / scale;
			double m2 = 0.0;
			for (double xI : elements) {
				final double xIScaled = xI * r;
				m2 += xIScaled * xIScaled;
			}
			return m2 * scale2;
		}
	}

	/**
	 * <p>
	 * Create the vector that is the mean of this vector with another vector.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) vector.</li>
	 * <li>The {@linkplain ImmutableVector#getDimension() dimension} of the mean
	 * vector is equal to the dimension of this vector.</li>
	 * </ul>
	 * 
	 * @param that
	 *            The vector to take the mean with
	 * @return the mean vector
	 * 
	 * @throws NullPointerException
	 *             If {@code that} is null.
	 * @throws IllegalArgumentException
	 *             If the {@linkplain ImmutableVector#getDimension() dimension}
	 *             of }@code that} is not equal to ehe dimension of this vector.
	 */
	public final ImmutableVector mean(ImmutableVector that) {
		Objects.requireNonNull(that, "that");
		requireConsistentDimensions(this, that);
		final int n = elements.length;
		final double[] mean = new double[n];
		for (int i = 0; i < n; i++) {
			mean[i] = (elements[i] + that.elements[i]) * 0.5;
		}
		return new ImmutableVector(mean);
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
		final int n = elements.length;
		final double[] minus = new double[n];
		for (int i = 0; i < n; ++i) {
			minus[i] = -elements[i];
		}
		return new ImmutableVector(minus);
	}

	/**
	 * <p>
	 * Create the vector that is a given vector subtracted from this vector; the
	 * difference between this vector and another.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) vector.</li>
	 * <li>The difference vector has the same {@linkplain #getDimension()
	 * dimension} as this vector.</li>
	 * <li>The {@linkplain #get(int) components} of the difference vector are
	 * the difference of the corresponding component of this vector.</li>
	 * </ul>
	 * 
	 * @param that
	 *            The other vector
	 * @return the difference vector
	 * 
	 * @throws NullPointerException
	 *             If {@code that} is null.
	 * @throws IllegalArgumentException
	 *             If the {@linkplain #getDimension() dimension} of {@code that}
	 *             is not equal to the dimension of this.
	 */
	public final ImmutableVector minus(ImmutableVector that) {
		Objects.requireNonNull(that, "that");
		requireConsistentDimensions(this, that);

		final int n = elements.length;
		final double[] minus = new double[n];
		for (int i = 0; i < n; ++i) {
			minus[i] = elements[i] - that.elements[i];
		}
		return new ImmutableVector(minus);
	}

	/**
	 * <p>
	 * Create the vector that is a given vector added to this vector; the sum of
	 * this vector and another.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) vector.</li>
	 * <li>The sum vector has the same {@linkplain #getDimension() dimension} as
	 * this vector.</li>
	 * <li>The {@linkplain #get(int) components} of the sum vector are the sum
	 * with the corresponding component of this vector.</li>
	 * </ul>
	 * 
	 * @param that
	 *            The other vector
	 * @return the sum vector
	 * 
	 * @throws NullPointerException
	 *             If {@code that} is null.
	 * @throws IllegalArgumentException
	 *             If the {@linkplain #getDimension() dimension} of {@code that}
	 *             is not equal to the dimension of this.
	 * @see #sum(ImmutableVector...)
	 */
	public final ImmutableVector plus(ImmutableVector that) {
		Objects.requireNonNull(that, "that");
		requireConsistentDimensions(this, that);

		final int n = elements.length;
		final double[] minus = new double[n];
		for (int i = 0; i < n; ++i) {
			minus[i] = elements[i] + that.elements[i];
		}
		return new ImmutableVector(minus);
	}

	/**
	 * <p>
	 * Create a vector that is this vector scaled by a given scalar.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) vector.
	 * <li>
	 * <li>The {@linkplain ImmutableVector#getDimension() dimension} of the
	 * scaled vector is equal to the dimension of this vector.</li>
	 * </ul>
	 * 
	 * @param f
	 *            the scalar
	 * @return the scaled vector
	 */
	public final ImmutableVector scale(double f) {
		final int n = elements.length;
		final double[] s = new double[n];
		for (int i = 0; i < n; ++i) {
			s[i] = elements[i] * f;
		}
		return new ImmutableVector(s);
	}

	@Override
	public final String toString() {
		return Arrays.toString(elements);
	}

}
