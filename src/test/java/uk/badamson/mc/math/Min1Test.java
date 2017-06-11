package uk.badamson.mc.math;

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
			assertTrue("The inner point is to the right of the leftmost point", left.getX() < innerX);
			assertTrue("The inner point is below the leftmost point.", innerY < left.getY());
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
}
