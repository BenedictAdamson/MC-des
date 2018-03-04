package uk.badamson.mc.mind;

import java.util.Objects;

import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.Message;

/**
 * <p>
 * A concrete class that implements the {@link Mind} interface.
 * </p>
 * <p>
 * This class is intended to be used as a base class. Objects of this type act
 * are <i>null objects</i>: their mutator methods have no behaviour.
 * </p>
 */
public class AbstractMind implements Mind {

    /**
     * <p>
     * Construct a {@linkplain AbstractMind mind}.
     * </p>
     */
    public AbstractMind() {
    }

    @Override
    public void tellBeginReceivingMessage(Medium medium) {
        Objects.requireNonNull(medium, "medium");
    }

    @Override
    public void tellMessageReceptionProgress(MessageTransferInProgress messageBeingReceived, boolean complete) {
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
