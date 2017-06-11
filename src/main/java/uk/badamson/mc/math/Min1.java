package uk.badamson.mc.math;

import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * Functions and auxiliary classes for minimization of a {@linkplain Function1
 * one dimensional function}.
 * </p>
 */
public final class Min1 {

	/**
	 * <p>
	 * A bracket of the minimum of a {@linkplain Function1 one dimensional
	 * function}.
	 * </p>
	 * <p>
	 * A Bracket indicates a range of values within which the minimum is known
	 * to be located.
	 * </p>
	 * <p>
	 * Each iteration of an iterative minimisation methods computes a new,
	 * smaller, bracket located within the previous bracket. A complication of
	 * iterative minimisation methods is that iterations should remember some of
	 * the previous function evaluations. It is therefore useful if the bracket
	 * records the function evaluations for the end points of the range of
	 * values. A further complication is that, to be sure we have a minimum
	 * within the bracket, we need to know that, somewhere within the range of
	 * values, there is a function value smaller than the values at the end
	 * points of the range. Furthermore, it is is useful to retain the position
	 * and the function value for that point. Hence a bracket has three
	 * {@linkplain Point2 points}:
	 * 
	 * </p>
	 */
	@Immutable
	public static final class Bracket {
		private final Point2 left;
		private final Point2 inner;
		private final Point2 right;

		/**
		 * <p>
		 * Construct a Bracket having given attribute values.
		 * </p>
		 *
		 * <section>
		 * <h1>Post Conditions</h1>
		 * <ul>
		 * <li>The constructed bracket has the given attribute values.</li>
		 * </ul>
		 * </section>
		 *
		 * @param left
		 *            The leftmost point of this Bracket.
		 * @param inner
		 *            The inner point of this Bracket.
		 * @param right
		 *            The right point of of this Bracket.
		 * 
		 * @throws NullPointerException
		 *             <ul>
		 *             <li>If {@code left} is null.</li>
		 *             <li>If {@code inner} is null.</li>
		 *             <li>If {@code right} is null.</li>
		 *             </ul>
		 * @throws IllegalArgumentException
		 *             <ul>
		 *             <li>If {@code inner} is not to the right of
		 *             {@code left}.</li>
		 *             <li>If {@code inner} is not below {@code left}.</li>
		 *             <li>If {@code right} is not to the right of
		 *             {@code inner}.</li>
		 *             <li>If {@code right} point is not above
		 *             {@code inner}.</li>
		 *             </ul>
		 */
		public Bracket(Point2 left, Point2 inner, Point2 right) {
			Objects.requireNonNull(left, "left");
			Objects.requireNonNull(inner, "inner");
			Objects.requireNonNull(right, "right");
			/* Attention: precondition checks must handle NaN values. */
			final double innerX = inner.getX();
			final double innerY = inner.getY();
			if (!(left.getX() < innerX)) {
				throw new IllegalArgumentException("inner <" + inner + "> not to the right of left <" + left + ">");
			}
			if (!(innerX < right.getX())) {
				throw new IllegalArgumentException("right <" + right + "> not to the right of inner <" + inner + ">");
			}
			if (!(innerY < left.getY())) {
				throw new IllegalArgumentException("inner <" + inner + "> not below left <" + left + ">");
			}
			if (!(innerY < right.getY())) {
				throw new IllegalArgumentException("inner <" + inner + "> not below right <" + right + ">");
			}
			this.left = left;
			this.inner = inner;
			this.right = right;
		}

		/**
		 * <p>
		 * Whether this object is <dfn>equivalent</dfn> another object.
		 * </p>
		 * <p>
		 * The {@link Min1.Bracket} class has <i>value semantics</i>: this
		 * object is equivalent to another object if, and only if, the other
		 * object is also a {@link Min1.Bracket} object, and thw two objects
		 * have equivalent attributes.
		 * </p>
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Bracket other = (Bracket) obj;
			return left.equals(other.left) && inner.equals(other.inner) && right.equals(other.right);
		}

		/**
		 * <p>
		 * The inner point of this Bracket.
		 * </p>
		 * <ul>
		 * <li>Always have a (non null) inner point.</li>
		 * <li>The inner point is to the right of the {@linkplain #getLeft()
		 * leftmost point}; it has a larger {@linkplain Point2#getX()
		 * abscissa}.</li>
		 * <li>The inner point is below the leftmost point; it has a smaller
		 * {@linkplain Point2#getY() ordinate}.</li>
		 * </ul>
		 *
		 * @return the inner point.
		 */
		public final Point2 getInner() {
			return inner;
		}

		/**
		 * <p>
		 * The leftmost point of this Bracket.
		 * </p>
		 *
		 * @return the leftmost point; not null.
		 */
		public final Point2 getLeft() {
			return left;
		}

