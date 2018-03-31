package uk.badamson.mc.physics;

import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Vector;
import uk.badamson.mc.math.VectorTest;

/**
 * <p>
 * Unit tests for classes that implement the {@link VectorStateSpaceMapper}
 * interface.
 */
public class VectorStateSpaceMapperTest {

    public static <VECTOR extends Vector> void assertInvariants(VectorStateSpaceMapper<VECTOR> mapper) {
        MatrixStateSpaceMapperTest.assertInvariants(mapper);// inherited
    }

    public static <VECTOR extends Vector> void assertInvariants(VectorStateSpaceMapper<VECTOR> mapper1,
            VectorStateSpaceMapper<VECTOR> mapper2) {
        MatrixStateSpaceMapperTest.assertInvariants(mapper1, mapper2);// inherited
    }

    public static <VECTOR extends Vector> void fromObject(VectorStateSpaceMapper<VECTOR> mapper, double[] state,
            VECTOR vector) {
        MatrixStateSpaceMapperTest.fromObject(mapper, state, vector);

        assertInvariants(mapper);// check for side-effects
        VectorTest.assertInvariants(vector);// check for side-effects
    }

    public static <VECTOR extends Vector> VECTOR toObject(VectorStateSpaceMapper<VECTOR> mapper,
            ImmutableVectorN state) {
        VECTOR vector = MatrixStateSpaceMapperTest.toObject(mapper, state);

        assertInvariants(mapper);// check for side-effects
        VectorTest.assertInvariants(vector);

        return vector;
    }
}
