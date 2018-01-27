package uk.badamson.mc.simulation;

import java.util.Objects;
import java.util.PriorityQueue;
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

    private final class ScheduledAction implements Comparable<ScheduledAction> {
        final long when;
        final Runnable action;

        private ScheduledAction(long when, Runnable action) {
            this.when = when;
            this.action = action;
        }

        @Override
        public int compareTo(ScheduledAction that) {
            return Long.compare(when, that.when);
        }

    }// class

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
    private final PriorityQueue<ScheduledAction> scheduledActions = new PriorityQueue<>();

    private long time;
    private ScheduledAction currentScheduledAction;

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
     * <ul>
     * <li>The method advances the time to the new time implied by the given amount.
     * <li>However, if any actions had been
     * {@linkplain #scheduleAction(long, Runnable) scheduled} for points in time
     * before that new time, the method first advances the clock to those scheduled
     * times, in ascending time order, {@linkplain Runnable#run() performing} each
     * of those actions at their scheduled time.</li>
     * </ul>
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
     * @throws IllegalStateException
     *             If the method is called from the {@link Runnable#run()} method of
     *             a {@linkplain #scheduleAction(long, Runnable) scheduled action}.
     */
    public final void advance(long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("amount " + amount);
        }
        if (Long.MAX_VALUE - amount < time) {
            throw new TimeOverflowException();
        }
        if (currentScheduledAction != null) {
            throw new IllegalStateException("Called from the run method of a scheduled action");
        }
        final long newTime = time + amount;
        while (!scheduledActions.isEmpty() && scheduledActions.peek().when <= newTime) {
            currentScheduledAction = scheduledActions.poll();
            time = currentScheduledAction.when;
            currentScheduledAction.action.run();// may throw
            currentScheduledAction = null;
        }
        time = newTime;
    }

    /**
     * <p>
     * Advance the {@linkplain #getTime() time} of this clock to a given time.
     * </p>
     * <ul>
     * <li>The method advances the time to the given new time.
     * <li>However, if any actions had been
     * {@linkplain #scheduleAction(long, Runnable) scheduled} for points in time
     * before that new time, the method first advances the clock to those scheduled
     * times, in ascending time order, {@linkplain Runnable#run() performing} each
     * of those actions at their scheduled time.</li>
     * </ul>
     * 
     * @param when
     *            The new time, in {@linkplain #getUnit() units} of this clock.
     * @throws IllegalArgumentException
     *             If {@code when} is before the {@linkplain #getTime() current
     *             time}.
     * @throws IllegalStateException
     *             If the method is called from the {@link Runnable#run()} method of
     *             a {@linkplain #scheduleAction(long, Runnable) scheduled action}.
     */
    public final void advanceTo(long when) {
        if (when < time) {
            throw new IllegalArgumentException("when " + when + " is before now " + time);
        }
        if (currentScheduledAction != null) {
            throw new IllegalStateException("Called from the run method of a scheduled action");
        }
        while (!scheduledActions.isEmpty() && scheduledActions.peek().when <= when) {
            currentScheduledAction = scheduledActions.poll();
            time = currentScheduledAction.when;
            currentScheduledAction.action.run();// may throw
            currentScheduledAction = null;
        }
        time = when;
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

    /**
     * <p>
     * Schedule that a given action should be performed when the
     * {@linkplain #getTime() time} of this clock reaches a given point in time.
     * </p>
     * <p>
     * The clock records the action for future use when the clock is
     * {@linkplain #advance(long) advanced} to (or through) the point in time when
     * the action is to be performed. The clock guarantees that the clock time is
     * that point in time when it performs the action.
     * </p>
     * 
     * @param when
     *            The point in time when the action should be performed
     * @param action
     *            The action to perform. The the action must not (directly or
     *            indirectly) try to {@linkplain #advance(long) advance} this clock.
     * @throws NullPointerException
     *             If {@code action} is null
     * @throws IllegalArgumentException
     *             If {@code when} is before the {@linkplain #getTime() current
     *             time}.
     */
    public final void scheduleAction(long when, Runnable action) {
        Objects.requireNonNull(action, "action");
        if (when < time) {
            throw new IllegalArgumentException("when <" + when + "> is before now <" + time + ">");
        }

        scheduledActions.add(new ScheduledAction(when, action));
    }

}
