package uk.badamson.mc.physics.kinematics;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import uk.badamson.mc.math.FunctionNWithGradientValue;
import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.math.MinN;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTerm;
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
		public final boolean isValidForDimension(int n) {
			return 2 <= n;
		}

		@Override
		public final double evaluate(double[] dedx, ImmutableVector state0, ImmutableVector state, double dt) {
			final double v0 = state0.get(1);
			final double v = state.get(1);
			final double vError = v - v0;
			final double mvError = mass * vError;
			final double eError = 0.5 * mvError * vError;
			dedx[1] += mvError;
			return eError;
		}
		
	}// class

	private static void constantVelocity(double x0, double v0, double dt, double mass, double tolerance) {
		final ImmutableVector direction = ImmutableVector.create(1.0);
		final int[] positionTerm = new int[] { 0 };
		final int[] velocityTerm = new int[] { 1 };
		final int[] accelerationTerm = new int[] { 2 };
		final ImmutableVector state0 = ImmutableVector.create(x0, v0, 0.0);
		final List<TimeStepEnergyErrorFunctionTerm> terms = Arrays
				.asList(new PositionError(direction, mass, positionTerm, velocityTerm),
						new VelocityError(direction, mass, velocityTerm, accelerationTerm),
						new ConstantVelocityError(mass)
						);

		final TimeStepEnergyErrorFunction errorFunction = new TimeStepEnergyErrorFunction(state0, dt, terms);
		final FunctionNWithGradientValue solution = MinN.findFletcherReevesPolakRibere(errorFunction, state0, tolerance);
		final ImmutableVector state = solution.getX();
		final double x = state.get(0);
		final double v = state.get(1);
		final double a = state.get(2);

		assertEquals("x", x0 + dt * v0, x, tolerance);
		assertEquals("v", v0, v, tolerance);
		assertEquals("a", 0, a, tolerance);
	}
	
	@Test
	public void constantVelocity_Base() {
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double dt = 1.0;
		final double mass = 1.0;
		final double tolerance = 1E-3;
		
		constantVelocity(x0, v0, dt, mass, tolerance);
	}
	@Test
	public void constantVelocity_x() {
		final double x0 = 2.0;
		final double v0 = 0.0;
		final double dt = 1.0;
		final double mass = 1.0;
		final double tolerance = 1E-3;
		
		constantVelocity(x0, v0, dt, mass, tolerance);
	}
	@Test
	public void constantVelocity_v() {
		final double x0 = 0.0;
		final double v0 = 2.0;
		final double dt = 1.0;
		final double mass = 1.0;
		final double tolerance = 1E-3;
		
		constantVelocity(x0, v0, dt, mass, tolerance);
	}
	@Test
	public void constantVelocity_dt() {
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double dt = 2.0;
		final double mass = 1.0;
		final double tolerance = 1E-3;
		
		constantVelocity(x0, v0, dt, mass, tolerance);
	}
	@Test
	public void constantVelocity_m() {
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double dt = 1.0;
		final double mass = 2.0;
		final double tolerance = 1E-3;
		
		constantVelocity(x0, v0, dt, mass, tolerance);
	}
}
