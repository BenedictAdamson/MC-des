package uk.badamson.mc.math;

import java.util.Objects;

/**
 * <p>
 * Functions and auxiliary classes for minimization of a {@linkplain FunctionN
 * multidimensional function}.
 * </p>
 */
public final class MinN {

    private static double basicPowell(final FunctionN f, final double[] x0, final double[] dx0, final double[] x,
            final double[][] dx) throws PoorlyConditionedFunctionException {
        final int n = f.getDimension();
        assert n == x0.length;
        assert n == dx0.length;
        assert n == x.length;
        assert n == dx.length;
        copyTo(x0, x);
        for (int i = 0; i < n; ++i) {
            copyTo(dx0, dx[i]);
            minimiseAlongLine(f, x, dx0);
        }
        dx[n - 1] = dx[0];// recycle array
        for (int i = 0; i < n - 1; ++i) {
            dx[i] = dx[i + 1];
        }
        double xNewMax = 0;
        for (int j = 0; j < n; ++j) {
            final double xNew = x[j] - x0[j];
            dx[n - 1][j] = xNew;
            xNewMax = Math.max(xNewMax, Math.abs(xNew));
        }
        if (xNewMax < Min1.TOLERANCE) {
            /*
             * We have converged on the minimum, or the search directions have degenerated.
             */
            resetSearchDirections(dx);
        }
        copyTo(dx0, dx[n - 1]);
        return minimiseAlongLine(f, x, dx0);
    }

    private static void copyTo(double[] x, double[] y) {
        for (int j = 0, n = x.length; j < n; ++j) {
            x[j] = y[j];
        }
    }

    /**
     * <p>
     * Create a {@linkplain Function1 functor for a one-dimensional function of a
     * continuous variable} that is the evaluation of a {@linkplain FunctionN
     * functor for a multi-dimensional function of continuous variables} along a
     * given line.
     * </p>
     * <p>
     * The created functor retains references to the given objects. Those objects
     * should therefore not be changed while the created function is in use.
     * </p>
     * 
     * @param f
     *            The multi-dimensional function
     * @param x0
     *            The origin point; the position in the space of the
     *            multidimensional function corresponding to the origin point of the
     *            created function.
     * @param dx
     *            The direction vector of the line in the space of the
     *            multidimensional function; (x + dx) corresponds to the value for
     *            1.0 of the created function.
     * @return the created functor; not null.
     * 
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code f} is null.</li>
     *             <li>If {@code x0} is null.</li>
     *             <li>If {@code dx} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the length of {code x0} is 0.</li>
     *             <li>If the length of {code x0} is different from the length of
     *             {@code dx}.</li></li>
     *             <li>If the length of {code x0} is different from the
     *             {@linkplain FunctionN#getDimension() number of dimensions} of
     *             {@code f}.</li></li>
     *             </ul>
     */
    static Function1 createLineFunction(final FunctionN f, final double[] x0, final double[] dx) {
        Objects.requireNonNull(f, "f");
        Objects.requireNonNull(x0, "x0");
        Objects.requireNonNull(dx, "dx");
        final int n = x0.length;
        if (n == 0) {
            throw new IllegalArgumentException("x0.length == 0");
        }
        if (n != dx.length || n != f.getDimension()) {
            throw new IllegalArgumentException(
                    "Inconsistent lengths, x0 " + n + ", dx " + dx.length + ", f.dimensions " + f.getDimension());
        }

        return new Function1() {

            @Override
            public double value(double w) {
                final double[] x = new double[n];
                for (int i = 0; i < n; i++) {
                    x[i] = x0[i] + w * dx[i];
                }
                return f.value(x);
            }
        };
    }

