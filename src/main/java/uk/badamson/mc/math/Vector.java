package uk.badamson.mc.math;

/**
 * <p>
 * A column vector.
 * </p>
 */
public interface Vector extends Matrix {

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

}