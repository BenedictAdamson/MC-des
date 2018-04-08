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

    private static final double LENGTH_1 = 1.0;
    private static final double LENGTH_2 = 1E7;

    private static final double MASS_1 = 2.0;
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

    @Test
    public void constructor_A() {
        constructor(LENGTH_1, MASS_1, new QuaternionStateSpaceMapper(0));
    }

    @Test
    public void constructor_B() {
        constructor(LENGTH_2, MASS_2, new QuaternionStateSpaceMapper(1));
    }

    @Test
    public void evaluate_versor_ia() {
        evaluate_versor(LENGTH_1, MASS_1, Quaternion.I, DT_1);
    }

    @Test
    public void evaluate_versor_ib() {
        evaluate_versor(LENGTH_2, MASS_2, Quaternion.I, DT_2);
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
