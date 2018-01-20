package uk.badamson.mc.math;

/**
 * <p>
 * An exception class for indicating that minimization of a
 * {@linkplain Function1 one dimensional function} is not possible because the
 * function is poorly conditioned.
 * </p>
 * <ul>
 * <li>The exception might indicate that the function does not have a
 * minimum</li>
 * <li>The exception might indicate that, although function has a minimum, it is
 * impossible to {@linkplain Min1#findBracket(Function1, double, double) find a
 * bracket} for a function with the starting points because the function has an
 * odd-powered high order term that causes the iterative procedure to
 * diverge.</li>
 * </ul>
 */
public final class PoorlyConditionedFunctionException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    PoorlyConditionedFunctionException(Function1 f) {
	super("Poorly conditioned function " + f);
    }

}// class