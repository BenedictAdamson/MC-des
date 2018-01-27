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

    /**
     * <p>
     * An exception for indicating that the {@linkplain Runnable#run() action} of a
     * {@linkplain Clock#scheduleActionAt(long, Runnable) scheduled action} threw a
     * {@link RuntimeException}.
     * </p>
     */
    public static final class ActionException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private ActionException(RuntimeException cause) {
            super("Exception thrown by scheduled action", cause);
        }

        /**
         * <p>
         * The {@link RuntimeException} that the {@linkplain Runnable#run() action} of
         * the {@linkplain Clock#scheduleActionAt(long, Runnable) scheduled action}
         * threw.
         * </p>
         * 
         * @return the cause, or null if the cause is unknown.
         */
        @Override
        public final RuntimeException getCause() {
            return (RuntimeException) super.getCause();
        }

    }// class

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
     * {@linkplain #scheduleActionAt(long, Runnable) scheduled} for points in time
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
     *             a {@linkplain #scheduleActionAt(long, Runnable) scheduled
     *             action}.
     * @throws ActionException
     *             If the {@link Runnable#run()} method of a
     *             {@linkplain #scheduleActionAt(long, Runnable) scheduled action}
     *             threw a {@link RuntimeException}.
     */
    public final void advance(long amount) {
        if (Long.MAX_VALUE - amount < time) {
            throw new TimeOverflowException();
        }
        advanceTo(time + amount);
    }

    /**
     * <p>
     * Advance the {@linkplain #getTime() time} of this clock to a given time.
     * </p>
     * <ul>
     * <li>The method advances the time to the given new time.
     * <li>However, if any actions had been
     * {@linkplain #scheduleActionAt(long, Runnable) scheduled} for points in time
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
     *             a {@linkplain #scheduleActionAt(long, Runnable) scheduled
     *             action}.
     * @throws ActionException
     *             If the {@link Runnable#run()} method of a
     *             {@linkplain #scheduleActionAt(long, Runnable) scheduled action}
     *             threw a {@link RuntimeException}.
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
            try {
                currentScheduledAction.action.run();
            } catch (RuntimeException e) {
                throw new ActionException(e);
            } finally {
                currentScheduledAction = null;
            }
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
     *            The point in time when the action should be performed, measured in
     *            the {@linkplain #getUnit() unit} of this clock.
     * @param action
     *            The action to perform. The the action must not (directly or
     *            indirectly) try to {@linkplain #advance(long) advance} this clock.
     * @throws NullPointerException
     *             If {@code action} is null
     * @throws IllegalArgumentException
     *             If {@code when} is before the {@linkplain #getTime() current
     *             time}.
     */
    public final void scheduleActionAt(long when, Runnable action) {
        Objects.requireNonNull(action, "action");
        if (when < time) {
            throw new IllegalArgumentException("when <" + when + "> is before now <" + time + ">");
        }

        scheduledActions.add(new ScheduledAction(when, action));
    }

    /**
     * <p>
     * Schedule that a given action should be performed at a {@linkplain #getTime()
     * time} in the future, with that time specified by by the duration from the
     * current time to that time.
     * </p>
     * <p>
     * The clock records the action for future use when the clock is
     * {@linkplain #advance(long) advanced} to (or through) the point in time when
     * the action is to be performed. The clock guarantees that the clock time is
     * that point in time when it performs the action.
     * </p>
     * 
     * @param delay
     *            The magnitude of the delay before the action should be performed,
     *            measured in the given delay unit.
     * @param delayUnit
     *            The time unit in which the delay is measured.
     * @param action
     *            The action to perform. The the action must not (directly or
     *            indirectly) try to {@linkplain #advance(long) advance} this clock.
     * @return the clock {@linkplain #getTime() time} at which the action will be
     *         performed. At or after teh current time.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code delayUnit} is null</li>
     *             <li>If {@code action} is null</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If {@code delay} is negative.
     * @throws TimeOverflowException
     *             <ul>
     *             <li>If the {@code delay} indicates a delay in the
     *             {@linkplain #getUnit() units} of this clock that is larger than
     *             {@link Long#MAX_VALUE}.</li>
     *             <li>If the {@code delay} indicates a point in time with a
     *             {@linkplain #getTime() time} value larger than
     *             {@link Long#MAX_VALUE}.</li>
     *             </ul>
     */
    public final long scheduleDelayedAction(long delay, TimeUnit delayUnit, Runnable action) {
        Objects.requireNonNull(delayUnit, "delayUnit");
        if (delay < 0) {
            throw new IllegalArgumentException("delay <" + delay + ">");
        } else if (delay == Long.MAX_VALUE) {
            throw new TimeOverflowException();
        }
        final long convertedDelay = unit.convert(delay, delayUnit);
        if (convertedDelay == Long.MAX_VALUE || Long.MAX_VALUE - convertedDelay < time) {
            throw new TimeOverflowException();
        }
        final long when = time + convertedDelay;
        scheduleActionAt(when, action);
        return when;
    }

}
