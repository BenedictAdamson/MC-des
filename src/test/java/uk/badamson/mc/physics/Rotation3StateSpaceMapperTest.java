package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVector3;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Quaternion;
import uk.badamson.mc.math.Rotation3;
import uk.badamson.mc.math.Rotation3Test;

/**
 * <p>
 * Unit tests for the {@link Rotation3StateSpaceMapper} class.
 * </p>
 */
public class Rotation3StateSpaceMapperTest {

    private static final double SMALL_ANGLE = Math.PI * 0.003;

    public static void assertInvariants(Rotation3StateSpaceMapper mapper) {
        ObjectStateSpaceMapperTest.assertInvariants(mapper);// inherited
    }

    public static void assertInvariants(Rotation3StateSpaceMapper mapper1, Rotation3StateSpaceMapper mapper2) {
        ObjectStateSpaceMapperTest.assertInvariants(mapper1, mapper2);// inherited
    }

    private static void fromObject(int index0, int stateSize, final Rotation3 r1, final Rotation3 r2) {
        final double tolerance = 2 * (Math.nextAfter(1.0, 2.0) - 1.0);
        final Quaternion sum = r1.getVersor().plus(r2.getVersor());
        final QuaternionStateSpaceMapper quaternionMapper = new QuaternionStateSpaceMapper(index0);
        final Rotation3StateSpaceMapper rotationMapper = new Rotation3StateSpaceMapper(quaternionMapper);
        final double[] state = new double[stateSize];
        rotationMapper.fromObject(state, r1);

        fromObject(rotationMapper, state, r2);

        assertEquals("state[index0]", sum.getA(), state[index0], tolerance);
        assertEquals("state[index0+1]", sum.getB(), state[index0 + 1], tolerance);
        assertEquals("state[index0+2]", sum.getC(), state[index0 + 2], tolerance);
        assertEquals("state[index0+3]", sum.getD(), state[index0 + 3], tolerance);
    }

    public static void fromObject(Rotation3StateSpaceMapper mapper, double[] state, Rotation3 quaternion) {
        ObjectStateSpaceMapperTest.fromObject(mapper, state, quaternion);

        assertInvariants(mapper);// check for side-effects
        Rotation3Test.assertInvariants(quaternion);// check for side-effects
    }

    private static void fromToObjectSymmetry(int index0, int stateSize, Rotation3 original) {
        final QuaternionStateSpaceMapper quaternionMapper = new QuaternionStateSpaceMapper(index0);
        final Rotation3StateSpaceMapper rotationMapper = new Rotation3StateSpaceMapper(quaternionMapper);
        final double[] state = new double[stateSize];

        fromToObjectSymmetry(rotationMapper, state, original);
    }

    private static void fromToObjectSymmetry(Rotation3StateSpaceMapper mapper, double[] state, Rotation3 original) {
        mapper.fromObject(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final Rotation3 reconstructed = toObject(mapper, stateVector);

        assertThat("symmetric", reconstructed, Rotation3Test.closeToRotation3(original));
    }

    public static Rotation3 toObject(Rotation3StateSpaceMapper mapper, ImmutableVectorN state) {
        Rotation3 vector = ObjectStateSpaceMapperTest.toObject(mapper, state);

        assertInvariants(mapper);// check for side-effects
        Rotation3Test.assertInvariants(vector);

        return vector;
    }

    @Test
    public void fromObject_0() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Rotation3.ZERO, Rotation3.ZERO);
    }

    @Test
    public void fromObject_extraAfter() {
        final int index = 0;
        final int stateSize = 6;
        fromObject(index, stateSize, Rotation3.ZERO, Rotation3.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromObject_extraBefore() {
        final int index = 2;
        final int stateSize = 6;
        fromObject(index, stateSize, Rotation3.ZERO, Rotation3.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromObject_iA() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Rotation3.ZERO, Rotation3.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromObject_iB() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Rotation3.ZERO, Rotation3.valueOfAxisAngle(ImmutableVector3.I, Math.PI * 0.5));
    }

    @Test
    public void fromObject_initialState() {
        final int index = 0;
        final int stateSize = 4;
        final Rotation3 q = Rotation3.valueOfAxisAngle(ImmutableVector3.create(1, 2, 3), SMALL_ANGLE);
        fromObject(index, stateSize, q, q);
    }

    @Test
    public void fromObject_j() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Rotation3.ZERO, Rotation3.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE));
    }

    @Test
    public void fromObject_k() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Rotation3.ZERO, Rotation3.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE));
    }

    @Test
    public void fromToObjectSymmetry_extraAfter() {
        final int index0 = 0;
        final int stateSize = 6;
        fromToObjectSymmetry(index0, stateSize, Rotation3.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromToObjectSymmetry_extraBefore() {
        final int index0 = 2;
        final int stateSize = 6;
        fromToObjectSymmetry(index0, stateSize, Rotation3.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromToObjectSymmetry_ia() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Rotation3.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromToObjectSymmetry_ib() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Rotation3.valueOfAxisAngle(ImmutableVector3.I, Math.PI * 0.5));
    }

    @Test
    public void fromToObjectSymmetry_j() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Rotation3.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE));
    }

    @Test
    public void fromToObjectSymmetry_k() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Rotation3.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE));
    }

}
