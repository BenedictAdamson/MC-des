package uk.badamson.mc.physics.kinematics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.math.ImmutableVector;

/**
 * <p>
 * Unit tests of the class {@link Orientation3}.
 * </p>
 */
public class Orientation3Test {

	public static void assertInvariants(Orientation3 orientation) {
		ObjectTest.assertInvariants(orientation);// inherited

		final ImmutableVector e1 = orientation.getE1();
		final ImmutableVector e2 = orientation.getE2();
		final ImmutableVector e3 = orientation.getE3();

		assertNotNull("Not null, e1", e1);// guard
		assertNotNull("Not null, e2", e2);// guard
		assertNotNull("Not null, e3", e3);// guard

		assertEquals("The e1 vector is 3 dimensional.", 3, e1.getDimension());// guard
		assertEquals("The e2 vector is 3 dimensional.", 3, e2.getDimension());// guard
		assertEquals("The e3 vector is 3 dimensional.", 3, e3.getDimension());// guard

		assertEquals("The e1 vector has unit magnitude.", 1.0, e1.magnitude(), Double.MIN_NORMAL);
		assertEquals("The e2 vector has unit magnitude.", 1.0, e2.magnitude(), Double.MIN_NORMAL);
		assertEquals("The e3 vector has unit magnitude.", 1.0, e3.magnitude(), Double.MIN_NORMAL);
		assertEquals("The e1 vector is orthogonal to vector e2.", 0.0, e1.dot(e2), Double.MIN_NORMAL);
		assertEquals("The e1 vector is orthogonal to vector e3.", 0.0, e1.dot(e3), Double.MIN_NORMAL);
		assertEquals("The e2 vector is orthogonal to vector e3.", 0.0, e2.dot(e3), Double.MIN_NORMAL);
	}

	public static void assertInvariants(Orientation3 orientation1, Orientation3 orientation2) {
		ObjectTest.assertInvariants(orientation1, orientation2);// inherited
	}

	private static Orientation3 createFromOrthogonalUnitBasisVectors(ImmutableVector e1, ImmutableVector e2,
			ImmutableVector e3) {
		final Orientation3 orientation = Orientation3.createFromOrthogonalUnitBasisVectors(e1, e2, e3);

		assertNotNull("Not null, result", orientation);// guard
		assertInvariants(orientation);
		assertEquals("e1", e1, orientation.getE1());
		assertEquals("e2", e2, orientation.getE2());
		assertEquals("e3", e3, orientation.getE3());

		return orientation;
	}

	@Test
	public void createFromOrthogonalUnitBasisVectors_A() {
		final ImmutableVector e1 = ImmutableVector.create(1, 0, 0);
		final ImmutableVector e2 = ImmutableVector.create(0, 1, 0);
		final ImmutableVector e3 = ImmutableVector.create(0, 0, 1);

		createFromOrthogonalUnitBasisVectors(e1, e2, e3);
	}

	@Test
	public void createFromOrthogonalUnitBasisVectors_B() {
		final ImmutableVector e1 = ImmutableVector.create(0, 1, 0);
		final ImmutableVector e2 = ImmutableVector.create(0, 0, 1);
		final ImmutableVector e3 = ImmutableVector.create(1, 0, 0);

		createFromOrthogonalUnitBasisVectors(e1, e2, e3);
	}

	@Test
	public void createFromOrthogonalUnitBasisVectors_C() {
		final double f = 1.0 / Math.sqrt(2.0);
		final ImmutableVector e1 = ImmutableVector.create(f, f, 0);
		final ImmutableVector e2 = ImmutableVector.create(-f, f, 0);
		final ImmutableVector e3 = ImmutableVector.create(0, 0, 1);

		createFromOrthogonalUnitBasisVectors(e1, e2, e3);
	}

	@Test
	public void createFromOrthogonalUnitBasisVectors_D() {
		final double f = 1.0 / Math.sqrt(2.0);
		final ImmutableVector e1 = ImmutableVector.create(1, 0, 0);
		final ImmutableVector e2 = ImmutableVector.create(0, f, f);
		final ImmutableVector e3 = ImmutableVector.create(0, -f, f);

		createFromOrthogonalUnitBasisVectors(e1, e2, e3);
	}
}
