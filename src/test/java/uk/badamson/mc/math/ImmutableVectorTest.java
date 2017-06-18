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

	private static ImmutableVector createOnLine(final ImmutableVector x0, ImmutableVector dx, double w) {
		final ImmutableVector x = ImmutableVector.createOnLine(x0, dx, w);

		assertNotNull("Always returns a (non null) vector", x);
		assertEquals("dimension", x0.getDimension(), x.getDimension());

		return x;
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

		assertEquals("magnitude", 0.0, x.magnitude(), Double.MIN_NORMAL);
	}

	@Test
	public void create_1B() {
		final ImmutableVector x = create(-1.0);

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

		final double magnitude = x.magnitude();
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
}
