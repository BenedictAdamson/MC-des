package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * Units tests for the class {@link VersorError}.
 * </p>
 */
public class VersorErrorTest {

    private static final double LENGTH_1 = 1.0;
    private static final double LENGTH_2 = 1E7;

    private static final double MASS_1 = 2.0;
    private static final double MASS_2 = 1E24;

    public static void assertInvariants(VersorError term) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

        final double mass = term.getMass();
        final double length = term.getLength();
        final QuaternionStateSpaceMapper quaternionMapper = term.getQuaternionMapper();

        assertNotNull("quaternionMapper", quaternionMapper);// guard

        AbstractTimeStepEnergyErrorFunctionTermTest.assertIsReferenceScale("mass", mass);
        AbstractTimeStepEnergyErrorFunctionTermTest.assertIsReferenceScale("length", length);
    }

    public static void assertInvariants(VersorError term1, VersorError term2) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
    }

    private static VersorError constructor(double length, double mass, QuaternionStateSpaceMapper quaternionMapper) {
        final VersorError term = new VersorError(length, mass, quaternionMapper);

        assertInvariants(term);

        assertEquals("length", length, term.getLength(), Double.MIN_NORMAL);
        assertEquals("mass", mass, term.getMass(), Double.MIN_NORMAL);
        assertSame("quaternionMapper", quaternionMapper, term.getQuaternionMapper());

        return term;
    }

    private static double evaluate(VersorError term, double[] dedx, ImmutableVectorN x0, ImmutableVectorN x,
            double dt) {
        final double e = AbstractTimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, x0, x, dt);

        assertInvariants(term);

        return e;
    }

    @Test
    public void constructor_A() {
        constructor(LENGTH_1, MASS_1, new QuaternionStateSpaceMapper(0));
    }

    @Test
    public void constructor_B() {
        constructor(LENGTH_2, MASS_2, new QuaternionStateSpaceMapper(1));
    }

}
