
package uk.badamson.mc.actor;

import java.util.Collections;
import java.util.Set;

/**
 * <p>
 * The API (service interface) through which a human or AI players ("actors") of
 * the Mission Command game effect changes to the simulation.
 * </p>
 */
public interface ActorInterface {

    /**
     * <p>
     * Indicate that the {@linkplain #getActor() actor} begins sending a message
     * through a given transmission medium.
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
     * <li>The given medium is the current {@linkplain #getSendingMedium() sending
     * medium}.</li>
     * <li>The given message is the current {@linkplain #getSendingMessage() sending
     * message}.</li>
     * <li>The simulation guarantees that it will eventually
     * {@linkplain Actor#tellMessageSendingEnded(Medium, Message, double) call back}
     * to the {@linkplain #getActor() actor} of this interface, to report completion
     * or halting of sending of the message.
     * </ul>
     * </section>
     * 
     * @param medium
     *            The transmission medium (or means) through which to send the
     *            message.
     * @param message
     *            The message to begin sending.
     * @param sendingEndCallBack
     *            An operation to perform when sending of the message ends; or null
     *            for no callback. The call back will be called when sending the
     *            message is complete, or when sending the message has been
     *            interrupted. When the callback is executed, the
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code medium} is null.</li>
     *             <li>If {@code message} is null.</li>
     *             </ul>
     * @throws IllegalStateException
     *             If this is already {@linkplain #getSendingMessage() sending a
     *             message}.
     * @throws MediumUnavailableException
     *             If the {@code medium} is not one of the {@linkplain #getMedia()
     *             currently available media}.
     */
    public void beginSendingMessage(Medium medium, Message message) throws MediumUnavailableException;

    /**
     * <p>
     * The actor for which this is the service interface.
     * </p>
     * <ul>
     * <li>Always have an (non null) actor.</li>
     * <li>This is the {@linkplain Actor#getActorInterface() actor interface} of the
     * actor of this actor interface.</li>
     * </ul>
     * 
     * @return The actor; not null.
     */
    public Actor getActor();

    /**
     * <p>
     * How much of the {@linkplain #getSendingMessage() message that this actor is
     * sending} has been sent through the {@linkplain #getSendingMedium()
     * transmission medium}.
     * </p>
     * <ul>
     * <li>The amount of message sent is never negative.</li>
     * <li>The amount of message sent is zero if the actor is not currently sending
     * a message.</li>
     * <li>The amount of message sent is measured in bits of information.</li>
     * <li>The amount of message sent is less than or equal to the
     * {@linkplain Message#getLength() length} of the message being sent, if a
     * message is being sent.</li>
     * <li>The amount of message sent is equal to the length of the message being
     * sent (if a message is being sent) only for the instance that sending of the
     * message is completed.</li>
     * </ul>
     * <p>
     * The simulation should arrange that, when the actor is sending a message, the
     * amount of message sent increases with time, at a rate similar to the
     * {@linkplain Medium#getTypicalTransmissionRate() typical transmission rate} of
     * the {@linkplain #getSendingMedium() sending medium}.
     * </p>
     * 
     * @return the amount sent
     */
    public double getAmountOfMessageSent();

    /**
     * <p>
     * The current set of transmission media (or means) through which the
     * {@linkplain #getActor() actor} can send {@linkplain Message messages}.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of media.</li>
     * <li>The set of media does not contain an null element.</li>
     * <li>The set of media may change as means of communication become available
     * and cease to be available.
     * <li>The set of media is {@linkplain Collections#unmodifiableSet(Set)
     * unmodifiable}.
     * <li>
     * </ul>
     * 
     * @return the media.
     */
    public Set<Medium> getMedia();

    /**
     * <p>
     * The transmission medium that the {@linkplain #getActor() actor} is currently
     * using to send a {@linkplain #getSendingMessage() message}.
     * </p>
     * <ul>
     * <li>A null value indicates that the actor is not currently sending a
     * message.</li>
     * <li>A non null value is one of the {@linkplain #getMedia() media} that the
     * actor can use.</i>
     * </ul>
     * 
     * @return the medium
     */
    public Medium getSendingMedium();

    /**
     * <p>
     * The message that the {@linkplain #getActor() actor} is currently using to
     * sending.
     * </p>
     * <ul>
     * <li>A null value indicates that the actor is not currently sending a
     * message.</li>
     * <li>The sending message is null if, and only if, the
     * {@linkplain #getSendingMedium() sending medium} is null.</li>
     * </ul>
     * 
     * @return the medium
     */
    public Message getSendingMessage();
}
