package uk.badamson.mc.physics.dynamics;

import java.util.Arrays;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.ImmutableVector;
import uk.badamson.mc.physics.AbstractTimeStepEnergyErrorFunctionTerm;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunction;
import uk.badamson.mc.physics.TimeStepEnergyErrorFunctionTerm;

/**
 * <p>
 * A {@linkplain TimeStepEnergyErrorFunctionTerm term} for a
 * {@linkplain TimeStepEnergyErrorFunction functor that calculates the physical
 * modelling error of a system at a future point in time} that gives the degree
 * to which a body does not conform to Newton's second law of motion.
 * </p>
 */
@Immutable
public final class Newton2Error extends AbstractTimeStepEnergyErrorFunctionTerm {

    private static boolean isValidForTerm(int n, int term[]) {
        for (int i = 0, tn = term.length; i < tn; ++i) {
            if (n < term[i] + 1) {
                return false;
            }
        }
        return true;
    }

    private final double massReference;
    private final double timeReference;

    private final int massTerm;
    private final int[] accelerationTerm;
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
     * @param massReference
     *            A reference mass scale.
     * @param timeReference
     *            A reference time scale.
     * @param massTerm
     *            Which term in the solution space vector correspond to the mass of
     *            the body.
     * @param velocityTerm
     *            Which terms in the solution space vector correspond to the
     *            components of the velocity vector of the body.
     *            {@code velocityTerm[i]} is the index of component <var>i</var>.
     * @param accelerationTerm
     *            Which terms in the solution space vector correspond to the
     *            components of the acceleration vector of the body.
     *            {@code accelerationTerm[i]} is the index of component
     *            <var>i</var>.
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
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code velocityTerm} is null.
     *             <li>
     *             <li>If {@code accelerationTerm} is null.
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
     *             <li>If {@code massReference} is not positive and
     *             {@code Double#isInfinite() finite}.</li>
     *             <li>If {@code timeReference} is not positive and finite.</li>
     *             <li>If {@code velocityTerm} and {@code accelerationTerm} have
     *             different lengths.</li>
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
    public Newton2Error(double massReference, double timeReference, int massTerm, int[] velocityTerm,
            int[] accelerationTerm, boolean[] massTransferInto, int[] advectionMassRateTerm,
            int[] advectionVelocityTerm, boolean[] forceOn, int[] forceTerm) {
        this.massReference = requireReferenceScale(massReference, "massReference");
        this.timeReference = requireReferenceScale(timeReference, "timeReference");
        this.massTerm = requireTermIndex(massTerm, "massTerm");
        this.velocityTerm = copyTermIndex(velocityTerm, "velocityTerm");
        this.accelerationTerm = copyTermIndex(accelerationTerm, "accelerationTerm");
        this.massTransferInto = Arrays.copyOf(massTransferInto, massTransferInto.length);
        this.advectionMassRateTerm = copyTermIndex(advectionMassRateTerm, "advectionMassRateTerm");
        this.advectionVelocityTerm = copyTermIndex(advectionVelocityTerm, "advectionVelocityTerm");
        this.forceOn = Arrays.copyOf(forceOn, forceOn.length);
        this.forceTerm = copyTermIndex(forceTerm, "forceTerm");

        final int nSpace = velocityTerm.length;
        final int nAdvection = massTransferInto.length;
        final int nForce = forceOn.length;
        requireConsistentLengths(velocityTerm, "velocityTerm", accelerationTerm, "accelerationTerm");
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
     * <li>The method uses the term index information to extract force, velocity and
     * acceleration vectors and mass transfer rates from the given current state
     * vector.</li>
     * <li>It uses those values to calculate an error in obeying Newton's second law
     * of motion, in terms of a force error.</li>
     * <li>From that it calculates an equivalent kinetic energy error, using the
     * {@linkplain #getMassReference() characteristic mass value} and
     * {@linkplain #getMassReference() characteristic time value}. That is the error
     * term it returns.</li>
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

        final int ns = getSpaceDimension();
        final int nm = getNumberOfMassTransfers();
        final int nf = getNumberOfForces();
        final double mRef2 = massReference * massReference;

        final double m = state.get(massTerm);

        final ImmutableVector a = extract(state, accelerationTerm);
        final ImmutableVector v = extract(state, velocityTerm);

        double massRateTotal = 0.0;
        final double[] massRate = new double[nm];
        final ImmutableVector[] vrel = new ImmutableVector[nm];
        for (int j = 0; j < nm; ++j) {
            final double sign = massTransferInto[j] ? 1.0 : -1.0;
            final double massRateJ = sign * state.get(advectionMassRateTerm[j]);
            massRate[j] = massRateJ;
            massRateTotal += massRateJ;
            vrel[j] = extract(state, advectionVelocityTerm, j * ns, ns).minus(v);
        }

        final ImmutableVector[] f = new ImmutableVector[nf];
        final double[] fs = new double[nf];
        for (int k = 0; k < nf; ++k) {
            f[k] = extract(state, forceTerm, k * ns, ns);
            fs[k] = forceOn[k] ? 1.0 : -1.0;
        }

        final ImmutableVector advectionTotal = 0 < nm ? ImmutableVector.weightedSum(massRate, vrel)
                : ImmutableVector.create0(ns);
        final ImmutableVector fTotal = 0 < nf ? ImmutableVector.weightedSum(fs, f) : ImmutableVector.create0(ns);

        final ImmutableVector fe = a.scale(m).minus(advectionTotal).minus(fTotal);
        final ImmutableVector ve = fe.scale(getTimeReference() / mRef2);
        final ImmutableVector xe = ve.scale(getTimeReference());
        final double e = 0.5 * massReference * ve.magnitude2();

        dedx[massTerm] += xe.dot(a);
        for (int i = 0; i < ns; ++i) {
            final double xi = xe.get(i);
            dedx[getVelocityTerm(i)] += massRateTotal * xi;
            dedx[getAccelerationTerm(i)] += m * xi;
        }

        for (int j = 0; j < nm; ++j) {
            final double dedmrate = xe.dot(vrel[j]);
            final ImmutableVector dedu = xe.scale(-massRate[j]);
            if (massTransferInto[j]) {
                dedx[advectionMassRateTerm[j]] -= dedmrate;
            } else {
                dedx[advectionMassRateTerm[j]] += dedmrate;
            }
            for (int i = 0; i < ns; ++i) {
                dedx[getAdvectionVelocityTerm(j, i)] += dedu.get(i);
            }
        }

        for (int k = 0; k < nf; ++k) {
            final double fsk = fs[k];
            for (int i = 0; i < ns; ++i) {
                dedx[getForceTerm(k, i)] -= fsk * xe.get(i);
            }
        }

        return e;
    }

    /**
     * <p>
     * Which terms in the solution space vector correspond to the components of the
     * acceleration vector of the body.
     * </p>
     * 
     * @param i
     *            The component of interest.
     * @return the index of the component of the acceleration vector; not negative
     * 
     * @throws IndexOutOfBoundsException
     *             <ul>
     *             <li>If {@code i} is negative.</li>
     *             <li>If {@code i} is not less than the
     *             {@linkplain #getSpaceDimension() space dimension}.</li>
     *             </ul>
     */
    public final int getAccelerationTerm(int i) {
        return accelerationTerm[i];
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
     * A reference time scale.
     * </p>
     * <p>
     * The functor uses this value to convert a force error into an energy error. It
     * is tempting to use the time step size, but that will produce bad results if
     * the time-step is small; it is better to use a value equal to the duration of
     * a typical mass transfer process or of an impulse.
     * </p>
     * 
     * @return the time; positive and {@linkplain Double#isFinite(double) finite}
     */
    public final double getTimeReference() {
        return timeReference;
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
        return (massTerm + 1 <= n) && isValidForTerm(n, accelerationTerm) && isValidForTerm(n, velocityTerm)
                && isValidForTerm(n, advectionVelocityTerm) && isValidForTerm(n, advectionMassRateTerm)
                && isValidForTerm(n, forceTerm);
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
