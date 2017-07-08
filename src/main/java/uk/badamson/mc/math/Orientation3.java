package uk.badamson.mc.math;

import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * The orientation of a body in 3D space.
 * </p>
 * <p>
 * The orientation can be described by three orthogonal unit basis vectors,
 * {@linkplain #getE1() e1}, {@linkplain #getE2() e2} and {@linkplain #getE3()
 * e3}.
 * </p>
 */
@Immutable
public final class Orientation3 {

	/**
	 * <p>
	 * The orientation corresponding to the global coordinate system.
	 * </p>
	 * <ul>
	 * <li>The global basis is a (non null) orientation.</li>
	 * <li>The {@linkplain Orientation3#getE1() e1} vector of the global basis
	 * is the global x axis.</li>
	 * <li>The {@linkplain Orientation3#getE2() e2} vector of the global basis
	 * is the global y axis.</li>
	 * <li>The {@linkplain Orientation3#getE3() e3} vector of the global basis
	 * is the global z axis.</li>
	 * </ul>
	 */
	public static final Orientation3 GLOBAL_BASIS = new Orientation3(ImmutableVector.create(1, 0, 0),
			ImmutableVector.create(0, 1, 0), ImmutableVector.create(0, 0, 1));

	private static final double TOLERANCE = Math.max(Math.nextAfter(1.0, 2.0) - 1.0, 1.0 - Math.nextAfter(1.0, 0.0));

	/**
	 * <p>
	 * Create an orientation from three orthogonal unit basis vectors.
	 * </p>
	 *
	 * <section>
	 * <h1>Post Conditions</h1>
	 * <ul>
	 * <li>Always creates a (non null) object.</li>
	 * <li>The created object has the given attributes.</li>
	 * </ul>
	 * </section>
	 *
	 * @param e1
	 *            The first orthogonal unit basis vector of this orientation;
	 *            its local <i>x</i> direction.
	 * @param e2
	 *            The second orthogonal unit basis vector of this orientation;
	 *            its local <i>y</i> direction.
	 * @param e3
	 *            The third orthogonal unit basis vector of this orientation;
	 *            its local <i>z</i> direction.
	 * 
	 * @throws NullPointerException
	 *             <ul>
	 *             <li>If {@code e1} is null.</li>
	 *             <li>If {@code e2} is null.</li>
	 *             <li>If {@code e3} is null.</li>
	 *             </ul>
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>If {@code e1} does not have unit
	 *             {@linkplain ImmutableVector#magnitude() magnitude}.</li>
	 *             <li>If {@code e2} does not have unit magnitude.</li>
	 *             <li>If {@code e3} does not have unit magnitude.</li>
	 *             <li>If {@code e1} is not 3
	 *             {@linkplain ImmutableVector#getDimension() dimensional}.</li>
	 *             <li>If {@code e2} is not 3
	 *             {@linkplain ImmutableVector#getDimension() dimensional}.</li>
	 *             <li>If {@code e3} is not 3
	 *             {@linkplain ImmutableVector#getDimension() dimensional}.</li>
	 *             <li>If {@code e1} and {@code e2} do not have a zero
	 *             {@linkplain ImmutableVector#dot(ImmutableVector) dot
	 *             product}.</li>
	 *             <li>If {@code e1} and {@code e3} do not have a zero dot
	 *             product.</li>
	 *             <li>If {@code e2} and {@code e3} do not have a zero dot
	 *             product.</li>
	 *             </ul>
	 */
	public static Orientation3 createFromOrthogonalUnitBasisVectors(ImmutableVector e1, ImmutableVector e2,
			ImmutableVector e3) {
		requireUnit3Vector(e1, "e1");
		requireUnit3Vector(e2, "e2");
		requireUnit3Vector(e3, "e3");
		requireOrthogonal(e1, "e1", e2, "e2");
		requireOrthogonal(e1, "e1", e3, "e3");
		requireOrthogonal(e2, "e2", e3, "e3");

		return new Orientation3(e1, e2, e3);
	}

	private static void requireOrthogonal(ImmutableVector e1, String name1, ImmutableVector e2, String name2) {
		if (0.0 < Math.abs(e1.dot(e2))) {
			throw new IllegalArgumentException("Not orthogonal " + name1 + " " + e1 + " " + name2 + " " + e1);
		}
	}

	private static ImmutableVector requireUnit3Vector(ImmutableVector e, String message) {
		Objects.requireNonNull(e, message);
		final double m = e.magnitude2();
		if (e.getDimension() != 3 || TOLERANCE < Math.abs(m - 1.0)) {
			throw new IllegalArgumentException(message + " " + e + " (magnitude " + m + ")");
		}
		return e;
	}

	private final ImmutableVector e1;

	private final ImmutableVector e2;

	private final ImmutableVector e3;

	private Orientation3(ImmutableVector e1, ImmutableVector e2, ImmutableVector e3) {
		this.e1 = e1;
		this.e2 = e2;
		this.e3 = e3;
	}

	/**
	 * <p>
	 * The first orthogonal unit basis vector of this orientation; its local
	 * <i>x</i> direction.
	 * </p>
	 * <ul>
	 * <li>Always has a (non null) e1 vector</li>
	 * <li>The e1 vector is 3 {@linkplain ImmutableVector#getDimension()
	 * dimensional}.</li>
	 * <li>The e1 vector has unit {@linkplain ImmutableVector#magnitude()
	 * magnitude}</li>
	 * <li>The e1 vector is orthogonal to (has zero
	 * {@linkplain ImmutableVector#dot(ImmutableVector) dot product with})
	 * vector {@linkplain #getE2() e2}.</li>
	 * </ul>
	 *
	 * @return the e1 vector
	 */
	public final ImmutableVector getE1() {
		return e1;
	}

	/**
	 * <p>
	 * The second orthogonal unit basis vector of this orientation; its local
	 * <i>y</i> direction.
	 * </p>
	 * <ul>
	 * <li>Always has a (non null) e2 vector</li>
	 * <li>The e2 vector is 3 {@linkplain ImmutableVector#getDimension()
	 * dimensional}.</li>
	 * <li>The e2 vector has unit {@linkplain ImmutableVector#magnitude()
	 * magnitude}</li>
	 * <li>The e2 vector is orthogonal to (has zero
	 * {@linkplain ImmutableVector#dot(ImmutableVector) dot product with})
	 * vector {@linkplain #getE3() e3}.</li>
	 * </ul>
	 *
	 * @return the e2 vector
	 */
	public final ImmutableVector getE2() {
		return e2;
	}

	/**
	 * <p>
	 * The third orthogonal unit basis vector of this orientation; its local
	 * <i>z</i> direction.
	 * </p>
	 * <ul>
	 * <li>Always has a (non null) e3 vector</li>
	 * <li>The e3 vector is 3 {@linkplain ImmutableVector#getDimension()
	 * dimensional}.</li>
	 * <li>The e3 vector has unit {@linkplain ImmutableVector#magnitude()
	 * magnitude}</li>
	 * </ul>
	 *
	 * @return the e3 vector
	 */
	public final ImmutableVector getE3() {
		return e3;
	}

}
