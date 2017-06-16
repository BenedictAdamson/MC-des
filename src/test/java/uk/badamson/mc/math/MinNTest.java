package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Test;

/**
 * <p>
 * Unit tests of the {@link MinN} class.
 * </p>
 */
public class MinNTest {

	private static final FunctionN CONSTANT_1 = new FunctionN() {

		@Override
		public int getDimension() {
			return 1;
		}

		@Override
		public double value(double[] x) {
			return 1.0;
		}
	};

	private static final FunctionN BILINEANR_1 = new FunctionN() {

		@Override
		public int getDimension() {
			return 2;
		}

		@Override
		public double value(double[] x) {
			return x[0] + x[1];
		}
	};

	private static final FunctionN PARABOLOID = new FunctionN() {

		@Override
		public int getDimension() {
			return 2;
		}

		@Override
		public double value(double[] x) {
			return x[0] * x[0] + x[1] * x[1];
		}
	};

	private static final FunctionNWithGradient PARABOLOID_WITH_GRADIENT = new FunctionNWithGradient() {

		@Override
		public int getDimension() {
			return 2;
		}

		@Override
		public FunctionNValueWithGradient value(ImmutableVector x) {
			final double x0 = x.get(0);
			final double x1 = x.get(1);
			return new FunctionNValueWithGradient(x, x0 * x0 + x1 * x1, ImmutableVector.create(2.0 * x0, 2.0 * x1));
		}
	};

	private static final double adjacentPrecision(double x) {
		final double next = Math.nextAfter(x, Double.POSITIVE_INFINITY);
		return Math.max(x - next, Min1.TOLERANCE * Math.abs(x));
	}

	private static Function1 createLineFunction(final FunctionN f, final double[] x0, final double[] dx) {
		final Function1 lineFunction = MinN.createLineFunction(f, x0, dx);

		assertNotNull("Not null, result", lineFunction);

		return lineFunction;
	}

	private static Function1WithGradient createLineFunction(final FunctionNWithGradient f, ImmutableVector x0,
			ImmutableVector dx) {
		final Function1WithGradient lineFunction = MinN.createLineFunction(f, x0, dx);

		assertNotNull("Not null, result", lineFunction);

		return lineFunction;
	}

	private static void createLineFunction_paraboloidWithGradient(double x00, double x01, double dx0, double dx1,
			double w, double expectedF, double expectedDfDw, double toleranceF, double toleranceDfDw) {
		final ImmutableVector x = ImmutableVector.create(x00, x01);
		final ImmutableVector dx = ImmutableVector.create(dx0, dx1);

		final Function1WithGradient f = createLineFunction(PARABOLOID_WITH_GRADIENT, x, dx);

		final Function1ValueWithGradient fw = Function1WithGradientTest.value(f, w);
		assertEquals("f(" + w + ")", expectedF, fw.getF(), toleranceF);
		assertEquals("dfdw(" + w + ")", expectedDfDw, fw.getDfDx(), toleranceDfDw);
	}

	private static double findPowell(final FunctionN f, final double[] x, double tolerance) {
		final double min = MinN.findPowell(f, x, tolerance);

		assertEquals("Minimum value", f.value(x), min, adjacentPrecision(min));

		return min;
	}

	private static void findPowell_paraboloid(double x0, double x1, double tolerance) {
		final double[] x = { x0, x1 };
		final double precision = Math.sqrt(tolerance);

		findPowell(PARABOLOID, x, tolerance);

		assertEquals("x[0]", 0.0, x[0], precision);
		assertEquals("x[1]", 0.0, x[1], precision);
	}

	private static double magnitude(double[] x) {
		double m2 = 0.0;
		for (double xi : x) {
			m2 += xi * xi;
		}
		return Math.sqrt(m2);
	}

