package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests of classes that implement the {@link Matrix} interface.
 * </p>
 */
public class MatrixTest {

    public static void assertInvariants(Matrix matrix) {
        assertTrue("rows is positive", 0 < matrix.getRows());
        assertTrue("columns is positive", 0 < matrix.getColumns());
    }

    public static void assertInvariants(Matrix matrix1, Matrix matrix2) {
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

    public static final ImmutableVectorN multiply(Matrix a, ImmutableVectorN x) {
        final ImmutableVectorN ax = a.multiply(x);

        assertNotNull("Not null, result", ax);// guard
        ImmutableVectorNTest.assertInvariants(ax);
        assertInvariants(a, ax);
        ImmutableVectorNTest.assertInvariants(x, ax);

        assertEquals("The number of rows of the product is equal to the number of rows of this matrix.", a.getRows(),
                ax.getRows());

        return ax;
    }
}
