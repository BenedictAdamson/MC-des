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
 */
public final class Clock {

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
