package uk.badamson.mc.physics.dynamics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTermTest;

/**
 * <p>
 * Unit tests for the class {@link Newton2Error}.
 * </p>
 */
public class Newton2ErrorTest {

	private static final double MASS_REFERENCE_1 = 1.0;

	private static final double MASS_REFERENCE_2 = 12.0;

	private static final double TIME_REFERNCE_1 = 1.0;

	private static final double TIME_REFERNCE_2 = 1.0E3;

	public static void assertInvariants(Newton2Error term) {
		AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

		final int spaceDimension = term.getSpaceDimension();
		final int numberOfForces = term.getNumberOfForces();
		final int numberOfMassTransfers = term.getNumberOfMassTransfers();
		final double massReference = term.getMassReference();
		final double timeReference = term.getTimeReference();

		assertTrue("numberOfMassTransfers not negative", 0 <= numberOfMassTransfers);
		assertTrue("numberOfForces not negative", 0 <= numberOfForces);
		assertTrue("spaceDimension is positive", 0 < spaceDimension);

		assertTrue("massReference is positive and finite", 0.0 < massReference && Double.isFinite(massReference));
		assertTrue("timeReference is positive and finite", 0.0 < timeReference && Double.isFinite(timeReference));

		assertTrue("massTerm is not negative", 0 <= term.getMassTerm());
		for (int i = 0; i < spaceDimension; ++i) {
			assertTrue("accelerationTerm is not negative", 0 <= term.getAccelerationTerm(i));
			assertTrue("velocityTerm is not negative", 0 <= term.getVelocityTerm(i));
			for (int j = 0; j < numberOfMassTransfers; ++j) {
				assertTrue("advectionVelocityTerm is not negative", 0 <= term.getAdvectionVelocityTerm(j, i));
			}
			for (int k = 0; k < numberOfForces; ++k) {
				assertTrue("forceTerm is not negative", 0 <= term.getForceTerm(k, i));
			}
		}
		for (int j = 0; j < numberOfMassTransfers; ++j) {
			assertTrue("advectionMassRateTerm is not negative", 0 <= term.getAdvectionMassRateTerm(j));
		}
	}

	public static void assertInvariants(Newton2Error term1, Newton2Error term2) {
		AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
	}

	private static Newton2Error constructor(double massReference, double timeReference, int massTerm,
			int[] velocityTerm, int[] accelerationTerm, boolean[] massTransferInto, int[] advectionMassRateTerm,
			int[] advectionVelocityTerm, boolean[] forceOn, int[] forceTerm) {
		final Newton2Error term = new Newton2Error(massReference, timeReference, massTerm, velocityTerm,
				accelerationTerm, massTransferInto, advectionMassRateTerm, advectionVelocityTerm, forceOn, forceTerm);

		assertInvariants(term);

		assertEquals("massReference", massReference, term.getMassReference(), Double.MIN_NORMAL);
		assertEquals("timeReference", timeReference, term.getTimeReference(), Double.MIN_NORMAL);

		assertEquals("spaceDimension", velocityTerm.length, term.getSpaceDimension());
		assertEquals("numberOfMassTransfers", massTransferInto.length, term.getNumberOfMassTransfers());
		assertEquals("numberOfForces", forceOn.length, term.getNumberOfForces());

		assertEquals("massTerm", massTerm, term.getMassTerm());
		for (int i = 0; i < velocityTerm.length; ++i) {
			assertEquals("velocityTerm[" + i + "]", velocityTerm[i], term.getVelocityTerm(i));
			assertEquals("accelerationTerm[" + i + "]", accelerationTerm[i], term.getAccelerationTerm(i));
		}
		for (int j = 0; j < massTransferInto.length; ++j) {
			assertEquals("massTransferInto[" + j + "]", massTransferInto[j], term.isMassTransferInto(j));
			assertEquals("advectionMassRateTerm[" + j + "]", advectionMassRateTerm[j],
					term.getAdvectionMassRateTerm(j));
			for (int i = 0; i < velocityTerm.length; ++i) {
				assertEquals("advectionVelocityTerm[" + j + "," + i + "]",
						advectionVelocityTerm[j * velocityTerm.length + i], term.getAdvectionVelocityTerm(j, i));
			}
		}
		for (int k = 0; k < forceOn.length; ++k) {
			assertEquals("forceOn[" + k + "]", forceOn[k], term.isForceOn(k));
			for (int i = 0; i < velocityTerm.length; ++i) {
				assertEquals("forceTerm[" + k + "," + i + "]", forceTerm[k * velocityTerm.length + i],
						term.getForceTerm(k, i));
			}
		}

		return term;
	}