    /**
     * <p>
     * Create a {@linkplain Function1 functor for a one-dimensional function of a
     * continuous variable} that is the evaluation of a {@linkplain FunctionN
     * functor for a multi-dimensional function of continuous variables} along a
     * given line.
     * </p>
     * <p>
     * The created functor retains references to the given objects. Those objects
     * should therefore not be changed while the created function is in use.
     * </p>
     * 
     * @param f
     *            The multi-dimensional function
     * @param x0
     *            The origin point; the position in the space of the
     *            multidimensional function corresponding to the origin point of the
     *            created function.
     * @param dx
     *            The direction vector of the line in the space of the
     *            multidimensional function; (x + dx) corresponds to the value for
     *            1.0 of the created function.
     * @return the created functor; not null.
     * 
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code f} is null.</li>
     *             <li>If {@code x0} is null.</li>
     *             <li>If {@code dx} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the {@linkplain ImmutableVector#getDimension() dimension}
     *             of {code x0} is different from the dimension of
     *             {@code dx}.</li></li>
     *             <li>If the dimension of {code x0} is different from the
     *             {@linkplain FunctionN#getDimension() number of dimensions} of
     *             {@code f}.</li></li>
     *             </ul>
     */
    public static Function1WithGradient createLineFunction(final FunctionNWithGradient f, final ImmutableVector x0,
            final ImmutableVector dx) {
        Objects.requireNonNull(f, "f");
        Objects.requireNonNull(x0, "x0");
        Objects.requireNonNull(dx, "dx");
        final int n = x0.getDimension();
        if (n != dx.getDimension() || n != f.getDimension()) {
            throw new IllegalArgumentException("Inconsistent lengths, x0 " + n + ", dx " + dx.getDimension()
                    + ", f.dimensions " + f.getDimension());
        }

        return new Function1WithGradient() {

            @Override
            public String toString() {
                final StringBuilder str = new StringBuilder();
                str.append(f);
                str.append(" along ");
                str.append(x0);
                str.append(" + w*");
                str.append(dx);
                return str.toString();
            }

            @Override
            public Function1WithGradientValue value(double w) {
                final ImmutableVector x = ImmutableVector.createOnLine(x0, dx, w);
                final FunctionNWithGradientValue v = f.value(x);
                return new Function1WithGradientValue(w, v.getF(), v.getDfDx().dot(dx));
            }
        };
    }

    private static ImmutableVector downSlope(FunctionNWithGradientValue fx) {
        final ImmutableVector dfDx = fx.getDfDx();
        if (dfDx.magnitude2() < Double.MIN_NORMAL) {
            /* Avoid division by zero when close to a minimum */
            final double[] x = new double[fx.getX().getDimension()];
            x[0] = 1.0;
            return ImmutableVector.create(x);
        } else {
            return dfDx.minus();
        }
    }

    /**
     * <p>
     * Find a minimum of a {@linkplain FunctionNWithGradient scalar function of a
     * vector that also has a computable gradient} using the Polak-Ribere's
     * modification of the Fletcher-Reeves conjugate gradient algorithm.
     * </p>
     * 
     * @param f
     *            The function for which a minimum is to be found.
     * @param x0
     *            A point at which to start the search.
     * @param tolerance
     *            The convergence tolerance; the dimensionless measure of the
     *            maximum error of the position of the minimum (the returned
     *            {@linkplain FunctionNWithGradientValue#getX() x} value).
     * @return a minimum of the function; not null.
     * 
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code f} is null.</li>
     *             <li>If {@code x} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the {@linkplain ImmutableVector#getDimension() dimension}
     *             of {code x} is different from the
     *             {@linkplain FunctionN#getDimension() dimension} of
     *             {@code f}.</li></li>
     *             <li>If {@code tolerance} is not in the range (0.0, 1.0).</li>
     *             </ul>
     * @throws PoorlyConditionedFunctionException
     *             <ul>
     *             <li>If {@code f} does not have a minimum</li>
     *             <li>If {@code f} has a minimum, but it is impossible to find
     *             using {@code x} because the function has an odd-powered high
     *             order term that causes the iterative procedure to diverge.</li>
     *             </ul>
     */
    public static FunctionNWithGradientValue findFletcherReevesPolakRibere(final FunctionNWithGradient f,
            ImmutableVector x0, double tolerance) throws PoorlyConditionedFunctionException {
        Objects.requireNonNull(f, "f");
        Objects.requireNonNull(x0, "x");
        requireToleranceInRange(tolerance);
        final int n = f.getDimension();
        if (x0.getDimension() != n) {
            throw new IllegalArgumentException("Inconsistent dimensions f <" + n + "> x <" + x0.getDimension() + ">");
        }

        FunctionNWithGradientValue fx = f.value(x0);
        ImmutableVector g = downSlope(fx);
        ImmutableVector dx = g;
        ImmutableVector h = g;
        double fScale = 0.0;

        while (true) {
            final ImmutableVector x = fx.getX();
            FunctionNWithGradientValue fXNew;
            try {
                fXNew = minimiseAlongLine(f, x, dx);
            } catch (PoorlyConditionedFunctionException e) {
                /*
                 * Can indicate that g has become a zero vector because we have reached the
                 * minimum.
                 */
                break;
            }
            final double df = fx.getF() - fXNew.getF();
            assert 0.0 <= df;
            fScale = Math.max(fScale, df);
            final double fTolerance = fScale * tolerance * tolerance * 0.5;
            ;
            if (df <= fTolerance) {
                fx = fXNew;
                break;// converged
            }

            final ImmutableVector gNew = downSlope(fXNew);
            final double gamma = (gNew.minus(g)).dot(gNew) / g.magnitude2();

            if (Math.abs(gamma) <= tolerance) {
                /*
                 * The gamma value is a dimensionless measure of the change in the search
                 * vector. When that becomes very small, the search vector is effectively zero,
                 * and we have located the minimum.
                 */
                fx = fXNew;
                break;// converged
            }
            final ImmutableVector hNew = ImmutableVector.createOnLine(gNew, h, gamma);

            g = gNew;
            h = hNew;
            fx = fXNew;
            dx = hNew;
        }
        return fx;
    }

