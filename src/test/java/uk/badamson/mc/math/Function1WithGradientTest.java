package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Unit tests for classes that implement the {@link Function1WithGradient}
 * interface.
 * </p>
 */
public class Function1WithGradientTest {

    public static void assertInvariants(Function1WithGradient f) {
        // Do nothing
    }

    public static void assertValueConsistentWithGradient(final Function1WithGradient f, final double x1,
            final double x2, final int n) {
        assert 3 <= n;
        final Function1WithGradientValue[] fx = new Function1WithGradientValue[n];
        for (int i = 0; i < n; ++i) {
            final double x = x1 + (x2 - x1) * (i) / (n);
            fx[i] = f.value(x);
        }
        for (int i = 1; i < n - 1; i++) {
            final Function1WithGradientValue fl = fx[i - 1];
            final Function1WithGradientValue fi = fx[i];
            final Function1WithGradientValue fr = fx[i + 1];
            final double dfl = fi.getF() - fl.getF();
            final double dfr = fr.getF() - fi.getF();
            assertTrue("Consistent gradient <" + fl + "," + fi + "," + fr + ">",
                    sign(dfl) != sign(dfr) || sign(fi.getDfDx()) == sign(dfl));
        }
    }

    private static int sign(double x) {
        if (x < -Double.MIN_NORMAL) {
            return -1;
        } else if (Double.MIN_NORMAL < x) {
            return 1;
        } else {
            return 0;
        }
    }

    public static Function1WithGradientValue value(Function1WithGradient f, double x) {
        final Function1WithGradientValue v = f.value(x);

        assertNotNull("Not null, result", v);// guard
        Function1WithGradientValueTest.assertInvariants(v);
        assertEquals("x", x, v.getX(), Double.MIN_NORMAL);

        return v;
    }
}
