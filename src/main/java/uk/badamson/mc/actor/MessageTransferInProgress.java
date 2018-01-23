package uk.badamson.mc.actor;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.actor.medium.Medium;
import uk.badamson.mc.actor.message.Message;
import uk.badamson.mc.actor.message.UnusableIncompleteMessage;

/**
 * <p>
 * Information about the progress of transmission of a {@linkplain Message
 * message} through a {@linkplain Medium communication medium}.
 * </p>
 * <p>
 * Objects of this class can represent a transmission in progress from the
 * perspective of a message sender and the perspective of a message receiver.
 * </p>
 */
@Immutable
public interface MessageTransferInProgress {

    /**
     * <p>
     * The transmission medium (or means) through which the transmission is being
     * made
     * </p>
     * 
     * @return the medium; not null.
     */
    public Medium getMedium();

    /**
     * <p>
     * The message that has been sent so far.
     * </p>
     * <ul>
     * <li>A null message so far indicates that transmission has only just
     * started.</li>
     * <li>This might be an {@link UnusableIncompleteMessage}, if no comprehensible
     * information has been transmitted so far.
     * <li>
     * <li>If a complex message is being sent, this indicates how much of the
     * message has been sent or received so far.</li>
     * <li>From the perspective of a sender, this indicates the content that has
     * been sent so far.</li>
     * <li>From the perspective of a receiver, this indicates the content that has
     * been received so far.</li>
     * <li>The {@linkplain Message#getInformationContent() length (information
     * content)} of this message indicates how much of the message being sent has
     * been sent or received.</li>
     * </ul>
     * 
     * @return the message sent so far.
     */
    public Message getMessageSofar();

}
