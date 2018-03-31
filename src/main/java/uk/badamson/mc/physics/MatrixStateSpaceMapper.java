package uk.badamson.mc.physics;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.Matrix;

/**
 * <p>
 * A Strategy for mapping from an object representation of a {@linkplain Matrix
 * matrix} to (part of) a state-space representation, and vice versa.
 * </p>
 */
@Immutable
public interface MatrixStateSpaceMapper<MATRIX extends Matrix> extends ObjectStateSpaceMapper<MATRIX> {

}
