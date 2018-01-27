package uk.badamson.mc.physics;

import java.util.Arrays;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVector;

/**
 * <p>
 * A {@linkplain TimeStepEnergyErrorFunctionTerm term} for a
 * {@linkplain TimeStepEnergyErrorFunction functor that calculates the physical
 * modelling error of a system at a future point in time} that gives the degree
 * to which a body does not conserve mass.
 * </p>
 */
@Immutable
public final class MassConservationError extends AbstractTimeStepEnergyErrorFunctionTerm {

    private static boolean isValidForTerm(int n, int term[]) {
        for (int i = 0, tn = term.length; i < tn; ++i) {
            if (n < term[i] + 1) {
                return false;
            }
        }
        return true;
    }

    private final double massReference;
    private final double specificEnergyReference;

    private final int massTerm;
    private final int[] advectionMassRateTerm;

    private final boolean[] massTransferInto;

    /**
     * <p>
     * Construct a MassConservationError.
     * </p>
     *
     * <section>
     * <h1>Post Conditions</h1>
     * <ul>
     * <li>The constructed object has the given attribute values.</li>
     * </ul>
     * </section>
     * 
     * @param massReference
     *            A reference mass scale.
     * @param specificEnergyReference
     *            A reference energy per unit mass.
     * @param massTerm
     *            Which term in the solution space vector correspond to the mass of
     *            the body.
     * @param massTransferInto
     *            Whether one of the mass transfer processes has the <i>sense<i>
     *            that a positive mass transfer rate corresponds to mass transfer
     *            into the body. {@code massTransferInto[j]} indicates that
     *            advection <var>j</var> has positive sense.
     * @param advectionMassRateTerm
     *            Which term in the solution space vector correspond to the mass
     *            transfer rate of an advection (mass transfer process) affecting
     *            the body. {@code advectionMassRateTerm[j]} is the index of
     *            advection <var>j</var>.
     *
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code massTransferInto} is null.
     *             <li>
     *             <li>If {@code advectionMassRateTerm} is null.
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code massReference} is not positive and
     *             {@code Double#isInfinite() finite}.</li>
     *             <li>If {@code specificEnergyReference} is not positive and
     *             finite.</li>
     *             <li>If {@code massTransferInto} and {@code advectionMassRateTerm}
     *             have different lengths.</li>
     *             </ul>
     */
    public MassConservationError(double massReference, double specificEnergyReference, int massTerm,
            boolean[] massTransferInto, int[] advectionMassRateTerm) {
        this.massReference = requireReferenceScale(massReference, "massReference");
        this.specificEnergyReference = requireReferenceScale(specificEnergyReference, "specificEnergyReference");
        this.massTerm = requireTermIndex(massTerm, "massTerm");
        this.massTransferInto = Arrays.copyOf(massTransferInto, massTransferInto.length);
        this.advectionMassRateTerm = copyTermIndex(advectionMassRateTerm, "advectionMassRateTerm");

        final int nAdvection = massTransferInto.length;
        if (nAdvection != advectionMassRateTerm.length) {
            throw new IllegalArgumentException("Inconsistent massTransferInto.length " + nAdvection
                    + " advectionMassRateTerm.length " + advectionMassRateTerm.length);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <ol>
     * <li>The method uses the term index information to extract mass and mass
     * transfer rates from the given current state vector.</li>
     * <li>It calculates a mean mass transfer rate, from the old and new mass
     * transfer rates.</li>
     * <li>It uses those values to calculate a mass conservation error.</li>
     * <li>It multiplies the square of the mass conservation error by the
     * {@linkplain #getSpecificEnergyReference() specific energy reference} and
     * divides by the {@linkplain #getMassReference() mass reference} to calculate
     * an equivalent energy error.. That is the error term it returns.</li>
     * </ol>
     * 
     * @param dedx
     *            {@inheritDoc}
     * @param state0
     *            {@inheritDoc}
     * @param state
     *            {@inheritDoc}
     * @param dt
     *            {@inheritDoc}
     * @return the value; not negative
     * 
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     * @throws IllegalArgumentException
     *             If the length of {@code dedx} does not equal the
     *             {@linkplain ImmutableVector#getDimension() dimension} of
     *             {@code state0}.
     */
    @Override
    public final double evaluate(double[] dedx, ImmutableVector state0, ImmutableVector state, double dt) {
        super.evaluate(dedx, state0, state, dt);// check preconditions

        final int nm = getNumberOfMassTransfers();

        final double m0 = state0.get(massTerm);
        final double m = state.get(massTerm);

        double massRateMean = 0.0;
        for (int j = 0; j < nm; ++j) {
            final double sign = massTransferInto[j] ? 1.0 : -1.0;
            final double massRate0 = state0.get(advectionMassRateTerm[j]);
            final double massRate = state.get(advectionMassRateTerm[j]);
            massRateMean += 0.5 * sign * (massRate0 + massRate);
        }

        final double me = (m - m0) + dt * massRateMean;
        final double ce = specificEnergyReference * (me / massReference);
        // ce may be negative
        final double e = ce * me;

        dedx[massTerm] += 2.0 * ce;
        final double dedmrate = ce * dt;
        for (int j = 0; j < nm; ++j) {
            if (massTransferInto[j]) {
                dedx[advectionMassRateTerm[j]] += dedmrate;
            } else {
                dedx[advectionMassRateTerm[j]] -= dedmrate;
            }
        }

        return e;
    }

    /**
     * <p>
     * Which term in the solution space vector correspond to the mass transfer rate
     * of an advection (mass transfer process) affecting the body.
     * </p>
     * 
     * @param j
     *            The mass transfer process (advection) of interest
     * @return the index of the component of the advection mass transfer rate; not
     *         negative
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code j} is negative.</li>
     *             <li>If {@code j} is not less than the
     *             {@linkplain #getNumberOfMassTransfers() number of mass transfer
     *             processes}.</li>
     *             </ul>
     */
    public final int getAdvectionMassRateTerm(int j) {
        return advectionMassRateTerm[j];
    }

    /**
     * <p>
     * A reference mass scale.
     * </p>
     * <p>
     * The functor uses this value to convert a force error into an energy error. It
     * is tempting to use the mass of the solid body for which this functor
     * calculates the error, but that will produce bad results if there are multiple
     * bodies and they have very different masses; it is better to use the same
     * value for all bodies, with that value equal to the mass of a typical body.
     * </p>
     * 
     * @return the mass; positive and {@linkplain Double#isFinite(double) finite}
     */
    public final double getMassReference() {
        return massReference;
    }

    /**
     * <p>
     * Which term in the solution space vector correspond to the mass of the body.
     * </p>
     * 
     * @return the index of the mass; not negative
     */
    public final int getMassTerm() {
        return massTerm;
    }

    /**
     * <p>
     * The number of separate mass transfer processes (advections) changing the mass
     * and momentum of the body.
     * </p>
     * 
     * @return the number of mass transfer processes; not negative.
     */
    public final int getNumberOfMassTransfers() {
        return massTransferInto.length;
    }

    /**
     * <p>
     * A reference specific energy scale.
     * </p>
     * <p>
     * The functor uses this value to convert a mass error error into an energy
     * error. It is tempting to use the specific energy of the body, but that will
     * produce bad results if different bodies have very different specific
     * energies; it is better to use a value equal to the typical specific energy of
     * a body.
     * </p>
     * 
     * @return the time; positive and {@linkplain Double#isFinite(double) finite}
     */
    public final double getSpecificEnergyReference() {
        return specificEnergyReference;
    }

    /**
     * <p>
     * Whether one of the mass transfer processes has the <i>sense<i> that a
     * positive mass transfer rate corresponds to mass transfer into the body .
     * </p>
     * <p>
     * This enables one state space variable to indicate the mass transfer rate out
     * of one body and into another body, by having opposite senses for the term
     * used for those two bodies.
     * </p>
     *
     * @param j
     *            The mass transfer process (advection) of interest
     * @return the sense of the mass transfer process
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code j} is negative.</li>
     *             <li>If {@code j} is not less than the
     *             {@linkplain #getNumberOfMassTransfers() number of mass transfer
     *             processes}.</li>
     *             </ul>
     */
    public final boolean isMassTransferInto(int j) {
        return massTransferInto[j];
    }

    /**
     * <p>
     * Whether this term can be calculated for a physical state vector that has a
     * given number of variables.
     * </p>
     * 
     * @return whether valid.
     * @throws IllegalArgumentException
     *             If {@code n} is not positive.
     */
    @Override
    public final boolean isValidForDimension(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n " + n);
        }
        return (massTerm + 1 <= n) && isValidForTerm(n, advectionMassRateTerm);
    }

}
