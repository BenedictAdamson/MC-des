package uk.badamson.mc.physics.kinematics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVector1;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Vector;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTermTest;
import uk.badamson.mc.physics.ImmutableVector1StateSpaceMapper;
import uk.badamson.mc.physics.ImmutableVector3StateSpaceMapper;
import uk.badamson.mc.physics.VectorStateSpaceMapper;
import uk.badamson.mc.physics.VectorStateSpaceMapperTest;

/**
 * <p>
 * Unit tests for the class {@link VelocityError}.
 * </p>
 */
public class VelocityErrorTest {

    private static final double MASS_1 = 2.0;

    private static final double MASS_2 = 1E24;

    public static <VECTOR extends Vector> void assertInvariants(VelocityError<VECTOR> term) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

        final double mass = term.getMass();
        final int spaceDimension = term.getSpaceDimension();
        final VectorStateSpaceMapper<VECTOR> velocityVectorMapper = term.getVelocityVectorMapper();
        final VectorStateSpaceMapper<VECTOR> accelerationVectorMapper = term.getAccelerationVectorMapper();

        assertThat("velocityVectorMapper", velocityVectorMapper, org.hamcrest.core.IsNull.notNullValue());// guard
        assertThat("accelerationVectorMapper", accelerationVectorMapper, org.hamcrest.core.IsNull.notNullValue());// guard

        VectorStateSpaceMapperTest.assertInvariants(velocityVectorMapper);
        VectorStateSpaceMapperTest.assertInvariants(accelerationVectorMapper);
        VectorStateSpaceMapperTest.assertInvariants(velocityVectorMapper, accelerationVectorMapper);

        final int velocityVectorMapperDimension = velocityVectorMapper.getDimension();

