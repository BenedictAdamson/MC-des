package uk.badamson.mc.actor.message;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.actor.Actor;
import uk.badamson.mc.actor.Medium;

/**
 * <p>
 * A message that an {@linkplain Actor actor} might send through a
 * {@linkplain Medium medium}.
 * </p>
 * <p>
 * The message may be a {@linkplain Command command}, and the medium may enable
 * the intended recipient of that command to receive the command. Objects of
 * this class are therefore means by which actors can express orders to
 * sub-ordinates.
 * </p>
 */
@Immutable
public interface Message {

    /**
     * <p>
     * Whether this object is <em>equivalent</em> to another object.
     * </p>
     * <p>
     * {@link Message} objects have <i>value semantics</i>: this object is
     * equivalent to another object if, and only if, they have the same type and
     * their attributes are equivalent.
     * </p>
     * 
     * @param that
     *            The other object.
     * @return whether equivalent
     */
    @Override
    public boolean equals(Object that);

    /**
     * <p>
     * The information content of this message; its length in a notional essential
     * compact form.
     * </p>
     * <ul>
     * <li>The length of a message is positive.</li>
     * <li>The length of a message is {@linkplain Double#isFinite() finite}.</li>
     * <li>The unit of message length is bits of information.</li>
     * </ul>
     * <p>
     * Longer messages take longer to transmit. Actors must therefore be judicious
     * in sending long, detailed and comprehensive messages rather than short, vague
     * and limited messages.
     * </p>
     * 
     * @return the length
     */
    public double getInformationContent();

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
