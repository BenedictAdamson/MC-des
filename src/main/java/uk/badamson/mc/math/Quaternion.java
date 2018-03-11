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

    private final double a;
    private final double b;
    private final double c;
    private final double d;

    /**
     * <p>
     * Construct a quaternion with given components.
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
    public Quaternion(double a, double b, double c, double d) {
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
     * The real-number component of this quaternion.
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
        if (Double.isFinite(s) && Double.MIN_NORMAL < s) {
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
     * Create a quaternion that is the Hamilton product of this quaternion and a
     * given quaternion.
     * </p>
     * <ul>
     * <li>Always returns a (non null) quaternion.
     * <li>
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
}
