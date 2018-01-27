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
public final class Person implements ActorInterface {
    private final Set<Medium> media = new HashSet<>();

    private Actor actor;
    private MessageTransferInProgress transmissionInProgress;
    private Message transmittingMessage;

    /**
     * <p>
     * Construct a simulated person that is currently doing nothing.
     * </p>
     * <ul>
     * <li>This does not have an {@linkplain #getActor() actor} (it is null).
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
     * <p>
     * The interface through which the simulation interacts with a human or AI
     * player controlling this person.
     * </p>
     * <ul>
     * <li>Always have an (non null) actor.</li>
     * <li>This is the {@linkplain Actor#getActorInterface() actor interface} of the
     * actor of this actor interface.</li>
     * </ul>
     * 
     * @return The actor.
     */
    @Override
    public final Actor getActor() {
        return actor;
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
     * <p>
     * Change the interface through which the simulation interacts with a human or
     * AI player controlling this person.
     * </p>
     * 
     * @param actor
     *            the interface to use from now on
     * @throws IllegalArgumentException
     *             If {@code actor} is not null and the
     *             {@linkplain Actor#getActorInterface() actor interface} of the
     *             {@code actor} is not this object.
     */
    public final void setActor(Actor actor) {
        if (actor != null && actor.getActorInterface() != this) {
            throw new IllegalArgumentException("actor does not use this ActorInterface");
        }
        this.actor = actor;
    }

}
