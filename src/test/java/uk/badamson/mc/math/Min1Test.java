package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the class {@link Min1}.
 * </p>
 */
public class Min1Test {

	public static class BracketTest {

		public static void assertInvariants(Min1.Bracket bracket) {
			ObjectTest.assertInvariants(bracket);// inherited

			final Point2 left = bracket.getLeft();
			final Point2 inner = bracket.getInner();
			final Point2 right = bracket.getRight();

			assertNotNull("Not null, left", left);// guard
			assertNotNull("Not null, inner", inner);// guard
			assertNotNull("Not null, right", right);// guard

			Point2Test.assertInvariants(left);
			Point2Test.assertInvariants(inner);
			Point2Test.assertInvariants(right);

			final double innerX = inner.getX();
			final double innerY = inner.getY();
			assertTrue("The inner point " + inner + " is to the right of the leftmost point " + left,
					left.getX() < innerX);
			assertTrue("The inner point " + inner + " is below the leftmost point " + left + ".", innerY < left.getY());
			assertTrue("The rightmost point is to the right of the inner point.", innerX < right.getX());
			assertTrue("The rightmost point is above the inner point.", innerY < right.getY());
		}

		public static void assertInvariants(Min1.Bracket bracket1, Min1.Bracket bracket2) {
			ObjectTest.assertInvariants(bracket1, bracket2);// inherited
		}

		private static Min1.Bracket constructor(Point2 left, Point2 inner, Point2 right) {
			final Min1.Bracket bracket = new Min1.Bracket(left, inner, right);
			assertInvariants(bracket);
			assertSame("left", left, bracket.getLeft());
			assertSame("inner", inner, bracket.getInner());
			assertSame("right", right, bracket.getRight());
			return bracket;
		}

		@Test
		public void constructor_a() {
			constructor(POINT_1, POINT_2, POINT_5);
		}

		@Test
		public void constructor_b() {
			constructor(POINT_2, POINT_3, POINT_4);
		}
	}// class

	private static final Point2 POINT_1 = new Point2(1, 8);
	private static final Point2 POINT_2 = new Point2(2, 7);
	private static final Point2 POINT_3 = new Point2(4, 3);
	private static final Point2 POINT_4 = new Point2(5, 4);
	private static final Point2 POINT_5 = new Point2(6, 9);

	private static final Function1 SQUARED = new Function1() {

		@Override
		public double value(double x) {
			return x * x;
		}
	};

	private static final Function1 POWER_4 = new Function1() {

		@Override
		public double value(double x) {
			final double x2 = x * x;
			return x2 * x2;
		}
	};

	private static final Function1 ORDER_3 = new Function1() {

		@Override
		public double value(double x) {
			final double x2 = x * x;
			return x + x2 - x * x2;
		}
	};

	private static final Function1 NOT_SMOOTH = new Function1() {

		@Override
		public double value(double x) {
			double f = x * x;
			if (-1 < x) {
				f += x + 1;
			}
			return f;
		}
	};

	private static final Function1 COS = new Function1() {

		@Override
		public double value(double x) {
			return Math.cos(x);
		}
	};

	private static Min1.Bracket findBracket(final Function1 f, double x1, double x2)
			throws Min1.PoorlyConditionedFunctionException {
		final Min1.Bracket bracket = Min1.findBracket(f, x1, x2);

		assertNotNull("Not null, bracket", bracket);// guard
		BracketTest.assertInvariants(bracket);

		final Point2 left = bracket.getLeft();
		final Point2 inner = bracket.getInner();
		final Point2 right = bracket.getRight();

		assertEquals("left y", f.value(left.getX()), left.getY(), Double.MIN_NORMAL);
		assertEquals("inner y", f.value(inner.getX()), inner.getY(), Double.MIN_NORMAL);
		assertEquals("right y", f.value(right.getX()), right.getY(), Double.MIN_NORMAL);

		return bracket;
	}

	@Test
	public void findBracket_nearMaxA() {
		findBracket(COS, 0.0, 0.1);
	}

	@Test
	public void findBracket_nearMaxB() {
		findBracket(COS, -0.1, 0.1);
	}

	@Test
	public void findBracket_notSmoothA() {
		findBracket(NOT_SMOOTH, -3.0, -2.0);
	}

	@Test
	public void findBracket_notSmoothB() {
		findBracket(NOT_SMOOTH, -4.0, -3.0);
	}

	@Test
	public void findBracket_order3A() {
		try {
			findBracket(ORDER_3, -1.0, 0.0);
		} catch (Min1.PoorlyConditionedFunctionException e) {
			// Permitted
		}
	}

	@Test
	public void findBracket_order3B() {
		try {
			findBracket(ORDER_3, -1.6, 0.0);
		} catch (Min1.PoorlyConditionedFunctionException e) {
			// Permitted
		}
	}

	@Test
	public void findBracket_power4Left() {
		findBracket(POWER_4, -2.0, -1.0);
	}

	@Test
	public void findBracket_power4LeftFar() {
		findBracket(POWER_4, -1E9, -0.9E9);
	}

	@Test
	public void findBracket_power4LeftReversed() {
		findBracket(POWER_4, -1.0, -2.0);
	}

	@Test
	public void findBracket_power4Right() {
		findBracket(POWER_4, 1.0, 2.0);
	}

	@Test
	public void findBracket_power4RightFar() {
		findBracket(POWER_4, 0.9E9, 1E9);
	}

	@Test
	public void findBracket_power4RightReversed() {
		findBracket(POWER_4, 2.0, 1.0);
	}

	@Test
	public void findBracket_power4Span() {
		findBracket(POWER_4, -1.0, 1.0);
	}

	@Test
	public void findBracket_squaredLeft() {
		findBracket(SQUARED, -2.0, -1.0);
	}

	@Test
	public void findBracket_squaredLeftFar() {
		findBracket(SQUARED, -1E9, -0.9E9);
	}

	@Test
	public void findBracket_squaredLeftReversed() {
		findBracket(SQUARED, -1.0, -2.0);
	}

	@Test
	public void findBracket_squaredRight() {
		findBracket(SQUARED, 1.0, 2.0);
	}

	@Test
	public void findBracket_squaredRightFar() {
		findBracket(SQUARED, 0.9E9, 1E9);
	}

	@Test
	public void findBracket_squaredRightReversed() {
		findBracket(SQUARED, 2.0, 1.0);
	}

	@Test
	public void findBracket_squaredSpan() {
		findBracket(SQUARED, -1.0, 1.0);
	}

}
