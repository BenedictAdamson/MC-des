package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static uk.badamson.mc.math.Rotation3Test.closeToRotation3;

import java.util.Arrays;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVector3;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Rotation3AxisAngle;
import uk.badamson.mc.math.Rotation3AxisAngleTest;

/**
 * <p>
 * Unit tests for the {@link Rotation3AxisAngleStateSpaceMapper} class.
 * </p>
 */
public class Rotation3AxisAngleStateSpaceMapperTest {

    private static final double SMALL_ANGLE = Math.PI * 0.003;

    public static void assertInvariants(Rotation3AxisAngleStateSpaceMapper mapper) {
        ObjectStateSpaceMapperTest.assertInvariants(mapper);// inherited
    }

    public static void assertInvariants(Rotation3AxisAngleStateSpaceMapper mapper1,
            Rotation3AxisAngleStateSpaceMapper mapper2) {
        ObjectStateSpaceMapperTest.assertInvariants(mapper1, mapper2);// inherited
    }

    private static void fromObject(int index0, int stateSize, final Rotation3AxisAngle r1,
            final Rotation3AxisAngle r2) {
        final double tolerance = 2 * (Math.nextAfter(1.0, 2.0) - 1.0);
        final ImmutableVector3StateSpaceMapper axisMapper = new ImmutableVector3StateSpaceMapper(index0 + 1);
        final Rotation3AxisAngleStateSpaceMapper rotationMapper = new Rotation3AxisAngleStateSpaceMapper(index0,
                axisMapper);
        final double[] state = new double[stateSize];
        rotationMapper.fromObject(state, r1);
        final double[] state0 = Arrays.copyOf(state, stateSize);

        fromObject(rotationMapper, state, r2);

        assertEquals("state[index0]", state0[index0] + r2.getAngle(), state[index0], tolerance);
        assertEquals("state[index0+1]", state0[index0 + 1] + r2.getAxis().get(0), state[index0 + 1], tolerance);
        assertEquals("state[index0+2]", state0[index0 + 2] + r2.getAxis().get(1), state[index0 + 2], tolerance);
        assertEquals("state[index0+3]", state0[index0 + 3] + r2.getAxis().get(2), state[index0 + 3], tolerance);
    }

    public static void fromObject(Rotation3AxisAngleStateSpaceMapper mapper, double[] state,
            Rotation3AxisAngle quaternion) {
        ObjectStateSpaceMapperTest.fromObject(mapper, state, quaternion);

        assertInvariants(mapper);// check for side-effects
        Rotation3AxisAngleTest.assertInvariants(quaternion);// check for side-effects
    }

    private static void fromToObjectSymmetry(int index0, int stateSize, Rotation3AxisAngle original) {
        final ImmutableVector3StateSpaceMapper axisMapper = new ImmutableVector3StateSpaceMapper(index0 + 1);
        final Rotation3AxisAngleStateSpaceMapper rotationMapper = new Rotation3AxisAngleStateSpaceMapper(index0,
                axisMapper);
        final double[] state = new double[stateSize];

        fromToObjectSymmetry(rotationMapper, state, original);
    }

    private static void fromToObjectSymmetry(Rotation3AxisAngleStateSpaceMapper mapper, double[] state,
            Rotation3AxisAngle original) {
        mapper.fromObject(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final Rotation3AxisAngle reconstructed = toObject(mapper, stateVector);

        assertThat("symmetric", reconstructed, closeToRotation3(original));
    }

    public static Rotation3AxisAngle toObject(Rotation3AxisAngleStateSpaceMapper mapper, ImmutableVectorN state) {
        Rotation3AxisAngle vector = ObjectStateSpaceMapperTest.toObject(mapper, state);

        assertInvariants(mapper);// check for side-effects
        Rotation3AxisAngleTest.assertInvariants(vector);

        return vector;
    }

    @Test
    public void fromObject_0() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Rotation3AxisAngle.ZERO, Rotation3AxisAngle.ZERO);
    }

    @Test
    public void fromObject_extraAfter() {
        final int index = 0;
        final int stateSize = 6;
        fromObject(index, stateSize, Rotation3AxisAngle.ZERO,
                Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromObject_extraBefore() {
        final int index = 2;
        final int stateSize = 6;
        fromObject(index, stateSize, Rotation3AxisAngle.ZERO,
                Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromObject_iA() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Rotation3AxisAngle.ZERO,
                Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromObject_iB() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Rotation3AxisAngle.ZERO,
                Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.I, Math.PI * 0.5));
    }

    @Test
    public void fromObject_initialState() {
        final int index = 0;
        final int stateSize = 4;
        final Rotation3AxisAngle q = Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.create(1, 2, 3), SMALL_ANGLE);
        fromObject(index, stateSize, q, q);
    }

    @Test
    public void fromObject_j() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Rotation3AxisAngle.ZERO,
                Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE));
    }

    @Test
    public void fromObject_k() {
        final int index = 0;
        final int stateSize = 4;
        fromObject(index, stateSize, Rotation3AxisAngle.ZERO,
                Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE));
    }

    @Test
    public void fromToObjectSymmetry_extraAfter() {
        final int index0 = 0;
        final int stateSize = 6;
        fromToObjectSymmetry(index0, stateSize, Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromToObjectSymmetry_extraBefore() {
        final int index0 = 2;
        final int stateSize = 6;
        fromToObjectSymmetry(index0, stateSize, Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromToObjectSymmetry_ia() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.I, SMALL_ANGLE));
    }

    @Test
    public void fromToObjectSymmetry_ib() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.I, Math.PI * 0.5));
    }

    @Test
    public void fromToObjectSymmetry_j() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.J, SMALL_ANGLE));
    }

    @Test
    public void fromToObjectSymmetry_k() {
        final int index0 = 0;
        final int stateSize = 4;
        fromToObjectSymmetry(index0, stateSize, Rotation3AxisAngle.valueOfAxisAngle(ImmutableVector3.K, SMALL_ANGLE));
    }

}
