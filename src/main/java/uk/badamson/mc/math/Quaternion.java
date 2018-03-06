package uk.badamson.mc.math;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A number system that extends the complex numbers to four dimensions.
 * </p>
 */
@Immutable
public final class Quaternion {

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

}
