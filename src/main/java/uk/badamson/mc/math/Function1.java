package uk.badamson.mc.math;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A functor for a one-dimensional function of a continuous variable.
 * </p>
 */
@FunctionalInterface
@Immutable
public interface Function1 {

    /**
     * <p>
     * The value of the function for a given value of the continuous variable.
     * </p>
     * 
     * @param x
     *            The value of the continuous variable
     * @return The value of the function.
     */
    public double value(double x);
}
