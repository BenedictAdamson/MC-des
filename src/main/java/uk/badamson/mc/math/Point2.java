package uk.badamson.mc.math;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A point in two dimensional space.
 * </p>
 */
@Immutable
public final class Point2 {

	private final double x;
	private final double y;

	/**
	 * <p>
	 * Construct an object with given attribute values.
	 * </p>
	 * 
	 * @param x
	 *            The x coordinate of this point; its <i>abcissa</i>.
	 * @param y
	 *            The y coordinate of this point; its <i>ordinate</i>.
	 */
	public Point2(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * <p>
	 * Whether this object is <dfn>equivalent</dfn> another object.
	 * </p>
	 * <p>
	 * The {@link Point2} class has <i>value semantics</i>: this object is
	 * equivalent to another object if, and only if, the other object is also a
	 * {@link Point2} object, and thw two objects have equivalent attributes.
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
		Point2 other = (Point2) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}

	/**
	 * <p>
	 * The x coordinate of this point; its <i>abcissa</i>.
	 * </p>
	 */
	public final double getX() {
		return x;
	}

	/**
	 * <p>
	 * The y coordinate of this point; its <i>ordinate</i>.
	 * </p>
	 */
	public final double getY() {
		return y;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		final long xBits = Double.doubleToLongBits(x);
		final long yBits = Double.doubleToLongBits(y);
		result = prime * result + (int) (xBits ^ (xBits >>> 32));
		result = prime * result + (int) (yBits ^ (yBits >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

}
