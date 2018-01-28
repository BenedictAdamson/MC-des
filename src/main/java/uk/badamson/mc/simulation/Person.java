package uk.badamson.mc.simulation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
     * While simulating the transmission of a message, the nominal minimum time
     * interval, in seconds, between
     * {@linkplain Actor#tellMessageTransmissionProgress() telling} the
     * {@linkplain #getActor() actor} of progress in sending the message.
     * </p>
     */
    public static final double MIN_MESSAGE_TRANSMISSION_PROGRESS_INTERVAL = 1.0;

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
     * directly tell teh actor about state changes. Instead schedules immediate
     * events to do so.
     */
    private void advance(double dt) {
        assert 0.0 < dt;
        if (transmissionInProgress != null) {
            final Message messageSofar = transmissionInProgress.getMessageSofar();
            final double informationSentPreviously = messageSofar == null ? 0.0 : messageSofar.getInformationContent();
            final double fullMessageInformation = transmittingMessage.getInformationContent();
            final Medium medium = transmissionInProgress.getMedium();
            double informationSent = informationSentPreviously + dt * medium.getTypicalTransmissionRate();
            informationSent = Double.min(informationSent, fullMessageInformation);
            if (informationSent < fullMessageInformation) {
                transmissionInProgress = new MessageTransferInProgress(medium,
                        transmittingMessage.getPartialMessage(informationSent));
            } else {
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
        assert medium instanceof HandSignals;
        transmittingMessage = message;
        transmissionInProgress = new MessageTransferInProgress(medium, null);
        scheduleUpdateMessageTransmissionProgress();
    }

    private void clearSendingMessage() {
        transmittingMessage = null;
        transmissionInProgress = null;
    }

    private void endMessageSending() {
        final Message fullMessage = transmittingMessage;
        final MessageTransferInProgress finalProgress = new MessageTransferInProgress(
                transmissionInProgress.getMedium(), fullMessage);
        clock.scheduleActionAt(clock.getTime(), new Runnable() {
            @Override
            public final void run() {
                actor.tellMessageSendingEnded(finalProgress, fullMessage);
            }
        });
        clearSendingMessage();
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
        if (transmissionInProgress == null) {
            throw new IllegalStateException("No a transmission in progress");
        }
        clearSendingMessage();
    }

    private void scheduleUpdateMessageTransmissionProgress() {
        final Message messageSofar = transmissionInProgress.getMessageSofar();
        final double transmissionRate = transmissionInProgress.getMedium().getTypicalTransmissionRate();
        final double fullInformation = transmittingMessage.getInformationContent();
        final double sentInformation = messageSofar == null ? 0.0 : messageSofar.getInformationContent();
        final double transmissionTime = fullInformation / transmissionRate;
        final double remainingTime = (fullInformation - sentInformation) / transmissionRate;
        double delay = transmissionTime / (1 + MIN_MESSAGE_TRANSMISSION_PROGRESS_COUNT);
        delay = Double.min(delay, MIN_MESSAGE_TRANSMISSION_PROGRESS_INTERVAL);
        delay = Double.min(delay, remainingTime);
        final long dt = Long.max(1, (long) (1E3 * delay));
        clock.scheduleDelayedAction(dt, TimeUnit.MILLISECONDS, new Runnable() {

            @Override
            public void run() {
                updateState();
                if (actor != null && transmissionInProgress != null
                        && transmissionInProgress.getMessageSofar() != null) {
                    actor.tellMessageTransmissionProgress(transmissionInProgress, transmittingMessage);
                    scheduleUpdateMessageTransmissionProgress();
                }
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
        if (actor != null && actor.getActorInterface() != this) {
            throw new IllegalArgumentException("actor does not use this ActorInterface");
        }
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
