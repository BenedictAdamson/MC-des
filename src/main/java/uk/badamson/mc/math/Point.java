package uk.badamson.mc.math;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A point in multi-dimensional space.
 * </p>
 */
@Immutable
public interface Point {

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> another object.
     * </p>
     * <p>
     * The {@link Point} class has <i>value semantics</i>: this object is equivalent
     * to another object if, and only if, the other object is also a
     * {@link Function1Value} object, and the two objects have equivalent
     * attributes.
     * </p>
     */
    @Override
    public boolean equals(Object obj);

    /**
     * <p>
     * The number of dimensions of the space containing this point.
     * </p>
     * 
     * @return the number of dimensions; positive.
     */
    public int getDimensions();

    /**
     * <p>
     * A coordinate of this point.
     * </p>
     * 
     * @param i
     *            The dimension for which the coordinate is wanted.
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code i} is negative.</li>
     *             <li>If {@code i} is not less than the
     *             {@linkplain #getDimensions() number of dimensions} of this
     *             point.</li>
     *             </ul>
     */
    public double getX(int i);

}