    /**
     * <p>
     * Find a minimum of a {@linkplain FunctionN multidimensional function} using
     * <i>basic Powell's method</i> with periodic resetting of the search
     * directions.
     * </p>
     * <p>
     * This method is appropriate for a function that is approximately a quadratic
     * form.
     * </p>
     * 
     * @param f
     *            The function for which a minimum is to be found.
     * @param x
     *            A point at which to start the search. The method changes this
     *            value to record the minimum point.
     * @param tolerance
     *            The convergence tolerance; the minimum fractional change in the
     *            value of the minimum for which continuing to iterate is
     *            worthwhile.
     * @return a minimum of the function.
     * 
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code f} is null.</li>
     *             <li>If {@code x} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the length of {code x} is different from the
     *             {@linkplain FunctionN#getDimension() number of dimensions} of
     *             {@code f}.</li></li>
     *             <li>If {@code tolerance} is not in the range (0.0, 1.0).</li>
     *             </ul>
     * @throws PoorlyConditionedFunctionException
     *             <ul>
     *             <li>If {@code f} does not have a minimum</li>
     *             <li>If {@code f} has a minimum, but it is impossible to find
     *             using {@code x} because the function has an odd-powered high
     *             order term that causes the iterative procedure to diverge.</li>
     *             </ul>
     */
    public static double findPowell(final FunctionN f, final double[] x, double tolerance)
            throws PoorlyConditionedFunctionException {
        Objects.requireNonNull(f, "f");
        Objects.requireNonNull(x, "x");
        requireToleranceInRange(tolerance);
        final int n = f.getDimension();
        if (x.length != n) {
            throw new IllegalArgumentException("Inconsistent dimensions f <" + n + "> x <" + x.length + ">");
        }

        final double[] x0 = new double[n];
        final double[] dx0 = new double[n];
        final double[][] dx = new double[n][];
        for (int i = 0; i < n; ++i) {
            dx[i] = new double[n];
        }

        int iteration = 0;
        double min = Double.POSITIVE_INFINITY;
        while (true) {
            if (iteration % n == 0) {
                /*
                 * To prevent the search directions collapsing to a bundle of linearly dependent
                 * vectors, reset them to the basis vectors.
                 */
                resetSearchDirections(dx);
            }

            final double minNext = basicPowell(f, x0, dx0, x, dx);
            assert minNext <= min;
            final double dMin = minNext - min;
            min = minNext;
            iteration++;
            if (n <= iteration && dMin <= min * tolerance) {
                break;
            }
        }

        return min;
    }

