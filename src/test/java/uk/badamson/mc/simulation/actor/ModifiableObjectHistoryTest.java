package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2021.
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Flux;
import uk.badamson.mc.JsonTest;
import uk.badamson.mc.ThreadTest;
import uk.badamson.mc.history.ValueHistory;

@SuppressFBWarnings(justification = "Checking contract", value = "EC_NULL_ARG")
public class ModifiableObjectHistoryTest {

    @Nested
    public class CommitTo {

        @Test
        public void before() {
            final Duration end0 = WHEN_A;
            final Duration when = end0.minusNanos(1);// tough test

            test(end0, Integer.valueOf(0), when);
        }

        @Test
        public void endOfTime() {
            final Duration end0 = WHEN_A;
            final Duration when = ValueHistory.END_OF_TIME;// critical

            test(end0, Integer.valueOf(0), when);
        }

        @Test
        public void equal() {
            final long time = 1000;
            final Duration end0 = Duration.ofMillis(time);
            final Duration when = Duration.ofMillis(time);
            assert end0.equals(when);
            assert end0 != when;// tough test

            test(end0, Integer.valueOf(0), when);
        }

        @Test
        public void far() {
            final Duration end0 = WHEN_B;
            final Duration when = end0.plusDays(365);

            test(end0, Integer.valueOf(1), when);
        }

        @RepeatedTest(4)
        public void multipleThreads() {
            final int nThreads = 32;
            final Duration end0 = WHEN_B;
            final var history = new ModifiableObjectHistory<>(OBJECT_A, end0, Integer.valueOf(1));

            final CountDownLatch ready = new CountDownLatch(1);
            final var random = new Random(0);
            final List<Future<Void>> futures = new ArrayList<>(nThreads);
            for (int t = 0; t < nThreads; ++t) {
                futures.add(ThreadTest.runInOtherThread(ready,
                        () -> commitTo(history, end0.plusMillis(random.nextInt(1000)))));
            }
            ready.countDown();
            ThreadTest.get(futures);
        }

        @Test
        public void near() {
            final Duration end0 = WHEN_A;
            final Duration when = end0.plusNanos(1);// critical

            test(end0, Integer.valueOf(0), when);
        }

        private void test(@Nonnull final Duration end0, final Integer state, @Nonnull final Duration when) {
            final var history = new ModifiableObjectHistory<>(OBJECT_A, end0, state);

            commitTo(history, when);
        }

    }// class

    @Nested
    public class Constructor {

        @Nested
        public class Copy {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(1));
            }