	private static double minimiseAlongLine(final FunctionN f, final double[] x, final double[] dx) {
		final int n = x.length;
		final double[] x0 = Arrays.copyOf(x, n);
		final double[] e0 = normalized(dx);

		final double min = MinN.minimiseAlongLine(f, x, dx);

		final double[] e = normalized(dx);
		final double em = magnitude(e);
		assertEquals("Minimum value", f.value(x), min, adjacentPrecision(min));
		for (int i = 0; i < n; ++i) {
			assertEquals("dx[" + i + "]", x[i] - x0[i], dx[i], adjacentPrecision(magnitude(dx)));
			assertEquals("direction[" + i + "]", em < Double.MIN_NORMAL ? 0.0 : e0[i], e[i], adjacentPrecision(e0[i]));
		}

		return min;
	}

	private static void minimiseAlongLine_paraboloid(double x0, double x1, double dx0, double dx1, double expectedXMin0,
			double expectedXMin1) {
		final double[] x = { x0, x1 };
		final double[] dx = { dx0, dx1 };
		final double precision = adjacentPrecision(magnitude(dx));

		minimiseAlongLine(PARABOLOID, x, dx);

		assertEquals("x[0]", expectedXMin0, x[0], precision);
		assertEquals("x[1]", expectedXMin1, x[1], precision);
	}

	private static void minimiseAlongLine_paraboloidAtMin(final double x0, final double x1, final double dx0,
			final double dx1) {
		final double expectedXMin0 = x0;
		final double expectedXMin1 = x1;

		minimiseAlongLine_paraboloid(x0, x1, dx0, dx1, expectedXMin0, expectedXMin1);
	}

	private static double[] normalized(double[] x) {
		final int n = x.length;
		final double m = magnitude(x);
		final double f = 0 < m ? 1.0 / m : 1.0;
		final double[] e = new double[n];
		for (int i = 0; i < n; ++i) {
			e[i] = x[i] * f;
		}
		return e;
	}

	@Test
	public void createLineFunction_bilinearA() {
		final double[] x0 = { 0.0, 0.0 };
		final double[] dx = { 1.0, 0.0 };

		final Function1 lineFunction = createLineFunction(BILINEANR_1, x0, dx);

		assertEquals("lineFunction[0]", 0.0, lineFunction.value(0.0), 1E-3);
		assertEquals("lineFunction[1.0]", 1.0, lineFunction.value(1.0), 1E-3);
		assertEquals("lineFunction[-1.0]", -1.0, lineFunction.value(-1.0), 1E-3);
	}

	@Test
	public void createLineFunction_bilinearB() {
		final double[] x0 = { 0.0, 0.0 };
		final double[] dx = { 0.0, 1.0 };

		final Function1 lineFunction = createLineFunction(BILINEANR_1, x0, dx);

		assertEquals("lineFunction[0]", 0.0, lineFunction.value(0.0), 1E-3);
		assertEquals("lineFunction[1.0]", 1.0, lineFunction.value(1.0), 1E-3);
		assertEquals("lineFunction[-1.0]", -1.0, lineFunction.value(-1.0), 1E-3);
	}

	@Test
	public void createLineFunction_bilinearC() {
		final double[] x0 = { 0.0, 0.0 };
		final double[] dx = { 1.0, 1.0 };

		final Function1 lineFunction = createLineFunction(BILINEANR_1, x0, dx);

		assertEquals("lineFunction[0]", 0.0, lineFunction.value(0.0), 1E-3);
		assertEquals("lineFunction[1.0]", 2.0, lineFunction.value(1.0), 1E-3);
		assertEquals("lineFunction[-1.0]", -2.0, lineFunction.value(-1.0), 1E-3);
	}

	@Test
	public void createLineFunction_constant() {
		final double[] x0 = { 0.0 };
		final double[] dx = { 1.0 };

		final Function1 lineFunction = createLineFunction(CONSTANT_1, x0, dx);

		assertEquals("lineFunction[0]", 1.0, lineFunction.value(0.0), 1E-3);
		assertEquals("lineFunction[1.0]", 1.0, lineFunction.value(1.0), 1E-3);
		assertEquals("lineFunction[-1.0]", 1.0, lineFunction.value(-1.0), 1E-3);
	}

