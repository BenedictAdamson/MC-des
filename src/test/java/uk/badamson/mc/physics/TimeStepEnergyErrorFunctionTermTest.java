package uk.badamson.mc.physics;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.math.ImmutableVector;

/**
 * <p>
 * Unit tests classes that implement the interface
 * {@link TimeStepEnergyErrorFunctionTerm}.
 * </p>
 */
public class TimeStepEnergyErrorFunctionTermTest {

    public static void assertInvariants(TimeStepEnergyErrorFunctionTerm t) {
	ObjectTest.assertInvariants(t);// inherited
    }

    public static void assertInvariants(TimeStepEnergyErrorFunctionTerm t1, TimeStepEnergyErrorFunctionTerm t2) {
	ObjectTest.assertInvariants(t1, t2);// inherited
    }

    public static double evaluate(TimeStepEnergyErrorFunctionTerm term, double[] dedx, ImmutableVector x0,
	    ImmutableVector x, double dt) {
	final double e = term.evaluate(dedx, x0, x, dt);

	assertInvariants(term);

	return e;
    }
}