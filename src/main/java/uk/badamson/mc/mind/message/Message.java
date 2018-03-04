package uk.badamson.mc.mind.message;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.mind.Mind;
import uk.badamson.mc.mind.medium.Medium;

/**
 * <p>
 * A message that a {@linkplain Mind minds} might send through a
 * {@linkplain Medium medium}.
 * </p>
 * <p>
 * The message may be a {@linkplain Command command}, and the medium may enable
 * the intended recipient of that command to receive the command. Objects of
 * this class are therefore means by which minds can express orders to
 * sub-ordinates.
 * </p>
 */
@Immutable
public interface Message extends MessageElement {

    /**
     * <p>
     * Retrieve or create a {@link Message} object that represents partial
     * transmission of reception of this message.
     * </p>
     * <ul>
     * <li>Always returns a (non null) message.</li>
     * <li>The {@linkplain #getInformationContent() length (information content)} of
     * the returned message is equal to the given part length.</li>
     * <li>The returned message is never equivalent to this message.</li>
     * </ul>
     * 
     * @param partLength
     *            The amount of this message that has been transmitted or received.
     *            Measured in bits of information.
     * @return the partial message
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If {@code partLength} is not positive.</li>
     *             <li>If {@code partLength} is not less than the length of this
     *             message.</li>
     *             </ul>
     */
    public Message getPartialMessage(double partLength);
}
