package uk.badamson.mc;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * Unit tests for the {@link Object} class.
 * </p>
 */
public final class ObjectTest {

    public static void assertInvariants(Object object) {
        assert object != null;
        assertEquals(object, object, "An object is always equivalent to itself");
        assertFalse(object.equals(null), "An object is never equivalent to null");
    }

    public static void assertInvariants(Object object1, Object object2) {
        assert object1 != null;
        assert object2 != null;
        final boolean equals = object1.equals(object2);
        assertEquals(equals, object2.equals(object1), "Equality is symmetric");
        assertFalse(equals && object1.hashCode() != object2.hashCode(), "hashCode() is consistent with equals()");
    }
}
