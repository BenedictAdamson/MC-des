package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests of the class {@link ImmutableMatrix}
 * </p>
 */
public class ImmutableMatrixTest {

	public static void assertInvariants(ImmutableMatrix matrix) {
		ObjectTest.assertInvariants(matrix);// inherited

		assertTrue("rows is positive", 0 < matrix.getRows());
		assertTrue("columns is positive", 0 < matrix.getColumns());
	}

	public static void assertInvariants(ImmutableMatrix matrix1, ImmutableMatrix matrix2) {
		ObjectTest.assertInvariants(matrix1, matrix2);// inherited

		if (matrix1.equals(matrix2)) {
			final int rows1 = matrix1.getRows();
			final int columns1 = matrix1.getColumns();
			assertEquals("Equality requires equal rows", rows1, matrix2.getRows());// guard
			assertEquals("Equality requires equal columns", columns1, matrix2.getColumns());// guard
			for (int i = 0; i < rows1; i++) {
				for (int j = 0; j < columns1; j++) {
					assertEquals("Equality requires equal components [" + i + "," + j + "]", matrix1.get(i, j),
							matrix2.get(i, j), Double.MIN_NORMAL);
				}
			}
		}
	}

	private static ImmutableMatrix create(int rows, int columns, double[] elements) {
		final ImmutableMatrix matrix = ImmutableMatrix.create(rows, columns, elements);

		assertNotNull("Always creates a matrix", matrix);// guard
		assertInvariants(matrix);
		assertEquals("rows", rows, matrix.getRows());
		assertEquals("columns", columns, matrix.getColumns());
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				assertEquals("element [" + i + "," + j + "] bits", Double.doubleToLongBits(elements[i * columns + j]),
						Double.doubleToLongBits(matrix.get(i, j)));
			}
		}
		return matrix;
	}

	private static void create_1x1(double x) {
		create(1, 1, new double[] { x });
	}

	private static void create_equals(int rows, int columns, double[] elements) {
		final ImmutableMatrix matrix1 = ImmutableMatrix.create(rows, columns, elements);
		final ImmutableMatrix matrix2 = ImmutableMatrix.create(rows, columns, elements);

		assertInvariants(matrix1, matrix2);
		assertEquals("Equivalent", matrix1, matrix2);
	}

	private static void create_equals_1x1(double x) {
		create_equals(1, 1, new double[] { x });
	}

	private static void create_equals_2x1(double x11, double x21) {
		create_equals(2, 1, new double[] { x11, x21 });
	}

	private static void create_notEquals_1x1(double x1, double x2) {
		final ImmutableMatrix matrix1 = ImmutableMatrix.create(1, 1, new double[] { x1 });
		final ImmutableMatrix matrix2 = ImmutableMatrix.create(1, 1, new double[] { x2 });

		assertInvariants(matrix1, matrix2);
		assertNotEquals("Not equivalent", matrix1, matrix2);
	}

	@Test
	public void create_1x1_0() {
		create_1x1(0.0);
	}

	@Test
	public void create_1x1_1() {
		create_1x1(1.0);
	}

	@Test
	public void create_1x1_nan() {
		create_1x1(Double.NaN);
	}

	@Test
	public void create_2x3() {
		create(2, 3, new double[] { 1, 2, 3, 4, 5, 6 });
	}

	@Test
	public void create_equals_1x1_0() {
		create_equals_1x1(0.0);
	}

	@Test
	public void create_equals_1x1_1() {
		create_equals_1x1(1.0);
	}

	@Test
	public void create_equals_1x1_nan() {
		create_equals_1x1(Double.NaN);
	}

	@Test
	public void create_equals_2x1_A() {
		create_equals_2x1(0.0, 1.0);
	}

	@Test
	public void create_equals_2x1_B() {
		create_equals_2x1(1.0, 4.0);
	}

	@Test
	public void create_notEquals_1x1_A() {
		create_notEquals_1x1(1.0, 2.0);
	}

	@Test
	public void create_notEquals_1x1_B() {
		create_notEquals_1x1(3.0, 5.0);
	}

	/**
	 * Tough test: same elements arrays used for construction.
	 */
	@Test
	public void create_notEquals_C() {
		final double[] elements = new double[] { 1.0, 2.1 };
		final ImmutableMatrix matrix1 = ImmutableMatrix.create(2, 1, elements);
		final ImmutableMatrix matrix2 = ImmutableMatrix.create(1, 2, elements);

		assertInvariants(matrix1, matrix2);
		assertNotEquals("Not equivalent", matrix1, matrix2);
	}
}
