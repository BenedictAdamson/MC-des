package uk.badamson.mc.physics.kinematics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunctionTermTest;

/**
 * <p>
 * Unit tests for the class {@link PositionError}.
 * </p>
 */
public class PositionErrorTest {

	private static final double TOLERANCE_1 = Math.nextAfter(1.0, Double.POSITIVE_INFINITY) - 1.0;

	public static void assertInvariants(PositionError term) {
		ObjectTest.assertInvariants(term);// inherited
		TimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

		final ImmutableVector direction = term.getDirection();
		final double mass = term.getMass();
		final int spaceDimension = term.getSpaceDimension();

		assertNotNull("Not null, direction", direction);// guard

		assertEquals("The direction vector is a unit vector", 1.0, direction.magnitude2(),
				Math.sqrt(TOLERANCE_1 * spaceDimension));
		assertEquals("The number of space dimensions is equal to the dimension of the direction vector.",
				direction.getDimension(), spaceDimension);
		assertTrue("Mass <" + mass + "> is positive and  finite", 0.0 < mass && Double.isFinite(mass));

		int maxTerm = -1;
		for (int i = 0; i < spaceDimension; i++) {
			final int positionTerm = term.getPositionTerm(i);
			final int velocityTerm = term.getVelocityTerm(i);
			assertTrue("positionTerm[" + i + "] <" + positionTerm + "> not negative", 0 <= positionTerm);
			assertTrue("velocityTerm[" + i + "] <" + velocityTerm + "> not negative", 0 <= velocityTerm);
			maxTerm = Math.max(maxTerm, positionTerm);
			maxTerm = Math.max(maxTerm, velocityTerm);
		}
		assertTrue("This is valid for a dimension if, and only if, "
				+ "the number of variables exceeds the largest position term index and "
				+ "exceeds the largest velocity term index", term.isValidForDimension(maxTerm + 1));
	}

	public static void assertInvariants(PositionError term1, PositionError term2) {
		ObjectTest.assertInvariants(term1, term2);// inherited
		TimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
	}

	private static PositionError constructor(ImmutableVector direction, double mass, int[] positionTerm,
			int[] velocityTerm) {
		final PositionError term = new PositionError(direction, mass, positionTerm, velocityTerm);

		assertInvariants(term);

		assertSame("direction", direction, term.getDirection());
		assertEquals("mass", mass, term.getMass(), Double.MIN_NORMAL);
		for (int i = 0, n = direction.getDimension(); i < n; ++i) {
			assertEquals("positionTerm[" + i + "]", positionTerm[i], term.getPositionTerm(i));
			assertEquals("velocityTerm[" + i + "]", velocityTerm[i], term.getVelocityTerm(i));
		}

		return term;
	}

	private static double evaluate(PositionError term, double[] dedx, ImmutableVector x0, ImmutableVector x,
			double dt) {
		final double e = TimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, x0, x, dt);

		assertInvariants(term);

