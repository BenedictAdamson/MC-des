package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVector;

/**
 * <p>
 * Unit tests for the class {@link MomentumConservationError}.
 * </p>
 */
public class MomentumConservationErrorTest {

	public static void assertInvariants(MomentumConservationError term) {
		AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

		final int spaceDimension = term.getSpaceDimension();
		final int numberOfForces = term.getNumberOfForces();
		final int numberOfMassTransfers = term.getNumberOfMassTransfers();

		assertTrue("numberOfMassTransfers not negative", 0 <= numberOfMassTransfers);
		assertTrue("numberOfForces not negative", 0 <= numberOfForces);
		assertTrue("spaceDimension is positive", 0 < spaceDimension);

		assertTrue("massTerm is not negative", 0 <= term.getMassTerm());
		for (int i = 0; i < spaceDimension; ++i) {
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

	public static void assertInvariants(MomentumConservationError term1, MomentumConservationError term2) {
		AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
	}

	private static MomentumConservationError constructor(int massTerm, int[] velocityTerm, boolean[] massTransferInto,
			int[] advectionMassRateTerm, int[] advectionVelocityTerm, boolean[] forceOn, int[] forceTerm) {
		final MomentumConservationError term = new MomentumConservationError(massTerm, velocityTerm, massTransferInto,
				advectionMassRateTerm, advectionVelocityTerm, forceOn, forceTerm);

		assertInvariants(term);

		assertEquals("spaceDimension", velocityTerm.length, term.getSpaceDimension());
		assertEquals("numberOfMassTransfers", massTransferInto.length, term.getNumberOfMassTransfers());
		assertEquals("numberOfForces", forceOn.length, term.getNumberOfForces());

		assertEquals("massTerm", massTerm, term.getMassTerm());
		for (int i = 0; i < velocityTerm.length; ++i) {
			assertEquals("velocityTerm[" + i + "]", velocityTerm[i], term.getVelocityTerm(i));
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

	private static double evaluate(MomentumConservationError term, double[] dedx, ImmutableVector state0,
			ImmutableVector state, double dt) {
		final double e = AbstractTimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, state0, state, dt);// inherited

		assertInvariants(term);
		assertTrue("value is not negative", 0.0 <= e);

		return e;
	}

	private static void evaluate_1Advection(boolean massTransferInto, double m0, double v0, double mrate0, double u0,
			double m, double v, double mrate, double u, double dt, double dedmrate0, double dedu0, double expectedE,
			double expectedDedm, double expectedDedv, double expectedDedmrate, double expectedDedu) {
		final int massTerm = 0;
		final int[] velocityTerm = { 1 };
		final int[] advectionMassRateTerm = { 2 };
		final int[] advectionVelocityTerm = { 3 };
		final boolean[] forceOn = {};
		final int[] forceTerm = {};

		final double[] dedx = { 0.0, 0.0, dedmrate0, dedu0 };
		final ImmutableVector state0 = ImmutableVector.create(m0, v0, mrate0, u0);
		final ImmutableVector state = ImmutableVector.create(m, v, mrate, u);

		final MomentumConservationError term = new MomentumConservationError(massTerm, velocityTerm,
				new boolean[] { massTransferInto }, advectionMassRateTerm, advectionVelocityTerm, forceOn, forceTerm);

		final double e = evaluate(term, dedx, state0, state, dt);

		assertEquals("e", expectedE, e, 1E-8);
		assertEquals("dedm", expectedDedm, dedx[0], 1E-8);
		assertEquals("dedv", expectedDedv, dedx[1], 1E-8);
		assertEquals("dedmrate", expectedDedmrate, dedx[3], 1E-8);
		assertEquals("dedu", expectedDedu, dedx[4], 1E-8);
	}

	private static void evaluate_1Closed(double dedm0, double dedv0, double m0, double v0, double m, double v,
			double dt, double expectedE, double expectedDedm, double expectedDedv) {
		final int massTerm = 0;
		final int[] velocityTerm = { 1 };
		final boolean[] massTransferInto = {};
		final int[] advectionMassRateTerm = {};
		final int[] advectionVelocityTerm = {};
		final boolean[] forceOn = {};
		final int[] forceTerm = {};

		final double[] dedx = { dedm0, dedv0 };
		final ImmutableVector state0 = ImmutableVector.create(m0, v0);
		final ImmutableVector state = ImmutableVector.create(m, v);

		final MomentumConservationError term = new MomentumConservationError(massTerm, velocityTerm, massTransferInto,
				advectionMassRateTerm, advectionVelocityTerm, forceOn, forceTerm);

		final double e = evaluate(term, dedx, state0, state, dt);

		assertEquals("e", expectedE, e, 1E-8);
		assertEquals("dedm", expectedDedm, dedx[0], 1E-8);
		assertEquals("dedv", expectedDedv, dedx[1], 1E-8);
	}

	private static void evaluate_1Force(boolean forceOn, double m0, double v0, double f0, double m, double v, double f,
			double dt, double dedf0, double expectedE, double expectedDedm, double expectedDedv, double expectedDedf) {
		final int massTerm = 0;
		final int[] velocityTerm = { 1 };
		final boolean[] massTransferInto = {};
		final int[] advectionMassRateTerm = {};
		final int[] advectionVelocityTerm = {};
		final int[] forceTerm = { 2 };

		final double[] dedx = { 0.0, 0.0, dedf0 };
		final ImmutableVector state0 = ImmutableVector.create(m0, v0, f0);
		final ImmutableVector state = ImmutableVector.create(m, v, f);

		final MomentumConservationError term = new MomentumConservationError(massTerm, velocityTerm, massTransferInto,
				advectionMassRateTerm, advectionVelocityTerm, new boolean[] { forceOn }, forceTerm);

		final double e = evaluate(term, dedx, state0, state, dt);

		assertEquals("e", expectedE, e, 1E-8);
		assertEquals("dedm", expectedDedm, dedx[0], 1E-8);
		assertEquals("dedv", expectedDedv, dedx[1], 1E-8);
		assertEquals("dedf", expectedDedf, dedx[2], 1E-8);
	}

	@Test
	public void constructor_1A() {
		final int massTerm = 0;
		final int[] velocityTerm = { 2 };
		final boolean[] massTransferInto = {};
		final int[] advectionMassRateTerm = {};
		final int[] advectionVelocityTerm = {};
		final boolean[] forceOn = {};
		final int[] forceTerm = {};

		constructor(massTerm, velocityTerm, massTransferInto, advectionMassRateTerm, advectionVelocityTerm, forceOn,
				forceTerm);
	}

	@Test
	public void constructor_1B() {
		final int massTerm = 3;
		final int[] velocityTerm = { 5 };
		final boolean[] massTransferInto = { false };
		final int[] advectionMassRateTerm = { 11 };
		final int[] advectionVelocityTerm = { 13 };
		final boolean[] forceOn = {};
		final int[] forceTerm = {};

		constructor(massTerm, velocityTerm, massTransferInto, advectionMassRateTerm, advectionVelocityTerm, forceOn,
				forceTerm);
	}

	@Test
	public void constructor_1C() {
		final int massTerm = 0;
		final int[] velocityTerm = { 2 };
		final boolean[] massTransferInto = {};
		final int[] advectionMassRateTerm = {};
		final int[] advectionVelocityTerm = {};
		final boolean[] forceOn = { true };
		final int[] forceTerm = { 7 };

		constructor(massTerm, velocityTerm, massTransferInto, advectionMassRateTerm, advectionVelocityTerm, forceOn,
				forceTerm);
	}

	@Test
	public void evaluate_1AdvectionBase() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionDedmrate0() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 2.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDedmrate = 2.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionDedu0() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 2.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = 1.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionDt() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 2.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionM() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;

		final double m = 2.0;
		final double v = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 2.0;
		final double expectedDedm = 2.0;
		final double expectedDedv = 2.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -2.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionM0() {
		final boolean massTransferInto = true;

		final double m0 = 2.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionMRate() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double mrate = 2.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 2.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -2.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionMrate0() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 2.0;
		final double u0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionU() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double mrate = 1.0;
		final double u = 2.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.0;
		final double expectedDedm = 0.0;
		final double expectedDedv = 0.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = 0.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionU0() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 2.0;

		final double m = 1.0;
		final double v = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionV() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;

		final double m = 1.0;
		final double v = 2.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 2.0;
		final double expectedDedm = 2.0;
		final double expectedDedv = 2.0;
		final double expectedDedmrate = 2.0;
		final double expectedDedu = -2.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1AdvectionV0() {
		final boolean massTransferInto = true;

		final double m0 = 1.0;
		final double v0 = 2.0;
		final double mrate0 = 1.0;
		final double u0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double mrate = 1.0;
		final double u = 1.0;

		final double dt = 1.0;
		final double dedmrate0 = 0.0;
		final double dedu0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = 1.0;
		final double expectedDedv = 1.0;
		final double expectedDedmrate = 0.0;
		final double expectedDedu = -1.0;

		evaluate_1Advection(massTransferInto, m0, v0, mrate0, u0, m, v, mrate, u, dt, dedmrate0, dedu0, expectedE,
				expectedDedm, expectedDedv, expectedDedmrate, expectedDedu);
	}

	@Test
	public void evaluate_1ClosedBase() {
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.0;
		final double expectedDedm = 0.0;
		final double expectedDedv = 0.0;

		evaluate_1Closed(dedm0, dedv0, m0, v0, m, v, dt, expectedE, expectedDedm, expectedDedv);
	}

	@Test
	public void evaluate_1ClosedDedm0() {
		final double dedm0 = 2.0;
		final double dedv0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.0;
		final double expectedDedm = 2.0;
		final double expectedDedv = 0.0;

		evaluate_1Closed(dedm0, dedv0, m0, v0, m, v, dt, expectedE, expectedDedm, expectedDedv);
	}

	@Test
	public void evaluate_1ClosedDedv0() {
		final double dedm0 = 0.0;
		final double dedv0 = 2.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.0;
		final double expectedDedm = 0.0;
		final double expectedDedv = 2.0;

		evaluate_1Closed(dedm0, dedv0, m0, v0, m, v, dt, expectedE, expectedDedm, expectedDedv);
	}

	@Test
	public void evaluate_1ClosedDt() {
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double dt = 2.0;
		final double expectedE = 0.0;
		final double expectedDedm = 0.0;
		final double expectedDedv = 0.0;

		evaluate_1Closed(dedm0, dedv0, m0, v0, m, v, dt, expectedE, expectedDedm, expectedDedv);
	}

	@Test
	public void evaluate_1ClosedM() {
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double m = 2.0;
		final double v = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.25;
		final double expectedDedm = 0.375;
		final double expectedDedv = 1.0;

		evaluate_1Closed(dedm0, dedv0, m0, v0, m, v, dt, expectedE, expectedDedm, expectedDedv);
	}

	@Test
	public void evaluate_1ClosedM0() {
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double m0 = 2.0;
		final double v0 = 1.0;
		final double m = 1.0;
		final double v = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = -1.5;
		final double expectedDedv = -1.0;

		evaluate_1Closed(dedm0, dedv0, m0, v0, m, v, dt, expectedE, expectedDedm, expectedDedv);
	}

	@Test
	public void evaluate_1ClosedV() {
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 1.0;
		final double m = 1.0;
		final double v = 2.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = 1.5;
		final double expectedDedv = 1.0;

		evaluate_1Closed(dedm0, dedv0, m0, v0, m, v, dt, expectedE, expectedDedm, expectedDedv);
	}

	@Test
	public void evaluate_1ClosedV0() {
		final double dedm0 = 0.0;
		final double dedv0 = 0.0;
		final double m0 = 1.0;
		final double v0 = 2.0;
		final double m = 1.0;
		final double v = 1.0;
		final double dt = 1.0;
		final double expectedE = 0.5;
		final double expectedDedm = -1.5;
		final double expectedDedv = -1.0;

		evaluate_1Closed(dedm0, dedv0, m0, v0, m, v, dt, expectedE, expectedDedm, expectedDedv);
	}

	@Test
	public void evaluate_1ForceBase() {
		final boolean forceOn = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double f0 = 0.0;

		final double m = 1.0;
		final double v = 1.0;
		final double f = 1.0;

		final double dt = 1.0;
		final double dedf0 = 0.0;

		final double expectedE = 0.125;
		final double expectedDedm = -0.625;
		final double expectedDedv = -0.5;
		final double expectedDedf = 0.25;

		evaluate_1Force(forceOn, m0, v0, f0, m, v, f, dt, dedf0, expectedE, expectedDedm, expectedDedv, expectedDedf);
	}

	@Test
	public void evaluate_1ForceDedf0() {
		final boolean forceOn = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double f0 = 0.0;

		final double m = 1.0;
		final double v = 1.0;
		final double f = 1.0;

		final double dt = 1.0;
		final double dedf0 = 1.0;

		final double expectedE = 0.125;
		final double expectedDedm = -0.625;
		final double expectedDedv = -0.5;
		final double expectedDedf = 1.25;

		evaluate_1Force(forceOn, m0, v0, f0, m, v, f, dt, dedf0, expectedE, expectedDedm, expectedDedv, expectedDedf);
	}

	@Test
	public void evaluate_1ForceDt() {
		final boolean forceOn = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double f0 = 0.0;

		final double m = 1.0;
		final double v = 1.0;
		final double f = 1.0;

		final double dt = 2.0;
		final double dedf0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = -1.5;
		final double expectedDedv = -1.0;
		final double expectedDedf = 1.0;

		evaluate_1Force(forceOn, m0, v0, f0, m, v, f, dt, dedf0, expectedE, expectedDedm, expectedDedv, expectedDedf);
	}

	@Test
	public void evaluate_1ForceF() {
		final boolean forceOn = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double f0 = 0.0;

		final double m = 1.0;
		final double v = 1.0;
		final double f = 2.0;

		final double dt = 1.0;
		final double dedf0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = -1.5;
		final double expectedDedv = -1.0;
		final double expectedDedf = 0.5;

		evaluate_1Force(forceOn, m0, v0, f0, m, v, f, dt, dedf0, expectedE, expectedDedm, expectedDedv, expectedDedf);
	}

	@Test
	public void evaluate_1ForceF0() {
		final boolean forceOn = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double f0 = 1.0;

		final double m = 1.0;
		final double v = 1.0;
		final double f = 1.0;

		final double dt = 1.0;
		final double dedf0 = 0.0;

		final double expectedE = 0.5;
		final double expectedDedm = -1.5;
		final double expectedDedv = -1.0;
		final double expectedDedf = 0.5;

		evaluate_1Force(forceOn, m0, v0, f0, m, v, f, dt, dedf0, expectedE, expectedDedm, expectedDedv, expectedDedf);
	}

	@Test
	public void evaluate_1ForceForceOn() {
		final boolean forceOn = false;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double f0 = 0.0;

		final double m = 1.0;
		final double v = 1.0;
		final double f = 1.0;

		final double dt = 1.0;
		final double dedf0 = 0.0;

		final double expectedE = 0.125;
		final double expectedDedm = 0.375;
		final double expectedDedv = 0.5;
		final double expectedDedf = 0.25;

		evaluate_1Force(forceOn, m0, v0, f0, m, v, f, dt, dedf0, expectedE, expectedDedm, expectedDedv, expectedDedf);
	}

	@Test
	public void evaluate_1ForceM() {
		final boolean forceOn = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double f0 = 0.0;

		final double m = 2.0;
		final double v = 1.0;
		final double f = 1.0;

		final double dt = 1.0;
		final double dedf0 = 0.0;

		final double expectedE = 0.0625;
		final double expectedDedm = 0.21875;
		final double expectedDedv = 0.5;
		final double expectedDedf = -0.125;

		evaluate_1Force(forceOn, m0, v0, f0, m, v, f, dt, dedf0, expectedE, expectedDedm, expectedDedv, expectedDedf);
	}

	@Test
	public void evaluate_1ForceM0() {
		final boolean forceOn = true;

		final double m0 = 2.0;
		final double v0 = 1.0;
		final double f0 = 0.0;

		final double m = 1.0;
		final double v = 1.0;
		final double f = 1.0;

		final double dt = 1.0;
		final double dedf0 = 0.0;

		final double expectedE = 1.125;
		final double expectedDedm = -2.625;
		final double expectedDedv = -1.5;
		final double expectedDedf = 0.75;

		evaluate_1Force(forceOn, m0, v0, f0, m, v, f, dt, dedf0, expectedE, expectedDedm, expectedDedv, expectedDedf);
	}

	@Test
	public void evaluate_1ForceV() {
		final boolean forceOn = true;

		final double m0 = 1.0;
		final double v0 = 1.0;
		final double f0 = 0.0;

		final double m = 1.0;
		final double v = 2.0;
		final double f = 1.0;

		final double dt = 1.0;
		final double dedf0 = 0.0;

		final double expectedE = 0.125;
		final double expectedDedm = 0.875;
		final double expectedDedv = 0.5;
		final double expectedDedf = -0.25;

		evaluate_1Force(forceOn, m0, v0, f0, m, v, f, dt, dedf0, expectedE, expectedDedm, expectedDedv, expectedDedf);
	}

	@Test
	public void evaluate_1ForceV0() {
		final boolean forceOn = true;

		final double m0 = 1.0;
		final double v0 = 2.0;
		final double f0 = 0.0;

		final double m = 1.0;
		final double v = 1.0;
		final double f = 1.0;

		final double dt = 1.0;
		final double dedf0 = 0.0;

		final double expectedE = 1.125;
		final double expectedDedm = -2.625;
		final double expectedDedv = -1.5;
		final double expectedDedf = 0.75;

		evaluate_1Force(forceOn, m0, v0, f0, m, v, f, dt, dedf0, expectedE, expectedDedm, expectedDedv, expectedDedf);
	}

}