	@Test
	public void createLineFunction_paraboloidWithGradientA() {
		final double x00 = 0.0;
		final double x01 = 0.0;
		final double dx0 = 1.0;
		final double dx1 = 0.0;
		final double w = 0.0;
		final double expectedF = 0.0;
		final double expectedDfDw = 0.0;
		final double toleranceF = Double.MIN_NORMAL;
		final double toleranceDfDw = Double.MIN_NORMAL;

		createLineFunction_paraboloidWithGradient(x00, x01, dx0, dx1, w, expectedF, expectedDfDw, toleranceF,
				toleranceDfDw);
	}

	@Test
	public void createLineFunction_paraboloidWithGradientB() {
		final double x00 = 1.0;
		final double x01 = 0.0;
		final double dx0 = 1.0;
		final double dx1 = 0.0;
		final double w = 0.0;
		final double expectedF = 1.0;
		final double expectedDfDw = 2.0;
		final double toleranceF = Double.MIN_NORMAL;
		final double toleranceDfDw = Double.MIN_NORMAL;

		createLineFunction_paraboloidWithGradient(x00, x01, dx0, dx1, w, expectedF, expectedDfDw, toleranceF,
				toleranceDfDw);
	}

	@Test
	public void createLineFunction_paraboloidWithGradientC() {
		final double x00 = 0.0;
		final double x01 = 0.0;
		final double dx0 = 2.0;
		final double dx1 = 0.0;
		final double w = 0.0;
		final double expectedF = 0.0;
		final double expectedDfDw = 0.0;
		final double toleranceF = Double.MIN_NORMAL;
		final double toleranceDfDw = Double.MIN_NORMAL;

		createLineFunction_paraboloidWithGradient(x00, x01, dx0, dx1, w, expectedF, expectedDfDw, toleranceF,
				toleranceDfDw);
	}

	@Test
	public void createLineFunction_paraboloidWithGradientD() {
		final double x00 = 0.0;
		final double x01 = 0.0;
		final double dx0 = 1.0;
		final double dx1 = 1.0;
		final double w = 0.0;
		final double expectedF = 0.0;
		final double expectedDfDw = 0.0;
		final double toleranceF = Double.MIN_NORMAL;
		final double toleranceDfDw = Double.MIN_NORMAL;

		createLineFunction_paraboloidWithGradient(x00, x01, dx0, dx1, w, expectedF, expectedDfDw, toleranceF,
				toleranceDfDw);
	}

	@Test
	public void createLineFunction_paraboloidWithGradientE() {
		final double x00 = 0.0;
		final double x01 = 0.0;
		final double dx0 = 1.0;
		final double dx1 = 0.0;
		final double w = 1.0;
		final double expectedF = 1.0;
		final double expectedDfDw = 2.0;
		final double toleranceF = Double.MIN_NORMAL;
		final double toleranceDfDw = Double.MIN_NORMAL;

		createLineFunction_paraboloidWithGradient(x00, x01, dx0, dx1, w, expectedF, expectedDfDw, toleranceF,
				toleranceDfDw);
	}

	@Test
	public void createLineFunction_paraboloidWithGradientF() {
		final double x00 = 1.0;
		final double x01 = 0.0;
		final double dx0 = 1.0;
		final double dx1 = 0.0;
		final double w = 1.0;
		final double expectedF = 4.0;
		final double expectedDfDw = 4.0;
		final double toleranceF = Double.MIN_NORMAL;
		final double toleranceDfDw = Double.MIN_NORMAL;

		createLineFunction_paraboloidWithGradient(x00, x01, dx0, dx1, w, expectedF, expectedDfDw, toleranceF,
				toleranceDfDw);
	}

