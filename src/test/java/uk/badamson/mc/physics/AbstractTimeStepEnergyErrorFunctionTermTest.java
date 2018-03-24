package uk.badamson.mc.physics;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * Unit tests for classes derived from
 * {@link AbstractTimeStepEnergyErrorFunctionTerm}.
 * </p>
 */
public class AbstractTimeStepEnergyErrorFunctionTermTest {

    public static void assertInvariants(AbstractTimeStepEnergyErrorFunctionTerm term) {
        ObjectTest.assertInvariants(term);// inherited
        TimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited
    }

    public static void assertInvariants(AbstractTimeStepEnergyErrorFunctionTerm term1,
            AbstractTimeStepEnergyErrorFunctionTerm term2) {
        ObjectTest.assertInvariants(term1, term2);// inherited
        TimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
    }

    public static double evaluate(AbstractTimeStepEnergyErrorFunctionTerm term, double[] dedx, ImmutableVectorN state0,
            ImmutableVectorN state, double dt) {
        final double e = TimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, state0, state, dt);

        assertInvariants(term);

        return e;
    }
}
