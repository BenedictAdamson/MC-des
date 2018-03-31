package uk.badamson.mc.physics;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.Vector;

/**
 * <p>
 * A Strategy for mapping from an object representation of a {@linkplain Vector
 * vector} to (part of) a state-space representation, and vice versa.
 * </p>
 */
@Immutable
public interface VectorStateSpaceMapper<VECTOR extends Vector> extends MatrixStateSpaceMapper<VECTOR> {

}
