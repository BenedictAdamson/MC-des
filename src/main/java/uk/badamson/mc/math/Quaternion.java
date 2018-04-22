package uk.badamson.mc.math;

import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A number system that extends the complex numbers to four dimensions.
 * </p>
 */
@Immutable
public final class Quaternion {

    /**
     * <p>
     * The quaternion that has a value of zero for each of its components.
     * </p>
     */
    public static final Quaternion ZERO = new Quaternion(0, 0, 0, 0);

    /**
     * <p>
     * The real quaternion of unit {@linkplain #norm() norm}.
     * </p>
     */
    public static final Quaternion ONE = new Quaternion(1, 0, 0, 0);

    /**
     * <p>
     * The quaternion having using {@linkplain #getB() i component} with all other
     * components zero.
     * </p>
     */
    public static final Quaternion I = new Quaternion(0, 1, 0, 0);

    /**
     * <p>
     * The quaternion having using {@linkplain #getC() j component} with all other
     * components zero.
     * </p>
     */
    public static final Quaternion J = new Quaternion(0, 0, 1, 0);

    /**
     * <p>
     * The quaternion having using {@linkplain #getD() k component} with all other
     * components zero.
     * </p>
     */
    public static final Quaternion K = new Quaternion(0, 0, 0, 1);

    private static double EXP_TOL = Math.pow(Double.MIN_NORMAL, 1.0 / 6.0) * 840.0;

    /**
     * <p>
     * Create a quaternion with given components.
     * </p>
     * 
     * @param a
     *            The real-number component of this quaternion.
     * @param b
     *            The <b>i</b> component of this quaternion.
     * @param c
     *            The <b>j</b> component of this quaternion.
     * @param d
     *            The <b>k</b> component of this quaternion.
     */
    public static Quaternion create(double a, double b, double c, double d) {
        return new Quaternion(a, b, c, d);
    }

    private final double a;
    private final double b;
    private final double c;

    private final double d;

