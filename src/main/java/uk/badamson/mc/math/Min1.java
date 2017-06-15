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
	 * {@linkplain Function1Value points}:
	 * 
	 * </p>
	 */
	@Immutable
	public static final class Bracket {
		private final Function1Value left;
		private final Function1Value inner;
		private final Function1Value right;

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
		public Bracket(Function1Value left, Function1Value inner, Function1Value right) {
			Objects.requireNonNull(left, "left");
			Objects.requireNonNull(inner, "inner");
			Objects.requireNonNull(right, "right");
			/* Attention: precondition checks must handle NaN values. */
			final double innerX = inner.getX();
			final double innerY = inner.getF();
			if (!(left.getX() < innerX)) {
				throw new IllegalArgumentException("inner <" + inner + "> not to the right of left <" + left + ">");
			}
			if (!(innerX < right.getX())) {
				throw new IllegalArgumentException("right <" + right + "> not to the right of inner <" + inner + ">");
			}
			if (!(innerY < left.getF())) {
				throw new IllegalArgumentException("inner <" + inner + "> not below left <" + left + ">");
			}
			if (!(innerY < right.getF())) {
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
		 * leftmost point}; it has a larger {@linkplain Function1Value#getX()
		 * abscissa}.</li>
		 * <li>The inner point is below the leftmost point; it has a smaller
		 * {@linkplain Function1Value#getF() ordinate}.</li>
		 * </ul>
		 *
		 * @return the inner point.
		 */
		public final Function1Value getInner() {
			return inner;
		}

		/**
		 * <p>
		 * The leftmost point of this Bracket.
		 * </p>
		 *
		 * @return the leftmost point; not null.
		 */
		public final Function1Value getLeft() {
			return left;
		}

		/**
		 * <p>
		 * The smallest function value of the points constituting this bracket.
		 * </p>
		 * <ul>
		 * <li>The smallest function value of the points constituting this
		 * bracket is the {@linkplain Function1Value#getF() y value (ordinate)}
		 * of the {@linkplain #getInner() inner point} of the bracket.</li>
		 * </ul>
		 * 
		 * @return
		 */
		public final double getMin() {
			return inner.getF();
		}

		/**
		 * <p>
		 * The right point of of this Bracket.
		 * </p>
		 * <ul>
		 * <li>Always have a (non null) rightmost point.</li>
		 * <li>The rightmost point is to the right of the
		 * {@linkplain #getInner() inner point}; it has a smaller
		 * {@linkplain Function1Value#getX() abscissa}.</li>
		 * <li>The rightmost point is above the inner point; it has a larger
		 * {@linkplain Function1Value#getF() ordinate}.</li>
		 * </ul>
		 *
		 * @return the rightmost point
		 */
		public final Function1Value getRight() {
			return right;
		}

		/**
		 * <p>
		 * The size of this bracket.
		 * </p>
		 * <ul>
		 * <li>The width of a bracket is always positive.</li>
		 * <li>The width is the difference between the
		 * {@linkplain Function1Value#getX() x coordinate (abscissa)} of the
		 * {@linkplain #getRight() rightmost} point and the x coordinate of the
		 * {@linkplain #getLeft() leftmost} point.</li>
		 * </ul>
		 * 
		 * @return the width
		 */
		public final double getWidth() {
			return right.getX() - left.getX();
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
	 * The typical fractional tolerance possible for minimization of a
	 * {@linkplain Function1 one dimensional function}.
	 * </p>
	 * <p>
	 * If the true minimum occurs where the variable has value <var>x</var>, a
	 * numeric procedure will locate the minimum with an accuracy no better than
	 * this tolerance multiplied by <var>x</var>.
	 * </p>
	 */
	public static final double TOLERANCE = Math.sqrt(Math.nextAfter(1.0, 2.0) - 1.0);

	private static final double GOLD = (1.0 + Math.sqrt(5.0)) * 0.5;

	private static final double MAX_STEP = 100;

	/*
	 * Rounding errors mean that it is never worthwhile taking tiny steps.
	 * Instead try stepping into the largest interval.
	 */
	private static double avoidTinyStep(final double xNew, final double xTolerance, final double xLeft,
			final double xInner, final double xRight) {
		double dx = xNew - xInner;
		if (Math.abs(dx) < xTolerance) {
			final double xr = xRight - xInner;
			final double xl = xInner - xLeft;
			if (xl <= xr) {
				dx = Math.min(xTolerance, xr * 0.5);
			} else {
				dx = -Math.min(xTolerance, xl * 0.5);
			}
			return xInner + dx;
		} else {
			return xNew;
		}
	}

	private static double bisect(Function1ValueWithGradient left, Function1ValueWithGradient inner,
			Function1ValueWithGradient right) {
		final double xi = inner.getX();
		/*
		 * Use the gradient at teh inner point to guess which side probably
		 * contains the minmiium
		 */
		if (0.0 <= inner.getDfDx()) {
			return xi + (left.getX() - xi) * 0.5;
		} else {
			return xi + (right.getX() - xi) * 0.5;
		}
	}

	private static Function1Value evaluate(Function1 f, double x) {
		return new Function1Value(x, f.value(x));
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
	 * <li>The returned bracket has {@linkplain Function1Value#getF() y
	 * (ordinate)} values calculated from the corresponding
	 * {@linkplain Function1Value#getX() x (abscissa)} values using the given
	 * function.</li>
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

		Function1Value p1 = evaluate(f, x1);
		Function1Value p2 = evaluate(f, x2);
		if (p1.getF() < p2.getF()) {
			// Swap
			final Function1Value pTemp = p1;
			p1 = p2;
			p2 = pTemp;
		}
		assert p2.getF() <= p1.getF();
		/*
		 * First guess is to step in the same direction:
		 */
		Function1Value p3 = stepFurther(f, p1, p2);

		while (p3.getF() <= p2.getF() || p1.getF() <= p2.getF()) {
			final double xLimit = p2.getX() + MAX_STEP * (p3.getX() - p2.getX());
			final double xNew = parabolicExtrapolation(p1, p2, p3);
			if (isBetween(p2.getX(), xNew, p3.getX())) {
				final double fNew = f.value(xNew);
				if (fNew < p3.getF()) {
					// Found a minimum between p2 and p3
					assert !Double.isNaN(fNew);
					p1 = p2;
					p2 = new Function1Value(xNew, fNew);
					// p3 unchanged
					break;
				} else if (p2.getF() < fNew) {
					/*
					 * Function has higher order terms. We have p1.y > p2.y and
					 * fNew > p2.y, which can form a bracket
					 */
					assert !Double.isNaN(fNew);
					// p1 unchanged
					// p2 unchanged
					p3 = new Function1Value(xNew, fNew);
					break;
				} else {
					/*
					 * Parabolic fit failed; Step even further in the same
					 * direction and try again.
					 */
					final Function1Value pNew = stepFurther(f, p2, p3);
					assert !Double.isNaN(pNew.getF());
					p1 = p2;
					p2 = p3;
					p3 = pNew;
				}
			} else if (isBetween(p3.getX(), xNew, xLimit)) {
				/* Extrapolation is not excessive. */
				final Function1Value pNew = evaluate(f, xNew);
				assert !Double.isNaN(pNew.getF());
				if (pNew.getF() < p3.getF()) {
					/*
					 * Have stepped further down hill; continue stepping.
					 */
					p1 = p2;
					p2 = p3;
					p3 = pNew;
				} else if (p3.getF() < p2.getF()) {
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
					 * p2 and p3. Hard to test.
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
				final Function1Value pNew = evaluate(f, xLimit);
				assert !Double.isNaN(pNew.getF());
				p1 = p2;
				p2 = p3;
				p3 = pNew;
			} else if (isBetween(p1.getX(), xNew, p2.getX())) {
				final double fNew = f.value(xNew);
				if (fNew < p1.getF() && fNew < p2.getF()) {
					/* p1, pNew, p2 form a bracket. */
					// p1 unchanged
					p3 = p2;
					p2 = new Function1Value(xNew, fNew);
					break;
				} else {
					/*
					 * xNew seems to be near a local maximum. Step even further
					 * in the same direction to try to get away from it.
					 */
					final Function1Value pNew = stepFurther(f, p2, p3);
					assert !Double.isNaN(pNew.getF());
					p1 = p2;
					p2 = p3;
					p3 = pNew;
				}
			} else {// xNew <= p1.x
				/*
				 * Parabolic extrapolation stepped backwards. Step even further
				 * in the same direction and try again.
				 */
				final Function1Value pNew = stepFurther(f, p2, p3);
				assert !Double.isNaN(pNew.getF());
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

	/**
	 * <p>
	 * Find a minimum of a {@linkplain Function1 one dimensional function} using
	 * <i>Brent's method</i>.
	 * </p>
	 * <ul>
	 * <li>The method always returns a (non null) point.</li>
	 * <li>The returned point is consistent with the given function.</li>
	 * <li>The {@linkplain Function1Value#getF() codomain value} of the returned
	 * point is not larger than the {@linkplain Bracket#getMin() minimum value}
	 * of the given bracket.</li>
	 * </ul>
	 * 
	 * @param f
	 *            The function for which a minimum is to be found.
	 * @param bracket
	 *            A bracket of the minimum to be found.
	 * @param tolerance
	 *            The convergence tolerance. This should not normally exceed the
	 *            {@linkplain #TOLERANCE recommended minimum tolerance}.
	 * @return a minimum of the function.
	 * 
	 * @throws NullPointerException
	 *             <ul>
	 *             <li>If {@code f} is null.</li>
	 *             <li>If {@code bracket} is null.</li>
	 *             </ul>
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>If {@code tolerance} is not in the range (0.0, 1.0).</li>
	 *             <li>(Optional) if {@code bracket} is not consistent with
	 *             {@code f}.</li>
	 *             </ul>
	 */
	public static Function1Value findBrent(final Function1 f, Bracket bracket, double tolerance) {
		Objects.requireNonNull(f, "f");
		Objects.requireNonNull(bracket, "bracket");
		if (!(0.0 < tolerance && tolerance < 1.0)) {
			throw new IllegalArgumentException("tolerance <" + tolerance + ">");
		}

		Function1Value left = bracket.getLeft();
		Function1Value inner = bracket.getInner();
		Function1Value right = bracket.getRight();
		Function1Value secondLeast;
		Function1Value previousSecondLeast;
		if (left.getF() < right.getF()) {
			secondLeast = left;
			previousSecondLeast = right;
		} else {
			secondLeast = right;
			previousSecondLeast = left;
		}
		double width = right.getX() - left.getX();
		// Recent step sizes (start with guesses)
		double dx2 = width * (GOLD - 1.0);
		double dx3 = dx2 * GOLD;

		while (true) {
			final double xTolerance = Double.max(Math.abs(inner.getX()) * tolerance, Double.MIN_NORMAL);
			if (width <= xTolerance * 2.0) {
				// Converged
				break;
			}
			double xNew;
			if (xTolerance < Math.abs(dx3)) {
				/*
				 * We are making big steps, which are not close to the round-off
				 * limits, so parabolic extrapolation is worth trying.
				 */
				xNew = parabolicExtrapolation(inner, secondLeast, previousSecondLeast);
				/*
				 * But did parabolic extrapolation produce a reliable result? If
				 * not, fall back to golden section search.
				 */
				if (!Double.isFinite(xNew) || xNew <= left.getX() || right.getX() <= xNew
						|| Math.abs(dx3) * 0.5 <= Math.abs(xNew - inner.getX())) {
					xNew = goldenSectionSearch(left, inner, right);
				}
			} else {
				/*
				 * We are making small steps, which can be vulnerable to
				 * round-off error. So use golden section search instead.
				 */
				xNew = goldenSectionSearch(left, inner, right);
			}
			xNew = avoidTinyStep(xNew, xTolerance, left.getX(), inner.getX(), right.getX());
			final double dx = xNew - inner.getX();
			assert left.getX() < xNew && xNew < right.getX();
			final Function1Value pNew = evaluate(f, xNew);
			if (pNew.getF() < inner.getF()) {
				// Found a new minimum.
				previousSecondLeast = secondLeast;
				secondLeast = inner;
				if (xNew < inner.getX()) {
					// To the left of the current minimum
					// left unchanged
					right = inner;
				} else {
					// To the right of the current minimum
					// right unchanged
					left = inner;
				}
				inner = pNew;
			} else {
				/*
				 * Not a new minimum; but have narrowed the bracket.
				 */
				if (xNew < inner.getX()) {
					// To the left of the current minimum
					left = pNew;
				} else {
					right = pNew;
				}
				/*
				 * And might be a new value for the second smallest value.
				 */
				if (pNew.getF() < secondLeast.getF()) {
					previousSecondLeast = secondLeast;
					secondLeast = pNew;
				}
			}
			dx3 = dx2;
			dx2 = dx;
			width = right.getX() - left.getX();
		}

		return inner;
	}

	/**
	 * <p>
	 * Find a minimum of a {@linkplain Function1WithGradient one dimensional
	 * function that also has a computable gradient}, using <i>Brent's
	 * method</i>.
	 * </p>
	 * <ul>
	 * <li>The method always returns a (non null) point.</li>
	 * <li>The returned point is consistent with the given function.</li>
	 * <li>The {@linkplain Function1ValueWithGradient#getF() codomain value} of
	 * the returned point is not larger than the {@linkplain Bracket#getMin()
	 * minimum value} of the given bracket.</li>
	 * </ul>
	 * 
	 * @param f
	 *            The function for which a minimum is to be found.
	 * @param bracket
	 *            A bracket of the minimum to be found.
	 * @param tolerance
	 *            The convergence tolerance. This should not normally exceed the
	 *            {@linkplain #TOLERANCE recommended minimum tolerance}.
	 * @return a minimum of the function.
	 * 
	 * @throws NullPointerException
	 *             <ul>
	 *             <li>If {@code f} is null.</li>
	 *             <li>If {@code bracket} is null.</li>
	 *             </ul>
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>If {@code tolerance} is not in the range (0.0, 1.0).</li>
	 *             <li>(Optional) if {@code bracket} is not consistent with
	 *             {@code f}.</li>
	 *             </ul>
	 */
	public static Function1ValueWithGradient findBrent(final Function1WithGradient f, Bracket bracket,
			double tolerance) {
		Objects.requireNonNull(f, "f");
		Objects.requireNonNull(bracket, "bracket");
		if (!(0.0 < tolerance && tolerance < 1.0)) {
			throw new IllegalArgumentException("tolerance <" + tolerance + ">");
		}

		Function1ValueWithGradient left = f.value(bracket.getLeft().getX());
		Function1ValueWithGradient inner = f.value(bracket.getInner().getX());
		Function1ValueWithGradient right = f.value(bracket.getRight().getX());
		Function1ValueWithGradient secondLeast;
		Function1ValueWithGradient previousSecondLeast;
		if (left.getF() < right.getF()) {
			secondLeast = left;
			previousSecondLeast = right;
		} else {
			secondLeast = right;
			previousSecondLeast = left;
		}
		double width = right.getX() - left.getX();
		// Recent step sizes (start with guesses)
		double dx2 = width * (GOLD - 1.0);
		double dx3 = dx2 * GOLD;

		while (true) {
			final double xTolerance = Double.max(Math.abs(inner.getX()) * tolerance, Double.MIN_NORMAL);
			if (width <= xTolerance * 2.0) {
				// Converged
				break;
			}
			double xNew;
			if (xTolerance < Math.abs(dx3)) {
				/*
				 * We are making big steps, which are not close to the round-off
				 * limits, so secant extrapolation of the gradient is worth
				 * trying.
				 */
				final double xNew1 = secantGradientExtrapolation(inner, secondLeast);
				final double xNew2 = secantGradientExtrapolation(inner, previousSecondLeast);
				/*
				 * But did the extrapolation produce reliable results?
				 */
				final boolean ok1 = secantExtrapolationOk(xNew1, left, inner, right);
				final boolean ok2 = secantExtrapolationOk(xNew2, left, inner, right);
				/*
				 * Choose between the two options.
				 */
				if (ok1 && ok2) {
					/*
					 * Prefer the smallest change
					 */
					if (Math.abs(xNew1 - inner.getX()) <= Math.abs(xNew2 - inner.getX())) {
						xNew = xNew1;
					} else {
						xNew = xNew2;
					}
				} else if (ok1) {// && !ok2
					xNew = xNew1;
				} else if (ok2) {// && !ok1
					xNew = xNew2;
				} else { // !ok1 && !ok2
					/*
					 * Fall back to bisection
					 */
					xNew = bisect(left, inner, right);
				}
			} else {
				/*
				 * We are making small steps, which can be vulnerable to
				 * round-off error. So use bisection instead.
				 */
				xNew = bisect(left, inner, right);
			}
			xNew = avoidTinyStep(xNew, xTolerance, left.getX(), inner.getX(), right.getX());
			final double dx = xNew - inner.getX();
			assert left.getX() < xNew && xNew < right.getX();
			final Function1ValueWithGradient pNew = f.value(xNew);
			if (pNew.getF() < inner.getF()) {
				// Found a new minimum.
				previousSecondLeast = secondLeast;
				secondLeast = inner;
				if (xNew < inner.getX()) {
					// To the left of the current minimum
					// left unchanged
					right = inner;
				} else {
					// To the right of the current minimum
					// right unchanged
					left = inner;
				}
				inner = pNew;
			} else {
				/*
				 * Not a new minimum; but have narrowed the bracket.
				 */
				if (xNew < inner.getX()) {
					left = pNew;
				} else {
					right = pNew;
				}
				/*
				 * And might be a new value for the second smallest value.
				 */
				if (pNew.getF() < secondLeast.getF()) {
					previousSecondLeast = secondLeast;
					secondLeast = pNew;
				}
			}
			dx3 = dx2;
			dx2 = dx;
			width = right.getX() - left.getX();
		}

		return inner;
	}

	private static double goldenSectionSearch(Function1Value p1, Function1Value p2, Function1Value p3) {
		final double x2 = p2.getX();
		final double x21 = x2 - p1.getX();
		final double x32 = p3.getX() - x2;
		assert 0.0 < x21;
		assert 0.0 < x32;
		if (x21 <= x32) {
			return x2 + (2.0 - GOLD) * x32;
		} else {
			return x2 - (2.0 - GOLD) * x21;
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

	private static double parabolicExtrapolation(Function1Value p1, Function1Value p2, Function1Value p3) {
		final double x2 = p2.getX();
		final double y2 = p2.getF();
		final double x21 = x2 - p1.getX();
		final double x23 = x2 - p3.getX();
		final double y23 = y2 - p3.getF();
		final double y21 = y2 - p1.getF();
		final double x21y23 = x21 * y23;
		final double x23y21 = x23 * y21;
		final double xNew = x2 - (x21 * x21y23 - x23 * x23y21) / (2.0 * (x21y23 - x23y21));
		return xNew;
	}

	private static boolean secantExtrapolationOk(double xNew, Function1ValueWithGradient left,
			Function1ValueWithGradient inner, Function1ValueWithGradient right) {
		return left.getX() < xNew && xNew < right.getX() && (xNew - inner.getX()) * inner.getDfDx() <= 0;
	}

	private static double secantGradientExtrapolation(Function1ValueWithGradient p1, Function1ValueWithGradient p2) {
		final double x1 = p1.getX();
		final double x2 = p2.getX();
		final double y1 = p1.getDfDx();
		final double y2 = p2.getDfDx();

		final double y21 = y2 - y1;
		final double x21 = x2 - x1;

		final double dxdy = x21 / y21;

		return x1 - y1 * dxdy;
	}

	private static Function1Value stepFurther(Function1 f, Function1Value p1, Function1Value p2)
			throws PoorlyConditionedFunctionException {
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
			return new Function1Value(xNew, fNew);
		} else {
			throw new PoorlyConditionedFunctionException(f);
		}
	}

	private Min1() {
		throw new AssertionError("Class should not be instantiated");
	}

}