	@Test
	public void createLineFunction_paraboloidWithGradientG() {
		final double x00 = 0.0;
		final double x01 = 1.0;
		final double dx0 = 1.0;
		final double dx1 = 0.0;
		final double w = 1.0;
		final double expectedF = 2.0;
		final double expectedDfDw = 2.0;
		final double toleranceF = Double.MIN_NORMAL;
		final double toleranceDfDw = Double.MIN_NORMAL;

		createLineFunction_paraboloidWithGradient(x00, x01, dx0, dx1, w, expectedF, expectedDfDw, toleranceF,
				toleranceDfDw);
	}

	@Test
	public void findPowell_paraboloidA() {
		findPowell_paraboloid(0, 1, 1E-3);
	}

	@Test
	public void findPowell_paraboloidAtMin() {
		findPowell_paraboloid(0, 0, 1E-3);
	}

	@Test
	public void findPowell_paraboloidB() {
		findPowell_paraboloid(1, 0, 1E-3);
	}

	@Test
	public void findPowell_paraboloidC() {
		findPowell_paraboloid(1, 1, 1E-3);
	}

	@Test
	public void findPowell_paraboloidD() {
		findPowell_paraboloid(1, 1, 1E-5);
	}

	@Test
	public void minimiseAlongLine_paraboloidA() {
		final double x0 = -1;
		final double x1 = 0;
		final double dx0 = 1;
		final double dx1 = 0;
		final double expectedXMin0 = 0;
		final double expectedXMin1 = 0;

		minimiseAlongLine_paraboloid(x0, x1, dx0, dx1, expectedXMin0, expectedXMin1);
	}

	@Test
	public void minimiseAlongLine_paraboloidAtMinA() {
		final double x0 = 0;
		final double x1 = 0;
		final double dx0 = 1;
		final double dx1 = 0;
		minimiseAlongLine_paraboloidAtMin(x0, x1, dx0, dx1);
	}

	@Test
	public void minimiseAlongLine_paraboloidAtMinB() {
		final double x0 = 0;
		final double x1 = 0;
		final double dx0 = 0;
		final double dx1 = 1;
		minimiseAlongLine_paraboloidAtMin(x0, x1, dx0, dx1);
	}

	@Test
	public void minimiseAlongLine_paraboloidAtMinC() {
		final double x0 = 0;
		final double x1 = 0;
		final double dx0 = 1;
		final double dx1 = 1;
		minimiseAlongLine_paraboloidAtMin(x0, x1, dx0, dx1);
	}

	@Test
	public void minimiseAlongLine_paraboloidAtMinD() {
		final double x0 = 0;
		final double x1 = 1;
		final double dx0 = 1;
		final double dx1 = 0;
		minimiseAlongLine_paraboloidAtMin(x0, x1, dx0, dx1);
	}

	@Test
	public void minimiseAlongLine_paraboloidAtMinE() {
		final double x0 = 1;
		final double x1 = 0;
		final double dx0 = 0;
		final double dx1 = 1;
		minimiseAlongLine_paraboloidAtMin(x0, x1, dx0, dx1);
	}

	@Test
	public void minimiseAlongLine_paraboloidB() {
		final double x0 = -2;
		final double x1 = 0;
		final double dx0 = 1;
		final double dx1 = 0;
		final double expectedXMin0 = 0;
		final double expectedXMin1 = 0;

		minimiseAlongLine_paraboloid(x0, x1, dx0, dx1, expectedXMin0, expectedXMin1);
	}

	@Test
	public void minimiseAlongLine_paraboloidC() {
		final double x0 = 1;
		final double x1 = 1;
		final double dx0 = 0;
		final double dx1 = -5;
		final double expectedXMin0 = 1;
		final double expectedXMin1 = 0;

		minimiseAlongLine_paraboloid(x0, x1, dx0, dx1, expectedXMin0, expectedXMin1);
	}
}
