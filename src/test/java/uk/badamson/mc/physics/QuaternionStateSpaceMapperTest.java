package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Quaternion;
import uk.badamson.mc.math.QuaternionTest;

/**
 * <p>
 * Unit tests for the {@link QuaternionStateSpaceMapper} class.
 * </p>
 */
public class QuaternionStateSpaceMapperTest {

    public static void assertInvariants(QuaternionStateSpaceMapper mapper) {
        ObjectStateSpaceMapperTest.assertInvariants(mapper);// inherited
    }

    public static void assertInvariants(QuaternionStateSpaceMapper mapper1, QuaternionStateSpaceMapper mapper2) {
        ObjectStateSpaceMapperTest.assertInvariants(mapper1, mapper2);// inherited
    }

    private static void fromObject(int index0, int stateSize, final Quaternion q1, final Quaternion q2) {
        final double tolerance = (q1.norm() + q2.norm()) * (Math.nextAfter(1.0, 2.0) - 1.0);
        final Quaternion sum = q1.plus(q2);
        final QuaternionStateSpaceMapper mapper = new QuaternionStateSpaceMapper(index0);
        final double[] state = new double[stateSize];
        mapper.fromObject(state, q1);

        fromObject(mapper, state, q2);

        assertEquals("state[index0]", sum.getA(), state[index0], tolerance);
        assertEquals("state[index0+1]", sum.getB(), state[index0 + 1], tolerance);
        assertEquals("state[index0+2]", sum.getC(), state[index0 + 2], tolerance);
        assertEquals("state[index0+3]", sum.getD(), state[index0 + 3], tolerance);
    }

    public static void fromObject(QuaternionStateSpaceMapper mapper, double[] state, Quaternion quaternion) {
        ObjectStateSpaceMapperTest.fromObject(mapper, state, quaternion);

        assertInvariants(mapper);// check for side-effects
        QuaternionTest.assertInvariants(quaternion);// check for side-effects
    }

    private static void fromToObjectSymmetry(int index0, int stateSize, Quaternion original) {
        final QuaternionStateSpaceMapper mapper = new QuaternionStateSpaceMapper(index0);
        final double[] state = new double[stateSize];

        fromToObjectSymmetry(mapper, state, original);
    }

    private static void fromToObjectSymmetry(QuaternionStateSpaceMapper mapper, double[] state, Quaternion original) {
        mapper.fromObject(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final Quaternion reconstructed = toObject(mapper, stateVector);

        assertEquals("Symmetric", original, reconstructed);
    }

    public static Quaternion toObject(QuaternionStateSpaceMapper mapper, ImmutableVectorN state) {
        Quaternion vector = ObjectStateSpaceMapperTest.toObject(mapper, state);

        assertInvariants(mapper);// check for side-effects
        QuaternionTest.assertInvariants(vector);

        return vector;
    }

    @Test
    public void fromObject_2i() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Quaternion.ZERO, Quaternion.I.scale(2));
    }

    @Test
    public void fromObject_extraAfter() {
        final int index = 0;
        final int stateSize = 6;
        fromObject(index, stateSize, Quaternion.ZERO, Quaternion.I);
    }

    @Test
    public void fromObject_extraBefore() {
        final int index = 2;
        final int stateSize = 6;
        fromObject(index, stateSize, Quaternion.ZERO, Quaternion.I);
    }

    @Test
    public void fromObject_i() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Quaternion.ZERO, Quaternion.I);
    }

    @Test
    public void fromObject_initialState() {
        final int index = 0;
        final int stateSize = 4;
        final Quaternion q = Quaternion.create(1, 2, 3, 4);
        fromObject(index, stateSize, q, q);
    }

    @Test
    public void fromObject_j() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Quaternion.ZERO, Quaternion.J);
    }

    @Test
    public void fromObject_k() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Quaternion.ZERO, Quaternion.K);
    }

    @Test
    public void fromToObjectSymmetry_2i() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Quaternion.I.scale(2));
    }

    @Test
    public void fromToObjectSymmetry_extraAfter() {
        final int index0 = 0;
        final int stateSize = 6;
        fromToObjectSymmetry(index0, stateSize, Quaternion.I);
    }

    @Test
    public void fromToObjectSymmetry_extraBefore() {
        final int index0 = 2;
        final int stateSize = 6;
        fromToObjectSymmetry(index0, stateSize, Quaternion.I);
    }

    @Test
    public void fromToObjectSymmetry_i() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Quaternion.I);
    }

    @Test
    public void fromToObjectSymmetry_j() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Quaternion.J);
    }

    @Test
    public void fromToObjectSymmetry_k() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Quaternion.K);
    }

}
