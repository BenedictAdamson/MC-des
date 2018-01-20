package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests of the class {@link FunctionNWithGradientValue}.
 * </p>
 */
public class FunctionNWithGradientValueTest {

    public static void assertInvariants(FunctionNWithGradientValue f) {
	ObjectTest.assertInvariants(f);

	final ImmutableVector x = f.getX();
	final ImmutableVector dfdx = f.getDfDx();

	assertNotNull("Not null, x", x);// guard
	assertNotNull("Not null, dfdx", dfdx);// guard
	assertEquals("The dimension of the gradient vector is equal to the dimension of the domain vector",
		x.getDimension(), dfdx.getDimension());
    }

    public static void assertInvariants(FunctionNWithGradientValue f1, FunctionNWithGradientValue f2) {
	ObjectTest.assertInvariants(f1, f2);

	final boolean equals = f1.equals(f2);
	assertFalse("Equality requires equivalent attributes, f",
		equals && Double.doubleToLongBits(f1.getF()) != Double.doubleToLongBits(f2.getF()));
	assertFalse("Equality requires equivalent attributes, x", equals && !f1.getX().equals(f2.getX()));
	assertFalse("Equality requires equivalent attributes, dfdx", equals && !f1.getDfDx().equals(f2.getDfDx()));
    }

    private static FunctionNWithGradientValue constructor(ImmutableVector x, double f, ImmutableVector dfdx) {
	final FunctionNWithGradientValue v = new FunctionNWithGradientValue(x, f, dfdx);

	assertInvariants(v);
	assertEquals("f (bits)", Double.doubleToLongBits(f), Double.doubleToLongBits(v.getF()));
	assertEquals("x", x, v.getX());
	assertEquals("dfdx", dfdx, v.getDfDx());

	return v;
    }

    private static void constructor_equals(ImmutableVector x, double f, ImmutableVector dfdx) {
	final FunctionNWithGradientValue v1 = new FunctionNWithGradientValue(x, f, dfdx);
	final FunctionNWithGradientValue v2 = new FunctionNWithGradientValue(x, f, dfdx);

	assertInvariants(v1, v2);
	assertEquals(v1, v2);
    }

    @Test
    public void constructor_1A() {
	constructor(ImmutableVector.create(0.0), 0.0, ImmutableVector.create(0.0));
    }

    @Test
    public void constructor_1B() {
	constructor(ImmutableVector.create(1.0), 2.0, ImmutableVector.create(3.0));
    }

    @Test
    public void constructor_2() {
	constructor(ImmutableVector.create(0.0, 1.0), 2.0, ImmutableVector.create(3.0, 4.0));
    }

    @Test
    public void constructor_equals1A() {
	constructor_equals(ImmutableVector.create(0.0), 0.0, ImmutableVector.create(0.0));
    }

    @Test
    public void constructor_equals1B() {
	constructor_equals(ImmutableVector.create(1.0), 2.0, ImmutableVector.create(3.0));
    }

    @Test
    public void constructor_equals2() {
	constructor_equals(ImmutableVector.create(0.0, 1.0), 2.0, ImmutableVector.create(3.0, 4.0));
    }

    @Test
    public void constructor_notEqualsDfDx() {
	final ImmutableVector x = ImmutableVector.create(1.0);
	final double f = 2.0;
	final FunctionNWithGradientValue v1 = new FunctionNWithGradientValue(x, f, ImmutableVector.create(3.0));
	final FunctionNWithGradientValue v2 = new FunctionNWithGradientValue(x, f, ImmutableVector.create(4.0));

	assertInvariants(v1, v2);
	assertNotEquals(v1, v2);
    }

    @Test
    public void constructor_notEqualsF() {
	final ImmutableVector x = ImmutableVector.create(1.0);
	final ImmutableVector dfdx = ImmutableVector.create(4.0);
	final FunctionNWithGradientValue v1 = new FunctionNWithGradientValue(x, 2.0, dfdx);
	final FunctionNWithGradientValue v2 = new FunctionNWithGradientValue(x, 3.0, dfdx);

	assertInvariants(v1, v2);
	assertNotEquals(v1, v2);
    }

    @Test
    public void constructor_notEqualsX() {
	final double f = 3.0;
	final ImmutableVector dfdx = ImmutableVector.create(4.0);
	final FunctionNWithGradientValue v1 = new FunctionNWithGradientValue(ImmutableVector.create(1.0), f, dfdx);
	final FunctionNWithGradientValue v2 = new FunctionNWithGradientValue(ImmutableVector.create(2.0), f, dfdx);

	assertInvariants(v1, v2);
	assertNotEquals(v1, v2);
    }
}
