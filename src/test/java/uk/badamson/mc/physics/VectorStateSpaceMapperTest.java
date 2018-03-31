package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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

        assertThat("Number of dimensions", mapper.getDimension(),
                org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo(1));
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

    public static <VECTOR extends Vector> void fromToObjectSymmetry(VectorStateSpaceMapper<VECTOR> mapper,
            double[] state, VECTOR original) {
        mapper.fromObject(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final VECTOR reconstructed = toObject(mapper, stateVector);

        assertEquals("Symmetric", original, reconstructed);
    }

    public static <VECTOR extends Vector> void fromToVectorSymmetry(VectorStateSpaceMapper<VECTOR> mapper,
            double[] state, Vector original) {
        mapper.fromVector(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final VECTOR reconstructed = toObject(mapper, stateVector);

        assertEquals("Symmetric", original, reconstructed);
    }

    public static <VECTOR extends Vector> void fromVector(VectorStateSpaceMapper<VECTOR> mapper, double[] state,
            Vector vector) {
        mapper.fromVector(state, vector);

        assertInvariants(mapper);// check for side effects
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
