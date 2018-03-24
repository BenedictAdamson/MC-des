package uk.badamson.mc.physics;

import java.util.Arrays;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.physics.dynamics.Newton2Error;

/**
 * <p>
 * A {@linkplain TimeStepEnergyErrorFunctionTerm term} for a
 * {@linkplain TimeStepEnergyErrorFunction functor that calculates the physical
 * modelling error of a system at a future point in time} that gives the degree
 * to which a body does not conserve mass.
 * </p>
 * <p>
 * This gives an error in terms of the {@linkplain #getVelocityTerm(int)
 * velocity} of the body, rather than its acceleration, unlike the
 * {@link Newton2Error} class. The error calculation includes contributions from
 * the old state. It therefore tends to prevent accumulation of momentum errors:
 * an error for the old state will tend to be balanced by an equal and opposite
 * error at the new state. However, this can cause instability in the solution,
 * with the absolute size of the error remaining large but alternating in sign
 * (or increasing in magnitude). Also, this term works poorly for low mass
 * bodies and small time-steps. This term should therefore not be used alone to
 * ensure momentum conservation; it should be used with the {@link Newton2Error}
 * term.
 * </p>
 * 
 * @see Newton2Error
 */
@Immutable
public final class MomentumConservationError extends AbstractTimeStepEnergyErrorFunctionTerm {

    private static boolean isValidForTerm(int n, int term[]) {
        for (int i = 0, tn = term.length; i < tn; ++i) {
            if (n < term[i] + 1) {
                return false;
            }
        }
        return true;
    }

    private final int massTerm;
    private final int[] velocityTerm;
    private final int[] advectionVelocityTerm;
    private final int[] advectionMassRateTerm;
    private final int[] forceTerm;

    private final boolean[] massTransferInto;
    private final boolean[] forceOn;

