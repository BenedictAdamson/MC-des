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
public final class ImmutableMatrix {

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
	 *            <i>row-major</i> order, so {@code element[i*columns + j]} is
	 *            the value of cardinal row <var>i</var>, cardinal column
	 *            <var>j</var>.
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
		return new ImmutableMatrix(rows, columns, Arrays.copyOf(elements, elements.length));
	}
	private final int rows;
	private final int columns;

	private final double[] elements;

	private ImmutableMatrix(int rows, int columns, double[] elements) {
		this.rows = rows;
		this.columns = columns;
		this.elements = elements;
	}

	/**
	 * <p>
	 * The value of an element of this matrix.</li>
	 * 
	 * @param i
	 *            the cardinal number of the row of the element (0 for the first
	 *            row, 1 for the second row, and so on).
	 * @param j
	 *            the cardinal number of the column of the element (0 for the
	 *            first column, 1 for the solcumn row, and so on).
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
}
