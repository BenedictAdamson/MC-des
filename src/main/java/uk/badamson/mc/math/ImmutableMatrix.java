package uk.badamson.mc.math;

import java.util.Arrays;
import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A constant (immutable) array of real numbers.
 * </p>
 */
@Immutable
public class ImmutableMatrix {

    /**
     * <p>
     * Create a matrix with given element values.
     * </p>
     * <ul>
     * <li>Always creates a (non null) matrix.</li>
     * <li>The created matrix has the given attribute values.</li>
     * </ul>
     * 
     * @param rows
     *            The number of rows of the matrix.
     * @param columns
     *            The number of columns of this matrix.
     * @param elements
     *            The values of the elements of the matrix; the elements are in
     *            <i>row-major</i> order, so {@code element[i*columns + j]} is the
     *            value of cardinal row <var>i</var>, cardinal column <var>j</var>.
     * @return the created matrix
     * 
     * @throws NullPointerException
     *             If {@code elements} is null.
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code rows} is not positive.</li>
     *             <li>If {@code columns} is not positive.</li>
     *             <li>If the length of {@code elements} is not equal to
     *             {@code rows} multiplied by {@code columns}.</li>
     *             </ul>
     */
    public static ImmutableMatrix create(int rows, int columns, double[] elements) {
        Objects.requireNonNull(elements, "elements");
        if (rows < 1) {
            throw new IllegalArgumentException("rows " + rows);
        }
        if (columns < 1) {
            throw new IllegalArgumentException("columns " + columns);
        }
        if (elements.length != rows * columns) {
            throw new IllegalArgumentException(
                    "Inconsistent rows " + rows + " columns " + columns + " elements.length " + elements.length);
        }
        if (columns == 1) {
            return ImmutableVector.create(elements);
        } else {
            return new ImmutableMatrix(rows, columns, Arrays.copyOf(elements, elements.length));
        }
    }

    private final int rows;
    private final int columns;
    protected final double[] elements;

    ImmutableMatrix(int rows, int columns, double[] elements) {
        this.rows = rows;
        this.columns = columns;
        this.elements = elements;
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to another object.
     * </p>
     * <p>
     * The {@link ImmutableMatrix} class has <i>value semantics</i>: this object is
     * equivalent to another if, and only if, the other object is also an
     * {@link ImmutableMatrix} and they have equivalent attribtues.
     * </p>
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ImmutableMatrix))
            return false;
        ImmutableMatrix other = (ImmutableMatrix) obj;
        return rows == other.rows && Arrays.equals(elements, other.elements);
    }

    /**
     * <p>
     * The value of an element of this matrix.</li>
     * 
     * @param i
     *            the cardinal number of the row of the element (0 for the first
     *            row, 1 for the second row, and so on).
     * @param j
     *            the cardinal number of the column of the element (0 for the first
     *            column, 1 for the solcumn row, and so on).
     * @return the value of the element
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code i} is negative.</li>
     *             <li>If {@code i} is greater than or equal to the number of
     *             {@linkplain #getRows() rows} of this matrix.</li>
     *             <li>If {@code j} is negative.</li>
     *             <li>If {@code j} is greater than or equal to the number of
     *             {@linkplain #getColumns() columns} of this matrix.</li>
     *             </ul>
     */
    public final double get(int i, int j) {
        if (i < 0 || rows <= i) {
            throw new IndexOutOfBoundsException("i " + i);
        }
        if (j < 0 || columns <= j) {
            throw new IndexOutOfBoundsException("j " + j);
        }
        return elements[i * columns + j];
    }

    /**
     * <p>
     * The number of columns of this matrix.
     * </p>
     *
     * @return the columns of rows; positive.
     */
    public final int getColumns() {
        return columns;
    }

    /**
     * <p>
     * The number of rows of this matrix.
     *
     * @return the number of rows; positive.
     */
    public final int getRows() {
        return rows;
    }

    @Override
    public final int hashCode() {
        final int prime = 37;
        int result = columns;
        result = prime * result + rows;
        result = prime * result + Arrays.hashCode(elements);
        return result;
    }

    /**
     * <p>
     * Calculate the result of multiplying a vector by this matrix.
     * </p>
     * <ul>
     * <li>Always returns a (non null) vector.</li>
     * <li>The {@linkplain ImmutableVector#getRows() number of rows} of the product
     * is equal to the number of rows of this matrix.</li>
     * </ul>
     * 
     * @param x
     *            The vector to multiply
     * @return the product of this and the given vector.
     * 
     * @throws NullPointerException
     *             If {@code x} is null.
     * @throws IllegalArgumentException
     *             If the {@linkplain ImmutableVector#getRows() number of rows} of
     *             {@code x} is not equal to the {@linkplain #getColumns() number of
     *             columns} of this.
     */
    public final ImmutableVector multiply(ImmutableVector x) {
        Objects.requireNonNull(x, "x");
        final int columns = getColumns();
        if (columns != x.getRows()) {
            throw new IllegalArgumentException("Inconsistent numbers of columns and rows");
        }
        final int n = getRows();
        final double[] ax = new double[n];
        for (int i = 0; i < n; ++i) {
            double dot = 0.0;
            final int j0 = i * columns;
            for (int j = 0; j < columns; ++j) {
                dot += elements[j0 + j] * x.elements[j];
            }
            ax[i] = dot;
        }
        return new ImmutableVector(ax);
    }

}
