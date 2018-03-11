package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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

    private static Quaternion plus(Quaternion q, Quaternion that) {
        final Quaternion sum = q.plus(that);

        assertInvariants(sum);
        assertInvariants(sum, q);
        assertInvariants(sum, that);

        return sum;
    }

    private static void plus_0(double a, double b, double c, double d) {
        final Quaternion q = new Quaternion(a, b, c, d);

        final Quaternion sum = plus(q, Quaternion.ZERO);

        assertEquals("Unchanged", q, sum);
    }

    private static void plus_negative(double a, double b, double c, double d) {
        final Quaternion p = new Quaternion(a, b, c, d);
        final Quaternion m = new Quaternion(-a, -b, -c, -d);

        final Quaternion sum = plus(p, m);

        assertEquals("sum", Quaternion.ZERO, sum);
    }

    private static Quaternion scale(Quaternion x, double f) {
        final Quaternion scaled = x.scale(f);

        assertNotNull("Not null, result", scaled);
        assertInvariants(scaled);
        assertInvariants(x, scaled);

        return scaled;
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

    @Test
    public void plus_0A() {
        plus_0(1, 2, 3, 4);
    }

    @Test
    public void plus_0B() {
        plus_0(-9, -8, -7, -6);
    }

    @Test
    public void plus_negativeA() {
        plus_negative(1, 2, 3, 4);
    }

    /////

    @Test
    public void plus_negativeB() {
        plus_negative(-9, -8, -7, -6);
    }

    @Test
    public void scale_0A() {
        final Quaternion scaled = scale(Quaternion.ZERO, 0.0);

        assertEquals("scaled", Quaternion.ZERO, scaled);
    }

    @Test
    public void scale_0B() {
        final Quaternion scaled = scale(Quaternion.ZERO, 1.0);

        assertEquals("scaled", Quaternion.ZERO, scaled);
    }

    @Test
    public void scale_1A() {
        final Quaternion one = new Quaternion(1, 1, 1, 1);

        final Quaternion scaled = scale(one, 1);

        assertEquals("scaled", one, scaled);
    }

    @Test
    public void scale_1B() {
        final Quaternion one = new Quaternion(1, 1, 1, 1);

        final Quaternion scaled = scale(one, -2.0);

        assertEquals("scaled a", -2.0, scaled.getA(), Double.MIN_NORMAL);
        assertEquals("scaled b", -2.0, scaled.getB(), Double.MIN_NORMAL);
        assertEquals("scaled c", -2.0, scaled.getC(), Double.MIN_NORMAL);
        assertEquals("scaled d", -2.0, scaled.getD(), Double.MIN_NORMAL);
    }

    @Test
    public void scale_B() {
        final Quaternion original = new Quaternion(2, 3, 4, 5);

        final Quaternion scaled = scale(original, 4);

        assertEquals("scaled a", 8, scaled.getA(), Double.MIN_NORMAL);
        assertEquals("scaled b", 12, scaled.getB(), Double.MIN_NORMAL);
        assertEquals("scaled c", 16, scaled.getC(), Double.MIN_NORMAL);
        assertEquals("scaled d", 20, scaled.getD(), Double.MIN_NORMAL);
    }

}
