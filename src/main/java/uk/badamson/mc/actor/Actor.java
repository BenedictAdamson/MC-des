package uk.badamson.mc.actor;

import uk.badamson.mc.actor.message.Message;

/**
 * <p>
 * The interface through which the simulation interacts with a human or AI
 * player of the Mission Command game.
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
     *             If {@code receptionStarted} is null.
     * @throws IllegalArgumentException
     *             If the {@linkplain MessageTransferInProgress#getMessageSofar()
     *             message received so far} of the {@code receptionStarted} is not
     *             null, which would indicate that reception actually started some
     *             time ago.
     */
    public void tellBeginReceivingMessage(MessageTransferInProgress receptionStarted);

    /**
     * <p>
     * React to the reception of more of a message that is being transmitted to the
     * actor.
     * </p>
     * 
     * @param previousMessageSoFar
     *            The amount of the message that had been received the previous time
     *            the actor was told about reception of the message. This may be
     *            null, which indicates that this is the first call to indicate
     *            progress, and the previous call about the
     *            {@linkplain MessageTransferInProgress#getMedium() medium} of the
     *            message was a call to indicate that
     *            {@linkplain #tellBeginReceivingMessage(MessageTransferInProgress)
     *            receiving the message began}.
     * @param messageBeingReceived
     *            The current progress of reception of the message.
     * 
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code messageBeingReceived} is null.</li>
     *             <li>If the
     *             {@linkplain MessageTransferInProgress#getMessageSofar() message
     *             received so far} of the {@code messageBeingReceived} is
     *             null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If the {@code previousMessageSoFar} is non null and the
     *             {@linkplain Message#getInformationContent() length (information
     *             content)} of the message received so far of the
     *             {@code messageBeingReceived} is not greater than the length of
     *             the {@code previousMessageSoFar}.
     */
    public void tellMessageReceptionProgress(Message previousMessageSoFar,
            MessageTransferInProgress messageBeingReceived);

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
     *            sent so far} of the transmission progress is the actual message
     *            sent. This will be the same as the full message if sending of the
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
     *             <li>If the
     *             {@linkplain MessageTransferInProgress#getMessageSofar() message
     *             sent so far} of the {@code transmissionProgress} is not null and
     *             its {@linkplain Message#getInformationContent() length
     *             (information content)} exceeds the length of the
     *             {@code fullMessage}.</li>
     *             <li>If the message sent so far of the
     *             {@code transmissionProgress} is not null and the
     *             {@linkplain Message#getInformationContent() information content}
     *             (length) of that message equals the information content of the
     *             {@code fullMessage}, but that message sent so far is not the same
     *             as the {@code fullMessage}.</li>
     *             </ul>
     */
    public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage);

    /**
     * <p>
     * React to transmission of more of the message that is the actor is
     * transmitting.
     * </p>
     * <p>
     * The method can query the {@linkplain #getActorInterface() actor interface}
     * for the {@linkplain ActorInterface#getTransmissionInProgress() current state
     * of the transmission in progress}.
     * </p>
     * 
     * @param transmissionProgress
     *            The current state of the transmission.
     * @param fullMessage
     *            The message is being sent.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code transmissionProgress} is null.</li>
     *             <li>If {@code fullMessage} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If the {@linkplain MessageTransferInProgress#getMessageSofar()
     *             message sent so far} of the {@code transmissionProgress} is not
     *             null and its {@linkplain Message#getInformationContent() length
     *             (information content)} equals or exceeds the length of the
     *             {@code fullMessage}.</li>
     */
    public void tellMessageTransmissionProgress(MessageTransferInProgress transmissionProgress, Message fullMessage);
}
