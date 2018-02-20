package uk.badamson.mc.actor;

import java.util.Objects;

import uk.badamson.mc.actor.message.Message;
import uk.badamson.mc.actor.message.UnusableIncompleteMessage;

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

    /**
     * <p>
     * Construct an {@linkplain Actor actor}.
     * </p>
     */
    public AbstractActor() {
    }

    @Override
    public void tellBeginReceivingMessage(MessageTransferInProgress receptionStarted) {
        Objects.requireNonNull(receptionStarted, "receptionStarted");
        if (!UnusableIncompleteMessage.EMPTY_MESSAGE.equals(receptionStarted.getMessageSofar())) {
            throw new IllegalArgumentException("reception actually started some time ago.");
        }
    }

    @Override
    public void tellMessageReceptionProgress(MessageTransferInProgress messageBeingReceived) {
        Objects.requireNonNull(messageBeingReceived, "messageBeingReceived");
        Objects.requireNonNull(messageBeingReceived.getMessageSofar(), "messageBeingReceived.messageSoFar");
    }

    @Override
    public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage) {
        Objects.requireNonNull(transmissionProgress, "transmissionProgress");
        Objects.requireNonNull(fullMessage, "fullMessage");
        final Message messageSofar = transmissionProgress.getMessageSofar();
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

    @Override
    public void tellMessageTransmissionProgress(MessageTransferInProgress transmissionProgress, Message fullMessage) {
        Objects.requireNonNull(transmissionProgress, "transmissionProgress");
        Objects.requireNonNull(fullMessage, "fullMessage");
        final Message messageSofar = transmissionProgress.getMessageSofar();
        final double informationSoFar = messageSofar.getInformationContent();
        if (informationSoFar == 0.0) {
            throw new IllegalStateException("No message so far");
        }
    }

}
