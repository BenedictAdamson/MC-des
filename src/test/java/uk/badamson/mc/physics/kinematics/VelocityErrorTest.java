package uk.badamson.mc.physics.kinematics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTermTest;

/**
 * <p>
 * Unit tests for the class {@link VelocityError}.
 * </p>
 */
public class VelocityErrorTest {

    public static void assertInvariants(VelocityError term) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

        final double mass = term.getMass();
        final int spaceDimension = term.getSpaceDimension();

        assertTrue("Mass <" + mass + "> is positive and  finite", 0.0 < mass && Double.isFinite(mass));

        int maxTerm = -1;
        for (int i = 0; i < spaceDimension; i++) {
            final int velocityTerm = term.getVelocityTerm(i);
            final int accelerationTerm = term.getAccelerationTerm(i);
            assertTrue("velocityTerm[" + i + "] <" + velocityTerm + "> not negative", 0 <= velocityTerm);
            assertTrue("accelerationTerm[" + i + "] <" + accelerationTerm + "> not negative", 0 <= accelerationTerm);
            maxTerm = Math.max(maxTerm, velocityTerm);
            maxTerm = Math.max(maxTerm, accelerationTerm);
        }
        assertTrue("This is valid for a dimension if, and only if, "
                + "the number of variables exceeds the largest velocity term index and "
                + "exceeds the largest acceleration term index", term.isValidForDimension(maxTerm + 1));
    }

    public static void assertInvariants(VelocityError term1, VelocityError term2) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
    }

    private static VelocityError constructor(double mass, int[] velocityTerm, int[] accelerationTerm) {
        final VelocityError term = new VelocityError(mass, velocityTerm, accelerationTerm);

        assertInvariants(term);

        assertEquals("spaceDimension", velocityTerm.length, term.getSpaceDimension());
        assertEquals("mass", mass, term.getMass(), Double.MIN_NORMAL);
        for (int i = 0, n = velocityTerm.length; i < n; ++i) {
            assertEquals("velocityTerm[" + i + "]", velocityTerm[i], term.getVelocityTerm(i));
            assertEquals("accelerationTerm[" + i + "]", accelerationTerm[i], term.getAccelerationTerm(i));
        }

        return term;
    }

    private static double evaluate(VelocityError term, double[] dedx, ImmutableVector x0, ImmutableVector x,
            double dt) {
        final double e = AbstractTimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, x0, x, dt);

        assertInvariants(term);

        return e;
    }

    private static final void evaluate_1(double mass, double dedv0, double deda0, double v0, double a0, double v,
            double a, double dt, double eExpected, double dEDVExpected, double dEDAExpected, double tolerance) {
        final int velocityTerm = 0;
        final int accelerationTerm = 1;
        final double[] dedx = { dedv0, deda0 };
        final ImmutableVector state0 = ImmutableVector.create(v0, a0);
        final ImmutableVector state = ImmutableVector.create(v, a);
        final VelocityError term = new VelocityError(mass, new int[] { velocityTerm }, new int[] { accelerationTerm });

        final double e = evaluate(term, dedx, state0, state, dt);

        assertEquals("energy", eExpected, e, tolerance);
        assertEquals("dedx[velocityTerm]", dEDVExpected, dedx[velocityTerm], tolerance);
        assertEquals("dedx[accelerationTerm]", dEDAExpected, dedx[accelerationTerm], tolerance);
    }

    private static final void evaluate_1Minimum(double mass, double dedv0, double deda0, double v0, double a0, double v,
            double a, double dt, double tolerance) {
        final double eExpected = 0.0;
        final double dEDVExpected = dedv0;
        final double dEDAExpected = deda0;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void constructor_1A() {
        final double mass = 2.0;
        final int[] positionTerm = { 3 };
        final int[] velocityTerm = { 4 };

        constructor(mass, positionTerm, velocityTerm);
    }

    @Test
    public void constructor_1B() {
        final double mass = 1E24;
        final int[] positionTerm = { 7 };
        final int[] velocityTerm = { 11 };

        constructor(mass, positionTerm, velocityTerm);
    }

    @Test
    public void constructor_2() {
        final double mass = 1000.0;
        final int[] positionTerm = { 3, 4 };
        final int[] velocityTerm = { 5, 6 };

        constructor(mass, positionTerm, velocityTerm);
    }

    @Test
    public void evaluate_1A() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 2.0;
        final double dt = 1.0;
        final double eExpected = 0.5;
        final double dEDVExpected = -1.0;
        final double dEDAExpected = 0.5;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1A0() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 2.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double eExpected = 0.5;
        final double dEDVExpected = -1.0;
        final double dEDAExpected = 0.5;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1DT() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 2.0;
        final double dt = 2.0;
        final double eExpected = 2.0;
        final double dEDVExpected = -2.0;
        final double dEDAExpected = 2.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1MinimumA0A() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0;
        final double v0 = 0.0;
        final double a0 = 2.0;
        final double v = 0.0;
        final double a = -2.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumA0V() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0;
        final double v0 = 0.0;
        final double a0 = 2.0;
        final double v = 1.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumBase() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumDEDA0() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 2.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumDEDV0() {
        final double mass = 1.0;
        final double dedv0 = 2.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumMass() {
        final double mass = 2.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumV0A() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0;
        final double v0 = 2.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = -4.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumV0V() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0;
        final double v0 = 2.0;
        final double a0 = 0.0;
        final double v = 2.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1V() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 2.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double eExpected = 2.0;
        final double dEDVExpected = 2.0;
        final double dEDAExpected = -1.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1V0() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 2.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double eExpected = 2.0;
        final double dEDVExpected = -2.0;
        final double dEDAExpected = 1.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1VDT() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 2.0;
        final double a = 0.0;
        final double dt = 2.0;
        final double eExpected = 2.0;
        final double dEDVExpected = 2.0;
        final double dEDAExpected = -2.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1VM() {
        final double mass = 2.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 2.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double eExpected = 4.0;
        final double dEDVExpected = 4.0;
        final double dEDAExpected = -2.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }
}
