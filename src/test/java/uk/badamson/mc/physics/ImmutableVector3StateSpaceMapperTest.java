package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVector3;
import uk.badamson.mc.math.ImmutableVector3Test;
import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * Unit tests for classes that implement the {@link VectorStateSpaceMapper}
 * interface.
 */
public class ImmutableVector3StateSpaceMapperTest {

    public static void assertInvariants(ImmutableVector3StateSpaceMapper mapper) {
        VectorStateSpaceMapperTest.assertInvariants(mapper);// inherited
    }

    public static void assertInvariants(ImmutableVector3StateSpaceMapper mapper1,
            ImmutableVector3StateSpaceMapper mapper2) {
        VectorStateSpaceMapperTest.assertInvariants(mapper1, mapper2);// inherited
    }

    public static void fromObject(ImmutableVector3StateSpaceMapper mapper, double[] state, ImmutableVector3 vector) {
        VectorStateSpaceMapperTest.fromObject(mapper, state, vector);

        assertInvariants(mapper);// check for side-effects
        ImmutableVector3Test.assertInvariants(vector);// check for side-effects
    }

    private static void fromToObjectSymmetry(ImmutableVector3StateSpaceMapper mapper, double[] state,
            ImmutableVector3 original) {
        mapper.fromObject(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final ImmutableVector3 reconstructed = toObject(mapper, stateVector);

        assertEquals("Symmetric", original, reconstructed);
    }

    private static void fromToObjectSymmetry(int index0, int stateSize, ImmutableVector3 original) {
        final ImmutableVector3StateSpaceMapper mapper = new ImmutableVector3StateSpaceMapper(index0);
        final double[] state = new double[stateSize];

        fromToObjectSymmetry(mapper, state, original);
    }

    public static ImmutableVector3 toObject(ImmutableVector3StateSpaceMapper mapper, ImmutableVectorN state) {
        ImmutableVector3 vector = VectorStateSpaceMapperTest.toObject(mapper, state);

        assertInvariants(mapper);// check for side-effects
        ImmutableVector3Test.assertInvariants(vector);

        return vector;
    }

    @Test
    public void fromToObjectSymmetry_2i() {
        final int index0 = 0;
        final int stateSize = 3;
        fromToObjectSymmetry(index0, stateSize, ImmutableVector3.I.scale(2));
    }

    @Test
    public void fromToObjectSymmetry_extraAfter() {
        final int index0 = 0;
        final int stateSize = 5;
        fromToObjectSymmetry(index0, stateSize, ImmutableVector3.I);
    }

    @Test
    public void fromToObjectSymmetry_extraBefore() {
        final int index0 = 2;
        final int stateSize = 5;
        fromToObjectSymmetry(index0, stateSize, ImmutableVector3.I);
    }

    @Test
    public void fromToObjectSymmetry_i() {
        final int index0 = 0;
        final int stateSize = 3;
        fromToObjectSymmetry(index0, stateSize, ImmutableVector3.I);
    }

    @Test
    public void fromToObjectSymmetry_j() {
        final int index0 = 0;
        final int stateSize = 3;
        fromToObjectSymmetry(index0, stateSize, ImmutableVector3.J);
    }

    @Test
    public void fromToObjectSymmetry_k() {
        final int index0 = 0;
        final int stateSize = 3;
        fromToObjectSymmetry(index0, stateSize, ImmutableVector3.K);
    }

}
