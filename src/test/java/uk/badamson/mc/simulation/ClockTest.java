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
}
