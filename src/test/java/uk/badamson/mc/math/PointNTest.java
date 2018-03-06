package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the class {@link PointN}.
 * </p>
 */
public class PointNTest {

    public static void assertInvariants(PointN point) {
        ObjectTest.assertInvariants(point);// inherited
        PointTest.assertInvariants(point);// inherited
    }

    public static void assertInvariants(PointN point1, PointN point2) {
        ObjectTest.assertInvariants(point1, point2);// inherited
        PointTest.assertInvariants(point1, point2);// inherited
    }

    private static PointN create(double[] x) {
        final PointN point = PointN.create(x);

        assertInvariants(point);
        assertEquals("dimensions", x.length, point.getDimensions());
        for (int i = 0; i < x.length; ++i) {
            assertEquals("x[" + i + "]", Double.doubleToLongBits(x[i]), Double.doubleToLongBits(point.getX(i)));
        }
        return point;
    }

    private static void equals_equivalent(double[] x) {
        final double[] x2 = Arrays.copyOf(x, x.length);
        final PointN point1 = PointN.create(x);
        final PointN point2 = PointN.create(x2);

        assertInvariants(point1, point2);
        assertEquals("Equivalent", point1, point2);
    }

    @Test
    public void create_1A() {
        create(new double[] { 1.0 });
    }

    @Test
    public void create_1B() {
        create(new double[] { -2.0 });
    }

    @Test
    public void create_1Nan() {
        create(new double[] { Double.POSITIVE_INFINITY });
    }

    @Test
    public void create_2() {
        create(new double[] { 1.0, 2.0 });
    }

    @Test
    public void equals_equivalent1A() {
        equals_equivalent(new double[] { 1.0 });
    }

    @Test
    public void equals_equivalent1B() {
        equals_equivalent(new double[] { -2.0 });
    }

    @Test
    public void equals_equivalent1Nan() {
        equals_equivalent(new double[] { Double.POSITIVE_INFINITY });
    }

    @Test
    public void equals_equivalent2() {
        equals_equivalent(new double[] { 1.0, 2.0 });
    }

}
