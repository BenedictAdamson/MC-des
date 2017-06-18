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

	public static FunctionNWithGradientValue value(FunctionNWithGradient f, ImmutableVector x) {
		final FunctionNWithGradientValue v = f.value(x);

		assertNotNull("Not null, result", v);// guard
		FunctionNWithGradientValueTest.assertInvariants(v);
		assertTrue("x", x.minus(v.getX()).magnitude2() <= Double.MIN_NORMAL);

		return v;
	}
}
