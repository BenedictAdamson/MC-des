package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2018,2021-22.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.badamson.dbc.assertions.CollectionVerifier;
import uk.badamson.dbc.assertions.EqualsSemanticsVerifier;
import uk.badamson.dbc.assertions.ObjectVerifier;
import uk.badamson.mc.history.ValueHistory;
import uk.badamson.mc.simulation.TimestampedId;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class UniverseTest {

    static final UUID OBJECT_A = ObjectHistoryTest.OBJECT_A;
    static final UUID OBJECT_B = ObjectHistoryTest.OBJECT_B;
    static final UUID OBJECT_C = ObjectHistoryTest.OBJECT_C;
    static final Duration WHEN_A = ObjectHistoryTest.WHEN_A;
    static final Duration WHEN_B = ObjectHistoryTest.WHEN_B;
    static final Duration WHEN_C = ObjectHistoryTest.WHEN_C;
    static final Duration WHEN_D = ObjectHistoryTest.WHEN_D;
    private static final UUID SIGNAL_A = SignalTest.ID_A;
    private static final UUID SIGNAL_B = SignalTest.ID_B;
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private static <STATE> void assertEmpty(@Nonnull final Universe<STATE> universe) {
        assertAll("empty", () -> assertThat("objects", universe.getObjects(), empty()),
                () -> assertThat("objectHistories", universe.getObjectHistories(), empty()));
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe) {
        ObjectVerifier.assertInvariants(universe);// inherited

        final Set<UUID> objects = universe.getObjects();
        final Collection<ObjectHistory<STATE>> objectHistories = universe.getObjectHistories();

        assertAll("Not null", () -> assertNotNull(objects, "objects"), // guard
                () -> assertNotNull(objectHistories, "objectHistories")// guard
        );
        assertFalse(objects.stream().anyMatch(Objects::isNull), "The set of object IDs does not contain a null.");
        CollectionVerifier.assertForAllElements("objectHistories", objectHistories, history -> {
            assertThat(history, notNullValue());// guard
            ObjectHistoryTest.assertInvariants(history);
        });
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe1,
                                                @Nonnull final Universe<STATE> universe2) {
        ObjectVerifier.assertInvariants(universe1, universe2);// inherited

        assertAll("Value semantics",
                () -> EqualsSemanticsVerifier.assertValueSemantics(universe1, universe2, "objectHistories",
                        Universe::getObjectHistories),
                () -> assertEquals(universe1.equals(universe2), universe1.getObjectHistories()
                        .equals(universe2.getObjectHistories()), "equals"));
    }

    private static <STATE> void constructor(@Nonnull final Collection<ObjectHistory<STATE>> objectHistories) {
        final var universe = new Universe<>(objectHistories);

        assertInvariants(universe);
        assertThat("copied objectHistories", new HashSet<>(universe.getObjectHistories()),
                is(new HashSet<>(objectHistories)));
    }

    private static <STATE> void constructor(@Nonnull final Universe<STATE> that) {
        final var copy = new Universe<>(that);

        assertInvariants(copy);
        assertInvariants(that);
        assertInvariants(copy, that);
        assertThat("equals", copy, is(that));

        assertAll("copied content", () -> assertThat("objects", copy.getObjects(), is(that.getObjects())),
                () -> assertThat("objectHistories", copy.getObjectHistories(), is(that.getObjectHistories())));
    }

    @Nonnull
    private static <STATE> Universe<STATE>.SchedulingMedium createMedium(@Nonnull final Universe<STATE> universe,
                                                                         @Nonnull final Executor executor, @Nonnull final Duration advanceTo) {
        final var medium = universe.createMedium(executor, advanceTo);

        assertInvariants(universe);
        assertThat("result", medium, notNullValue());// guard
        SchedulingMediumTest.assertInvariants(medium);

        return medium;
    }

    private static <STATE> ObjectHistory<STATE> getObjectHistory(@Nonnull final Universe<STATE> universe,
                                                                 @Nonnull final UUID object) {
        final var objectHistory = universe.getObjectHistory(object);

        assertInvariants(universe);
        assertThat("Returns null if, and only if, object is not the ID of an object in this universe",
                objectHistory == null == !universe.getObjects().contains(object));
        if (objectHistory != null) {
            ObjectHistoryTest.assertInvariants(objectHistory);
            assertThat("objectHistory.object", objectHistory.getObject(), is(object));
        }

        return objectHistory;
    }

    private static class DeferringExecutor implements Executor {

        @Override
        public void execute(@Nonnull final Runnable command) {
            // Do nothing
        }

    }// class

    public static class SchedulingMediumTest {

        static <STATE> void addAll(@Nonnull final Universe<STATE>.SchedulingMedium medium,
                                   @Nonnull final Collection<Signal<STATE>> signals) {
            MediumTest.addAll(medium, signals);// inherited

            assertInvariants(medium);
        }

        static <STATE> void assertInvariants(@Nonnull final Universe<STATE>.SchedulingMedium medium) {
            ObjectVerifier.assertInvariants(medium);// inherited
            MediumTest.assertInvariants(medium);// inherited

            final var signals = medium.getSignals();
            CollectionVerifier.assertForAllElements(medium.getUniverse().getObjectHistories(), history -> {
                assertThat("history", history, notNullValue());// guard
                assertThat("signals contains the received and incoming signals of all the object histories",
                        signals.containsAll(history.getReceivedAndIncomingSignals()));
            });
        }

        static <STATE> void removeAll(@Nonnull final Universe<STATE>.SchedulingMedium medium,
                                      @Nonnull final Collection<Signal<STATE>> signals) {
            MediumTest.removeAll(medium, signals);// inherited

            assertInvariants(medium);
        }

        private static <STATE> void scheduleAdvanceObject(@Nonnull final Universe<STATE>.SchedulingMedium medium,
                                                          @Nonnull final UUID object) {
            medium.scheduleAdvanceObject(object);

            assertInvariants(medium);
        }

        @Nested
        public class AddAll {

            @Nested
            public class DeferredTasks {

                @Test
                public void a() {
                    test(OBJECT_A, OBJECT_B, SIGNAL_A, WHEN_A, WHEN_B);
                }

                @Test
                public void b() {
                    test(OBJECT_B, OBJECT_C, SIGNAL_B, WHEN_B, WHEN_C);
                }

                private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver,
                                  @Nonnull final UUID signalId, @Nonnull final Duration start, @Nonnull final Duration whenSet) {
                    final Integer state0 = 0;
                    final Duration advanceTo = ValueHistory.END_OF_TIME;
                    final var history0 = new ObjectHistory<>(receiver, start, state0);
                    final TimestampedId sentFrom = new TimestampedId(sender, whenSet);
                    final Signal<Integer> signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);
                    final Universe<Integer> universe = new Universe<>(List.of(history0));
                    final Executor executor = new DeferringExecutor();// critical
                    final var medium = universe.createMedium(executor, advanceTo);
                    final var signals = Set.of(signal);

                    addAll(medium, signals);

                    final var history = universe.getObjectHistory(receiver);
                    UniverseTest.assertInvariants(universe);
                    assertInvariants(medium);
                    assertThat("receiver history", history, notNullValue());// guard
                    ObjectHistoryTest.assertInvariants(history);
                    assertAll(() -> assertThat("signals", medium.getSignals(), is(signals)),
                            () -> assertThat("receiver incomingSignals",
                                    history.getIncomingSignals(), is(signals)));
                }

            }// class

            @Nested
            public class RunningAllTasks {
                @Test
                public void a() {
                    test(OBJECT_A, OBJECT_B, SIGNAL_A, WHEN_A, WHEN_B);
                }

                @Test
                public void b() {
                    test(OBJECT_B, OBJECT_C, SIGNAL_B, WHEN_B, WHEN_C);
                }

                private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver,
                                  @Nonnull final UUID signalId, @Nonnull final Duration start, @Nonnull final Duration whenSet) {
                    final Integer state0 = 0;
                    final Duration advanceTo = ValueHistory.END_OF_TIME;
                    final var history0 = new ObjectHistory<>(receiver, start, state0);
                    final TimestampedId sentFrom = new TimestampedId(sender, whenSet);
                    final Signal<Integer> signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);
                    final Universe<Integer> universe = new Universe<>(List.of(history0));
                    final var medium = universe.createMedium(DIRECT_EXECUTOR, advanceTo);
                    final var signals = Set.of(signal);

                    addAll(medium, signals);

                    final var history = universe.getObjectHistory(receiver);
                    UniverseTest.assertInvariants(universe);
                    assertInvariants(medium);
                    assertThat("receiver history", history, notNullValue());// guard
                    assertAll("scheduled task to receive signal",
                            () -> assertThat("receivedSignals", history.getReceivedSignals(), not(empty())),
                            () -> assertThat("events", history.getEvents(), not(empty())));
                }

            }// class

        }// class

        @Nested
        public class RemoveAll {

            @Nested
            public class DeferredTasks {

                @Test
                public void a() {
                    test(OBJECT_A, OBJECT_B, SIGNAL_A, WHEN_A, WHEN_B);
                }

                @Test
                public void b() {
                    test(OBJECT_B, OBJECT_C, SIGNAL_B, WHEN_B, WHEN_C);
                }

                private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver,
                                  @Nonnull final UUID signalId, @Nonnull final Duration start, @Nonnull final Duration whenSet) {
                    final Integer state0 = 0;
                    final Duration advanceTo = ValueHistory.END_OF_TIME;
                    final var history0 = new ObjectHistory<>(receiver, start, state0);
                    final TimestampedId sentFrom = new TimestampedId(sender, whenSet);
                    final Signal<Integer> signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);
                    final Universe<Integer> universe = new Universe<>(List.of(history0));
                    final Executor executor = new DeferringExecutor();
                    final var medium = universe.createMedium(executor, advanceTo);
                    final var signals = Set.of(signal);
                    medium.addAll(signals);

                    removeAll(medium, signals);

                    final var history = universe.getObjectHistory(receiver);
                    UniverseTest.assertInvariants(universe);
                    assertInvariants(medium);
                    assertThat("receiver history", history, notNullValue());// guard
                    ObjectHistoryTest.assertInvariants(history);
                    assertAll(() -> assertThat("signals", medium.getSignals(), empty()),
                            () -> assertThat("receiver incomingSignals",
                                    history.getIncomingSignals(), empty()));
                }

            }// class

            @Nested
            public class RunningAllTasks {

                @Test
                public void a() {
                    test(WHEN_A, WHEN_B, WHEN_C);
                }

                @Test
                public void b() {
                    test(WHEN_B, WHEN_C, WHEN_D);
                }

                private void test(@Nonnull final Duration start, @Nonnull final Duration whenSent1,
                                  @Nonnull final Duration whenSent2) {
                    assert start.compareTo(whenSent1) < 0;
                    assert whenSent1.compareTo(whenSent2) < 0;
                    final UUID sender = OBJECT_A;
                    final UUID receiver = OBJECT_B;
                    final Integer state0 = 0;
                    final boolean strobe1 = false;// simplification
                    final boolean strobe2 = true;// tough test: emitted signals

                    final var sentFrom1 = new TimestampedId(sender, whenSent1);
                    final var signal1 = new SignalTest.TestSignal(SIGNAL_A, sentFrom1, receiver, strobe1);
                    final var sentFrom2 = new TimestampedId(sender, whenSent2);
                    final var signal2 = new SignalTest.TestSignal(SIGNAL_B, sentFrom2, receiver, strobe2);

                    final var history0 = new ObjectHistory<>(receiver, start, state0);
                    final Universe<Integer> universe = new Universe<>(List.of(history0));
                    final var medium = universe.createMedium(DIRECT_EXECUTOR, whenSent2);
                    medium.addAll(List.of(signal1, signal2));

                    removeAll(medium, List.of(signal1));

                    final var history = universe.getObjectHistory(receiver);
                    assertThat("receiver history", history, notNullValue());// guard
                    assertAll("scheduled and ran tasks to reprocess receiver",
                            () -> assertThat("receivedSignals", history.getReceivedSignals(), not(empty())),
                            () -> assertThat("events", history.getEvents(), not(empty())));
                }

            }// class
        }// class

        @Nested
        public class ScheduleAdvanceObject {

            @Test
            public void hasAdvancedFarEnough() {
                final Integer state0 = 0;
                final UUID sender = OBJECT_A;
                final UUID receiver = OBJECT_B;

                final var sentFrom1 = new TimestampedId(sender, WHEN_B);
                final var signal1 = new SignalTest.TestSignal(SIGNAL_A, sentFrom1, receiver);
                final var event1 = signal1.receive(state0);
                final var advanceTo = event1.getWhenOccurred();
                final var sentFrom2 = new TimestampedId(sender, advanceTo);
                final var signal2 = new SignalTest.TestSignal(SIGNAL_B, sentFrom2, receiver);

                final var history0 = new ObjectHistory<>(receiver, WHEN_A, state0);
                final var universe = new Universe<>(List.of(history0));
                final var medium = universe.createMedium(DIRECT_EXECUTOR, advanceTo);
                medium.addAll(List.of(signal1, signal2));
                medium.scheduleAdvanceObject(receiver);

                scheduleAdvanceObject(medium, receiver);

                final var history = universe.getObjectHistory(receiver);
                assertThat("receiver history", history, notNullValue());// guard
                assertAll("receiver",
                        () -> assertThat("incomingSignals (did not process the second signal)",
                                history.getIncomingSignals(), not(empty())),
                        () -> assertThat("receivedSignals", history.getReceivedSignals(), not(empty())));
            }

            @Nested
            public class EndIsFarEnough {

                @Test
                public void a() {
                    test(OBJECT_A, OBJECT_B, WHEN_A, WHEN_B, WHEN_C);
                }

                @Test
                public void b() {
                    test(OBJECT_B, OBJECT_A, WHEN_B, WHEN_C, WHEN_D);
                }

                @Test
                public void close() {
                    final var advanceTo = WHEN_A;
                    test(OBJECT_A, OBJECT_B, advanceTo, advanceTo, WHEN_C);
                }

                private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver,
                                  @Nonnull final Duration advanceTo, @Nonnull final Duration end, final Duration whenSent) {
                    assert advanceTo.compareTo(end) <= 0;
                    assert end.compareTo(whenSent) <= 0;
                    final Integer state0 = 0;

                    final var sentFrom = new TimestampedId(sender, whenSent);
                    final var signal = new SignalTest.TestSignal(SIGNAL_A, sentFrom, receiver);
                    final var history0 = new ObjectHistory<>(receiver, end, state0);
                    final var universe = new Universe<>(List.of(history0));
                    final var medium = universe.createMedium(DIRECT_EXECUTOR, advanceTo);
                    medium.addAll(List.of(signal));

                    scheduleAdvanceObject(medium, receiver);

                    final var history = universe.getObjectHistory(receiver);
                    assertThat("receiver history", history, notNullValue());// guard
                    assertAll("receiver did not process a signal",
                            () -> assertThat("incomingSignals", history.getIncomingSignals(), not(empty())),
                            () -> assertThat("receivedSignals", history.getReceivedSignals(), empty()));
                }

            }// class

            @Nested
            public class HasIncomingSignal {

                @Test
                public void a() {
                    test(OBJECT_A, OBJECT_B);
                }

                @Test
                public void b() {
                    test(OBJECT_B, OBJECT_A);
                }

                private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver) {
                    final Integer state0 = 0;
                    final var advanceTo = ValueHistory.END_OF_TIME;

                    final var sentFrom = new TimestampedId(sender, WHEN_B);
                    final var signal = new SignalTest.TestSignal(SIGNAL_A, sentFrom, receiver);
                    final var history0 = new ObjectHistory<>(receiver, WHEN_A, state0);
                    final var universe = new Universe<>(List.of(history0));
                    final var medium = universe.createMedium(DIRECT_EXECUTOR, advanceTo);
                    medium.addAll(List.of(signal));

                    scheduleAdvanceObject(medium, receiver);

                    final var history = universe.getObjectHistory(receiver);
                    assertThat("receiver history", history, notNullValue());// guard
                    assertAll("receiver", () -> assertThat("incomingSignals", history.getIncomingSignals(), empty()),
                            () -> assertThat("receivedSignals", history.getReceivedSignals(), not(empty())));
                }

            }// class

            @Nested
            public class RepeatedTasks {

                @Test
                public void far() {
                    final var start = WHEN_B;
                    final var advanceTo = start.plusDays(1);
                    test(start, WHEN_C, advanceTo);
                }

                @Test
                public void near() {
                    final var start = WHEN_A;
                    final var advanceTo = start.plusNanos(1);// critical
                    test(start, WHEN_B, advanceTo);
                }

                private void test(final Duration start, final Duration whenSent1, final Duration advanceTo) {
                    assert start.compareTo(advanceTo) < 0;
                    final boolean strobe = true;// critical
                    final UUID receiver = OBJECT_A;
                    final Integer state0 = 0;

                    final var sentFrom1 = new TimestampedId(OBJECT_A, whenSent1);
                    final var signal1 = new SignalTest.TestSignal(SIGNAL_A, sentFrom1, receiver, strobe);
                    final var history0 = new ObjectHistory<>(receiver, start, state0);
                    final var universe = new Universe<>(List.of(history0));
                    final var medium = universe.createMedium(DIRECT_EXECUTOR, advanceTo);
                    medium.addAll(List.of(signal1));

                    scheduleAdvanceObject(medium, receiver);

                    final var history = universe.getObjectHistory(receiver);
                    assertThat("receiver history", history, notNullValue());// guard
                    final var lastEvent = history.getLastEvent();
                    assertThat("lastEvent", lastEvent, notNullValue());// guard
                    assertThat("when last event occurred", lastEvent.getWhenOccurred(), greaterThan(advanceTo));
                }

            }// class

        }// class
    }// class

    @Nested
    public class Constructor {

        @Test
        public void noArgs() {
            final var universe = new Universe<>();

            assertInvariants(universe);
            assertEmpty(universe);
        }

        @Nested
        public class Copy {

            @Test
            public void empty() {
                final var universe = new Universe<Integer>();

                constructor(universe);

                assertEmpty(universe);
            }

            @Nested
            public class One {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, 0);
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, 1);
                }

                private void test(@Nonnull final UUID object, @Nonnull final Duration start,
                                  @Nonnull final Integer state) {
                    final var objectHistory = new ObjectHistory<>(object, start, state);
                    final Collection<ObjectHistory<Integer>> objectHistories = List.of(objectHistory);
                    final var universe = new Universe<>(objectHistories);

                    constructor(universe);
                }
            }// class
        }// class

        @Nested
        public class FromObjectHistories {

            @Test
            public void empty() {
                constructor(List.of());
            }

            @Test
            public void two() {
                final var objectHistoryA = new ObjectHistory<>(OBJECT_A, WHEN_A, 0);
                final var objectHistoryB = new ObjectHistory<>(OBJECT_B, WHEN_B, 1);
                final Collection<ObjectHistory<Integer>> objectHistories = List.of(objectHistoryA, objectHistoryB);

                constructor(objectHistories);
            }

            @Nested
            public class One {

                @Test
                public void a() {
                    test(OBJECT_A, WHEN_A, 0);
                }

                @Test
                public void b() {
                    test(OBJECT_B, WHEN_B, 1);
                }

                private void test(@Nonnull final UUID object, @Nonnull final Duration start,
                                  @Nonnull final Integer state) {
                    final var objectHistory = new ObjectHistory<>(object, start, state);
                    final Collection<ObjectHistory<Integer>> objectHistories = List.of(objectHistory);

                    constructor(objectHistories);
                }
            }// class
        }// class

        @Nested
        public class Two {

            @Test
            public void different_objects() {
                final var state = 0;
                final var objectHistoryA = new ObjectHistory<>(OBJECT_A, WHEN_A, state);
                final var objectHistoryB = new ObjectHistory<>(OBJECT_B, WHEN_A, state);
                final Collection<ObjectHistory<Integer>> objectHistoriesA = List.of(objectHistoryA);
                final Collection<ObjectHistory<Integer>> objectHistoriesB = List.of(objectHistoryB);
                final var universeA = new Universe<>(objectHistoriesA);
                final var universeB = new Universe<>(objectHistoriesB);

                assertInvariants(universeA, universeB);
                assertThat(universeA, not(is(universeB)));
            }

            @Test
            public void different_stateHistories() {
                final var objectHistoryA = new ObjectHistory<>(OBJECT_A, WHEN_A, 0);
                final var objectHistoryB = new ObjectHistory<>(OBJECT_A, WHEN_B, 1);
                final Collection<ObjectHistory<Integer>> objectHistoriesA = List.of(objectHistoryA);
                final Collection<ObjectHistory<Integer>> objectHistoriesB = List.of(objectHistoryB);
                final var universeA = new Universe<>(objectHistoriesA);
                final var universeB = new Universe<>(objectHistoriesB);

                assertInvariants(universeA, universeB);
                assertThat(universeA, not(is(universeB)));
            }

            @Test
            public void equivalent_empty() {
                final var universeA = new Universe<Integer>();
                final var universeB = new Universe<Integer>();

                assertInvariants(universeA, universeB);
                assertThat("equals", universeA, is(universeB));
            }

            @Test
            public void equivalent_nonEmpty() {
                final var object = OBJECT_A;
                final var objectHistoryA = new ObjectHistory<>(object, WHEN_A, 0);
                final var objectHistoryB = new ObjectHistory<>(object, WHEN_A, 0);
                final Collection<ObjectHistory<Integer>> objectHistoriesA = List.of(objectHistoryA);
                final Collection<ObjectHistory<Integer>> objectHistoriesB = List.of(objectHistoryB);
                final var universeA = new Universe<>(objectHistoriesA);
                final var universeB = new Universe<>(objectHistoriesB);

                assertInvariants(universeA, universeB);
                assertThat("equals", universeA, is(universeB));
            }
        }// class

    }// class

    @Nested
    public class CreateMedium {

        @Nested
        public class OneIncomingSignal {

            @Test
            public void a() {
                test(OBJECT_A, OBJECT_B, SIGNAL_A, WHEN_A, WHEN_B);
            }

            @Test
            public void b() {
                test(OBJECT_B, OBJECT_C, SIGNAL_B, WHEN_B, WHEN_C);
            }

            private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver, @Nonnull final UUID signalId,
                              @Nonnull final Duration start, @Nonnull final Duration whenSet) {
                final Integer state0 = 0;
                final Duration advanceTo = ValueHistory.END_OF_TIME;
                final var history = new ObjectHistory<>(receiver, start, state0);
                final TimestampedId sentFrom = new TimestampedId(sender, whenSet);
                final Signal<Integer> signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);
                history.addIncomingSignals(List.of(signal));
                final Universe<Integer> universe = new Universe<>(List.of(history));
                assert !history.getIncomingSignals().isEmpty();

                final var medium = createMedium(universe, DIRECT_EXECUTOR, advanceTo);

                assertThat("signals", medium.getSignals(), is(Set.of(signal)));
            }
        }// class

        @Nested
        public class OneReceivedSignal {

            @Test
            public void a() {
                test(OBJECT_A, OBJECT_B, SIGNAL_A, WHEN_A, WHEN_B);
            }

            @Test
            public void b() {
                test(OBJECT_B, OBJECT_C, SIGNAL_B, WHEN_B, WHEN_C);
            }

            private void test(@Nonnull final UUID sender, @Nonnull final UUID receiver, @Nonnull final UUID signalId,
                              @Nonnull final Duration start, @Nonnull final Duration whenSet) {
                final Integer state0 = 0;
                final Duration advanceTo = ValueHistory.END_OF_TIME;
                final var history = new ObjectHistory<>(receiver, start, state0);
                final TimestampedId sentFrom = new TimestampedId(sender, whenSet);
                final Signal<Integer> signal = new SignalTest.TestSignal(signalId, sentFrom, receiver);
                history.addIncomingSignals(List.of(signal));
                final Universe<Integer> universe = new Universe<>(List.of(history));
                final var medium0 = universe.createMedium(DIRECT_EXECUTOR, advanceTo);
                history.receiveNextSignal(medium0);
                assert !history.getReceivedSignals().isEmpty();

                final var medium = createMedium(universe, DIRECT_EXECUTOR, advanceTo);

                assertThat("signals", medium.getSignals(), is(Set.of(signal)));
            }
        }// class

        @Nested
        public class NoObjects {

            @Test
            public void toEndOfTime() {
                test(DIRECT_EXECUTOR, ValueHistory.END_OF_TIME);
            }

            @Test
            public void a() {
                test(new DeferringExecutor(), WHEN_A);
            }

            private void test(final Executor executor, final Duration advanceTo) {
                final var universe = new Universe<Integer>();

                final var medium = createMedium(universe, executor, advanceTo);

                assertThat("signals", medium.getSignals(), empty());
            }

        }// class


    }// class

    @Nested
    public class GetObjectHistory {

        @Test
        public void absent() {
            final var universe = new Universe<Integer>();

            final var objectHistory = getObjectHistory(universe, OBJECT_A);

            assertThat("objectHistory", objectHistory, nullValue());
        }

        @Nested
        public class Present {

            @Test
            public void a() {
                test(OBJECT_A);
            }

            @Test
            public void b() {
                test(OBJECT_B);
            }

            private void test(@Nonnull final UUID object) {
                final Integer state = 0;
                final var objectHistory0 = new ObjectHistory<>(object, WHEN_A, state);
                final var universe = new Universe<>(List.of(objectHistory0));

                final var objectHistory = getObjectHistory(universe, object);

                assertThat("objectHistory", objectHistory, is(objectHistory0));
            }

        }// class
    }// class
}
