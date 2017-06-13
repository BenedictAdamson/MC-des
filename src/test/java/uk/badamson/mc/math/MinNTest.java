package uk.badamson.mc.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * <p>
 * Unit tests of the {@link MinN} class.
 * </p>
 */
public class MinNTest {

	private static final FunctionN CONSTANT_1 = new FunctionN() {

		@Override
		public int getDimensions() {
			return 1;
		}

		@Override
		public double value(double[] x) {
			return 1.0;
		}
	};

	private static final FunctionN BILINEANR_1 = new FunctionN() {

		@Override
		public int getDimensions() {
			return 2;
		}

		@Override
		public double value(double[] x) {
			return x[0] + x[1];
		}
	};

	private static Function1 createLineFunction(final FunctionN f, final double[] x0, final double[] dx) {
		final Function1 lineFunction = MinN.createLineFunction(f, x0, dx);

		assertNotNull("Not null, result", lineFunction);

		return lineFunction;
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

}
