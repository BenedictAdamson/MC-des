package uk.badamson.mc.math;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Unit tests for classes that implement the {@link FunctionNWithGradient}
 * interface.
 * </p>
 */
public class FunctionNWithGradientTest {

    public static void assertInvariants(FunctionNWithGradient f) {
        // Do nothing
    }

    public static void assertValueConsistentWithGradientAlongLine(final FunctionNWithGradient f, final double w1,
            final double w2, final int n, final ImmutableVectorN x0, final ImmutableVectorN dx) {
        final Function1WithGradient fLine = MinN.createLineFunction(f, x0, dx);
        Function1WithGradientTest.assertValueConsistentWithGradient(fLine, w1, w2, n);
    }

    public static FunctionNWithGradientValue value(FunctionNWithGradient f, ImmutableVectorN x) {
        final FunctionNWithGradientValue v = f.value(x);

        assertNotNull("Not null, result", v);// guard
        FunctionNWithGradientValueTest.assertInvariants(v);
        assertTrue("x <expected " + x + ", actual " + v.getX() + ">",
                x.minus(v.getX()).magnitude2() <= Double.MIN_NORMAL);

        return v;
    }
}
