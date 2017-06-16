package uk.badamson.mc.math;

import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * One vector from the domain of a {@linkplain FunctionNWithGradient scalar
 * function of a vector that also has a computable gradient} to the
 * corresponding value in the codomain of the function and the gradient of the
 * function.
 * </p>
 */
@Immutable
public final class FunctionNValueWithGradient {

	private final double f;
	private final ImmutableVector x;
	private final ImmutableVector dfdx;

	/**
	 * <p>
	 * Construct an object with given attribute values.
	 * </p>
	 * 
	 * @param x
	 *            The domain vector.
	 * @param f
	 *            The codomain value.
	 * @param dfdx
	 *            The gradient vector
	 * 
	 * @throws NullPointerException
	 *             <ul>
	 *             <li>If {@code x} is null.</li>
	 *             <li>If {@code dfdx} is null.</li>
	 *             </ul>
	 * @throws IllegalArgumentException
	 *             If {@code x} and {@code dfdx} have different
	 *             {@linkplain ImmutableVector#getDimension() dimensions}.
	 */
	public FunctionNValueWithGradient(ImmutableVector x, double f, ImmutableVector dfdx) {
		Objects.requireNonNull(x, "x");
		Objects.requireNonNull(dfdx, "dfdx");
		if (x.getDimension() != dfdx.getDimension()) {
			throw new IllegalArgumentException(
					"Inconsistent dimensions x<" + x.getDimension() + ">, dfdx <" + dfdx.getDimension() + ">");
		}

		this.f = f;
		this.x = x;
		this.dfdx = dfdx;
	}

	/**
	 * <p>
	 * Whether this object is <dfn>equivalent</dfn> another object.
	 * </p>
	 * <p>
	 * The {@link FunctionNValueWithGradient} class has <i>value semantics</i>:
	 * this object is equivalent to another object if, and only if, the other
	 * object is also a {@link FunctionNValueWithGradient} object, and the two
	 * objects have equivalent attributes.
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
		FunctionNValueWithGradient other = (FunctionNValueWithGradient) obj;
		return Double.doubleToLongBits(f) == Double.doubleToLongBits(other.f) && x.equals(other.x)
				&& dfdx.equals(other.dfdx);
	}

	/**
	 * <p>
	 * The gradient vector.
	 * </p>
	 * <ul>
	 * <li>Always have a (non null) gradient vector.</li>
	 * <li>The {@linkplain ImmutableVector#getDimension() dimension} of the
	 * gradient vector is equal to the dimension of the {@linkplain #getX()
	 * domain vector}.</li>
	 * </ul>
	 */
	public final ImmutableVector getDfDx() {
		return dfdx;
	}

	/**
	 * <p>
	 * The codomain value.
	 * </p>
	 */
	public final double getF() {
		return f;
	}

	/**
	 * <p>
	 * The domain vector
	 * </p>
	 * 
	 * <ul>
	 * <li>Always have a (non null) domain vector.</li>
	 * </ul>
	 */
	public final ImmutableVector getX() {
		return x;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		final long fBits = Double.doubleToLongBits(f);
		result = prime * result + (int) (fBits ^ (fBits >>> 32));
		result = prime * result + x.hashCode();
		result = prime * result + dfdx.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "[" + x + "->" + f + ", dfdx=" + dfdx + "]";
	}

}
