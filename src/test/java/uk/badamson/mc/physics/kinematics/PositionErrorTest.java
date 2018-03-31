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
 * Unit tests for the class {@link PositionError}.
 * </p>
 */
public class PositionErrorTest {

    private static final double MASS_1 = 2.0;

    private static final double MASS_2 = 1E24;

    public static <VECTOR extends Vector> void assertInvariants(PositionError<VECTOR> term) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

        final double mass = term.getMass();
        final int spaceDimension = term.getSpaceDimension();
        final VectorStateSpaceMapper<VECTOR> positionVectorMapper = term.getPositionVectorMapper();
        final VectorStateSpaceMapper<VECTOR> velocityVectorMapper = term.getVelocityVectorMapper();

        assertThat("positionVectorMapper", positionVectorMapper, org.hamcrest.core.IsNull.notNullValue());// guard
        assertThat("velocityVectorMapper", velocityVectorMapper, org.hamcrest.core.IsNull.notNullValue());// guard

        VectorStateSpaceMapperTest.assertInvariants(positionVectorMapper);
        VectorStateSpaceMapperTest.assertInvariants(velocityVectorMapper);
        VectorStateSpaceMapperTest.assertInvariants(positionVectorMapper, velocityVectorMapper);

        final int positionVectorMapperDimension = positionVectorMapper.getDimension();

