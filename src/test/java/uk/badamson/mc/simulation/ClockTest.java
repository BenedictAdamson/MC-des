package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

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

    private static final long TIME_1 = -123L;
    private static final long TIME_2 = 1_000L;
    private static final long TIME_3 = 7_000L;
    private static final long TIME_4 = 60_000L;
    private static final long TIME_5 = 3_600_000L;

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
        clock.scheduleAction(actionTime, action);

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
        clock.scheduleAction(actionTime1, action1);
        clock.scheduleAction(actionTime2, action2);

        advance(clock, timeToAdvanceTo - time0);

        action1.assertRan(1);
        action2.assertRan(1);
    }

    private static void advance_withLaterAction(long time0, long timeToAdvanceTo, long actionTime) {
        assert time0 <= timeToAdvanceTo;
        assert timeToAdvanceTo < actionTime;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time0);
        final RunnableSpy action = new RunnableSpy(clock, actionTime);
        clock.scheduleAction(actionTime, action);

        advance(clock, timeToAdvanceTo - time0);

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

    public static void scheduleAction(Clock clock, long when, Runnable action) {
        clock.scheduleAction(when, action);

        assertInvariants(clock);
    }

    private static void scheduleAction_future(final long time, final long when) {
        assert time < when;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time);
        final RunnableSpy action = new RunnableSpy(clock, when);

        scheduleAction(clock, when, action);

        action.assertRan(0);
    }

    private static void scheduleAction_immediate(long time) {
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time);
        final RunnableSpy action = new RunnableSpy(clock, time);

        scheduleAction(clock, time, action);

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
        clock.scheduleAction(actionTime, action);
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
    public void constructor_A() {
        constructor(TimeUnit.MILLISECONDS, TIME_1);
    }

    @Test
    public void constructor_B() {
        constructor(TimeUnit.MICROSECONDS, TIME_2);
    }

    @Test
    public void scheduleAction_futureA() {
        scheduleAction_future(TIME_1, TIME_1 + 1L);
    }

    @Test
    public void scheduleAction_futureB() {
        scheduleAction_future(TIME_2, Long.MAX_VALUE);
    }

    @Test
    public void scheduleAction_immediateA() {
        scheduleAction_immediate(TIME_1);
    }

    @Test
    public void scheduleAction_immediateB() {
        scheduleAction_immediate(TIME_2);
    }
}
