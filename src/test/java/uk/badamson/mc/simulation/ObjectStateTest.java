package uk.badamson.mc.simulation;
/*
 * © Copyright Benedict Adamson 2018.
 *
 * This file is part of MC-des.
 *
 * MC-des is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MC-des is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MC-des.  If not, see <https://www.gnu.org/licenses/>.
 */

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
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

    static class DependentTestObjectState extends TestObjectState {
        private final UUID dependent;
        private final Duration dependencyDelay;

        DependentTestObjectState(final int i, final UUID dependent, final Duration dependencyDelay) {
            super(i);
            this.dependent = Objects.requireNonNull(dependent, "dependent");
            this.dependencyDelay = Objects.requireNonNull(dependencyDelay, "dependencyDelay");
            if (dependencyDelay.isNegative() || dependencyDelay.isZero())
                throw new IllegalArgumentException("dependencyDelay" + dependencyDelay);
        }

        @Override
        public void putNextStateTransition(final Universe.Transaction transaction, final UUID object,
                final Duration when) {
            requirePutNextStateTransitionPreconditions(this, transaction, object, when);

            final TestObjectState nextState = new TestObjectState(i + 1);
            transaction.getObjectState(dependent, when.minus(dependencyDelay));
            transaction.beginWrite(when.plusSeconds(1));
            transaction.put(object, nextState);
        }
    }// class

    static final class SelfDestructingObjectState extends TestObjectState {

        SelfDestructingObjectState(final int i) {
            super(i);
        }

        @Override
        public void putNextStateTransition(final Universe.Transaction transaction, final UUID object,
                final Duration when) {
            requirePutNextStateTransitionPreconditions(this, transaction, object, when);

            final ObjectState nextState = null;
            transaction.beginWrite(when.plusSeconds(1));
            transaction.put(object, nextState);
        }
    }

    static class SpawningTestObjectState extends TestObjectState {
        private final UUID child;
        private final int childId;

        SpawningTestObjectState(final int i, final int childId, final UUID child) {
            super(i);
            this.child = Objects.requireNonNull(child, "child");
            this.childId = childId;
        }

        @Override
        public void putNextStateTransition(final Universe.Transaction transaction, final UUID object,
                final Duration when) {
            requirePutNextStateTransitionPreconditions(this, transaction, object, when);

            final TestObjectState nextState = new TestObjectState(i + 1);
            final TestObjectState childState = new TestObjectState(childId);
            transaction.beginWrite(when.plusSeconds(1));
            transaction.put(object, nextState);
            transaction.put(child, childState);
        }
    }// class

    static class TestObjectState implements ObjectState {
        protected final int i;

        public TestObjectState(final int i) {
            this.i = i;
        }

        @Override
        public final boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof TestObjectState))
                return false;
            final TestObjectState other = (TestObjectState) obj;
            return i == other.i;
        }

        @Override
        public final int hashCode() {
            return i;
        }

        @Override
        public void putNextStateTransition(final Universe.Transaction transaction, final UUID object,
                final Duration when) {
            requirePutNextStateTransitionPreconditions(this, transaction, object, when);

            final TestObjectState nextState = new TestObjectState(i + 1);
            transaction.beginWrite(when.plusSeconds(1));
            transaction.put(object, nextState);
        }

        @Override
        public final String toString() {
            return getClass().getSimpleName() + " [" + i + "]";
        }

    }// class

    static final UUID OBJECT_A = UUID.randomUUID();
    static final UUID OBJECT_B = UUID.randomUUID();
    static final Duration WHEN_1 = Duration.ofSeconds(13);
    static final Duration WHEN_2 = Duration.ofSeconds(23);

    public static void assertInvariants(final ObjectState state) {
        // Do nothing
    }

    public static void assertInvariants(final ObjectState state1, final ObjectState state2) {
        // Do nothing
    }

    public static void putNextStateTransition(final ObjectState state, final Universe.Transaction transaction,
            final UUID object, final Duration when) {
        state.putNextStateTransition(transaction, object, when);

        assertInvariants(state);
        UniverseTest.TransactionTest.assertInvariants(transaction);

        final Duration transactionWhen = transaction.getWhen();
        assertThat("The method puts the given transaction into write mode with a write time-stamp in the future.",
                transactionWhen, greaterThan(when));
        for (final var dependencyEntry : transaction.getObjectStatesRead().entrySet()) {
            final ObjectStateId dependencyId = dependencyEntry.getKey();
            final UUID dependencyObject = dependencyId.getObject();
            final Duration dependencyWhen = dependencyId.getWhen();
            assertAll(
                    "The points in time for which the method fetches state information must be before the given point in time.",
                    () -> assertThat(dependencyWhen, lessThanOrEqualTo(when)),
                    () -> assertTrue(dependencyObject.equals(object) || dependencyWhen.compareTo(when) < 0));
        }
    }

    private static void requirePutNextStateTransitionPreconditions(final ObjectState state,
            final Universe.Transaction transaction, final UUID object, final Duration when) {
        Objects.requireNonNull(transaction, "transaction");
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(when, "when");
        if (!Collections.singletonMap(new ObjectStateId(object, when), state).equals(transaction.getObjectStatesRead()))
            throw new IllegalArgumentException("objectStatesRead does not consists of only this state");
    }
}