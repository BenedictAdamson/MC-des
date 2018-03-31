package uk.badamson.mc.math;

import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A constant (immutable) mathematical vector or pseudo vector of
 * {@linkplain #getRows() size} (dimension) 1.
 * </p>
 */
@Immutable
public final class ImmutableVector1 implements Vector {

    /**
     * <p>
     * The 1 dimensional zero vector.
     * </p>
     */
    public static final ImmutableVector1 ZERO = new ImmutableVector1(0.0);

    /**
     * <p>
     * The unit direction vector point along the x axis.
     * </p>
     */
    public static final ImmutableVector1 I = new ImmutableVector1(1.0);

    /**
     * <p>
     * Create a vector from its components.
     * </p>
     * <ul>
     * <li>Always returns a (non null) vector.</li>
     * <li>This has the given value for its component.</li>
     * </ul>
     *
     * @param x
     *            The x component of this vector
     * @return the created vector
     * 
     * @throws NullPointerException
     *             If {@code x} is null.
     */
    public static ImmutableVector1 create(double x) {
        return new ImmutableVector1(x);
    }

    private static void requireDimension1(Vector vector) {
        if (vector.getDimension() != 1) {
            throw new IllegalArgumentException("Inconsistent dimension, " + vector.getDimension());
        }
    }

    /**
     * <p>
     * Calculate the sum of several 3 dimensional vectors.
     * </p>
     * </ul>
     * <li>Always returns a (non null) sum vector.</li>
     * <li>The dimension of the sum equals the dimension of the summed vectors.</li>
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
     * @see #plus(ImmutableVector1)
     */
    public static ImmutableVector1 sum(ImmutableVector1... x) {
        Objects.requireNonNull(x, "x");
        final int n = x.length;
        if (n == 0) {
            throw new IllegalArgumentException("Number of vector arguments");
        }
        Objects.requireNonNull(x[0], "x[0]");

        double sumX = 0.0;
        for (int j = 0; j < n; ++j) {
            final ImmutableVector1 xj = x[j];
            Objects.requireNonNull(xj, "x[j]");
            sumX += xj.x;
        }

        return new ImmutableVector1(sumX);
    }

    /**
     * <p>
     * Calculate the weighted sum of several 3 dimensional vectors.
     * </p>
     * 
     * @param weight
     *            The weights to apply; {@code weight[i]} is the weight for vector
     *            {@code x[i]}.
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
     *             <li>If {@code weight} and {@code x} have different lengths.</li>
     *             </ul>
     */
    public static ImmutableVector1 weightedSum(double[] weight, ImmutableVector1[] x) {
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

        double sumX = 0.0;
        for (int j = 0; j < n; ++j) {
            final double wj = weight[j];
            final ImmutableVector1 xj = x[j];
            Objects.requireNonNull(xj, "x[j]");
            sumX += wj * xj.x;
        }

        return new ImmutableVector1(sumX);
    }

    private final double x;

    private ImmutableVector1(double x) {
        this.x = x;
    }

    /**
     * <p>
     * Calculate the dot product of this vector and another 3 dimensional vector.
     * </p>
     * 
     * @param that
     *            The other vector
     * @return the product
     * 
     * @throws NullPointerException
     *             If {@code that} is null.
     * @throws IllegalArgumentException
     *             If the {@linkplain #getDimension() dimension} of {@code that} is
     *             not equal to the dimension of this.
     */
    public double dot(ImmutableVector1 that) {
        Objects.requireNonNull(that, "that");

        return x * that.x;
    }

    @Override
    public double dot(Vector that) {
        if (that instanceof ImmutableVector1) {
            return dot((ImmutableVector1) that);
        } else {
            Objects.requireNonNull(that, "that");
            requireDimension1(that);

            return x * that.get(0);
        }
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImmutableVector1 other = (ImmutableVector1) obj;
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IndexOutOfBoundsException
     *             {@inheritDoc}
     */
    @Override
    public final double get(int i) {
        switch (i) {
        case 0:
            return x;
        default:
            throw new IndexOutOfBoundsException("i " + i);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IndexOutOfBoundsException
     *             {@inheritDoc}
     */
    @Override
    public final double get(int i, int j) {
        if (j != 0) {
            throw new IndexOutOfBoundsException("j " + j);
        }
        return get(i);
    }

    @Override
    public final int getColumns() {
        return 1;
    }

    @Override
    public final int getDimension() {
        return 1;
    }

    @Override
    public final int getRows() {
        return 1;
    }

    @Override
    public final int hashCode() {
        final long bits = Double.doubleToLongBits(x);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public final double magnitude() {
        return Math.abs(x);
    }

    @Override
    public final double magnitude2() {
        return x * x;
    }

    /**
     * <p>
     * Create the vector that is the mean of this vector with another vector.
     * </p>
     * <ul>
     * <li>Always returns a (non null) vector.</li>
     * <li>The {@linkplain ImmutableVector1#getDimension() dimension} of the mean
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
     *             If the {@linkplain ImmutableVector1#getDimension() dimension} of
     *             }@code that} is not equal to the dimension of this vector.
     */
    public final ImmutableVector1 mean(ImmutableVector1 that) {
        Objects.requireNonNull(that, "that");
        return new ImmutableVector1(0.5 * (x + that.x));
    }

    @Override
    public final ImmutableVector1 mean(Vector that) {
        if (that instanceof ImmutableVector1) {
            return mean((ImmutableVector1) that);
        } else {
            Objects.requireNonNull(that, "that");
            requireDimension1(that);
            return new ImmutableVector1(0.5 * (x + that.get(0)));
        }
    }

    @Override
    public final ImmutableVector1 minus() {
        return new ImmutableVector1(-x);
    }

    /**
     * <p>
     * Create the vector that is a given vector subtracted from this vector; the
     * difference between this vector and another.
     * </p>
     * <ul>
     * <li>Always returns a (non null) vector.</li>
     * <li>The difference vector has the same {@linkplain #getDimension() dimension}
     * as this vector.</li>
     * <li>The {@linkplain #get(int) components} of the difference vector are the
     * difference of the corresponding component of this vector.</li>
     * </ul>
     * 
     * @param that
     *            The other vector
     * @return the difference vector
     * 
     * @throws NullPointerException
     *             If {@code that} is null.
     * @throws IllegalArgumentException
     *             If the {@linkplain #getDimension() dimension} of {@code that} is
     *             not equal to the dimension of this.
     */
    public final ImmutableVector1 minus(ImmutableVector1 that) {
        Objects.requireNonNull(that, "that");
        return new ImmutableVector1(x - that.x);
    }

    @Override
    public final ImmutableVector1 minus(Vector that) {
        if (that instanceof ImmutableVector1) {
            return minus((ImmutableVector1) that);
        } else {
            Objects.requireNonNull(that, "that");
            requireDimension1(that);
            return new ImmutableVector1(x - that.get(0));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *             If {@code x} is null.
     * @throws IllegalArgumentException
     *             If {@code x} is not null, because a 3 dimensional vector can not
     *             be used to matrix-multiply a vector.
     */
    @Override
    public final Vector multiply(Vector x) {
        Objects.requireNonNull(x, "x");
        throw new IllegalArgumentException("Can not use a 1 dimensional vector to matrix-multipley a vector");
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
     * <li>The {@linkplain #get(int) components} of the sum vector are the sum with
     * the corresponding component of this vector.</li>
     * </ul>
     * 
     * @param that
     *            The other vector
     * @return the sum vector
     * 
     * @throws NullPointerException
     *             If {@code that} is null.
     * @throws IllegalArgumentException
     *             If the {@linkplain #getDimension() dimension} of {@code that} is
     *             not equal to the dimension of this.
     * @see #sum(ImmutableVector1...)
     */
    public final ImmutableVector1 plus(ImmutableVector1 that) {
        Objects.requireNonNull(that, "that");
        return new ImmutableVector1(x + that.x);
    }

    @Override
    public final ImmutableVector1 plus(Vector that) {
        if (that instanceof ImmutableVector1) {
            return plus((ImmutableVector1) that);
        } else {
            Objects.requireNonNull(that, "that");
            requireDimension1(that);
            return new ImmutableVector1(x + that.get(0));
        }
    }

    @Override
    public final ImmutableVector1 scale(double f) {
        return new ImmutableVector1(x * f);
    }

    @Override
    public final String toString() {
        return "(" + x + ")";
    }

}
