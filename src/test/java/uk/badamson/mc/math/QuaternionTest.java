package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests of the {@link Quaternion} class.
 * </p>
 */
public class QuaternionTest {

    public static void assertInvariants(Quaternion q) {
        ObjectTest.assertInvariants(q);// inherited
    }

    public static void assertInvariants(Quaternion q1, Quaternion q2) {
        ObjectTest.assertInvariants(q1, q2);// inherited

        final boolean equals = q1.equals(q2);
        assertFalse("Equality requires equivalent attributes, a",
                equals && Double.doubleToLongBits(q1.getA()) != Double.doubleToLongBits(q2.getA()));
        assertFalse("Equality requires equivalent attributes, b",
                equals && Double.doubleToLongBits(q1.getB()) != Double.doubleToLongBits(q2.getB()));
        assertFalse("Equality requires equivalent attributes, c",
                equals && Double.doubleToLongBits(q1.getC()) != Double.doubleToLongBits(q2.getC()));
        assertFalse("Equality requires equivalent attributes, d",
                equals && Double.doubleToLongBits(q1.getD()) != Double.doubleToLongBits(q2.getD()));
    }

    private static Quaternion constructor(double a, double b, double c, double d) {
        final Quaternion q = new Quaternion(a, b, c, d);

        assertInvariants(q);
        assertEquals("a", Double.doubleToLongBits(a), Double.doubleToLongBits(q.getA()));
        assertEquals("b", Double.doubleToLongBits(b), Double.doubleToLongBits(q.getB()));
        assertEquals("c", Double.doubleToLongBits(c), Double.doubleToLongBits(q.getC()));
        assertEquals("d", Double.doubleToLongBits(d), Double.doubleToLongBits(q.getD()));

        return q;
    }

    private static void constructor_2equivalent(double a, double b, double c, double d) {
        final Quaternion q1 = new Quaternion(a, b, c, d);
        final Quaternion q2 = new Quaternion(a, b, c, d);

        assertInvariants(q1, q2);
        assertEquals(q1, q2);
    }

    @Test
    public void constructor_2differentA() {
        final double b = 2.0;
        final double c = 3.0;
        final double d = 4.0;
        final Quaternion q1 = new Quaternion(1.0, b, c, d);
        final Quaternion q2 = new Quaternion(-1.0, b, c, d);

        assertInvariants(q1, q2);
        assertNotEquals(q1, q2);
    }

    @Test
    public void constructor_2differentB() {
        final double a = 1.0;
        final double c = 3.0;
        final double d = 4.0;
        final Quaternion q1 = new Quaternion(a, 2.0, c, d);
        final Quaternion q2 = new Quaternion(a, -2.0, c, d);

        assertInvariants(q1, q2);
        assertNotEquals(q1, q2);
    }

    @Test
    public void constructor_2differentC() {
        final double a = 1.0;
        final double b = 2.0;
        final double d = 4.0;
        final Quaternion q1 = new Quaternion(a, b, 3.0, d);
        final Quaternion q2 = new Quaternion(a, b, -3.0, d);

        assertInvariants(q1, q2);
        assertNotEquals(q1, q2);
    }

    @Test
    public void constructor_2differentD() {
        final double a = 1.0;
        final double b = 2.0;
        final double c = 3.0;
        final Quaternion q1 = new Quaternion(a, b, c, 4.0);
        final Quaternion q2 = new Quaternion(a, b, c, -4.0);

        assertInvariants(q1, q2);
        assertNotEquals(q1, q2);
    }

    @Test
    public void constructor_2equivalentA() {
        constructor_2equivalent(1.0, 2.0, 3.0, 4.0);
    }

    @Test
    public void constructor_2equivalentB() {
        constructor_2equivalent(9.0, 7.0, 6.0, 5.0);
    }

    @Test
    public void constructor_A() {
        constructor(1.0, 2.0, 3.0, 4.0);
    }

    @Test
    public void constructor_B() {
        constructor(9.0, 7.0, 6.0, 5.0);
    }
}
