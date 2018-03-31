package uk.badamson.mc.physics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.math.Function1WithGradientValue;
import uk.badamson.mc.math.FunctionNWithGradient;
import uk.badamson.mc.math.FunctionNWithGradientValue;
import uk.badamson.mc.math.ImmutableVectorN;
import uk.badamson.mc.math.MinN;

/**
 * <p>
 * A {@linkplain FunctionNWithGradient functor} that calculates the physical
 * modelling error of a system at a future point in time.</p
 * <p>
 * The function can be {@linkplain MinN minimised} to calculate the state of the
 * physical system at that future point in time. By solving for a
 * {@linkplain ImmutableVectorN multi-dimensional state vector}, that
 * simultaneously solves for all the physical parameters of the system. By using
 * minimisation to calculate the state, it is straightforward to include
 * non-linear coupling of parameters of the physical system.
 * </p>
 * <p>
 * The function also calculates the
 * {@linkplain FunctionNWithGradientValue#getDfDx() rate of change} of the error
 * with respect to changes in the state vector. That enables the minimisation to
 * be performed using a
 * {@linkplain MinN#findFletcherReevesPolakRibere(FunctionNWithGradient, ImmutableVectorN, double)
 * conjugate-gradient method}. The conjugate-gradient method is likely to be
 * efficient because the number of large eigen vectors of the state vector is
 * likely to be far fewer than the number of physical parameters: if the system
 * consists of loosely coupled objects, there is only one large eigen vector per
 * object; if the system has some strongly coupled objects there will be one
 * large eigen vector per vibration mode of the coupled objects.
 * </p>
 * <p>
 * The function calculates an error value that has dimensions of energy. It is
 * therefore straightforward for the function to ensure conservation of energy.
 * Other conservation laws can be incorporated by using appropriate dimension
 * scales to convert other errors to energy errors.
 * </p>
 * <p>
 * The function is {@linkplain #value(ImmutableVectorN) calculated} by summing
 * the contributions of a {@linkplain #getTerms() collection of}
 * {@linkplain TimeStepEnergyErrorFunctionTerm terms}. Multiple physical
 * processes and multiple objects can be modelled by including terms for each of
 * the processes and objects.
 * </p>
 */
@Immutable
public final class TimeStepEnergyErrorFunction implements FunctionNWithGradient {

    private final ImmutableVectorN x0;
    private final double dt;
    private final List<TimeStepEnergyErrorFunctionTerm> terms;

    /**
     * <p>
     * Construct a functor that calculates the physical modelling error of a system
     * at a future point in time.
     * </p>
     *
     * <section>
     * <h1>Post Conditions</h1>
     * <ul>
     * <li>The constructed object has attribute and aggregate values equal to the
     * given values.</li>
     * </ul>
     * </section>
     *
     * @param x0
     *            The state vector of the physical system at the current point in
     *            time.
     * @param dt
     *            The size of the time-step; the difference between the future point
     *            in time and the current point in time.
     * @param terms
     *            The terms that contribute to the
     *            {@linkplain #value(ImmutableVectorN) value} of this function.
     * 
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code x0} is null.</li>
     *             <li>If {@code terms} is null.</li>
     *             <li>If {@code terms} contains any null references.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code dt} is not positive and
     *             {@linkplain Double#isInfinite() finite}.</li>
     *             <li>If and of the {@code terms} is not
     *             {@linkplain TimeStepEnergyErrorFunctionTerm#isValidForDimension(int)
     *             valid} for the {@linkplain ImmutableVectorN#getDimension()
     *             dimension} of {@code x0}.</li>
     *             </ul>
     */
    public TimeStepEnergyErrorFunction(ImmutableVectorN x0, double dt, List<TimeStepEnergyErrorFunctionTerm> terms) {
        Objects.requireNonNull(x0, "x0");
        Objects.requireNonNull(terms, "terms");
        if (dt <= 0.0 || !Double.isFinite(dt)) {
            throw new IllegalArgumentException("dt " + dt);
        }

        this.x0 = x0;
        this.dt = dt;
        this.terms = Collections.unmodifiableList(new ArrayList<>(terms));

        /* Check precondition after construction to avoid race hazards. */
        final int dimension = x0.getDimension();
        for (TimeStepEnergyErrorFunctionTerm term : this.terms) {
            Objects.requireNonNull(term, "term");
            if (!term.isValidForDimension(dimension)) {
                throw new IllegalArgumentException("term <" + term + "> not valid for " + dimension + " dimensions");
            }
        }
    }

    /**
     * <p>
     * The number of independent variables of this function; the number of variables
     * of the physical model.
     * </p>
     * <ul>
     * <li>The dimension equals the {@linkplain ImmutableVectorN#getDimension()
     * dimension} of the {@linkplain #getX0() state vector of the physical system at
     * the current point in time}.</li>
     * </ul>
     * 
     * @return the number of dimensions; positive.
     */
    @Override
    public final int getDimension() {
        return x0.getDimension();
    }

    /**
     * <p>
     * The size of the time-step; the difference between the future point in time
     * and the current point in time.
     * </p>
     * <ul>
     * <li>The time-step is positive and {@linkplain Double#isInfinite()
     * finite}.</li>
     * </ul>
     *
     * @return the dt
     */
    public final double getDt() {
        return dt;
    }

    /**
     * <p>
     * The terms that contribute to the {@linkplain #value(ImmutableVectorN) value}
     * of this function.
     * </p>
     * <ul>
     * <li>Always have a (non null) collection of terms.</li>
     * <li>The collection of terms does not {@linkplain Collection#contains(Object)
     * contain} any null elements.</li>
     * <li>The collection of terms may include duplicates.</li>
     * <li>The collection of terms may be
     * {@linkplain Collections#unmodifiableCollection(Collection)
     * unmodifiable}.</li>
     * </ul>
     *
     * @return the terms
     */
    public final List<TimeStepEnergyErrorFunctionTerm> getTerms() {
        return terms;
    }

    /**
     * <p>
     * The state vector of the physical system at the current point in time.
     * </p>
     * <ul>
     * <li>Always have a (non null) state vector of the physical system at the
     * current point in time.</li>
     * </ul>
     *
     * @return the state vector; not null.
     */
    public final ImmutableVectorN getX0() {
        return x0;
    }

    /**
     * <p>
     * Calculate the physical modelling error of the system at the
     * {@linkplain #getDt() future point in time}.
     * </p>
     * <ul>
     * <li>Always returns a (non null) value.</li>
     * <li>The {@linkplain Function1WithGradientValue#getX() domain value} of the
     * returned object is the given domain value.</li>
     * </ul>
     * 
     * @param state
     *            The state of the physical system at the future point in time
     * @return The error.
     * @throws NullPointerException
     *             If {@code state} is null.
     * @throws IllegalArgumentException
     *             If the {@linkplain ImmutableVectorN#getDimension() dimension} of
     *             {@code state} does not equal the {@linkplain #getDimension()
     *             dimension} of this functor.
     */
    @Override
    public final FunctionNWithGradientValue value(ImmutableVectorN state) {
        double e = 0.0;
        double[] dedx = new double[getDimension()];
        for (TimeStepEnergyErrorFunctionTerm term : terms) {
            e += term.evaluate(dedx, x0, state, dt);
        }
        return new FunctionNWithGradientValue(state, e, ImmutableVectorN.create(dedx));
    }

}
