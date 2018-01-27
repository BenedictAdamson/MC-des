package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the {@link Clock} class.
 * </p>
 */
public class ClockTest {

    private static final long TIME_1 = 1_000L;
    private static final long TIME_2 = -123L;

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

    private static void scheduleAction_future(long time, long when) {
        assert time < when;
        final Clock clock = new Clock(TimeUnit.MILLISECONDS, time);
        AtomicBoolean acted = new AtomicBoolean(false);
        final Runnable action = new Runnable() {

            @Override
            public final void run() {
                acted.set(true);
            }
        };

        scheduleAction(clock, when, action);

        assertFalse("Did not perform the action", acted.get());
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
}
