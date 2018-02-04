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
import uk.badamson.mc.actor.message.UnusableIncompleteMessage;

/**
 * <p>
 * A simulated person.
 * </p>
 */
public final class Person implements ActorInterface {

    /**
     * <p>
     * While simulating the transmission of a message, the nominal number of times
     * to {@linkplain Actor#tellMessageTransmissionProgress() telling} the
     * {@linkplain #getActor() actor} of progress in sending the message.
     * </p>
     */
    public static final int MIN_MESSAGE_TRANSMISSION_PROGRESS_COUNT = 4;

    /**
     * <p>
     * While simulating the transmission of a message, the nominal maximum time
     * interval, in seconds, between
     * {@linkplain Actor#tellMessageTransmissionProgress() telling} the
     * {@linkplain #getActor() actor} of progress in sending the message.
     * </p>
     */
    public static final double MAX_MESSAGE_TRANSMISSION_PROGRESS_INTERVAL = 1.0;

    /**
     * <p>
     * While simulating the transmission of a message, the nominal minimum time
     * interval, in seconds, between
     * {@linkplain Actor#tellMessageTransmissionProgress() telling} the
     * {@linkplain #getActor() actor} of progress in sending the message.
     * </p>
     * <p>
     * This value also models the minimum reaction time to completion of the
     * transmission.
     * </p>
     */
    public static final double MIN_MESSAGE_TRANSMISSION_PROGRESS_INTERVAL = 0.125;

    private final Clock clock;
    private final Set<Medium> media = new HashSet<>();

    private Actor actor;
    private MessageTransferInProgress transmissionInProgress;
    private Message transmittingMessage;
    private long previousUpdate;

    /**
     * <p>
     * Construct a simulated person that is currently doing nothing.
     * </p>
     * <ul>
     * <li>The {@linkplain #getClock() clock} of this person is the given clock.
     * <li>This does not have an {@linkplain #getActor() actor} (it is null).
     * <li>The {@linkplain #getMedia() media} through which this actor can send
     * messages consists of {@linkplain HandSignals hand signals}.</li>
     * <li>This actor is {@linkplain #getMessagesBeingReceived() receiving} no
     * messages.</li>
     * <li>This actor is not {@linkplain #getTransmissionInProgress() transmitting}
     * a message.</li>
     * </ul>
     * 
     * @param clock
     *            The clock of the simulated world that this person is in.
     * @throws NullPointerException
     *             If {@code clock} is null.
     */
    public Person(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        media.add(HandSignals.INSTANCE);
        previousUpdate = clock.getTime();
    }

    /*
     * To avoid tricky corner cases when an actor triggers a lazy update, does not
     * directly tell the actor about state changes. Instead schedules immediate
     * events to do so.
     */
    private void advance(double dt) {
        assert 0.0 < dt;
        if (transmissionInProgress != null) {
            final double informationSentPreviously = transmissionInProgress.getMessageSofar().getInformationContent();
            final double fullMessageInformation = transmittingMessage.getInformationContent();
            final Medium medium = transmissionInProgress.getMedium();
            double informationSent = informationSentPreviously + dt * medium.getTypicalTransmissionRate();
            informationSent = Double.min(informationSent, fullMessageInformation);
            if (informationSent < fullMessageInformation) {
                transmissionInProgress = new MessageTransferInProgress(medium,
                        transmittingMessage.getPartialMessage(informationSent));
            } else {
                transmissionInProgress = new MessageTransferInProgress(transmissionInProgress.getMedium(),
                        transmittingMessage);
                endMessageSending();
            }
        }
    }

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
        // TODO updateState()
        assert medium instanceof HandSignals;
        transmittingMessage = message;
        transmissionInProgress = new MessageTransferInProgress(medium, UnusableIncompleteMessage.EMPTY_MESSAGE);
        scheduleUpdateMessageTransmission();
    }

    private void endMessageSending() {
        assert transmissionInProgress != null;
        assert transmittingMessage != null;
        final MessageTransferInProgress finalProgress = transmissionInProgress;
        final Message fullMessage = transmittingMessage;

        clock.scheduleActionAt(clock.getTime(), new Runnable() {
            @Override
            public final void run() {
                if (actor != null) {
                    actor.tellMessageSendingEnded(finalProgress, fullMessage);
                }
            }
        });

        transmittingMessage = null;
        transmissionInProgress = null;
    }

    /**
     * <p>
     * The interface through which the simulation interacts with a human or AI
     * player controlling this person.
     * </p>
     * 
     * @return The actor, or null if this person does not (yet) have a human or AI
     *         controller.
     */
    public final Actor getActor() {
        return actor;
    }

    /**
     * <p>
     * The clock of the simulated world that this person is in.
     * </p>
     * 
     * @return the clock; not null
     */
    public final Clock getClock() {
        return clock;
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
        updateState();
        return transmissionInProgress;
    }

    @Override
    public final Message getTransmittingMessage() {
        return transmittingMessage;
    }

    @Override
    public final void haltSendingMessage() {
        updateState();
        if (transmissionInProgress == null) {
            throw new IllegalStateException("Not a transmission in progress");
        }
        endMessageSending();
    }

    private void scheduleUpdateMessageTransmission() {
        final double transmissionRate = transmissionInProgress.getMedium().getTypicalTransmissionRate();
        final double fullInformation = transmittingMessage.getInformationContent();
        final double sentInformation = transmissionInProgress.getMessageSofar().getInformationContent();
        final double transmissionTime = fullInformation / transmissionRate;
        final double remainingTime = (fullInformation - sentInformation) / transmissionRate;
        double delay = transmissionTime / (1 + MIN_MESSAGE_TRANSMISSION_PROGRESS_COUNT);
        delay = Double.min(delay, MAX_MESSAGE_TRANSMISSION_PROGRESS_INTERVAL);
        delay = Double.min(delay, remainingTime);
        delay = Double.max(delay, MIN_MESSAGE_TRANSMISSION_PROGRESS_INTERVAL);
        clock.scheduleDelayedActionSeconds(delay, new Runnable() {

            @Override
            public void run() {
                updateState();
                if (actor != null && transmissionInProgress != null) {
                    actor.tellMessageTransmissionProgress(transmissionInProgress, transmittingMessage);
                    scheduleUpdateMessageTransmission();
                }
                /* else updateState() ended message transmission. */
            }
        });
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
        this.actor = actor;
    }

    private void updateState() {
        final long time = clock.getTime();
        final long dt = time - previousUpdate;
        if (0 < dt) {
            final double nanoSeconds = clock.getUnit().toNanos(dt);
            advance(1E-9 * nanoSeconds);
            previousUpdate = time;
        }
    }

}
