package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.math.Min1.Bracket;

/**
 * <p>
 * Unit tests for the class {@link Min1}.
 * </p>
 */
public class Min1Test {

    public static class BracketTest {

	public static void assertInvariants(Min1.Bracket bracket) {
	    ObjectTest.assertInvariants(bracket);// inherited

	    final Function1Value left = bracket.getLeft();
	    final Function1Value inner = bracket.getInner();
	    final Function1Value right = bracket.getRight();
	    final double width = bracket.getWidth();

	    assertNotNull("Not null, left", left);// guard
	    assertNotNull("Not null, inner", inner);// guard
	    assertNotNull("Not null, right", right);// guard

	    Function1ValueTest.assertInvariants(left);
	    Function1ValueTest.assertInvariants(inner);
	    Function1ValueTest.assertInvariants(right);

	    final double innerX = inner.getX();
	    final double innerY = inner.getF();
	    assertTrue("The inner point " + inner + " is to the right of the leftmost point " + left,
		    left.getX() < innerX);
	    assertTrue("The inner point " + inner + " is below the leftmost point " + left + ".", innerY < left.getF());
	    assertTrue("The rightmost point is to the right of the inner point.", innerX < right.getX());
	    assertTrue("The rightmost point is above the inner point.", innerY < right.getF());

	    assertEquals(
		    "The smallest function value of the points constituting this bracket is the y value of the inner point of the bracket",
		    inner.getF(), bracket.getMin(), Double.MIN_NORMAL);
	    assertTrue("The width of a bracket <" + width + "> is always positive.", 0.0 < width);
	    assertEquals("width", right.getX() - left.getX(), width, Double.MIN_NORMAL);
	}

	public static void assertInvariants(Min1.Bracket bracket1, Min1.Bracket bracket2) {
	    ObjectTest.assertInvariants(bracket1, bracket2);// inherited
	}

	private static Min1.Bracket constructor(Function1Value left, Function1Value inner, Function1Value right) {
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

    private static final Function1Value POINT_1 = new Function1Value(1, 8);
    private static final Function1Value POINT_2 = new Function1Value(2, 7);
    private static final Function1Value POINT_3 = new Function1Value(4, 3);
    private static final Function1Value POINT_4 = new Function1Value(5, 4);
    private static final Function1Value POINT_5 = new Function1Value(6, 9);

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

    private static final Function1WithGradient SQUARED_WITH_GRADIENT = new Function1WithGradient() {

	@Override
	public Function1WithGradientValue value(double x) {
	    return new Function1WithGradientValue(x, x * x, 2.0 * x);
	}
    };

    private static final Function1WithGradient POWER_4_WITH_GRADIENT = new Function1WithGradient() {

	@Override
	public Function1WithGradientValue value(double x) {
	    final double x2 = x * x;
	    return new Function1WithGradientValue(x, x2 * x2, 4.0 * x2 * x);
	}
    };

    private static void assertConsistent(final Min1.Bracket bracket, final Function1 f) {
	assertConsistent("Left point of bracket", bracket.getLeft(), f);
	assertConsistent("Inner point of bracket", bracket.getInner(), f);
	assertConsistent("Right point of bracket", bracket.getRight(), f);
    }

    private static void assertConsistent(String message, final Function1Value p, final Function1 f) {
	assertEquals(message + " <" + p + "> is consistent with function <" + f + ">", f.value(p.getX()), p.getF(),
		Double.MIN_NORMAL);
    }

    private static void assertConsistent(String message, final Function1WithGradientValue p,
	    final Function1WithGradient f) {
	final Function1WithGradientValue fp = f.value(p.getX());
	assertEquals(message + " <" + p + "> is consistent with function <" + f + ">, codomain", fp.getF(), p.getF(),
		Double.MIN_NORMAL);
	assertEquals(message + " <" + p + "> is consistent with function <" + f + ">, gradient", fp.getDfDx(),
		p.getDfDx(), Double.MIN_NORMAL);
    }

    private static Min1.Bracket findBracket(final Function1 f, double x1, double x2)
	    throws PoorlyConditionedFunctionException {
	final Min1.Bracket bracket = Min1.findBracket(f, x1, x2);

	assertNotNull("Not null, bracket", bracket);// guard
	BracketTest.assertInvariants(bracket);
	assertConsistent(bracket, f);

	return bracket;
    }

    private static Function1Value findBrent(final Function1 f, Min1.Bracket bracket, double tolerance) {
	final Function1Value min = Min1.findBrent(f, bracket, tolerance);

	assertNotNull("The method always returns a bracket", min);// guard
	BracketTest.assertInvariants(bracket);
	assertConsistent("Minimum", min, f);

	assertTrue(
		"The minimum value of the returned bracket <" + min
			+ "> is not larger than the minimum value of the given bracket <" + bracket + ">",
		min.getF() <= bracket.getInner().getF());

	return min;
    }

    private static Function1WithGradientValue findBrent(final Function1WithGradient f, Bracket bracket,
	    double tolerance) {
	final Function1WithGradientValue min = Min1.findBrent(f, bracket, tolerance);

	assertNotNull("The method always returns a bracket", min);// guard
	BracketTest.assertInvariants(bracket);
	assertConsistent("Minimum", min, f);

	assertTrue(
		"The minimum value of the returned bracket <" + min
			+ "> is not larger than the minimum value of the given bracket <" + bracket + ">",
		min.getF() <= bracket.getInner().getF());
	return min;
    }

    private static final void findBrent_power4(double x1, double x2, double x3, double tolerance) {
	assert x1 < x2;
	assert x2 < x3;
	assert x1 < 0.0;
	assert 0.0 < x3;
	final Min1.Bracket bracket = new Bracket(new Function1Value(x1, POWER_4.value(x1)),
		new Function1Value(x2, POWER_4.value(x2)), new Function1Value(x3, POWER_4.value(x3)));

	findBrent(SQUARED, bracket, tolerance);
    }

    private static final void findBrent_squared(double x1, double x2, double x3, double tolerance) {
	assert x1 < x2;
	assert x2 < x3;
	assert x1 < 0.0;
	assert 0.0 < x3;
	final Min1.Bracket bracket = new Bracket(new Function1Value(x1, SQUARED.value(x1)),
		new Function1Value(x2, SQUARED.value(x2)), new Function1Value(x3, SQUARED.value(x3)));

	findBrent(SQUARED, bracket, tolerance);
    }

    private static final void findBrent_withGradientPower4(double x1, double x2, double x3, double tolerance) {
	assert x1 < x2;
	assert x2 < x3;
	assert x1 < 0.0;
	assert 0.0 < x3;
	final Min1.Bracket bracket = new Bracket(new Function1Value(x1, POWER_4.value(x1)),
		new Function1Value(x2, POWER_4.value(x2)), new Function1Value(x3, POWER_4.value(x3)));

	findBrent(POWER_4_WITH_GRADIENT, bracket, tolerance);
    }

    private static final void findBrent_withGradientSquared(double x1, double x2, double x3, double tolerance) {
	assert x1 < x2;
	assert x2 < x3;
	assert x1 < 0.0;
	assert 0.0 < x3;
	final Min1.Bracket bracket = new Bracket(new Function1Value(x1, SQUARED.value(x1)),
		new Function1Value(x2, SQUARED.value(x2)), new Function1Value(x3, SQUARED.value(x3)));

	findBrent(SQUARED_WITH_GRADIENT, bracket, tolerance);
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
	} catch (PoorlyConditionedFunctionException e) {
	    // Permitted
	}
    }

