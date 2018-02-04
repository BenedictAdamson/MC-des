
package uk.badamson.mc.actor;

import java.util.Collections;
import java.util.Set;

import uk.badamson.mc.actor.medium.Medium;
import uk.badamson.mc.actor.message.Command;
import uk.badamson.mc.actor.message.IllegalMessageException;
import uk.badamson.mc.actor.message.Message;
import uk.badamson.mc.actor.message.UnusableIncompleteMessage;

/**
 * <p>
 * The API (service interface) through which a human or AI player ("actor") of
 * the Mission Command game effect changes to the simulation of a person.
 * </p>
 */
public interface ActorInterface {

    /**
     * <p>
     * Indicate that the person begins sending a message through a given
     * transmission medium.
     * </p>
     * <p>
     * The message may be a {@linkplain Command command}, and the medium may enable
     * the intended recipient of that command to receive the command. This method is
     * therefore the means by which actors can issue orders to sub-ordinates.
     * </p>
     * 
     * <section>
     * <h1>Post Conditions</h1>
     * <ul>
     * <li>This has a (non null) {@linkplain #getTransmissionInProgress()
     * transmission in progress}.</li>
     * <li>The {@linkplain MessageTransferInProgress#getMedium() medium} of the
     * transmission in progress is the given medium.</li>
     * <li>The {@linkplain MessageTransferInProgress#getMessageSofar() message
     * transmitted so far} is an {@linkplain UnusableIncompleteMessage#EMPTY_MESSAGE
     * empty unusable message}.</li>
     * <li>The given message is the current {@linkplain #getTransmittingMessage()
     * transmitting message}.</li>
     * <li>The simulation guarantees that, if it runs for sufficiently long, it will
     * eventually {@linkplain Actor#tellMessageSendingEnded(Medium, Message) call
     * back} to the person of this interface, to report completion or halting of
     * sending of the message.</li>
     * <li>Other actors that can receive through the medium of the message had an
     * additional entry in their {@linkplain #getMessagesBeingReceived() set of
     * messages being received}, representing the start of them receiving the
     * message.</li>
     * </ul>
     * </section>
     * 
     * @param medium
     *            The transmission medium (or means) through which to send the
     *            message.
     * @param message
     *            The message to begin sending.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code medium} is null.</li>
     *             <li>If {@code message} is null.</li>
     *             </ul>
     * @throws IllegalMessageException
     *             If sending the {@code message} through the {@code medium} is not
     *             permitted because the message is of the incorrect class or type,
     *             or has incorrect characteristics, to be sent through that medium.
     * @throws IllegalStateException
     *             If this is already {@linkplain #getTransmittingMessage() sending
     *             a message}.
     * @throws MediumUnavailableException
     *             If the {@code medium} is not one of the {@linkplain #getMedia()
     *             currently available media}.
     */
    public void beginSendingMessage(Medium medium, Message message) throws MediumUnavailableException;

    /**
     * <p>
     * The current set of transmission media (or means) through which the person can
     * send {@linkplain Message messages}.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of media.</li>
     * <li>The set of media does not contain a null element.</li>
     * <li>The set of media may change as means of communication become available
     * and cease to be available.
     * <li>The set of media is {@linkplain Collections#unmodifiableSet(Set)
     * unmodifiable}.</li>
     * </ul>
     * 
     * @return the media.
     */
    public Set<Medium> getMedia();

    /**
     * <p>
     * The set of messages that the person is currently receiving through its
     * {@linkplain #getMedia() communication media}.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of messages being received.</li>
     * <li>The set of messages being received does not contain a null element.</li>
     * <li>The set of messages being received is
     * {@linkplain Collections#unmodifiableSet(Set) unmodifiable}.</li>
     * <li>The {@linkplain MessageTransferInProgress#getMedium() medium} of each
     * message being received is one of the communication media of the actor.</li>
     * </ul>
     * 
     * @return the messages currently being received.
     */
    public Set<MessageTransferInProgress> getMessagesBeingReceived();

    /**
     * <p>
     * Information about the progress of the current transmission that the person is
     * making.
     * </p>
     * <ul>
     * <li>A null transmission in progress indicates that the person is not
     * currently transmitting.</li>
     * <li>This has a transmission in progress if, and only if, this has a
     * {@linkplain transmitting message}.</li>
     * <li>If there is a (non null) transmission in progress, its
     * {@linkplain MessageTransferInProgress#getMedium() medium} is one of the
     * {@linkplain #getMedia() media} that the actor can use.</li>
     * <li>If there is a (non null) transmission in progress, the
     * {@linkplain Message#getInformationContent() length (information content)} of
     * the {@linkplain MessageTransferInProgress#getMessageSofar() message sent so
     * far} is less than or equal to the length of the
     * {@linkplain #getTransmittingMessage() message being sent}.</li>
     * <li>If there is a (non null) transmission in progress, and the length of the
     * message sent so far equals the length of the message being sent, the message
     * sent so far is the same as the message being sent.</li>
     * </ul>
     * <p>
     * The simulation should arrange that, while the actor has a transmission in
     * progress, the message sent so far continually changes, such that its length
     * continually increases at a rate similar to the
     * {@linkplain Medium#getTypicalTransmissionRate() typical transmission rate} of
     * the {@linkplain MessageTransferInProgress#getMedium() transmitting medium}.
     * </p>
     * </ul>
     * 
     * @return the transmission currently in progress.
     */
    public MessageTransferInProgress getTransmissionInProgress();

    /**
     * <p>
     * The message that the person is currently transmitting (sending).
     * </p>
     * <ul>
     * <li>A null value indicates that the actor is not currently sending a
     * message.</li>
     * </ul>
     * 
     * @return the medium
     */
    public Message getTransmittingMessage();

    /**
     * <p>
     * Indicate that the person ceases sending a message, before completion of
     * sending of the message.
     * </p>
     * 
     * <section>
     * <h1>Post Conditions</h1>
     * <ul>
     * <li>This has no {@linkplain #getTransmissionInProgress() transmission in
     * progress}.</li>
     * </ul>
     * </section>
     * 
     * @throws IllegalStateException
     *             If this has no {@linkplain #getTransmissionInProgress()
     *             transmission in progress}.
     */
    public void haltSendingMessage();
}
