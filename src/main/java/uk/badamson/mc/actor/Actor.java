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
     * React to the start of a message being received.
     * </p>
     * 
     * @param receptionStarted
     *            The message transfer that has just started.
     * 
     * @throws NullPointerException
     *             If {@code messageBeingReceived} is null.
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code messageBeingReceived} is not one of the
     *             {@linkplain ActorInterface#getMessagesBeingReceived() messages
     *             being received} according to the {@linkplain #getActorInterface()
     *             actor interface} of this actor.</li>
     *             <li>If the
     *             {@linkplain MessageTransferInProgress#getMessageSofar() message
     *             received so far} of the reception start is non null.</li>
     *             </ul>
     */
    public void tellBeginReceivingMessage(MessageTransferInProgress receptionStarted);

    /**
     * <p>
     * React to the ending of sending a message.
     * </p>
     * <p>
     * This method may be executed when sending of a message completes, or when
     * sending is halted for other reasons.
     * </p>
     * 
     * @param transmissionProgress
     *            The state of the transmission when sending ended. The
     *            {@linkplain MessageTransferInProgress#getMessageSofar() message
     *            sent so far} of the transmission progress is actual message sent.
     *            This will be the same as the full message if sending of the
     *            message was completed.
     * @param fullMessage
     *            The message that was wanted to be sent.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code transmissionProgress} is null.</li>
     *             <li>If {@code fullMessage} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If the {@linkplain Message#getLength() length} of the
     *             {@linkplain MessageTransferInProgress#getMessageSofar() message
     *             sent so far} of the {@code transmissionProgress} exceeds the
     *             length of the {@code fullMessage}.</li>
     *             <li>If the length of the message sent so far of the
     *             {@code messageSent} equals the length of the {@code fullMessage},
     *             but that message sent so far is not the same as the
     *             {@code fullMessage}.
     *             </ul>
     * @throws IllegalStateException
     *             If the {@linkplain ActorInterface#getTransmittingMessage()
     *             currently sending message} of the
     *             {@linkplain #getActorInterface() actor interface} of this actor
     *             is not null.
     */
    public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage);
}
