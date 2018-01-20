package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import uk.badamson.mc.math.FunctionNWithGradientTest;
import uk.badamson.mc.math.FunctionNWithGradientValue;
import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.math.MinN;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTerm;
import uk.badamson.mc.physics.MassConservationError;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunction;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunctionTerm;
import uk.badamson.mc.physics.dynamics.Newton2Error;
import uk.badamson.mc.physics.kinematics.PositionError;
import uk.badamson.mc.physics.kinematics.VelocityError;

/**
 * <p>
 * Integration tests of classes in the package uk.badamson.mc.physics.
 * </p>
 */
public class IntegrationTest {
    private static final class ConstantForceError extends AbstractTimeStepEnergyErrorFunctionTerm {

	private final boolean forceOn;

	public ConstantForceError(boolean forceOn) {
	    this.forceOn = forceOn;
	}

	@Override
	public final double evaluate(double[] dedx, ImmutableVector state0, ImmutableVector state, double dt) {
	    final double sign = forceOn ? 1.0 : -1.0;
	    final double m0 = state0.get(massTerm);
	    final double f0 = state0.get(forceTerm[0]) * sign;

	    final double m = state.get(massTerm);
	    final double f = state.get(forceTerm[0]);

	    final double mMean = (m + m0) * 0.5;

	    final double fe = f - f0;
	    final double ae = fe / mMean;
	    final double ve = ae * dt;
	    final double e = 0.5 * mMean * ve * ve;

	    final double dedf = ve * dt / mMean;
	    dedx[forceTerm[0]] += dedf;

	    return e;
	}

	@Override
	public final boolean isValidForDimension(int n) {
	    return 2 <= n;
	}

    }// class

    private static final int massTerm = 0;
    private static final int[] positionTerm = { 1 };
    private static final int[] velocityTerm = { 2 };
    private static final int[] accelerationTerm = { 3 };
    private static final int[] forceTerm = { 4 };
    private static final boolean[] massTransferInto = {};
    private static final int[] advectionMassRateTerm = {};

    private static final int[] advectionVelocityTerm = {};

    private static void assert1DConstantForceErrorFunctionConsitentWithGradientAlongLine(boolean forceOn,
	    double massReference, double timeReference, double specificEnergyReference, double m0, double x0, double v0,
	    double a0, double dt, double dm, double dx, double dv, double da, double df, double tolerance, double w1,
	    double w2, int n) {
	final TimeStepEnergyErrorFunction errorFunction = create1DConstantForceErrorFunction(forceOn, massReference,
		timeReference, specificEnergyReference, m0, x0, v0, a0, dt);
	final ImmutableVector s0 = create1DStateVector(m0, x0, v0, a0, m0 * a0);
	final ImmutableVector ds = create1DStateVector(dm, dx, dv, da, df);
	FunctionNWithGradientTest.assertValueConsistentWithGradientAlongLine(errorFunction, w1, w2, n, s0, ds);
    }

    private static void constantForceSolution(boolean forceOn, double massReference, double timeReference,
	    double specificEnergyReference, double m0, double x0, double v0, double a0, double dt, double tolerance) {
	final TimeStepEnergyErrorFunction errorFunction = create1DConstantForceErrorFunction(forceOn, massReference,
		timeReference, specificEnergyReference, m0, x0, v0, a0, dt);
	final double f0 = m0 * a0;
	final ImmutableVector state0 = create1DStateVector(m0, x0, v0, a0, f0);

	final FunctionNWithGradientValue solution = MinN.findFletcherReevesPolakRibere(errorFunction, state0,
		tolerance);

	final ImmutableVector state = solution.getX();
	final double m = state.get(0);
	final double x = state.get(1);
	final double v = state.get(2);
	final double a = state.get(3);
	final double f = state.get(4);

	assertEquals("m", m0, m, tolerance);
	assertEquals("x", x0 + dt * (v0 + 0.5 * a0 * dt), x, tolerance);
	assertEquals("v", v0 + dt * a0, v, tolerance);
	assertEquals("a", a0, a, tolerance);
	assertEquals("f", f0, f, tolerance);
    }