    private Quaternion(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    /**
     * <p>
     * Create a quaternion that is the conjugate of this quaternion.
     * </p>
     * 
     * @return the conjugate; not null
     */
    public final Quaternion conjugate() {
        return new Quaternion(a, -b, -c, -d);
    }

    /**
     * <p>
     * Create a quaternion that is the conjugation of a quaternion by this
     * quaternion.
     * </p>
     * <p>
     * That is, if this is <var>q</var> and the other quaternion is <var>p</var>,
     * the method computes <var>q</var><var>p</var><var>q</var><sup>-1</sup>.
     * 
     * @param p
     *            The quaternion to be conjugated.
     * @return the conjugation; not null
     * @throws NullPointerException
     *             If {@code p} is null
     */
    public final Quaternion conjugation(Quaternion p) {
        Objects.requireNonNull(p, "p");
        return product(p).product(conjugate()).scale(1.0 / norm2());
    }

    /**
     * <p>
     * The distance between this quaternion and another
     * </p>
     * <ul>
     * <li>The distance is nominally equal to
     * <code>this.minus(that).norm()</code>.</li>
     * </ul>
     * 
     * @param that
     *            The other quaternion
     * @return the difference quaternion
     * 
     * @throws NullPointerException
     *             If {@code that} is null.
     */
    public final double distance(Quaternion that) {
        return minus(that).norm();
    }

    /**
     * <p>
     * Calculate the dot product (inner product) of this quaternion with another
     * quaternion.
     * </p>
     * 
     * @param that
     *            The other quaternion with which to calculate the dot product.
     * @return the dot product; not null.
     * @throws NullPointerException
     *             If {@code that} is null.
     */
    public final double dot(Quaternion that) {
        Objects.requireNonNull(that, "that");
        return a * that.a + b * that.b + c * that.c + d * that.d;
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> another object.
     * </p>
     * <p>
     * The {@link Quaternion} class has <i>value semantics</i>: this object is
     * equivalent to another object if, and only if, the other object is also a
     * {@link Quaternion} object, and the two objects have equivalent attributes.
     * </p>
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Quaternion other = (Quaternion) obj;
        return (Double.doubleToLongBits(a) == Double.doubleToLongBits(other.a))
                && (Double.doubleToLongBits(b) == Double.doubleToLongBits(other.b))
                && (Double.doubleToLongBits(c) == Double.doubleToLongBits(other.c))
                && (Double.doubleToLongBits(d) == Double.doubleToLongBits(other.d));
    }

    /**
     * <p>
     * Create a quaternion that is the exponential of this quaternion.
     * </p>
     * 
     * @return the exponential; not null
     * @see Math#exp(double)
     */
    public final Quaternion exp() {
        final double ea = Math.exp(a);
        final Quaternion v = vector();
        final double vn = v.norm();
        final double cos = Math.cos(vn);
        final double sinTerm;
        if (EXP_TOL < Math.abs(vn)) {
            sinTerm = Math.sin(vn) / vn;
        } else {
            /* Special handling for scalars and near scalars. */
            final double x2 = vn * vn;
            sinTerm = 1.0 - x2 * (1.0 / 6.0) * (1.0 - x2 * 0.05);
        }
        final Quaternion c1 = new Quaternion(ea * cos, 0, 0, 0);
        final Quaternion s1 = v.scale(ea * sinTerm);
        return c1.plus(s1);
    }

    /**
     * <p>
     * The real-number component of this quaternion.
     * </p>
     * <p>
     * Its <i>scalar part</i>.
     * </p>
     */
    public final double getA() {
        return a;
    }

    /**
     * <p>
     * The <b>i</b> component of this quaternion.
     * </p>
     */
    public final double getB() {
        return b;
    }

    /**
     * <p>
     * The <b>j</b> component of this quaternion.
     * </p>
     */
    public final double getC() {
        return c;
    }

    /**
     * <p>
     * The <b>k</b> component of this quaternion.
     * </p>
     */
    public final double getD() {
        return d;
    }

    private double getScale() {
        final double s = Double.max(Double.max(Math.abs(a), Math.abs(b)), Double.max(Math.abs(c), Math.abs(d)));
        if (Double.isFinite(s) && Double.MIN_NORMAL <= s) {
            return s;
        } else {
            /* Must accept overflow or underflow. */
            return 1.0;
        }
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(a);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(b);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(c);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(d);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * <p>
     * Create a quaternion that is the natural logarithm of this quaternion.
     * </p>
     * <p>
     * The method takes care to properly handle quaternions with components that are
     * large, not numbers, or which differ greatly in magnitude, and quaternions
     * that are close to {@linkplain #ZERO zero}, or which have a small
     * {@linkplain #vector() vector} part.
     * </p>
     * 
     * @return the natural logarithm; not null
     * @see Math#log(double)
     */
    public final Quaternion log() {
        final Quaternion v = vector();
        final double n = norm();
        final Quaternion sTerm = new Quaternion(Math.log(n), 0, 0, 0);
        final Quaternion vTerm = v.versor().scale(Math.acos(a / n) / n);
        return sTerm.plus(vTerm);
    }

    /**
     * <p>
     * Create the quaternion that is the mean of this quaternion with another
     * quaternion.
     * </p>
     * 
     * @param that
     *            The quaternion to take the mean with
     * @return the mean quaternion; not null.
     * 
     * @throws NullPointerException
     *             If {@code quaternion} is null.
     */
    public final Quaternion mean(Quaternion that) {
        return new Quaternion((a + that.a) * 0.5, (b + that.b) * 0.5, (c + that.c) * 0.5, (d + that.d) * 0.5);
    }

    /**
     * <p>
     * Create the quaternion that is a given quaternion subtracted from this
     * quaternion; the difference of this quaternion and another.
     * </p>
     * 
     * @param that
     *            The other quaternion
     * @return the difference quaternion
     * 
     * @throws NullPointerException
     *             If {@code that} is null.
     */
    public final Quaternion minus(Quaternion that) {
        Objects.requireNonNull(that, "that");
        return new Quaternion(a - that.a, b - that.b, c - that.c, d - that.d);
    }

    /**
     * <p>
     * The norm of this quaternion.
     * </p>
     * <p>
     * The method takes care to properly handle quaternions with components that are
     * large, not numbers, or which differ greatly in magnitude.
     * </p>
     * 
     * @return the norm
     */
    public final double norm() {
        final double s = getScale();
        final double f = 1.0 / s;
        final double as = a * f;
        final double bs = b * f;
        final double cs = c * f;
        final double ds = d * f;
        return Math.sqrt(as * as + bs * bs + cs * cs + ds * ds) * s;
    }

    /**
     * <p>
     * The square of the {@linkplain #norm() norm} of this quaternion.
     * </p>
     * <p>
     * The method takes care to properly handle quaternions with components that are
     * large, not numbers, or which differ greatly in magnitude.
     * </p>
     * 
     * @return the square of the norm.
     */
    public final double norm2() {
        final double s = getScale();
        final double f = 1.0 / s;
        final double as = a * f;
        final double bs = b * f;
        final double cs = c * f;
        final double ds = d * f;
        return (as * as + bs * bs + cs * cs + ds * ds) * (s * s);
    }

    /**
     * <p>
     * Create the quaternion that is a given quaternion added to this quaternion;
     * the sum of this quaternion and another.
     * </p>
     * 
     * @param that
     *            The other quaternion
     * @return the sum quaternion
     * 
     * @throws NullPointerException
     *             If {@code that} is null.
     */
    public final Quaternion plus(Quaternion that) {
        Objects.requireNonNull(that, "that");
        return new Quaternion(a + that.a, b + that.b, c + that.c, d + that.d);
    }

    /**
     * <p>
     * Create a quaternion that is this quaternion raised to a given real power.
     * </p>
     * <p>
     * The method takes care to properly handle quaternions with components that are
     * large, not numbers, or which differ greatly in magnitude, and quaternions
     * that are close to {@linkplain #ZERO zero}, or which have a small
     * {@linkplain #vector() vector} part.
     * </p>
     * 
     * @param p
     *            The power to raise this quaternion to
     * @return the power; not null
     * @see Math#pow(double, double)
     */
    public final Quaternion pow(double p) {
        final double n = norm();
        final Quaternion v = vector();
        final Quaternion direction = v.versor();
        final double y = direction.conjugate().product(v).getA();
        final double theta = Math.atan2(y, a);
        return direction.scale(theta * p).exp().scale(Math.pow(n, p));
    }

    /**
     * <p>
     * Create a quaternion that is the Hamilton product of this quaternion and a
     * given quaternion.
     * </p>
     * <ul>
     * <li>Always returns a (non null) quaternion.</li>
     * </ul>
     * 
     * @param that
     *            the other quaternion
     * @return the product
     * @throws NullPointerException
     *             If {@code that} is null.
     */
    public final Quaternion product(Quaternion that) {
        Objects.requireNonNull(that, "that");
        return new Quaternion(a * that.a - b * that.b - c * that.c - d * that.d,
                a * that.b + b * that.a + c * that.d - d * that.c, a * that.c - b * that.d + c * that.a + d * that.b,
                a * that.d + b * that.c - c * that.b + d * that.a);
    }

    /**
     * <p>
     * Create a quaternion that is the reciprocal of this quaternion.
     * </p>
     * 
     * @return the conjugate; not null
     */
    public final Quaternion reciprocal() {
        return conjugate().scale(1.0 / norm2());
    }

    /**
     * <p>
     * Create a quaternion that is this quaternion scaled by a given scalar.
     * </p>
     * <ul>
     * <li>Always returns a (non null) quaternion.
     * <li>
     * </ul>
     * 
     * @param f
     *            the scalar
     * @return the scaled quaternion
     */
    public final Quaternion scale(double f) {
        return new Quaternion(a * f, b * f, c * f, d * f);
    }

    @Override
    public final String toString() {
        return a + "+" + b + "i+" + c + "j+" + d + "k";
    }

    /**
     * <p>
     * Create a quaternion that is the vector part of this quaternion.
     * </p>
     * 
     * @return the vector part; not null
     */
    public final Quaternion vector() {
        return new Quaternion(0, b, c, d);
    }

    /**
     * <p>
     * Create a versor (a quaternion that has unit {@linkplain #norm() norm}) that
     * points in the same direction as this quaternion.
     * </p>
     * <p>
     * The method takes care to properly handle quaternions with components that are
     * large, not numbers, or which differ greatly in magnitude, and quaternions
     * that are close to {@linkplain #ZERO zero}. In the case of a quaternion very
     * close to zero, the method returns zero as the versor (rather than a versor
     * with {@linkplain Double#isFinite(double) non finite} components).
     * </p>
     * 
     * @return the versor; not null
     */
    public final Quaternion versor() {
        final double s = getScale();
        final double f1 = 1.0 / s;
        final double as = a * f1;
        final double bs = b * f1;
        final double cs = c * f1;
        final double ds = d * f1;
        final double hypot = Math.sqrt(as * as + bs * bs + cs * cs + ds * ds);
        if (Double.MIN_NORMAL < hypot) {
            final double f2 = f1 / hypot;
            return scale(f2);
        } else {
            return ZERO;
        }
    }
}
