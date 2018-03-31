package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVector3;
import uk.badamson.mc.math.ImmutableVector3Test;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Vector;

/**
 * <p>
 * Unit tests for the {@link ImmutableVector3StateSpaceMapper} class.
 * </p>
 */
public class ImmutableVector3StateSpaceMapperTest {

    public static void assertInvariants(ImmutableVector3StateSpaceMapper mapper) {
        VectorStateSpaceMapperTest.assertInvariants(mapper);// inherited
        assertEquals("Number of dimensions", mapper.getDimension(), 3);
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

    private static void fromObject(int index0, int stateSize, final ImmutableVector3 v1, final ImmutableVector3 v2) {
        final double tolerance = (v1.magnitude() + v2.magnitude()) * (Math.nextAfter(1.0, 2.0) - 1.0);
        final ImmutableVector3 sum = v1.plus(v2);
        final ImmutableVector3StateSpaceMapper mapper = new ImmutableVector3StateSpaceMapper(index0);
        final double[] state = new double[stateSize];
        mapper.fromObject(state, v1);

        fromObject(mapper, state, v2);

        assertEquals("state[index0]", sum.get(0), state[index0], tolerance);
        assertEquals("state[index0+1]", sum.get(1), state[index0 + 1], tolerance);
        assertEquals("state[index0+2]", sum.get(2), state[index0 + 2], tolerance);
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

    private static void fromToVectorSymmetry(ImmutableVector3StateSpaceMapper mapper, double[] state,
            ImmutableVector3 original) {
        mapper.fromVector(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final ImmutableVector3 reconstructed = toObject(mapper, stateVector);

        assertEquals("Symmetric", original, reconstructed);
    }

    private static void fromToVectorSymmetry(int index0, int stateSize, ImmutableVector3 original) {
        final ImmutableVector3StateSpaceMapper mapper = new ImmutableVector3StateSpaceMapper(index0);
        final double[] state = new double[stateSize];

        fromToVectorSymmetry(mapper, state, original);
    }

    public static void fromVector(ImmutableVector3StateSpaceMapper mapper, double[] state, Vector vector) {
        VectorStateSpaceMapperTest.fromVector(mapper, state, vector);

        assertInvariants(mapper);// check for side-effects
    }

    public static ImmutableVector3 toObject(ImmutableVector3StateSpaceMapper mapper, ImmutableVectorN state) {
        ImmutableVector3 vector = VectorStateSpaceMapperTest.toObject(mapper, state);

        assertInvariants(mapper);// check for side-effects
        ImmutableVector3Test.assertInvariants(vector);

        return vector;
    }

    @Test
    public void fromObject_2i() {
        final int index = 0;
        final int stateSize = 3;
        fromObject(index, stateSize, ImmutableVector3.ZERO, ImmutableVector3.I.scale(2));
    }

    @Test
    public void fromObject_extraAfter() {
        final int index = 0;
        final int stateSize = 5;
        fromObject(index, stateSize, ImmutableVector3.ZERO, ImmutableVector3.I);
    }

    @Test
    public void fromObject_extraBefore() {
        final int index = 2;
        final int stateSize = 5;
        fromObject(index, stateSize, ImmutableVector3.ZERO, ImmutableVector3.I);
    }

    @Test
    public void fromObject_i() {
        final int index = 0;
        final int stateSize = 3;
        fromObject(index, stateSize, ImmutableVector3.ZERO, ImmutableVector3.I);
    }

    @Test
    public void fromObject_initialState() {
        final int index = 0;
        final int stateSize = 3;
        final ImmutableVector3 v = ImmutableVector3.create(1, 2, 3);
        fromObject(index, stateSize, v, v);
    }

    @Test
    public void fromObject_j() {
        final int index = 0;
        final int stateSize = 3;
        fromObject(index, stateSize, ImmutableVector3.ZERO, ImmutableVector3.J);
    }

    @Test
    public void fromObject_k() {
        final int index = 0;
        final int stateSize = 3;
        fromObject(index, stateSize, ImmutableVector3.ZERO, ImmutableVector3.K);
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

    @Test
    public void fromToVectorSymmetry_2i() {
        final int index0 = 0;
        final int stateSize = 3;
        fromToVectorSymmetry(index0, stateSize, ImmutableVector3.I.scale(2));
    }

    @Test
    public void fromToVectorSymmetry_extraAfter() {
        final int index0 = 0;
        final int stateSize = 5;
        fromToVectorSymmetry(index0, stateSize, ImmutableVector3.I);
    }

    @Test
    public void fromToVectorSymmetry_extraBefore() {
        final int index0 = 2;
        final int stateSize = 5;
        fromToVectorSymmetry(index0, stateSize, ImmutableVector3.I);
    }

    @Test
    public void fromToVectorSymmetry_i() {
        final int index0 = 0;
        final int stateSize = 3;
        fromToVectorSymmetry(index0, stateSize, ImmutableVector3.I);
    }

    @Test
    public void fromToVectorSymmetry_j() {
        final int index0 = 0;
        final int stateSize = 3;
        fromToVectorSymmetry(index0, stateSize, ImmutableVector3.J);
    }

    @Test
    public void fromToVectorSymmetry_k() {
        final int index0 = 0;
        final int stateSize = 3;
        fromToVectorSymmetry(index0, stateSize, ImmutableVector3.K);
    }
}
