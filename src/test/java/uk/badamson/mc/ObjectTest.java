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
	final boolean equals = object1.equals(object2);
	assertEquals("Equality is symmetric", equals, object2.equals(object1));
	assertFalse("hashCode() is consistent with equals()", equals && object1.hashCode() != object2.hashCode());
    }
}