        assertTrue("Mass <" + mass + "> is positive and  finite", 0.0 < mass && Double.isFinite(mass));
        assertThat("spaceDimension", spaceDimension, org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo(1));
        assertEquals("The number of dimensions equals the number of dimensions of the position vector mapper.",
                positionVectorMapperDimension, spaceDimension);
        assertEquals(
                "The dimension of the velocity vector mapper is equal to the dimension of the position vector mapper.",
                positionVectorMapperDimension, velocityVectorMapper.getDimension());
    }

    public static <VECTOR extends Vector> void assertInvariants(PositionError<VECTOR> term1,
            PositionError<VECTOR> term2) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
    }

    private static <VECTOR extends Vector> PositionError<VECTOR> constructor(double mass,
            VectorStateSpaceMapper<VECTOR> positionVectorMapper, VectorStateSpaceMapper<VECTOR> velocityVectorMapper) {
        final PositionError<VECTOR> term = new PositionError<>(mass, positionVectorMapper, velocityVectorMapper);

        assertInvariants(term);

        assertEquals("mass", mass, term.getMass(), Double.MIN_NORMAL);
        assertSame("positionVectorMapper", positionVectorMapper, term.getPositionVectorMapper());
        assertSame("velocityVectorMapper", velocityVectorMapper, term.getVelocityVectorMapper());

        return term;
    }

    private static <VECTOR extends Vector> double evaluate(PositionError<VECTOR> term, double[] dedx,
            ImmutableVectorN x0, ImmutableVectorN x, double dt) {
        final double e = AbstractTimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, x0, x, dt);

        assertInvariants(term);

        return e;
    }

    private static final void evaluate_1(double mass, int positionTerm, int velocityTerm, double dedx0, double dedv0,
            double x0, double v0, double x, double v, double dt, double eExpected, double dEDXExpected,
            double dEDVExpected, double tolerance) {
        final ImmutableVector1StateSpaceMapper positionVectorMapper = new ImmutableVector1StateSpaceMapper(
                positionTerm);
        final ImmutableVector1StateSpaceMapper velocityVectorMapper = new ImmutableVector1StateSpaceMapper(
                velocityTerm);
        final PositionError<ImmutableVector1> term = new PositionError<>(mass, positionVectorMapper,
                velocityVectorMapper);
        final double[] dedx = { dedx0, dedv0 };

        final double e = evaluate(term, dedx, ImmutableVectorN.create(x0, v0), ImmutableVectorN.create(x, v), dt);

        assertEquals("energy", eExpected, e, tolerance);
        assertEquals("dedx[positionTerm]", dEDXExpected, dedx[positionTerm], tolerance);
        assertEquals("dedx[velocityTerm]", dEDVExpected, dedx[velocityTerm], tolerance);
    }

    private static void evaluate_1Minimum(final double mass, final int positionTerm, final int velocityTerm,
            final double dedx0, final double dedv0, final double x0, final double v0, final double x, final double v,
            final double dt, final double tolerance) {
        final double eExpected = 0.0;
        final double dEDXExpected = dedx0;
        final double dEDVExpected = dedv0;

        evaluate_1(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
                dEDVExpected, tolerance);
    }

    @Test
    public void constructor_1A() {
        ImmutableVector1StateSpaceMapper positionVectorMapper = new ImmutableVector1StateSpaceMapper(3);
        ImmutableVector1StateSpaceMapper velocityVectorMapper = new ImmutableVector1StateSpaceMapper(4);

        constructor(MASS_1, positionVectorMapper, velocityVectorMapper);
    }

    @Test
    public void constructor_1B() {
        ImmutableVector1StateSpaceMapper positionVectorMapper = new ImmutableVector1StateSpaceMapper(7);
        ImmutableVector1StateSpaceMapper velocityVectorMapper = new ImmutableVector1StateSpaceMapper(11);

        constructor(MASS_2, positionVectorMapper, velocityVectorMapper);
    }

    @Test
    public void constructor_3() {
        ImmutableVector3StateSpaceMapper positionVectorMapper = new ImmutableVector3StateSpaceMapper(3);
        ImmutableVector3StateSpaceMapper velocityVectorMapper = new ImmutableVector3StateSpaceMapper(4);

        constructor(MASS_1, positionVectorMapper, velocityVectorMapper);
    }

    @Test
    public void evaluate_1MassX0() {
        final double mass = 2.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 0.0;
        final double dedv0 = 0.0;
        final double x0 = 2.0;
        final double v0 = 0.0;
        final double x = 0.0;
        final double v = 0.0;
        final double dt = 1.0;
        final double eExpected = 4.0;
        final double dEDXExpected = -4.0;
        final double dEDVExpected = 2.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
                dEDVExpected, tolerance);
    }

    @Test
    public void evaluate_1MinimumBase() {
        final double mass = 1.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 0.0;
        final double dedv0 = 0.0;
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double x = 0.0;
        final double v = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumDEDV0() {
        final double mass = 1.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 0.0;
        final double dedv0 = 2.0;
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double x = 0.0;
        final double v = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumDEDX0() {
        final double mass = 1.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 2.0;
        final double dedv0 = 0.0;
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double x = 0.0;
        final double v = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumMass() {
        final double mass = 2.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 0.0;
        final double dedv0 = 0.0;
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double x = 0.0;
        final double v = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumMoving() {
        final double mass = 1.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 0.0;
        final double dedv0 = 0.0;
        final double x0 = 0.0;
        final double v0 = 1.0;
        final double x = 1.0;
        final double v = v0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
    }

    @Test
    public void evaluate_1MinimumTerms() {
        final double mass = 1.0;
        final int positionTerm = 1;
        final int velocityTerm = 0;
        final double dedx0 = 0.0;
        final double dedv0 = 0.0;
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double x = 0.0;
        final double v = 0.0;
        final double dt = 1.0;
        final double tolerance = 1E-3;

        evaluate_1Minimum(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, tolerance);
    }

    @Test
    public void evaluate_1V() {
        final double mass = 1.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 0.0;
        final double dedv0 = 0.0;
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double x = 0.0;
        final double v = 2.0;
        final double dt = 1.0;
        final double eExpected = 0.5;
        final double dEDXExpected = -1.0;
        final double dEDVExpected = 0.5;
        final double tolerance = 1E-3;

        evaluate_1(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
                dEDVExpected, tolerance);
    }

    @Test
    public void evaluate_1V0() {
        final double mass = 1.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 0.0;
        final double dedv0 = 0.0;
        final double x0 = 0.0;
        final double v0 = 2.0;
        final double x = 0.0;
        final double v = 0.0;
        final double dt = 1.0;
        final double eExpected = 0.5;
        final double dEDXExpected = -1.0;
        final double dEDVExpected = 0.5;
        final double tolerance = 1E-3;

        evaluate_1(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
                dEDVExpected, tolerance);
    }

    @Test
    public void evaluate_1X() {
        final double mass = 1.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 0.0;
        final double dedv0 = 0.0;
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double x = 2.0;
        final double v = 0.0;
        final double dt = 1.0;
        final double eExpected = 2.0;
        final double dEDXExpected = 2.0;
        final double dEDVExpected = -1.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
                dEDVExpected, tolerance);
    }

    @Test
    public void evaluate_1X0() {
        final double mass = 1.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 0.0;
        final double dedv0 = 0.0;
        final double x0 = 2.0;
        final double v0 = 0.0;
        final double x = 0.0;
        final double v = 0.0;
        final double dt = 1.0;
        final double eExpected = 2.0;
        final double dEDXExpected = -2.0;
        final double dEDVExpected = 1.0;
        final double tolerance = 1E-3;

        evaluate_1(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
                dEDVExpected, tolerance);
    }

    @Test
    public void evaluate_1XDT() {
        final double mass = 1.0;
        final int positionTerm = 0;
        final int velocityTerm = 1;
        final double dedx0 = 0.0;
        final double dedv0 = 0.0;
        final double x0 = 0.0;
        final double v0 = 0.0;
        final double x = 2.0;
        final double v = 0.0;
        final double dt = 2.0;
        final double eExpected = 0.5;
        final double dEDXExpected = 0.5;
        final double dEDVExpected = -0.5;
        final double tolerance = 1E-3;

        evaluate_1(mass, positionTerm, velocityTerm, dedx0, dedv0, x0, v0, x, v, dt, eExpected, dEDXExpected,
                dEDVExpected, tolerance);
    }
}
