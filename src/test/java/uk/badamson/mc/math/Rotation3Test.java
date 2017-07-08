package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Unit tests for the class {@link Rotation3}.
 * </p>
 */
public class Rotation3Test {

	public static void assertInvariants(Rotation3 rotation) {
		ImmutableMatrixTest.assertInvariants(rotation);// inherited

		assertEquals("rows", 3, rotation.getRows());
		assertEquals("columns", 3, rotation.getColumns());
	}

	public static void assertInvariants(Rotation3 rotation1, Rotation3 rotation2) {
		ImmutableMatrixTest.assertInvariants(rotation1, rotation2);// inherited
	}
}