    private static TimeStepEnergyErrorFunction create1DConstantForceErrorFunction(boolean forceOn, double massReference,
	    double timeReference, double specificEnergyReference, double m0, double x0, double v0, double a0,
	    double dt) {
	final double f0 = m0 * a0;
	final List<TimeStepEnergyErrorFunctionTerm> terms = Arrays.asList(
		new PositionError(massReference, positionTerm, velocityTerm),
		new VelocityError(massReference, velocityTerm, accelerationTerm),
		new Newton2Error(massReference, timeReference, massTerm, velocityTerm, accelerationTerm,
			massTransferInto, advectionMassRateTerm, advectionVelocityTerm, new boolean[] { forceOn },
			forceTerm),
		new MassConservationError(massReference, specificEnergyReference, massTerm, massTransferInto,
			advectionMassRateTerm),
		new MomentumConservationError(massTerm, velocityTerm, massTransferInto, advectionMassRateTerm,
			advectionVelocityTerm, new boolean[] { forceOn }, forceTerm),
		new ConstantForceError(forceOn));

	final TimeStepEnergyErrorFunction errorFunction = new TimeStepEnergyErrorFunction(
		create1DStateVector(m0, x0, v0, a0, f0), dt, terms);
	return errorFunction;
    }

    private static ImmutableVector create1DStateVector(double m, double x, double v, double a, double f) {
	return ImmutableVector.create(m, x, v, a, f);
    }

    @Test
    public void constantForceErrorFunctionConsitentWithGradientAlongLine_da() {
	final boolean forceOn = true;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 1.0;
	final double v0 = 1.0;
	final double a0 = 1.0;

	final double dt = 1.0;

	final double dm = 0.0;
	final double dx = 0.0;
	final double dv = 0.0;
	final double da = 1.0;
	final double df = 0.0;

	final double tolerance = 0.0;
	final double w1 = -2.0;
	final double w2 = 2.0;
	final int n = 20;

	assert1DConstantForceErrorFunctionConsitentWithGradientAlongLine(forceOn, massReference, timeReference,
		specificEnergyReference, m0, x0, v0, a0, dt, dm, dx, dv, da, df, tolerance, w1, w2, n);
    }

    @Test
    public void constantForceErrorFunctionConsitentWithGradientAlongLine_dfA() {
	final boolean forceOn = true;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 1.0;
	final double v0 = 1.0;
	final double a0 = 1.0;

	final double dt = 1.0;

	final double dm = 0.0;
	final double dx = 0.0;
	final double dv = 0.0;
	final double da = 0.0;
	final double df = 1.0;

	final double tolerance = 0.0;
	final double w1 = -2.0;
	final double w2 = 2.0;
	final int n = 20;

	assert1DConstantForceErrorFunctionConsitentWithGradientAlongLine(forceOn, massReference, timeReference,
		specificEnergyReference, m0, x0, v0, a0, dt, dm, dx, dv, da, df, tolerance, w1, w2, n);
    }

    @Test
    public void constantForceErrorFunctionConsitentWithGradientAlongLine_dfB() {
	final boolean forceOn = false;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 1.0;
	final double v0 = 1.0;
	final double a0 = 1.0;

	final double dt = 1.0;

	final double dm = 0.0;
	final double dx = 0.0;
	final double dv = 0.0;
	final double da = 0.0;
	final double df = 1.0;

	final double tolerance = 0.0;
	final double w1 = -2.0;
	final double w2 = 2.0;
	final int n = 20;

	assert1DConstantForceErrorFunctionConsitentWithGradientAlongLine(forceOn, massReference, timeReference,
		specificEnergyReference, m0, x0, v0, a0, dt, dm, dx, dv, da, df, tolerance, w1, w2, n);
    }

