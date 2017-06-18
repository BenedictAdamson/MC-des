package uk.badamson.mc.physics.kinematics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunctionTest;

/**
 * <p>
 * Unit tests for the class {@link PositionError}.
 * </p>
 */
public class PositionErrorTest {

	private static final double TOLERANCE_1 = Math.nextAfter(1.0, Double.POSITIVE_INFINITY) - 1.0;

	public static void assertInvariants(PositionError term) {
		ObjectTest.assertInvariants(term);// inherited
		TimeStepEnergyErrorFunctionTest.TermTest.assertInvariants(term);// inherited

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
		TimeStepEnergyErrorFunctionTest.TermTest.assertInvariants(term1, term2);// inherited
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
}
