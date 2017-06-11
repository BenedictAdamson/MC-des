package uk.badamson.mc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * <p>
 * Unit tests for the {@link Object} class.
 * </p>
 */
public final class ObjectTest {

	public static void assertInvariants(Object object) {
		assert object != null;
		assertEquals("An object is always equivalent to itself", object, object);
		assertFalse("An object is never equivalent to null", object.equals(null));
	}

	public static void assertInvariants(Object object1, Object object2) {
		assert object1 != null;
		assert object2 != null;
		assertEquals("Equality is symmetric", object1.equals(object2), object2.equals(object1));
	}
}
