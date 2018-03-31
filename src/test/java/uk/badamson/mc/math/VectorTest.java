package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * <p>
 * Unit tests of classes that implement the {@link Vector} interface.
 * </p>
 */
public class VectorTest {

    private static class IsCloseTo extends TypeSafeMatcher<Vector> {
        private final double tolerance;
        private final Vector value;

        private IsCloseTo(Vector value, double tolerance) {
            this.tolerance = tolerance;
            this.value = value;
        }

        @Override
        public void describeMismatchSafely(Vector item, Description mismatchDescription) {
            mismatchDescription.appendValue(item).appendText(" differed by ").appendValue(distance(item));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a vector within ").appendValue(tolerance).appendText(" of ").appendValue(value);
        }

        private final double distance(Vector item) {
            return value.minus(item).magnitude();
        }

        @Override
        public boolean matchesSafely(Vector item) {
            return distance(item) <= tolerance;
        }
    }

    public static void assertInvariants(Vector vector) {
        MatrixTest.assertInvariants(vector);// inherited
        assertEquals("columns", 1, vector.getColumns());
        assertEquals("The number of dimensions equals the number of rows.", vector.getRows(), vector.getDimension());
    }

    public static void assertInvariants(Vector vector1, Vector vector2) {
        MatrixTest.assertInvariants(vector1, vector2);// inherited
    }

    @Factory
    public static Matcher<Vector> closeTo(Vector operand, double tolerance) {
        return new IsCloseTo(operand, tolerance);
    }

    public static Vector mean(Vector x, Vector that) {
        final Vector mean = x.mean(that);

        assertNotNull("Not null, mean", mean);// guard
        assertInvariants(mean);
        assertInvariants(x, mean);
        assertInvariants(that, mean);
        assertEquals("dimension", x.getDimension(), mean.getDimension());

        return mean;
    }

    public static final Vector minus(Vector x) {
        final Vector minus = x.minus();

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

    public static final Vector minus(Vector x, Vector that) {
        final Vector diff = x.minus(that);

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

    public static final Vector multiply(Vector a, Vector x) {
        final Vector ax = MatrixTest.multiply(a, x);// inherited

        assertInvariants(a);// check for side-efffects
        assertInvariants(a, ax);

        return ax;
    }

    public static final Vector plus(Vector x, Vector that) {
        final Vector sum = x.plus(that);

        assertNotNull("Not null, result", sum);// guard
        assertInvariants(sum);
        assertInvariants(sum, x);
        assertInvariants(sum, that);

        final int dimension = sum.getDimension();
        assertEquals("dimension", x.getDimension(), dimension);// guard
        for (int i = 0; i < dimension; i++) {
            assertEquals("plus[" + i + "]", x.get(i) + that.get(i), sum.get(i), Double.MIN_NORMAL);
        }

        return sum;
    }

    public static Vector scale(Vector x, double f) {
        final Vector scaled = x.scale(f);

        assertNotNull("Not null, result", scaled);
        assertInvariants(scaled);
        assertInvariants(x, scaled);
        assertEquals("dimension", x.getDimension(), scaled.getDimension());

        return scaled;
    }

}
