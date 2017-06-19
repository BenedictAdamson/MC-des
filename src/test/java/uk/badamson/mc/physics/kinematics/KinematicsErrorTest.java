package uk.badamson.mc.physics.kinematics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTermTest;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunctionTermTest;

/**
 * <p>
 * Unit tests for classes derived from the {@link KinematicsError} abstract base
 * class.
 * </p>
 */
public class KinematicsErrorTest {

	private static final double TOLERANCE_1 = Math.nextAfter(1.0, Double.POSITIVE_INFINITY) - 1.0;

	public static void assertInvariants(KinematicsError term) {
		AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

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

	public static void assertInvariants(KinematicsError term1, KinematicsError term2) {
		ObjectTest.assertInvariants(term1, term2);// inherited
		TimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
	}

	public static double evaluate(KinematicsError term, double[] dedx, ImmutableVector x0, ImmutableVector x,
			double dt) {
		final double e = AbstractTimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, x0, x, dt);// inherited

		assertInvariants(term);

		return e;
	}
}