    /**
     * <p>
     * Construct a Newton2Error.
     * </p>
     *
     * <section>
     * <h1>Post Conditions</h1>
     * <ul>
     * <li>The constructed object has the given attribute values.</li>
     * </ul>
     * </section>
     * 
     * @param massTerm
     *            Which term in the solution space vector correspond to the mass of
     *            the body.
     * @param velocityTerm
     *            Which terms in the solution space vector correspond to the
     *            components of the velocity vector of the body.
     *            {@code velocityTerm[i]} is the index of component <var>i</var>.
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
     * @param advectionVelocityTerm
     *            Which terms in the solution space vector correspond to the
     *            components of the velocity vector of an advection (mass transfer
     *            process) affecting the body. {@code advectionVelocityTerm[j*n+i]}
     *            is the term of component <var>i</var> of the velocity for
     *            advection <var>j</var>.
     * @param forceOn
     *            Whether one of the forces on this body has the <i>sense</i> that a
     *            positive force component corresponds to a force that increases
     *            that momentum component of this body. {@code forceOn[k]} indicates
     *            that force <var>k</var> has a positive sense.
     * @param forceTerm
     *            Which terms in the solution space vector correspond to the
     *            components of the force vector of a force affecting the body.
     *            {@code forceTerm[k*n+i]} is the term of component <var>i</var> of
     *            force <var>k</var>.
     *
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code velocityTerm} is null.
     *             <li>
     *             <li>If {@code massTransferInto} is null.
     *             <li>
     *             <li>If {@code advectionMassRateTerm} is null.
     *             <li>
     *             <li>If {@code advectionVelocityTerm} is null.
     *             <li>
     *             <li>If {@code forceOn} is null.
     *             <li>
     *             <li>If {@code forceTerm} is null.
     *             <li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code massTransferInto} and {@code advectionMassRateTerm}
     *             have different lengths.</li>
     *             <li>If the length of {@code advectionMassRateTerm} is not equal
     *             to the produce of the lengths of {@code massTransferInto} and
     *             {@code velocityTerm}.</li>
     *             <li>If the length of {@code forceTerm} is not equal to the
     *             produce of the lengths of {@code forceOn} and
     *             {@code velocityTerm}.</li>
     *             </ul>
     */
    public MomentumConservationError(int massTerm, int[] velocityTerm, boolean[] massTransferInto,
            int[] advectionMassRateTerm, int[] advectionVelocityTerm, boolean[] forceOn, int[] forceTerm) {
        this.massTerm = requireTermIndex(massTerm, "massTerm");
        this.velocityTerm = copyTermIndex(velocityTerm, "velocityTerm");
        this.massTransferInto = Arrays.copyOf(massTransferInto, massTransferInto.length);
        this.advectionMassRateTerm = copyTermIndex(advectionMassRateTerm, "advectionMassRateTerm");
        this.advectionVelocityTerm = copyTermIndex(advectionVelocityTerm, "advectionVelocityTerm");
        this.forceOn = Arrays.copyOf(forceOn, forceOn.length);
        this.forceTerm = copyTermIndex(forceTerm, "forceTerm");

        final int nSpace = velocityTerm.length;
        final int nAdvection = massTransferInto.length;
        final int nForce = forceOn.length;
        if (nAdvection != advectionMassRateTerm.length) {
            throw new IllegalArgumentException("Inconsistent massTransferInto.length " + nAdvection
                    + " advectionMassRateTerm.length " + advectionMassRateTerm.length);
        }
        if (nSpace * nAdvection != advectionVelocityTerm.length) {
            throw new IllegalArgumentException(
                    "Inconsistent velocityTerm.length " + nSpace + ", massTransferInto.length " + nAdvection
                            + ", advectionVelocityTerm.length" + advectionVelocityTerm.length);
        }
        if (nSpace * nForce != forceTerm.length) {
            throw new IllegalArgumentException("Inconsistent velocityTerm.length " + nSpace + ", forceOn.length "
                    + nForce + ", forceTerm.length" + forceTerm.length);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <ol>
     * <li>The method uses the term index information to extract force and velocity
     * vectors and mass transfer rates from the given current state vector.</li>
     * <li>It uses those values to calculate an extrapolated momentum for the new
     * point in time.</li>
     * <li>It compares that extrapolated momentum with the momentum of the given
     * state vector to calculate a momentum conservation error.</li>
     * <li>From that it calculates an equivalent kinetic energy error, using mass of
     * the body. That is the error term it returns.</li>
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
     *             {@linkplain ImmutableVectorN#getDimension() dimension} of
     *             {@code state0}.
     */
    @Override
    public final double evaluate(double[] dedx, ImmutableVectorN state0, ImmutableVectorN state, double dt) {
        super.evaluate(dedx, state0, state, dt);// check preconditions

        final int ns = getSpaceDimension();
        final int nm = getNumberOfMassTransfers();
        final int nf = getNumberOfForces();
        final ImmutableVectorN zero = ImmutableVectorN.create0(ns);

        final double m0 = state0.get(massTerm);
        final ImmutableVectorN v0 = extract(state0, velocityTerm);

        final double m = state.get(massTerm);
        final ImmutableVectorN v = extract(state, velocityTerm);

        double massRateTotal = 0.0;
        final double[] massRate = new double[nm];
        final ImmutableVectorN[] vrel = new ImmutableVectorN[nm];
        final ImmutableVectorN[] pRateAdvection = new ImmutableVectorN[nm];
        for (int j = 0; j < nm; ++j) {
            final double sign = massTransferInto[j] ? 1.0 : -1.0;
            final double massRate0J = sign * state0.get(advectionMassRateTerm[j]);
            final double massRateJ = sign * state.get(advectionMassRateTerm[j]);
            final ImmutableVectorN vrel0j = extract(state0, advectionVelocityTerm, j * ns, ns).minus(v0);
            final ImmutableVectorN vrelj = extract(state, advectionVelocityTerm, j * ns, ns).minus(v);
            massRate[j] = massRateJ;
            vrel[j] = vrelj;
            pRateAdvection[j] = vrel0j.scale(massRate0J).mean(vrelj.scale(massRateJ));
            massRateTotal += massRateJ;
        }

        final ImmutableVectorN pRateAdvectionTotal = 0 < nm ? ImmutableVectorN.sum(pRateAdvection) : zero;

        final ImmutableVectorN[] fMean = new ImmutableVectorN[nf];
        final double[] fs = new double[nf];
        for (int k = 0; k < nf; ++k) {
            double sign = 1.0;
            ImmutableVectorN f0k = extract(state0, forceTerm, k * ns, ns);
            ImmutableVectorN fk = extract(state, forceTerm, k * ns, ns);
            ImmutableVectorN mean = f0k.mean(fk);
            if (!forceOn[k]) {
                sign = -1.0;
                mean = mean.minus();
            }
            fs[k] = sign;
            fMean[k] = mean;
        }

        final ImmutableVectorN fTotal = 0 < nf ? ImmutableVectorN.sum(fMean) : zero;

        final ImmutableVectorN p0 = v0.scale(m0);
        final ImmutableVectorN p = v.scale(m);
        final ImmutableVectorN pRate = pRateAdvectionTotal.plus(fTotal);

        final ImmutableVectorN pe = (p.minus(p0)).minus(pRate.scale(dt));
        final ImmutableVectorN ve = pe.scale(1.0 / m);
        final double e = 0.5 * pe.dot(ve);

        dedx[massTerm] += ve
                .dot(ImmutableVectorN.weightedSum(new double[] { 1.0, -0.5 }, new ImmutableVectorN[] { v, ve }));
        final double mdedv = m - 0.5 * dt * massRateTotal;
        for (int i = 0; i < ns; ++i) {
            dedx[getVelocityTerm(i)] += ve.get(i) * mdedv;
        }
        for (int j = 0; j < nm; ++j) {
            final double sign = massTransferInto[j] ? 1.0 : -1.0;
            final double mdedu = -0.25 * massRate[j];
            dedx[advectionMassRateTerm[j]] += -0.5 * dt * sign * ve.dot(vrel[j]);
            for (int i = 0; i < ns; ++i) {
                dedx[getAdvectionVelocityTerm(j, i)] += mdedu * ve.get(i);
            }
        }

        for (int k = 0; k < nf; ++k) {
            final double fsk = fs[k];
            for (int i = 0; i < ns; ++i) {
                dedx[getForceTerm(k, i)] += -0.5 * dt * fsk * ve.get(i);
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
     * Which terms in the solution space vector correspond to the components of the
     * velocity vector of an advection (mass transfer process) affecting the body.
     * </p>
     * 
     * @param j
     *            The mass transfer process (advection) of interest
     * @param i
     *            The component of interest.
     * @return the index of the component of the advection velocity vector; not
     *         negative
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code j} is negative.</li>
     *             <li>If {@code j} is not less than the
     *             {@linkplain #getNumberOfMassTransfers() number of mass transfer
     *             processes}.</li>
     *             <li>If {@code i} is negative.</li>
     *             <li>If {@code i} is not less than the
     *             {@linkplain #getSpaceDimension() space dimension}.</li>
     *             </ul>
     */
    public final int getAdvectionVelocityTerm(int j, int i) {
        return advectionVelocityTerm[requireAdvectionProcess(j) * getSpaceDimension() + requireVectorComponent(i)];
    }

    /**
     * <p>
     * Which terms in the solution space vector correspond to the components of the
     * force vector of a force affecting the body.
     * </p>
     * 
     * @param k
     *            The force of interest
     * @param i
     *            The component of interest.
     * @return the index of the component of the force vector; not negative
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code k} is negative.</li>
     *             <li>If {@code k} is not less than the
     *             {@linkplain #getNumberOfForces() number of forces on the
     *             body}.</li>
     *             <li>If {@code i} is negative.</li>
     *             <li>If {@code i} is not less than the
     *             {@linkplain #getSpaceDimension() space dimension}.</li>
     *             </ul>
     */
    public final int getForceTerm(int k, int i) {
        return forceTerm[requireForce(k) * getSpaceDimension() + requireVectorComponent(i)];
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
     * The number of forces acting on the body.
     * </p>
     * 
     * @return the number of forces; not negative.
     */
    public final int getNumberOfForces() {
        return forceOn.length;
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
     * The number of space dimensions for which this calculates a velocity error.
     * </p>
     * 
     * @return the number of dimensions; positive.
     */
    public final int getSpaceDimension() {
        return velocityTerm.length;
    }

    /**
     * <p>
     * Which terms in the solution space vector correspond to the components of the
     * velocity vector of the body.
     * </p>
     * 
     * @param i
     *            The component of interest.
     * @return the index of the component of the velocity vector; not negative
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code i} is negative.</li>
     *             <li>If {@code i} is not less than the
     *             {@linkplain #getSpaceDimension() space dimension}.</li>
     *             </ul>
     */
    public final int getVelocityTerm(int i) {
        return velocityTerm[i];
    }

    /**
     * <p>
     * Whether one of the forces on this body has the <i>sense</i> that a positive
     * force component corresponds to a force that increases that momentum component
     * of this body.
     * </p>
     * <p>
     * This enables one state space variable to indicate a component of a force
     * acting on one body and the equal and opposite force acting on another, by
     * having opposite senses for the term used for those two bodies.
     * </p>
     * 
     * @param k
     *            The force of interest
     * @return the sense of the force
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code k} is negative.</li>
     *             <li>If {@code k} is not less than the
     *             {@linkplain #getNumberOfForces() number of forces on the
     *             body}.</li>
     *             </ul>
     */
    public final boolean isForceOn(int k) {
        return forceOn[k];
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
        return (massTerm + 1 <= n) && isValidForTerm(n, velocityTerm) && isValidForTerm(n, advectionVelocityTerm)
                && isValidForTerm(n, advectionMassRateTerm) && isValidForTerm(n, forceTerm);
    }

    private int requireAdvectionProcess(int j) {
        if (j < 0 || getNumberOfMassTransfers() <= j) {
            throw new IndexOutOfBoundsException("Not an advection component " + j);
        }
        return j;
    }

    private int requireForce(int k) {
        if (k < 0 || getNumberOfForces() <= k) {
            throw new IndexOutOfBoundsException("Not a force " + k);
        }
        return k;
    }

    private int requireVectorComponent(int i) {
        if (i < 0 || getSpaceDimension() <= i) {
            throw new IndexOutOfBoundsException("Not a space vector component " + i);
        }
        return i;
    }

}
