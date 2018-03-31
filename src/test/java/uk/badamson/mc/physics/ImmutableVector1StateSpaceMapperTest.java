package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVector1;
import uk.badamson.mc.math.ImmutableVector1Test;
import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * Unit tests for the {@link ImmutableVector1StateSpaceMapper} class.
 * </p>
 */
public class ImmutableVector1StateSpaceMapperTest {

    public static void assertInvariants(ImmutableVector1StateSpaceMapper mapper) {
        VectorStateSpaceMapperTest.assertInvariants(mapper);// inherited
    }

    public static void assertInvariants(ImmutableVector1StateSpaceMapper mapper1,
            ImmutableVector1StateSpaceMapper mapper2) {
        VectorStateSpaceMapperTest.assertInvariants(mapper1, mapper2);// inherited
    }

    public static void fromObject(ImmutableVector1StateSpaceMapper mapper, double[] state, ImmutableVector1 vector) {
        VectorStateSpaceMapperTest.fromObject(mapper, state, vector);

        assertInvariants(mapper);// check for side-effects
        ImmutableVector1Test.assertInvariants(vector);// check for side-effects
    }

    private static void fromToObjectSymmetry(ImmutableVector1StateSpaceMapper mapper, double[] state,
            ImmutableVector1 original) {
        mapper.fromObject(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final ImmutableVector1 reconstructed = toObject(mapper, stateVector);

        assertEquals("Symmetric", original, reconstructed);
    }

    private static void fromToObjectSymmetry(int index0, int stateSize, ImmutableVector1 original) {
        final ImmutableVector1StateSpaceMapper mapper = new ImmutableVector1StateSpaceMapper(index0);
        final double[] state = new double[stateSize];

        fromToObjectSymmetry(mapper, state, original);
    }

    public static ImmutableVector1 toObject(ImmutableVector1StateSpaceMapper mapper, ImmutableVectorN state) {
        ImmutableVector1 vector = VectorStateSpaceMapperTest.toObject(mapper, state);

        assertInvariants(mapper);// check for side-effects
        ImmutableVector1Test.assertInvariants(vector);

        return vector;
    }

    @Test
    public void fromToObjectSymmetry_2i() {
        final int index0 = 0;
        final int stateSize = 3;
        fromToObjectSymmetry(index0, stateSize, ImmutableVector1.I.scale(2));
    }

    @Test
    public void fromToObjectSymmetry_extraAfter() {
        final int index0 = 0;
        final int stateSize = 5;
        fromToObjectSymmetry(index0, stateSize, ImmutableVector1.I);
    }

    @Test
    public void fromToObjectSymmetry_extraBefore() {
        final int index0 = 2;
        final int stateSize = 5;
        fromToObjectSymmetry(index0, stateSize, ImmutableVector1.I);
    }

    @Test
    public void fromToObjectSymmetry_i() {
        final int index0 = 0;
        final int stateSize = 3;
        fromToObjectSymmetry(index0, stateSize, ImmutableVector1.I);
    }

}
