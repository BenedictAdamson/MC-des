package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * <p>
 * Unit tests of the class {@link ImmutableVector}.
 * </p>
 */
public class ImmutableVectorTest {

    public static void assertInvariants(ImmutableVector x) {
        ImmutableMatrixTest.assertInvariants(x);// inherited

        assertEquals("columns", 1, x.getColumns());
        final int dimensions = x.getDimension();
        assertTrue("The number of dimensions <" + dimensions + "> is positive", 0 < dimensions);
    }

    public static void assertInvariants(ImmutableVector x1, ImmutableVector x2) {
        ImmutableMatrixTest.assertInvariants(x1, x2);// inherited

        if (x1.equals(x2)) {
            final int dimensions1 = x1.getDimension();
            assertEquals("Equality requires equal dimensions", dimensions1, x2.getDimension());// guard
            for (int i = 0; i < dimensions1; i++) {
                assertEquals("Equality requires equal components [" + i + "]", x1.get(i), x2.get(i), Double.MIN_NORMAL);
            }
        }
    }

    private static void construct_equals(double x) {
        final ImmutableVector v1 = ImmutableVector.create(x);
        final ImmutableVector v2 = ImmutableVector.create(x);

        assertInvariants(v1, v2);
        assertEquals("Equivalent", v1, v2);
    }

    private static ImmutableVector create(double... x) {
        final ImmutableVector v = ImmutableVector.create(x);

        assertInvariants(v);
        assertEquals("dimension", x.length, v.getDimension());
        for (int i = 0; i < x.length; i++) {
            assertEquals("x[" + i + "]", x[i], v.get(i), Double.MIN_NORMAL);
        }

        return v;
    }

    private static ImmutableVector create0(int dimension) {
        final ImmutableVector zero = ImmutableVector.create0(dimension);

        assertNotNull("Always returns a vector", zero);// guard
        assertInvariants(zero);
        assertEquals("dimension", dimension, zero.getDimension());
        for (int i = 0; i < dimension; ++i) {
            assertEquals("The elements of the zero vector are all zero.", 0.0, zero.get(i), Double.MIN_NORMAL);
        }

        return zero;
    }

    private static ImmutableVector createOnLine(final ImmutableVector x0, ImmutableVector dx, double w) {
        final ImmutableVector x = ImmutableVector.createOnLine(x0, dx, w);

        assertNotNull("Always returns a (non null) vector", x);
        assertEquals("dimension", x0.getDimension(), x.getDimension());

        return x;
    }

    private static ImmutableVector mean(ImmutableVector x, ImmutableVector that) {
        final ImmutableVector mean = x.mean(that);

        assertNotNull("Not null, mean", mean);// guard
        assertInvariants(mean);
        assertInvariants(x, mean);
        assertInvariants(that, mean);
        assertEquals("dimension", x.getDimension(), mean.getDimension());

        return mean;
    }

    private static final ImmutableVector minus(ImmutableVector x) {
        final ImmutableVector minus = x.minus();

        assertNotNull("Not null, result", minus);// guard
        assertInvariants(minus);
        assertInvariants(x, minus);

        final int dimension = minus.getDimension();
        assertEquals("dimension", x.getDimension(), dimension);
        for (int i = 0; i < dimension; i++) {
            final double xI = x.get(i);
            final double minusI = minus.get(i);
            final boolean signed = Double.isInfinite(xI) || Double.isFinite(xI);
            assertTrue("minus[" + i + "] <" + xI + "," + minusI + ">",
                    !signed || Double.doubleToLongBits(-xI) == Double.doubleToLongBits(minus.get(i)));
        }

        return minus;
    }

    private static final ImmutableVector minus(ImmutableVector x, ImmutableVector that) {
        final ImmutableVector diff = x.minus(that);

        assertNotNull("Not null, result", diff);// guard
        assertInvariants(diff);
        assertInvariants(diff, x);
        assertInvariants(diff, that);

        final int dimension = diff.getDimension();
        assertEquals("dimension", x.getDimension(), dimension);// guard
        for (int i = 0; i < dimension; i++) {
            assertEquals("diff[" + i + "]", x.get(i) - that.get(i), diff.get(i), Double.MIN_NORMAL);
        }

        return diff;
    }

