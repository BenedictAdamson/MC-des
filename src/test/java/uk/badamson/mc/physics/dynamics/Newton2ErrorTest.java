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
}
