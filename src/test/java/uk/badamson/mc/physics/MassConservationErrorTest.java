package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * Unit tests for the class {@link MassConservationError}.
 * </p>
 */
public class MassConservationErrorTest {

    private static final double MASS_REFERENCE_1 = 1.0;
    private static final double MASS_REFERENCE_2 = 2.0;
    private static final double SPECIFIC_ENERGY_REFERNCE_1 = 1.0;
    private static final double SPECIFIC_ENERGY_REFERNCE_2 = 1.0E3;

    public static void assertInvariants(MassConservationError term) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited

        final int numberOfMassTransfers = term.getNumberOfMassTransfers();
        final double timeReference = term.getSpecificEnergyReference();

        assertTrue("numberOfMassTransfers not negative", 0 <= numberOfMassTransfers);

        assertTrue("timeReference is positive and finite", 0.0 < timeReference && Double.isFinite(timeReference));

        assertTrue("massTerm is not negative", 0 <= term.getMassTerm());
        for (int j = 0; j < numberOfMassTransfers; ++j) {
            assertTrue("advectionMassRateTerm is not negative", 0 <= term.getAdvectionMassRateTerm(j));
        }
    }

    public static void assertInvariants(MassConservationError term1, MassConservationError term2) {
        AbstractTimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
    }

    private static MassConservationError constructor(double massReference, double specificEnergyReference, int massTerm,
            boolean[] massTransferInto, int[] advectionMassRateTerm) {
        final MassConservationError term = new MassConservationError(massReference, specificEnergyReference, massTerm,
                massTransferInto, advectionMassRateTerm);

        assertInvariants(term);

        assertEquals("massReference", massReference, term.getMassReference(), Double.MIN_NORMAL);
        assertEquals("specificEnergyReference", specificEnergyReference, term.getSpecificEnergyReference(),
                Double.MIN_NORMAL);

        assertEquals("numberOfMassTransfers", massTransferInto.length, term.getNumberOfMassTransfers());

        assertEquals("massTerm", massTerm, term.getMassTerm());
        for (int j = 0; j < massTransferInto.length; ++j) {
            assertEquals("massTransferInto[" + j + "]", massTransferInto[j], term.isMassTransferInto(j));
            assertEquals("advectionMassRateTerm[" + j + "]", advectionMassRateTerm[j],
                    term.getAdvectionMassRateTerm(j));
        }

        return term;
    }

    private static double evaluate(MassConservationError term, double[] dedx, ImmutableVectorN state0,
            ImmutableVectorN state, double dt) {
        final double e = AbstractTimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, state0, state, dt);// inherited

        assertInvariants(term);
        assertTrue("value is not negative", 0.0 <= e);

        return e;
    }

    private static void evaluate_closed(double massReference, double specificEnergyReference, double m0, double m,
            double dedm0, double dt, double eExpected, double dedmExpected) {
        final int massTerm = 0;
        final boolean[] massTransferInto = {};
        final int[] advectionMassRateTerm = {};
        final MassConservationError term = new MassConservationError(massReference, specificEnergyReference, massTerm,
                massTransferInto, advectionMassRateTerm);

        final ImmutableVectorN state0 = ImmutableVectorN.create(m0);
        final ImmutableVectorN state = ImmutableVectorN.create(m);
        final double[] dedx = { dedm0 };

        final double e = evaluate(term, dedx, state0, state, dt);

        assertEquals("e", eExpected, e, 1E-6);
        assertEquals("dedm", dedmExpected, dedx[0], 1E-6);
    }

    private static void evaluate_open(double massReference, double specificEnergyReference, boolean massTransferInto,
            double m0, double mrate0, double m, double mrate, double dedmrate0, double dt, double eExpected,
            double dedmExpected, double dedmrateExpected) {
        final int massTerm = 0;
        final int[] advectionMassRateTerm = { 1 };
        final MassConservationError term = new MassConservationError(massReference, specificEnergyReference, massTerm,
                new boolean[] { massTransferInto }, advectionMassRateTerm);

        final ImmutableVectorN state0 = ImmutableVectorN.create(m0, mrate0);
        final ImmutableVectorN state = ImmutableVectorN.create(m, mrate);
        final double[] dedx = { 0.0, dedmrate0 };

        final double e = evaluate(term, dedx, state0, state, dt);

        assertEquals("e", eExpected, e, 1E-6);
        assertEquals("dedm", dedmExpected, dedx[0], 1E-6);
        assertEquals("dedmrate0", dedmrateExpected, dedx[1], 1E-6);
    }

    @Test
    public void constructor_0A() {
        final int massTerm = 0;
        final boolean[] massTransferInto = {};
        final int[] advectionMassRateTerm = {};

        constructor(MASS_REFERENCE_1, SPECIFIC_ENERGY_REFERNCE_1, massTerm, massTransferInto, advectionMassRateTerm);
    }

    @Test
    public void constructor_0B() {
        final int massTerm = 13;
        final boolean[] massTransferInto = {};
        final int[] advectionMassRateTerm = {};

        constructor(MASS_REFERENCE_2, SPECIFIC_ENERGY_REFERNCE_2, massTerm, massTransferInto, advectionMassRateTerm);
    }

    @Test
    public void constructor_1A() {
        final int massTerm = 0;
        final boolean[] massTransferInto = { true };
        final int[] advectionMassRateTerm = { 1 };

        constructor(MASS_REFERENCE_1, SPECIFIC_ENERGY_REFERNCE_1, massTerm, massTransferInto, advectionMassRateTerm);
    }

    @Test
    public void constructor_1B() {
        final int massTerm = 13;
        final boolean[] massTransferInto = { false };
        final int[] advectionMassRateTerm = { 27 };

        constructor(MASS_REFERENCE_1, SPECIFIC_ENERGY_REFERNCE_1, massTerm, massTransferInto, advectionMassRateTerm);
    }

    @Test
    public void evaluate_closedBase() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final double m0 = 1.0;
        final double m = 2.0;

        final double dedm0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 1.0;
        final double dedmExpected = 2.0;

        evaluate_closed(massReference, specificEnergyReference, m0, m, dedm0, dt, eExpected, dedmExpected);
    }

    @Test
    public void evaluate_closedDedm0() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final double m0 = 1.0;
        final double m = 2.0;

        final double dedm0 = 1.0;
        final double dt = 1.0;

        final double eExpected = 1.0;
        final double dedmExpected = 3.0;

        evaluate_closed(massReference, specificEnergyReference, m0, m, dedm0, dt, eExpected, dedmExpected);
    }

    @Test
    public void evaluate_closedDm() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final double m0 = 2.0;
        final double m = 1.0;

        final double dedm0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 1.0;
        final double dedmExpected = -2.0;

        evaluate_closed(massReference, specificEnergyReference, m0, m, dedm0, dt, eExpected, dedmExpected);
    }

    @Test
    public void evaluate_closedDt() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final double m0 = 1.0;
        final double m = 2.0;

        final double dedm0 = 0.0;
        final double dt = 2.0;

        final double eExpected = 1.0;
        final double dedmExpected = 2.0;

        evaluate_closed(massReference, specificEnergyReference, m0, m, dedm0, dt, eExpected, dedmExpected);
    }

    @Test
    public void evaluate_closedM() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final double m0 = 1.0;
        final double m = 3.0;

        final double dedm0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 4.0;
        final double dedmExpected = 4.0;

        evaluate_closed(massReference, specificEnergyReference, m0, m, dedm0, dt, eExpected, dedmExpected);
    }

    @Test
    public void evaluate_closedM0() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final double m0 = 2.0;
        final double m = 2.0;

        final double dedm0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 0.0;
        final double dedmExpected = 0.0;

        evaluate_closed(massReference, specificEnergyReference, m0, m, dedm0, dt, eExpected, dedmExpected);
    }

    @Test
    public void evaluate_closedMassReference() {
        final double massReference = 2.0;
        final double specificEnergyReference = 1.0;
        final double m0 = 1.0;
        final double m = 2.0;

        final double dedm0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 0.5;
        final double dedmExpected = 1.0;

        evaluate_closed(massReference, specificEnergyReference, m0, m, dedm0, dt, eExpected, dedmExpected);
    }

    @Test
    public void evaluate_closedSpecificEnergyReference() {
        final double massReference = 1.0;
        final double specificEnergyReference = 2.0;
        final double m0 = 1.0;
        final double m = 2.0;

        final double dedm0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 2.0;
        final double dedmExpected = 4.0;

        evaluate_closed(massReference, specificEnergyReference, m0, m, dedm0, dt, eExpected, dedmExpected);
    }

    @Test
    public void evaluate_openBase() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final boolean massTransferInto = true;
        final double m0 = 1.0;
        final double mrate0 = 1.0;
        final double m = 1.0;
        final double mrate = 1.0;

        final double dedmrate0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 1.0;
        final double dedmExpected = 2.0;
        final double dedmrateExpected = 1.0;

        evaluate_open(massReference, specificEnergyReference, massTransferInto, m0, mrate0, m, mrate, dedmrate0, dt,
                eExpected, dedmExpected, dedmrateExpected);
    }

    @Test
    public void evaluate_openDedmrate0() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final boolean massTransferInto = true;
        final double m0 = 1.0;
        final double mrate0 = 1.0;
        final double m = 1.0;
        final double mrate = 1.0;

        final double dedmrate0 = 1.0;
        final double dt = 1.0;

        final double eExpected = 1.0;
        final double dedmExpected = 2.0;
        final double dedmrateExpected = 2.0;

        evaluate_open(massReference, specificEnergyReference, massTransferInto, m0, mrate0, m, mrate, dedmrate0, dt,
                eExpected, dedmExpected, dedmrateExpected);
    }

    @Test
    public void evaluate_openDt() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final boolean massTransferInto = true;
        final double m0 = 1.0;
        final double mrate0 = 1.0;
        final double m = 1.0;
        final double mrate = 1.0;

        final double dedmrate0 = 0.0;
        final double dt = 2.0;

        final double eExpected = 4.0;
        final double dedmExpected = 4.0;
        final double dedmrateExpected = 4.0;

        evaluate_open(massReference, specificEnergyReference, massTransferInto, m0, mrate0, m, mrate, dedmrate0, dt,
                eExpected, dedmExpected, dedmrateExpected);
    }

    @Test
    public void evaluate_openM() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final boolean massTransferInto = true;
        final double m0 = 1.0;
        final double mrate0 = 1.0;
        final double m = 2.0;
        final double mrate = 1.0;

        final double dedmrate0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 4.0;
        final double dedmExpected = 4.0;
        final double dedmrateExpected = 2.0;

        evaluate_open(massReference, specificEnergyReference, massTransferInto, m0, mrate0, m, mrate, dedmrate0, dt,
                eExpected, dedmExpected, dedmrateExpected);
    }

    @Test
    public void evaluate_openM0() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final boolean massTransferInto = true;
        final double m0 = 2.0;
        final double mrate0 = 1.0;
        final double m = 1.0;
        final double mrate = 1.0;

        final double dedmrate0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 0.0;
        final double dedmExpected = 0.0;
        final double dedmrateExpected = 0.0;

        evaluate_open(massReference, specificEnergyReference, massTransferInto, m0, mrate0, m, mrate, dedmrate0, dt,
                eExpected, dedmExpected, dedmrateExpected);
    }

    @Test
    public void evaluate_openMassReference() {
        final double massReference = 2.0;
        final double specificEnergyReference = 1.0;
        final boolean massTransferInto = true;
        final double m0 = 1.0;
        final double mrate0 = 1.0;
        final double m = 1.0;
        final double mrate = 1.0;

        final double dedmrate0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 0.5;
        final double dedmExpected = 1.0;
        final double dedmrateExpected = 0.5;

        evaluate_open(massReference, specificEnergyReference, massTransferInto, m0, mrate0, m, mrate, dedmrate0, dt,
                eExpected, dedmExpected, dedmrateExpected);
    }

    @Test
    public void evaluate_openMassTransferInto() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final boolean massTransferInto = false;
        final double m0 = 1.0;
        final double mrate0 = 1.0;
        final double m = 1.0;
        final double mrate = 1.0;

        final double dedmrate0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 1.0;
        final double dedmExpected = -2.0;
        final double dedmrateExpected = 1.0;

        evaluate_open(massReference, specificEnergyReference, massTransferInto, m0, mrate0, m, mrate, dedmrate0, dt,
                eExpected, dedmExpected, dedmrateExpected);
    }

    @Test
    public void evaluate_openMrate() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final boolean massTransferInto = true;
        final double m0 = 1.0;
        final double mrate0 = 1.0;
        final double m = 1.0;
        final double mrate = 3.0;

        final double dedmrate0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 4.0;
        final double dedmExpected = 4.0;
        final double dedmrateExpected = 2.0;

        evaluate_open(massReference, specificEnergyReference, massTransferInto, m0, mrate0, m, mrate, dedmrate0, dt,
                eExpected, dedmExpected, dedmrateExpected);
    }

    @Test
    public void evaluate_openMrate0() {
        final double massReference = 1.0;
        final double specificEnergyReference = 1.0;
        final boolean massTransferInto = true;
        final double m0 = 1.0;
        final double mrate0 = 2.0;
        final double m = 1.0;
        final double mrate = 1.0;

        final double dedmrate0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 2.25;
        final double dedmExpected = 3.0;
        final double dedmrateExpected = 1.5;

        evaluate_open(massReference, specificEnergyReference, massTransferInto, m0, mrate0, m, mrate, dedmrate0, dt,
                eExpected, dedmExpected, dedmrateExpected);
    }

    @Test
    public void evaluate_openSpecificEnergyReference() {
        final double massReference = 1.0;
        final double specificEnergyReference = 2.0;
        final boolean massTransferInto = true;
        final double m0 = 1.0;
        final double mrate0 = 1.0;
        final double m = 1.0;
        final double mrate = 1.0;

        final double dedmrate0 = 0.0;
        final double dt = 1.0;

        final double eExpected = 2.0;
        final double dedmExpected = 4.0;
        final double dedmrateExpected = 2.0;

        evaluate_open(massReference, specificEnergyReference, massTransferInto, m0, mrate0, m, mrate, dedmrate0, dt,
                eExpected, dedmExpected, dedmrateExpected);
    }

}
