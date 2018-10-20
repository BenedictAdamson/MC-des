package uk.badamson.mc.simulation;

import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>
 * Unit tests and auxiliary test code for classes that implement the
 * {@link ObjectState} interface.
 * </p>
 */
public class ObjectStateTest {

    static final class TestObjectState implements ObjectState {
        private final int i;

        public TestObjectState(int i) {
            this.i = i;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof TestObjectState))
                return false;
            TestObjectState other = (TestObjectState) obj;
            return i == other.i;
        }

        @Override
        public int hashCode() {
            return i;
        }

        @Override
        public void putNextStateTransition(Universe.Transaction transaction, UUID object, Duration when) {
            Objects.requireNonNull(transaction, "transaction");
            Objects.requireNonNull(object, "object");
            Objects.requireNonNull(when, "when");
            if (!Collections.singletonMap(new ObjectStateId(object, when), this)
                    .equals(transaction.getObjectStatesRead())) {
                throw new IllegalArgumentException("objectStatesRead does not consists of only this state");
            }

            final TestObjectState nextState = new TestObjectState(i + 1);
            transaction.put(object, nextState);
        }

        @Override
        public String toString() {
            return "TestObjectState [" + i + "]";
        }

    }// class

    static final UUID OBJECT_A = UUID.randomUUID();
    static final UUID OBJECT_B = UUID.randomUUID();
    static final Duration WHEN_1 = Duration.ofSeconds(13);
    static final Duration WHEN_2 = Duration.ofSeconds(23);

    public static void assertInvariants(ObjectState state) {
        // Do nothing
    }

    public static void assertInvariants(ObjectState state1, ObjectState state2) {
        // Do nothing
    }

    public static void putNextStateTransition(ObjectState state, Universe.Transaction transaction, UUID object,
            Duration when) {
        state.putNextStateTransition(transaction, object, when);

        assertInvariants(state);
        UniverseTest.TransactionTest.assertInvariants(transaction);

        final Duration transactionWhen = transaction.getWhen();
        assertThat("The method puts the given transaction into write mode with a write time-stamp in the future.",
                transactionWhen, greaterThan(when));
        for (var dependencyEntry : transaction.getObjectStatesRead().entrySet()) {
            final ObjectStateId dependencyId = dependencyEntry.getKey();
            final UUID dependencyObject = dependencyId.getObject();
            final Duration dependencyWhen = dependencyId.getWhen();
            assertAll(
                    "The points in time for which the method fetches state information must be before the given point in time.",
                    () -> assertThat(dependencyWhen, lessThanOrEqualTo(when)),
                    () -> assertTrue(dependencyObject.equals(object) || dependencyWhen.compareTo(when) < 0));
        }
    }
}