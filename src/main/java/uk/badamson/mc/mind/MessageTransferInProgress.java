package uk.badamson.mc.mind;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.Message;
import uk.badamson.mc.mind.message.UnusableIncompleteMessage;

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
public final class MessageTransferInProgress {
    private final Medium medium;
    private final Message messageSofar;

    /**
     * <p>
     * Construct an object with given values.
     * </p>
     * 
     * @param medium
     *            The transmission medium (or means) through which the transmission
     *            is being made.
     * @param messageSofar
     *            The message that has been sent so far.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code medium} is null.</li>
     *             <li>If {@code messageSofar} is null.</li>
     *             </ul>
     */
    public MessageTransferInProgress(Medium medium, Message messageSofar) {
        this.medium = Objects.requireNonNull(medium, "medium");
        this.messageSofar = Objects.requireNonNull(messageSofar, "messageSofar");
    }

    /**
     * <p>
     * Whether this object is <dfn>equivalent</dfn> to another object.
     * </p>
     * <p>
     * The {@link MessageTransferInProgress} class has <i>value semantics</i>: this
     * object is equivalent to another if, and only if, the other object is also a
     * {@linkplain MessageTransferInProgress} and the two obejcts have equivaletn
     * attributes.
     * </p>
     * 
     * @param obj
     *            The other object.
     * @return whether equivalent.
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MessageTransferInProgress other = (MessageTransferInProgress) obj;
        return medium.equals(other.medium) && messageSofar.equals(other.messageSofar);
    }

    /**
     * <p>
     * The transmission medium (or means) through which the transmission is being
     * made.
     * </p>
     * 
     * @return the medium; not null.
     */
    public final Medium getMedium() {
        return medium;
    }

    /**
     * <p>
     * The message that has been sent so far.
     * </p>
     * <ul>
     * <li>Always have a (non null) message so far.</li>
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
    public final Message getMessageSofar() {
        return messageSofar;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + medium.hashCode();
        result = prime * result + messageSofar.hashCode();
        return result;
    }

    @Override
    public final String toString() {
        return "MessageTransferInProgress [medium=" + medium + ", messageSofar=" + messageSofar + "]";
    }

}
