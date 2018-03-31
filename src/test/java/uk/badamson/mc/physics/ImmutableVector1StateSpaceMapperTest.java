package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVector1;
import uk.badamson.mc.math.ImmutableVector1Test;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Vector;

/**
 * <p>
 * Unit tests for the {@link ImmutableVector1StateSpaceMapper} class.
 * </p>
 */
public class ImmutableVector1StateSpaceMapperTest {

    public static void assertInvariants(ImmutableVector1StateSpaceMapper mapper) {
        VectorStateSpaceMapperTest.assertInvariants(mapper);// inherited

        assertEquals("Number of dimensions", mapper.getDimension(), 1);
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

    private static void fromObject(int index, int stateSize, double state0, double x) {
        final double tolerance = (Math.abs(state0) + Math.abs(x)) * (Math.nextAfter(1.0, 2.0) - 1.0);
        final ImmutableVector1StateSpaceMapper mapper = new ImmutableVector1StateSpaceMapper(index);
        final double[] state = new double[stateSize];
        state[index] = state0;
        final ImmutableVector1 vector = ImmutableVector1.create(x);

        fromObject(mapper, state, vector);

        assertEquals("state[index]", state0 + x, state[index], tolerance);
    }

    private static void fromToObjectSymmetry(ImmutableVector1StateSpaceMapper mapper, double[] state,
            ImmutableVector1 original) {
        mapper.fromObject(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final ImmutableVector1 reconstructed = toObject(mapper, stateVector);

        assertEquals("Symmetric", original, reconstructed);
    }

    private static void fromToObjectSymmetry(int index, int stateSize, ImmutableVector1 original) {
        final ImmutableVector1StateSpaceMapper mapper = new ImmutableVector1StateSpaceMapper(index);
        final double[] state = new double[stateSize];

        fromToObjectSymmetry(mapper, state, original);
    }

    private static void fromToVectorSymmetry(ImmutableVector1StateSpaceMapper mapper, double[] state,
            ImmutableVector1 original) {
        mapper.fromVector(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final ImmutableVector1 reconstructed = toObject(mapper, stateVector);

        assertEquals("Symmetric", original, reconstructed);
    }

    private static void fromToVectorSymmetry(int index, int stateSize, ImmutableVector1 original) {
        final ImmutableVector1StateSpaceMapper mapper = new ImmutableVector1StateSpaceMapper(index);
        final double[] state = new double[stateSize];

        fromToVectorSymmetry(mapper, state, original);
    }

    public static void fromVector(ImmutableVector1StateSpaceMapper mapper, double[] state, Vector vector) {
        VectorStateSpaceMapperTest.fromVector(mapper, state, vector);

        assertInvariants(mapper);// check for side-effects
    }

    public static ImmutableVector1 toObject(ImmutableVector1StateSpaceMapper mapper, ImmutableVectorN state) {
        ImmutableVector1 vector = VectorStateSpaceMapperTest.toObject(mapper, state);

        assertInvariants(mapper);// check for side-effects
        ImmutableVector1Test.assertInvariants(vector);

        return vector;
    }

    @Test
    public void fromObject_2i() {
        final int index = 0;
        final int stateSize = 1;
        final double state0 = 0;
        fromObject(index, stateSize, state0, 2.0);
    }

    @Test
    public void fromObject_extraAfter() {
        final int index = 0;
        final int stateSize = 2;
        final double state0 = 0;
        fromObject(index, stateSize, state0, 1.0);
    }

    @Test
    public void fromObject_extraBefore() {
        final int index = 2;
        final int stateSize = 3;
        final double state0 = 0;
        fromObject(index, stateSize, state0, 1.0);
    }

    @Test
    public void fromObject_i() {
        final int index = 0;
        final int stateSize = 1;
        final double state0 = 0;
        fromObject(index, stateSize, state0, 1.0);
    }

    @Test
    public void fromObject_initialState() {
        final int index = 0;
        final int stateSize = 1;
        final double state0 = 7.0;
        fromObject(index, stateSize, state0, 1.0);
    }

    @Test
    public void fromToObjectSymmetry_2i() {
        final int index = 0;
        final int stateSize = 1;
        fromToObjectSymmetry(index, stateSize, ImmutableVector1.I.scale(2));
    }

    @Test
    public void fromToObjectSymmetry_extraAfter() {
        final int index = 0;
        final int stateSize = 1;
        fromToObjectSymmetry(index, stateSize, ImmutableVector1.I);
    }

    @Test
    public void fromToObjectSymmetry_extraBefore() {
        final int index = 2;
        final int stateSize = 3;
        fromToObjectSymmetry(index, stateSize, ImmutableVector1.I);
    }

    @Test
    public void fromToObjectSymmetry_i() {
        final int index = 0;
        final int stateSize = 1;
        fromToObjectSymmetry(index, stateSize, ImmutableVector1.I);
    }

    @Test
    public void fromToVectorSymmetry_2i() {
        final int index = 0;
        final int stateSize = 1;
        fromToVectorSymmetry(index, stateSize, ImmutableVector1.I.scale(2));
    }

    @Test
    public void fromToVectorSymmetry_extraAfter() {
        final int index = 0;
        final int stateSize = 2;
        fromToVectorSymmetry(index, stateSize, ImmutableVector1.I);
    }

    @Test
    public void fromToVectorSymmetry_extraBefore() {
        final int index = 2;
        final int stateSize = 5;
        fromToVectorSymmetry(index, stateSize, ImmutableVector1.I);
    }

    @Test
    public void fromToVectorSymmetry_i() {
        final int index = 0;
        final int stateSize = 3;
        fromToVectorSymmetry(index, stateSize, ImmutableVector1.I);
    }

}
