package uk.badamson.mc.math;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * One value from the domain of a {@linkplain Function1WithGradient single
 * dimensional function of a continuous variable that also has a computable
 * gradient} to the corresponding value in the codomain of the function and the
 * gradient of the function.
 * </p>
 */
@Immutable
public final class Function1WithGradientValue {

    private final double x;
    private final double f;
    private final double dfdx;

    /**
     * <p>
     * Construct an object with given attribute values.
     * </p>
     * 
     * @param x
     *            The domain value.
     * @param f
     *            The codomain value.
     * @param dfdx
     *            The gradient value
     */
    public Function1WithGradientValue(double x, double f, double dfdx) {
        this.x = x;
        this.f = f;
        this.dfdx = dfdx;
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> another object.
     * </p>
     * <p>
     * The {@link Function1WithGradientValue} class has <i>value semantics</i>: this
     * object is equivalent to another object if, and only if, the other object is
     * also a {@link Function1WithGradientValue} object, and the two objects have
     * equivalent attributes.
     * </p>
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Function1WithGradientValue other = (Function1WithGradientValue) obj;
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x)
                && Double.doubleToLongBits(f) == Double.doubleToLongBits(other.f)
                && Double.doubleToLongBits(dfdx) == Double.doubleToLongBits(other.dfdx);
    }

    /**
     * <p>
     * The gradient value.
     * </p>
     */
    public final double getDfDx() {
        return dfdx;
    }

    /**
     * <p>
     * The codomain value.
     * </p>
     */
    public final double getF() {
        return f;
    }

    /**
     * <p>
     * The domain value
     * </p>
     */
    public final double getX() {
        return x;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final long xBits = Double.doubleToLongBits(x);
        final long fBits = Double.doubleToLongBits(f);
        final long dfdxBits = Double.doubleToLongBits(dfdx);
        result = prime * result + (int) (xBits ^ (xBits >>> 32));
        result = prime * result + (int) (fBits ^ (fBits >>> 32));
        result = prime * result + (int) (dfdxBits ^ (dfdxBits >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + f + ", dfdx " + dfdx + ")";
    }

}
