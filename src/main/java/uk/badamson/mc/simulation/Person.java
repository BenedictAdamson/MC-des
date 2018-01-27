package uk.badamson.mc.simulation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import uk.badamson.mc.actor.Actor;
import uk.badamson.mc.actor.ActorInterface;
import uk.badamson.mc.actor.MediumUnavailableException;
import uk.badamson.mc.actor.MessageTransferInProgress;
import uk.badamson.mc.actor.medium.HandSignals;
import uk.badamson.mc.actor.medium.Medium;
import uk.badamson.mc.actor.message.IllegalMessageException;
import uk.badamson.mc.actor.message.Message;

/**
 * <p>
 * A simulated person.
 * </p>
 */
public final class Person implements ActorInterface, Actor {
    private final Set<Medium> media = new HashSet<>();

    private MessageTransferInProgress transmissionInProgress;
    private Message transmittingMessage;

    /**
     * <p>
     * Construct a simulated person that is currently doing nothing.
     * </p>
     * <ul>
     * <li>The {@linkplain #getMedia() media} through which this actor can send
     * messages consists of {@linkplain HandSignals hand signals}.</li>
     * <li>This actor is {@linkplain #getMessagesBeingReceived() receiving} no
     * messages.</li>
     * <li>This actor is not {@linkplain #getTransmissionInProgress() transmitting}
     * a message.</li>
     * </ul>
     */
    public Person() {
        media.add(HandSignals.INSTANCE);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IllegalMessageException
     *             {@inheritDoc}
     * @throws IllegalStateException
     *             {@inheritDoc}
     * @throws MediumUnavailableException
     *             {@inheritDoc}
     */
    @Override
    public final void beginSendingMessage(Medium medium, Message message) throws MediumUnavailableException {
        Objects.requireNonNull(medium, "medium");
        Objects.requireNonNull(message, "message");
        if (!medium.canConvey(message)) {
            throw new IllegalMessageException();
        }
        if (!media.contains(medium)) {
            throw new MediumUnavailableException();
        }
        if (transmissionInProgress != null) {
            throw new IllegalStateException("This is already sending a message");
        }
        assert medium instanceof HandSignals;
        transmittingMessage = message;
        transmissionInProgress = new MessageTransferInProgress(medium, null);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Additional constraints of the {@link Person} class:
     * </p>
     * <ul>
     * <li>This {@linkplain ActorInterface actor interface} is its own actor.</li>
     * </ul>
     */
    @Override
    public final Actor getActor() {
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Additional constraints of the {@link Person} class:
     * </p>
     * <ul>
     * <li>This {@linkplain Actor actor} is its own actor interface.</li>
     * </ul>
     */
    @Override
    public final ActorInterface getActorInterface() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Set<Medium> getMedia() {
        return Collections.unmodifiableSet(media);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Set<MessageTransferInProgress> getMessagesBeingReceived() {
        return Collections.emptySet();// TODO
    }

    @Override
    public final MessageTransferInProgress getTransmissionInProgress() {
        return transmissionInProgress;
    }

    @Override
    public final Message getTransmittingMessage() {
        return transmittingMessage;
    }

    /**
     * @param receptionStarted
     */
    @Override
    public void tellBeginReceivingMessage(MessageTransferInProgress receptionStarted) {
        // TODO Auto-generated method stub

    }

    /**
     * @param previousMessageSoFar
     * @param messageBeingReceived
     */
    @Override
    public void tellMessageReceptionProgress(Message previousMessageSoFar,
            MessageTransferInProgress messageBeingReceived) {
        // TODO Auto-generated method stub

    }

    /**
     * @param transmissionProgress
     * @param fullMessage
     */
    @Override
    public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage) {
        // TODO Auto-generated method stub

    }

    /**
     * @param previousMessageSoFar
     */
    @Override
    public void tellMessageTransmissionProgress(Message previousMessageSoFar) {
        // TODO Auto-generated method stub

    }

}
