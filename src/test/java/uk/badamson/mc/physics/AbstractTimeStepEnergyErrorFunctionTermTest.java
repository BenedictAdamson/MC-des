package uk.badamson.mc.physics;

import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * Unit tests for classes derived from
 * {@link AbstractTimeStepEnergyErrorFunctionTerm}.
 * </p>
 */
public class AbstractTimeStepEnergyErrorFunctionTermTest {

    private static class IsFinite extends TypeSafeMatcher<Double> {

        @Override
        public void describeMismatchSafely(Double item, Description mismatchDescription) {
            mismatchDescription.appendValue(item).appendText(" is not finite");
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a finite value");
        }

        @Override
        public boolean matchesSafely(Double item) {
            return item != null && Double.isFinite(item);
        }
    }// class

    private static final IsFinite IS_FINITE = new IsFinite();

    public static void assertInvariants(AbstractTimeStepEnergyErrorFunctionTerm term) {
        ObjectTest.assertInvariants(term);// inherited
        TimeStepEnergyErrorFunctionTermTest.assertInvariants(term);// inherited
    }

    public static void assertInvariants(AbstractTimeStepEnergyErrorFunctionTerm term1,
            AbstractTimeStepEnergyErrorFunctionTerm term2) {
        ObjectTest.assertInvariants(term1, term2);// inherited
        TimeStepEnergyErrorFunctionTermTest.assertInvariants(term1, term2);// inherited
    }

    public static void assertIsReferenceScale(String name, double scale) {
        assertThat(name, scale, allOf(greaterThan(0.0), IS_FINITE));
    }

    public static double evaluate(AbstractTimeStepEnergyErrorFunctionTerm term, double[] dedx, ImmutableVectorN state0,
            ImmutableVectorN state, double dt) {
        final double e = TimeStepEnergyErrorFunctionTermTest.evaluate(term, dedx, state0, state, dt);

        assertInvariants(term);

        return e;
    }
}