    private static final ImmutableVector plus(ImmutableVector x, ImmutableVector that) {
        final ImmutableVector diff = x.plus(that);

        assertNotNull("Not null, result", diff);// guard
        assertInvariants(diff);
        assertInvariants(diff, x);
        assertInvariants(diff, that);

        final int dimension = diff.getDimension();
        assertEquals("dimension", x.getDimension(), dimension);// guard
        for (int i = 0; i < dimension; i++) {
            assertEquals("plus[" + i + "]", x.get(i) + that.get(i), diff.get(i), Double.MIN_NORMAL);
        }

        return diff;
    }

    private static ImmutableVector scale(ImmutableVector x, double f) {
        final ImmutableVector scaled = x.scale(f);

        assertNotNull("Not null, result", scaled);
        assertInvariants(scaled);
        assertInvariants(x, scaled);
        assertEquals("dimension", x.getDimension(), scaled.getDimension());

        return scaled;
    }

    private static ImmutableVector sum(ImmutableVector... x) {
        final ImmutableVector sum = ImmutableVector.sum(x);

        assertNotNull("Always returns a sum vector.", sum);// guard
        assertInvariants(sum);
        for (ImmutableVector xi : x) {
            assertInvariants(sum, xi);
        }

        assertEquals("The dimension of the sum equals the dimension of the summed vectors.", x[0].getDimension(),
                sum.getDimension());

        return sum;
    }

    private static final void sum_multiple1(double x) {
        final ImmutableVector sum = sum(new ImmutableVector[] { ImmutableVector.create(x) });

        assertEquals("sum[0]", x, sum.get(0), Double.MIN_NORMAL);
    }

    private static final void sum_multiple2(double x1, double x2) {
        final ImmutableVector sum = sum(
                new ImmutableVector[] { ImmutableVector.create(x1), ImmutableVector.create(x2) });

        assertEquals("sum[0]", x1 + x2, sum.get(0), Double.MIN_NORMAL);
    }

    private static ImmutableVector weightedSum(double[] weight, ImmutableVector[] x) {
        final ImmutableVector sum = ImmutableVector.weightedSum(weight, x);

        assertNotNull("Always returns a sum vector.", sum);// guard
        assertInvariants(sum);
        for (ImmutableVector xi : x) {
            assertInvariants(sum, xi);
        }

        assertEquals("The dimension of the sum equals the dimension of the summed vectors.", x[0].getDimension(),
                sum.getDimension());

        return sum;
    }

