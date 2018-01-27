package uk.badamson.mc.actor.message;

import uk.badamson.mc.actor.ActorInterface;
import uk.badamson.mc.actor.medium.Medium;

/**
 * <p>
 * An (unchecked) exception for indicating that
 * {@linkplain ActorInterface#beginSendingMessage(Message) beginning to send} a
 * particular {@linkplain Message message} through a {@linkplain Medium medium}
 * is not permitted because the message is of the incorrect class or type, or
 * has incorrect characteristics, to be sent through that medium.
 * </p>
 * <p>
 * Some media are not general purpose; they can not be used to send all kinds of
 * message.
 * </p>
 */
public final class IllegalMessageException extends IllegalArgumentException {

    private static final String MESSAGE = "Invalid message type for medium";
    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * Create an exception object for indicating that
     * {@linkplain ActorInterface#beginSendingMessage(Message) beginning to send} a
     * particular {@linkplain Message message} through a {@linkplain Medium medium}
     * is not permitted because the message is of the incorrect class or type, or
     * has incorrect characteristics, to be sent through that medium.
     * </p>
     */
    public IllegalMessageException() {
        super(MESSAGE);
    }

    /**
     * <p>
     * Create an exception object for indicating that
     * {@linkplain ActorInterface#beginSendingMessage(Message) beginning to send} a
     * particular {@linkplain Message message} through a {@linkplain Medium medium}
     * is not permitted because the message is of the incorrect class or type, or
     * has incorrect characteristics, to be sent through that medium, with an
     * underlying cause of that restriction being detected.
     * </p>
     * 
     * @param cause
     *            The underlying cause that indicated that the message is
     *            inappropriate.
     *            </p>
     */
    public IllegalMessageException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
