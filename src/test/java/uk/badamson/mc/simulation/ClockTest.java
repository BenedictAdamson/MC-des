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

    private static final long TIME_1 = 1_000L;
    private static final long TIME_2 = -123L;

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

    @Test
    public void constructor_A() {
        constructor(TimeUnit.MILLISECONDS, TIME_1);
    }

    @Test
    public void constructor_B() {
        constructor(TimeUnit.MICROSECONDS, TIME_2);
    }

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
}
