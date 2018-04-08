package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests of the {@link Quaternion} class.
 * </p>
 */
public class QuaternionTest {

    private static class IsCloseTo extends TypeSafeMatcher<Quaternion> {
        private final double tolerance;
        private final Quaternion value;

        private IsCloseTo(Quaternion value, double tolerance) {
            this.tolerance = tolerance;
            this.value = value;
        }

        @Override
        public void describeMismatchSafely(Quaternion item, Description mismatchDescription) {
            mismatchDescription.appendValue(item).appendText(" differed by ").appendValue(distance(item));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a quaternion within ").appendValue(tolerance).appendText(" of ").appendValue(value);
        }

        private final double distance(Quaternion item) {
            return value.distance(item);
        }

        @Override
        public boolean matchesSafely(Quaternion item) {
            return distance(item) <= tolerance;
        }
    }// class

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

    @Factory
    public static Matcher<Quaternion> closeToQuaternion(Quaternion operand, double tolerance) {
        return new IsCloseTo(operand, tolerance);
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

    public static final Quaternion conjugation(Quaternion q, Quaternion p) {
        final double tolerance = Math.max(q.norm() * p.norm(), Double.MIN_NORMAL);
        final Quaternion expected = q.product(p).product(q.reciprocal());

        final Quaternion c = q.conjugation(p);

        assertNotNull("Not null, result", c);
        assertInvariants(q);// check for side effects
        assertInvariants(p);// check for side effects
        assertInvariants(p, q);// check for side effects
        assertInvariants(c);
        assertInvariants(p, c);
        assertInvariants(q, c);
        assertTrue("Expected result", expected.distance(c) < tolerance);

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

    private static double distance(Quaternion q, Quaternion that) {
        final double d = q.distance(that);

        assertInvariants(q);

        return d;
    }

    private static void distance_0(double a, double b, double c, double d) {
        final Quaternion q = Quaternion.create(a, b, c, d);

        final double distance = distance(q, Quaternion.ZERO);

        assertEquals("The distance of a quaternion from zero is its norm", q.norm(), distance, Double.MIN_NORMAL);
    }

    private static void distance_self(double a, double b, double c, double d) {
        final Quaternion p = Quaternion.create(a, b, c, d);

        final double distance = distance(p, p);

        assertEquals("The distance of a quaternion from itself is zero", 0.0, distance, Double.MIN_NORMAL);
    }

    private static Quaternion exp(Quaternion q) {
        final Quaternion eq = q.exp();

        assertNotNull("Not null, result", eq);// guard
        assertInvariants(q);// check for side-effects
        assertInvariants(eq);
        assertInvariants(eq, q);

        return eq;
    }

    private static void exp_finite(double a, double b, double c, double d) {
        final Quaternion q = Quaternion.create(a, b, c, d);
        final Quaternion m = Quaternion.create(-a, -b, -c, -d);
        final double precision = q.norm();

        final Quaternion eq = exp(q);

        final Quaternion em = m.exp();
        final Quaternion eqem = eq.product(em);
        final Quaternion logexp = eq.log();

        assertInvariants(eq, em);
        assertTrue("exp(q)*exp(-q) = exp(q-q) = exp(0) = 1", Quaternion.ONE.distance(eqem) < precision);
        assertTrue("exp and log are inverse operations", q.distance(logexp) < precision);
    }

    private static void exp_finiteScalar(double a) {
        final double precision = Double.MIN_NORMAL * Math.abs(a);
        final Quaternion q = Quaternion.create(a, 0, 0, 0);

        final Quaternion eq = exp(q);

        assertEquals("exponential a", Math.exp(a), eq.getA(), precision);
        assertEquals("exponential b", 0.0, eq.getB(), precision);
        assertEquals("exponential c", 0.0, eq.getC(), precision);
        assertEquals("exponential d", 0.0, eq.getD(), precision);
    }

    private static Quaternion log(Quaternion q) {
        final Quaternion log = q.log();

        assertNotNull("Not null, result", log);// guard
        assertInvariants(q);// check for side-effects
        assertInvariants(log);
        assertInvariants(log, q);

        return log;
    }

    private static void log_finite(double a, double b, double c, double d) {
        final Quaternion q = Quaternion.create(a, b, c, d);
        final double precision = q.norm();

        final Quaternion log = log(q);

        final Quaternion explog = log.exp();

        assertInvariants(log, explog);
        assertTrue("log and exp are inverse operations", q.distance(explog) < precision);
    }

    private static void log_finitePositiveScalar(double a) {
        final double precision = Double.MIN_NORMAL * Math.abs(a);
        final Quaternion q = Quaternion.create(a, 0, 0, 0);

        final Quaternion log = log(q);

        assertEquals("log a", Math.log(a), log.getA(), precision);
        assertEquals("log b", 0.0, log.getB(), precision);
        assertEquals("log c", 0.0, log.getC(), precision);
        assertEquals("log d", 0.0, log.getD(), precision);
    }

    public static Quaternion mean(Quaternion x, Quaternion that) {
        final Quaternion mean = x.mean(that);

        assertNotNull("Not null, mean", mean);// guard
        assertInvariants(mean);
        assertInvariants(x, mean);
        assertInvariants(that, mean);

        return mean;
    }

    private static Quaternion minus(Quaternion q, Quaternion that) {
        final Quaternion sum = q.minus(that);

        assertInvariants(sum);
        assertInvariants(sum, q);
        assertInvariants(sum, that);

        return sum;
    }

    private static void minus_0(double a, double b, double c, double d) {
        final Quaternion q = Quaternion.create(a, b, c, d);

        final Quaternion sum = minus(q, Quaternion.ZERO);

        assertEquals("Unchanged", q, sum);
    }

    private static void minus_self(double a, double b, double c, double d) {
        final Quaternion p = Quaternion.create(a, b, c, d);

        final Quaternion sum = minus(p, p);

        assertEquals("sum", Quaternion.ZERO, sum);
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

    private static void plus_minus(double a, double b, double c, double d) {
        final Quaternion q = Quaternion.create(a, b, c, d);

        final Quaternion result = Quaternion.ZERO.plus(q).minus(q);

        assertEquals("plus and minus are inverse operations", Quaternion.ZERO, result);
    }

    private static void plus_negative(double a, double b, double c, double d) {
        final Quaternion p = Quaternion.create(a, b, c, d);
        final Quaternion m = Quaternion.create(-a, -b, -c, -d);

        final Quaternion sum = plus(p, m);

        assertEquals("sum", Quaternion.ZERO, sum);
    }

    private static Quaternion pow(Quaternion q, double p) {
        final Quaternion qp = q.pow(p);

        assertNotNull("Not null, result", qp);// guard
        assertInvariants(q);// checks for side effects
        assertInvariants(qp);
        assertInvariants(q);

        return qp;
    }

    private static void pow_finite(double a, double b, double c, double d, double p) {
        final Quaternion q = Quaternion.create(a, b, c, d);
        final double precision = q.norm() * 4.0;

        final Quaternion qp = pow(q, p);
        final Quaternion qm = pow(q, -p);
        final Quaternion qpqm = qp.product(qm);
        final Quaternion qprp = qp.pow(1.0 / p);

        assertTrue("q^p*q^-p = q^(p-p) = q^0 = 1 <" + qpqm + ">", Quaternion.ONE.distance(qpqm) <= precision);
        assertTrue("(q^p)^(1/p) = q^(p/p) = q^1 = q <" + qprp + ">", q.distance(qprp) <= precision);
    }

    private static void pow_finiteScalar(double a, double p) {
        final Quaternion q = Quaternion.create(a, 0, 0, 0);
        final double precision = Math.abs(a) * 4.0;

        final Quaternion qp = pow(q, p);

        assertEquals("pow a", Math.pow(a, p), qp.getA(), precision);
        assertEquals("pow b", 0.0, qp.getB(), precision);
        assertEquals("pow c", 0.0, qp.getC(), precision);
        assertEquals("pow d", 0.0, qp.getD(), precision);
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

    private static Quaternion reciprocal(Quaternion q) {
        final Quaternion r = q.reciprocal();

        assertNotNull("Not null, reciprocal", r);// guard
        assertInvariants(r);
        assertInvariants(q);
        assertInvariants(r, q);

        return r;
    }

    private static void reciprocal_finite(double a, double b, double c, double d) {
        final Quaternion q = Quaternion.create(a, b, c, d);

        final Quaternion r = reciprocal(q);

        final Quaternion qr = q.product(r);
        assertTrue("The product of a quaternion with its reciprocal <" + qr + "> is one",
                qr.distance(Quaternion.ONE) < Double.MIN_NORMAL * 4.0);
        assertTrue("The reciprocal of a quaternion is its conjugate divided by the square of its norm",
                r.distance(q.conjugate().scale(1.0 / q.norm2())) < Double.MIN_NORMAL);
    }

    private static Quaternion scale(Quaternion x, double f) {
        final Quaternion scaled = x.scale(f);

        assertNotNull("Not null, result", scaled);
        assertInvariants(scaled);
        assertInvariants(x, scaled);

        return scaled;
    }

    private static Quaternion vector(Quaternion q) {
        final Quaternion v = q.vector();

        assertNotNull("Not null, vector part", v);// guard
        assertInvariants(q);// side-effects
        assertInvariants(v);
        assertInvariants(q, v);

        assertEquals("vector a", 0, v.getA(), Double.MIN_NORMAL);
        assertEquals("vector b", Double.doubleToLongBits(q.getB()), Double.doubleToLongBits(v.getB()));
        assertEquals("vector c", Double.doubleToLongBits(q.getC()), Double.doubleToLongBits(v.getC()));
        assertEquals("vector d", Double.doubleToLongBits(q.getD()), Double.doubleToLongBits(v.getD()));

        return v;
    }

    private static Quaternion versor(Quaternion q) {
        final Quaternion v = q.versor();

        assertNotNull("Not null, versor", v);// guard
        assertInvariants(q);
        assertInvariants(v);
        assertInvariants(q, v);

        return v;
    }

    private static void versor_finite(double a, double b, double c, double d) {
        final Quaternion q = Quaternion.create(a, b, c, d);
        final double n = q.norm();

        final Quaternion v = versor(q);

        assertEquals("versor has unit norm", 1.0, v.norm(), Math.nextAfter(1.0, Double.POSITIVE_INFINITY) - 1.0);
        assertTrue("quaternion is equivalent to its versor <" + v + "> scaled by its norm <" + n + ">",
                q.distance(v.scale(n)) < Math.max(1.0, n) * Double.MIN_NORMAL);
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
    public void conjugation_11() {
        conjugation(Quaternion.ONE, Quaternion.ONE);
    }

    @Test
    public void conjugation_12() {
        conjugation(Quaternion.ONE, Quaternion.create(2, 0, 0, 0));
    }

    @Test
    public void conjugation_21() {
        conjugation(Quaternion.create(2, 0, 0, 0), Quaternion.ONE);
    }

    @Test
    public void conjugation_ii() {
        conjugation(Quaternion.I, Quaternion.I);
    }

    @Test
    public void conjugation_ij() {
        conjugation(Quaternion.I, Quaternion.J);
    }

    @Test
    public void conjugation_ik() {
        conjugation(Quaternion.I, Quaternion.K);
    }

    @Test
    public void conjugation_ji() {
        conjugation(Quaternion.J, Quaternion.I);
    }

    @Test
    public void conjugation_jj() {
        conjugation(Quaternion.J, Quaternion.J);
    }

    @Test
    public void conjugation_jk() {
        conjugation(Quaternion.J, Quaternion.K);
    }

    @Test
    public void conjugation_ki() {
        conjugation(Quaternion.K, Quaternion.I);
    }

    @Test
    public void conjugation_kj() {
        conjugation(Quaternion.K, Quaternion.J);
    }

    @Test
    public void conjugation_kk() {
        conjugation(Quaternion.K, Quaternion.K);
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
    public void distance_0A() {
        distance_0(1, 2, 3, 4);
    }

    @Test
    public void distance_0B() {
        distance_0(-9, -8, -7, -6);
    }

    @Test
    public void distance_selfA() {
        distance_self(1, 2, 3, 4);
    }

    @Test
    public void distance_selfB() {
        distance_self(-9, -8, -7, -6);
    }

    @Test
    public void exp_b1() {
        exp_finite(0, 1, 0, 0);
    }

    @Test
    public void exp_b2() {
        exp_finite(0, 2, 0, 0);
    }

    @Test
    public void exp_bMinus1() {
        exp_finite(0, -1, 0, 0);
    }

    @Test
    public void exp_c1() {
        exp_finite(0, 0, 1, 0);
    }

    @Test
    public void exp_c2() {
        exp_finite(0, 0, 2, 0);
    }

    @Test
    public void exp_cMinus1() {
        exp_finite(0, 0, -1, 0);
    }

    @Test
    public void exp_combined() {
        exp_finite(1, 1, 1, 1);
    }

    @Test
    public void exp_d1() {
        exp_finite(0, 0, 0, 1);
    }

    @Test
    public void exp_d2() {
        exp_finite(0, 0, 0, 2);
    }

    @Test
    public void exp_dMinus1() {
        exp_finite(0, 0, 0, -1);
    }

    @Test
    public void exp_scalar0() {
        exp_finiteScalar(0.0);
    }

    @Test
    public void exp_scalar1() {
        exp_finiteScalar(1.0);
    }

    @Test
    public void exp_scalar2() {
        exp_finiteScalar(2.0);
    }

    @Test
    public void exp_scalarMinus1() {
        exp_finiteScalar(-1.0);
    }

    @Test
    public void log_b1() {
        log_finite(0, 1, 0, 0);
    }

    @Test
    public void log_b2() {
        log_finite(0, 2, 0, 0);
    }

    @Test
    public void log_bMinus1() {
        log_finite(0, -1, 0, 0);
    }

    @Test
    public void log_c1() {
        log_finite(0, 0, 1, 0);
    }

    @Test
    public void log_c2() {
        log_finite(0, 0, 2, 0);
    }

    @Test
    public void log_cMinus1() {
        log_finite(0, 0, -1, 0);
    }

    @Test
    public void log_combined() {
        log_finite(1, 1, 1, 1);
    }

    @Test
    public void log_d1() {
        log_finite(0, 0, 0, 1);
    }

    @Test
    public void log_d2() {
        log_finite(0, 0, 0, 2);
    }

    @Test
    public void log_dMinus1() {
        log_finite(0, 0, 0, -1);
    }

    @Test
    public void log_scalar1() {
        log_finitePositiveScalar(1.0);
    }

    @Test
    public void log_scalar2() {
        log_finitePositiveScalar(2.0);
    }

    @Test
    public void mean_all() {
        final Quaternion mean = mean(Quaternion.create(3, 4, 5, 6), Quaternion.create(7, 8, 9, 10));
        assertThat("mean", mean, closeToQuaternion(Quaternion.create(5, 6, 7, 8), Double.MIN_NORMAL));
    }

    @Test
    public void mean_x01() {
        final Quaternion mean = mean(Quaternion.ZERO, Quaternion.ONE);
        assertThat("mean", mean, closeToQuaternion(Quaternion.create(0.5, 0, 0, 0), Double.MIN_NORMAL));
    }

    @Test
    public void mean_x02() {
        final Quaternion mean = mean(Quaternion.ZERO, Quaternion.create(2, 0, 0, 0));
        assertThat("mean", mean, closeToQuaternion(Quaternion.create(1, 0, 0, 0), Double.MIN_NORMAL));
    }

    @Test
    public void mean_x0i() {
        final Quaternion mean = mean(Quaternion.ZERO, Quaternion.I);
        assertThat("mean", mean, closeToQuaternion(Quaternion.create(0, 0.5, 0, 0), Double.MIN_NORMAL));
    }

    @Test
    public void mean_x0j() {
        final Quaternion mean = mean(Quaternion.ZERO, Quaternion.J);
        assertThat("mean", mean, closeToQuaternion(Quaternion.create(0, 0, 0.5, 0), Double.MIN_NORMAL));
    }

    @Test
    public void mean_x0k() {
        final Quaternion mean = mean(Quaternion.ZERO, Quaternion.K);
        assertThat("mean", mean, closeToQuaternion(Quaternion.create(0, 0, 0, 0.5), Double.MIN_NORMAL));
    }

    @Test
    public void minus_0A() {
        minus_0(1, 2, 3, 4);
    }

    @Test
    public void minus_0B() {
        minus_0(-9, -8, -7, -6);
    }

    @Test
    public void minus_selfA() {
        minus_self(1, 2, 3, 4);
    }

    @Test
    public void minus_selfB() {
        minus_self(-9, -8, -7, -6);
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
    public void plus_minusA() {
        plus_minus(1, 2, 3, 4);
    }

    @Test
    public void plus_minusB() {
        plus_minus(-9, -8, -7, -6);
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
    public void pow_0A() {
        pow_finiteScalar(0, 2);
    }

    @Test
    public void pow_b13() {
        pow_finite(0, 1, 0, 0, 3);
    }

    @Test
    public void pow_b23() {
        pow_finite(0, 2, 0, 0, 3);
    }

    @Test
    public void pow_b25() {
        pow_finite(0, 2, 0, 0, 5);
    }

    @Test
    public void pow_c13() {
        pow_finite(0, 0, 1, 0, 3);
    }

    @Test
    public void pow_c23() {
        pow_finite(0, 0, 2, 0, 3);
    }

    @Test
    public void pow_c25() {
        pow_finite(0, 0, 2, 0, 5);
    }

    @Test
    public void pow_d13() {
        pow_finite(0, 0, 0, 1, 3);
    }

    @Test
    public void pow_d23() {
        pow_finite(0, 0, 0, 2, 3);
    }

    @Test
    public void pow_d25() {
        pow_finite(0, 0, 0, 2, 5);
    }

    @Test
    public void pow_finiteScalarA() {
        pow_finiteScalar(2, 2);
    }

    @Test
    public void pow_finiteScalarB() {
        pow_finiteScalar(2, 3);
    }

    @Test
    public void pow_finiteScalarC() {
        pow_finiteScalar(-2, 2);
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
    public void reciprocal_1() {
        reciprocal_finite(1, 1, 1, 1);
    }

    @Test
    public void reciprocal_a1() {
        reciprocal_finite(1, 0, 0, 0);
    }

    @Test
    public void reciprocal_b1() {
        reciprocal_finite(0, 1, 0, 0);
    }

    @Test
    public void reciprocal_c1() {
        reciprocal_finite(0, 0, 1, 0);
    }

    @Test
    public void reciprocal_d1() {
        reciprocal_finite(0, 0, 0, 1);
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

    @Test
    public void vector_0() {
        vector(Quaternion.ZERO);
    }

    @Test
    public void vector_1() {
        vector(Quaternion.ONE);
    }

    @Test
    public void vector_a2() {
        vector(Quaternion.create(2, 0, 0, 0));
    }

    @Test
    public void vector_b2() {
        vector(Quaternion.create(0, 2, 0, 0));
    }

    @Test
    public void vector_c2() {
        vector(Quaternion.create(0, 0, 2, 0));
    }

    @Test
    public void vector_d2() {
        vector(Quaternion.create(0, 0, 0, 2));
    }

    @Test
    public void versor_0() {
        assertEquals("versoer of zero is taken to be zero", Quaternion.ZERO, Quaternion.ZERO.versor());
    }

    @Test
    public void versor_a1() {
        versor_finite(1, 0, 0, 0);
    }

    @Test
    public void versor_a2() {
        versor_finite(2, 0, 0, 0);
    }

    @Test
    public void versor_aMinus1() {
        versor_finite(-1, 0, 0, 0);
    }

    @Test
    public void versor_aSmall() {
        versor_finite(Double.MIN_NORMAL, 0, 0, 0);
    }

    @Test
    public void versor_b1() {
        versor_finite(0, 1, 0, 0);
    }

    @Test
    public void versor_b2() {
        versor_finite(0, 2, 0, 0);
    }

    @Test
    public void versor_bMinus1() {
        versor_finite(0, -1, 0, 0);
    }

    @Test
    public void versor_bSmall() {
        versor_finite(0, Double.MIN_NORMAL, 0, 0);
    }

    @Test
    public void versor_c1() {
        versor_finite(0, 0, 1, 0);
    }

    @Test
    public void versor_c2() {
        versor_finite(0, 0, 2, 0);
    }

    @Test
    public void versor_cMinus1() {
        versor_finite(0, 0, -1, 0);
    }

    @Test
    public void versor_cSmall() {
        versor_finite(0, 0, Double.MIN_NORMAL, 0);
    }

    @Test
    public void versor_d1() {
        versor_finite(0, 0, 0, 1);
    }

    @Test
    public void versor_d2() {
        versor_finite(0, 0, 0, 2);
    }

    @Test
    public void versor_dMinus1() {
        versor_finite(0, 0, 0, -1);
    }

    @Test
    public void versor_dSmall() {
        versor_finite(0, 0, 0, Double.MIN_NORMAL);
    }
}
