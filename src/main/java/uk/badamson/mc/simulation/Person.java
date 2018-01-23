package uk.badamson.mc.simulation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import uk.badamson.mc.actor.Actor;
import uk.badamson.mc.actor.ActorInterface;
import uk.badamson.mc.actor.MediumUnavailableException;
import uk.badamson.mc.actor.MessageTransferInProgress;
import uk.badamson.mc.actor.medium.HandSignals;
import uk.badamson.mc.actor.medium.Medium;
import uk.badamson.mc.actor.message.Message;

/**
 * <p>
 * A simulated person.
 * </p>
 */
public final class Person implements ActorInterface, Actor {
    private final Set<Medium> media = new HashSet<>();

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
     * @param medium
     * @param message
     * @throws MediumUnavailableException
     */
    @Override
    public void beginSendingMessage(Medium medium, Message message) throws MediumUnavailableException {
	// TODO Auto-generated method stub

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

    /**
     * @return
     */
    @Override
    public MessageTransferInProgress getTransmissionInProgress() {
	// TODO Auto-generated method stub
	return null;
    }

    /**
     * @return
     */
    @Override
    public Message getTransmittingMessage() {
	// TODO Auto-generated method stub
	return null;
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