    @Test
    public void findBracket_order3B() {
	try {
	    findBracket(ORDER_3, -1.6, 0.0);
	} catch (PoorlyConditionedFunctionException e) {
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
    public void findBracket_squaredLeftFarA() {
	findBracket(SQUARED, -1E9, -0.9E9);
    }

    @Test
    public void findBracket_squaredLeftFarB() {
	findBracket(SQUARED, -1E9, -0.9999E9);
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

    @Test
    public void findBrent_power4Centre() {
	final double x1 = -1.0;
	final double x2 = 0.0;
	final double x3 = 1.0;
	final double xTolerance = 1E-3;

	findBrent_power4(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_power4Left() {
	final double x1 = -3.0;
	final double x2 = -1.0;
	final double x3 = 2.0;
	final double xTolerance = 1E-3;

	findBrent_power4(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_power4Right() {
	final double x1 = -2.0;
	final double x2 = 1.0;
	final double x3 = 3.0;
	final double xTolerance = 1E-3;

	findBrent_power4(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_squaredCentre() {
	final double x1 = -1.0;
	final double x2 = 0.0;
	final double x3 = 1.0;
	final double xTolerance = 1E-3;

	findBrent_squared(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_squaredLeft() {
	final double x1 = -3.0;
	final double x2 = -1.0;
	final double x3 = 2.0;
	final double xTolerance = 1E-3;

	findBrent_squared(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_squaredRight() {
	final double x1 = -2.0;
	final double x2 = 1.0;
	final double x3 = 3.0;
	final double xTolerance = 1E-3;

	findBrent_squared(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_withGradientPower4Centre() {
	final double x1 = -1.0;
	final double x2 = 0.0;
	final double x3 = 1.0;
	final double xTolerance = 1E-3;

	findBrent_withGradientPower4(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_withGradientPower4Left() {
	final double x1 = -3.0;
	final double x2 = -1.0;
	final double x3 = 2.0;
	final double xTolerance = 1E-3;

	findBrent_withGradientPower4(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_withGradientPower4Right() {
	final double x1 = -2.0;
	final double x2 = 1.0;
	final double x3 = 3.0;
	final double xTolerance = 1E-3;

	findBrent_withGradientPower4(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_withGradientSquaredCentre() {
	final double x1 = -1.0;
	final double x2 = 0.0;
	final double x3 = 1.0;
	final double xTolerance = 1E-3;

	findBrent_withGradientSquared(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_withGradientSquaredLeft() {
	final double x1 = -3.0;
	final double x2 = -1.0;
	final double x3 = 2.0;
	final double xTolerance = 1E-3;

	findBrent_withGradientSquared(x1, x2, x3, xTolerance);
    }

    @Test
    public void findBrent_withGradientSquaredRight() {
	final double x1 = -2.0;
	final double x2 = 1.0;
	final double x3 = 3.0;
	final double xTolerance = 1E-3;

	findBrent_withGradientSquared(x1, x2, x3, xTolerance);
    }

}
