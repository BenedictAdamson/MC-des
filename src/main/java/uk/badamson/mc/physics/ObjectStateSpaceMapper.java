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
     * <li>The method sets (assigns) components of the state-space vector.</li>
     * <li>This is the inverse operation of the {@link #toObject(ImmutableVectorN)}
     * method.
     * </ul>
     * 
     * @param state
     *            The components of the state-space vector.
     * @param object
     *            The object to convert to state-space representation.
     * @throws NullPointerException
     *             If {@code state} is null.
     * @throws IndexOutOfBoundsException
     *             If {@code state} does not have the number of dimensions that this
     *             mapper expects.
     */
    public void fromObject(double[] state, OBJECT object);

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
     * @throws IndexOutOfBoundsException
     *             If {@code state} does not have the number of
     *             {@linkplain ImmutableVectorN#getDimension() dimensions} that this
     *             mapper expects.
     */
    public OBJECT toObject(ImmutableVectorN state);
}
