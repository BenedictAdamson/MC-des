package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
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
}
