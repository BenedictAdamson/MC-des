package uk.badamson.mc.math;

/**
 * <p>
 * An immutable matrix that describes a rotation in 3 dimensional Euclidean
 * space.
 * </p>
 * <ul>
 * <li>A 3D rotation matrix has 3 {@linkplain #getRows() rows}.</li>
 * <li>A 3D rotation matrix has 3 {@linkplain #getColumns() columns}.</li>
 * </ul>
 */
public final class RotationMatrix3 extends ImmutableMatrix {

    RotationMatrix3(double[] elements) {
        super(3, 3, elements);
    }
}
