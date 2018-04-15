package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * <p>
 * Unit tests of the class {@link ImmutableVector1}.
 * </p>
 */
public class ImmutableVector1Test {

    public static void assertInvariants(ImmutableVector1 x) {
        VectorTest.assertInvariants(x);// inherited
    }

    public static void assertInvariants(ImmutableVector1 x1, ImmutableVector1 x2) {
        VectorTest.assertInvariants(x1, x2);// inherited
    }

    @Factory
    public static Matcher<Vector> closeTo(ImmutableVector1 operand, double tolerance) {
        return VectorTest.closeToVector(operand, tolerance);
    }

    private static ImmutableVector1 create(double x) {
        final ImmutableVector1 v = ImmutableVector1.create(x);

        assertNotNull("Not null, result", v);
        assertInvariants(v);

        assertEquals("x", Double.doubleToLongBits(x), Double.doubleToLongBits(v.get(0)));

        return v;
    }

    private static void create_2equals(double x) {
        final ImmutableVector1 v1 = ImmutableVector1.create(x);
        final ImmutableVector1 v2 = ImmutableVector1.create(x);

        assertInvariants(v1, v2);
        assertEquals("Equivalent", v1, v2);
    }

    private static ImmutableVector1 mean(ImmutableVector1 x, ImmutableVector1 that) {
        final ImmutableVector1 mean = (ImmutableVector1) VectorTest.mean(x, that);// inherited

        assertInvariants(mean);
        assertInvariants(x, mean);
        assertInvariants(that, mean);

        return mean;
    }

    public static ImmutableVector1 mean(ImmutableVector1 x, Vector that) {
        final ImmutableVector1 mean = (ImmutableVector1) VectorTest.mean(x, that);// inherited

        assertInvariants(mean);
        assertInvariants(x, mean);

        return mean;
    }

    public static final ImmutableVector1 minus(ImmutableVector1 x) {
        final ImmutableVector1 minus = (ImmutableVector1) VectorTest.minus(x);// inherited

        assertInvariants(minus);
        assertInvariants(x, minus);

        return minus;
    }

    private static final ImmutableVector1 minus(ImmutableVector1 x, ImmutableVector1 that) {
        final ImmutableVector1 diff = (ImmutableVector1) VectorTest.minus(x, that);// inherited

        assertInvariants(diff);
        assertInvariants(diff, x);
        assertInvariants(diff, that);
        return diff;
    }

    public static final ImmutableVector1 minus(ImmutableVector1 x, Vector that) {
        final ImmutableVector1 diff = (ImmutableVector1) VectorTest.minus(x, that);// inherited

        assertInvariants(diff);
        assertInvariants(diff, x);

        return diff;
    }

    private static final ImmutableVector1 plus(ImmutableVector1 x, ImmutableVector1 that) {
        final ImmutableVector1 sum = (ImmutableVector1) VectorTest.plus(x, that);// inherited

        assertInvariants(sum);
        assertInvariants(sum, x);
        assertInvariants(sum, that);

        return sum;
    }

    public static final ImmutableVector1 plus(ImmutableVector1 x, Vector that) {
        final ImmutableVector1 diff = (ImmutableVector1) VectorTest.plus(x, that);// inherited

        assertInvariants(diff);
        assertInvariants(diff, x);

        return diff;
    }

    public static ImmutableVector1 scale(ImmutableVector1 x, double f) {
        final ImmutableVector1 scaled = (ImmutableVector1) VectorTest.scale(x, f);// inherited

        assertInvariants(scaled);
        assertInvariants(x, scaled);

        return scaled;
    }

    private static ImmutableVector1 sum(ImmutableVector1... x) {
        final ImmutableVector1 sum = ImmutableVector1.sum(x);

        assertNotNull("Always returns a sum vector.", sum);// guard
        assertInvariants(sum);
        for (ImmutableVector1 xi : x) {
            assertInvariants(sum, xi);
        }

        assertEquals("The dimension of the sum equals the dimension of the summed vectors.", x[0].getDimension(),
                sum.getDimension());

        return sum;
    }

    private static final void sum_multiple1(double x) {
        final ImmutableVector1 sum = sum(new ImmutableVector1[] { ImmutableVector1.create(x) });

        assertEquals("sum x", x, sum.get(0), Double.MIN_NORMAL);
    }

    private static final void sum_multiple2(double x1, double x2) {
        final ImmutableVector1 sum = sum(
                new ImmutableVector1[] { ImmutableVector1.create(x1), ImmutableVector1.create(x2) });

        assertEquals("sum x", x1 + x2, sum.get(0), Double.MIN_NORMAL);
    }

