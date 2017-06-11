package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the class {@link Point2}.
 * </p>
 */
public class Point2Test {

	public static void assertInvariants(Point2 point) {
		ObjectTest.assertInvariants(point);// inherited
	}

	public static void assertInvariants(Point2 point1, Point2 point2) {
		ObjectTest.assertInvariants(point1, point2);// inherited

		final boolean equals = point1.equals(point2);
		assertFalse("Value semantics, x",
				equals && Double.doubleToLongBits(point1.getX()) != Double.doubleToLongBits(point2.getX()));
		assertFalse("Value semantics, y",
				equals && Double.doubleToLongBits(point1.getY()) != Double.doubleToLongBits(point2.getY()));
	}

	private static Point2 constructor(double x, double y) {
		final Point2 point = new Point2(x, y);

		assertInvariants(point);
		assertEquals("x bits", Double.doubleToLongBits(x), Double.doubleToLongBits(point.getX()));
		assertEquals("y bits", Double.doubleToLongBits(y), Double.doubleToLongBits(point.getY()));
		return point;
	}

	private static void equals_equivalent(final double x, final double y) {
		final Point2 point1 = new Point2(x, y);
		final Point2 point2 = new Point2(x, y);

		assertInvariants(point1, point2);
		assertEquals("Equivalent", point1, point2);
	}

	@Test
	public void constructor_a() {
		constructor(0, 1);
	}

	@Test
	public void constructor_b() {
		constructor(-1, 2);
	}

	@Test
	public void constructor_nan() {
		constructor(Double.NaN, Double.POSITIVE_INFINITY);
	}

	@Test
	public void equals_equivalentA() {
		equals_equivalent(1.0, 2.0);
	}

	@Test
	public void equals_equivalentNan() {
		equals_equivalent(Double.NaN, Double.POSITIVE_INFINITY);
	}
}
