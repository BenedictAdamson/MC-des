package uk.badamson.mc.actor;

/**
 * <p>
 * A {@linkplain Exception checked exception} for indicating that an
 * {@linkplain ActorInterface action} that an {@linkplain Actor actor} wants to
 * perform is impossible to perform because of the (current) state of the
 * simulation.
 * <p>
 * That is, the simulation indicates that the action is <em>currently</em>
 * physically impossible, but the action is sometimes possible. This is similar
 * to an {@link IllegalStateException}, but this is not an
 * {@link IllegalStateException}, (which is a {@link RuntimeException}) because
 * it can be practically impossible, or inconvenient, to determine whether an
 * action is possible other than trying to do it. This is a checked exception
 * because an {@linkplain Actor actor} must always be prepared to handle
 * attempted actions that fail.
 * </p>
 * <p>
 */
public class ImpossibleActionException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * Construct an exception object for indicating an impossible action.
     * </p>
     */
    public ImpossibleActionException() {
        super("Impossible action");
    }

    /**
     * <p>
     * Construct an exception object for indicating an impossible action, with a
     * given {@linkplain #getMessage() detail message}.
     * </p>
     * 
     * @param message
     *            The detail message
     */
    public ImpossibleActionException(String message) {
        super(message);
    }

    /**
     * <p>
     * Construct an exception object for indicating an impossible action, with a
     * given {@linkplain #getMessage() detail message} and underlying
     * {@linkplain #getCause() cause}.
     * </p>
     * 
     * @param message
     *            The detail message
     * @param cause
     *            The cause of the exception.
     */
    public ImpossibleActionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * <p>
     * Construct an exception object for indicating an impossible action, which has
     * an underlying {@linkplain #getCause() cause}.
     * </p>
     * 
     * @param cause
     *            The cause of the exception.
     */
    public ImpossibleActionException(Throwable cause) {
        super("Impossible action", cause);
    }

}