    private static final void weightedSum_11(double weight, double x) {
        final ImmutableVector sum = weightedSum(new double[] { weight },
                new ImmutableVector[] { ImmutableVector.create(x) });

        assertEquals("sum[0]", weight * x, sum.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void construct_equals1A() {
        construct_equals(0.0);
    }

    @Test
    public void construct_equals1B() {
        construct_equals(1.0);
    }

    @Test
    public void construct_equals1C() {
        construct_equals(Double.POSITIVE_INFINITY);
    }

    @Test
    public void construct_equals2() {
        final double x1 = 0.0;
        final double x2 = 1.0;
        final ImmutableVector v1 = ImmutableVector.create(x1, x2);
        final ImmutableVector v2 = ImmutableVector.create(x1, x2);

        assertInvariants(v1, v2);
        assertEquals("Equivalent", v1, v2);
    }

    @Test
    public void construct_notEqualsA() {
        final ImmutableVector v1 = ImmutableVector.create(0.0);
        final ImmutableVector v2 = ImmutableVector.create(1.0);

        assertInvariants(v1, v2);
        assertNotEquals("Not equivalent", v1, v2);
    }

    @Test
    public void construct_notEqualsB() {
        final ImmutableVector v1 = ImmutableVector.create(0.0);
        final ImmutableVector v2 = ImmutableVector.create(0.0, 1.0);

        assertInvariants(v1, v2);
        assertNotEquals("Not equivalent", v1, v2);
    }

    @Test
    public void create_10() {
        final ImmutableVector x = create(0.0);

        assertEquals("magnitude^2", 0.0, x.magnitude2(), Double.MIN_NORMAL);
        assertEquals("magnitude", 0.0, x.magnitude(), Double.MIN_NORMAL);
    }

    @Test
    public void create_1B() {
        final ImmutableVector x = create(-1.0);

        assertEquals("magnitude^2", 1.0, x.magnitude2(), Double.MIN_NORMAL);
        assertEquals("magnitude", 1.0, x.magnitude(), Double.MIN_NORMAL);
    }

    @Test
    public void create_1Max() {
        final double xI = Double.MAX_VALUE;
        final ImmutableVector x = create(xI);

        assertEquals("magnitude", xI, x.magnitude(), xI * 1.0E-6);
    }

    @Test
    public void create_1Nan() {
        final ImmutableVector x = create(Double.POSITIVE_INFINITY);

        final double magnitude2 = x.magnitude2();
        final double magnitude = x.magnitude();
        assertEquals("magnitude^2 <" + magnitude2 + "> (bits)", Double.doubleToLongBits(magnitude2),
                Double.doubleToLongBits(Double.POSITIVE_INFINITY));
        assertEquals("magnitude <" + magnitude + "> (bits)", Double.doubleToLongBits(magnitude),
                Double.doubleToLongBits(Double.POSITIVE_INFINITY));
    }

    @Test
    public void create_2A() {
        final ImmutableVector x = create(0.0, 1.0);

        assertEquals("magnitude", 1.0, x.magnitude(), Double.MIN_NORMAL);
    }

    @Test
    public void create_2B() {
        final ImmutableVector x = create(1.0, 1.0);

        assertEquals("magnitude", Math.sqrt(2.0), x.magnitude(), Double.MIN_NORMAL);
    }

    @Test
    public void create01() {
        create0(1);
    }

    @Test
    public void create02() {
        create0(2);
    }

    @Test
    public void createOnLine_A() {
        final ImmutableVector x0 = ImmutableVector.create(0.0);
        final ImmutableVector dx = ImmutableVector.create(1.0);
        final double w = 1.0;

        final ImmutableVector x = createOnLine(x0, dx, w);

        assertEquals("x", 1.0, x.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void createOnLine_B() {
        final ImmutableVector x0 = ImmutableVector.create(1.0);
        final ImmutableVector dx = ImmutableVector.create(1.0);
        final double w = 1.0;

        final ImmutableVector x = createOnLine(x0, dx, w);

        assertEquals("x", 2.0, x.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void createOnLine_C() {
        final ImmutableVector x0 = ImmutableVector.create(0.0);
        final ImmutableVector dx = ImmutableVector.create(2.0);
        final double w = 1.0;

        final ImmutableVector x = createOnLine(x0, dx, w);

        assertEquals("x", 2.0, x.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void createOnLine_D() {
        final ImmutableVector x0 = ImmutableVector.create(0.0);
        final ImmutableVector dx = ImmutableVector.create(1.0);
        final double w = 2.0;

        final ImmutableVector x = createOnLine(x0, dx, w);

        assertEquals("x", 2.0, x.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void dot_A() {
        final double d = ImmutableVector.create(1.0).dot(ImmutableVector.create(1.0));
        assertEquals("dot product", 1.0, d, Double.MIN_NORMAL);
    }

    @Test
    public void dot_B() {
        final double d = ImmutableVector.create(2.0).dot(ImmutableVector.create(1.0));
        assertEquals("dot product", 2.0, d, Double.MIN_NORMAL);
    }

    @Test
    public void dot_C() {
        final double d = ImmutableVector.create(1.0).dot(ImmutableVector.create(2.0));
        assertEquals("dot product", 2.0, d, Double.MIN_NORMAL);
    }

    @Test
    public void dot_D() {
        final double d = ImmutableVector.create(2.0).dot(ImmutableVector.create(2.0));
        assertEquals("dot product", 4.0, d, Double.MIN_NORMAL);
    }

    @Test
    public void dot_E() {
        final double d = ImmutableVector.create(1.0, 1.0).dot(ImmutableVector.create(1.0, 1.0));
        assertEquals("dot product", 2.0, d, Double.MIN_NORMAL);
    }

    @Test
    public void mean_1A() {
        final ImmutableVector mean = mean(ImmutableVector.create(1.0), ImmutableVector.create(1.0));
        assertEquals("mean[0]", 1.0, mean.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void mean_1B() {
        final ImmutableVector mean = mean(ImmutableVector.create(1.0), ImmutableVector.create(-1.0));
        assertEquals("mean[0]", 0.0, mean.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void mean_1C() {
        final ImmutableVector mean = mean(ImmutableVector.create(2.0), ImmutableVector.create(0.0));
        assertEquals("mean[0]", 1.0, mean.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void mean_1D() {
        final ImmutableVector mean = mean(ImmutableVector.create(0.0), ImmutableVector.create(2.0));
        assertEquals("mean[0]", 1.0, mean.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void mean_2() {
        final ImmutableVector mean = mean(ImmutableVector.create(1.0, 2.0), ImmutableVector.create(3.0, 4.0));
        assertEquals("mean[0]", 2.0, mean.get(0), Double.MIN_NORMAL);
        assertEquals("mean[1]", 3.0, mean.get(1), Double.MIN_NORMAL);
    }

    @Test
    public void minus_0() {
        minus(ImmutableVector.create(0.0));
    }

    @Test
    public void minus_1A() {
        minus(ImmutableVector.create(1.0));
    }

    @Test
    public void minus_1B() {
        minus(ImmutableVector.create(-2.0));
    }

    @Test
    public void minus_1Infinity() {
        minus(ImmutableVector.create(Double.POSITIVE_INFINITY));
    }

    @Test
    public void minus_1Nan() {
        minus(ImmutableVector.create(Double.NaN));
    }

    @Test
    public void minus_2() {
        minus(ImmutableVector.create(1.0, 2.0));
    }

    @Test
    public void minus_vector0A() {
        final ImmutableVector x1 = ImmutableVector.create(0);
        final ImmutableVector x2 = ImmutableVector.create(0);

        minus(x1, x2);
    }

    @Test
    public void minus_vector0B() {
        final ImmutableVector x1 = ImmutableVector.create(1);
        final ImmutableVector x2 = ImmutableVector.create(0);

        minus(x1, x2);
    }

    @Test
    public void minus_vector0C() {
        final ImmutableVector x1 = ImmutableVector.create(1, 2);
        final ImmutableVector x2 = ImmutableVector.create(0, 0);

        minus(x1, x2);
    }

    @Test
    public void minus_vectorA() {
        final ImmutableVector x1 = ImmutableVector.create(2);
        final ImmutableVector x2 = ImmutableVector.create(1);

        minus(x1, x2);
    }

    @Test
    public void minus_vectorB() {
        final ImmutableVector x1 = ImmutableVector.create(2);
        final ImmutableVector x2 = ImmutableVector.create(-1);

        minus(x1, x2);
    }

    @Test
    public void minus_vectorC() {
        final ImmutableVector x1 = ImmutableVector.create(1, 2);
        final ImmutableVector x2 = ImmutableVector.create(3, 4);

        minus(x1, x2);
    }

    @Test
    public void plus_0A() {
        final ImmutableVector x1 = ImmutableVector.create(0);
        final ImmutableVector x2 = ImmutableVector.create(0);

        plus(x1, x2);
    }

    @Test
    public void plus_0B() {
        final ImmutableVector x1 = ImmutableVector.create(1);
        final ImmutableVector x2 = ImmutableVector.create(0);

        plus(x1, x2);
    }

    @Test
    public void plus_0C() {
        final ImmutableVector x1 = ImmutableVector.create(1, 2);
        final ImmutableVector x2 = ImmutableVector.create(0, 0);

        plus(x1, x2);
    }

    @Test
    public void plus_A() {
        final ImmutableVector x1 = ImmutableVector.create(2);
        final ImmutableVector x2 = ImmutableVector.create(1);

        plus(x1, x2);
    }

    @Test
    public void plus_B() {
        final ImmutableVector x1 = ImmutableVector.create(2);
        final ImmutableVector x2 = ImmutableVector.create(-1);

        plus(x1, x2);
    }

    @Test
    public void plus_C() {
        final ImmutableVector x1 = ImmutableVector.create(1, 2);
        final ImmutableVector x2 = ImmutableVector.create(3, 4);

        plus(x1, x2);
    }

    @Test
    public void scale_1A() {
        final ImmutableVector scaled = scale(ImmutableVector.create(1.0), 0.0);

        assertEquals("scaled[0]", 0.0, scaled.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void scale_1B() {
        final ImmutableVector scaled = scale(ImmutableVector.create(0.0), 1.0);

        assertEquals("scaled[0]", 0.0, scaled.get(0), Double.MIN_NORMAL);
    }

    //////////////////////////

    @Test
    public void scale_1C() {
        final ImmutableVector scaled = scale(ImmutableVector.create(1.0), 1.0);

        assertEquals("scaled[0]", 1.0, scaled.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void scale_1D() {
        final ImmutableVector scaled = scale(ImmutableVector.create(1.0), -2.0);

        assertEquals("scaled[0]", -2.0, scaled.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void scale_2() {
        final ImmutableVector scaled = scale(ImmutableVector.create(1.0, 2.0), 4.0);

        assertEquals("scaled[0]", 4.0, scaled.get(0), Double.MIN_NORMAL);
        assertEquals("scaled[1]", 8.0, scaled.get(1), Double.MIN_NORMAL);
    }

    @Test
    public void sum_multiple1A() {
        sum_multiple1(1.0);
    }

    @Test
    public void sum_multiple1B() {
        sum_multiple1(0.0);
    }

    @Test
    public void sum_multiple1C() {
        sum_multiple1(-1.0);
    }

    @Test
    public void sum_multiple1D() {
        final ImmutableVector sum = sum(new ImmutableVector[] { ImmutableVector.create(1.0, 3.0) });

        assertEquals("sum[0]", 1.0, sum.get(0), Double.MIN_NORMAL);
        assertEquals("sum[1]", 3.0, sum.get(1), Double.MIN_NORMAL);
    }

    @Test
    public void sum_multiple2A() {
        sum_multiple2(1.0, 0.0);
    }

    @Test
    public void sum_multiple2B() {
        sum_multiple2(0.0, 1.0);
    }

    @Test
    public void sum_multiple2C() {
        sum_multiple2(1.0, 1.0);
    }

    @Test
    public void sum_multiple2D() {
        sum_multiple2(1.0, 2.0);
    }

    @Test
    public void weightedSum_11A() {
        weightedSum_11(0.0, 1.0);
    }

    @Test
    public void weightedSum_11B() {
        weightedSum_11(1.0, 0.0);
    }

    @Test
    public void weightedSum_11C() {
        weightedSum_11(1.0, 1.0);
    }

    @Test
    public void weightedSum_11D() {
        weightedSum_11(2.0, 1.0);
    }

    @Test
    public void weightedSum_11E() {
        weightedSum_11(1.0, 2.0);
    }

    @Test
    public void weightedSum_12() {
        final ImmutableVector sum = weightedSum(new double[] { 2.0 },
                new ImmutableVector[] { ImmutableVector.create(1.0, 3.0) });

        assertEquals("sum[0]", 2.0, sum.get(0), Double.MIN_NORMAL);
        assertEquals("sum[1]", 6.0, sum.get(1), Double.MIN_NORMAL);
    }

    @Test
    public void weightedSum_21() {
        final ImmutableVector sum = weightedSum(new double[] { 1.0, 2.0 },
                new ImmutableVector[] { ImmutableVector.create(3.0), ImmutableVector.create(5.0) });

        assertEquals("sum[0]", 13.0, sum.get(0), Double.MIN_NORMAL);
    }

}
