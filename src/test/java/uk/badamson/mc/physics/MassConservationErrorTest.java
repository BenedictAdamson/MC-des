package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import uk.badamson.mc.math.ImmutableVector;

/**
 * <p>
 * Unit tests for the class {@link MassConservationError}.
 * </p>
 */
public class MassConservationErrorTest {

	private static final double SPECIFIC_ENERGY_REFERNCE_1 = 1.0;

	private static final double SPECIFIC_ENERGY_REFERNCE_2 = 1.0E3;

	public static void assertInvariants(MassConservationError term) {
		AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

		final int numberOfMassTransfers = term.getNumberOfMassTransfers();
		final double timeReference = term.getSpecificEnergyReference();

		assertTrue("numberOfMassTransfers not negative", 0 <= numberOfMassTransfers);

		assertTrue("timeReference is positive and finite", 0.0 < timeReference && Double.isFinite(timeReference));

		assertTrue("massTerm is not negative", 0 <= term.getMassTerm());
		for (int j = 0; j < numberOfMassTransfers; ++j) {
			assertTrue("advectionMassRateTerm is not negative", 0 <= term.getAdvectionMassRateTerm(j));
		}
	}

	public static void assertInvariants(MassConservationError term1, MassConservationError term2) {
		AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
	}

	private static MassConservationError constructor(double specificEnergyReference, int massTerm,
			boolean[] massTransferInto, int[] advectionMassRateTerm) {
		final MassConservationError term = new MassConservationError(specificEnergyReference, massTerm,
				massTransferInto, advectionMassRateTerm);

		assertInvariants(term);

		assertEquals("specificEnergyReference", specificEnergyReference, term.getSpecificEnergyReference(),
				Double.MIN_NORMAL);

		assertEquals("numberOfMassTransfers", massTransferInto.length, term.getNumberOfMassTransfers());

		assertEquals("massTerm", massTerm, term.getMassTerm());
		for (int j = 0; j < massTransferInto.length; ++j) {
			assertEquals("massTransferInto[" + j + "]", massTransferInto[j], term.isMassTransferInto(j));
			assertEquals("advectionMassRateTerm[" + j + "]", advectionMassRateTerm[j],
					term.getAdvectionMassRateTerm(j));
		}

		return term;
	}

	private static double evaluate(MassConservationError term, double[] dedx, ImmutableVector state0,
			ImmutableVector state, double dt) {
		final double e = AbstractTimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, state0, state, dt);// inherited

		assertInvariants(term);
		assertTrue("value is not negative", 0.0 <= e);

		return e;
	}

}