    @Test
    public void constantForceErrorFunctionConsitentWithGradientAlongLine_dm() {
	final boolean forceOn = true;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 1.0;
	final double v0 = 1.0;
	final double a0 = 1.0;

	final double dt = 1.0;

	final double dm = 1.0;
	final double dx = 0.0;
	final double dv = 0.0;
	final double da = 0.0;
	final double df = 0.0;

	final double tolerance = 0.0;
	final double w1 = -2.0;
	final double w2 = 2.0;
	final int n = 20;

	assert1DConstantForceErrorFunctionConsitentWithGradientAlongLine(forceOn, massReference, timeReference,
		specificEnergyReference, m0, x0, v0, a0, dt, dm, dx, dv, da, df, tolerance, w1, w2, n);
    }

    @Test
    public void constantForceErrorFunctionConsitentWithGradientAlongLine_dv() {
	final boolean forceOn = true;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 1.0;
	final double v0 = 1.0;
	final double a0 = 1.0;

	final double dt = 1.0;

	final double dm = 0.0;
	final double dx = 0.0;
	final double dv = 1.0;
	final double da = 0.0;
	final double df = 0.0;

	final double tolerance = 0.0;
	final double w1 = -2.0;
	final double w2 = 2.0;
	final int n = 20;

	assert1DConstantForceErrorFunctionConsitentWithGradientAlongLine(forceOn, massReference, timeReference,
		specificEnergyReference, m0, x0, v0, a0, dt, dm, dx, dv, da, df, tolerance, w1, w2, n);
    }

    @Test
    public void constantForceErrorFunctionConsitentWithGradientAlongLine_dx() {
	final boolean forceOn = true;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 1.0;
	final double v0 = 1.0;
	final double a0 = 1.0;

	final double dt = 1.0;

	final double dm = 0.0;
	final double dx = 1.0;
	final double dv = 0.0;
	final double da = 0.0;
	final double df = 0.0;

	final double tolerance = 0.0;
	final double w1 = -2.0;
	final double w2 = 2.0;
	final int n = 20;

	assert1DConstantForceErrorFunctionConsitentWithGradientAlongLine(forceOn, massReference, timeReference,
		specificEnergyReference, m0, x0, v0, a0, dt, dm, dx, dv, da, df, tolerance, w1, w2, n);
    }

    @Test
    public void constantForceSolution_0() {
	final boolean forceOn = true;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 0.0;
	final double v0 = 0.0;
	final double a0 = 0.0;

	final double dt = 1.0;
	final double tolerance = 1.0E-6;

	constantForceSolution(forceOn, massReference, timeReference, specificEnergyReference, m0, x0, v0, a0, dt,
		tolerance);
    }

    @Test
    public void constantForceSolution_a() {
	final boolean forceOn = true;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 0.0;
	final double v0 = 0.0;
	final double a0 = 1.0;

	final double dt = 1.0;
	final double tolerance = 1.0E-6;

	constantForceSolution(forceOn, massReference, timeReference, specificEnergyReference, m0, x0, v0, a0, dt,
		tolerance);
    }

    @Test
    public void constantForceSolution_v() {
	final boolean forceOn = true;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 0.0;
	final double v0 = 1.0;
	final double a0 = 0.0;

	final double dt = 1.0;
	final double tolerance = 1.0E-6;

	constantForceSolution(forceOn, massReference, timeReference, specificEnergyReference, m0, x0, v0, a0, dt,
		tolerance);
    }

    @Test
    public void constantForceSolution_va() {
	final boolean forceOn = true;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 0.0;
	final double v0 = 1.0;
	final double a0 = 1.0;

	final double dt = 1.0;
	final double tolerance = 1.0E-6;

	constantForceSolution(forceOn, massReference, timeReference, specificEnergyReference, m0, x0, v0, a0, dt,
		tolerance);
    }

    @Test
    public void constantForceSolution_x() {
	final boolean forceOn = true;
	final double massReference = 1.0;
	final double timeReference = 1.0;
	final double specificEnergyReference = 1.0;

	final double m0 = 1.0;
	final double x0 = 1.0;
	final double v0 = 0.0;
	final double a0 = 0.0;

	final double dt = 1.0;
	final double tolerance = 1.0E-6;

	constantForceSolution(forceOn, massReference, timeReference, specificEnergyReference, m0, x0, v0, a0, dt,
		tolerance);
    }

}