	private static double evaluate(Newton2Error term, double[] dedx, ImmutableVector state0, ImmutableVector state,
			double dt) {
		final double e = AbstractTimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, state0, state, dt);// inherited

		assertInvariants(term);
		assertTrue("value is not negative", 0.0 <= e);

		return e;
	}

	private static void evaluate_1Advection(double massReference, double timeReference, boolean massTransferInto,
			double m0, double v0, double mrate0, double u0, double a0, double m, double v, double a, double mrate,
			double u, double dt, double dedmrate0, double dedu0, double expectedE, double expectedDedm,
			double expectedDedv, double expectedDeda, double expectedDedmrate, double expectedDedu) {
		final int massTerm = 0;
		final int[] velocityTerm = { 1 };
		final int[] accelerationTerm = { 2 };
		final int[] advectionMassRateTerm = { 3 };
		final int[] advectionVelocityTerm = { 4 };
		final boolean[] forceOn = {};
		final int[] forceTerm = {};

		final double[] dedx = { 0.0, 0.0, 0.0, dedmrate0, dedu0 };
		final ImmutableVector state0 = ImmutableVector.create(m0, v0, a0, mrate0, u0);
		final ImmutableVector state = ImmutableVector.create(m, v, a, mrate, u);

		final Newton2Error term = new Newton2Error(massReference, timeReference, massTerm, velocityTerm,
				accelerationTerm, new boolean[] { massTransferInto }, advectionMassRateTerm, advectionVelocityTerm,
				forceOn, forceTerm);

		final double e = evaluate(term, dedx, state0, state, dt);

		assertEquals("e", expectedE, e, 1E-8);
		assertEquals("dedm", expectedDedm, dedx[0], 1E-8);
		assertEquals("dedv", expectedDedv, dedx[1], 1E-8);
		assertEquals("deda", expectedDeda, dedx[2], 1E-8);
		assertEquals("dedmrate", expectedDedmrate, dedx[3], 1E-8);
		assertEquals("dedu", expectedDedu, dedx[4], 1E-8);
	}

	private static void evaluate_1Closed(double massReference, double timeReference, double dedm0, double dedv0,
			double deda0, double m0, double v0, double a0, double m, double v, double a, double dt, double expectedE,
			double expectedDedm, double expectedDedv, double expectedDeda) {
		final int massTerm = 0;
		final int[] velocityTerm = { 1 };
		final int[] accelerationTerm = { 2 };
		final boolean[] massTransferInto = {};
		final int[] advectionMassRateTerm = {};
		final int[] advectionVelocityTerm = {};
		final boolean[] forceOn = {};
		final int[] forceTerm = {};

		final double[] dedx = { dedm0, dedv0, deda0 };
		final ImmutableVector state0 = ImmutableVector.create(m0, v0, a0);
		final ImmutableVector state = ImmutableVector.create(m, v, a);

		final Newton2Error term = new Newton2Error(massReference, timeReference, massTerm, velocityTerm,
				accelerationTerm, massTransferInto, advectionMassRateTerm, advectionVelocityTerm, forceOn, forceTerm);

		final double e = evaluate(term, dedx, state0, state, dt);

		assertEquals("e", expectedE, e, 1E-8);
		assertEquals("dedm", expectedDedm, dedx[0], 1E-8);
		assertEquals("dedv", expectedDedv, dedx[1], 1E-8);
		assertEquals("deda", expectedDeda, dedx[2], 1E-8);
	}

	@Test
	public void constructor_1A() {
		final int massTerm = 0;
		final int[] velocityTerm = { 2 };
		final int[] accelerationTerm = { 3 };
		final boolean[] massTransferInto = {};
		final int[] advectionMassRateTerm = {};
		final int[] advectionVelocityTerm = {};
		final boolean[] forceOn = {};
		final int[] forceTerm = {};

		constructor(MASS_REFERENCE_1, TIME_REFERNCE_1, massTerm, velocityTerm, accelerationTerm, massTransferInto,
				advectionMassRateTerm, advectionVelocityTerm, forceOn, forceTerm);
	}

	@Test
	public void constructor_1B() {
		final int massTerm = 3;
		final int[] velocityTerm = { 5 };
		final int[] accelerationTerm = { 7 };
		final boolean[] massTransferInto = { false };
		final int[] advectionMassRateTerm = { 11 };
		final int[] advectionVelocityTerm = { 13 };
		final boolean[] forceOn = {};
		final int[] forceTerm = {};

		constructor(MASS_REFERENCE_2, TIME_REFERNCE_2, massTerm, velocityTerm, accelerationTerm, massTransferInto,
				advectionMassRateTerm, advectionVelocityTerm, forceOn, forceTerm);
	}

	@Test
	public void constructor_1C() {
		final int massTerm = 0;
		final int[] velocityTerm = { 2 };
		final int[] accelerationTerm = { 3 };
		final boolean[] massTransferInto = {};
		final int[] advectionMassRateTerm = {};
		final int[] advectionVelocityTerm = {};
		final boolean[] forceOn = { true };
		final int[] forceTerm = { 7 };

		constructor(MASS_REFERENCE_1, TIME_REFERNCE_1, massTerm, velocityTerm, accelerationTerm, massTransferInto,
				advectionMassRateTerm, advectionVelocityTerm, forceOn, forceTerm);
	}

	@Test
	public void evaluate_1AdvectionA() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 2.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 2.0;
		final double expectedDedm = 4.0;
		final double expectedDedv = 2.0;
		final double expectedDeda = 2.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -2.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionA0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 2.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionBase() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionDedmrate0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 2.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 2.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionDedu0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 2.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = 1.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionDt() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 2.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionM() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 2.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 2.0;
		final double expectedDedm = 2.0;
		final double expectedDedv = 2.0;
		final double expectedDeda = 4.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -2.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionM0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 2.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionMassReference() {
		final double massReference = 2.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.0625;
		final double expectedDedm = 0.25;
		final double expectedDedv = 0.25;
		final double expectedDeda = 0.25;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -0.25;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionMassTransferInto() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = false;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = -1.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = 1.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionMRate() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 2.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 2.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -2.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionMrate0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 2.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionTimeReference() {
		final double massReference = 1.0;
		final double timeReference = 2.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 2.0;
		final double expectedDedm = 4.0;
		final double expectedDedv = 4.0;
		final double expectedDeda = 4.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -4.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionU() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 2.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.0;
		final double expectedDedm = 0.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 0.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = 0.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionU0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 2.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionV() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 2.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 2.0;
		final double expectedDedm = 2.0;
		final double expectedDedv = 2.0;
		final double expectedDeda = 2.0;
		final double expectedDedmrate = 2.0;
		final double expectedDedu = -2.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionV0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 2.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;
		final double a0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDeda = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massReference, timeReference, massTransferInto, m0, v0, mrate0, u0, a0, m, v, a, mrate, u,
				dt, dedmrate0, dedu0, expectedE, expectedDedm, expectedDedv, expectedDeda, expectedDedmrate,
				expectedDedu);
	}

	@Test
	public void evaluate_1ClosedA() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 2.0;
		final double dt = 1.0;
		final double expectedE = 2.0;
		final double expectedDedm = 4.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 2.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedA0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 2.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 1.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedBase() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 1.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedDeda0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 2.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 3.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedDedm0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 2.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = 3.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 1.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedDedv0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 2.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 2.0;
		final double expectedDeda = 1.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedDt() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 2.0;
		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 1.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedM() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 2.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 2.0;
		final double expectedDedm = 2.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 4.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedM0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 2.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 1.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedTimeReference() {
		final double massReference = 1.0;
		final double timeReference = 2.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 2.0;
		final double expectedDedm = 4.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 4.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedV() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 2.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 1.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1ClosedV0() {
		final double massReference = 1.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 2.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 0.0;
		final double expectedDeda = 1.0;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

	@Test
	public void evaluate_1MassReference() {
		final double massReference = 2.0;
		final double timeReference = 1.0;
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double deda0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double a0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double a = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.0625;
		final double expectedDedm = 0.25;
		final double expectedDedv = 0.0;
		final double expectedDeda = 0.25;

		evaluate_1Closed(massReference, timeReference, dedm0, dedv0, deda0, m0, v0, a0, m, v, a, dt, expectedE,
				expectedDedm, expectedDedv, expectedDeda);
	}

}
