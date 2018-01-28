package uk.badamson.mc.actor;

import java.util.Objects;

import uk.badamson.mc.actor.message.Message;

/**
 * <p>
 * A concrete class that implements the {@link Actor} interface.
 * </p>
 * <p>
 * This class is intended to be used as a base class. Objects of this type act
 * are <i>null objects</i>: their mutator methods have no behaviour.
 * </p>
 */
public class AbstractActor implements Actor {

    private final ActorInterface actorInterface;

    /**
     * <p>
     * Construct an {@linkplain Actor actor} that uses a given API (service
     * interface) to effect changes to the simulation.
     * </p>
     * 
     * @param actorInterface
     *            The API (service interface) through which this effects changes to
     *            the simulation.
     * @throws NullPointerException
     *             If {@code actorInterface} is null.
     */
    public AbstractActor(ActorInterface actorInterface) {
        this.actorInterface = Objects.requireNonNull(actorInterface, "actorInterface");
    }

    @Override
    public final ActorInterface getActorInterface() {
        return actorInterface;
    }

    @Override
    public void tellBeginReceivingMessage(MessageTransferInProgress receptionStarted) {
        Objects.requireNonNull(receptionStarted, "receptionStarted");
        if (receptionStarted.getMessageSofar() != null) {
            throw new IllegalArgumentException("reception actually started some time ago.");
        }
    }

    @Override
    public void tellMessageReceptionProgress(Message previousMessageSoFar,
            MessageTransferInProgress messageBeingReceived) {
        Objects.requireNonNull(messageBeingReceived, "messageBeingReceived");
        Objects.requireNonNull(messageBeingReceived.getMessageSofar(), "messageBeingReceived.messageSoFar");
        if (previousMessageSoFar != null && messageBeingReceived.getMessageSofar()
                .getInformationContent() <= previousMessageSoFar.getInformationContent()) {
            throw new IllegalArgumentException("No progress in reception");
        }
    }

    @Override
    public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage) {
        Objects.requireNonNull(transmissionProgress, "transmissionProgress");
        Objects.requireNonNull(fullMessage, "fullMessage");
        final Message messageSofar = transmissionProgress.getMessageSofar();
        if (messageSofar != null) {
            final double fullInformation = fullMessage.getInformationContent();
            final double sentInformation = messageSofar.getInformationContent();
            if (fullInformation < sentInformation) {
                throw new IllegalArgumentException("message sent so far is longer <" + sentInformation
                        + "> than the full message <" + fullInformation + ">");
            } else if (fullInformation == sentInformation && messageSofar != fullMessage) {
                throw new IllegalArgumentException(
                        "Information content of message so far indicates full message sent, but message so far <"
                                + messageSofar + "> is not the full message <" + fullInformation + ">");
            }
        }
    }

    @Override
    public void tellMessageTransmissionProgress(MessageTransferInProgress transmissionProgress, Message fullMessage) {
        Objects.requireNonNull(transmissionProgress, "transmissionProgress");
        Objects.requireNonNull(fullMessage, "fullMessage");
        final Message messageSofar = transmissionProgress.getMessageSofar();
        Objects.requireNonNull(messageSofar, "No progress since start of transmission");
        final double informationSoFar = messageSofar.getInformationContent();
        if (informationSoFar == 0.0) {
            throw new IllegalStateException("No message so far");
        }
    }

}
