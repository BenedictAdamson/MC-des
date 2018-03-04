package uk.badamson.mc.simulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import uk.badamson.mc.mind.MediumUnavailableException;
import uk.badamson.mc.mind.MessageTransferInProgress;
import uk.badamson.mc.mind.Mind;
import uk.badamson.mc.mind.MindInterface;
import uk.badamson.mc.mind.ai.AI;
import uk.badamson.mc.mind.medium.HandSignals;
import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.IllegalMessageException;
import uk.badamson.mc.mind.message.Message;
import uk.badamson.mc.mind.message.UnusableIncompleteMessage;

/**
 * <p>
 * A simulated person.
 * </p>
 */
public final class Person implements MindInterface {

    /**
     * <p>
     * While simulating the transmission of a message, the nominal number of times
     * to {@linkplain Mind#tellMessageTransmissionProgress() telling} the
     * {@linkplain #getPlayer() player} of progress in sending the message.
     * </p>
     */
    public static final int MIN_MESSAGE_TRANSMISSION_PROGRESS_COUNT = 4;

    /**
     * <p>
     * While simulating the transmission of a message, the nominal maximum time
     * interval, in seconds, between
     * {@linkplain Mind#tellMessageTransmissionProgress() telling} the
     * {@linkplain #getPlayer() player} of progress in sending the message.
     * </p>
     */
    public static final double MAX_MESSAGE_TRANSMISSION_PROGRESS_INTERVAL = 1.0;

    /**
     * <p>
     * While simulating the transmission of a message, the nominal minimum time
     * interval, in seconds, between
     * {@linkplain Mind#tellMessageTransmissionProgress() telling} the
     * {@linkplain #getPlayer() player} of progress in sending the message.
     * </p>
     * <p>
     * This value also models the minimum reaction time to completion of the
     * transmission.
     * </p>
     */
    public static final double MIN_MESSAGE_TRANSMISSION_PROGRESS_INTERVAL = 0.125;

    private final Clock clock;
    private final AI ai;
    private final Map<Medium, Set<Person>> mediaReceivers = new HashMap<>();
    private final Set<MessageTransferInProgress> messagesBeingReceived = new HashSet<>();

    private MessageTransferInProgress transmissionInProgress;
    private Message transmittingMessage;
    private long previousUpdate;

    /**
     * <p>
     * Construct a simulated person that is currently doing nothing.
     * </p>
     * <ul>
     * <li>The {@linkplain #getClock() clock} of this person is the given clock.
     * <li>This does not have an {@linkplain #getPlayer() actor} (it is null).
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
        this.ai = new AI(clock);
        mediaReceivers.put(HandSignals.INSTANCE, new HashSet<>());
        previousUpdate = clock.getTime();
    }

    /**
     * <p>
     * Add a person as a person that can {@linkplain #getMessagesBeingReceived()
     * receive} messages {@linkplain #beginSendingMessage(Medium, Message) sent} by
     * this person using a given medium.
     * </p>
     * <ul>
     * <li>The given medium is one of the {@linkplain #getMedia() media} through
     * which this person can send messages.</li>
     * <li>Subsequent {@linkplain #beginSendingMessage(Medium, Message) sending} of
     * messages by this person through the medium will result in the given person
     * {@linkplain #getMessagesBeingReceived() receiving} those messages.</li>
     * </ul>
     * 
     * @param medium
     *            The transmission medium (or means) through which this person can
     *            send messages to the receiver
     * @param receiver
     *            The person who can receive messages sent by this person through
     *            the medium.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code medium} is null.</li>
     *             <li>If {@code receiver} is null.</li>
     *             </ul>
     */
    public final void addReceiver(Medium medium, Person receiver) {
        Objects.requireNonNull(medium, "medium");
        Objects.requireNonNull(receiver, "receiver");
        // TODO handle new medium
        mediaReceivers.get(medium).add(receiver);
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
        final Set<Person> receivers = mediaReceivers.get(medium);
        if (receivers == null) {
            throw new MediumUnavailableException();
        }
        if (transmissionInProgress != null) {
            throw new IllegalStateException("This is already sending a message");
        }
        // TODO updateState()
        assert medium instanceof HandSignals;
        transmittingMessage = message;
        final long now = clock.getTime();
        final MessageTransferInProgress messageTransferInProgress0 = new MessageTransferInProgress(medium,
                UnusableIncompleteMessage.EMPTY_MESSAGE);
        transmissionInProgress = messageTransferInProgress0;
        for (final Person receiver : receivers) {
            receiver.messagesBeingReceived.add(messageTransferInProgress0);

            clock.scheduleActionAt(now, new Runnable() {
                @Override
                public final void run() {
                    if (receiver.ai.getPlayer() != null) {
                        receiver.ai.getPlayer().tellBeginReceivingMessage(messageTransferInProgress0);
                    }
                }
            });
        }
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
                final Mind player = getPlayer();
                if (player != null) {
                    getPlayer().tellMessageSendingEnded(finalProgress, fullMessage);
                    // TODO tell receiver actor tellMessageReceptionProgress
                }
            }
        });

        transmittingMessage = null;
        transmissionInProgress = null;
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

    @Override
    public final Set<Medium> getMedia() {
        return Collections.unmodifiableSet(mediaReceivers.keySet());
    }

    @Override
    public final Set<MessageTransferInProgress> getMessagesBeingReceived() {
        return Collections.unmodifiableSet(messagesBeingReceived);
    }

    /**
     * <p>
     * The interface through which the simulation interacts with a player
     * controlling this person.
     * </p>
     * 
     * @return The player, or null if this person does not (yet) have a player.
     */
    public final Mind getPlayer() {
        return ai.getPlayer();
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
                final Mind player = getPlayer();
                if (player != null && transmissionInProgress != null) {
                    getPlayer().tellMessageTransmissionProgress(transmissionInProgress, transmittingMessage);
                    // TODO tell receiver actor tellMessageReceptionProgress
                    scheduleUpdateMessageTransmission();
                }
                /* else updateState() ended message transmission. */
            }
        });
    }

    /**
     * <p>
     * Change the interface through which the simulation interacts with a player
     * controlling this person.
     * </p>
     * 
     * @param player
     *            the interface to use from now on
     * @throws IllegalArgumentException
     *             If {@code player} is not null and the
     *             {@linkplain Mind#getActorInterface() actor interface} of the
     *             {@code actor} is not this object.
     */
    public final void setPlayer(Mind player) {
        ai.setPlayer(player);
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