    /**
     * <p>
     * Perform <i>line minimisation</i> of a {@linkplain FunctionN multidimensional
     * function}.
     * </p>
     * <p>
     * That is, find the minimum value of the function along a straight line.
     * </p>
     * 
     * <section>
     * <h1>Post Conditions</h1>
     * <ul>
     * <li>The point on the line ({@code x}) has been moved to the position of the
     * minimum found.</li>
     * <li>The direction vector has been set to the amount the point of the line was
     * moved to move from the original position to the position of the minimum.</li>
     * </ul>
     * </section>
     * 
     * @param f
     *            The multi-dimensional function
     * @param x
     *            A point on the line.
     * @param dx
     *            The direction vector of the line.
     * @return the minimum value along the line.
     * 
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code f} is null.</li>
     *             <li>If {@code x} is null.</li>
     *             <li>If {@code dx} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the length of {code x} is 0.</li>
     *             <li>If the length of {code x} is different from the length of
     *             {@code dx}.</li></li>
     *             <li>If the length of {code x} is different from the
     *             {@linkplain FunctionN#getDimension() number of dimensions} of
     *             {@code f}.</li></li>
     *             </ul>
     * @throws PoorlyConditionedFunctionException
     *             <ul>
     *             <li>If {@code f} does not have a minimum</li>
     *             <li>If {@code f} has a minimum, but it is impossible to find a
     *             bracket for {@code f} using {@code x} and {@code dx} because the
     *             function has an odd-powered high order term that causes the
     *             iterative procedure to diverge.</li>
     *             <li>The magnitude of {@code dx} is zero (or very small).</li>
     *             </ul>
     */
    static double minimiseAlongLine(final FunctionN f, final double[] x, final double[] dx)
            throws PoorlyConditionedFunctionException {
        final Function1 fLine = createLineFunction(f, x, dx);
        final Min1.Bracket bracket = Min1.findBracket(fLine, 0.0, 1.0);
        final Function1Value p = Min1.findBrent(fLine, bracket, Min1.TOLERANCE);
        final double w = p.getX();
        for (int i = 0, n = x.length; i < n; i++) {
            final double dxi = dx[i] * w;
            dx[i] = dxi;
            x[i] += dxi;
        }
        return p.getF();
    }

    /**
     * <p>
     * Perform <i>line minimisation</i> of a {@linkplain FunctionNWithGradient
     * scalar function of a vector that also has a computable gradient}.
     * </p>
     * <p>
     * That is, find the minimum value of the function along a straight line.
     * </p>
     * 
     * @param f
     *            The function
     * @param x
     *            A point on the line.
     * @param dx
     *            The direction vector of the line.
     * @return the minimum value along the line; not null.
     * 
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code f} is null.</li>
     *             <li>If {@code x} is null.</li>
     *             <li>If {@code dx} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the length of {code x} is 0.</li>
     *             <li>If the length of {code x} is different from the length of
     *             {@code dx}.</li></li>
     *             <li>If the length of {code x} is different from the
     *             {@linkplain FunctionN#getDimension() number of dimensions} of
     *             {@code f}.</li></li>
     *             </ul>
     * @throws PoorlyConditionedFunctionException
     *             <ul>
     *             <li>If {@code f} does not have a minimum</li>
     *             <li>If {@code f} has a minimum, but it is impossible to find a
     *             bracket for {@code f} using {@code x} and {@code dx} because the
     *             function has an odd-powered high order term that causes the
     *             iterative procedure to diverge.</li>
     *             <li>The magnitude of {@code dx} is zero (or very small).</li>
     *             </ul>
     */
    static FunctionNWithGradientValue minimiseAlongLine(final FunctionNWithGradient f, final ImmutableVector x,
            ImmutableVector dx) throws PoorlyConditionedFunctionException {
        final Function1WithGradient fLine = createLineFunction(f, x, dx);
        final Function1 f1Line = new Function1() {

            @Override
            public String toString() {
                return fLine.toString();
            }

            @Override
            public double value(double x) {
                return fLine.value(x).getF();
            }

        };
        final Min1.Bracket bracket = Min1.findBracket(f1Line, 0.0, 1.0);
        final Function1WithGradientValue p = Min1.findBrent(fLine, bracket, Min1.TOLERANCE);
        final ImmutableVector xMin = ImmutableVector.createOnLine(x, dx, p.getX());
        return f.value(xMin);
    }

    private static void requireToleranceInRange(double tolerance) {
        if (!(0.0 < tolerance && tolerance < 1.0)) {
            throw new IllegalArgumentException("tolerance <" + tolerance + ">");
        }
    }

    private static void resetSearchDirections(final double[][] dx) {
        final int n = dx.length;
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                dx[i][j] = (i == j ? 1 : 0);
            }
        }
    }

    private MinN() {
        throw new AssertionError("Class should not be instantiated");
    }

}
