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

    /**
     * <p>
     * Safely copy a term index array.
     * </p>
     * <p>
     * The copy is <dfn>safe</dfn> in that it the method throws exceptions rather
     * than copying an invalid term index array.
     * </p>
     * 
     * @param index
     *            The array to copy.
     * @param name
     *            The name or identifier of the term index array.
     * @return The copy.
     * @throws NullPointerException
     *             If {@code index} is null.
     * @throws IllegalArgumentException
     *             If and value of {@code index} is negative.
     */
    protected static final int[] copyTermIndex(int[] index, String name)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(index, name);
        final int n = index.length;
        final int[] copy = Arrays.copyOf(index, n);
        /* Check precondition after copy to avoid race hazards. */
        for (int i = 0; i < n; ++i) {
            requireTermIndex(copy[i], name + "[" + i + "]");
        }
        return copy;
    }

    /**
     * <p>
     * Extract some terms from a large state vector into a smaller state vector.
     * </p>
     * 
     * @param x
     *            The state vector.
     * @param term
     *            Which terms in the solution large state vector correspond to the
     *            components of smaller state vector {@code term[i]} is the index of
     *            component <var>i</var>.
     * @return the smaller vector
     */
    protected static final ImmutableVectorN extract(ImmutableVectorN x, int term[]) {
        final int n = term.length;
        final double[] extract = new double[n];
        for (int i = 0; i < n; i++) {
            extract[i] = x.get(term[i]);
        }
        return ImmutableVectorN.create(extract);
    }

    /**
     * <p>
     * Extract some terms from a large state vector into a smaller state vector for
     * a case in which multiple smaller state vectors could be extracted.
     * </p>
     * 
     * @param x
     *            The state vector.
     * @param term
     *            Which terms in the solution large state vector correspond to the
     *            components of smaller state vector {@code term[i0 + i]} is the
     *            index of component <var>i</var>.
     * @param i0
     *            Indicates which part of the {@code term} addressing array
     *            indicates the terms for this extraction.
     * @param n
     *            The number of dimensions of the smaller state vector
     * @return the smaller vector
     */
    protected static final ImmutableVectorN extract(ImmutableVectorN x, int term[], int i0, int n) {
        final double[] extract = new double[n];
        for (int i = 0; i < n; i++) {
            extract[i] = x.get(term[i0 + i]);
        }
        return ImmutableVectorN.create(extract);
    }

    /**
     * <p>
     * Throw an {@link IllegalArgumentException} iff two arrays have different
     * lengths.
     * </p>
     * 
     * @param array1
     *            The first of the two arrays to compare.
     * @param name1
     *            A name or identifier for the first array.
     * @param array2
     *            The second of the two arrays to compare.
     * @param name2
     *            A name or identifier for the second array.
     * @throws IllegalArgumentException
     *             If {@code array1} and {@code array2} have different lengths.
     */
    protected static void requireConsistentLengths(int[] array1, String name1, int[] array2, String name2)
            throws IllegalArgumentException {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException(
                    "Inconsistent " + name1 + ".length " + array1.length + " " + name2 + ".length " + array2.length);
        }
    }

    /**
     * <p>
     * Throw an {@link IllegalArgumentException} if a given value is unsuitable as a
     * reference scale.
     * </p>
     * 
     * @param s
     *            The value
     * @param name
     *            The name or identifier of the value
     * @return The given value.
     * @throws IllegalArgumentException
     *             If {@code s} is not positive and finite.
     */
    protected static double requireReferenceScale(double s, String name) throws IllegalArgumentException {
        if (!(0.0 < s && Double.isFinite(s))) {
            throw new IllegalArgumentException(name + " scale " + s);
        }
        return s;
    }

    /**
     * <p>
     * Throw an {@link IllegalArgumentException} if a given value is unsuitable as a
     * term index.
     * </p>
     * 
     * @param index
     *            The index
     * @param name
     *            The name or identifier of the value
     * @return The given index.
     * @throws IllegalArgumentException
     *             If {@code index} is negative.
     */
    protected static final int requireTermIndex(int index, String name) throws IllegalArgumentException {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index term " + name + " " + index);
        }
        return index;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The implementation provided for the
     * {@link AbstractTimeStepEnergyErrorFunctionTerm} type returns 0.
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
     *             <ul>
     *             <li>If the length of {@code dedx} does not equal the
     *             {@linkplain ImmutableVectorN#getDimension() dimension} of
     *             {@code state0}.</ul
     *             </ul>
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
