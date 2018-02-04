package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the {@link Clock} class.
 * </p>
 */
public class ClockTest {

    private static final class RunnableSpy implements Runnable {

        private final Clock clock;
        private final long when;
        private int runs = 0;

        public RunnableSpy(Clock clock, long when) {
            this.clock = clock;
            this.when = when;
        }

        public final void assertRan(int expectedRuns) {
            assertEquals("ran", expectedRuns, runs);
        }

        @Override
        public final void run() {
            assertEquals("time", when, clock.getTime());
            runs++;
        }
    }// class

    static final long TIME_1 = -123L;
    static final long TIME_2 = 1_000L;
    static final long TIME_3 = 7_000L;
    static final long TIME_4 = 60_000L;
    static final long TIME_5 = 3_600_000L;

    public static void advance(Clock clock, long amount) {
        final long time0 = clock.getTime();

        clock.advance(amount);

        assertInvariants(clock);
        assertEquals("time", time0 + amount, clock.getTime());
    }

    private static void advance(long time, long amount) {
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time);

        advance(clock, amount);
    }

    private static void advance_with1Action(long time0, long actionTime, long timeToAdvanceTo) {
        assert time0 < actionTime;
        assert actionTime <= timeToAdvanceTo;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time0);
        final RunnableSpy action = new RunnableSpy(clock, actionTime);
        clock.scheduleActionAt(actionTime, action);

        advance(clock, timeToAdvanceTo - time0);

        action.assertRan(1);
    }

    private static void advance_with2Actions(long time0, long actionTime1, long actionTime2, long timeToAdvanceTo) {
        assert time0 < actionTime1;
        assert time0 < actionTime2;
        assert actionTime2 <= timeToAdvanceTo;
        assert actionTime1 <= timeToAdvanceTo;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time0);
        final RunnableSpy action1 = new RunnableSpy(clock, actionTime1);
        final RunnableSpy action2 = new RunnableSpy(clock, actionTime2);
        clock.scheduleActionAt(actionTime1, action1);
        clock.scheduleActionAt(actionTime2, action2);

        advance(clock, timeToAdvanceTo - time0);

        action1.assertRan(1);
        action2.assertRan(1);
    }

    private static void advance_withLaterAction(long time0, long timeToAdvanceTo, long actionTime) {
        assert time0 <= timeToAdvanceTo;
        assert timeToAdvanceTo < actionTime;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time0);
        final RunnableSpy action = new RunnableSpy(clock, actionTime);
        clock.scheduleActionAt(actionTime, action);

        advance(clock, timeToAdvanceTo - time0);

        action.assertRan(0);
    }

    public static final void advanceSeconds(Clock clock, double amount) {
        clock.advanceSeconds(amount);

        assertInvariants(clock);
    }

    private static void advanceSeconds(TimeUnit unit, long time, double amount, long expectedDt) {
        final Clock clock = new Clock(unit, time);

        advanceSeconds(clock, amount);

        assertEquals("time", time + expectedDt, clock.getTime());
    }

    public static void advanceTo(Clock clock, long when) {
        clock.advanceTo(when);

        assertInvariants(clock);
        assertEquals("time", when, clock.getTime());
    }

    private static void advanceTo(long time0, long when) {
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time0);

        advanceTo(clock, when);
    }

    private static void advanceTo_with1Action(long time0, long actionTime, long when) {
        assert time0 < actionTime;
        assert actionTime <= when;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time0);
        final RunnableSpy action = new RunnableSpy(clock, actionTime);
        clock.scheduleActionAt(actionTime, action);

        advanceTo(clock, when);

        action.assertRan(1);
    }

    private static void advanceTo_with1DelayedAction(long time0, long expectedActionTime, long when, TimeUnit clockUnit,
            long delay, TimeUnit delayUnit) {
        assert time0 < expectedActionTime;
        assert expectedActionTime <= when;
        final Clock clock = new Clock(clockUnit, time0);
        final RunnableSpy action = new RunnableSpy(clock, expectedActionTime);
        clock.scheduleDelayedAction(delay, delayUnit, action);

        advanceTo(clock, when);

        action.assertRan(1);
    }

    private static void advanceTo_with2Actions(long time0, long actionTime1, long actionTime2, long when) {
        assert time0 < actionTime1;
        assert time0 < actionTime2;
        assert actionTime2 <= when;
        assert actionTime1 <= when;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time0);
        final RunnableSpy action1 = new RunnableSpy(clock, actionTime1);
        final RunnableSpy action2 = new RunnableSpy(clock, actionTime2);
        clock.scheduleActionAt(actionTime1, action1);
        clock.scheduleActionAt(actionTime2, action2);

        advanceTo(clock, when);

        action1.assertRan(1);
        action2.assertRan(1);
    }

    private static void advanceTo_withLaterAction(long time0, long when, long actionTime) {
        assert time0 <= when;
        assert when < actionTime;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time0);
        final RunnableSpy action = new RunnableSpy(clock, actionTime);
        clock.scheduleActionAt(actionTime, action);

        advanceTo(clock, when);

        action.assertRan(0);
    }

    public static void assertInvariants(Clock clock) {
        ObjectTest.assertInvariants(clock);// inherited

        assertNotNull("Not null, unit", clock.getUnit());
    }

    public static void assertInvariants(Clock clock1, Clock clock2) {
        ObjectTest.assertInvariants(clock1, clock2);// inherited
    }

    private static Clock constructor(TimeUnit unit, long time) {
        final Clock clock = new Clock(unit, time);

        assertInvariants(clock);
        assertSame("unit", unit, clock.getUnit());
        assertEquals("time", time, clock.getTime());

        return clock;
    }

    public static void scheduleActionAt(Clock clock, long when, Runnable action) {
        clock.scheduleActionAt(when, action);

        assertInvariants(clock);
    }

    private static void scheduleActionAt_future(final long time, final long when) {
        assert time < when;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time);
        final RunnableSpy action = new RunnableSpy(clock, when);

        scheduleActionAt(clock, when, action);

        action.assertRan(0);
    }

    private static void scheduleActionAt_immediate(long time) {
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time);
        final RunnableSpy action = new RunnableSpy(clock, time);

        scheduleActionAt(clock, time, action);

        action.assertRan(0);
    }

    public static long scheduleDelayedAction(Clock clock, long delay, TimeUnit delayUnit, Runnable action) {
        final long when = clock.scheduleDelayedAction(delay, delayUnit, action);

        assertInvariants(clock);
        assertTrue("Scheduled time is at or after the current time", clock.getTime() <= when);
        return when;
    }

    private static void scheduleDelayedAction_future(final long time, final long delay) {
        assert 0L < delay;
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        final Clock clock = new Clock(timeUnit, time);
        final RunnableSpy action = new RunnableSpy(clock, delay);

        final long when = scheduleDelayedAction(clock, delay, timeUnit, action);

        assertEquals("Scheduled time", clock.getTime() + delay, when);
        action.assertRan(0);
    }

    private static void scheduleDelayedAction_immediate(long time, TimeUnit clockUnit, TimeUnit delayUnit) {
        final Clock clock = new Clock(clockUnit, time);
        final long delay = 0L;
        final RunnableSpy action = new RunnableSpy(clock, time);

        final long when = scheduleDelayedAction(clock, delay, delayUnit, action);

        assertEquals("Scheduled for the current time", time, when);
        action.assertRan(0);
    }

    public static long scheduleDelayedActionSeconds(Clock clock, double delay, Runnable action) {
        final long when = clock.scheduleDelayedActionSeconds(delay, action);

        assertInvariants(clock);
        assertTrue("Scheduled time is at or after the current time", clock.getTime() <= when);
        return when;
    }

    private static void scheduleDelayedActionSeconds_future(final long time, final long delayMilliseconds) {
        assert 0.0 < delayMilliseconds;
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        final Clock clock = new Clock(timeUnit, time);
        final RunnableSpy action = new RunnableSpy(clock, delayMilliseconds);

        final long when = scheduleDelayedActionSeconds(clock, 1.0E-3 * delayMilliseconds, action);

        assertEquals("Scheduled time", clock.getTime() + delayMilliseconds, when);
        action.assertRan(0);
    }

    private static void scheduleDelayedActionSeconds_immediate(long time, TimeUnit clockUnit) {
        final Clock clock = new Clock(clockUnit, time);
        final long delay = 0L;
        final RunnableSpy action = new RunnableSpy(clock, time);

        final long when = scheduleDelayedActionSeconds(clock, delay, action);

        assertEquals("Scheduled for the current time", time, when);
        action.assertRan(0);
    }

    @Test
    public void advance_0() {
        advance(TIME_1, 0L);
    }

    @Test
    public void advance_0AtMax() {
        advance(Long.MAX_VALUE, 0L);
    }

    @Test
    public void advance_1() {
        advance(TIME_1, 1L);
    }

    @Test
    public void advance_1ToMax() {
        advance(Long.MAX_VALUE - 1L, 1L);
    }

    @Test
    public void advance_afterAction() {
        final long time0 = TIME_1;
        final long actionTime = TIME_2;
        final long timeToAdvanceTo2 = TIME_3;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time0);
        final RunnableSpy action = new RunnableSpy(clock, actionTime);
        clock.scheduleActionAt(actionTime, action);
        clock.advance(actionTime - time0);

        advance(clock, timeToAdvanceTo2 - time0);

        /*
         * Check that did not perform the actino twice.
         */
        action.assertRan(1);
    }

    @Test
    public void advance_with1ActionA() {
        final long time0 = TIME_1;
        final long actionTime = TIME_2;
        final long timeToAdvanceTo = TIME_3;
        advance_with1Action(time0, actionTime, timeToAdvanceTo);
    }

    @Test
    public void advance_with1ActionAtEnd() {
        final long time0 = TIME_1;
        final long actionTime = TIME_2;
        final long timeToAdvanceTo = actionTime;// critical
        advance_with1Action(time0, actionTime, timeToAdvanceTo);
    }

    @Test
    public void advance_with1ActionB() {
        final long time0 = TIME_2;
        final long actionTime = TIME_3;
        final long timeToAdvanceTo = TIME_4;
        advance_with1Action(time0, actionTime, timeToAdvanceTo);
    }

    @Test
    public void advance_with2ActionsA() {
        final long time0 = TIME_1;
        final long actionTime1 = TIME_2;
        final long actionTime2 = TIME_3;
        final long timeToAdvanceTo = TIME_4;

        advance_with2Actions(time0, actionTime1, actionTime2, timeToAdvanceTo);
    }

    @Test
    public void advance_with2ActionsAtEnd() {
        final long time0 = TIME_1;
        final long actionTime1 = TIME_2;
        final long actionTime2 = actionTime1;// critical
        final long timeToAdvanceTo = actionTime1;// critical

        advance_with2Actions(time0, actionTime1, actionTime2, timeToAdvanceTo);
    }

    @Test
    public void advance_with2ActionsB() {
        final long time0 = TIME_2;
        final long actionTime1 = TIME_3;
        final long actionTime2 = TIME_4;
        final long timeToAdvanceTo = TIME_5;

        advance_with2Actions(time0, actionTime1, actionTime2, timeToAdvanceTo);
    }

    @Test
    public void advance_with2ActionsSameTime() {
        final long time0 = TIME_1;
        final long actionTime1 = TIME_2;
        final long actionTime2 = actionTime1;// critical
        final long timeToAdvanceTo = TIME_4;

        advance_with2Actions(time0, actionTime1, actionTime2, timeToAdvanceTo);
    }

    @Test
    public void advance_with2ActionsScheduledInReverseOrder() {
        final long time0 = TIME_1;
        final long actionTime1 = TIME_3;
        final long actionTime2 = TIME_2;
        final long timeToAdvanceTo = TIME_4;
        assert actionTime2 < actionTime1;

        advance_with2Actions(time0, actionTime1, actionTime2, timeToAdvanceTo);
    }

    @Test
    public void advance_withLaterActionA() {
        final long time0 = TIME_1;
        final long timeToAdvanceTo = TIME_2;
        final long actionTime = TIME_3;
        advance_withLaterAction(time0, timeToAdvanceTo, actionTime);
    }

    @Test
    public void advance_withLaterActionJust() {
        final long time0 = TIME_1;
        final long timeToAdvanceTo = TIME_2;
        final long actionTime = timeToAdvanceTo + 1L;// critical
        advance_withLaterAction(time0, timeToAdvanceTo, actionTime);
    }

    @Test
    public void advance_withLaterActionNoOp() {
        final long time0 = TIME_1;
        final long timeToAdvanceTo = time0;// critical
        final long actionTime = TIME_2;
        advance_withLaterAction(time0, timeToAdvanceTo, actionTime);
    }

    @Test
    public void advanceSeconds_1d() {
        final long time = TIME_1;
        final double amount = 1.0;
        final long expectedDt = 0L;
        advanceSeconds(TimeUnit.DAYS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_1hr() {
        final long time = TIME_1;
        final double amount = 1.0;
        final long expectedDt = 0L;
        advanceSeconds(TimeUnit.HOURS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_1min() {
        final long time = TIME_1;
        final double amount = 1.0;
        final long expectedDt = 0L;
        advanceSeconds(TimeUnit.MINUTES, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_1ms() {
        final long time = TIME_1;
        final double amount = 1.0;
        final long expectedDt = 1_000L;
        advanceSeconds(TimeUnit.MILLISECONDS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_1ns() {
        final long time = TIME_1;
        final double amount = 1.0;
        final long expectedDt = 1_000_000_000L;
        advanceSeconds(TimeUnit.NANOSECONDS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_1sA() {
        final long time = TIME_1;
        final double amount = 1.0;
        final long expectedDt = 1L;
        advanceSeconds(TimeUnit.SECONDS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_1sB() {
        final long time = TIME_2;
        final double amount = 1.0;
        final long expectedDt = 1L;
        advanceSeconds(TimeUnit.SECONDS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_1us() {
        final long time = TIME_1;
        final double amount = 1.0;
        final long expectedDt = 1_000_000L;
        advanceSeconds(TimeUnit.MICROSECONDS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_2s() {
        final long time = TIME_1;
        final double amount = 2.0;
        final long expectedDt = 2L;
        advanceSeconds(TimeUnit.SECONDS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_tick_day() {
        final long time = TIME_1;
        final double amount = 3_600.0 * 24;
        final long expectedDt = 1L;
        advanceSeconds(TimeUnit.DAYS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_tick_hr() {
        final long time = TIME_1;
        final double amount = 3_600;
        final long expectedDt = 1L;
        advanceSeconds(TimeUnit.HOURS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_tick_min() {
        final long time = TIME_1;
        final double amount = 60;
        final long expectedDt = 1L;
        advanceSeconds(TimeUnit.MINUTES, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_tick_ms() {
        final long time = TIME_1;
        final double amount = 1.0E-3;
        final long expectedDt = 1L;
        advanceSeconds(TimeUnit.MILLISECONDS, time, amount, expectedDt);
    }

    ///////////////////////////

    @Test
    public void advanceSeconds_tick_ns() {
        final long time = TIME_1;
        final double amount = 1.0E-9;
        final long expectedDt = 1L;
        advanceSeconds(TimeUnit.NANOSECONDS, time, amount, expectedDt);
    }

    @Test
    public void advanceSeconds_tick_us() {
        final long time = TIME_1;
        final double amount = 1.0E-6;
        final long expectedDt = 1L;
        advanceSeconds(TimeUnit.MICROSECONDS, time, amount, expectedDt);
    }

    @Test
    public void advanceTo_0() {
        advanceTo(TIME_1, TIME_1);
    }

    @Test
    public void advanceTo_0AtMax() {
        advanceTo(Long.MAX_VALUE, Long.MAX_VALUE);
    }

    @Test
    public void advanceTo_1() {
        advanceTo(TIME_1, TIME_1 + 1L);
    }

    @Test
    public void advanceTo_1ToMax() {
        advanceTo(Long.MAX_VALUE - 1L, Long.MAX_VALUE);
    }

    @Test
    public void advanceTo_afterAction() {
        final long time0 = TIME_1;
        final long actionTime = TIME_2;
        final long when = TIME_3;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time0);
        final RunnableSpy action = new RunnableSpy(clock, actionTime);
        clock.scheduleActionAt(actionTime, action);
        clock.advanceTo(actionTime);

        advanceTo(clock, when);

        /*
         * Check that did not perform the action twice.
         */
        action.assertRan(1);
    }

    @Test
    public void advanceTo_with1ActionA() {
        final long time0 = TIME_1;
        final long actionTime = TIME_2;
        final long when = TIME_3;
        advanceTo_with1Action(time0, actionTime, when);
    }

    @Test
    public void advanceTo_with1ActionAtEnd() {
        final long time0 = TIME_1;
        final long actionTime = TIME_2;
        final long when = actionTime;// critical
        advanceTo_with1Action(time0, actionTime, when);
    }

    @Test
    public void advanceTo_with1ActionB() {
        final long time0 = TIME_2;
        final long actionTime = TIME_3;
        final long when = TIME_4;
        advanceTo_with1Action(time0, actionTime, when);
    }

    @Test
    public void advanceTo_with1DelayedActionDifferentUnit() {
        final long time0 = TIME_1;
        final long delay = 7L;
        final long expectedActionTime = time0 + delay * 1_000L;
        final long when = TIME_3;
        final TimeUnit clockUnit = TimeUnit.MILLISECONDS;
        final TimeUnit delayUnit = TimeUnit.SECONDS;

        advanceTo_with1DelayedAction(time0, expectedActionTime, when, clockUnit, delay, delayUnit);
    }

    @Test
    public void advanceTo_with1DelayedActionSameUnitA() {
        final long time0 = TIME_1;
        final long expectedActionTime = TIME_2;
        final long when = TIME_3;
        final TimeUnit clockUnit = TimeUnit.MILLISECONDS;
        final long delay = expectedActionTime - time0;
        final TimeUnit delayUnit = clockUnit;// critical

        advanceTo_with1DelayedAction(time0, expectedActionTime, when, clockUnit, delay, delayUnit);
    }

    @Test
    public void advanceTo_with1DelayedActionSameUnitB() {
        final long time0 = TIME_2;
        final long expectedActionTime = TIME_3;
        final long when = TIME_4;
        final TimeUnit clockUnit = TimeUnit.MICROSECONDS;
        final long delay = expectedActionTime - time0;
        final TimeUnit delayUnit = clockUnit;// critical

        advanceTo_with1DelayedAction(time0, expectedActionTime, when, clockUnit, delay, delayUnit);
    }

    @Test
    public void advanceTo_with2ActionsA() {
        final long time0 = TIME_1;
        final long actionTime1 = TIME_2;
        final long actionTime2 = TIME_3;
        final long when = TIME_4;

        advanceTo_with2Actions(time0, actionTime1, actionTime2, when);
    }

    @Test
    public void advanceTo_with2ActionsAtEnd() {
        final long time0 = TIME_1;
        final long actionTime1 = TIME_2;
        final long actionTime2 = actionTime1;// critical
        final long when = actionTime1;// critical

        advanceTo_with2Actions(time0, actionTime1, actionTime2, when);
    }

    @Test
    public void advanceTo_with2ActionsB() {
        final long time0 = TIME_2;
        final long actionTime1 = TIME_3;
        final long actionTime2 = TIME_4;
        final long when = TIME_5;

        advanceTo_with2Actions(time0, actionTime1, actionTime2, when);
    }

    @Test
    public void advanceTo_with2ActionsSameTime() {
        final long time0 = TIME_1;
        final long actionTime1 = TIME_2;
        final long actionTime2 = actionTime1;// critical
        final long when = TIME_4;

        advanceTo_with2Actions(time0, actionTime1, actionTime2, when);
    }

    @Test
    public void advanceTo_with2ActionsScheduledInReverseOrder() {
        final long time0 = TIME_1;
        final long actionTime1 = TIME_3;
        final long actionTime2 = TIME_2;
        final long when = TIME_4;
        assert actionTime2 < actionTime1;

        advanceTo_with2Actions(time0, actionTime1, actionTime2, when);
    }

    @Test
    public void advanceTo_withLaterActionA() {
        final long time0 = TIME_1;
        final long when = TIME_2;
        final long actionTime = TIME_3;
        advanceTo_withLaterAction(time0, when, actionTime);
    }

    @Test
    public void advanceTo_withLaterActionJust() {
        final long time0 = TIME_1;
        final long when = TIME_2;
        final long actionTime = when + 1L;// critical
        advanceTo_withLaterAction(time0, when, actionTime);
    }

    @Test
    public void advanceTo_withLaterActionNoOp() {
        final long time0 = TIME_1;
        final long when = time0;// critical
        final long actionTime = TIME_2;
        advanceTo_withLaterAction(time0, when, actionTime);
    }

    @Test
    public void constructor_A() {
        constructor(TimeUnit.MILLISECONDS, TIME_1);
    }

    @Test
    public void constructor_B() {
        constructor(TimeUnit.MICROSECONDS, TIME_2);
    }

    @Test
    public void scheduleActionAt_futureA() {
        scheduleActionAt_future(TIME_1, TIME_1 + 1L);
    }

    @Test
    public void scheduleActionAt_futureB() {
        scheduleActionAt_future(TIME_2, Long.MAX_VALUE);
    }

    @Test
    public void scheduleActionAt_immediateA() {
        scheduleActionAt_immediate(TIME_1);
    }

    @Test
    public void scheduleActionAt_immediateB() {
        scheduleActionAt_immediate(TIME_2);
    }

    @Test
    public void scheduleDelayedAction_futureA() {
        scheduleDelayedAction_future(TIME_1, 1L);
    }

    @Test
    public void scheduleDelayedAction_futureB() {
        scheduleDelayedAction_future(TIME_2, 1_000L);
    }

    @Test
    public void scheduleDelayedAction_immediateA() {
        scheduleDelayedAction_immediate(TIME_1, TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS);
    }

    @Test
    public void scheduleDelayedAction_immediateB() {
        scheduleDelayedAction_immediate(TIME_2, TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS);
    }

    @Test
    public void scheduleDelayedActionSeconds_futureA() {
        scheduleDelayedActionSeconds_future(TIME_1, 1L);
    }

    @Test
    public void scheduleDelayedActionSeconds_futureB() {
        scheduleDelayedActionSeconds_future(TIME_2, 1_000L);
    }

    @Test
    public void scheduleDelayedActionSeconds_immediateA() {
        scheduleDelayedActionSeconds_immediate(TIME_1, TimeUnit.MILLISECONDS);
    }

    @Test
    public void scheduleDelayedActionSeconds_immediateB() {
        scheduleDelayedActionSeconds_immediate(TIME_2, TimeUnit.MICROSECONDS);
    }
}