        assertTrue("Mass <" + mass + "> is positive and  finite", 0.0 < mass && Double.isFinite(mass));
        assertThat("spaceDimension", spaceDimension, org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo(1));
        assertEquals("The number of dimensions equals the number of dimensions of the velocity vector mapper.",
                velocityVectorMapperDimension, spaceDimension);
        assertEquals(
                "The dimension of the acceleration vector mapper is equal to the dimension of the velocity vector mapper.",
                velocityVectorMapperDimension, accelerationVectorMapper.getDimension());
    }

    public static <VECTOR extends Vector> void assertInvariants(VelocityError<VECTOR> term1,
            VelocityError<VECTOR> term2) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
    }

    private static <VECTOR extends Vector> VelocityError<VECTOR> constructor(double mass,
            VectorStateSpaceMapper<VECTOR> velocityVectorMapper,
            VectorStateSpaceMapper<VECTOR> accelerationVectorMapper) {
        final VelocityError<VECTOR> term = new VelocityError<>(mass, velocityVectorMapper, accelerationVectorMapper);

        assertInvariants(term);

        assertEquals("mass", mass, term.getMass(), Double.MIN_NORMAL);
        assertSame("velocityVectorMapper", velocityVectorMapper, term.getVelocityVectorMapper());
        assertSame("accelerationVectorMapper", accelerationVectorMapper, term.getAccelerationVectorMapper());

        return term;
    }

    private static <VECTOR extends Vector> double evaluate(VelocityError<VECTOR> term, double[] dedx,
            ImmutableVectorN x0, ImmutableVectorN x, double dt) {
        final double e = AbstractTimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, x0, x, dt);

        assertInvariants(term);

        return e;
    }

    private static final void evaluate_1(double mass, double dedv0, double deda0, double v0, double a0, double v,
            double a, double dt, double eExpected, double dEDVExpected, double dEDAExpected, double tolerance) {
        final int velocityTerm = 0;
        final int accelerationTerm = 1;
        final ImmutableVector1StateSpaceMapper velocityVectorMapper = new ImmutableVector1StateSpaceMapper(
                velocityTerm);
        final ImmutableVector1StateSpaceMapper accelerationVectorMapper = new ImmutableVector1StateSpaceMapper(
                accelerationTerm);
        final double[] dedx = { dedv0, deda0 };
        final ImmutableVectorN state0 = ImmutableVectorN.create(v0, a0);
        final ImmutableVectorN state = ImmutableVectorN.create(v, a);
        final VelocityError<ImmutableVector1> term = new VelocityError<>(mass, velocityVectorMapper,
                accelerationVectorMapper);

        final double e = evaluate(term, dedx, state0, state, dt);

        assertEquals("energy", eExpected, e, tolerance);
        assertEquals("dedx[velocityTerm]", dEDVExpected, dedx[velocityTerm], tolerance);
        assertEquals("dedx[accelerationTerm]", dEDAExpected, dedx[accelerationTerm], tolerance);
    }

    private static final void evaluate_1Minimum(double mass, double dedv0, double deda0, double v0, double a0, double v,
            double a, double dt, double tolerance) {
        final double eExpected = 0.0;
        final double dEDVExpected = dedv0;
        final double dEDAExpected = deda0;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void constructor_1A() {
        ImmutableVector1StateSpaceMapper velocityVectorMapper = new ImmutableVector1StateSpaceMapper(3);
        ImmutableVector1StateSpaceMapper accelerationVectorMapper = new ImmutableVector1StateSpaceMapper(4);

        constructor(MASS_1, velocityVectorMapper, accelerationVectorMapper);
    }

    @Test
    public void constructor_1B() {
        ImmutableVector1StateSpaceMapper velocityVectorMapper = new ImmutableVector1StateSpaceMapper(7);
        ImmutableVector1StateSpaceMapper accelerationVectorMapper = new ImmutableVector1StateSpaceMapper(11);

        constructor(MASS_2, velocityVectorMapper, accelerationVectorMapper);
    }

    @Test
    public void constructor_3() {
        ImmutableVector3StateSpaceMapper velocityVectorMapper = new ImmutableVector3StateSpaceMapper(3);
        ImmutableVector3StateSpaceMapper accelerationVectorMapper = new ImmutableVector3StateSpaceMapper(4);

        constructor(MASS_1, velocityVectorMapper, accelerationVectorMapper);
    }

    @Test
    public void evaluate_1A() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 2.0;
        final double dt = 1.0;
        final double eExpected = 0.5;
        final double dEDVExpected = -1.0;
        final double dEDAExpected = 0.5;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1A0() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 2.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double eExpected = 0.5;
        final double dEDVExpected = -1.0;
        final double dEDAExpected = 0.5;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1DT() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 2.0;
        final double dt = 2.0;
        final double eExpected = 2.0;
        final double dEDVExpected = -2.0;
        final double dEDAExpected = 2.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1MinimumA0A() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0;
        final double v0 = 0.0;
        final double a0 = 2.0;
        final double v = 0.0;
        final double a = -2.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumA0V() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0;
        final double v0 = 0.0;
        final double a0 = 2.0;
        final double v = 1.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumBase() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumDEDA0() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 2.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumDEDV0() {
        final double mass = 1.0;
        final double dedv0 = 2.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumMass() {
        final double mass = 2.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumV0A() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0;
        final double v0 = 2.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = -4.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumV0V() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0;
        final double v0 = 2.0;
        final double a0 = 0.0;
        final double v = 2.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, dedv0, deda0, v0, a0, v, a, dt, tolerance);
    }

    @Test
    public void evaluate_1V() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 2.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double eExpected = 2.0;
        final double dEDVExpected = 2.0;
        final double dEDAExpected = -1.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1V0() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 2.0;
        final double a0 = 0.0;
        final double v = 0.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double eExpected = 2.0;
        final double dEDVExpected = -2.0;
        final double dEDAExpected = 1.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1VDT() {
        final double mass = 1.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 2.0;
        final double a = 0.0;
        final double dt = 2.0;
        final double eExpected = 2.0;
        final double dEDVExpected = 2.0;
        final double dEDAExpected = -2.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }

    @Test
    public void evaluate_1VM() {
        final double mass = 2.0;
        final double dedv0 = 0.0;
        final double deda0 = 0.0;
        final double v0 = 0.0;
        final double a0 = 0.0;
        final double v = 2.0;
        final double a = 0.0;
        final double dt = 1.0;
        final double eExpected = 4.0;
        final double dEDVExpected = 4.0;
        final double dEDAExpected = -2.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, dedv0, deda0, v0, a0, v, a, dt, eExpected, dEDVExpected, dEDAExpected, tolerance);
    }
}
