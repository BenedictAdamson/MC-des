package uk.badamson.mc.actor;

/**
 * <p>
 * A human or AI player of the Mission Command game.
 * </p>
 */
public interface Actor {

    /**
     * <p>
     * The API (service interface) through which this actor effects changes to the
     * simulation.
     * </p>
     * <ul>
     * <li>Always have an (non null) actor interface.</li>
     * <li>This is the {@linkplain ActorInterface#getActor() actor} of the actor
     * interface of this actor.</li>
     * </ul>
     * 
     * @return the actor interface; not null.
     */
    public ActorInterface getActorInterface();

    /**
     * <p>
     * React to the ending of sending a message.
     * </p>
     * <p>
     * This method may be executed when sending of a message completes, or when
     * sending is halted for other reasons.
     * </p>
     * 
     * @param medium
     *            The transmission medium (or means) through which the message was
     *            being sent.
     * @param message
     *            The message that was being sent.
     * @param amountSent
     *            How much of the message had been sent through the transmission
     *            medium when sending ended, measured in bits of information. This
     *            will be less than the {@linkplain Message#getLength() length} of
     *            the message if, and only if, sending was halted before it could be
     *            completed.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code medium} is null.</li>
     *             <li>If {@code message} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code amountSent} is negative.</li>
     *             <li>If the {@code amountSent} exceeds the
     *             {@linkplain Message#getLength() length} of the
     *             {@code message}.</li>
     *             </ul>
     * @throws IllegalStateException
     *             If the {@linkplain ActorInterface#getSendingMessage() currently
     *             sending message} of the {@linkplain #getActorInterface() actor
     *             interface} of this actor is not null.
     */
    public void tellMessageSendingEnded(Medium medium, Message message, double amountSent);
}
