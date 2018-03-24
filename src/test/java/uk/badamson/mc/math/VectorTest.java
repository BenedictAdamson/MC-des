package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Unit tests of classes that implement the {@link Vector} interface.
 * </p>
 */
public class VectorTest {

    public static void assertInvariants(Vector vector) {
        MatrixTest.assertInvariants(vector);// inherited
        assertEquals("columns", 1, vector.getColumns());
    }

    public static void assertInvariants(Vector vector1, Vector vector2) {
        MatrixTest.assertInvariants(vector1, vector2);// inherited
    }
}
