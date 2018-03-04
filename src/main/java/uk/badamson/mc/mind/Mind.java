package uk.badamson.mc.mind;

import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.Message;
import uk.badamson.mc.simulation.Person;

/**
 * <p>
 * The interface through which the simulation interacts with the human or AI
 * intelligence that controls a {@link Person}.
 * </p>
 */
public interface Mind {

    /**
     * <p>
     * React to the start of a message being received.
     * </p>
     * 
     * @param medium
     *            The medium through which the message transfer that has just
     *            started.
     * 
     * @throws NullPointerException
     *             If {@code medium} is null.
     */
    public void tellBeginReceivingMessage(Medium medium);

    /**
     * <p>
     * React to the reception of more of a message that is being transmitted to the
     * actor.
     * </p>
     * 
     * @param messageBeingReceived
     *            The current progress of reception of the message.
     * @throws NullPointerException
     *             If {@code messageBeingReceived} is null.
     */
    public void tellMessageReceptionProgress(MessageTransferInProgress messageBeingReceived);

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