            private <STATE> void test(@Nonnull final UUID object, @Nonnull final Duration start,
                    @Nonnull final STATE state) {
                /*
                 * tough test: not a ModifiableObjectHistory
                 */
                final var history = new ObjectHistory<>(object, start, state);

                constructor(history);
            }

        }// class

        @Nested
        public class History {

            @Nested
            public class OneStateTransition {
                @Test
                public void far() {
                    test(OBJECT_A, WHEN_A, WHEN_A.plusDays(365), Integer.valueOf(0));
                }

                @Test
                public void near() {
                    final var start = WHEN_B;
                    final var end = start.plusNanos(1);// critical
                    test(OBJECT_A, start, end, Integer.valueOf(3));
                }

                private void test(final UUID object, final Duration start, final Duration end, final Integer state) {
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                    stateTransitions.put(start, state);

                    constructor(object, end, stateTransitions);
                }

            }// class

            @Nested
            public class Two {

                @Test
                public void differentEnd() {
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>(
                            Map.of(WHEN_A, Integer.valueOf(-1)));
                    final var endA = WHEN_A.plusMillis(10);
                    final var endB = WHEN_A.plusMillis(20);

                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, endA, stateTransitions);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, endB, stateTransitions);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentObject() {
                    final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>(
                            Map.of(WHEN_A, Integer.valueOf(-1)));
                    final var end = WHEN_A.plusMillis(10);

                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, end, stateTransitions);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_B, end, stateTransitions);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentstateTransitions() {
                    final Duration start = WHEN_A;
                    final SortedMap<Duration, Integer> stateTransitionsA = new TreeMap<>(
                            Map.of(start, Integer.valueOf(-1)));
                    final SortedMap<Duration, Integer> stateTransitionsB = new TreeMap<>(
                            Map.of(start, Integer.valueOf(-2)));
                    final var end = start.plusMillis(10);
                    assert !stateTransitionsA.equals(stateTransitionsB);

                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, end, stateTransitionsA);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, end, stateTransitionsB);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void equivalent() {
                    final var objectA = OBJECT_A;
                    final var objectB = new UUID(objectA.getMostSignificantBits(), objectA.getLeastSignificantBits());
                    final long endMillis = 6000;
                    final var endA = Duration.ofMillis(endMillis);
                    final var endB = Duration.ofMillis(endMillis);
                    final SortedMap<Duration, Integer> stateTransitionsA = new TreeMap<>(
                            Map.of(Duration.ofMillis(5000), Integer.valueOf(Integer.MAX_VALUE)));
                    final SortedMap<Duration, Integer> stateTransitionsB = new TreeMap<>(stateTransitionsA);

                    assert objectA.equals(objectB);
                    assert endA.equals(endB);
                    assert stateTransitionsA.equals(stateTransitionsB);
                    assert objectA != objectB;// tough test
                    assert endA != endB;// tough test
                    assert stateTransitionsA != stateTransitionsB;// tough test

                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, endA, stateTransitionsA);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, endA, stateTransitionsA);

                    assertInvariants(historyA, historyB);
                    assertThat("equals", historyA, is(historyB));
                }

            }// class

            @Test
            public void twoStateTransitions() {
                final SortedMap<Duration, Integer> stateTransitions = new TreeMap<>();
                stateTransitions.put(WHEN_A, Integer.valueOf(0));
                stateTransitions.put(WHEN_A.plusSeconds(1), Integer.valueOf(1));

                constructor(OBJECT_A, WHEN_A.plusSeconds(2), stateTransitions);
            }

        }// class

        @Nested
        public class InitialState {

            @Nested
            public class Two {

                @Test
                public void differentObject() {
                    final var state = Integer.valueOf(0);
                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, WHEN_A, state);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_B, WHEN_A, state);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentStart() {
                    final var state = Integer.valueOf(0);
                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, WHEN_A, state);
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, WHEN_B, state);

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void differentState() {
                    final var historyA = new ModifiableObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(0));
                    final var historyB = new ModifiableObjectHistory<>(OBJECT_A, WHEN_A, Integer.valueOf(1));

                    assertInvariants(historyA, historyB);
                    assertThat("not equals", historyA, not(is(historyB)));
                }

                @Test
                public void equivalent() {
                    final var objectA = OBJECT_A;
                    final var objectB = new UUID(objectA.getMostSignificantBits(), objectA.getLeastSignificantBits());
                    final long startMillis = 6000;
                    final var startA = Duration.ofMillis(startMillis);
                    final var startB = Duration.ofMillis(startMillis);
                    final var stateA = Integer.valueOf(Integer.MAX_VALUE);
                    final var stateB = Integer.valueOf(Integer.MAX_VALUE);

                    assert objectA.equals(objectB);
                    assert startA.equals(startB);
                    assert stateA.equals(stateB);
                    assert objectA != objectB;// tough test
                    assert startA != startB;// tough test
                    assert stateA != stateB;// tough test

                    final var historyA = new ModifiableObjectHistory<>(objectA, startA, stateA);
                    final var historyB = new ModifiableObjectHistory<>(objectB, startB, stateB);

                    assertInvariants(historyA, historyB);
                    assertThat("equals", historyA, is(historyB));
                }
            }// class

            @Test
            public void a() {
                constructor(OBJECT_A, WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                constructor(OBJECT_B, WHEN_B, Integer.valueOf(1));
            }

        }// class

    }// class

    @Nested
    public class JSON {

        @Nested
        public class OneTransition {

            @Test
            public void a() {
                test(OBJECT_A, WHEN_A, Integer.valueOf(0));
            }

            @Test
            public void b() {
                test(OBJECT_B, WHEN_B, Integer.valueOf(1));
            }

            private <STATE> void test(@Nonnull final UUID object, @Nonnull final Duration start,
                    @Nonnull final STATE state) {
                final var history = new ModifiableObjectHistory<>(object, start, state);
                final var deserialized = JsonTest.serializeAndDeserialize(history);

                assertInvariants(history);
                assertInvariants(history, deserialized);
                assertAll(() -> assertThat("equals", deserialized, is(history)),
                        () -> assertEquals(history.getObject(), deserialized.getObject(), "object"),
                        () -> assertEquals(history.getStart(), deserialized.getStart(), "start"),
                        () -> assertEquals(history.getEnd(), deserialized.getEnd(), "end"),
                        () -> assertEquals(history.getStateHistory(), deserialized.getStateHistory(), "stateHistory"));
            }

        }// class

    }// class

    private static final UUID OBJECT_A = ObjectHistoryTest.OBJECT_A;
    private static final UUID OBJECT_B = ObjectHistoryTest.OBJECT_B;

    private static final Duration WHEN_A = ObjectHistoryTest.WHEN_A;
    private static final Duration WHEN_B = ObjectHistoryTest.WHEN_B;

    public static <STATE> void assertInvariants(@Nonnull final ModifiableObjectHistory<STATE> history) {
        ObjectHistoryTest.assertInvariants(history);// inherited
    }

    public static <STATE> void assertInvariants(@Nonnull final ModifiableObjectHistory<STATE> history1,
            @Nonnull final ModifiableObjectHistory<STATE> history2) {
        ObjectHistoryTest.assertInvariants(history1, history2);// inherited
    }

    private static <STATE> void commitTo(@Nonnull final ModifiableObjectHistory<STATE> history,
            @Nonnull final Duration when) {
        final var end0 = history.getEnd();

        history.commitTo(when);

        assertInvariants(history);
        final var end = history.getEnd();
        assertAll("end", () -> assertThat("does not decrease", end, greaterThanOrEqualTo(end0)),
                () -> assertThat("at least the given value", end, greaterThanOrEqualTo(when)));
    }

    private static <STATE> void constructor(@Nonnull final ObjectHistory<STATE> that) {
        final var copy = new ModifiableObjectHistory<>(that);

        assertInvariants(copy);
        ObjectHistoryTest.assertInvariants(copy, that);
        assertEquals(copy, that);
        assertAll("Copied", () -> assertSame(that.getEnd(), copy.getEnd(), "end"),
                () -> assertSame(that.getObject(), copy.getObject(), "object"),
                () -> assertSame(that.getStart(), copy.getStart(), "start"),
                () -> assertEquals(that.getStateHistory(), copy.getStateHistory(), "stateHistory"));
    }

    private static <STATE> void constructor(@Nonnull final UUID object, @Nonnull final Duration end,
            @Nonnull final SortedMap<Duration, STATE> stateTransitions) {
        final var history = new ModifiableObjectHistory<>(object, end, stateTransitions);

        assertInvariants(history);
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(end, history.getEnd(), "end"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, history.getStateTransitions(), "stateTransitions"));
    }

    private static <STATE> void constructor(@Nonnull final UUID object, @Nonnull final Duration start,
            @Nonnull final STATE state) {
        final var history = new ModifiableObjectHistory<>(object, start, state);

        assertInvariants(history);
        final var stateTransitions = history.getStateTransitions();
        assertAll(() -> assertSame(object, history.getObject(), "object"),
                () -> assertSame(start, history.getStart(), "start"), () -> assertSame(start, history.getEnd(), "end"),
                () -> assertSame(stateTransitions.firstKey(), history.getStart(), "start"),
                () -> assertEquals(stateTransitions, Map.of(start, state), "stateTransitions"));
    }

    public static <STATE> Publisher<Optional<STATE>> observeState(@Nonnull final ModifiableObjectHistory<STATE> history,
            @Nonnull final Duration when) {
        final var states = ObjectHistoryTest.observeState(history, when);// inherited

        assertInvariants(history);
        return states;
    }

    public static <STATE> Flux<ObjectHistory.TimestampedState<STATE>> observeTimestampedStates(
            @Nonnull final ModifiableObjectHistory<STATE> history) {
        final var flux = ObjectHistoryTest.observeTimestampedStates(history);// inherited

        assertInvariants(history);
        return flux;
    }
}