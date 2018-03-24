package uk.badamson.mc.physics;

import java.util.Arrays;
import java.util.Objects;

import uk.badamson.mc.math.ImmutableVectorN;;

/**
 * <p>
 * An abstract base class for implementing a
 * {@linkplain TimeStepEnergyErrorFunctionTerm term} for a
 * {@linkplain TimeStepEnergyErrorFunction functor that calculates the physical
 * modelling error of a system at a future point in time}
 * </p>
 */
public abstract class AbstractTimeStepEnergyErrorFunctionTerm implements TimeStepEnergyErrorFunctionTerm {

    protected static final int[] copyTermIndex(int[] index, String name) {
        Objects.requireNonNull(index, name);
        final int n = index.length;
        final int[] copy = Arrays.copyOf(index, n);
        /* Check precondition after copy to avoid race hazards. */
        for (int i = 0; i < n; ++i) {
            requireTermIndex(copy[i], name + "[" + i + "]");
        }
        return copy;
    }

    protected static final ImmutableVectorN extract(ImmutableVectorN x, int term[]) {
        final int n = term.length;
        final double[] extract = new double[n];
        for (int i = 0; i < n; i++) {
            extract[i] = x.get(term[i]);
        }
        return ImmutableVectorN.create(extract);
    }

    protected static final ImmutableVectorN extract(ImmutableVectorN x, int term[], int i0, int n) {
        final double[] extract = new double[n];
        for (int i = 0; i < n; i++) {
            extract[i] = x.get(term[i0 + i]);
        }
        return ImmutableVectorN.create(extract);
    }

    protected static void requireConsistentLengths(int[] index1, String name1, int[] index2, String name2) {
        if (index1.length != index2.length) {
            throw new IllegalArgumentException(
                    "Inconsistent " + name1 + ".length " + index1.length + " " + name2 + ".length " + index2.length);
        }
    }

    protected static double requireReferenceScale(double s, String name) {
        if (!(0.0 < s && Double.isFinite(s))) {
            throw new IllegalArgumentException(name + " scale " + s);
        }
        return s;
    }

    protected static final int requireTermIndex(int index, String name) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index term " + name + " " + index);
        }
        return index;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The provided implementation checks its arguments and returns 0.
     * </p>
     * 
     * @param dedx
     *            {@inheritDoc}
     * @param state0
     *            {@inheritDoc}
     * @param state
     *            {@inheritDoc}
     * @param dt
     *            {@inheritDoc}
     * @return the value
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
    public double evaluate(double[] dedx, ImmutableVectorN state0, ImmutableVectorN state, double dt) {
        Objects.requireNonNull(dedx, "dedx");
        Objects.requireNonNull(state0, "x0");
        Objects.requireNonNull(state, "x");
        if (!(0.0 < dt && Double.isFinite(dt))) {
            throw new IllegalArgumentException("dt " + dt);
        }
        final int nState = state0.getDimension();
        if (state.getDimension() != nState) {
            throw new IllegalArgumentException(
                    "Inconsistent dimensions x0 " + nState + " and x " + state.getDimension());
        }
        if (dedx.length != nState) {
            throw new IllegalArgumentException(
                    "Inconsistent length of dedx " + dedx.length + " and dimension of x0 " + nState);
        }

        return 0.0;
    }
}