		/**
		 * <p>
		 * The right point of of this Bracket.
		 * </p>
		 * <ul>
		 * <li>Always have a (non null) rightmost point.</li>
		 * <li>The rightmost point is to the right of the
		 * {@linkplain #getInner() inner point}; it has a smaller
		 * {@linkplain Point2#getX() abscissa}.</li>
		 * <li>The rightmost point is above the inner point; it has a larger
		 * {@linkplain Point2#getY() ordinate}.</li>
		 * </ul>
		 *
		 * @return the rightmost point
		 */
		public final Point2 getRight() {
			return right;
		}

		@Override
		public final int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + left.hashCode();
			result = prime * result + inner.hashCode();
			result = prime * result + right.hashCode();
			return result;
		}

	}// class

	/**
	 * <p>
	 * An exception class for indicating that minimization of a
	 * {@linkplain Function1 one dimensional function} is not possible because
	 * the function is poorly conditioned.
	 * </p>
	 * <ul>
	 * <li>The exception might indicate that the function does not have a
	 * minimum</li>
	 * <li>The exception might indicate that, although function has a minimum,
	 * it is impossible to
	 * {@linkplain Min1#findBracket(Function1, double, double) find a bracket}
	 * for a function with the starting points because the function has an
	 * odd-powered high order term that causes the iterative procedure to
	 * diverge.</li>
	 * </ul>
	 */
	public static final class PoorlyConditionedFunctionException extends IllegalArgumentException {

		private static final long serialVersionUID = 1L;

		private PoorlyConditionedFunctionException(Function1 f) {
			super("Poorly conditioned function " + f);
		}

	}// class

	private static final double GOLD = (1.0 + Math.sqrt(5.0)) * 0.5;

	private static final double MAX_STEP = 100;

	private static Point2 evaluate(Function1 f, double x) {
		return new Point2(x, f.value(x));
	}

	/**
	 * <p>
	 * Find a {@linkplain Min1.Bracket bracket} of a {@linkplain Function1 one
	 * dimensional function of a continuous variable}, given two values of the
	 * continuous variable.
	 * </p>
	 * <p>
	 * The method optimistically assumes that the two given values are close to
	 * a minimum, but copes if they are not near the minimum. The function will
	 * therefore be more efficient if the two given values <em>are</em> close to
	 * a minimum.
	 * </p>
	 * <ul>
	 * <li>Always returns a (non null) bracket.</li>
	 * <li>The returned bracket has {@linkplain Point2#getY() y (ordinate)}
	 * values calculated from the corresponding {@linkplain Point2#getX() x
	 * (abscissa)} values using the given function.</li>
	 * </ul>
	 * 
	 * @param f
	 *            The function for which a bracket is to be found.
	 * @param x1
	 *            A guess for the position of a minimum
	 * @param x2
	 *            A second guess for the position of the minimum
	 * @return a bracket
	 * 
	 * @throws NullPointerException
	 *             If {@code f} is null.
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>If {@code x1} equals {@code x2}.</li>
	 *             <li>If {@code x1} is {@linkplain Double#isNaN(double) is not
	 *             a number}.</li>
	 *             <li>If {@code x2} is is not a number.</li>
	 *             </ul>
	 * @throws PoorlyConditionedFunctionException
	 *             <ul>
	 *             <li>If {@code f} does not have a minimum</li>
	 *             <li>If {@code f} has a minimum, but it is impossible to find
	 *             a bracket for P@code f} using {@code x1} and {@code x2}
	 *             because the function has an odd-powered high order term that
	 *             causes the iterative procedure to diverge.</li>
	 *             </ul>
	 * 
	 */
	public static Bracket findBracket(final Function1 f, double x1, double x2)
			throws PoorlyConditionedFunctionException {
		Objects.requireNonNull(f, "f");
		if (x1 == x2) {
			throw new IllegalArgumentException("x1 == x2 <" + x1 + ">");
		}
		if (Double.isNaN(x1)) {
			throw new IllegalArgumentException("x1 NAN <" + x1 + ">");
		}
		if (Double.isNaN(x2)) {
			throw new IllegalArgumentException("x2 NAN <" + x2 + ">");
		}

		Point2 p1 = evaluate(f, x1);
		Point2 p2 = evaluate(f, x2);
		if (p1.getY() < p2.getY()) {
			// Swap
			final Point2 pTemp = p1;
			p1 = p2;
			p2 = pTemp;
		}
		assert p2.getY() <= p1.getY();
		/*
		 * First guess is to step in the same direction:
		 */
		Point2 p3 = stepFurther(f, p1, p2);

		while (p3.getY() <= p2.getY() || p1.getY() <= p2.getY()) {
			final double xLimit = p2.getX() + MAX_STEP * (p3.getX() - p2.getX());
			final double xNew = parabolicExtrapolation(p1, p2, p3);
			if (isBetween(p2.getX(), xNew, p3.getX())) {
				final double fNew = f.value(xNew);
				if (fNew < p3.getY()) {
					// Found a minimum between p2 and p3
					assert !Double.isNaN(fNew);
					p1 = p2;
					p2 = new Point2(xNew, fNew);
					// p3 unchanged
					break;
				} else if (p2.getY() < fNew) {
					/*
					 * Function has higher order terms. We have p1.y > p2.y and
					 * fNew > p2.y, which can form a bracket
					 */
					assert !Double.isNaN(fNew);
					// p1 unchanged
					// p2 unchanged
					p3 = new Point2(xNew, fNew);
					break;
				} else {
					/*
					 * Parabolic fit failed; Step even further in the same
					 * direction and try again.
					 */
					final Point2 pNew = stepFurther(f, p2, p3);
					assert !Double.isNaN(pNew.getY());
					p1 = p2;
					p2 = p3;
					p3 = pNew;
				}
			} else if (isBetween(p3.getX(), xNew, xLimit)) {
				/* Extrapolation is not excessive. */
				final Point2 pNew = evaluate(f, xNew);
				assert !Double.isNaN(pNew.getY());
				if (pNew.getY() < p3.getY()) {
					/*
					 * Have stepped further down hill; continue stepping.
					 */
					p1 = p2;
					p2 = p3;
					p3 = pNew;
				} else if (p3.getY() < p2.getY()) {
					/*
					 * Function has higher order terms. We have p3.y < p2.y and
					 * p3.y < pNew.y, which can form a bracket.
					 */
					return new Bracket(p2, p3, pNew);
				} else {
					/*
					 * We have p2.y < p1.y and p3.y = p2.y and p3.y <= fNew.
					 * Unclear where the minimum might be, but is perhaps
					 * between p2 and p3 (it will be, if the higher order terms
					 * do not contribute). If we use (p2, p3, pNew) as our next
					 * guess, parabolic extrapolation will guess a point between
					 * p2 and p3.
					 */
					p1 = p2;
					p2 = p3;
					p3 = pNew;
				}
			} else if (isBeyond(p3.getX(), xLimit, xNew)) {
				/*
				 * Extrapolation was excessive; clamp to the limit. In
				 * particular, this handles cases when the extrapolated value is
				 * infinite. Assume it is nevertheless a step in the right
				 * direction
				 */
				final Point2 pNew = evaluate(f, xLimit);
				assert !Double.isNaN(pNew.getY());
				p1 = p2;
				p2 = p3;
				p3 = pNew;
			} else if (isBetween(p1.getX(), xNew, p2.getX())) {
				final double fNew = f.value(xNew);
				if (fNew < p1.getY() && fNew < p2.getY()) {
					/* p1, pNew, p2 form a bracket. */
					// p1 unchanged
					p3 = p2;
					p2 = new Point2(xNew, fNew);
					break;
				} else {
					/*
					 * xNew seems to be near a local maximum. Step even further
					 * in the same direction to try to get away from it.
					 */
					final Point2 pNew = stepFurther(f, p2, p3);
					assert !Double.isNaN(pNew.getY());
					p1 = p2;
					p2 = p3;
					p3 = pNew;
				}
			} else {// xNew <= p1.x
				/*
				 * Parabolic extrapolation stepped backwards. Step even further
				 * in the same direction and try again.
				 */
				final Point2 pNew = stepFurther(f, p2, p3);
				assert !Double.isNaN(pNew.getY());
				p1 = p2;
				p2 = p3;
				p3 = pNew;
			}
		}
		if (p1.getX() < p2.getX()) {
			return new Bracket(p1, p2, p3);
		} else {
			return new Bracket(p3, p2, p1);
		}
	}

	private static boolean isBetween(double x1, double x2, double x3) {
		if (x1 < x3) {
			return x1 < x2 && x2 < x3;
		} else {
			return x3 < x2 && x2 < x1;
		}
	}

	private static boolean isBeyond(double x1, double x2, double x3) {
		if (x1 < x2) {
			return x2 < x3;
		} else {
			return x3 < x1;
		}
	}

	private static double parabolicExtrapolation(Point2 p1, Point2 p2, Point2 p3) {
		final double x2 = p2.getX();
		final double y2 = p2.getY();
		final double x21 = x2 - p1.getX();
		final double x23 = x2 - p3.getX();
		final double y23 = y2 - p3.getY();
		final double y21 = y2 - p1.getY();
		final double x21y23 = x21 * y23;
		final double x23y21 = x23 * y21;
		final double xNew = x2 - (x21 * x21y23 - x23 * x23y21) / (2.0 * (x21y23 - x23y21));
		return xNew;
	}

	private static Point2 stepFurther(Function1 f, Point2 p1, Point2 p2) throws PoorlyConditionedFunctionException {
		final double x2 = p2.getX();
		final double dx = x2 - p1.getX();
		double r = GOLD;
		double xNew;
		double fNew;
		do {
			xNew = x2 + r * dx;
			fNew = f.value(xNew);
			r *= 0.5;
		} while (Double.isNaN(fNew) && 0.0 < r && xNew != x2);
		if (xNew != x2) {
			return new Point2(xNew, fNew);
		} else {
			throw new PoorlyConditionedFunctionException(f);
		}
	}

	private Min1() {
		throw new AssertionError("Class should not be instantiated");
	}
}
