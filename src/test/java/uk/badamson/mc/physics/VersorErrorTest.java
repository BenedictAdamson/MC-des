package uk.badamson.mc.physics;

import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.Quaternion;

/**
 * <p>
 * Units tests for the class {@link VersorError}.
 * </p>
 */
public class VersorErrorTest {

    private static final double SMALL = 1E-3;

    private static final double LENGTH_1 = 1.0;
    private static final double LENGTH_2 = 1E7;

    private static final double MASS_1 = 1.0;
    private static final double MASS_2 = 1E24;

    private static final double DT_1 = 1.0;
    private static final double DT_2 = 1E-3;

    public static void assertInvariants(VersorError term) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

        final double mass = term.getMass();
        final double length = term.getLength();
        final QuaternionStateSpaceMapper quaternionMapper = term.getQuaternionMapper();

        assertNotNull("quaternionMapper", quaternionMapper);// guard

        AbstractTimeStepEnergyErrorFunctionTermTest.assertIsReferenceScale("mass", mass);
        AbstractTimeStepEnergyErrorFunctionTermTest.assertIsReferenceScale("length", length);
    }

    public static void assertInvariants(VersorError term1, VersorError term2) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
    }

    private static VersorError constructor(double length, double mass, QuaternionStateSpaceMapper quaternionMapper) {
        final VersorError term = new VersorError(length, mass, quaternionMapper);

        assertInvariants(term);

        assertEquals("length", length, term.getLength(), Double.MIN_NORMAL);
        assertEquals("mass", mass, term.getMass(), Double.MIN_NORMAL);
        assertSame("quaternionMapper", quaternionMapper, term.getQuaternionMapper());

        return term;
    }

    private static double evaluate(VersorError term, double[] dedx, ImmutableVectorN x0, ImmutableVectorN x,
            double dt) {
        final double e = AbstractTimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, x0, x, dt);

        assertInvariants(term);

        return e;
    }

    private static void evaluate_smallError(double length, double mass, Quaternion versor, Quaternion dq, double dt) {
        final Quaternion q = versor.plus(dq);
        final double qe = q.norm() - 1.0;
        final double le = qe * length;
        final double ve = le / dt;
        final double eExpected = 0.5 * mass * ve * ve;
        final double deda2 = -mass * ve * length / dt;
        final double eTolerance = tolerance(eExpected) * 5.0;
        final double dedxTolerance = 1E-6;

        final QuaternionStateSpaceMapper quaternionMapper = new QuaternionStateSpaceMapper(0);
        final VersorError term = new VersorError(length, mass, quaternionMapper);
        final double[] dedx = new double[4];
        final ImmutableVectorN x0 = ImmutableVectorN.create0(4);
        final ImmutableVectorN x = ImmutableVectorN.create(q.getA(), q.getB(), q.getC(), q.getD());

        final double e = evaluate(term, dedx, x0, x, dt);

        assertInvariants(term);

        assertThat("energy error", e, closeTo(eExpected, eTolerance));
        assertThat("dedex[0]", dedx[0], closeTo(deda2 * versor.getA(), dedxTolerance));
        assertThat("dedex[1]", dedx[1], closeTo(deda2 * versor.getB(), dedxTolerance));
        assertThat("dedex[2]", dedx[2], closeTo(deda2 * versor.getC(), dedxTolerance));
        assertThat("dedex[3]", dedx[3], closeTo(deda2 * versor.getD(), dedxTolerance));
    }

    private static void evaluate_versor(double length, double mass, Quaternion versor, double dt) {
        final QuaternionStateSpaceMapper quaternionMapper = new QuaternionStateSpaceMapper(0);
        final VersorError term = new VersorError(length, mass, quaternionMapper);
        final double[] dedx = new double[4];
        final ImmutableVectorN x0 = ImmutableVectorN.create0(4);
        final ImmutableVectorN x = ImmutableVectorN.create(versor.getA(), versor.getB(), versor.getC(), versor.getD());

        final double e = evaluate(term, dedx, x0, x, dt);

        assertInvariants(term);

        assertThat("energy error", e, closeTo(0, Double.MIN_NORMAL));
        assertThat("dedex[0]", dedx[0], closeTo(0, Double.MIN_NORMAL));
        assertThat("dedex[1]", dedx[1], closeTo(0, Double.MIN_NORMAL));
        assertThat("dedex[2]", dedx[2], closeTo(0, Double.MIN_NORMAL));
        assertThat("dedex[3]", dedx[3], closeTo(0, Double.MIN_NORMAL));
    }

    private static final double tolerance(double expected) {
        final double a = Math.max(1.0, Math.abs(expected));
        return Math.nextAfter(a, Double.POSITIVE_INFINITY) - a;
    }

    @Test
    public void constructor_A() {
        constructor(LENGTH_1, MASS_1, new QuaternionStateSpaceMapper(0));
    }

    @Test
    public void constructor_B() {
        constructor(LENGTH_2, MASS_2, new QuaternionStateSpaceMapper(1));
    }

    @Test
    public void evaluate_smallError_11A() {
        evaluate_smallError(LENGTH_1, MASS_1, Quaternion.ONE, Quaternion.ONE.scale(SMALL), DT_1);
    }

    @Test
    public void evaluate_smallError_11B() {
        evaluate_smallError(LENGTH_2, MASS_1, Quaternion.ONE, Quaternion.ONE.scale(SMALL), DT_1);
    }

    @Test
    public void evaluate_smallError_11C() {
        evaluate_smallError(LENGTH_1, MASS_2, Quaternion.ONE, Quaternion.ONE.scale(SMALL), DT_1);
    }

    @Test
    public void evaluate_smallError_11D() {
        evaluate_smallError(LENGTH_1, MASS_1, Quaternion.ONE, Quaternion.ONE.scale(1E-6), DT_1);
    }

    @Test
    public void evaluate_smallError_11E() {
        evaluate_smallError(LENGTH_1, MASS_1, Quaternion.ONE, Quaternion.ONE.scale(SMALL), DT_2);
    }

    @Test
    public void evaluate_smallError_11Smaller() {
        evaluate_smallError(LENGTH_1, MASS_1, Quaternion.ONE, Quaternion.ONE.scale(-SMALL), DT_1);
    }

    @Test
    public void evaluate_smallError_1i() {
        evaluate_smallError(LENGTH_1, MASS_1, Quaternion.ONE, Quaternion.I.scale(SMALL), DT_1);
    }

    @Test
    public void evaluate_smallError_1j() {
        evaluate_smallError(LENGTH_1, MASS_1, Quaternion.ONE, Quaternion.J.scale(SMALL), DT_1);
    }

    @Test
    public void evaluate_smallError_1k() {
        evaluate_smallError(LENGTH_1, MASS_1, Quaternion.ONE, Quaternion.K.scale(SMALL), DT_1);
    }

    @Test
    public void evaluate_smallError_ii() {
        evaluate_smallError(LENGTH_1, MASS_1, Quaternion.I, Quaternion.I.scale(SMALL), DT_1);
    }

    @Test
    public void evaluate_smallError_jj() {
        evaluate_smallError(LENGTH_1, MASS_1, Quaternion.J, Quaternion.J.scale(SMALL), DT_1);
    }

    @Test
    public void evaluate_smallError_kk() {
        evaluate_smallError(LENGTH_1, MASS_1, Quaternion.K, Quaternion.K.scale(SMALL), DT_1);
    }

    @Test
    public void evaluate_versor_1a() {
        evaluate_versor(LENGTH_1, MASS_1, Quaternion.ONE, DT_1);
    }

    @Test
    public void evaluate_versor_1b() {
        evaluate_versor(LENGTH_2, MASS_2, Quaternion.ONE, DT_2);
    }

    @Test
    public void evaluate_versor_i() {
        evaluate_versor(LENGTH_1, MASS_1, Quaternion.I, DT_1);
    }

    @Test
    public void evaluate_versor_j() {
        evaluate_versor(LENGTH_1, MASS_1, Quaternion.J, DT_1);
    }

    @Test
    public void evaluate_versor_k() {
        evaluate_versor(LENGTH_1, MASS_1, Quaternion.K, DT_1);
    }
}
