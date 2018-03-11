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

    private static Quaternion conjugate(Quaternion q) {
        final Quaternion c = q.conjugate();

        assertNotNull("Not null, conjugate", c);// guard
        assertInvariants(q);
        assertInvariants(c);
        assertInvariants(q, c);

        assertEquals("Conjugation is an involution (self inverse)", q, c.conjugate());
        assertEquals("conjugate a", q.getA(), c.getA(), Double.MIN_NORMAL);
        assertEquals("conjugate b", -q.getB(), c.getB(), Double.MIN_NORMAL);
        assertEquals("conjugate c", -q.getC(), c.getC(), Double.MIN_NORMAL);
        assertEquals("conjugate d", -q.getD(), c.getD(), Double.MIN_NORMAL);

        return c;
    }

    private static Quaternion constructor(double a, double b, double c, double d) {
        final Quaternion q = Quaternion.create(a, b, c, d);

        assertInvariants(q);
        assertEquals("a", Double.doubleToLongBits(a), Double.doubleToLongBits(q.getA()));
        assertEquals("b", Double.doubleToLongBits(b), Double.doubleToLongBits(q.getB()));
        assertEquals("c", Double.doubleToLongBits(c), Double.doubleToLongBits(q.getC()));
        assertEquals("d", Double.doubleToLongBits(d), Double.doubleToLongBits(q.getD()));

        return q;
    }

    private static void constructor_2equivalent(double a, double b, double c, double d) {
        final Quaternion q1 = Quaternion.create(a, b, c, d);
        final Quaternion q2 = Quaternion.create(a, b, c, d);

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
        final Quaternion q = Quaternion.create(a, b, c, d);

        final Quaternion sum = plus(q, Quaternion.ZERO);

        assertEquals("Unchanged", q, sum);
    }

    private static void plus_negative(double a, double b, double c, double d) {
        final Quaternion p = Quaternion.create(a, b, c, d);
        final Quaternion m = Quaternion.create(-a, -b, -c, -d);

        final Quaternion sum = plus(p, m);

        assertEquals("sum", Quaternion.ZERO, sum);
    }

    private static Quaternion product(Quaternion q, Quaternion that) {
        final Quaternion p = q.product(that);

        assertNotNull("product", p);// guard
        assertInvariants(q);
        assertInvariants(that);
        assertInvariants(p);
        assertInvariants(p, q);
        assertInvariants(p, that);

        return p;
    }

    private static void product_0(Quaternion q) {
        final Quaternion p = product(q, Quaternion.ZERO);

        assertEquals("product a", Quaternion.ZERO, p);
    }

    private static void product_a(Quaternion q, double a) {
        final Quaternion multiplier = Quaternion.create(a, 0, 0, 0);

        final Quaternion p = product(q, multiplier);

        assertEquals("product a", q.getA() * a, p.getA(), Double.MIN_NORMAL);
        assertEquals("product b", q.getB() * a, p.getB(), Double.MIN_NORMAL);
        assertEquals("product c", q.getC() * a, p.getC(), Double.MIN_NORMAL);
        assertEquals("product d", q.getD() * a, p.getD(), Double.MIN_NORMAL);
    }

    private static void product_b(Quaternion q, double b) {
        final Quaternion multiplier = Quaternion.create(0, b, 0, 0);

        final Quaternion p = product(q, multiplier);

        assertEquals("product a", q.getB() * -b, p.getA(), Double.MIN_NORMAL);
        assertEquals("product b", q.getA() * b, p.getB(), Double.MIN_NORMAL);
        assertEquals("product c", q.getD() * b, p.getC(), Double.MIN_NORMAL);
        assertEquals("product d", q.getC() * -b, p.getD(), Double.MIN_NORMAL);
    }

    private static void product_c(Quaternion q, double c) {
        final Quaternion multiplier = Quaternion.create(0, 0, c, 0);

        final Quaternion p = product(q, multiplier);

        assertEquals("product a", q.getC() * -c, p.getA(), Double.MIN_NORMAL);
        assertEquals("product b", q.getD() * -c, p.getB(), Double.MIN_NORMAL);
        assertEquals("product c", q.getA() * c, p.getC(), Double.MIN_NORMAL);
        assertEquals("product d", q.getB() * c, p.getD(), Double.MIN_NORMAL);
    }

    private static void product_d(Quaternion q, double d) {
        final Quaternion multiplier = Quaternion.create(0, 0, 0, d);

        final Quaternion p = product(q, multiplier);

        assertEquals("product a", q.getD() * -d, p.getA(), Double.MIN_NORMAL);
        assertEquals("product b", q.getC() * d, p.getB(), Double.MIN_NORMAL);
        assertEquals("product c", q.getB() * -d, p.getC(), Double.MIN_NORMAL);
        assertEquals("product d", q.getA() * d, p.getD(), Double.MIN_NORMAL);
    }

    private static Quaternion scale(Quaternion x, double f) {
        final Quaternion scaled = x.scale(f);

        assertNotNull("Not null, result", scaled);
        assertInvariants(scaled);
        assertInvariants(x, scaled);

        return scaled;
    }

    @Test
    public void conjugate_0() {
        conjugate(Quaternion.ZERO);
    }

    @Test
    public void conjugate_1() {
        conjugate(Quaternion.create(1, 1, 1, 1));
    }

    @Test
    public void conjugate_A() {
        conjugate(Quaternion.create(2, 3, 4, 5));
    }

    @Test
    public void constructor_1() {
        final Quaternion q = constructor(1, 1, 1, 1);

        assertEquals("norm^2", 4.0, q.norm2(), Double.MIN_NORMAL);
        assertEquals(" norm", 2.0, q.norm(), Double.MIN_NORMAL);
    }

    @Test
    public void constructor_2differentA() {
        final double b = 2.0;
        final double c = 3.0;
        final double d = 4.0;
        final Quaternion q1 = Quaternion.create(1.0, b, c, d);
        final Quaternion q2 = Quaternion.create(-1.0, b, c, d);

        assertInvariants(q1, q2);
        assertNotEquals(q1, q2);
    }

    @Test
    public void constructor_2differentB() {
        final double a = 1.0;
        final double c = 3.0;
        final double d = 4.0;
        final Quaternion q1 = Quaternion.create(a, 2.0, c, d);
        final Quaternion q2 = Quaternion.create(a, -2.0, c, d);

        assertInvariants(q1, q2);
        assertNotEquals(q1, q2);
    }

    @Test
    public void constructor_2differentC() {
        final double a = 1.0;
        final double b = 2.0;
        final double d = 4.0;
        final Quaternion q1 = Quaternion.create(a, b, 3.0, d);
        final Quaternion q2 = Quaternion.create(a, b, -3.0, d);

        assertInvariants(q1, q2);
        assertNotEquals(q1, q2);
    }

    @Test
    public void constructor_2differentD() {
        final double a = 1.0;
        final double b = 2.0;
        final double c = 3.0;
        final Quaternion q1 = Quaternion.create(a, b, c, 4.0);
        final Quaternion q2 = Quaternion.create(a, b, c, -4.0);

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
    public void constructor_a1() {
        final Quaternion q = constructor(1, 0, 0, 0);

        assertEquals("norm^2", 1.0, q.norm2(), Double.MIN_NORMAL);
        assertEquals(" norm", 1.0, q.norm(), Double.MIN_NORMAL);
    }

    @Test
    public void constructor_a2() {
        final Quaternion q = constructor(-2, 0, 0, 0);

        assertEquals("norm^2", 4.0, q.norm2(), Double.MIN_NORMAL);
        assertEquals(" norm", 2.0, q.norm(), Double.MIN_NORMAL);
    }

    @Test
    public void constructor_aMax() {
        final Quaternion q = constructor(Double.MAX_VALUE, 0, 0, 0);

        assertEquals("norm", Double.MAX_VALUE, q.norm(),
                (Math.nextAfter(1.0, Double.MAX_VALUE) - 1.0) * Double.MAX_VALUE);
    }

    @Test
    public void constructor_b1() {
        final Quaternion q = constructor(0, 1, 0, 0);

        assertEquals("norm^2", 1.0, q.norm2(), Double.MIN_NORMAL);
        assertEquals(" norm", 1.0, q.norm(), Double.MIN_NORMAL);
    }

    @Test
    public void constructor_b2() {
        final Quaternion q = constructor(0, -2, 0, 0);

        assertEquals("norm^2", 4.0, q.norm2(), Double.MIN_NORMAL);
        assertEquals(" norm", 2.0, q.norm(), Double.MIN_NORMAL);
    }

    @Test
    public void constructor_c1() {
        final Quaternion q = constructor(0, 0, 1, 0);

        assertEquals("norm^2", 1.0, q.norm2(), Double.MIN_NORMAL);
        assertEquals(" norm", 1.0, q.norm(), Double.MIN_NORMAL);
    }

    @Test
    public void constructor_c2() {
        final Quaternion q = constructor(0, 0, -2, 0);

        assertEquals("norm^2", 4.0, q.norm2(), Double.MIN_NORMAL);
        assertEquals(" norm", 2.0, q.norm(), Double.MIN_NORMAL);
    }

    @Test
    public void constructor_d1() {
        final Quaternion q = constructor(0, 0, 0, 1);

        assertEquals("norm^2", 1.0, q.norm2(), Double.MIN_NORMAL);
        assertEquals(" norm", 1.0, q.norm(), Double.MIN_NORMAL);
    }

    @Test
    public void constructor_d2() {
        final Quaternion q = constructor(0, 0, 0, -2);

        assertEquals("norm^2", 4.0, q.norm2(), Double.MIN_NORMAL);
        assertEquals(" norm", 2.0, q.norm(), Double.MIN_NORMAL);
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

    @Test
    public void plus_negativeB() {
        plus_negative(-9, -8, -7, -6);
    }

    @Test
    public void product_0A() {
        product_0(Quaternion.create(1, 2, 3, 4));
    }

    @Test
    public void product_0B() {
        product_0(Quaternion.create(8, 7, 6, 5));
    }

    @Test
    public void product_a1() {
        product_a(Quaternion.create(1, 2, 3, 4), 1);
    }

    @Test
    public void product_a2() {
        product_a(Quaternion.create(-1, -2, -3, -4), 2);
    }

    @Test
    public void product_b1() {
        product_b(Quaternion.create(1, 2, 3, 4), 1);
    }

    @Test
    public void product_b2() {
        product_b(Quaternion.create(-1, -2, -3, -4), 2);
    }

    @Test
    public void product_c1() {
        product_c(Quaternion.create(1, 2, 3, 4), 1);
    }

    @Test
    public void product_c2() {
        product_c(Quaternion.create(-1, -2, -3, -4), 2);
    }

    @Test
    public void product_d1() {
        product_d(Quaternion.create(1, 2, 3, 4), 1);
    }

    @Test
    public void product_d2() {
        product_d(Quaternion.create(-1, -2, -3, -4), 2);
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
        final Quaternion one = Quaternion.create(1, 1, 1, 1);

        final Quaternion scaled = scale(one, 1);

        assertEquals("scaled", one, scaled);
    }

    @Test
    public void scale_1B() {
        final Quaternion one = Quaternion.create(1, 1, 1, 1);

        final Quaternion scaled = scale(one, -2.0);

        assertEquals("scaled a", -2.0, scaled.getA(), Double.MIN_NORMAL);
        assertEquals("scaled b", -2.0, scaled.getB(), Double.MIN_NORMAL);
        assertEquals("scaled c", -2.0, scaled.getC(), Double.MIN_NORMAL);
        assertEquals("scaled d", -2.0, scaled.getD(), Double.MIN_NORMAL);
    }

    @Test
    public void scale_B() {
        final Quaternion original = Quaternion.create(2, 3, 4, 5);

        final Quaternion scaled = scale(original, 4);

        assertEquals("scaled a", 8, scaled.getA(), Double.MIN_NORMAL);
        assertEquals("scaled b", 12, scaled.getB(), Double.MIN_NORMAL);
        assertEquals("scaled c", 16, scaled.getC(), Double.MIN_NORMAL);
        assertEquals("scaled d", 20, scaled.getD(), Double.MIN_NORMAL);
    }

    @Test
    public void static_values() {
        assertInvariants(Quaternion.ZERO);

        assertEquals("zero norm^2", 0.0, Quaternion.ZERO.norm2(), Double.MIN_NORMAL);
        assertEquals("zero norm", 0.0, Quaternion.ZERO.norm(), Double.MIN_NORMAL);
    }
}