    private static ImmutableVector1 weightedSum(double[] weight, ImmutableVector1[] x) {
        final ImmutableVector1 sum = ImmutableVector1.weightedSum(weight, x);

        assertNotNull("Always returns a sum vector.", sum);// guard
        assertInvariants(sum);
        for (ImmutableVector1 xi : x) {
            assertInvariants(sum, xi);
        }

        return sum;
    }

    private static final void weightedSum_1(double weight, double x) {
        final ImmutableVector1 sum = weightedSum(new double[] { weight },
                new ImmutableVector1[] { ImmutableVector1.create(x) });

        assertEquals("sum x", weight * x, sum.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void create_0() {
        final ImmutableVector1 x = create(0.0);

        assertEquals("magnitude^2", 0.0, x.magnitude2(), Double.MIN_NORMAL);
        assertEquals("magnitude", 0.0, x.magnitude(), Double.MIN_NORMAL);
    }

    @Test
    public void create_2equalsA() {
        create_2equals(0.0);
    }

    @Test
    public void create_2equalsB() {
        create_2equals(1.0);
    }

    @Test
    public void create_2equalsC() {
        create_2equals(Double.POSITIVE_INFINITY);
    }

    @Test
    public void create_2notEquals() {
        final ImmutableVector1 v1 = ImmutableVector1.create(0.0);
        final ImmutableVector1 v2 = ImmutableVector1.create(1.0);

        assertInvariants(v1, v2);
        assertNotEquals("Not equivalent", v1, v2);
    }

    @Test
    public void create_max() {
        final double max = Double.MAX_VALUE;
        final ImmutableVector1 x = create(max);

        assertEquals("magnitude", max, x.magnitude(), max * 1.0E-6);
    }

    @Test
    public void create_Nan() {
        final ImmutableVector1 x = create(Double.POSITIVE_INFINITY);

        final double magnitude2 = x.magnitude2();
        final double magnitude = x.magnitude();
        assertEquals("magnitude^2 <" + magnitude2 + "> (bits)", Double.doubleToLongBits(magnitude2),
                Double.doubleToLongBits(Double.POSITIVE_INFINITY));
        assertEquals("magnitude <" + magnitude + "> (bits)", Double.doubleToLongBits(magnitude),
                Double.doubleToLongBits(Double.POSITIVE_INFINITY));
    }

    @Test
    public void create_negative() {
        final ImmutableVector1 x = create(-1.0);

        assertEquals("magnitude^2", 1.0, x.magnitude2(), Double.MIN_NORMAL);
        assertEquals("magnitude", 1.0, x.magnitude(), Double.MIN_NORMAL);
    }

    @Test
    public void dot_x11() {
        final double d = ImmutableVector1.create(1).dot(ImmutableVector1.create(1));
        assertEquals("dot product", 1.0, d, Double.MIN_NORMAL);
    }

    @Test
    public void dot_x12() {
        final double d = ImmutableVector1.create(1).dot(ImmutableVector1.create(2));
        assertEquals("dot product", 2.0, d, Double.MIN_NORMAL);
    }

    @Test
    public void dot_x21() {
        final double d = ImmutableVector1.create(2).dot(ImmutableVector1.create(1));
        assertEquals("dot product", 2.0, d, Double.MIN_NORMAL);
    }

    @Test
    public void dot_x22() {
        final double d = ImmutableVector1.create(2).dot(ImmutableVector1.create(2));
        assertEquals("dot product", 4.0, d, Double.MIN_NORMAL);
    }

    @Test
    public void mean_x02() {
        final ImmutableVector1 mean = mean(ImmutableVector1.create(0), ImmutableVector1.create(2));
        assertEquals("mean[0]", 1.0, mean.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void mean_x11() {
        final ImmutableVector1 mean = mean(ImmutableVector1.create(1), ImmutableVector1.create(1));
        assertEquals("mean[0]", 1.0, mean.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void mean_x1m1() {
        final ImmutableVector1 mean = mean(ImmutableVector1.create(1), ImmutableVector1.create(-1));
        assertEquals("mean[0]", 0.0, mean.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void mean_x20() {
        final ImmutableVector1 mean = mean(ImmutableVector1.create(2), ImmutableVector1.create(0));
        assertEquals("mean[0]", 1.0, mean.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void minus_0() {
        minus(ImmutableVector1.create(0));
    }

    @Test
    public void minus_1() {
        minus(ImmutableVector1.create(1));
    }

    @Test
    public void minus_infinity() {
        minus(ImmutableVector1.create(Double.POSITIVE_INFINITY));
    }

    @Test
    public void minus_m() {
        minus(ImmutableVector1.create(-1));
    }

    @Test
    public void minus_nan() {
        minus(ImmutableVector1.create(Double.NaN));
    }

    @Test
    public void minus_vector00() {
        final ImmutableVector1 x1 = ImmutableVector1.create(0);
        final ImmutableVector1 x2 = ImmutableVector1.create(0);

        minus(x1, x2);
    }

    @Test
    public void minus_vector01() {
        final ImmutableVector1 x1 = ImmutableVector1.create(0);
        final ImmutableVector1 x2 = ImmutableVector1.create(1);

        minus(x1, x2);
    }

    @Test
    public void minus_vector10() {
        final ImmutableVector1 x1 = ImmutableVector1.create(1);
        final ImmutableVector1 x2 = ImmutableVector1.create(0);

        minus(x1, x2);
    }

    @Test
    public void minus_vectorA() {
        final ImmutableVector1 x1 = ImmutableVector1.create(1);
        final ImmutableVector1 x2 = ImmutableVector1.create(2);

        minus(x1, x2);
    }

    @Test
    public void plus_00() {
        final ImmutableVector1 x1 = ImmutableVector1.create(0);
        final ImmutableVector1 x2 = ImmutableVector1.create(0);

        plus(x1, x2);
    }

    @Test
    public void plus_01() {
        final ImmutableVector1 x1 = ImmutableVector1.create(0);
        final ImmutableVector1 x2 = ImmutableVector1.create(1);

        plus(x1, x2);
    }

    @Test
    public void plus_0m1() {
        final ImmutableVector1 x1 = ImmutableVector1.create(0);
        final ImmutableVector1 x2 = ImmutableVector1.create(-1);

        plus(x1, x2);
    }

    @Test
    public void plus_10() {
        final ImmutableVector1 x1 = ImmutableVector1.create(1);
        final ImmutableVector1 x2 = ImmutableVector1.create(0);

        plus(x1, x2);
    }

    @Test
    public void plus_A() {
        final ImmutableVector1 x1 = ImmutableVector1.create(1);
        final ImmutableVector1 x2 = ImmutableVector1.create(2);

        plus(x1, x2);
    }

    @Test
    public void plus_C() {
        final ImmutableVector1 x1 = ImmutableVector1.create(1);
        final ImmutableVector1 x2 = ImmutableVector1.create(3);

        plus(x1, x2);
    }

    @Test
    public void plus_m10() {
        final ImmutableVector1 x1 = ImmutableVector1.create(-1);
        final ImmutableVector1 x2 = ImmutableVector1.create(0);

        plus(x1, x2);
    }

    @Test
    public void scale_01() {
        final ImmutableVector1 scaled = scale(ImmutableVector1.create(0), 1.0);

        assertEquals("scaled[0]", 0.0, scaled.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void scale_10() {
        final ImmutableVector1 scaled = scale(ImmutableVector1.create(1), 0.0);

        assertEquals("scaled[0]", 0.0, scaled.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void scale_11() {
        final ImmutableVector1 scaled = scale(ImmutableVector1.create(1), 1.0);

        assertEquals("scaled[0]", 1.0, scaled.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void scale_1m2() {
        final ImmutableVector1 scaled = scale(ImmutableVector1.create(1), -2.0);

        assertEquals("scaled[0]", -2.0, scaled.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void scale_A() {
        final ImmutableVector1 scaled = scale(ImmutableVector1.create(1), 4.0);

        assertEquals("scaled[0]", 4.0, scaled.get(0), Double.MIN_NORMAL);
    }

    @Test
    public void sum_multiple1A() {
        sum_multiple1(1);
    }

    @Test
    public void sum_multiple1B() {
        sum_multiple1(7);
    }

    @Test
    public void sum_multiple2A() {
        sum_multiple2(1, 2);
    }

    @Test
    public void sum_multiple2B() {
        sum_multiple2(1, -2);
    }

    @Test
    public void sum_multiple2C() {
        sum_multiple2(-1, 2);
    }

    @Test
    public void weightedSum_1A() {
        weightedSum_1(1, 1);
    }

    @Test
    public void weightedSum_1B() {
        weightedSum_1(2, 1);
    }

    @Test
    public void weightedSum_1C() {
        weightedSum_1(1, 7);
    }

    @Test
    public void weightedSum_2() {
        final ImmutableVector1 sum = weightedSum(new double[] { 1.0, 2.0 },
                new ImmutableVector1[] { ImmutableVector1.create(3), ImmutableVector1.create(5) });

        assertEquals("sum[0]", 13.0, sum.get(0), Double.MIN_NORMAL);
    }
}
