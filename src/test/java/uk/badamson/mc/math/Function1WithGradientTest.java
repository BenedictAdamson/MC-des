package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

	public static Function1WithGradientValue value(Function1WithGradient f, double x) {
		final Function1WithGradientValue v = f.value(x);

		assertNotNull("Not null, result", v);// guard
		Function1WithGradientValueTest.assertInvariants(v);
		assertEquals("x", x, v.getX(), Double.MIN_NORMAL);

		return v;
	}
}
