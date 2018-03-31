package uk.badamson.mc.physics.kinematics;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import uk.badamson.mc.math.FunctionNWithGradientTest;
import uk.badamson.mc.math.FunctionNWithGradientValue;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.MinN;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTerm;
import uk.badamson.mc.physics.ImmutableVector1StateSpaceMapper;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunction;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunctionTerm;

/**
 * <p>
 * Integration tests of classes in the package
 * uk.badamson.mc.physics.kinematics.
 * </p>
 */
public class IntegrationTest {

    private static final class ConstantVelocityError extends AbstractTimeStepEnergyErrorFunctionTerm {

        private final double mass;

        public ConstantVelocityError(double mass) {
            this.mass = mass;
        }

        @Override
        public final double evaluate(double[] dedx, ImmutableVectorN state0, ImmutableVectorN state, double dt) {
            final double v0 = state0.get(1);
            final double v = state.get(1);
            final double vError = v - v0;
            final double mvError = mass * vError;
            final double eError = 0.5 * mvError * vError;
            dedx[1] += mvError;
            return eError;
        }

        @Override
        public final boolean isValidForDimension(int n) {
            return 2 <= n;
        }

    }// class

    private static void assertConstantVelocityErrorFunctionConsitentWithGradientAlongLine(double x0, double v0,
            double dt, double dx, double dv, double da, double mass, double tolerance, double w1, double w2, int n) {
        final TimeStepEnergyErrorFunction errorFunction = create1DconstantVelocityErrorFunction(x0, v0, dt, mass);
        final ImmutableVectorN s0 = create1DStateVector(x0, v0, 0.0);
        final ImmutableVectorN ds = create1DStateVector(dx, dv, da);
        FunctionNWithGradientTest.assertValueConsistentWithGradientAlongLine(errorFunction, w1, w2, n, s0, ds);
    }

    private static void constantVelocitySolution(double x0, double v0, double dt, double mass, double tolerance) {
        final TimeStepEnergyErrorFunction errorFunction = create1DconstantVelocityErrorFunction(x0, v0, dt, mass);
        final ImmutableVectorN state0 = create1DStateVector(x0, v0, 0.0);

        final FunctionNWithGradientValue solution = MinN.findFletcherReevesPolakRibere(errorFunction, state0,
                tolerance);

        final ImmutableVectorN state = solution.getX();
        final double x = state.get(0);
        final double v = state.get(1);
        final double a = state.get(2);

        assertEquals("x", x0 + dt * v0, x, tolerance);
        assertEquals("v", v0, v, tolerance);
        assertEquals("a", 0, a, tolerance);
    }

    private static TimeStepEnergyErrorFunction create1DconstantVelocityErrorFunction(double x0, double v0, double dt,
            double mass) {
        final ImmutableVector1StateSpaceMapper positionVectorMapper = new ImmutableVector1StateSpaceMapper(0);
        final ImmutableVector1StateSpaceMapper velocityVectorMapper = new ImmutableVector1StateSpaceMapper(1);
        final int[] velocityTerm = new int[] { 1 };
        final int[] accelerationTerm = new int[] { 2 };
        final List<TimeStepEnergyErrorFunctionTerm> terms = Arrays.asList(
                new PositionError<>(mass, positionVectorMapper, velocityVectorMapper),
                new VelocityError(mass, velocityTerm, accelerationTerm), new ConstantVelocityError(mass));

        final TimeStepEnergyErrorFunction errorFunction = new TimeStepEnergyErrorFunction(
                create1DStateVector(x0, v0, 0.0), dt, terms);
        return errorFunction;
    }

    private static ImmutableVectorN create1DStateVector(double x, double v, double a) {
        return ImmutableVectorN.create(x, v, a);
    }

    @Test
    public void assertConstantVelocityErrorFunctionConsitentWithGradientAlongLineDA() {
        final double x0 = 0.0;
        final double v0 = 2.0;
        final double dt = 1.0;
        final double mass = 1.0;
        final double tolerance = 1E-6;
        ;
        final double dx = 0.0;
        final double dv = 0.0;
        final double da = 1.0;

        final double w1 = -2.0;
        final double w2 = 2.0;
        final int n = 20;

        assertConstantVelocityErrorFunctionConsitentWithGradientAlongLine(x0, v0, dt, dx, dv, da, mass, tolerance, w1,
                w2, n);
    }

    @Test
    public void assertConstantVelocityErrorFunctionConsitentWithGradientAlongLineDV() {
        final double x0 = 0.0;
        final double v0 = 2.0;
        final double dt = 1.0;
        final double mass = 1.0;
        final double tolerance = 1E-6;
        ;
        final double dx = 0.0;
        final double dv = 1.0;
        final double da = 0.0;

        final double w1 = -2.0;
        final double w2 = 2.0;
        final int n = 20;

        assertConstantVelocityErrorFunctionConsitentWithGradientAlongLine(x0, v0, dt, dx, dv, da, mass, tolerance, w1,
                w2, n);
    }

    @Test
    public void assertConstantVelocityErrorFunctionConsitentWithGradientAlongLineDX() {
        final double x0 = 0.0;
        final double v0 = 2.0;
        final double dt = 1.0;
        final double mass = 1.0;
        final double tolerance = 1E-6;
        ;
        final double dx = 1.0;
        final double dv = 0.0;
        final double da = 0.0;

        final double w1 = -2.0;
        final double w2 = 2.0;
        final int n = 20;

        assertConstantVelocityErrorFunctionConsitentWithGradientAlongLine(x0, v0, dt, dx, dv, da, mass, tolerance, w1,
                w2, n);
    }

    @Test
    public void constantVelocity_Base() {
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double dt = 1.0;
        final double mass = 1.0;
        final double tolerance = 1E-6;

        constantVelocitySolution(x0, v0, dt, mass, tolerance);
    }

    @Test
    public void constantVelocity_dt() {
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double dt = 2.0;
        final double mass = 1.0;
        final double tolerance = 1E-6;

        constantVelocitySolution(x0, v0, dt, mass, tolerance);
    }

    @Test
    public void constantVelocity_m() {
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double dt = 1.0;
        final double mass = 2.0;
        final double tolerance = 1E-6;

        constantVelocitySolution(x0, v0, dt, mass, tolerance);
    }

    @Test
    public void constantVelocity_v() {
        final double x0 = 0.0;
        final double v0 = 2.0;
        final double dt = 1.0;
        final double mass = 1.0;
        final double tolerance = 1E-6;

        constantVelocitySolution(x0, v0, dt, mass, tolerance);
    }

    @Test
    public void constantVelocity_x() {
        final double x0 = 2.0;
        final double v0 = 0.0;
        final double dt = 1.0;
        final double mass = 1.0;
        final double tolerance = 1E-6;

        constantVelocitySolution(x0, v0, dt, mass, tolerance);
    }

}
