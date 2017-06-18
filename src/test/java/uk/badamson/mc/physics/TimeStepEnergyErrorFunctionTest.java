package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.math.FunctionNWithGradientTest;
import uk.badamson.mc.math.FunctionNWithGradientValue;
import uk.badamson.mc.math.ImmutableVector;

/**
 * <p>
 * Unit tests for the class {@link TimeStepEnergyErrorFunction}.
 * </p>
 */
public class TimeStepEnergyErrorFunctionTest {

	private static final class Term0 implements TimeStepEnergyErrorFunction.Term {

		@Override
		public final boolean isValidForDimension(int n) {
			return true;
		}

	}// class

	/**
	 * <p>
	 * Unit tests classes that implement the interface
	 * {@link TimeStepEnergyErrorFunction.Test}.
	 * </p>
	 */
	public static class TermTest {

		public static void assertInvariants(TimeStepEnergyErrorFunction.Term t) {
			ObjectTest.assertInvariants(t);// inherited
		}

		public static void assertInvariants(TimeStepEnergyErrorFunction.Term t1, TimeStepEnergyErrorFunction.Term t2) {
			ObjectTest.assertInvariants(t1, t2);// inherited
		}
	}// class

	private static final double DT_A = 1.0;

	private static final double DT_B = 1E-3;
	private static final ImmutableVector X_1A = ImmutableVector.create(1.0);

	private static final ImmutableVector X_1B = ImmutableVector.create(2.0);
	private static final ImmutableVector X_2A = ImmutableVector.create(1.0, 2.0);
	private static final ImmutableVector X_2B = ImmutableVector.create(3.0, 5.0);

	private static final Term0 TERM_0A = new Term0();
	private static final Term0 TERM_0B = new Term0();

	public static void assertInvariants(TimeStepEnergyErrorFunction f) {
		ObjectTest.assertInvariants(f);// inherited
		FunctionNWithGradientTest.assertInvariants(f);// inherited

		final ImmutableVector x0 = f.getX0();
		final double dt = f.getDt();
		final Collection<TimeStepEnergyErrorFunction.Term> terms = f.getTerms();

		assertNotNull("Always have a state vector of the physical system at the current point in time.", x0);// guard
		assertNotNull("Always have a collection of terms.", terms);// guard

		assertTrue("The time-step <" + dt + "> is positive and finite.", 0.0 < dt && Double.isFinite(dt));
		for (TimeStepEnergyErrorFunction.Term term : terms) {
			assertNotNull("The collection of terms does not contain any null elements.", term);// guard
			TermTest.assertInvariants(term);
		}
		assertEquals(
				"The dimension equals the dimension of the state vector of the physical system at the current point in time.",
				x0.getDimension(), f.getDimension());
	}

	public static void assertInvariants(TimeStepEnergyErrorFunction f1, TimeStepEnergyErrorFunction f2) {
		ObjectTest.assertInvariants(f1, f2);// inherited
	}

	private static TimeStepEnergyErrorFunction constructor(ImmutableVector x0, double dt,
			List<TimeStepEnergyErrorFunction.Term> terms) {
		final TimeStepEnergyErrorFunction f = new TimeStepEnergyErrorFunction(x0, dt, terms);

		assertInvariants(f);
		assertSame("x0", x0, f.getX0());
		assertEquals("dt", dt, f.getDt(), Double.MIN_NORMAL);
		assertEquals("terms", terms, f.getTerms());

		return f;
	}

	private static FunctionNWithGradientValue value(TimeStepEnergyErrorFunction f, ImmutableVector x) {
		final FunctionNWithGradientValue fx = FunctionNWithGradientTest.value(f, x);// inherited

		return fx;
	}

	private static void value_0(ImmutableVector x0, double dt, ImmutableVector x) {
		final List<TimeStepEnergyErrorFunction.Term> terms = Collections.emptyList();
		final TimeStepEnergyErrorFunction f = new TimeStepEnergyErrorFunction(x0, dt, terms);

		final FunctionNWithGradientValue fx = FunctionNWithGradientTest.value(f, x);// inherited

		assertEquals("value.f", fx.getF(), 0.0, Double.MIN_NORMAL);
		assertEquals("value.dfDx <" + fx.getDfDx() + "> magnitude", fx.getDfDx().magnitude(), 0.0, Double.MIN_NORMAL);
	}

	@Test
	public void constructor_A() {
		final List<TimeStepEnergyErrorFunction.Term> terms = Collections.emptyList();
		constructor(X_1A, DT_A, terms);
	}

	@Test
	public void constructor_B() {
		final List<TimeStepEnergyErrorFunction.Term> terms = Collections.singletonList(TERM_0A);
		constructor(X_1A, DT_A, terms);
	}

	@Test
	public void constructor_C() {
		final List<TimeStepEnergyErrorFunction.Term> terms = Arrays.asList(TERM_0A, TERM_0B);
		constructor(X_1A, DT_B, terms);
	}

	@Test
	public void constructor_D() {
		final List<TimeStepEnergyErrorFunction.Term> terms = Arrays.asList(TERM_0A);
		constructor(X_2A, DT_B, terms);
	}

	@Test
	public void value_0A() {
		value_0(X_1A, DT_A, X_1B);
	}

	@Test
	public void value_0B() {
		value_0(X_2A, DT_B, X_2B);
	}
}
