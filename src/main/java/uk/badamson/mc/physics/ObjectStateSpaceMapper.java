package uk.badamson.mc.physics;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * A Strategy for mapping from an object representation to (part of) a
 * state-space representation, and vice versa.
 * </p>
 */
@Immutable
public interface ObjectStateSpaceMapper<OBJECT> {

    /**
     * <p>
     * Using this mapping to convert an object to (part of) a state-space vector.
     * </p>
     * <ul>
     * <li>The method <em>adds</em> the components of the given state-space vector.
     * Normally, the state-space vector should have been initialised to zero.</li>
     * <li>This is the inverse operation of the {@link #toObject(ImmutableVectorN)}
     * method.
     * </ul>
     * 
     * @param state
     *            The components of the state-space vector.
     * @param object
     *            The object to convert to state-space representation.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code state} is null.</li>
     *             <li>If {@code object} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If {@code object} is unsuitable for conversion.
     * @throws RuntimeException
     *             (Typically an {@link IndexOutOfBoundsException} or
     *             {@link IllegalArgumentException}). If the length of the
     *             {@code state} array is not {@linkplain #isValidForDimension(int)
     *             valid} for this mapper.
     */
    public void fromObject(double[] state, OBJECT object);

    /**
     * <p>
     * Whether this mapper can be used for a state-space vector that has a given
     * number of variables.
     * </p>
     * 
     * @return whether valid.
     * @throws IllegalArgumentException
     *             If {@code n} is not positive.
     */
    public boolean isValidForDimension(int n);

    /**
     * <p>
     * Using this mapping to convert (part of) a state-space vector to an object.
     * </p>
     * 
     * @param state
     *            The state-space vector
     * @return The representation of (part of) the state-space; not null.
     * @throws NullPointerException
     *             If {@code state} is null.
     * @throws RuntimeException
     *             (Typically an {@link IndexOutOfBoundsException} or
     *             {@link IllegalArgumentException}). If the length of the
     *             {@code state} array is not {@linkplain #isValidForDimension(int)
     *             valid} for this mapper.
     */
    public OBJECT toObject(ImmutableVectorN state);
}
