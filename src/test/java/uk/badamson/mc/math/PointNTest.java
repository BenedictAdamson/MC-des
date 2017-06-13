package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

		assertTrue("The number of dimensions is positive", 0 < point.getDimensions());
	}

	public static void assertInvariants(PointN point1, PointN point2) {
		ObjectTest.assertInvariants(point1, point2);// inherited

		final boolean equals = point1.equals(point2);
		final int dimensions1 = point1.getDimensions();
		assertFalse("Value semantics, dimensions", equals && dimensions1 != point2.getDimensions());
		for (int i = 0; i < dimensions1; ++i) {
			assertFalse("Value semantics, x[" + i + "]",
					equals && Double.doubleToLongBits(point1.getX(i)) != Double.doubleToLongBits(point2.getX(i)));
		}
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
