package uk.badamson.mc.math;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Unit tests for classes that implement the {@link Point} interface.
 * </p>
 */
public class PointTest {

    public static void assertInvariants(Point point) {
        assertTrue("The number of dimensions is positive", 0 < point.getDimensions());
    }

    public static void assertInvariants(Point point1, Point point2) {
        final boolean equals = point1.equals(point2);
        final int dimensions1 = point1.getDimensions();
        assertFalse("Value semantics, dimensions", equals && dimensions1 != point2.getDimensions());
        for (int i = 0; i < dimensions1; ++i) {
            assertFalse("Value semantics, x[" + i + "]",
                    equals && Double.doubleToLongBits(point1.getX(i)) != Double.doubleToLongBits(point2.getX(i)));
        }
    }

}
