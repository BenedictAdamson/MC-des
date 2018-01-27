package uk.badamson.mc.simulation;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * A simulation clock.
 * </p>
 * <p>
 * There is one clock object per simulated world. All objects that, directly or
 * indirectly, use a particular clock object are considered as belonging to the
 * same simulated world.
 * </p>
 * <p>
 * A simulation clock counts {@linkplain #getTime() time} in whole numbers of
 * time {@linkplain #getUnit() units}.
 * </p>
 * <p>
 * When using a simulation clock, the time unit and the initial time value
 * should be chosen so the time value will not
 * {@linkplain Clock.TimeOverflowException overflow} during the simulation.
 * </p>
 */
public final class Clock {

    /**
     * <p>
     * An exception for indicating that the {@linkplain Clock#getTime() time} of a
     * {@linkplain Clock clock} can not be advanced by a desired amount, because
     * doing so would cause the time value to overflow.
     * </p>
     */
    public static final class TimeOverflowException extends IllegalStateException {

        private static final long serialVersionUID = 1L;

        public TimeOverflowException() {
            super("Clock time would overflow");
        }
    }// class

    private final TimeUnit unit;
    private long time;

    /**
     * <p>
     * Construct a clock with given attribute values.
     * </p>
     * 
     * @param unit
     *            The granularity with which this clock measures the progress of
     *            time.
     * @param time
     *            The current time, according to this clock, measured in the time
     *            unit of this clock.
     */
    public Clock(TimeUnit unit, long time) {
        this.unit = unit;
        this.time = time;
    }

    /**
     * <p>
     * Advance (increment) the {@linkplain #getTime() time} of this clock by a given
     * amount
     * </p>
     * 
     * @param amount
     *            The amount of time, in {@linkplain #getUnit() units} of this
     *            clock, by which to advance this clock.
     * @throws IllegalArgumentException
     *             If {@code amount} is negative.
     * @throws TimeOverflowException
     *             If incrementing the time by the {@code amount} would cause the
     *             time to overflow. That is, if recording the new time would
     *             require a time value larger than {@link Long#MAX_VALUE}.
     */
    public final void advance(long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("amount " + amount);
        }
        if (Long.MAX_VALUE - amount < time) {
            throw new TimeOverflowException();
        }
        time += amount;
    }

    /**
     * <p>
     * The current time, according to this clock, measured in the time
     * {@linkplain #getUnit() unit} of this clock.
     * </p>
     * 
     * @return the time
     */
    public final long getTime() {
        return time;
    }

    /**
     * <p>
     * The granularity with which this clock measures the progress of time.
     * </p>
     * <p>
     * The duration of one &ldquo;tick&rdquo; of this clock.
     * </p>
     * 
     * @return the unit; not null.
     */
    public final TimeUnit getUnit() {
        return unit;
    }

}
