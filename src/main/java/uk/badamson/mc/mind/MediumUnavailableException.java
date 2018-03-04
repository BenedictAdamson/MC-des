package uk.badamson.mc.mind;

import uk.badamson.mc.mind.medium.Medium;

/**
 * <p>
 * A {@linkplain Exception checked exception} for indicating an
 * {@linkplain Actor actor} is unable to use a {@linkplain Medium communication
 * medium} it is trying to {@linkplain ActorInterface use}, because of the
 * <em>currently</em> state of the simulation.
 * <p>
 * That is, using the medium is physically impossible, but using the medium is
 * sometimes possible. This is not an {@link IllegalStateException}, (which is a
 * {@link RuntimeException}) because it can be practically impossible, or
 * inconvenient, to determine whether using a medium is possiblem other than
 * trying to use it. This is a checked exception because an {@linkplain Actor
 * actor} must always be prepared to handle attempted actions that become
 * unavailable without notice.
 * </p>
 * <p>
 */
public final class MediumUnavailableException extends ImpossibleActionException {

    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * Construct an exception object for indicating that a {@linkplain Medium
     * communication medium} currently can not be used.
     * </p>
     */
    public MediumUnavailableException() {
        super("Medium unavailable");
    }

    /**
     * <p>
     * Construct an exception object for indicating that a {@linkplain Medium
     * communication medium} currently can not be used. , which is due to an
     * underlying {@linkplain #getCause() cause}.
     * </p>
     * 
     * @param cause
     *            The cause of the exception.
     */
    public MediumUnavailableException(Throwable cause) {
        super("Medium unavailable", cause);
    }

}
