package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests of the class {@link ImmutableVector}.
 * </p>
 */
public class ImmutableVectorTest {

	public static void assertInvariants(ImmutableVector x) {
		ObjectTest.assertInvariants(x);// inherited

		final int dimensions = x.getDimension();
		assertTrue("The number of dimensions <" + dimensions + "> is positive", 0 < dimensions);
	}

	public static void assertInvariants(ImmutableVector x1, ImmutableVector x2) {
		ObjectTest.assertInvariants(x1, x2);// inherited

		if (x1.equals(x2)) {
			final int dimensions1 = x1.getDimension();
			assertEquals("Equality requires equal dimensions", dimensions1, x2.getDimension());// guard
			for (int i = 0; i < dimensions1; i++) {
				assertEquals("Equality requires equal components [" + i + "]", x1.get(i), x2.get(i), Double.MIN_NORMAL);
			}
		}
	}

	private static void construct_equals(double x) {
		final ImmutableVector v1 = new ImmutableVector(x);
		final ImmutableVector v2 = new ImmutableVector(x);

		assertInvariants(v1, v2);
		assertEquals("Equivalent", v1, v2);
	}

	private static ImmutableVector constructor(double... x) {
		final ImmutableVector v = new ImmutableVector(x);

		assertInvariants(v);
		assertEquals("dimension", x.length, v.getDimension());
		for (int i = 0; i < x.length; i++) {
			assertEquals("x[" + i + "]", x[i], v.get(i), Double.MIN_NORMAL);
		}

		return v;
	}

	private static ImmutableVector createOnLine(final ImmutableVector x0, ImmutableVector dx, double w) {
		final ImmutableVector x = ImmutableVector.createOnLine(x0, dx, w);

		assertNotNull("Always returns a (non null) vector", x);
		assertEquals("dimension", x0.getDimension(), x.getDimension());

		return x;
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
		final ImmutableVector v1 = new ImmutableVector(x1, x2);
		final ImmutableVector v2 = new ImmutableVector(x1, x2);

		assertInvariants(v1, v2);
		assertEquals("Equivalent", v1, v2);
	}

	@Test
	public void construct_notEqualsA() {
		final ImmutableVector v1 = new ImmutableVector(0.0);
		final ImmutableVector v2 = new ImmutableVector(1.0);

		assertInvariants(v1, v2);
		assertNotEquals("Not equivalent", v1, v2);
	}

	@Test
	public void construct_notEqualsB() {
		final ImmutableVector v1 = new ImmutableVector(0.0);
		final ImmutableVector v2 = new ImmutableVector(0.0, 1.0);

		assertInvariants(v1, v2);
		assertNotEquals("Not equivalent", v1, v2);
	}

	@Test
	public void constructor_1A() {
		constructor(0.0);
	}

	@Test
	public void constructor_1B() {
		constructor(-1.0);
	}

	@Test
	public void constructor_1Nan() {
		constructor(Double.POSITIVE_INFINITY);
	}

	@Test
	public void constructor_2() {
		constructor(0.0, 1.0);
	}

	@Test
	public void createOnLine_A() {
		final ImmutableVector x0 = new ImmutableVector(0.0);
		final ImmutableVector dx = new ImmutableVector(1.0);
		final double w = 1.0;

		final ImmutableVector x = createOnLine(x0, dx, w);

		assertEquals("x", 1.0, x.get(0), Double.MIN_NORMAL);
	}

	@Test
	public void createOnLine_B() {
		final ImmutableVector x0 = new ImmutableVector(1.0);
		final ImmutableVector dx = new ImmutableVector(1.0);
		final double w = 1.0;

		final ImmutableVector x = createOnLine(x0, dx, w);

		assertEquals("x", 2.0, x.get(0), Double.MIN_NORMAL);
	}

	@Test
	public void createOnLine_C() {
		final ImmutableVector x0 = new ImmutableVector(0.0);
		final ImmutableVector dx = new ImmutableVector(2.0);
		final double w = 1.0;

		final ImmutableVector x = createOnLine(x0, dx, w);

		assertEquals("x", 2.0, x.get(0), Double.MIN_NORMAL);
	}

	@Test
	public void createOnLine_D() {
		final ImmutableVector x0 = new ImmutableVector(0.0);
		final ImmutableVector dx = new ImmutableVector(1.0);
		final double w = 2.0;

		final ImmutableVector x = createOnLine(x0, dx, w);

		assertEquals("x", 2.0, x.get(0), Double.MIN_NORMAL);
	}
}