		return e;
	}

	private static final void evaluate_1(double direction, double mass, int positionTerm, int velocityTerm,
			double dedx0, double dedv0, double x0, double v0, double x, double v, double dt, double eExpected,
			double dEDXExpected, double dEDVExpected, double tolerance) {
		final PositionError term = new PositionError(ImmutableVector.create(direction), mass,
				new int[] { positionTerm }, new int[] { velocityTerm });
		final double[] dedx = { dedx0, dedv0 };

		final double e = evaluate(term, dedx, ImmutableVector.create(x0, v0), ImmutableVector.create(x, v), dt);

		assertEquals("energy", eExpected, e, tolerance);
		assertEquals("dedx[positionTerm]", dEDXExpected, dedx[positionTerm], tolerance);
		assertEquals("dedx[velocityTerm]", dEDVExpected, dedx[velocityTerm], tolerance);
	}

	private static void evaluate_1Minimum(final double direction, final double mass, final int positionTerm,
			final int velocityTerm, final double dedx0, final double dedv0, final double x0, final double v0,
			final double x, final double v, final double dt, final double tolerance) {
		final double eExpected = 0.0;
		final double dEDXExpected = dedx0;
		final double dEDVExpected = dedv0;

		evaluate_1(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
				dEDVExpected, tolerance);
	}

	@Test
	public void constructor_1A() {
		final ImmutableVector direction = ImmutableVector.create(1.0);
		final double mass = 2.0;
		final int[] positionTerm = { 3 };
		final int[] velocityTerm = { 4 };

		constructor(direction, mass, positionTerm, velocityTerm);
	}

	@Test
	public void constructor_1B() {
		final ImmutableVector direction = ImmutableVector.create(-1.0);
		final double mass = 1E24;
		final int[] positionTerm = { 7 };
		final int[] velocityTerm = { 11 };

		constructor(direction, mass, positionTerm, velocityTerm);
	}

	@Test
	public void constructor_2() {
		final double e = Math.sqrt(2.0) / 2.0;
		final ImmutableVector direction = ImmutableVector.create(e, e);
		final double mass = 1000.0;
		final int[] positionTerm = { 3, 4 };
		final int[] velocityTerm = { 5, 6 };

		constructor(direction, mass, positionTerm, velocityTerm);
	}

	@Test
	public void evaluate_1MassX0() {
		final double direction = 1.0;
		final double mass = 2.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 2.0;
		final double v0 = 0.0;
		final double x = 0.0;
		final double v = 0.0;
		final double dt = 1.0;
		final double eExpected = 4.0;
		final double dEDXExpected = -4.0;
		final double dEDVExpected = 2.0;
		final double tolerance = 1E-3;

		evaluate_1(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
				dEDVExpected, tolerance);
	}

	@Test
	public void evaluate_1MinimumBase() {
		final double direction = 1.0;
		final double mass = 1.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double x = 0.0;
		final double v = 0.0;
		final double dt = 1.0;
		final double tolerance = 1E-3;

		evaluate_1Minimum(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
	}

	@Test
	public void evaluate_1MinimumDEDV0() {
		final double direction = 1.0;
		final double mass = 1.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 2.0;
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double x = 0.0;
		final double v = 0.0;
		final double dt = 1.0;
		final double tolerance = 1E-3;

		evaluate_1Minimum(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
	}

	@Test
	public void evaluate_1MinimumDEDX0() {
		final double direction = 1.0;
		final double mass = 1.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 2.0;
		final double dedv0 = 0.0;
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double x = 0.0;
		final double v = 0.0;
		final double dt = 1.0;
		final double tolerance = 1E-3;

		evaluate_1Minimum(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
	}

	@Test
	public void evaluate_1MinimumDirection() {
		final double direction = -1.0;
		final double mass = 1.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double x = 0.0;
		final double v = 0.0;
		final double dt = 1.0;
		final double tolerance = 1E-3;

		evaluate_1Minimum(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
	}

	@Test
	public void evaluate_1MinimumMass() {
		final double direction = 1.0;
		final double mass = 2.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double x = 0.0;
		final double v = 0.0;
		final double dt = 1.0;
		final double tolerance = 1E-3;

		evaluate_1Minimum(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
	}

	@Test
	public void evaluate_1MinimumMoving() {
		final double direction = 1.0;
		final double mass = 1.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 0.0;
		final double v0 = 1.0;
		final double x = 1.0;
		final double v = v0;
		final double dt = 1.0;
		final double tolerance = 1E-3;

		evaluate_1Minimum(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
	}

	@Test
	public void evaluate_1MinimumTerms() {
		final double direction = 1.0;
		final double mass = 1.0;
		final int positionTerm = 1;
		final int velocityTerm = 0;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double x = 0.0;
		final double v = 0.0;
		final double dt = 1.0;
		final double tolerance = 1E-3;

		evaluate_1Minimum(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
	}

	@Test
	public void evaluate_1V() {
		final double direction = 1.0;
		final double mass = 1.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double x = 0.0;
		final double v = 2.0;
		final double dt = 1.0;
		final double eExpected = 0.5;
		final double dEDXExpected = -1.0;
		final double dEDVExpected = 0.5;
		final double tolerance = 1E-3;

		evaluate_1(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
				dEDVExpected, tolerance);
	}

	@Test
	public void evaluate_1V0() {
		final double direction = 1.0;
		final double mass = 1.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 0.0;
		final double v0 = 2.0;
		final double x = 0.0;
		final double v = 0.0;
		final double dt = 1.0;
		final double eExpected = 0.5;
		final double dEDXExpected = -1.0;
		final double dEDVExpected = 0.5;
		final double tolerance = 1E-3;

		evaluate_1(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
				dEDVExpected, tolerance);
	}

	@Test
	public void evaluate_1X() {
		final double direction = 1.0;
		final double mass = 1.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double x = 2.0;
		final double v = 0.0;
		final double dt = 1.0;
		final double eExpected = 2.0;
		final double dEDXExpected = 2.0;
		final double dEDVExpected = -1.0;
		final double tolerance = 1E-3;

		evaluate_1(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
				dEDVExpected, tolerance);
	}

	@Test
	public void evaluate_1X0() {
		final double direction = 1.0;
		final double mass = 1.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 2.0;
		final double v0 = 0.0;
		final double x = 0.0;
		final double v = 0.0;
		final double dt = 1.0;
		final double eExpected = 2.0;
		final double dEDXExpected = -2.0;
		final double dEDVExpected = 1.0;
		final double tolerance = 1E-3;

		evaluate_1(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
				dEDVExpected, tolerance);
	}

	@Test
	public void evaluate_1XDT() {
		final double direction = 1.0;
		final double mass = 1.0;
		final int positionTerm = 0;
		final int velocityTerm = 1;
		final double dedx0 = 0.0;
		final double dedv0 = 0.0;
		final double x0 = 0.0;
		final double v0 = 0.0;
		final double x = 2.0;
		final double v = 0.0;
		final double dt = 2.0;
		final double eExpected = 0.5;
		final double dEDXExpected = 0.5;
		final double dEDVExpected = -0.5;
		final double tolerance = 1E-3;

		evaluate_1(direction, mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
				dEDVExpected, tolerance);
	}
}
