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
			if (inner.getX() <= left.getX()) {
				throw new IllegalArgumentException("inner <" + inner + "> not to the right of left <" + left + ">");
			}
			if (right.getX() <= inner.getX()) {
				throw new IllegalArgumentException("right <" + right + "> not to the right of inner <" + inner + ">");
			}
			if (left.getY() <= inner.getY()) {
				throw new IllegalArgumentException("inner <" + inner + "> not below left <" + left + ">");
			}
			if (right.getY() <= inner.getY()) {
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

	private Min1() {
		throw new AssertionError("Class should not be instantiated");
	}
}
