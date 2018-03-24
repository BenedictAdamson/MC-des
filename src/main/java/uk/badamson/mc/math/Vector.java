package uk.badamson.mc.math;

/**
 * <p>
 * A column vector.
 * </p>
 */
public interface Vector extends Matrix {

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
     *             If the {@linkplain #getDimension() dimension} of {@code that} is
     *             not equal to the dimension of this.
     */
    public double dot(Vector that);

    /**
     * <p>
     * The value of an element of this vector.
     * </p>
     * 
     * @param i
     *            the cardinal number of the row of the element (0 for the first
     *            row, 1 for the second row, and so on).
     * @return the value of the element
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code i} is negative.</li>
     *             <li>If {@code i} is greater than or equal to the number of
     *             {@linkplain #getRows() rows} of this vector.</li>
     *             </ul>
     */
    public double get(int i);

    /**
     * {@inheritDoc}
     *
     * <ul>
     * <li>The number of columns of a vector is always 1.</li>
     * </ul>
     */
    @Override
    public int getColumns();

    /**
     * <p>
     * The number of dimensions of this vector.
     * </p>
     * <ul>
     * <li>The number of dimensions is positive.</li>
     * <li>The number of dimensions equals the {@linkplain #getRows() number of
     * rows}.</li>
     * </ul>
     * 
     * @return the number of dimensions
     */
    public int getDimension();

    /**
     * <p>
     * The magnitude of this vector.
     * </p>
     * <ul>
     * <li>The magnitude of this vector is the square root of the dot product of
     * this vector with itself.</li>
     * </ul>
     * 
     * @return the magnitude
     */
    public double magnitude();

    /**
     * <p>
     * The square of the magnitude of this vector.
     * </p>
     * <p>
     * The method takes care to properly handle vectors with components that are
     * large, not numbers, or which differ greatly in magnitude. It is otherwise
     * similar to the {@linkplain #dot(Vector) dot product} of the vector with
     * itself.
     * </p>
     * 
     * @return the square of the magnitude.
     */
    public double magnitude2();

    /**
     * <p>
     * Create the vector that is the mean of this vector with another vector.
     * </p>
     * <ul>
     * <li>Always returns a (non null) vector.</li>
     * <li>The {@linkplain #getDimension() dimension} of the mean vector is equal to
     * the dimension of this vector.</li>
     * </ul>
     * 
     * @param that
     *            The vector to take the mean with
     * @return the mean vector
     * 
     * @throws NullPointerException
     *             If {@code that} is null.
     * @throws IllegalArgumentException
     *             If the {@linkplain ImmutableVector3#getDimension() dimension} of
     *             }@code that} is not equal to the dimension of this vector.
     */
    public Vector mean(Vector that);

    /**
     * <p>
     * Create the vector that is opposite in direction of this vector.
     * </p>
     * <ul>
     * <li>Always returns a (non null) vector.</li>
     * <li>The opposite vector has the same {@linkplain #getDimension() dimension}
     * as this vector.</li>
     * <li>The {@linkplain #get(int) components} of the opposite vector are the
     * negative of the corresponding component of this vector.</li>
     * </ul>
     * 
     * @return the opposite vector.
     */
    public Vector minus();

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
    public Vector minus(Vector that);

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
     * @see #sum(ImmutableVector3...)
     */
    public Vector plus(Vector that);

    /**
     * <p>
     * Create a vector that is this vector scaled by a given scalar.
     * </p>
     * <ul>
     * <li>Always returns a (non null) vector.
     * <li>
     * <li>The {@linkplain ImmutableVector3#getDimension() dimension} of the scaled
     * vector is equal to the dimension of this vector.</li>
     * </ul>
     * 
     * @param f
     *            the scalar
     * @return the scaled vector
     */
    public Vector scale(double f);

}